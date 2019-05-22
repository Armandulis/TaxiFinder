package com.example.taxifinder;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openLogInDriver(View view) {
        Intent logInIntent = new Intent(this, LogInActivity.class);
        logInIntent.putExtra("isDriver", true);
        startActivity(logInIntent);
        finish();
    }

    public void openLogInCustomer(View view) {
        Intent logInIntent = new Intent(this, LogInActivity.class);
        logInIntent.putExtra("isDriver", false);
        startActivity(logInIntent);
        finish();
    }
}
