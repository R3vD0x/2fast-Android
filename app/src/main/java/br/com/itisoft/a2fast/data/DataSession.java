package br.com.itisoft.a2fast.data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.itisoft.a2fast.crypto.CryptoService;
import br.com.itisoft.a2fast.model.DatafileModel;
import br.com.itisoft.a2fast.model.TwoFACodeModel;
import br.com.itisoft.a2fast.otp.SteamOtp;
import br.com.itisoft.a2fast.otp.Totp;
import br.com.itisoft.a2fast.serialization.DatafileSerializer;

/**
 * In-memory session holding the unlocked datafile and helpers to load/save/generate codes.
 */
public final class DataSession {

    private static DataSession instance;

    private final List<TwoFACodeModel> accounts = new ArrayList<>();
    private String password;
    private String activeFileId;
    private int version = 2;
    private byte[] salt;
    private boolean loaded;

    public static synchronized DataSession get() {
        if (instance == null) {
            instance = new DataSession();
        }
        return instance;
    }

    public synchronized void unlock(String password, String json) {
        unlock(password, json, null);
    }

    public synchronized void unlock(String password, String json, String fileId) {
        DatafileModel meta = DatafileSerializer.parseMeta(json);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] key = CryptoService.deriveKey(passwordBytes, meta.Version, meta.Salt);
        DatafileModel data = DatafileSerializer.decryptAndParse(json, key, meta.IV, true);

        this.password = password;
        this.activeFileId = fileId;
        this.version = 2;
        this.salt = null;
        this.accounts.clear();
        if (data.Collection != null) {
            this.accounts.addAll(data.Collection);
        }
        this.loaded = true;
        refreshCodes();
    }

    public synchronized String createEmptyEncryptedFile(String password) {
        return createEmptyEncryptedFile(password, null);
    }

    public synchronized String createEmptyEncryptedFile(String password, String fileId) {
        this.password = password;
        this.activeFileId = fileId;
        this.version = 2;
        this.salt = null;
        this.accounts.clear();
        this.loaded = true;
        return serializeCurrent();
    }

    public synchronized String getActiveFileId() {
        return activeFileId;
    }

    public synchronized void setActiveFileId(String fileId) {
        this.activeFileId = fileId;
    }

    public synchronized String serializeCurrent() {
        ensureLoaded();
        DatafileModel model = new DatafileModel();
        model.IV = CryptoService.generateRandomIv();
        model.Version = version;
        model.Salt = salt;
        model.Collection = new ArrayList<>(accounts);
        model.GlobalCategories = new ArrayList<>();

        byte[] key = CryptoService.deriveKey(
                password.getBytes(StandardCharsets.UTF_8),
                model.Version,
                model.Salt
        );
        return DatafileSerializer.serializeEncrypt(model, key, model.IV);
    }

    public synchronized boolean verifyPassword(String candidate, String json) {
        try {
            DatafileModel meta = DatafileSerializer.parseMeta(json);
            if (meta.IV == null) {
                return false;
            }
            if (meta.Collection == null || meta.Collection.isEmpty()) {
                return candidate != null && !candidate.isEmpty();
            }
            byte[] key = CryptoService.deriveKey(
                    candidate.getBytes(StandardCharsets.UTF_8),
                    meta.Version,
                    meta.Salt
            );
            DatafileSerializer.decryptAndParse(json, key, meta.IV, true);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Empty desktop placeholders should be encrypted once on first successful open. */
    public synchronized boolean needsPlaceholderEncryption(String json) {
        DatafileModel meta = DatafileSerializer.parseMeta(json);
        return meta.Collection == null || meta.Collection.isEmpty();
    }

    public synchronized void addAccount(TwoFACodeModel account) {
        ensureLoaded();
        accounts.add(account);
        refreshCodes();
    }

    public synchronized void removeAccount(TwoFACodeModel account) {
        ensureLoaded();
        accounts.remove(account);
    }

    public synchronized List<TwoFACodeModel> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public synchronized void refreshCodes() {
        long now = System.currentTimeMillis() / 1000L;
        for (TwoFACodeModel account : accounts) {
            if (account.SecretByteArray == null || account.SecretByteArray.length == 0) {
                account.TwoFACode = "Error";
                continue;
            }
            try {
                if ("steam".equalsIgnoreCase(account.OTPType)) {
                    SteamOtp otp = new SteamOtp(
                            account.SecretByteArray,
                            account.Period,
                            account.HashMode,
                            account.TotpSize
                    );
                    account.TwoFACode = otp.computeTotp(now);
                    account.Seconds = otp.remainingSeconds(now);
                } else {
                    Totp totp = new Totp(
                            account.SecretByteArray,
                            account.Period,
                            account.HashMode,
                            account.TotpSize
                    );
                    account.TwoFACode = totp.computeTotp(now);
                    account.Seconds = totp.remainingSeconds(now);
                }
            } catch (RuntimeException e) {
                account.TwoFACode = "Error";
            }
        }
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized void lock() {
        password = null;
        activeFileId = null;
        accounts.clear();
        loaded = false;
    }

    private void ensureLoaded() {
        if (!loaded || password == null) {
            throw new IllegalStateException("Datafile is not unlocked");
        }
    }
}
