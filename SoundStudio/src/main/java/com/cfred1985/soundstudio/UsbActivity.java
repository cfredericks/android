package com.cfred1985.soundstudio;

import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

public class UsbActivity extends Activity implements OnClickListener, Runnable
{
    private static final String TAG = "UsbActivity";

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint mEndpointIntr;
    private SensorManager mSensorManager;
    private Sensor mGravitySensor;

    // USB control commands
    private static final int CMD_TOGGLE_LED       = 0x80;
    private static final int CMD_GET_PB_STATE     = 0x81;
    private static final int CMD_GET_LED_STATE    = 0x82;
    private static final int CMD_TOGGLE_ANIMATION = 0x83;
    private static final int CMD_PLAY_DTMF        = 0x84;
    private static final int CMD_GET_POT_STATE    = 0x85;

    // Constants for accelerometer orientation
    private static final int TILT_LEFT = 1;
    private static final int TILT_RIGHT = 2;
    private static final int TILT_UP = 4;
    private static final int TILT_DOWN = 8;
    private static final double THRESHOLD = 5.0;

    UsbDevice device;

    @Override
    public void run()
    {
        while (true)
        {
            // Get LED state
            Byte result = GetByteReply(CMD_GET_LED_STATE);
            if (result == null)
            {
                Log.e(TAG, "Bad reply for LED state request");
                break;
            }
            ((CheckBox)findViewById(R.id.chkLedStatus)).setChecked(result == 0x01);

            // Get push button state
            result = GetByteReply(CMD_GET_PB_STATE);
            if (result == null)
            {
                Log.e(TAG, "Bad reply for push button state request");
                break;
            }
            ((CheckBox)findViewById(R.id.chkPushbuttonStatus)).setChecked(result == 0x00);

            // Get potentiometer state
            result = GetByteReply(CMD_GET_POT_STATE);
            if (result == null)
            {
                Log.e(TAG, "Bad reply for potentiometer state request");
                break;
            }
            ((ProgressBar)findViewById(R.id.pgsPotentiometer)).setProgress(result);

            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {}
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);

        // Setup listeners
        findViewById(R.id.btnToggleLed).setOnClickListener(this);
        findViewById(R.id.btnToggleAnimation).setOnClickListener(this);

        findViewById(R.id.btnDtmf0).setOnClickListener(this);
        findViewById(R.id.btnDtmf1).setOnClickListener(this);
        findViewById(R.id.btnDtmf2).setOnClickListener(this);
        findViewById(R.id.btnDtmf3).setOnClickListener(this);
        findViewById(R.id.btnDtmf4).setOnClickListener(this);
        findViewById(R.id.btnDtmf5).setOnClickListener(this);
        findViewById(R.id.btnDtmf6).setOnClickListener(this);
        findViewById(R.id.btnDtmf7).setOnClickListener(this);
        findViewById(R.id.btnDtmf8).setOnClickListener(this);
        findViewById(R.id.btnDtmf9).setOnClickListener(this);

        findViewById(R.id.btnDtmfDial).setOnClickListener(this);
        findViewById(R.id.btnDtmfRing).setOnClickListener(this);
        findViewById(R.id.btnDtmfBusy).setOnClickListener(this);

        ((ProgressBar)findViewById(R.id.pgsPotentiometer)).setMax(0xFF);

        device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);

        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(mGravityListener);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(mGravityListener, mGravitySensor,
                                        SensorManager.SENSOR_DELAY_NORMAL);

        Intent intent = getIntent();
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();

        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
        {
            ((TextView)findViewById(R.id.lblUsbStatus)).setText("Disconnected");
            setDevice(device);
        }
        else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
        {
            ((TextView)findViewById(R.id.lblUsbStatus)).setText("Connected");
            if (mDevice != null && mDevice.equals(device))
            {
                setDevice(null);
            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    private void setDevice(UsbDevice device)
    {
        Log.d(TAG, "setDevice " + device);
        if (device.getInterfaceCount() != 1)
        {
            Log.e(TAG, "Could not find interface");
            return;
        }
        UsbInterface intf = device.getInterface(0);
        // device should have one endpoint
        if (intf.getEndpointCount() != 1)
        {
            Log.e(TAG, "Could not find endpoint");
            return;
        }
        // endpoint should be of type interrupt
        UsbEndpoint ep = intf.getEndpoint(0);
        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_INT)
        {
            Log.e(TAG, "Endpoint is not interrupt type");
            return;
        }
        mDevice = device;
        mEndpointIntr = ep;
        if (device != null)
        {
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null && connection.claimInterface(intf, true))
            {
                Log.d(TAG, "open SUCCESS");
                mConnection = connection;
                Thread thread = new Thread(this);
                thread.start();

            }
            else
            {
                Log.d(TAG, "open FAIL");
                mConnection = null;
            }
        }
    }

    // Accelerometer readings
    private int mLastValue = 0;
    SensorEventListener mGravityListener = new SensorEventListener()
    {
        public void onSensorChanged(SensorEvent event)
        {
            // Compute current tilt
            int value = 0;
            if (event.values[0] < -THRESHOLD)
            {
                value += TILT_LEFT;
            }
            else if (event.values[0] > THRESHOLD)
            {
                value += TILT_RIGHT;
            }
            if (event.values[1] < -THRESHOLD)
            {
                value += TILT_UP;
            }
            else if (event.values[1] > THRESHOLD)
            {
                value += TILT_DOWN;
            }

            // Send motion command if the tilt changed
            if (value != mLastValue)
            {
                mLastValue = value;
                switch (value)
                {
                    case TILT_LEFT:
                        // TODO(coreyf): Do something on LEFT tilt
                        break;
                    case TILT_RIGHT:
                        // TODO(coreyf): Do something on RIGHT tilt
                        break;
                    case TILT_UP:
                        // TODO(coreyf): Do something on UP tilt
                        break;
                    case TILT_DOWN:
                        // TODO(coreyf): Do something on DOWN tilt
                        break;
                    default:
                        // TODO(coreyf): Do something on no tilt
                        break;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {}
    };

    public void onClick(View v)
    {
        // Get non-DTMF commands
        switch (v.getId())
        {
            case R.id.btnToggleLed:
                SendCommand(CMD_TOGGLE_LED);
                return;
            case R.id.btnToggleAnimation:
                SendCommand(CMD_TOGGLE_ANIMATION);
                return;
        }

    	/*
                1209 Hz   1336 Hz   1477 Hz
        697 Hz  1         2         3
        770 Hz  4         5         6
        852 Hz  7         8         9
        941 Hz  *         0         #

                            Low frequency   High frequency
        Busy signal (US)    480 Hz          620 Hz
        Ringback tone (US)  440 Hz          480 Hz
        Dial tone (US)      350 Hz          440 Hz
        */

        // Get first frequency
        Short freq1;
        switch (v.getId())
        {
            case R.id.btnDtmf1:
            case R.id.btnDtmf4:
            case R.id.btnDtmf7:
            case R.id.btnDtmfStar:
                freq1 = 1209;
                break;
            case R.id.btnDtmf2:
            case R.id.btnDtmf5:
            case R.id.btnDtmf8:
            case R.id.btnDtmf0:
                freq1 = 1336;
                break;
            case R.id.btnDtmf3:
            case R.id.btnDtmf6:
            case R.id.btnDtmf9:
            case R.id.btnDtmfHash:
                freq1 = 1477;
                break;
            case R.id.btnDtmfBusy:
            case R.id.btnDtmfRing:
                freq1 = 480;
                break;
            case R.id.btnDtmfDial:
                freq1 = 350;
                break;
            default:
                return;
        }
        // Get second frequency
        Short freq2;
        switch (v.getId())
        {
            case R.id.btnDtmf1:
            case R.id.btnDtmf2:
            case R.id.btnDtmf3:
                freq2 = 697;
                break;
            case R.id.btnDtmf4:
            case R.id.btnDtmf5:
            case R.id.btnDtmf6:
                freq2 = 770;
                break;
            case R.id.btnDtmf7:
            case R.id.btnDtmf8:
            case R.id.btnDtmf9:
                freq2 = 852;
                break;
            case R.id.btnDtmfStar:
            case R.id.btnDtmf0:
            case R.id.btnDtmfHash:
                freq2 = 941;
                break;
            case R.id.btnDtmfDial:
            case R.id.btnDtmfRing:
                freq2 = 440;
                break;
            case R.id.btnDtmfBusy:
                freq2 = 620;
                break;
            default:
                return;
        }

        SendCommand(CMD_PLAY_DTMF, freq1, freq2);
    }

    /**
     * Send a request for byte of data over USB
     */
    private Byte GetByteReply(int command)
    {
        ByteBuffer buffer = ByteBuffer.allocate(64);
        UsbRequest request = new UsbRequest();
        request.initialize(mConnection, mEndpointIntr);

        // Queue a request on the interrupt endpoint
        request.queue(buffer, buffer.capacity());
        // Send poll status command
        SendCommand(command);

        if (mConnection.requestWait() == request)
        {
            byte cmd = buffer.get(0);
            if (cmd != command)
            {
                return null;
            }

            return buffer.get(1);
        }
        else
        {
            return null;
        }
    }

    /**
     * Send a command over USB
     */
    private void SendCommand(int cmd, Object... freqs)
    {
        ByteBuffer outBuffer = ByteBuffer.allocate(64);

        // Add the command value
        outBuffer.put((byte)cmd);

        // Check if we have a DTMF command
        if (freqs != null && freqs.length >= 2)
        {
			/*
			outBuffer[2] = (freq1 & 0xFF00) >> 8;
			outBuffer[3] = freq1 & 0x00FF;
			outBuffer[4] = (freq2 & 0xFF00) >> 8;
			outBuffer[5] = freq2 & 0x00FF;
			*/
            outBuffer.putShort((Short)freqs[0]);
            outBuffer.putShort((Short)freqs[1]);
        }

        if (mConnection != null)
        {
            mConnection.controlTransfer(0x21, 0x9, 0x200, 0, outBuffer.array(), outBuffer.capacity(), 0);
        }
        else
        {
            Log.e(TAG, "Could not send command '" + cmd + "' since there is no USB connection");
        }
    }
}
