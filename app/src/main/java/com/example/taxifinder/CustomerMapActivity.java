package com.example.taxifinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.taxifinder.Helpers.ValuesHelper;
import com.example.taxifinder.Services.AuthService;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity
        implements OnMapReadyCallback {

    // Map
    private GoogleMap mMap;
    private Marker pickupMarker;
    // Location
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProvider;
    private LatLng pickUpLocation;
    private LatLng destinationLatLog;
    // GeoQuery
    private GeoQuery geoQuery;
    // UI
    private Button mPickUpButton;
    private RatingBar mRatingBar;
    private LinearLayout driversDetailsLayout;
    private TextView textDriverDetailsName, textDriverDetailsPhone;
    private ImageView imageDriverPic;
    private RadioGroup radioGroupServiceType;
    // variables
    private boolean isRequested = false;
    private String requestService, customersDestination, customerID;
    private AuthService mAuthService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);


        // setting values
        textDriverDetailsName = findViewById(R.id.tvDriverDetailsName);
        textDriverDetailsPhone = findViewById(R.id.tvDriverDetailsPhone);
        driversDetailsLayout = findViewById(R.id.driversLayoutProfile);
        imageDriverPic = findViewById(R.id.imageDriverPicDetails);
        mRatingBar = findViewById(R.id.ratingBar);
        mPickUpButton = findViewById(R.id.btnPickUp);
        radioGroupServiceType = findViewById(R.id.radioGroupServiceTypeCustomer);
        radioGroupServiceType.check(R.id.radioButtonTaxiCustomer);

        mAuthService = new AuthService();

        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Asking for Permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ValuesHelper.PERMISSION_REQUEST_CODE);

        mFusedLocationProvider = LocationServices.getFusedLocationProviderClient(this);
        destinationLatLog = new LatLng(0.0, 0.0);

        // loading map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);

        setUpAutoComplete();


    }

    private  void setUpAutoComplete() {
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        Places.initialize(getApplicationContext(), ValuesHelper.API_KEY);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                    customersDestination = place.getName();
                    destinationLatLog = place.getLatLng();
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("Error on AutoComplete", "An error occurred: " + status);
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        radioGroupServiceType.check(R.id.radioButtonTaxiCustomer);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(4000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ValuesHelper.PERMISSION_REQUEST_CODE);
            }
        }
    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList  = locationResult.getLocations();
            mLastLocation = locationList.get(locationList.size() -1);
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ValuesHelper.PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                    mFusedLocationProvider.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }
                else {
                    Toast.makeText(CustomerMapActivity.this, "Cannot show your current location without Permission", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    public void tryLogOut(View view) {
        mAuthService.userSignOut();
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
        finish();
    }

    public void buttonRequestOrCancel(View view) {
        if (isRequested){
            endRide();
        } else {
         RadioButton serviceButton = findViewById(radioGroupServiceType.getCheckedRadioButtonId());
            if (serviceButton != null){
                requestService = serviceButton.getText().toString();
            }
            mPickUpButton.setText(R.string.pick_me_up);

            // setting up customer Request with current location
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("customerRequest");
            //using geofire to create location with users id, and pushin it to ''table called customerPickup
            GeoFire geoFire = new GeoFire(dbRef);
            geoFire.setLocation(customerID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

            //putting a marker for customer from where to pick him up
            pickUpLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            pickupMarker = mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Pick me Up!")
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer_foreground)));

            mPickUpButton.setText(R.string.waitting_for_pickup);

            getClosestDriver();

            isRequested = true;
}

    }

    private int searchRadius = 1;
    private boolean isDriverFound = false;
    private String driversID;
    private DatabaseReference closestDriverDatabaseRef;
    private ValueEventListener closestDriverDatabaseRefListener;
    private void getClosestDriver() {

        //creating reference to database to see available drivers
        DatabaseReference driversLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");


        // trying to find closest driver within 1 radius (probably kilomiters)
        GeoFire geoFire = new GeoFire(driversLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), searchRadius);
        geoQuery.removeAllListeners();


        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // check if driver is not found and user is requesting pickup
                if (!isDriverFound && isRequested) {
                    closestDriverDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    closestDriverDatabaseRefListener = closestDriverDatabaseRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                                if (!isDriverFound && isRequested) {
                                    Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                    if (driverMap.get("service") != null && driverMap.get("service").equals(requestService)) {
                                        isDriverFound = true;
                                        driversID = dataSnapshot.getKey();
                                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID).child("customerRequest");

                                        // placing customers information for Driver

                                        HashMap map = new HashMap();
                                        map.put("customerRideId", customerID);
                                        map.put("destination", customersDestination);
                                        map.put("destinationLat", destinationLatLog.latitude);
                                        map.put("destinationLng", destinationLatLog.longitude);
                                        driverRef.updateChildren(map);

                                        getDriversLocation();
                                        getDriversDetails();
                                        getHasRideEnded();

                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    
                }

            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            // adds one to search radius if driver was not found and calls this function again
            @Override
            public void onGeoQueryReady() {
                if (!isDriverFound && isRequested){
                    searchRadius++;
                    getClosestDriver();
                }

                // if no driver was found within 900 km, request will be canceled
                if (searchRadius == 900){
                    endRide();
                    Toast.makeText(CustomerMapActivity.this, "No avaiable drivers at the moment, try again later", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                endRide();
                Toast.makeText(CustomerMapActivity.this, "Something went wrong, please try again later", Toast.LENGTH_LONG).show();
            }
        });
    }
    DatabaseReference driversDetailsDatabaseRef;
    ValueEventListener driversDetailsDatabaseRefListener;
    private void getDriversDetails() {

        driversDetailsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID);
        driversDetailsDatabaseRefListener = driversDetailsDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    // getting Drivers Information
                    if (map.get("name") != null) {
                        textDriverDetailsName.setText( map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        textDriverDetailsPhone.setText( map.get("phone").toString());
                    }
                    if (map.get("profileImageUri") != null) {
                        Glide.with(getApplication())
                                .load(Uri.parse(map.get("profileImageUri").toString()))
                                .into(imageDriverPic);
                    }
                    // setting up avarage drivers rating
                    int ratingSum = 0;
                    int ratingTotal = 0;
                    float ratingAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingTotal++;
                    }
                    if(ratingTotal != 0){
                        ratingAvg = ratingSum/ratingTotal;
                        mRatingBar.setRating(ratingAvg);
                    }

                    driversDetailsLayout.setVisibility(View.VISIBLE);
                    mRatingBar.setVisibility(View.VISIBLE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private Marker mDriversMarker;
    private DatabaseReference driversLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriversLocation() {

        // getting 'l' means getting lat and lng from Geofire
        driversLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driversID).child("l");
        driverLocationRefListener = driversLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && isRequested){

                    //

                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                        Log.d("Pick Up", "Location for distance: " + locationLat);
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());

                    }

                    LatLng driversLatLng = new LatLng(locationLat, locationLng);
                    if (mDriversMarker != null){
                        mDriversMarker.remove();
                    }


                    // Getting location to calculate distance
                    Location locationDriver = new Location("");
                    locationDriver.setLatitude(driversLatLng.latitude);
                    locationDriver.setLongitude(driversLatLng.longitude);

                    Location locationCustomer = new Location("");
                    locationCustomer.setLatitude(pickUpLocation.latitude);
                    locationCustomer.setLongitude(pickUpLocation.longitude);

                        float distance = locationDriver.distanceTo(locationCustomer);
                        if (distance > 30){
                            mPickUpButton.setText("Distance: " + distance);
                            mDriversMarker = mMap.addMarker(new MarkerOptions().position(driversLatLng).title("Your Taxi is on the way")
                                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_taxi_foreground)));
                        } else {
                            mPickUpButton.setText("Your Ride is here!");
                        }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;
    private void getHasRideEnded() {
        // checks if there's still value inside customerRequest inside driver
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){


                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void endRide(){
        // removes all listeners and restores values
        isDriverFound = false;
        isRequested = false;
        driversDetailsLayout.setVisibility(View.GONE);
        mRatingBar.setVisibility(View.GONE);
        geoQuery.removeAllListeners();
        if (driversLocationRef != null){
            driversLocationRef.removeEventListener(driverLocationRefListener);
        }
        if (driveHasEndedRef != null){
            driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
        }
        if (closestDriverDatabaseRef != null){
            closestDriverDatabaseRef.removeEventListener(closestDriverDatabaseRefListener);
        }
        if (driversDetailsDatabaseRef != null){
            driversDetailsDatabaseRef.removeEventListener(driversDetailsDatabaseRefListener);
        }





        if (driversID != null){
            DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID).child("customerRequest");

            customerRef.removeValue();
            driversID = null;
            Log.d("RemoveValue", "buttonRequestOrCancel: remove value from customers request");
        }
        if (pickupMarker != null){
            pickupMarker.remove();
        }
        if (mDriversMarker != null){
            mDriversMarker.remove();
        }

        searchRadius = 1;
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(dbRef);
        geoFire.removeLocation(userID);
        mPickUpButton.setText("Call for ride!");
    }

    public void openProfile(View view) {
        Intent customerProfileIntent = new Intent(this, ProfileActivity.class);
        customerProfileIntent.putExtra("isDriver", false);
        startActivity(customerProfileIntent);

    }

    public void openHistory(View view) {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        historyIntent.putExtra("customerOrDriver", "Customers");
        startActivity(historyIntent);

    }

}