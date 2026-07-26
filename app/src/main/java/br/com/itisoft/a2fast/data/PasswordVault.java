package br.com.itisoft.a2fast.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Stores datafile passwords in EncryptedSharedPreferences.
 * Keys: file id (preferred) and legacy SHA-512 hash for migration.
 */
public final class PasswordVault {

    private static final String PREFS = "project2fa_vault";
    private static final String CONTAINER = "Project2FA";

    private final SharedPreferences prefs;

    public PasswordVault(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to open password vault", e);
        }
    }

    public void writePasswordForFile(String fileId, String passwordHash, String password) {
        SharedPreferences.Editor editor = prefs.edit();
        if (fileId != null && !fileId.isEmpty()) {
            editor.putString(fileKey(fileId), password);
        }
        if (passwordHash != null && !passwordHash.isEmpty()) {
            editor.putString(hashKey(passwordHash), password);
        }
        editor.apply();
    }

    public String readPasswordForFile(String fileId, String passwordHash) {
        if (fileId != null && !fileId.isEmpty()) {
            String byId = prefs.getString(fileKey(fileId), null);
            if (byId != null) {
                return byId;
            }
        }
        if (passwordHash != null && !passwordHash.isEmpty()) {
            return prefs.getString(hashKey(passwordHash), null);
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

    private static String fileKey(String fileId) {
        return CONTAINER + ":file:" + fileId;
    }

    private static String hashKey(String passwordHash) {
        return CONTAINER + ":" + passwordHash;
    }
}
