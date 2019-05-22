package com.example.taxifinder;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Console;

public class LogInActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Boolean isDriver;
    private TextView textViewWelcome;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener fbAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        isDriver = getIntent().getBooleanExtra("isDriver", false);


        mEmail = findViewById(R.id.etLogInEmail);
        mPassword = findViewById(R.id.etLogInPassword);
        textViewWelcome = findViewById(R.id.textViewLogInWelcome);

        if (isDriver){
            textViewWelcome.setText(R.string.welcome_drivers);
        } else {
            textViewWelcome.setText(R.string.welcome_customers);
        }

        mAuth = FirebaseAuth.getInstance();

        fbAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null){
                    Intent mapsIntent;
                    if (isDriver) mapsIntent = new Intent(LogInActivity.this, DriversMapActivity.class);
                    else  mapsIntent = new Intent(LogInActivity.this, CustomerMapActivity.class);

                    startActivity(mapsIntent);
                    finish();
                }
            }
        };
    }

    public void tryLogIn(View view) {
        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();
        if (email.equals("") && password.equals("")){
            Toast.makeText(LogInActivity.this, "Fill in you email and password!", Toast.LENGTH_LONG).show();
        } else {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(LogInActivity.this,
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(LogInActivity.this, "Email or password was incorrect, please try again", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    public void tryRegister(View view) {
        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();
        if (email.equals("") && password.equals("")){
            Toast.makeText(LogInActivity.this, "To sign up you need to write password and email!", Toast.LENGTH_LONG).show();

        } else {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this,
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(LogInActivity.this, "Something went wrong, please try again later", Toast.LENGTH_LONG).show();
                            } else {
                                    String userID = mAuth.getCurrentUser().getUid();
                                    if (isDriver){
                                        Log.d("users id", "userID: " + userID);
                                        DatabaseReference currentUserFromDB = FirebaseDatabase.getInstance().getReference()
                                                .child("Users").child("Drivers").child(userID).child("email");
                                        currentUserFromDB.setValue(email);
                                    } else {
                                        DatabaseReference currentUserFromDB = FirebaseDatabase.getInstance().getReference()
                                                .child("Users").child("Customers").child(userID).child("email");
                                        currentUserFromDB.setValue(email);
                                    }
                            }
                        }
                    });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(fbAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(fbAuthListener);
    }
}
