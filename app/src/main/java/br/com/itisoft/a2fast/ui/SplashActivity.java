package br.com.itisoft.a2fast.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import br.com.itisoft.a2fast.App;
import br.com.itisoft.a2fast.data.DataSession;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DataSession.get().lock();

        Intent next;
        if (App.get().preferences().hasConfiguredDatafile()) {
            next = new Intent(this, DataFilesActivity.class);
        } else {
            next = new Intent(this, WelcomeActivity.class);
        }
        startActivity(next);
        finish();
    }
}
