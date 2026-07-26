package br.com.itisoft.a2fast.model;

import java.util.ArrayList;
import java.util.List;

import br.com.itisoft.a2fast.otp.OtpHashMode;

/**
 * Account entry stored in a {@code .2fa} datafile. Field names match the C# model
 * for JSON compatibility (PascalCase).
 */
public class TwoFACodeModel {
    public String Label = "";
    public String Issuer = "";
    public boolean IsFavourite;
    public int Period = 30;
    public OtpHashMode HashMode = OtpHashMode.Sha1;
    public String OTPType = "totp";
    public int TotpSize = 6;
    public byte[] SecretByteArray;
    public String AccountIconName = "";
    public String Notes = "";
    public List<CategoryModel> SelectedCategories = new ArrayList<>();

    // Runtime-only fields (not persisted)
    public transient String TwoFACode = "";
    public transient double Seconds;
    public transient boolean HideTOTPCode;
}
