package com.cfred1985.soundstudio;

import java.util.Arrays;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.cfred1985.util.ArrayIndexComparator;
import com.cfred1985.util.audio.SoundMixer;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class AudioActivity extends Activity
{
    private static final String TAG = "AudioActivity";

    // Looping flag for audio recording
    private volatile Boolean stopped = false;

    // UI thread handler for dispatching UI changes
    private final Handler mHandler = new Handler();
    private Thread updateThread;

    // For recording and playing audio
    private AudioRecord recorder;
    private AudioTrack track = null;

    // Constants for audio generation and sampling
    private final int sample_rate = 8000;
    private final int fft_size = 2048;

    // Our in-place FFT for a double array
    private final  RealDoubleFFT transformer = new RealDoubleFFT(fft_size);

    // Objects for drawing frequency spectrum
    private ImageView imageView;
    private EditText txtFreq1;
    private EditText txtFreq2;
    private EditText txtDuration;
    private EditText txtFrequencies;
    private Button btnCaptureAudio;
    private Button btnPlaySound;

    private final Bitmap bitmap = Bitmap.createBitmap(384, 100, Bitmap.Config.ARGB_8888);
    private final Canvas canvas = new Canvas(bitmap);
    private final Paint paint = new Paint();

    /**
     * Called when the activity is first created
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        Log.d(TAG, "Creating Activity");

        txtFreq1 = (EditText)this.findViewById(R.id.txtFreq1);
        txtFreq2 = (EditText)this.findViewById(R.id.txtFreq2);;
        txtDuration = (EditText)this.findViewById(R.id.txtDuration);
        txtFrequencies = (EditText)this.findViewById(R.id.txtFrequencies);
        btnPlaySound = (Button)this.findViewById(R.id.btnPlaySound);
        btnCaptureAudio = (Button)this.findViewById(R.id.btnCaptureAudio);

        // Setup bitmap for displaying audio spectrum
        imageView = (ImageView)this.findViewById(R.id.imageView01);
        imageView.setImageBitmap(bitmap);
    }

    /**
     * Called when the Activity is destroyed
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();

        Log.d(TAG, "Destructing Activity");

        stopped = true;

        if (recorder != null)
        {
            if (recorder.getState() == AudioRecord.RECORDSTATE_RECORDING)
                recorder.stop();
            recorder.release();
            recorder = null;
        }
        if (track != null)
        {
            if (track.getState() == AudioTrack.PLAYSTATE_PLAYING)
                track.stop();
            track.flush();
            track.release();
            track = null;
        }
        if (updateThread != null && updateThread.isAlive())
        {
            try
            {
                updateThread.join();
                updateThread = null;
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the largest data sample in an array
     */
    double getMax(double[] data)
    {
        double curMax = Double.MIN_VALUE;
        for (double val : data)
        {
            if (val > curMax)
                curMax = val;
        }
        return curMax;
    }

    /**
     * Displays an array of data as a bar chart in an ImageView
     */
    protected void onProgressUpdate(double[] toTransform)
    {
        Log.d(TAG, "Updating frequency spectrum");

        canvas.drawColor(Color.BLACK);
        paint.setColor(Color.GREEN);

        final double[] data = toTransform;

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Log.d(TAG, "Calculating frequency spectrum plot");

                int maxY = bitmap.getHeight();
                int maxX = bitmap.getWidth();
                double maxData = getMax(data);
                int xScale = maxX / data.length;
                double yScale = maxY / maxData;
                for (int i = 0; i < data.length; i++)
                {
                    int downy = (int)(maxY - Math.abs(data[i]) * yScale);
                    int x = i * xScale;
                    canvas.drawLine(x, downy, x, maxY, paint);
                    imageView.invalidate();
                }
            }
        });
    }

    /**
     * Callback function for playing a DTMF sample
     */
    public void btnPlaySound_OnClick(View v)
    {
        btnPlaySound.setEnabled(false);

        // Generate DTMF audio sample
        double freq1 = Double.parseDouble(txtFreq1.getText().toString());
        double freq2 = Double.parseDouble(txtFreq2.getText().toString());
        double duration = Double.parseDouble(txtDuration.getText().toString());

        if (track != null)
        {
            track.stop();
            track.flush();
            track.release();
        }

        SoundMixer.PlaySound(duration, sample_rate, freq1, freq2);
        btnPlaySound.setEnabled(true);
    }

    /**
     * Callback function to record audio and take find the frequency spectrum
     */
    public void btnCaptureAudio_OnClick(View v)
    {
        if ("Start Capture".equals(btnCaptureAudio.getText()))
        {
            try
            {
                Log.i(TAG, "Attempting to start recording audio");

                //recorder = findAudioRecord();
                recorder = new AudioRecord(AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, fft_size);
                if (recorder == null)
                {
                    Log.e(TAG, "No AudioRecord device could be found");
                    btnCaptureAudio.setEnabled(false);
                    return;
                }

                recorder.startRecording();

                btnCaptureAudio.setText("Stop Capture");

                (updateThread = new Thread(
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Log.d(TAG, "Reading audio samples and taking FFT");

                                short[] buf = new short[fft_size];
                                double[] result = new double[fft_size];
                                double scale = sample_rate / (2.0 * fft_size);
                                stopped = false;
                                while(!stopped)
                                {
                                    // Get audio samples from the microphone
                                    int N = recorder.read(buf, 0, buf.length);

                                    // Convert to a double array and take FFT
                                    for (int i = 0; i < result.length && i < buf.length; i++)
                                    {
                                        result[i] = (double)buf[i] / Short.MAX_VALUE;
                                    }
                                    transformer.ft(result);

                                    // Plot spectrum
                                    onProgressUpdate(result);

                                    // Sort the spectrum, keeping track of the indices for frequency calculation
                                    ArrayIndexComparator comparator = new ArrayIndexComparator(result);
                                    Integer[] indexes = comparator.createIndexArray();
                                    Arrays.sort(indexes, comparator);

                                    // Scale the indices to make them frequency in Hertz
                                    final Double[] result_final = new Double[result.length];
                                    for (int i = 0; i < indexes.length; i++)
                                    {
                                        result_final[i] = indexes[i] * scale;
                                    }

                                    // List the top frequency components in a text box
                                    mHandler.post(
                                            new Runnable()
                                            {
                                                @Override
                                                public void run()
                                                {
                                                    Log.d(TAG, "Displaying top 5 frequencies");
                                                    txtFrequencies.setText("");
                                                    for (int i = 0; i < 5; i++)
                                                    {
                                                        txtFrequencies.append(result_final[i] + " Hz\n");
                                                    }
                                                }
                                            });
                                }

                            }
                        })).start();
            }
            catch(Throwable x)
            {
                Log.w(TAG, "Error reading voice audio", x);
            }
        }
        else
        {
            // Try to stop recording audio
            try
            {
                stopped = true;
                recorder.stop();
                recorder.release();

                updateThread.join();

                ((Button)v).setText("Start Capture");
            }
            catch(Throwable x)
            {
                Log.w("Audio", "Error closing voice audio", x);
                x.printStackTrace();
            }
        }
    }
}
