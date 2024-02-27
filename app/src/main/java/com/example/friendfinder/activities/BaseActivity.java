package com.example.friendfinder.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.friendfinder.utilities.Constants;
import com.example.friendfinder.utilities.PreferanceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseActivity extends AppCompatActivity {

    private DocumentReference documentReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferanceManager preferanceManager = new PreferanceManager(getApplicationContext());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        documentReference = database.collection(Constants.KEY_COLLEECTION_USERS)
                .document(preferanceManager.getString(Constants.KEY_USERID));
    }

    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(Constants.KEY_AVAILABLE, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        documentReference.update(Constants.KEY_AVAILABLE, 1);
    }
}
