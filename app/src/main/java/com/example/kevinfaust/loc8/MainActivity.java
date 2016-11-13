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
    private Location mLastLocation;
    //private Marker mCurrentUserMarker;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DatabaseReference mDatabaseRef;

    private User currentUser; //contains current user object
    private List<User> currentOnlineUserFriends; //contains all our ONLINE "friend" user objects
    private List<User> allFriends = new ArrayList<User>(); //contains ALL USER FRIENDS

    private ProgressDialog progressDialog;

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

                //if user not logged in, redirect them to login page
                if (firebaseAuth.getCurrentUser() == null) {
                    Intent loginIntent = new Intent(MainActivity.this, Login.class);
                    startActivity(loginIntent);
                    finish();
                }
            }
        };
        progressDialog = new ProgressDialog(this);
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        buildGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);

        // we're logged in

        // get email of currently logged in user
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {

            progressDialog.setMessage("Loading");
            progressDialog.show();

            String user_id = user.getUid();

            // get handle on logged in user object (updates automatically)
            final DatabaseReference currentUserRef = mDatabaseRef.child("UserData").child(user_id);
            currentUserRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    currentUser = dataSnapshot.getValue(User.class);

                    //TODO: load user's longitude/latitude on map with profile pic as marker and set status message
                    //Toast.makeText(MainActivity.this, "Some value in current user's database data has been changed or we are returning to this screen", Toast.LENGTH_SHORT).show();

                    // update user's status message
                    statusMessage.setText(currentUser.getStatusMsg());

                    Log.d(TAG, "Online? " + currentUser.isOnline());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            // find friend's datas (updates automatically)
            DatabaseReference users = mDatabaseRef.child("UserData");
            users.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    currentOnlineUserFriends = new ArrayList<>();

                    // loop through ALL the User objects
                    for (DataSnapshot user : dataSnapshot.getChildren()) {
                        User randomUser = user.getValue(User.class);
                        Log.d(TAG, "User 1:" + randomUser.getName() + ". friends: " + randomUser.getFriends());

                        //get the current random user's email
                        String randomUserEmail = randomUser.getEmail();

                        // look through our friend-list-map where the values are emails and check
                        // if current random user exists in user's friend list
                        if (currentUser.getFriends().values().contains(randomUserEmail)) {
                            if (randomUser.isOnline()) {
                                // this random user is an ONLINE friend
                                // add this random user to our special online friend's list
                                currentOnlineUserFriends.add(randomUser);
                            }

                            // add all friend users (online or offline) to our generic friend's list
                            allFriends.add(randomUser);

                        }
                    }

                    //debugging purposes
                    String onlineFriends = "";
                    for (User friend : currentOnlineUserFriends) {
                        Log.d(TAG, "online friend: " + friend.getName() + " with email " + friend.getEmail());
                        onlineFriends += friend.getName() + " ";
                    }
                    String offlineFriends = "";
                    for (User friend : allFriends) {
                        Log.d(TAG, "online friend: " + friend.getName() + " with email " + friend.getEmail());
                        offlineFriends += friend.getName() + " ";
                    }
                    //Toast.makeText(MainActivity.this, "Some user's data has been changed. List of user friend objects updated or we are returning to this screen", Toast.LENGTH_SHORT).show();
                    //Toast.makeText(MainActivity.this, "Your online friends: " + onlineFriends, Toast.LENGTH_SHORT).show();
                    //Toast.makeText(MainActivity.this, "ALL friends: " + offlineFriends, Toast.LENGTH_SHORT).show();


                    //TODO: take this list of ONLINE friend objects and place their locations on the map

                    if (map != null) {
                        map.clear();

                        updateMarkers();

                    }


                    mapFragment.getMapAsync(MainActivity.this);
                    progressDialog.dismiss();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }


/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_logout:
                mAuth.signOut();
                break;
        }

        return super.onOptionsItemSelected(item);
    }*/

    @OnClick(R.id.changeStatusAvailability)
    void onStatusChange() {

        DatabaseReference currentUserRef = mDatabaseRef.child("UserData").child(mAuth.getCurrentUser().getUid());

        if (currentUser.isOnline()) {
            Log.d(TAG, "Make me offline");
            currentUserRef.child("online").setValue(false);
            statusAvailability.setBackground(ContextCompat.getDrawable(this, R.drawable.red_circle));
        } else {
            Log.d(TAG, "Make me online");
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
        // retrieve all the user's friends list emails
        ArrayList<String> emails = new ArrayList<>();

        //loop through friend user objects and extract email
        for (User u : allFriends) {
            emails.add(u.getEmail());
        }

        //pass emails to search activity
        Intent i = new Intent(MainActivity.this, SearchActivity.class);
        i.putStringArrayListExtra("friendEmails", emails);
        startActivity(i);
    }

    @OnClick(R.id.logout)
    void onLogout() {
        mAuth.signOut();
    }


    /*
    Updates current user's location. This will automatically update the map
     */
    public void updateLocation(String longitude, String latitude) {
        DatabaseReference currentUserRef = mDatabaseRef.child("UserData").child(mAuth.getCurrentUser().getUid());
        currentUserRef.child("longitude").setValue(longitude);
        currentUserRef.child("latitude").setValue(latitude);
    }

    /*
    Set your status message
     */
    public void setStatusMessage(String message) {
        DatabaseReference currentUserRef = mDatabaseRef.child("UserData").child(mAuth.getCurrentUser().getUid());
        currentUserRef.child("statusMsg").setValue(message);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        googleMap.setMyLocationEnabled(true);

        // Add a marker to user's location and move the camera
/*
        LatLng location = new LatLng(Double.parseDouble(currentUser.getLatitude()), Double.parseDouble(currentUser.getLongitude()));

        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title("You. Status: " + currentUser.getStatusMsg());


        mCurrentUserMarker = googleMap.addMarker(markerOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location));
*/

        LatLng location = new LatLng(Double.parseDouble(currentUser.getLatitude()), Double.parseDouble(currentUser.getLongitude()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));


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
            map.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
/*        mLastLocation = location;
        if (mCurrentUserMarker != null) {
            mCurrentUserMarker.remove();



            //Place current location marker
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            updateLocation(String.valueOf(location.getLongitude()), String.valueOf(location.getLatitude()));

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("You");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            mCurrentUserMarker = map.addMarker(markerOptions);

            //move map camera
            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            map.animateCamera(CameraUpdateFactory.zoomTo(11));
        }*/

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }


    public void updateMarkers() {

        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));

        for (final User u : currentOnlineUserFriends) {
            Log.d(TAG, "Adding marker for user: " + u.getName());

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
                            .title(u.getName() + " status: " + u.getStatusMsg())
                            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
                    map.addMarker(options);
                }
            });


        }
    }

}
