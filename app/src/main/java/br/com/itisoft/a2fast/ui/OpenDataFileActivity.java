package br.com.itisoft.a2fast.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import br.com.itisoft.a2fast.App;
import br.com.itisoft.a2fast.R;
import br.com.itisoft.a2fast.crypto.CryptoService;
import br.com.itisoft.a2fast.data.DataSession;
import br.com.itisoft.a2fast.model.DatafileEntry;

public class OpenDataFileActivity extends AppCompatActivity {

    private TextView selectedFile;
    private TextInputEditText inputPassword;
    private Uri selectedUri;

    private final ActivityResultLauncher<String[]> openDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    return;
                }
                selectedUri = uri;
                selectedFile.setText(uri.toString());
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_datafile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.open_datafile_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        selectedFile = findViewById(R.id.textSelectedFile);
        inputPassword = findViewById(R.id.inputPassword);
        Button pick = findViewById(R.id.btnPickFile);
        Button open = findViewById(R.id.btnOpenFile);

        pick.setOnClickListener(v -> openDocument.launch(new String[]{"*/*", "application/octet-stream", "application/json", "text/plain"}));
        open.setOnClickListener(v -> openSelected());
    }

    private void openSelected() {
        if (selectedUri == null) {
            toast(R.string.error_no_file);
            return;
        }
        String password = inputPassword.getText() == null ? "" : inputPassword.getText().toString();
        if (password.isEmpty()) {
            toast(R.string.error_password_required);
            return;
        }

        try {
            App.get().datafileStorage().takePersistablePermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
            String json = App.get().datafileStorage().read(selectedUri);
            if (!DataSession.get().verifyPassword(password, json)) {
                toast(R.string.error_wrong_password);
                return;
            }

            String hash = CryptoService.createStringHash(password);
            String name = selectedUri.getLastPathSegment();
            if (name == null || name.isEmpty()) {
                name = "datafile.2fa";
            }
            int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf(':'));
            if (slash >= 0 && slash < name.length() - 1) {
                name = name.substring(slash + 1);
            }

            DatafileEntry entry = App.get().preferences().addOrUpdateDatafile(name, selectedUri.toString(), hash);
            DataSession.get().unlock(password, json, entry.id);

            // Only encrypt empty desktop placeholders — never rewrite healthy datafiles on open.
            if (DataSession.get().needsPlaceholderEncryption(json)) {
                App.get().datafileStorage().write(selectedUri, DataSession.get().serializeCurrent());
            }

            App.get().passwordVault().writePasswordForFile(entry.id, hash, password);

            Intent intent = new Intent(this, AccountListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            toast(R.string.error_wrong_password);
        }
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
