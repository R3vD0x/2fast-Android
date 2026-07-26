package br.com.itisoft.a2fast.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root envelope of a {@code .2fa} file. Compatible with Project2FA DatafileModel.
 */
public class DatafileModel {
    public List<TwoFACodeModel> Collection = new ArrayList<>();
    public List<CategoryModel> GlobalCategories = new ArrayList<>();
    public byte[] IV;
    public byte[] Salt;
    /** 0/1 = V1, 2 = V2 (current writes), 3+ = V3 with per-file salt. */
    public int Version = 2;
}
