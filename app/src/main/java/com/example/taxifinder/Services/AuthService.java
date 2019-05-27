package com.example.taxifinder.Services;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthService {


    public void userSignOut(){
        FirebaseAuth.getInstance().signOut();
    }

}
