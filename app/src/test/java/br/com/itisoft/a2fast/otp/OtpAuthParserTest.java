package br.com.itisoft.a2fast.otp;

import org.junit.Test;

import java.util.Map;

import br.com.itisoft.a2fast.model.TwoFACodeModel;
import br.com.itisoft.a2fast.parser.OtpAuthParser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OtpAuthParserTest {

    @Test
    public void parsesStandardTotpUri() {
        String uri = "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA1&digits=6&period=30";
        Map<String, String> params = OtpAuthParser.parse(uri);
        assertEquals("totp", params.get("auth"));
        assertEquals("JBSWY3DPEHPK3PXP", params.get("secret"));

        TwoFACodeModel model = OtpAuthParser.toAccount(uri);
        assertEquals("GitHub", model.Label);
        assertTrue(model.Issuer.contains("GitHub") || model.Issuer.contains("user@example.com"));
        assertArrayEquals(Base32Encoding.toBytes("JBSWY3DPEHPK3PXP"), model.SecretByteArray);
        assertEquals(6, model.TotpSize);
        assertEquals(30, model.Period);
    }
}
