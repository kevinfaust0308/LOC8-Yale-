package com.example.kevinfaust.loc8;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Login extends AppCompatActivity {
    @BindView(R.id.email_field)
    EditText emailField;
    @BindView(R.id.password_field)
    EditText passwordField;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        mAuth = FirebaseAuth.getInstance();
    }
    @OnClick(R.id.register_btn)
    void onRegister() {
        startActivity(new Intent(Login.this, Register.class));
    }
    @OnClick(R.id.login_btn)
    void onLogin() {
        startLogin();
    }
    @OnClick(R.id.forgot_pass_btn) void onForgot() {
        PasswordRequestDialog d = new PasswordRequestDialog();
        d.show(getSupportFragmentManager(), "PasswordRequestDialog");
    }
    public void startLogin() {
        if (!TextUtils.isEmpty(emailField.getText().toString().trim()) && !TextUtils.isEmpty(passwordField.getText().toString().trim())) {
            mAuth.signInWithEmailAndPassword(emailField.getText().toString().trim(), passwordField.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Intent loginIntent = new Intent(Login.this, MainActivity.class);
                        startActivity(loginIntent);
                        finish();
                    }
                }
            });
        }
    }
}