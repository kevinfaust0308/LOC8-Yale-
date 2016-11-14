package com.example.kevinfaust.loc8;

import android.app.ProgressDialog;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";

    @BindView(R.id.search_field)
    EditText searchField;

    @BindView(R.id.search_result)
    LinearLayout searchResult;
    @BindView(R.id.search_profile_pic_result)
    ImageView profilePicResult;
    @BindView(R.id.search_user_name_result)
    TextView nameResult;
    @BindView(R.id.add_user_btn)
    Button addUserBtn;

    private ArrayList<String> currentFriendEmails;
    private String searchResultFriendEmail;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("UserData");
        mAuth = FirebaseAuth.getInstance();

        currentFriendEmails = getIntent().getStringArrayListExtra("friendEmails");

    }

    @OnClick(R.id.search_btn)
    void onSearch() {

        final String searchText = searchField.getText().toString().trim();
        if (!TextUtils.isEmpty(searchText)) {

            Query queryRef = mDatabaseRef.orderByChild("email").equalTo(searchText);

            queryRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
                    User u = dataSnapshot.getValue(User.class);
                    nameResult.setText(u.getName());
                    Picasso.with(SearchActivity.this).load(u.getPictureUrl()).into(profilePicResult);
                    addUserBtn.setVisibility(View.VISIBLE);
                    if (currentFriendEmails.contains(u.getEmail())) {
                        addUserBtn.setText("Already added");
                        addUserBtn.setEnabled(false);
                    } else if (mAuth.getCurrentUser().getEmail().equals(searchText)) {
                        addUserBtn.setText("You");
                        addUserBtn.setEnabled(false);
                    } else {
                        addUserBtn.setText("Add");
                        addUserBtn.setEnabled(true);
                    }
                    searchResultFriendEmail = u.getEmail();
                }


                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }


    @OnClick(R.id.add_user_btn)
    void onAdd() {
        String user_id = mAuth.getCurrentUser().getUid();
        DatabaseReference currentUserDb = mDatabaseRef.child(user_id);
        currentUserDb.child("friends").push().setValue(searchResultFriendEmail);
        addUserBtn.setEnabled(false);
    }
}
