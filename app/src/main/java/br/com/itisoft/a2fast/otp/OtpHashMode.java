package br.com.itisoft.a2fast.otp;

/**
 * HMAC algorithm for OTP generation. Ordinal matches Otp.NET for JSON compatibility.
 */
public enum OtpHashMode {
    Sha1,
    Sha256,
    Sha512;

    public String macAlgorithm() {
        switch (this) {
            case Sha256:
                return "HmacSHA256";
            case Sha512:
                return "HmacSHA512";
            case Sha1:
            default:
                return "HmacSHA1";
        }
    }

    public static OtpHashMode fromOrdinal(int value) {
        OtpHashMode[] values = values();
        if (value < 0 || value >= values.length) {
            return Sha1;
        }
        return values[value];
    }

    public static OtpHashMode fromName(String name) {
        if (name == null || name.isEmpty()) {
            return Sha1;
        }
        switch (name.trim().toUpperCase()) {
            case "SHA256":
            case "SHA-256":
                return Sha256;
            case "SHA512":
            case "SHA-512":
                return Sha512;
            default:
                return Sha1;
        }
    }
}
