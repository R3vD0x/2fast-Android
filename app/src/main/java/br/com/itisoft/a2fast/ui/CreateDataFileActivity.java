package br.com.itisoft.a2fast.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
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

public class CreateDataFileActivity extends AppCompatActivity {

    private TextInputEditText inputName;
    private TextInputEditText inputPassword;
    private TextInputEditText inputPasswordRepeat;
    private final ActivityResultLauncher<String> createDocument =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                if (uri == null) {
                    return;
                }
                finishCreate(uri);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_datafile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.create_datafile_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        inputName = findViewById(R.id.inputFileName);
        inputPassword = findViewById(R.id.inputPassword);
        inputPasswordRepeat = findViewById(R.id.inputPasswordRepeat);
        Button create = findViewById(R.id.btnCreateFile);

        create.setOnClickListener(v -> startCreate());
    }

    private void startCreate() {
        String name = textOf(inputName);
        String password = textOf(inputPassword);
        String repeat = textOf(inputPasswordRepeat);

        if (name.isEmpty()) {
            name = "2fast";
        }
        if (!name.endsWith(".2fa")) {
            name = name + ".2fa";
        }
        if (password.length() < 4) {
            toast(R.string.error_password_short);
            return;
        }
        if (!password.equals(repeat)) {
            toast(R.string.error_password_mismatch);
            return;
        }

        inputName.setTag(name);
        inputPassword.setTag(password);
        createDocument.launch(name);
    }

    private void finishCreate(Uri uri) {
        String name = String.valueOf(inputName.getTag());
        String password = String.valueOf(inputPassword.getTag());
        try {
            App.get().datafileStorage().takePersistablePermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            String hash = CryptoService.createStringHash(password);
            DatafileEntry entry = App.get().preferences().addOrUpdateDatafile(name, uri.toString(), hash);

            String encrypted = DataSession.get().createEmptyEncryptedFile(password, entry.id);
            App.get().datafileStorage().write(uri, encrypted);
            App.get().passwordVault().writePasswordForFile(entry.id, hash, password);

            Intent intent = new Intent(this, AccountListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            toast(R.string.error_write_datafile);
        }
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
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
