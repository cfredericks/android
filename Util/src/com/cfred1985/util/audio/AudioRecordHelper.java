package com.cfred1985.util.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecordHelper
{
    private static final String TAG = "AudioRecordHelper";

    /**
     * Finds an audio recording format that is available on the current platform
     */
    public static AudioRecord findAudioRecord()
    {
        int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };

        for (int rate : mSampleRates)
        {
            for (int audioFormat : new int[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT })
            {
                for (int channelConfig : new int[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.CHANNEL_CONFIGURATION_STEREO })
                {
                    try
                    {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE)
                        {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, rate + "Exception, keep trying.", e);
                    }
                }
            }
        }

        Log.w(TAG, "No appropriate audio format found");
        return null;
    }
}
