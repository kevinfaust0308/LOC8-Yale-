package com.example.kevinfaust.loc8;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "MainActivity";

    @BindView(R.id.statusMessage)
    EditText statusMessage;
    @BindView(R.id.statusAvailability)
    ImageButton statusAvailability;

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DatabaseReference mDatabaseRef;

    private User currentUser;
    private List<User> currentOnlineUserFriends;
    private List<User> allFriends = new ArrayList<User>();

    private MapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if (firebaseAuth.getCurrentUser() == null) {
                    Intent loginIntent = new Intent(MainActivity.this, Login.class);
                    startActivity(loginIntent);
                    finish();
                }
            }
        };
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);


        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {

            buildGoogleApiClient();

            String user_id = user.getUid();

            final DatabaseReference currentUserRef = mDatabaseRef.child("UserData").child(user_id);
            currentUserRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    currentUser = dataSnapshot.getValue(User.class);
                    statusMessage.setText(currentUser.getStatusMsg());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


            DatabaseReference users = mDatabaseRef.child("UserData");
            users.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    currentOnlineUserFriends = new ArrayList<>();

                    for (DataSnapshot user : dataSnapshot.getChildren()) {
                        User randomUser = user.getValue(User.class);
                        Log.d(TAG, "User 1:" + randomUser.getName() + ". friends: " + randomUser.getFriends());

                        String randomUserEmail = randomUser.getEmail();

                        if (currentUser.getFriends().values().contains(randomUserEmail)) {
                            if (randomUser.isOnline()) {
                                currentOnlineUserFriends.add(randomUser);
                            }

                            allFriends.add(randomUser);

                        }
                    }

                    if (map != null) {
                        map.clear();

                        updateMarkers();

                    }

                    mapFragment.getMapAsync(MainActivity.this);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }


    @OnClick(R.id.changeStatusAvailability)
    void onStatusChange() {

        DatabaseReference currentUserRef = mDatabaseRef.child("UserData").child(mAuth.getCurrentUser().getUid());

        if (currentUser.isOnline()) {
            currentUserRef.child("online").setValue(false);
            statusAvailability.setBackground(ContextCompat.getDrawable(this, R.drawable.red_circle));
        } else {
            currentUserRef.child("online").setValue(true);
            statusAvailability.setBackground(ContextCompat.getDrawable(this, R.drawable.green_circle));
        }
    }

    @OnClick(R.id.saveStatusBtn)
    void onStatusSave() {

        String status = statusMessage.getText().toString();
        setStatusMessage(status);

    }

    @OnClick(R.id.search_btn)
    void onSearch() {
        ArrayList<String> emails = new ArrayList<>();
        for (User u : allFriends) {
            emails.add(u.getEmail());
        }
        Intent i = new Intent(MainActivity.this, SearchActivity.class);
        i.putStringArrayListExtra("friendEmails", emails);
        startActivity(i);
    }

    @OnClick(R.id.logout)
    void onLogout() {
        mAuth.signOut();
    }


    public void updateLocation(String longitude, String latitude) {
        DatabaseReference currentUserRef = mDatabaseRef.child("UserData").child(mAuth.getCurrentUser().getUid());
        currentUserRef.child("longitude").setValue(longitude);
        currentUserRef.child("latitude").setValue(latitude);
    }

    public void setStatusMessage(String message) {
        DatabaseReference currentUserRef = mDatabaseRef.child("UserData").child(mAuth.getCurrentUser().getUid());
        currentUserRef.child("statusMsg").setValue(message);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        LatLng location = new LatLng(Double.parseDouble(currentUser.getLatitude()), Double.parseDouble(currentUser.getLongitude()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location));

        updateMarkers();

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        updateLocation(String.valueOf(location.getLongitude()), String.valueOf(location.getLatitude()));

        //move map camera
        if (map != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }


    public void updateMarkers() {

        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));

        for (final User u : currentOnlineUserFriends) {

            final LatLng friend = new LatLng(Double.parseDouble(u.getLatitude()), Double.parseDouble(u.getLongitude()));

            Polyline line = map.addPolyline(new PolylineOptions()
                    .add(friend, new LatLng(Double.parseDouble(currentUser.getLatitude()), Double.parseDouble(currentUser.getLongitude())))
                    .width(5)
                    .color(Color.RED).geodesic(false)
            );

            imageLoader.loadImage(u.getPictureUrl(), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(loadedImage, 150, 150, false);

                    // Do whatever you want with Bitmap
                    MarkerOptions options = new MarkerOptions()
                            .position(friend)
                            .title("status: " + u.getStatusMsg())
                            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
                    map.addMarker(options);
                }
            });


        }
    }

}
