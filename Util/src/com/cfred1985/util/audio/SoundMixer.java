package com.cfred1985.util.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class SoundMixer
{
    private static final String TAG = "SoundMixer";

    /**
     * Plays a mixture of two frequencies through the sound card
     */
    public static AudioTrack PlaySound(double duration, int sample_rate, double freq1, double freq2)
    {
        // Ensure we have valid frequencies
        if (freq1 < 0)
            freq1 = freq2;
        if (freq2 < 0)
            freq2 = freq1;
        if (freq1 < 0 || freq2 < 0)
            return null;

        int num_samples = (int)(duration * sample_rate);

        Log.d(TAG, "Playing DTMF: " + freq1 + "Hz + " + freq2 + "Hz for " + duration + "sec");

        // Generate audio samples
        double[] samples = new double[num_samples];
        double mult1 = 2 * Math.PI * freq1 / sample_rate;
        double mult2 = 2 * Math.PI * freq2 / sample_rate;
        for (int i = 0; i < num_samples; i++)
        {
            samples[i] = (Math.sin(mult1 * i) + Math.sin(mult2 * i)) / 2.0;
        }

        // Convert to 16-bit PCM sound array
        // NOTE: assumes the sample buffer is normalized
        byte[] generatedSound = new byte[2 * num_samples];
        int idx = 0;
        for (double dVal : samples)
        {
            short val = (short)(dVal * 32767);
            generatedSound[idx++] = (byte)(val & 0x00ff);
            generatedSound[idx++] = (byte)((val & 0xff00) >>> 8);
        }

        // Play sound through the speakers
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, sample_rate, AudioFormat.CHANNEL_OUT_MONO,
                               AudioFormat.ENCODING_PCM_16BIT, 2 * generatedSound.length, AudioTrack.MODE_STATIC);

        track.write(generatedSound, 0, generatedSound.length);
        track.play();

        return track;
    }
}
