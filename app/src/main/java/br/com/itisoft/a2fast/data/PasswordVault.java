package br.com.itisoft.a2fast.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stores datafile passwords encrypted with an Android Keystore AES-GCM key.
 * Keys: file id (preferred) and legacy SHA-512 hash for migration.
 */
public final class PasswordVault {

    private static final String PREFS = "project2fa_vault_v2";
    private static final String LEGACY_PREFS = "project2fa_vault";
    private static final String CONTAINER = "Project2FA";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "project2fa_vault_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SharedPreferences prefs;
    private final SecretKey secretKey;

    public PasswordVault(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        context.deleteSharedPreferences(LEGACY_PREFS);

        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            secretKey = getOrCreateSecretKey();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to open password vault", e);
        }
    }

    public void writePasswordForFile(String fileId, String passwordHash, String password) {
        SharedPreferences.Editor editor = prefs.edit();
        if (fileId != null && !fileId.isEmpty()) {
            editor.putString(fileKey(fileId), encrypt(password));
        }
        if (passwordHash != null && !passwordHash.isEmpty()) {
            editor.putString(hashKey(passwordHash), encrypt(password));
        }
        editor.apply();
    }

    public String readPasswordForFile(String fileId, String passwordHash) {
        if (fileId != null && !fileId.isEmpty()) {
            String byId = decrypt(prefs.getString(fileKey(fileId), null));
            if (byId != null) {
                return byId;
            }
        }
        if (passwordHash != null && !passwordHash.isEmpty()) {
            return decrypt(prefs.getString(hashKey(passwordHash), null));
        }
        return null;
    }

    public void removePasswordForFile(String fileId, String passwordHash) {
        SharedPreferences.Editor editor = prefs.edit();
        if (fileId != null && !fileId.isEmpty()) {
            editor.remove(fileKey(fileId));
        }
        if (passwordHash != null && !passwordHash.isEmpty()) {
            editor.remove(hashKey(passwordHash));
        }
        editor.apply();
    }

    public void writePassword(String passwordHash, String password) {
        writePasswordForFile(null, passwordHash, password);
    }

    public String readPassword(String passwordHash) {
        return readPasswordForFile(null, passwordHash);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    private String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt vault value", e);
        }
    }

    private String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        try {
            byte[] payload = Base64.decode(encoded, Base64.NO_WRAP);
            if (payload.length <= IV_LENGTH) {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry =
                    (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            return entry.getSecretKey();
        }

        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return keyGenerator.generateKey();
    }

    private static String fileKey(String fileId) {
        return CONTAINER + ":file:" + fileId;
    }

    private static String hashKey(String passwordHash) {
        return CONTAINER + ":" + passwordHash;
    }
}
