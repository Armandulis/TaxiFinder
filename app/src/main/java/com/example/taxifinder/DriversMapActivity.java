package com.example.taxifinder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.taxifinder.Helpers.ValuesHelper;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriversMapActivity extends FragmentActivity
        implements OnMapReadyCallback, RoutingListener {

    // Maps
    private GoogleMap mMap;

    // Location
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProvider;
    private LatLng destinationLatLng, pickupLatLng;
    private Marker pickupMarker;
    // UI
    private LinearLayout customersDetailsLayout;
    private TextView textCustomerDetailsName, textCustomerDetailsPhone,textRideStatus, textCustomerDetailsDestination;
    private ImageView imageCustomerssPic;

    // Variables
    private String customersID = "", destination, driversID;
    private int status = 0;
    private boolean isLoggingOut = false;




    // Routing
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark,R.color.colorAccent,R.color.design_default_color_primary,R.color.primary_dark_material_light};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_map);


        // setting up values
        textCustomerDetailsName = findViewById(R.id.tvCustomerDetailsName);
        textCustomerDetailsPhone = findViewById(R.id.tvCustomerDetailsPhone);
        textCustomerDetailsDestination = findViewById(R.id.tvCustomerDetailsDestination);
        customersDetailsLayout = findViewById(R.id.customersLayoutProfile);
        imageCustomerssPic = findViewById(R.id.imageCustomerPicDetails);
        textRideStatus = findViewById(R.id.rideStatus);

        polylines = new ArrayList<>();

        driversID = FirebaseAuth.getInstance().getCurrentUser().getUid();
       // setting up map and requesting location
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ValuesHelper.PERMISSION_REQUEST_CODE);
        mFusedLocationProvider = LocationServices.getFusedLocationProviderClient(this);
        mapFragment.getMapAsync(this);

        getAssignedCustomer();
    }

    private void getAssignedCustomer() {
        //checking if there are any changes inside drivers customer request
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID).child("customerRequest").child("customerRideId");
        customerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    // if there is, we get following information
                    status = 1;
                    customersID = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickUpLocation();
                    getAssignedCustomerDetails();
                    getAssignedCustomerDestination();

                } else {

                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDestination() {
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID).child("customerRequest");
        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    // getting and setting up customer details
                    if(map.get("destination")!=null){
                        destination = map.get("destination").toString();
                        textCustomerDetailsDestination.setText("Destination " + destination);
                    }
                    else {
                        textCustomerDetailsDestination.setText("Destination: --");
                    }

                    //setting up destination, is 0.0 if customer had an error picking destination
                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if(map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng") != null){
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDetails() {

        DatabaseReference mCustomersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customersID);
            mCustomersDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        if (map.get("name") != null) {
                            textCustomerDetailsName.setText( map.get("name").toString());
                        }
                        if (map.get("phone") != null) {
                            textCustomerDetailsPhone.setText( map.get("phone").toString());
                        }
                        // glide is a library used to insert images into Imageview using Url/links
                        if (map.get("profileImageUri") != null) {
                            Glide.with(getApplication())
                                    .load(Uri.parse(map.get("profileImageUri").toString()))
                                    .into(imageCustomerssPic);
                        }

                        customersDetailsLayout.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
    }
    private  DatabaseReference customerPickUpLocationRef;
    private ValueEventListener  customerPickUpLocationRefListener;

    private void getAssignedCustomerPickUpLocation() {
        // l is geofire way of setting up lat and lng
        customerPickUpLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customersID).child("l");
        customerPickUpLocationRefListener = customerPickUpLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customersID.equals("")){
                   List<Object> map =(List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(0) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    // Letting driver know where to pick up customer
                    pickupLatLng  = new LatLng(locationLat, locationLng);

                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pick up Location"));
                    getRouteToCustomer(pickupLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getRouteToCustomer(LatLng pickupLatLng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .key(ValuesHelper.API_KEY)
                .alternativeRoutes(true)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
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

        mMap.setMyLocationEnabled(true);
        mFusedLocationProvider.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {

            List<Location> locationList  = locationResult.getLocations();
            mLastLocation = locationList.get(locationList.size() -1);
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference dbRefAvaibleDrivers = FirebaseDatabase.getInstance().getReference("driversAvailable");
            DatabaseReference dbRefDriversTaken = FirebaseDatabase.getInstance().getReference("driversWorking");
            GeoFire geoFireAvaible = new GeoFire(dbRefAvaibleDrivers);
            GeoFire geoFireTaken = new GeoFire(dbRefDriversTaken);

            //Checking if driver is active on app
            // using geo fire to set up drivers information
            if (customersID.equals("")){
                geoFireTaken.removeLocation(userID);
                geoFireAvaible.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            } else {
                geoFireAvaible.removeLocation(userID);
                geoFireTaken.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            }
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
                Toast.makeText(DriversMapActivity.this, "Cannot show your current location without Permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLoggingOut){
            disconnectDriver();
        }
    }

    private void disconnectDriver(){
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("driversAvailable");
        GeoFire geoFire = new GeoFire(dbRef);
        geoFire.removeLocation(userID);
    }
    public void tryLogOut(View view) {
        isLoggingOut = true;
        disconnectDriver();
        mFusedLocationProvider.removeLocationUpdates(mLocationCallback);

        if (customerPickUpLocationRefListener != null){
            customerPickUpLocationRef.removeEventListener(customerPickUpLocationRefListener);
        }
            FirebaseAuth.getInstance().signOut();

            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            startActivity(mainActivityIntent);
            finish();
    }

    public void openProfile(View view) {
        Intent profileIntent = new Intent(this, ProfileActivity.class);
        profileIntent.putExtra("isDriver", true);
        startActivity(profileIntent);
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e, Toast.LENGTH_SHORT).show();
        }
        else Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingStart() {

    }

    // Functions from library Google-Directions-Android
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteID) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    // switching between 'pickUpCustomer' and 'RideCompleted'
    public void switchStatus(View view){
        switch(status) {
            case 1:
                status = 2;
                deletePolylines();
                if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                    getRouteToCustomer(destinationLatLng);
                }
                textRideStatus.setText("ride completed");
                break;
            case 2:
                recordRide();
                endRide();
                break;
        }
    }

    private void endRide(){
        textRideStatus.setText("customer picked up");
        customersDetailsLayout.setVisibility(View.GONE);

        deletePolylines();

        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversID).child("customerRequest");
        driverRef.removeValue();

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(dbRef);
        geoFire.removeLocation(customersID);

        if (customerPickUpLocationRefListener != null){
            customerPickUpLocationRef.removeEventListener(customerPickUpLocationRefListener);
        }
        customersID = "";

        if (pickupMarker != null){
            pickupMarker.remove();
        }
    }

    private void recordRide(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customersID).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customersID);
        map.put("rating", 0);
        map.put("timestamp", getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);
        historyRef.child(requestId).updateChildren(map);
}

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void deletePolylines(){
        for (Polyline polyline : polylines ){
            polyline.remove();
        }
        polylines.clear();
    }
}
