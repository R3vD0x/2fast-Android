package br.com.itisoft.a2fast.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import br.com.itisoft.a2fast.R;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button create = findViewById(R.id.btnCreate);
        Button open = findViewById(R.id.btnOpen);

        create.setOnClickListener(v ->
                startActivity(new Intent(this, CreateDataFileActivity.class)));
        open.setOnClickListener(v ->
                startActivity(new Intent(this, OpenDataFileActivity.class)));
    }
}
