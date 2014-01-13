package com.cfred1985.util.magstripe;

import java.io.IOException;

import android.util.Log;

/**
 * Decodes (standard) Magnetic Stripe Binary
 */
public class dmsb
{
    private final static String TAG = "DMSB";

    public static String reverse_string(String string)
    {
        return new StringBuffer(string).reverse().toString();
    }

    public static String parse(String bitString, int byteSize)
    {
        StringBuilder decodedString = new StringBuilder();
        int lrc_start_pos, start_decode_pos;

        if (byteSize != 5 && byteSize != 7)
        {
            Logg(LogLevel.ERROR, "Only supporst 5-bites (ABA) and 7-bit (IATA) codes");
            return null;
        }

        String startSentinelBits = (byteSize == 5) ? "11010" : "1010001";
        String endSentinelBits   = (byteSize == 5) ? "11111" : "1111100";
        char startSentinel = (byteSize == 5) ? ';' : '%';
        char endSentinel = '?';

        // Initial condition is LRC of the start sentinel
        char[] lrc = startSentinelBits.toCharArray();
        int i, j;

        // Find start sentinel
        if ((start_decode_pos = bitString.indexOf(startSentinelBits)) < 0) { return null; }

        // Set start_decode to first bit (start of first byte) after start sentinel
        start_decode_pos += byteSize;

        // Find end sentinel
        if ((lrc_start_pos = bitString.indexOf(endSentinelBits)) < 0) { return null; }

        // Must be a multiple of byteSize
        while (((start_decode_pos - lrc_start_pos) % byteSize) != 0)
        {
            // Search for end sentinel again
            if ((lrc_start_pos = bitString.indexOf(endSentinelBits, ++lrc_start_pos)) < 0) { return null; }
        }

        String lrc_start = bitString.substring(lrc_start_pos, lrc_start_pos + byteSize);

        // Add start sentinel
        decodedString.append(startSentinel);

        // Decode each set of bits, check parity, check LRC, and add to decodedString
        String start_decode = bitString.substring(start_decode_pos, start_decode_pos + byteSize);
        while (start_decode_pos < lrc_start_pos)
        {
            // Check parity
            for (i = 0, j = 0; i < byteSize - 1; i++)
            {
                if (start_decode.charAt(i) == '1') j++;
            }
            if ((((j % 2) != 0) && start_decode.charAt(byteSize - 1) == '1') ||
                    (((j % 2) == 0) && start_decode.charAt(byteSize - 1) == '0'))
            {
                // Failed parity check
                return null;
            }

            // Generate ASCII value from bits
            // TODO(coreyf): This should be calculated and not hard-coded
            char asciichr = (byteSize == 5) ? (char)48 : (char)32;
            for (int k = 0; k < start_decode.length() - 1; k++)
                asciichr += start_decode.charAt(k) == '1' ? (int)Math.pow(2, k) : 0;

            // Add character to decoded string
            decodedString.append(asciichr);

            // Calculate LRC
            for (i = 0; i < byteSize - 1; i++)
                lrc[i] = (char)(lrc[i] ^ ((start_decode.charAt(i) == '1') ? 1 : 0));

            // Increment start_decode to next byte
            start_decode_pos += byteSize;
            start_decode = bitString.substring(start_decode_pos, start_decode_pos + byteSize);
        }

        // Add end sentinel
        decodedString.append(endSentinel);

        // Calculate CRC of end sentinel
        for (i = 0; i < byteSize - 1; i++)
            lrc[i] = (char)(lrc[i] ^ (startSentinelBits.charAt(i) == '1' ? 1 : 0));

        // Set LRC parity bit
        for (i = 0, j = 0; i < byteSize - 1; i++)
            if (lrc[i] != 0)
                j++;
        if ((j % 2) == 0)
            lrc[byteSize - 1] = 1;
        else
            lrc[byteSize - 1] = 0;

        // Check CRC
        for (i = 0; i < byteSize; i++)
        {
            // Failed CRC check
            if (((lrc[i] != 0) && lrc_start.charAt(i) == '0') ||
                    ((lrc[i] == 0) && lrc_start.charAt(i) == '1'))
            {
                return null;
            }
        }

        return decodedString.toString();
    }

    /********** parsing functions **********/

    /*
     * parse ABA format raw bits and return a pointer to the decoded string [bitstring] string to decode returns decoded string
     */
    public static String parse_ABA(String bitString)
    {
        return parse(bitString, 5);
    }

    /*
     * parse IATA format raw bits and return a pointer to the decoded string [bitstring] string to decode returns decoded string
     */
    public static String parse_IATA(String bitString)
    {
        return parse(bitString, 7);
    }

    public static String parse_bits(String bitString)
    {
        // Reverse bitstring
        String rbuf = reverse_string(bitString);
        String decoded_data;

        // Try ABA
        if ((decoded_data = parse_ABA(bitString)) != null)
        {
            Logg(LogLevel.INFO, "Success! ABA format detected:\n");
            return decoded_data;
        }

        // Try ABA - reversed
        if ((decoded_data = parse_ABA(rbuf)) != null)
        {
            Logg(LogLevel.INFO, "Success! ABA format (bits reversed) detected:\n");
            return decoded_data;
        }

        // Try IATA
        if ((decoded_data = parse_IATA(bitString)) != null)
        {
            Logg(LogLevel.INFO, "Success! IATA format detected:\n");
            return decoded_data;
        }

        // Try IATA - reversed
        if ((decoded_data = parse_IATA(rbuf)) != null)
        {
            Logg(LogLevel.INFO, "Success! IATA format (bits reversed) detected:\n");
            return decoded_data;
        }

        Logg(LogLevel.ERROR, "Detection failed. No suitable format found.");
        return null;
    }

    /********** end parsing functions **********/

    public static void main(String args[])
    {
        // Get string from stdin
        String buf = "";
        try
        {
            java.io.BufferedReader stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            buf = stdin.readLine();
        }
        catch (IOException e)
        {
            Logg(LogLevel.ERROR, "Could not read from stdin");
            System.exit(1);
        }

        String result = parse_bits(buf);
        if (result != null)
        {
            Logg(LogLevel.INFO, result);
        }
    }

    public enum LogLevel
    {
        NONE,
        INFO,
        WARNING,
        ERROR,
        FATAL
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
