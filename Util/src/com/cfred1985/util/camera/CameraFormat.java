package com.cfred1985.util.camera;

import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;

import java.util.List;

public class CameraFormat
{
    private static final String TAG = "CameraFormat";

    public static void ShowSupportedCameraFormats(Camera.Parameters p)
    {
        List<Integer> supportedPictureFormats = p.getSupportedPreviewFormats();
        Log.d(TAG, "preview format:" + CameraFormatIntToString(p.getPreviewFormat()));
        for (Integer x : supportedPictureFormats)
        {
            Log.d(TAG, "supported format: " + CameraFormatIntToString(x.intValue()));
        }
    }

    public static String CameraFormatIntToString(int format)
    {
        switch (format)
        {
            case PixelFormat.JPEG:
                return "JPEG";
            case PixelFormat.YCbCr_420_SP:
                return "NV21";
            case PixelFormat.YCbCr_422_I:
                return "YUY2";
            case PixelFormat.YCbCr_422_SP:
                return "NV16";
            case PixelFormat.RGB_565:
                return "RGB_565";
            default:
                return "Unknown:" + format;
        }
    }
}
