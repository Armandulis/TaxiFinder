package com.example.taxifinder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.taxifinder.Helpers.ValuesHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // UI
    private EditText metName, metPhone, metCar;
    private ImageView mImageProfilePic;
    private RadioGroup radioGroupServiceType;

    // Variables
    private FirebaseAuth mAuth;
    private DatabaseReference mCustomersDatabase;
    private String name, phone, profileImageUri, userID;
    private Uri resultUri;
    private boolean isDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        isDriver = getIntent().getBooleanExtra("isDriver", false);

        metName = findViewById(R.id.etUsersName);
        metPhone = findViewById(R.id.etUsersPhone);
        metCar = findViewById(R.id.etUsersCar);
        radioGroupServiceType = findViewById(R.id.radioGroupServiceType);

        mAuth = FirebaseAuth.getInstance();
        mImageProfilePic = findViewById(R.id.imageCustomerPic);
        mImageProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, ValuesHelper.START_GALARY_ACTIVITY);
            }
        });

        userID = mAuth.getCurrentUser().getUid();
        if (isDriver){
            mCustomersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userID);
        } else {
            mCustomersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userID);
            metCar.setVisibility(View.GONE);
            radioGroupServiceType.setVisibility(View.GONE);
        }
        setUpCustomersData();
    }

    public void btnSave(View view) {
        saveUsersData();
    }

    private void saveUsersData() {
        Map userInfo = new HashMap();
        userInfo.put("name", metName.getText().toString());
        userInfo.put("phone", metPhone.getText().toString());
        RadioButton serviceButton = findViewById(radioGroupServiceType.getCheckedRadioButtonId());
        if (serviceButton != null){
            userInfo.put("service", serviceButton.getText().toString());
        } else return;

        mCustomersDatabase.updateChildren(userInfo);

        if (resultUri != null){
            StorageReference dbImageReference = FirebaseStorage.getInstance().getReference().child("ProfileImages").child(userID);

            Bitmap bitmapOfUri = null;
            try{
                bitmapOfUri = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream compression = new ByteArrayOutputStream();
            bitmapOfUri.compress(Bitmap.CompressFormat.JPEG, 25, compression);

            byte[] data = compression.toByteArray();
            final UploadTask uploadTask = dbImageReference.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ProfileActivity.this, "Something went wrong adding picture, please try again later", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    StorageReference ref = FirebaseStorage.getInstance().getReference().child("ProfileImages").child(userID);
                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            Map newImage = new HashMap();
                            newImage.put("profileImageUri", uri.toString());
                            mCustomersDatabase.updateChildren(newImage);

                            finish();
                        }
                    });

                }
            });
        } else {

            finish();
        }
    }

    private void setUpCustomersData(){
        mCustomersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        name = map.get("name").toString();
                        metName.setText(name);
                    }
                    if (map.get("phone") != null) {
                        phone = map.get("phone").toString();
                        metPhone.setText(phone);
                    }
                    if (map.get("car") != null && isDriver) {
                        metCar.setText(map.get("car").toString());
                    }
                    if (map.get("profileImageUri") != null) {
                        profileImageUri = map.get("profileImageUri").toString();
                        Glide.with(getApplication())
                                .load(Uri.parse(profileImageUri))
                                .into(mImageProfilePic);
                    }
                    if (map.get("service") != null) {
                        String service = map.get("service").toString();
                        switch (service){
                            case "Taxi":
                                radioGroupServiceType.check(R.id.radioButtonTaxi);
                                break;
                            case "Uber":
                                radioGroupServiceType.check(R.id.radioButtonUber);
                                break;
                            case "Pick up truck":
                                radioGroupServiceType.check(R.id.radioButtonPickUpTruck);
                                break;
                            default:
                                break;
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void btnBack(View view) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ValuesHelper.START_GALARY_ACTIVITY && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri =imageUri;
            mImageProfilePic.setImageURI(resultUri);
        }
    }
}
