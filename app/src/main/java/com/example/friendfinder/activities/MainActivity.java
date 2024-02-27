package com.example.friendfinder.activities;


import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.example.friendfinder.adapters.RecentConversationAdapter;
import com.example.friendfinder.databinding.ActivityMainBinding;
import com.example.friendfinder.listeners.ConversationListener;
import com.example.friendfinder.models.Chat;
import com.example.friendfinder.models.User;
import com.example.friendfinder.utilities.Constants;
import com.example.friendfinder.utilities.PreferanceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversationListener {

    private ActivityMainBinding binding;
    private PreferanceManager preferanceManager;
    private List<Chat> conversationList;
    private RecentConversationAdapter conversationAdapter;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferanceManager = new PreferanceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        queryConversations();
    }

    private void init(){
        conversationList = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(conversationList, this);
        binding.conversationRV.setAdapter(conversationAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners(){
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UserActivity.class)));
    }

    private void loadUserDetails(){
        binding.textName.setText(preferanceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferanceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void queryConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDERID, preferanceManager.getString(Constants.KEY_USERID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVERID, preferanceManager.getString(Constants.KEY_USERID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderid = documentChange.getDocument().getString(Constants.KEY_SENDERID);
                    String receiverid = documentChange.getDocument().getString(Constants.KEY_RECEIVERID);
                    Chat chat = new Chat();
                    chat.senderId = senderid;
                    chat.recieverId = receiverid;

                    if(preferanceManager.getString(Constants.KEY_USERID).equals(senderid)){
                        chat.conversationImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chat.conversationName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chat.conversationId = documentChange.getDocument().getString(Constants.KEY_RECEIVERID);
                    }
                    else{
                        chat.conversationImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chat.conversationName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chat.conversationId = documentChange.getDocument().getString(Constants.KEY_SENDERID);
                    }

                    chat.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chat.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversationList.add(chat);
                }
                else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for(int i = 0; i < conversationList.size(); i++){
                        String senderid = documentChange.getDocument().getString(Constants.KEY_SENDERID);
                        String recieverid = documentChange.getDocument().getString(Constants.KEY_RECEIVERID);

                        if(conversationList.get(i).senderId.equals(senderid) && conversationList.get(i).recieverId.equals(recieverid)){
                            conversationList.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversationList.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }

            Collections.sort(conversationList, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversationAdapter.notifyDataSetChanged();
            binding.conversationRV.smoothScrollToPosition(0);
            binding.conversationRV.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    //Giving the user a token
    private void updateToken(String token){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLEECTION_USERS).document(
                        preferanceManager.getString(Constants.KEY_USERID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update the token"));
    }

    //deleting the user token
    private void signOut(){
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLEECTION_USERS).document(
                        preferanceManager.getString(Constants.KEY_USERID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferanceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    public void onConversationClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}