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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.taxifinder.Helpers.ValuesHelper;
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
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;

    Location mLastLocation;
    LocationRequest mLocationRequest;

    FusedLocationProviderClient mFusedLocationProvider;

    Button mPickUpButton;

    LatLng pickUpLocation;

    private LatLng destinationLatLog;


    GeoQuery geoQuery;

    private Marker pickupMarker;
    private boolean isRequested = false;

    private LinearLayout driversDetailsLayout;
    private TextView textDriverDetailsName, textDriverDetailsPhone;
    private ImageView imageDriverPic;
    private RadioGroup radioGroupServiceType;
    private String requestService;

    private String customersDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationProvider = LocationServices.getFusedLocationProviderClient(this);
        mPickUpButton = findViewById(R.id.btnPickUp);

        destinationLatLog = new LatLng(0.0, 0.0);


        textDriverDetailsName = findViewById(R.id.tvDriverDetailsName);
        textDriverDetailsPhone = findViewById(R.id.tvDriverDetailsPhone);
        driversDetailsLayout = findViewById(R.id.driversLayoutProfile);
        imageDriverPic = findViewById(R.id.imageDriverPicDetails);

        radioGroupServiceType = findViewById(R.id.radioGroupServiceTypeCustomer);

        radioGroupServiceType.check(R.id.radioButtonTaxiCustomer);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ValuesHelper.PERMISSION_REQUEST_CODE);

        setUpAutoComplete();


    }

    private  void setUpAutoComplete() {
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        Places.initialize(getApplicationContext(), "AIzaSyDb3jkSprTI-b0DLIu68F5XLuxPpNI3YY4");

        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);
        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            } else {
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
            }
            else {
                Toast.makeText(CustomerMapActivity.this, "Cannot show your current location without Permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void connectDriver(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ValuesHelper.PERMISSION_REQUEST_CODE);
    }

    private void disconnectDriver(){
        if (mFusedLocationProvider != null) {
            mFusedLocationProvider.removeLocationUpdates(mLocationCallback);
        }
    }

    public void tryLogOut(View view) {
        FirebaseAuth.getInstance().signOut();

        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
        finish();
    }

    public void buttonRequestOrCancel(View view) {
        //customer clicks button for someone to pick up
        //database creates reference as customerpickup in database

        if (isRequested){
            isDriverFound = false;
            isRequested = false;
            driversDetailsLayout.setVisibility(View.GONE);
            geoQuery.removeAllListeners();
            driversLocationRef.removeEventListener(driverLocationRefListener);

            if (driversID != null){
                DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID).child("customerRequest");

                customerRef.removeValue();
                driversID = null;
                Log.d("RemoveValue", "buttonRequestOrCancel: remove value from customers request");
            }
            if (pickupMarker != null){
                pickupMarker.remove();
            }

            searchRadius = 1;
            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("customerRequest");
            GeoFire geoFire = new GeoFire(dbRef);
            geoFire.removeLocation(userID);
            mPickUpButton.setText("Ca");

        } else {
            Log.d("RemoveValue", "buttonRequestOrCancel: Wil it do it again?");
            RadioButton serviceButton = findViewById(radioGroupServiceType.getCheckedRadioButtonId());
            if (serviceButton != null){
                requestService = serviceButton.getText().toString();
            }
            mPickUpButton.setText(R.string.pick_me_up);
            isRequested = true;
            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("customerRequest");
            Log.d("pleaseWork", "gpickMeUp " + userID);
            //using geofire to create location with users id, and pushin it to ''table called customerPickup
            GeoFire geoFire = new GeoFire(dbRef);
            geoFire.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

            //putting a marker for customer from where to pick him up
            pickUpLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            pickupMarker = mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Pick me Up!")
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer_foreground)));

            mPickUpButton.setText(R.string.waitting_for_pickup);

            //getting losest driver
            getClosestDriver();
}

    }

    private int searchRadius = 1;
    private boolean isDriverFound = false;
    private String driversID;
    private void getClosestDriver() {

        //creating reference to databse to see aviable drivers
        DatabaseReference driversLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driversLocation);
        Log.d("pleaseWork", "getClosestDriver: wads aw  without");

        // trying to find closest driver within 1 radius (probably kilomiters)
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), searchRadius);
        Log.d("pleaseWork", "getClosestDriver: wads aw " + pickUpLocation.latitude);
        geoQuery.removeAllListeners();


        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.d("Pick Up", "before isDriverFound: " + 123);
                //check if driver is not found
                if (!isDriverFound && isRequested) {


                    DatabaseReference customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    ValueEventListener listener = customerDatabaseRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                                if (isDriverFound && isRequested) {
                                    return;
                                }
                                    Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                    if (driverMap.get("service") != null && driverMap.get("service").equals(requestService)) {
                                        isDriverFound = true;
                                        driversID = dataSnapshot.getKey();
                                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID).child("customerRequest");
                                        String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                        HashMap map = new HashMap();
                                        map.put("customerRideId", customerID);
                                        map.put("destination", customersDestination);
                                        map.put("destinationLat", destinationLatLog.latitude);
                                        map.put("destinationLng", destinationLatLog.longitude );
                                        driverRef.updateChildren(map);
                                        getDriversLocation();
                                        getDriversDetails();

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
                Log.d("Pick Up", "exited isDriverFound: " + 123);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d("Pick Up", "moved isDriverFound: " + 123);
            }

            //does this query until GeoQuery is ready
            @Override
            public void onGeoQueryReady() {
                Log.d("Pick Up", "before isDriverFound in query: " + 123);
                if (isDriverFound){
                    searchRadius++;

                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.d("Pick Up", "error isDriverFound: " + 123);
            }
        });
    }

    private void getDriversDetails() {

        DatabaseReference mCustomersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID);
        mCustomersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
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

                    driversDetailsLayout.setVisibility(View.VISIBLE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private Marker mDriversMarker;
    DatabaseReference driversLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriversLocation() {
        driversLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driversID).child("l");
        driverLocationRefListener = driversLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

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


                        Location locationDriver = new Location("");
                        locationDriver.setLatitude(driversLatLng.latitude);
                    locationDriver.setLongitude(driversLatLng.longitude);

                        Location locationCustomer = new Location("");
                    locationCustomer.setLatitude(pickUpLocation.latitude);
                    locationCustomer.setLongitude(pickUpLocation.longitude);

                        float distance = locationDriver.distanceTo(locationCustomer);


                        if (distance > 2){
                            mPickUpButton.setText("Distance: " + distance);
                        } else {
                            mPickUpButton.setText("Your Taxi is here!");
                        }


                        // .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_taxi_foreground))
                        mDriversMarker = mMap.addMarker(new MarkerOptions().position(driversLatLng).title("Your Taxi is on the way")
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_taxi_foreground)));


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void openProfile(View view) {
        Intent customerProfileIntent = new Intent(this, ProfileActivity.class);
        customerProfileIntent.putExtra("isDriver", false);
        startActivity(customerProfileIntent);

    }


}