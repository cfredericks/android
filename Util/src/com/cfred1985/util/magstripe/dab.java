package com.cfred1985.util.magstripe;

import java.util.ArrayList;
import java.util.List;

import com.cfred1985.util.magstripe.dmsb.LogLevel;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

/**
 * Decode Aiken Biphase
 */
public class dab
{
    private final static String TAG = "DAB";

    private static int SAMPLE_RATE = 192000; /* default sample rate (hz) */
    private static int SILENCE_THRES = 5000; /* initial silence threshold */

    private static int AUTO_THRES = 30; /* pct of highest value to set silence_thres to */
    private static int END_LENGTH = 200; /* msec of silence to determine end of sample */
    private static int FREQ_THRES = 60; /* frequency threshold (pct) */
    private static int MAX_TERM = 60; /* sec before termination of print_max_level() */

    private static AudioRecord recorder;

    /********** dsp functions **********/

    /**
     * Prints the maximum dsp level to aid in setting the silence threshold [fd] file descriptor to read from [sample_rate] sample rate of
     * device
     */
    public static void print_max_level(AudioRecord record)
    {
        int bufferSize = AudioRecord.getMinBufferSize(record.getSampleRate(), record.getChannelConfiguration(), record.getAudioFormat());

        short[] buf = new short[bufferSize / 2];
        short last = 0;

        Logg(LogLevel.INFO, "Terminating after " + MAX_TERM + " seconds...");

        int samples_read = 0;
        record.startRecording();
        while (samples_read < record.getSampleRate() * MAX_TERM)
        {
            // Read sound sample
            samples_read += record.read(buf, 0, bufferSize);

            // Loop through all samples
            for (int i = 0; i < buf.length; i++)
            {
                // Absolute value
                if (buf[i] < 0)
                    buf[i] = (short) (-buf[i]);

                // Print if highest level
                if (buf[i] > last)
                {
                    Logg(LogLevel.INFO, "Maximum level: " + buf[i]);
                    last = buf[i];
                }
            }
        }
        record.stop();
    }

    /**
     * Finds the maximum value in sample* global ** [sample] sample [sample_size] number of frames in sample
     */
    public static short evaluate_max(Short[] samples)
    {
        int i;
        short max = 0;

        for (i = 0; i < samples.length; i++)
        {
            if (samples[i] > max)
                max = samples[i];
        }

        return max;
    }

    /**
     * Pauses until the dsp level is above the silence threshold [fd] file descriptor to read from [silence_thres] silence threshold
     */
    public static void silence_pause(AudioRecord record, int silence_thres)
    {
        short[] buf = new short[]
                { 0 };
        buf[0] = 0;

        // Loop while silent
        record.startRecording();
        while (buf[0] < silence_thres)
        {
            // Read sound sample
            record.read(buf, 0, 2);

            // Absolute value
            if (buf[0] < 0)
                buf[0] = (short) (-buf[0]);
        }
        record.stop();
    }

    /**
     * Gets a sample, terminating when the input goes below the silence threshold [fd] file descriptor to read from [sample_rate] sample
     * rate of device [silence_thres] silence threshold* global ** [sample] sample [sample_size] number of frames in sample
     */
    public static Short[] get_dsp(AudioRecord record, int silence_thres)
    {
        int bufferSize = AudioRecord.getMinBufferSize(record.getSampleRate(), record.getChannelConfiguration(), record.getAudioFormat());

        Boolean eos = false;
        short[] buf = new short[bufferSize];
        List<Short> samples = new ArrayList<Short>();

        // Wait for sample
        silence_pause(record, silence_thres);

        record.startRecording();
        while (!eos)
        {
            // Fill buffer
            int sample_size = record.read(buf, 0, bufferSize);
            for (short s : buf)
                samples.add(s);

            // Check for silence
            if (sample_size > (record.getSampleRate() * END_LENGTH) / 1000)
            {
                eos = true;
                for (int i = 0; i < (record.getSampleRate() * END_LENGTH) / 1000; i++)
                {
                    // Absolute value
                    if (buf[i] < 0)
                        buf[i] = (short) (-buf[i]);
                    // If we have a sample greater than the threshold, keep collecting samples
                    if (buf[i] > silence_thres)
                        eos = false;
                }
            }
            else
            {
                eos = false;
            }
        }
        record.stop();

        return samples.toArray(new Short[0]);
    }

    /********** end dsp functions **********/

    /**
     * Decodes aiken biphase and prints binary [freq_thres] frequency threshold* global ** [sample] sample [sample_size] number of frames in
     * sample
     */
    public static void decode_aiken_biphase(Short[] samples, int freq_thres, int silence_thres)
    {
        List<Integer> peaks = new ArrayList<Integer>();

        // Absolute value
        for (int i = 0; i < samples.length; i++)
        {
            if (samples[i] < 0)
                samples[i] = (short) (-samples[i]);
        }

        // Store peak differences
        int i = 0;
        int peak = 0;
        while (i < samples.length)
        {
            // Old peak value
            int ppeak = peak;

            // Skip silence until next peak
            while (i < samples.length && samples[i] <= silence_thres) i++;

            // Find highest sample to mark the peak
            peak = 0;
            while (i < samples.length && samples[i] > silence_thres)
            {
                if (samples[i] > samples[peak]) peak = i;
                i++;
            }
            if (peak - ppeak > 0)
            {
                peaks.add(peak - ppeak);
            }
        }

        // Decode aiken biphase allowing for frequency deviation based on freq_thres

        // Ignore first two peaks and last peak
        if (peaks.size() < 2)
        {
            Logg(LogLevel.ERROR, "*** Error: No data detected\n");
            System.exit(1);
        }
        int zerobl = peaks.get(2);
        for (i = 2; i < peaks.size() - 1; i++)
        {
            // Deltas for whether we have a 1 or a 0
            int deltaFreq1 = freq_thres * (zerobl / 2) / 100;
            int deltaFreq0 = freq_thres * zerobl / 100;

            if (peaks.get(i) < (zerobl / 2 + deltaFreq1) &&
                    peaks.get(i) > (zerobl / 2 - deltaFreq1))
            {
                if (peaks.get(i + 1) < (zerobl / 2 + deltaFreq1) &&
                        peaks.get(i + 1) > (zerobl / 2 - deltaFreq1))
                {
                    System.out.print("1");
                    zerobl = peaks.get(i) * 2;
                    i++;
                }
            }
            else if (peaks.get(i) < (zerobl + deltaFreq0) && peaks.get(i) > (zerobl - deltaFreq0))
            {
                System.out.print("0");
                zerobl = peaks.get(i);
            }
        }
        System.out.println();
    }

    public static void main(String args[])
    {
        // Configuration
        int auto_thres = AUTO_THRES;
        int sample_rate = SAMPLE_RATE;
        int silence_thres = SILENCE_THRES;

        Boolean max_level = false;

        // Initialize AudioRecord object
        int bufferSize = AudioRecord.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        /* show user maximum dsp level */
        if (max_level)
        {
            print_max_level(recorder);
            return;
        }

        /* silence_thres sanity check */
        if (silence_thres == 0)
        {
            Logg(LogLevel.ERROR, "*** Error: Invalid silence threshold\n");
            System.exit(1);
        }

        // Read sample
        Short[] samples;
        // if (use_sndfile)
        // {
        // buffer = get_sndfile(sndfile);
        // }
        // else
        {
            Logg(LogLevel.INFO, "*** Waiting for sample...\n");
            samples = get_dsp(recorder, silence_thres);
        }

        // Automatically set threshold
        if (auto_thres != 0)
        {
            silence_thres = auto_thres * evaluate_max(samples) / 100;
        }

        // Print silence threshold
        Logg(LogLevel.INFO, "*** Silence threshold: " + silence_thres + " (" + auto_thres + "% of max)\n");

        // Decode aiken biphase
        decode_aiken_biphase(samples, FREQ_THRES, silence_thres);

        // CLose sound device
        if (recorder != null)
        {
            if (recorder.getState() == AudioRecord.RECORDSTATE_RECORDING)
                recorder.stop();
            recorder.release();
        }
    }

    private static void Logg(LogLevel level, String msg)
    {
        // System.out.println(msg)
        switch (level)
        {
            case INFO:
                Log.i(TAG, msg);
                break;
            case WARNING:
                Log.w(TAG, msg);
            case ERROR:
                Log.e(TAG, msg);
            case FATAL:
                Log.wtf(TAG, msg);
            default:
                System.out.println(msg);
        }
    }
}
