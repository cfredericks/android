package com.cfred1985.util.graphics;

import android.graphics.Bitmap;

import java.nio.IntBuffer;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ImageFilters
{
    public static void DetectEdges(int[] output, int[] data, int width, int height)
    {
        int filterWidth = 3;
        int filterHeight = 3;
        double[][] filter =
                {
                        {-1, -1, -1},
                        {-1,  8, -1},
                        {-1, -1, -1}
                };

        Convolve(output, data, width, height, filter, filterWidth, filterHeight, 1.0, 0.0);
    }

    public static void Blur(int[] output, int[] data, int width, int height)
    {
        int filterWidth = 5;
        int filterHeight = 5;
        double[][] filter =
                {
                        {0, 0, 1, 0, 0},
                        {0, 1, 1, 1, 0},
                        {1, 1, 1, 1, 1},
                        {0, 1, 1, 1, 0},
                        {0, 0, 1, 0, 0},
                };

        Convolve(output, data, width, height, filter, filterWidth, filterHeight, 1.0 / 13.0, 0.0);
    }

    public static void Emboss(int[] output, int[] data, int width, int height)
    {
        int filterWidth = 3;
        int filterHeight = 3;
        double[][] filter =
                {
                        {-1, -1,  0},
                        {-1,  0,  1},
                        {0,  1,  1}
                };

        Convolve(output, data, width, height, filter, filterWidth, filterHeight, 1.0, 128.0);
        Grayscale(output, data, width, height);
    }

    public static void Convolve(int[] output, int[] data, int width, int height, double[][] filter, int filterWidth,
                                int filterHeight,
                                double factor, double bias)
    {
        // apply the filter
        for (int i = 0; i < data.length; i++)
        {
            int x = i % width;
            int y = i / width;

            double red = 0.0, green = 0.0, blue = 0.0;

            //multiply every value of the filter with corresponding image pixel
            int halfFilterWidth = filterWidth / 2;
            int halfFilterHeight = filterHeight / 2;
            for(int filterX = 0; filterX < filterWidth; filterX++)
                for(int filterY = 0; filterY < filterHeight; filterY++)
                {
                    int imageX = (x - halfFilterWidth + filterX + width) % width;
                    int imageY = (y - halfFilterHeight + filterY + height) % height;

                    int val = data[imageY * width + imageX];
                    int B = (val >> 16) & 0x000000ff;
                    int G = (val >> 8) & 0x000000ff;
                    int R = val & 0x000000ff;

                    red += R * filter[filterX][filterY];
                    green += G * filter[filterX][filterY];
                    blue += B * filter[filterX][filterY];
                }

            //truncate values smaller than zero and larger than 255
            int R = min(max((int)(factor * red + bias), 0), 255);
            int G = min(max((int)(factor * green + bias), 0), 255);
            int B = min(max((int)(factor * blue + bias), 0), 255);
            output[i] = 0xff000000 + (R << 16) + (G << 8) + B;
        }
    }

    public static void Grayscale(int[] output, int[] data, int width, int height)
    {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < data.length; i++)
        {
            int val = data[i];

            int B = (val >> 16) & 0x000000ff;
            int G = (val >> 8) & 0x000000ff;
            int R = val & 0x000000ff;

            if (R + B + G > max) max = R + B + G;
        }
        for (int i = 0; i < data.length; i++)
        {
            int val = data[i];

            int A = (val >> 24) & 0x000000ff;
            int B = (val >> 16) & 0x000000ff;
            int G = (val >> 8) & 0x000000ff;
            int R = val & 0x000000ff;

            int normVal = (int)(((double)(R + B + G) / max) * 255);

            output[i] = (A << 24) + (normVal << 16) + (normVal << 8) + normVal;
        }
    }

    public static Bitmap YUVToBitmap(byte[] data, int imageWidth, int imageHeight)
    {
        // the bitmap we want to fill with the image
        Bitmap bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        int numPixels = imageWidth * imageHeight;

        // the buffer we fill up which we then fill the bitmap with
        IntBuffer intBuffer = IntBuffer.allocate(numPixels);
        // If you're reusing a buffer, next line imperative to refill from the start,
        // if not good practice
        intBuffer.position(0);

        // Set the alpha for the image: 0 is transparent, 255 fully opaque
        final byte alpha = (byte) 255;

        // Get each pixel, one at a time
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                // Get the Y value, stored in the first block of data
                // The logical "AND 0xff" is needed to deal with the signed issue
                int Y = data[y * imageWidth + x] & 0xff;

                // Get U and V values, stored after Y values, one per 2x2 block
                // of pixels, interleaved. Prepare them as floats with correct range
                // ready for calculation later.
                int yby2 = y / 2;
                float U = (float)(data[numPixels + x + yby2 * imageWidth] & 0xff) - 128.0f;
                float V = (float)(data[numPixels + x + 1 + yby2 * imageWidth] & 0xff) - 128.0f;
                // Do the YUV -> RGB conversion
                float Yf = 1.164f * ((float)Y) - 16.0f;
                int R = (int)(Yf + 1.596f * V);
                int G = (int)(Yf - 0.813f * V - 0.391f * U);
                int B = (int)(Yf            + 2.018f * U);

                // Clip rgb values to 0-255
                R = R < 0 ? 0 : R > 255 ? 255 : R;
                G = G < 0 ? 0 : G > 255 ? 255 : G;
                B = B < 0 ? 0 : B > 255 ? 255 : B;

                // Put that pixel in the buffer
                intBuffer.put(alpha*16777216 + R*65536 + G*256 + B);
            }
        }

        // Get buffer ready to be read
        intBuffer.flip();

        // Push the pixel information from the buffer onto the bitmap.
        bitmap.copyPixelsFromBuffer(intBuffer);
        return bitmap;
    }

    /**
     * Decodes YUV frame to a buffer which can be use to create a bitmap. use
     * this for OS < FROYO which has a native YUV decoder decode Y, U, and V
     * values on the YUV 420 buffer described as YCbCr_422_SP by Android
     *
     * @param out    the outgoing array of RGB bytes
     * @param fg     the incoming frame bytes
     * @param width  of source frame
     * @param height of source frame
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static void YUVtoRGB(int[] out, byte[] fg, int width, int height)
            throws NullPointerException, IllegalArgumentException
    {
        int sz = width * height;
        if (out == null)
            throw new NullPointerException("buffer out is null");
        if (out.length < sz)
            throw new IllegalArgumentException("buffer out size " + out.length + " < minimum " + sz);
        if (fg == null)
            throw new NullPointerException("buffer 'fg' is null");
        if (fg.length < sz)
            throw new IllegalArgumentException("buffer fg size " + fg.length + " < minimum " + sz * 3 / 2);
        int i, j;
        int Y, Cr = 0, Cb = 0;
        for (j = 0; j < height; j++)
        {
            int pixPtr = j * width;
            final int jDiv2 = j >> 1;
            for (i = 0; i < width; i++)
            {
                Y = fg[pixPtr];
                if (Y < 0)
                    Y += 255;
                if ((i & 0x1) != 1)
                {
                    final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
                    Cb = fg[cOff];
                    if (Cb < 0)
                        Cb += 127;
                    else
                        Cb -= 128;
                    Cr = fg[cOff + 1];
                    if (Cr < 0)
                        Cr += 127;
                    else
                        Cr -= 128;
                }
                int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                if (R < 0)
                    R = 0;
                else if (R > 255)
                    R = 255;
                int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
                if (G < 0)
                    G = 0;
                else if (G > 255)
                    G = 255;
                int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
                if (B < 0)
                    B = 0;
                else if (B > 255)
                    B = 255;
                out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
            }
        }

    }
}
