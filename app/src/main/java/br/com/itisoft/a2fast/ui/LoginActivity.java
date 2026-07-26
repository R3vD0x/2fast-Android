package br.com.itisoft.a2fast.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import br.com.itisoft.a2fast.App;
import br.com.itisoft.a2fast.R;
import br.com.itisoft.a2fast.crypto.CryptoService;
import br.com.itisoft.a2fast.data.DataSession;
import br.com.itisoft.a2fast.model.DatafileEntry;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_ID = "file_id";

    private DatafileEntry targetFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        String fileId = getIntent().getStringExtra(EXTRA_FILE_ID);
        if (fileId != null) {
            targetFile = App.get().preferences().getDatafile(fileId);
        }
        if (targetFile == null) {
            targetFile = App.get().preferences().getActiveDatafile();
        }
        if (targetFile == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }
        App.get().preferences().setActiveDatafileId(targetFile.id);

        TextView subtitle = findViewById(R.id.textLoginSubtitle);
        if (subtitle != null) {
            subtitle.setText(getString(R.string.login_subtitle_named, targetFile.displayName));
        }

        TextInputEditText inputPassword = findViewById(R.id.inputPassword);
        Button login = findViewById(R.id.btnLogin);
        Button changeFile = findViewById(R.id.btnChangeFile);

        login.setOnClickListener(v -> {
            String password = inputPassword.getText() == null ? "" : inputPassword.getText().toString();
            attemptLogin(password);
        });

        changeFile.setOnClickListener(v -> {
            DataSession.get().lock();
            Intent intent = new Intent(this, DataFilesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void attemptLogin(String password) {
        if (password.isEmpty()) {
            Toast.makeText(this, R.string.error_password_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String computed = CryptoService.createStringHash(password);
        if (targetFile.passwordHash == null || !targetFile.passwordHash.equalsIgnoreCase(computed)) {
            Toast.makeText(this, R.string.error_wrong_password, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = Uri.parse(targetFile.uri);
            String json = App.get().datafileStorage().read(uri);
            DataSession.get().unlock(password, json, targetFile.id);
            App.get().passwordVault().writePasswordForFile(targetFile.id, computed, password);
            App.get().preferences().setActiveDatafileId(targetFile.id);

            Intent intent = new Intent(this, AccountListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_open_datafile, Toast.LENGTH_SHORT).show();
        }
    }
}
