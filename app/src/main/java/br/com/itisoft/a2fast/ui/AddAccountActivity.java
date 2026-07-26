package br.com.itisoft.a2fast.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import br.com.itisoft.a2fast.App;
import br.com.itisoft.a2fast.R;
import br.com.itisoft.a2fast.data.DataSession;
import br.com.itisoft.a2fast.model.DatafileEntry;
import br.com.itisoft.a2fast.model.TwoFACodeModel;
import br.com.itisoft.a2fast.parser.OtpAuthParser;

public class AddAccountActivity extends AppCompatActivity {

    private TextInputEditText inputLabel;
    private TextInputEditText inputIssuer;
    private TextInputEditText inputSecret;
    private TextInputEditText inputOtpAuth;
    private AutoCompleteTextView inputAlgorithm;
    private TextInputEditText inputPeriod;
    private TextInputEditText inputDigits;

    private final ActivityResultLauncher<Intent> scanQrLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                String qrContent = result.getData().getStringExtra(QrScanActivity.EXTRA_QR_CONTENT);
                if (qrContent == null || qrContent.isEmpty()) {
                    return;
                }
                inputOtpAuth.setText(qrContent);
                applyOtpAuthUri(qrContent);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.add_account_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        inputLabel = findViewById(R.id.inputLabel);
        inputIssuer = findViewById(R.id.inputIssuer);
        inputSecret = findViewById(R.id.inputSecret);
        inputOtpAuth = findViewById(R.id.inputOtpAuth);
        inputAlgorithm = findViewById(R.id.inputAlgorithm);
        inputPeriod = findViewById(R.id.inputPeriod);
        inputDigits = findViewById(R.id.inputDigits);
        Button save = findViewById(R.id.btnSaveAccount);
        Button parseUri = findViewById(R.id.btnParseUri);
        Button scanQr = findViewById(R.id.btnScanQr);

        String[] algorithms = new String[]{"SHA1", "SHA256", "SHA512"};
        inputAlgorithm.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, algorithms));
        inputAlgorithm.setText("SHA1", false);
        inputPeriod.setText("30");
        inputDigits.setText("6");

        scanQr.setOnClickListener(v ->
                scanQrLauncher.launch(new Intent(this, QrScanActivity.class)));

        parseUri.setOnClickListener(v -> {
            String uri = text(inputOtpAuth);
            if (uri.isEmpty()) {
                toast(R.string.error_otpauth_required);
                return;
            }
            applyOtpAuthUri(uri);
        });

        save.setOnClickListener(v -> {
            try {
                TwoFACodeModel model;
                Object tagged = inputSecret.getTag();
                if (tagged instanceof TwoFACodeModel) {
                    model = (TwoFACodeModel) tagged;
                    String label = text(inputLabel);
                    String issuer = text(inputIssuer);
                    if (!label.isEmpty()) {
                        model.Label = label;
                    }
                    if (!issuer.isEmpty()) {
                        model.Issuer = issuer;
                    }
                } else {
                    int period = parseInt(text(inputPeriod), 30);
                    int digits = parseInt(text(inputDigits), 6);
                    model = OtpAuthParser.fromManual(
                            text(inputLabel),
                            text(inputIssuer),
                            text(inputSecret),
                            text(inputAlgorithm),
                            period,
                            digits,
                            false
                    );
                }
                if (model.Label == null || model.Label.isEmpty()) {
                    toast(R.string.error_label_required);
                    return;
                }
                DataSession.get().addAccount(model);
                DatafileEntry active = App.get().preferences().getActiveDatafile();
                if (active == null || active.uri == null) {
                    toast(R.string.error_write_datafile);
                    return;
                }
                Uri uri = Uri.parse(active.uri);
                App.get().datafileStorage().write(uri, DataSession.get().serializeCurrent());
                finish();
            } catch (Exception e) {
                toast(R.string.error_add_account);
            }
        });
    }

    private void applyOtpAuthUri(String uri) {
        try {
            TwoFACodeModel model = OtpAuthParser.toAccount(uri);
            inputLabel.setText(model.Label);
            inputIssuer.setText(model.Issuer);
            inputAlgorithm.setText(model.HashMode.name().replace("Sha", "SHA"), false);
            inputPeriod.setText(String.valueOf(model.Period));
            inputDigits.setText(String.valueOf(model.TotpSize));
            inputSecret.setText("********");
            inputSecret.setTag(model);
            toast(R.string.otpauth_parsed);
        } catch (Exception e) {
            toast(R.string.error_invalid_otpauth);
        }
    }

    private static String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private static String text(AutoCompleteTextView editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
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
