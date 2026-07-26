package br.com.itisoft.a2fast.otp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * RFC 6238 appendix B test vectors (SHA1, secret = "12345678901234567890").
 */
public class TotpTest {

    private static final byte[] SECRET = "12345678901234567890".getBytes();

    @Test
    public void rfc6238Sha1Vectors() {
        Totp totp = new Totp(SECRET, 30, OtpHashMode.Sha1, 8);
        assertEquals("94287082", totp.computeTotp(59L));
        assertEquals("07081804", totp.computeTotp(1111111109L));
        assertEquals("14050471", totp.computeTotp(1111111111L));
        assertEquals("89005924", totp.computeTotp(1234567890L));
        assertEquals("69279037", totp.computeTotp(2000000000L));
        assertEquals("65353130", totp.computeTotp(20000000000L));
    }

    @Test
    public void remainingSecondsWrapsCorrectly() {
        Totp totp = new Totp(SECRET, 30, OtpHashMode.Sha1, 6);
        assertEquals(1, totp.remainingSeconds(29L));
        assertEquals(30, totp.remainingSeconds(30L));
        assertEquals(15, totp.remainingSeconds(45L));
    }
}
