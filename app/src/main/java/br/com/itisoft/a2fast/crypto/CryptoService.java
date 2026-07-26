package br.com.itisoft.a2fast.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-CBC field encryption and PBKDF2 key derivation compatible with the
 * desktop/Uno 2fast {@code CryptoService} and {@code .2fa} format.
 */
public final class CryptoService {

    private static final byte[] STATIC_SALT = new byte[]{
            14, (byte) 223, 35, (byte) 197, 93, (byte) 242, (byte) 239, 8
    };

    private static final String AES_TRANSFORM = "AES/CBC/PKCS5Padding";

    private static byte[] key;
    private static byte[] iv;

    private CryptoService() {
    }

    public static synchronized void initialize(byte[] keyBytes, byte[] ivBytes) {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("Key must be 32 bytes (256 bits) long.");
        }
        if (ivBytes == null || ivBytes.length != 16) {
            throw new IllegalArgumentException("IV must be 16 bytes (128 bits) long.");
        }
        key = Arrays.copyOf(keyBytes, keyBytes.length);
        iv = Arrays.copyOf(ivBytes, ivBytes.length);
    }

    public static synchronized boolean isInitialized() {
        return key != null && iv != null;
    }

    public static synchronized void clear() {
        if (key != null) {
            Arrays.fill(key, (byte) 0);
            key = null;
        }
        if (iv != null) {
            Arrays.fill(iv, (byte) 0);
            iv = null;
        }
    }

    public static synchronized String encrypt(String plainText) {
        byte[] bytes = plainText == null ? new byte[0] : plainText.getBytes(StandardCharsets.UTF_8);
        return encrypt(bytes);
    }

    public static synchronized String encrypt(byte[] bytes) {
        ensureInitialized();
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(bytes == null ? new byte[0] : bytes);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public static synchronized String decryptToString(String cipherText) {
        byte[] decrypted = decrypt(Base64.getDecoder().decode(cipherText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static synchronized byte[] decrypt(byte[] cipherBytes) {
        ensureInitialized();
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    public static byte[] createByteArrayKeyV1(byte[] secretByteArray) {
        return pbkdf2(secretByteArray, STATIC_SALT, 1000, 32, "HmacSHA1");
    }

    public static byte[] createByteArrayKeyV2(byte[] secretByteArray) {
        return pbkdf2(secretByteArray, STATIC_SALT, 25000, 32, "HmacSHA256");
    }

    public static byte[] createByteArrayKeyV3(byte[] secretByteArray, byte[] salt) {
        if (salt == null || salt.length < 16) {
            throw new IllegalArgumentException("Salt must be at least 16 bytes.");
        }
        return pbkdf2(secretByteArray, salt, 25000, 32, "HmacSHA256");
    }

    public static byte[] deriveKey(byte[] passwordUtf8, int version, byte[] salt) {
        if (version <= 1) {
            return createByteArrayKeyV1(passwordUtf8);
        }
        if (version >= 3 && salt != null && salt.length >= 16) {
            return createByteArrayKeyV3(passwordUtf8, salt);
        }
        return createByteArrayKeyV2(passwordUtf8);
    }

    public static byte[] generateRandomIv() {
        byte[] randomIv = new byte[16];
        new SecureRandom().nextBytes(randomIv);
        return randomIv;
    }

    public static byte[] generateRandomSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] createSha512ByteArrayHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String createStringHash(String input) {
        byte[] hash = createSha512ByteArrayHash(input);
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * RFC 2898 PBKDF2 using raw password bytes (matches .NET Rfc2898DeriveBytes(byte[])).
     */
    static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int keyLength, String macAlgorithm) {
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(new SecretKeySpec(password, macAlgorithm));
            int hashLength = mac.getMacLength();
            int blockCount = (keyLength + hashLength - 1) / hashLength;
            byte[] result = new byte[keyLength];
            byte[] block = new byte[4];
            int offset = 0;
            for (int i = 1; i <= blockCount; i++) {
                block[0] = (byte) (i >>> 24);
                block[1] = (byte) (i >>> 16);
                block[2] = (byte) (i >>> 8);
                block[3] = (byte) i;

                mac.update(salt);
                byte[] u = mac.doFinal(block);
                byte[] t = Arrays.copyOf(u, u.length);

                for (int j = 1; j < iterations; j++) {
                    u = mac.doFinal(u);
                    for (int k = 0; k < t.length; k++) {
                        t[k] ^= u[k];
                    }
                }

                int toCopy = Math.min(hashLength, keyLength - offset);
                System.arraycopy(t, 0, result, offset, toCopy);
                offset += toCopy;
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 failed", e);
        }
    }

    private static void ensureInitialized() {
        if (!isInitialized()) {
            throw new IllegalStateException("CryptoService is not initialized.");
        }
    }
}
