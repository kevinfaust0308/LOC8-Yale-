package com.example.kevinfaust.loc8;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by Kevin Faust on 11/12/2016.
 */

public class PasswordRequestDialog extends DialogFragment {

    private FirebaseAuth mAuth;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mAuth = FirebaseAuth.getInstance();

        final View v = LayoutInflater.from(getActivity()).inflate(R.layout.password_request_popup, null);
        final EditText e = (EditText) v.findViewById(R.id.resetEmail);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);
        builder.setMessage("Password Reset")
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // send password reset
                        String email = e.getText().toString();

                        if (!TextUtils.isEmpty(email)) {
                            mAuth.sendPasswordResetEmail(email);
                        } else {
                            Toast.makeText(getActivity(), "Please enter email", Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dismiss();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();

    }
}
