package br.com.itisoft.a2fast.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.itisoft.a2fast.model.TwoFACodeModel;
import br.com.itisoft.a2fast.otp.Base32Encoding;
import br.com.itisoft.a2fast.otp.OtpHashMode;

/**
 * Parses {@code otpauth://} URIs into account models.
 */
public final class OtpAuthParser {

    private static final Pattern OTP_AUTH = Pattern.compile(
            "otpauth://([^/]+)/([^?]*)(\\?.*)?", Pattern.CASE_INSENSITIVE);

    private OtpAuthParser() {
    }

    public static Map<String, String> parse(String qrCodeStr) {
        if (qrCodeStr == null || qrCodeStr.isEmpty()) {
            return Collections.emptyMap();
        }
        Matcher match = OTP_AUTH.matcher(qrCodeStr.trim());
        if (!match.matches()) {
            return Collections.emptyMap();
        }

        Map<String, String> params = new HashMap<>();
        String type = match.group(1);
        params.put("auth", type.toLowerCase(Locale.US));

        String labelPart = urlDecode(match.group(2) == null ? "" : match.group(2));
        String label = "";
        String issuer = "";
        if (labelPart.contains(":")) {
            String[] parts = labelPart.split(":", 2);
            label = parts[0].trim();
            issuer = parts[1].trim();
        } else if (!labelPart.isEmpty()) {
            issuer = labelPart.trim();
        }

        if (!label.isEmpty()) {
            params.put("label", label);
        }
        if (!issuer.isEmpty()) {
            params.put("issuer", issuer);
        }

        String query = match.group(3);
        if (query != null && query.startsWith("?")) {
            String q = query.substring(1);
            for (String pair : q.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                String[] kv = pair.split("=", 2);
                String name = urlDecode(kv[0]).toLowerCase(Locale.US);
                String value = kv.length > 1 ? urlDecode(kv[1]) : "";
                params.put(name, value);
            }
        }

        if (label.isEmpty() && params.containsKey("issuer") && !issuer.isEmpty()) {
            String queryIssuer = params.get("issuer");
            if (queryIssuer != null && !queryIssuer.equals(issuer)) {
                params.put("label", queryIssuer);
            }
        }

        return params;
    }

    public static TwoFACodeModel toAccount(String otpAuthUri) {
        Map<String, String> params = parse(otpAuthUri);
        if (params.isEmpty()) {
            throw new IllegalArgumentException("Not a valid otpauth URI");
        }
        String auth = params.get("auth");
        if (!"totp".equals(auth) && !"steam".equals(auth)) {
            throw new IllegalArgumentException("Unsupported OTP type: " + auth);
        }
        String secret = params.get("secret");
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("Missing secret");
        }

        TwoFACodeModel model = new TwoFACodeModel();
        model.Label = firstNonEmpty(params.get("label"), params.get("issuer"), "Account");
        model.Issuer = firstNonEmpty(params.get("issuer"), "");
        model.SecretByteArray = Base32Encoding.toBytes(secret);
        model.OTPType = "steam".equals(auth) ? "steam" : "totp";
        model.HashMode = OtpHashMode.fromName(params.get("algorithm"));

        if (params.containsKey("period")) {
            try {
                model.Period = Integer.parseInt(params.get("period"));
            } catch (NumberFormatException ignored) {
            }
        }
        if (params.containsKey("digits")) {
            try {
                model.TotpSize = Integer.parseInt(params.get("digits"));
            } catch (NumberFormatException ignored) {
            }
        }
        if ("steam".equals(model.OTPType) && model.TotpSize == 6) {
            model.TotpSize = 5;
        }
        return model;
    }

    public static TwoFACodeModel fromManual(String label, String issuer, String secretBase32,
                                            String algorithm, int period, int digits, boolean steam) {
        TwoFACodeModel model = new TwoFACodeModel();
        model.Label = label == null ? "" : label.trim();
        model.Issuer = issuer == null ? "" : issuer.trim();
        model.SecretByteArray = Base32Encoding.toBytes(secretBase32.replace(" ", ""));
        model.HashMode = OtpHashMode.fromName(algorithm);
        model.Period = period > 0 ? period : 30;
        model.TotpSize = digits > 0 ? digits : (steam ? 5 : 6);
        model.OTPType = steam ? "steam" : "totp";
        return model;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
