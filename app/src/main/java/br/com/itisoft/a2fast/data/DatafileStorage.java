package br.com.itisoft.a2fast.data;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Read/write {@code .2fa} documents via Storage Access Framework URIs.
 */
public final class DatafileStorage {

    private final Context context;

    public DatafileStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public String read(Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) {
                throw new IOException("Unable to open datafile for reading");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public void write(Uri uri, String content) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        try (OutputStream out = resolver.openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("Unable to open datafile for writing");
            }
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    public void takePersistablePermission(Uri uri, int flags) {
        final int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (SecurityException ignored) {
            // Some providers do not support persistable permissions.
        }
    }
}
