package br.com.itisoft.a2fast.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import br.com.itisoft.a2fast.crypto.CryptoService;
import br.com.itisoft.a2fast.model.CategoryModel;
import br.com.itisoft.a2fast.model.DatafileModel;
import br.com.itisoft.a2fast.model.TwoFACodeModel;
import br.com.itisoft.a2fast.otp.OtpHashMode;

/**
 * Reads/writes {@code .2fa} JSON with field-level AES encryption matching 2fast.
 */
public final class DatafileSerializer {

    private static final Type CATEGORY_LIST = new TypeToken<List<CategoryModel>>() {
    }.getType();

    private static final ThreadLocal<Boolean> STRICT = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DatafileSerializer() {
    }

    public static DatafileModel parseMeta(String json) {
        return metaGson().fromJson(json, DatafileModel.class);
    }

    public static DatafileModel decryptAndParse(String json, byte[] key, byte[] iv) {
        return decryptAndParse(json, key, iv, true);
    }

    public static DatafileModel decryptAndParse(String json, byte[] key, byte[] iv, boolean strict) {
        CryptoService.initialize(key, iv);
        STRICT.set(strict);
        try {
            DatafileModel model = cryptoGson().fromJson(json, DatafileModel.class);
            if (strict) {
                validateDecrypted(model);
            }
            return model;
        } finally {
            STRICT.remove();
            CryptoService.clear();
        }
    }

    public static String serializeEncrypt(DatafileModel model, byte[] key, byte[] iv) {
        CryptoService.initialize(key, iv);
        try {
            return cryptoGson().toJson(model);
        } finally {
            CryptoService.clear();
        }
    }

    private static void validateDecrypted(DatafileModel model) {
        if (model == null) {
            throw new IllegalStateException("Invalid datafile");
        }
        if (model.Collection == null || model.Collection.isEmpty()) {
            return;
        }
        for (TwoFACodeModel account : model.Collection) {
            if (!isValidOtpType(account.OTPType)) {
                throw new IllegalStateException("Wrong password or corrupted datafile");
            }
            if (account.SecretByteArray == null || account.SecretByteArray.length == 0) {
                throw new IllegalStateException("Wrong password or corrupted datafile");
            }
        }
    }

    private static boolean isValidOtpType(String otpType) {
        if (otpType == null) {
            return false;
        }
        String value = otpType.trim().toLowerCase(Locale.US);
        return "totp".equals(value) || "steam".equals(value) || "hotp".equals(value);
    }

    private static Gson metaGson() {
        return new GsonBuilder()
                .registerTypeAdapter(byte[].class, new PlainByteArrayAdapter())
                .registerTypeAdapter(OtpHashMode.class, new OtpHashModeAdapter())
                .create();
    }

    private static Gson cryptoGson() {
        return new GsonBuilder()
                .registerTypeAdapter(byte[].class, new PlainByteArrayAdapter())
                .registerTypeAdapter(OtpHashMode.class, new OtpHashModeAdapter())
                .registerTypeAdapter(TwoFACodeModel.class, new TwoFACodeModelAdapter())
                .registerTypeAdapter(CategoryModel.class, new CategoryModelAdapter())
                .create();
    }

    private static boolean strict() {
        return Boolean.TRUE.equals(STRICT.get());
    }

    private static String encString(String value) {
        if (!CryptoService.isInitialized()) {
            return value == null ? "" : value;
        }
        return CryptoService.encrypt(value == null ? "" : value);
    }

    private static String decString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        String value = element.getAsString();
        if (!CryptoService.isInitialized() || value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        try {
            return CryptoService.decryptToString(value);
        } catch (RuntimeException e) {
            if (strict()) {
                throw new IllegalStateException("Failed to decrypt field", e);
            }
            return value;
        }
    }

    private static String encBytes(byte[] value) {
        if (value == null) {
            value = new byte[0];
        }
        if (CryptoService.isInitialized()) {
            return CryptoService.encrypt(value);
        }
        return Base64.getEncoder().encodeToString(value);
    }

    private static byte[] decBytes(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new byte[0];
        }
        String encoded = element.getAsString();
        if (encoded == null || encoded.isEmpty()) {
            return new byte[0];
        }
        byte[] raw = Base64.getDecoder().decode(encoded);
        if (CryptoService.isInitialized()) {
            try {
                return CryptoService.decrypt(raw);
            } catch (RuntimeException e) {
                if (strict()) {
                    throw new IllegalStateException("Failed to decrypt secret", e);
                }
                return raw;
            }
        }
        return raw;
    }

    private static final class TwoFACodeModelAdapter
            implements JsonSerializer<TwoFACodeModel>, JsonDeserializer<TwoFACodeModel> {

        @Override
        public JsonElement serialize(TwoFACodeModel src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("Label", encString(src.Label));
            obj.addProperty("Issuer", encString(src.Issuer));
            obj.addProperty("IsFavourite", src.IsFavourite);
            obj.addProperty("Period", src.Period);
            obj.add("HashMode", context.serialize(src.HashMode));
            obj.addProperty("OTPType", encString(src.OTPType == null ? "totp" : src.OTPType));
            obj.addProperty("TotpSize", src.TotpSize);
            obj.addProperty("SecretByteArray", encBytes(src.SecretByteArray));
            obj.addProperty("AccountIconName", encString(src.AccountIconName));
            obj.addProperty("Notes", encString(src.Notes));
            obj.add("SelectedCategories", context.serialize(src.SelectedCategories, CATEGORY_LIST));
            return obj;
        }

        @Override
        public TwoFACodeModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            TwoFACodeModel model = new TwoFACodeModel();
            model.Label = decString(obj.get("Label"));
            model.Issuer = decString(obj.get("Issuer"));
            model.IsFavourite = obj.has("IsFavourite") && obj.get("IsFavourite").getAsBoolean();
            model.Period = obj.has("Period") ? obj.get("Period").getAsInt() : 30;
            model.HashMode = context.deserialize(obj.get("HashMode"), OtpHashMode.class);
            if (model.HashMode == null) {
                model.HashMode = OtpHashMode.Sha1;
            }
            model.OTPType = decString(obj.get("OTPType"));
            if (model.OTPType.isEmpty()) {
                model.OTPType = "totp";
            }
            model.TotpSize = obj.has("TotpSize") ? obj.get("TotpSize").getAsInt() : 6;
            model.SecretByteArray = decBytes(obj.get("SecretByteArray"));
            model.AccountIconName = decString(obj.get("AccountIconName"));
            model.Notes = decString(obj.get("Notes"));
            if (obj.has("SelectedCategories") && obj.get("SelectedCategories").isJsonArray()) {
                model.SelectedCategories = context.deserialize(obj.get("SelectedCategories"), CATEGORY_LIST);
            }
            if (model.SelectedCategories == null) {
                model.SelectedCategories = new ArrayList<>();
            }
            return model;
        }
    }

    private static final class CategoryModelAdapter
            implements JsonSerializer<CategoryModel>, JsonDeserializer<CategoryModel> {

        @Override
        public JsonElement serialize(CategoryModel src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("UnicodeString", encString(src.UnicodeString));
            obj.addProperty("UnicodeIndex", src.UnicodeIndex);
            obj.addProperty("Name", encString(src.Name));
            obj.addProperty("IsSelected", src.IsSelected);
            obj.addProperty("Guid", src.Guid);
            return obj;
        }

        @Override
        public CategoryModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject obj = json.getAsJsonObject();
            CategoryModel model = new CategoryModel();
            model.UnicodeString = decString(obj.get("UnicodeString"));
            model.UnicodeIndex = obj.has("UnicodeIndex") && !obj.get("UnicodeIndex").isJsonNull()
                    ? obj.get("UnicodeIndex").getAsString() : null;
            model.Name = decString(obj.get("Name"));
            model.IsSelected = obj.has("IsSelected") && obj.get("IsSelected").getAsBoolean();
            if (obj.has("Guid") && !obj.get("Guid").isJsonNull()) {
                model.Guid = obj.get("Guid").getAsString();
            }
            return model;
        }
    }

    private static final class PlainByteArrayAdapter
            implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

        @Override
        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
        }

        @Override
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return Base64.getDecoder().decode(json.getAsString());
        }
    }

    private static final class OtpHashModeAdapter
            implements JsonSerializer<OtpHashMode>, JsonDeserializer<OtpHashMode> {

        @Override
        public JsonElement serialize(OtpHashMode src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src == null ? 0 : src.ordinal());
        }

        @Override
        public OtpHashMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull()) {
                return OtpHashMode.Sha1;
            }
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                return OtpHashMode.fromOrdinal(json.getAsInt());
            }
            return OtpHashMode.fromName(json.getAsString());
        }
    }
}
