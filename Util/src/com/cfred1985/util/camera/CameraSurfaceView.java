package com.cfred1985.util.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.cfred1985.util.camera.CameraFormat;
import com.cfred1985.util.graphics.ImageFilters;

public class CameraSurfaceView extends SurfaceView implements Callback, Camera.PreviewCallback
{
    private static final String TAG = "CameraSurfaceView";

    private int width;
    private int height;

    private final SurfaceHolder mHolder;

    private Camera mCamera;
    private int[] rgbints;

    private boolean isPreviewRunning = false;

    public CameraSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

// @Override
// protected void onDraw(Canvas canvas) {
// Log.w(this.getClass().getName(), "On Draw Called");
// }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        synchronized (this)
        {
            if (isPreviewRunning)
                return;

            this.setWillNotDraw(false); // This allows us to make our own draw calls to this canvas

            mCamera = Camera.open();
            isPreviewRunning = true;
            Camera.Parameters p = mCamera.getParameters();
            Camera.Size size = p.getPreviewSize();
            width = size.width;
            height = size.height;
            p.setPreviewFormat(ImageFormat.NV21);
            CameraFormat.ShowSupportedCameraFormats(p);
            mCamera.setParameters(p);

            rgbints = new int[width * height];

            // try { mCamera.setPreviewDisplay(holder); } catch (IOException e)
            // { Log.e("Camera", "mCamera.setPreviewDisplay(holder);"); }

            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        synchronized (this)
        {
            try
            {
                if (mCamera != null)
                {
                    //mHolder.removeCallback(this);
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    isPreviewRunning = false;
                    mCamera.release();
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        if (!isPreviewRunning)
            return;

        Canvas canvas = null;

        if (mHolder == null)
        {
            return;
        }

        try
        {
            synchronized (mHolder)
            {
                canvas = mHolder.lockCanvas(null);
                int canvasWidth = canvas.getWidth();
                int canvasHeight = canvas.getHeight();

                ImageFilters.YUVtoRGB(rgbints, data, width, height);
                ImageFilters.Emboss(rgbints, rgbints, width, height);

                // draw the decoded image, centered on canvas
                canvas.drawBitmap(rgbints, 0, width, canvasWidth - ((width + canvasWidth) >> 1),
                                   canvasHeight - ((height + canvasHeight) >> 1), width, height, false, null);

                // use some color filter
                //canvas.drawColor(mMultiplyColor, PorterDuff.Mode.MULTIPLY);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (canvas != null)
            {
                mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
}