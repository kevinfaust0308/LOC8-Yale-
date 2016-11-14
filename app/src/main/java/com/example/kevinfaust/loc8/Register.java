package com.example.kevinfaust.loc8;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Register extends AppCompatActivity {


    private static final String TAG = "Register";
    private static final int GALLERY_REQUEST = 1;
    @BindView(R.id.name_field)
    EditText nameField;
    @BindView(R.id.email_field)
    EditText emailField;
    @BindView(R.id.password_field)
    EditText passwordField;
    @BindView(R.id.confirm_password_field)
    EditText confirmPasswordField;
    @BindView(R.id.upload_pic_btn)
    ImageButton uploadPic;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseRef;
    private StorageReference mStorageRef;

    private Uri mImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        mAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    @OnClick(R.id.register_btn)
    void OnRegister() {
        startRegister();
    }


    @OnClick(R.id.upload_pic_btn)
    void OnImageSelect() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    private void startRegister() {
        final String name = nameField.getText().toString().trim();
        final String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirm_password = confirmPasswordField.getText().toString().trim();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(name) && password.length() > 5 && password.equals(confirm_password) && mImageUri != null) {


            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    if (task.isSuccessful()) {
                        StorageReference filepath = mStorageRef.child("ProfilePictures").child(mImageUri.getLastPathSegment());
                        filepath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference currentUserDb = mDatabaseRef.child("UserData").child(user_id);
                                currentUserDb.child("name").setValue(name);
                                currentUserDb.child("email").setValue(email);
                                currentUserDb.child("pictureUrl").setValue(downloadUrl.toString());
                                currentUserDb.child("online").setValue(true);
                                currentUserDb.child("statusMsg").setValue("Hi I'm " + name + "!");
                                currentUserDb.child("friends").push().setValue("master@master.com");
                                currentUserDb.child("latitude").setValue("43.130026");
                                currentUserDb.child("longitude").setValue("-82.798263");

                                // user logged in
                                Intent loginIntent = new Intent(Register.this, MainActivity.class);
                                startActivity(loginIntent);
                                finish();
                            }


                        });
                    }
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_REQUEST) {
                mImageUri = data.getData();
                uploadPic.setImageURI(mImageUri);
            }
        }

    }
}