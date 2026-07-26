package br.com.itisoft.a2fast.otp;

/**
 * RFC 4648 Base32 encoding compatible with Otp.NET Base32Encoding.
 */
public final class Base32Encoding {

    private Base32Encoding() {
    }

    public static byte[] toBytes(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("input");
        }

        input = input.trim().replace("=", "").replace(" ", "").toUpperCase();
        int byteCount = input.length() * 5 / 8;
        byte[] returnArray = new byte[byteCount];

        byte curByte = 0;
        int bitsRemaining = 8;
        int arrayIndex = 0;

        for (int i = 0; i < input.length(); i++) {
            int cValue = charToValue(input.charAt(i));

            if (bitsRemaining > 5) {
                int mask = cValue << (bitsRemaining - 5);
                curByte = (byte) (curByte | mask);
                bitsRemaining -= 5;
            } else {
                int mask = cValue >> (5 - bitsRemaining);
                curByte = (byte) (curByte | mask);
                returnArray[arrayIndex++] = curByte;
                curByte = (byte) (cValue << (3 + bitsRemaining));
                bitsRemaining += 3;
            }
        }

        if (arrayIndex != byteCount) {
            returnArray[arrayIndex] = curByte;
        }

        return returnArray;
    }

    public static String toBase32String(byte[] input) {
        if (input == null || input.length == 0) {
            throw new IllegalArgumentException("input");
        }

        int charCount = (int) Math.ceil(input.length / 5.0) * 8;
        char[] returnArray = new char[charCount];

        byte nextChar = 0;
        int bitsRemaining = 5;
        int arrayIndex = 0;

        for (byte b : input) {
            nextChar = (byte) (nextChar | (b >> (8 - bitsRemaining)));
            returnArray[arrayIndex++] = valueToChar(nextChar);

            if (bitsRemaining < 4) {
                nextChar = (byte) ((b >> (3 - bitsRemaining)) & 31);
                returnArray[arrayIndex++] = valueToChar(nextChar);
                bitsRemaining += 5;
            }

            bitsRemaining -= 3;
            nextChar = (byte) ((b << bitsRemaining) & 31);
        }

        if (arrayIndex != charCount) {
            returnArray[arrayIndex++] = valueToChar(nextChar);
            while (arrayIndex != charCount) {
                returnArray[arrayIndex++] = '=';
            }
        }

        return new String(returnArray);
    }

    private static int charToValue(char c) {
        int value = c;
        if (value < 91 && value > 64) {
            return value - 65;
        }
        if (value < 56 && value > 49) {
            return value - 24;
        }
        if (value < 123 && value > 96) {
            return value - 97;
        }
        throw new IllegalArgumentException("Character is not a Base32 character: " + c);
    }

    private static char valueToChar(byte b) {
        if (b < 26) {
            return (char) (b + 65);
        }
        if (b < 32) {
            return (char) (b + 24);
        }
        throw new IllegalArgumentException("Byte is not a Base32 value.");
    }
}
