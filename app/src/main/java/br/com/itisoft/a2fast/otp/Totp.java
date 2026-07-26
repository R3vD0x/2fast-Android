package br.com.itisoft.a2fast.otp;

import java.nio.ByteBuffer;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * RFC 6238 TOTP implementation compatible with Otp.NET.
 */
public final class Totp {

    private final byte[] secretKey;
    private final int step;
    private final OtpHashMode hashMode;
    private final int totpSize;

    public Totp(byte[] secretKey, int step, OtpHashMode hashMode, int totpSize) {
        if (secretKey == null || secretKey.length == 0) {
            throw new IllegalArgumentException("secretKey empty");
        }
        if (step <= 0 || totpSize <= 0 || totpSize > 10) {
            throw new IllegalArgumentException("Invalid TOTP parameters");
        }
        this.secretKey = secretKey.clone();
        this.step = step;
        this.hashMode = hashMode == null ? OtpHashMode.Sha1 : hashMode;
        this.totpSize = totpSize;
    }

    public String computeTotp(long unixSeconds) {
        long window = unixSeconds / step;
        return digits(calculateOtp(window), totpSize);
    }

    public String computeTotp() {
        return computeTotp(System.currentTimeMillis() / 1000L);
    }

    public int remainingSeconds(long unixSeconds) {
        return step - (int) (unixSeconds % step);
    }

    public int remainingSeconds() {
        return remainingSeconds(System.currentTimeMillis() / 1000L);
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

    private static String digits(int input, int digitCount) {
        int truncated = input % (int) Math.pow(10, digitCount);
        return String.format(Locale.US, "%0" + digitCount + "d", truncated);
    }
}
