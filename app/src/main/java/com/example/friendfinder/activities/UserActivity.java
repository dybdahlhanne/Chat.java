package com.example.friendfinder.activities;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.friendfinder.adapters.UserAdapter;
import com.example.friendfinder.databinding.ActivityUserBinding;
import com.example.friendfinder.listeners.UserListener;
import com.example.friendfinder.models.User;
import com.example.friendfinder.utilities.Constants;
import com.example.friendfinder.utilities.PreferanceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserActivity extends BaseActivity implements UserListener, AdapterView.OnItemSelectedListener {

    private ActivityUserBinding binding;
    private PreferanceManager preferanceManager;
    private UserAdapter userAdapter;
    private final String[] spinnerValues = {"Oslo", "Viken", "Innlandet", "Troms og Finnmark", "Nordland", "Trøndelag", "Møre og Romsdal", "Vestland", "Rogaland", "Agder", "Vestfold og Telemark"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferanceManager = new PreferanceManager(getApplicationContext());
        setListeners();

        Spinner spinner = binding.regionSpinner;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(this);

        userAdapter = new UserAdapter(new ArrayList<>(), this);
        binding.usersRecyclerView.setAdapter(userAdapter);

        //getUsers("");
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        clearUserRecyclerView();
        String selectedRegion = parent.getItemAtPosition(position).toString();
        getUsers(selectedRegion);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Handle when nothing is selected in the spinner if needed
    }

    private void clearUserRecyclerView() {
        userAdapter.clear(); // Assuming you have a clear method in your UserAdapter
        binding.usersRecyclerView.setVisibility(View.GONE);
    }

    private void getUsers(String region){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        Query query = database.collection(Constants.KEY_COLLEECTION_USERS)
                .whereEqualTo("region", region);

        query.get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferanceManager.getString(Constants.KEY_USERID);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<User> userList = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            userList.add(user);
                        }

                        if(userList.size() > 0){
                            binding.textErrorMessage.setVisibility(View.INVISIBLE);
                            UserAdapter userAdapter = new UserAdapter(userList, this);
                            binding.usersRecyclerView.setAdapter(userAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        }
                        else{
                            showErrorMessage();
                        }
                    }
                    else{
                        showErrorMessage();
                    }
                });

    }

    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s", "No user available to chat"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}