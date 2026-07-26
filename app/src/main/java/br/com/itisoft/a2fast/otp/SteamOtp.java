package br.com.itisoft.a2fast.otp;

import java.nio.ByteBuffer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Steam Guard OTP variant compatible with Otp.NET Steam class.
 */
public final class SteamOtp {

    private static final String STEAM_CHARS = "23456789BCDFGHJKMNPQRTVWXY";

    private final byte[] secretKey;
    private final int step;
    private final OtpHashMode hashMode;
    private final int totpSize;

    public SteamOtp(byte[] secretKey, int step, OtpHashMode hashMode, int totpSize) {
        if (secretKey == null || secretKey.length == 0) {
            throw new IllegalArgumentException("secretKey empty");
        }
        this.secretKey = secretKey.clone();
        this.step = step <= 0 ? 30 : step;
        this.hashMode = hashMode == null ? OtpHashMode.Sha1 : hashMode;
        this.totpSize = totpSize <= 0 ? 5 : totpSize;
    }

    public String computeTotp(long unixSeconds) {
        long window = unixSeconds / step;
        return steamDigits(calculateOtp(window), totpSize);
    }

    public String computeTotp() {
        return computeTotp(System.currentTimeMillis() / 1000L);
    }

    public int remainingSeconds(long unixSeconds) {
        return step - (int) (unixSeconds % step);
    }

    private int calculateOtp(long counter) {
        byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
        try {
            Mac mac = Mac.getInstance(hashMode.macAlgorithm());
            mac.init(new SecretKeySpec(secretKey, hashMode.macAlgorithm()));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            return ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    private static String steamDigits(int input, int digitCount) {
        StringBuilder otp = new StringBuilder();
        int fullCode = input & 0x7fffffff;
        for (int i = 0; i < digitCount; i++) {
            otp.append(STEAM_CHARS.charAt(fullCode % STEAM_CHARS.length()));
            fullCode = (int) Math.floor(fullCode / (double) STEAM_CHARS.length());
        }
        return otp.toString();
    }
}
