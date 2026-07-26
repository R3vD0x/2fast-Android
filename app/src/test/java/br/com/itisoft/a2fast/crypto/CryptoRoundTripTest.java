package br.com.itisoft.a2fast.crypto;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import br.com.itisoft.a2fast.model.DatafileModel;
import br.com.itisoft.a2fast.model.TwoFACodeModel;
import br.com.itisoft.a2fast.otp.Base32Encoding;
import br.com.itisoft.a2fast.otp.OtpHashMode;
import br.com.itisoft.a2fast.serialization.DatafileSerializer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CryptoRoundTripTest {

    @Test
    public void encryptDecryptStringRoundTrip() {
        byte[] key = CryptoService.createByteArrayKeyV2("test-password".getBytes(StandardCharsets.UTF_8));
        byte[] iv = CryptoService.generateRandomIv();
        CryptoService.initialize(key, iv);
        try {
            String cipher = CryptoService.encrypt("GitHub");
            assertEquals("GitHub", CryptoService.decryptToString(cipher));
        } finally {
            CryptoService.clear();
        }
    }

    @Test
    public void datafileSerializeDecryptRoundTrip() {
        String password = "correct horse battery";
        byte[] key = CryptoService.createByteArrayKeyV2(password.getBytes(StandardCharsets.UTF_8));
        byte[] iv = CryptoService.generateRandomIv();

        TwoFACodeModel account = new TwoFACodeModel();
        account.Label = "GitHub";
        account.Issuer = "GitHub";
        account.SecretByteArray = Base32Encoding.toBytes("JBSWY3DPEHPK3PXP");
        account.HashMode = OtpHashMode.Sha1;
        account.Period = 30;
        account.TotpSize = 6;
        account.OTPType = "totp";

        DatafileModel model = new DatafileModel();
        model.IV = iv;
        model.Version = 2;
        model.Collection.add(account);

        String json = DatafileSerializer.serializeEncrypt(model, key, iv);
        DatafileModel meta = DatafileSerializer.parseMeta(json);
        assertNotNull(meta.IV);
        assertEquals(2, meta.Version);

        DatafileModel restored = DatafileSerializer.decryptAndParse(json, key, meta.IV);
        assertEquals(1, restored.Collection.size());
        TwoFACodeModel restoredAccount = restored.Collection.get(0);
        assertEquals("GitHub", restoredAccount.Label);
        assertEquals("GitHub", restoredAccount.Issuer);
        assertArrayEquals(account.SecretByteArray, restoredAccount.SecretByteArray);
        assertEquals(OtpHashMode.Sha1, restoredAccount.HashMode);
    }

    @Test
    public void wrongPasswordFailsDecrypt() {
        byte[] goodKey = CryptoService.createByteArrayKeyV2("good".getBytes(StandardCharsets.UTF_8));
        byte[] badKey = CryptoService.createByteArrayKeyV2("bad".getBytes(StandardCharsets.UTF_8));
        byte[] iv = CryptoService.generateRandomIv();

        TwoFACodeModel account = new TwoFACodeModel();
        account.Label = "Secret";
        account.OTPType = "totp";
        account.SecretByteArray = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        DatafileModel model = new DatafileModel();
        model.IV = iv;
        model.Version = 2;
        model.Collection.add(account);

        String json = DatafileSerializer.serializeEncrypt(model, goodKey, iv);
        try {
            DatafileSerializer.decryptAndParse(json, badKey, iv, true);
            throw new AssertionError("Expected decrypt to fail with wrong password");
        } catch (RuntimeException expected) {
            // ok
        }
    }
}
