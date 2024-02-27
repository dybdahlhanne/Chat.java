package com.example.friendfinder.activities;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import com.example.friendfinder.adapters.ChatAdapter;
import com.example.friendfinder.databinding.ActivityChatBinding;
import com.example.friendfinder.models.Chat;
import com.example.friendfinder.models.User;
import com.example.friendfinder.utilities.Constants;
import com.example.friendfinder.utilities.PreferanceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User reciever;
    private List<Chat> chatList;
    private ChatAdapter chatAdapter;
    private PreferanceManager preferanceManager;
    private FirebaseFirestore database;
    private String conversationId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadRecieverDetails();
        init();
        listenOnMessages();
    }

    private void init(){
        preferanceManager = new PreferanceManager(getApplicationContext());
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatList,
                getBitmap(reciever.image),
                preferanceManager.getString(Constants.KEY_USERID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void send(){
        HashMap<String,Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDERID, preferanceManager.getString(Constants.KEY_USERID));
        message.put(Constants.KEY_RECEIVERID, reciever.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if(conversationId != null){
            updateConversation(binding.inputMessage.getText().toString());
        }
        else{
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDERID, preferanceManager.getString(Constants.KEY_USERID));
            conversation.put(Constants.KEY_SENDER_NAME, preferanceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferanceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_RECEIVERID, reciever.id);
            conversation.put(Constants.KEY_RECEIVER_NAME, reciever.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE, reciever.image);
            conversation.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            addConversation(conversation);
        }

        binding.inputMessage.setText(null);
    }

    private void listenAvailabilityReceiver(){
        database.collection(Constants.KEY_COLLEECTION_USERS).document(
                reciever.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if(error != null){
                return;
            }
            if(value != null){
                if(value.getLong(Constants.KEY_AVAILABLE) != null){
                    int available = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABLE)).intValue();
                    isReceiverAvailable = available == 1;
                }
                reciever.token = value.getString(Constants.KEY_FCM_TOKEN);
            }
            if(isReceiverAvailable){
                binding.textAvailability.setVisibility(View.VISIBLE);
            }
            else{
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }

    private void listenOnMessages(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDERID, preferanceManager.getString(Constants.KEY_USERID))
                .whereEqualTo(Constants.KEY_RECEIVERID, reciever.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDERID, reciever.id)
                .whereEqualTo(Constants.KEY_RECEIVERID, preferanceManager.getString(Constants.KEY_USERID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            int count = chatList.size();
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    Chat chat = new Chat();
                    chat.senderId = documentChange.getDocument().getString(Constants.KEY_SENDERID);
                    chat.recieverId = documentChange.getDocument().getString(Constants.KEY_RECEIVERID);
                    chat.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chat.dateTime = getDateReadable(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chat.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatList.add(chat);
                }
            }

            Collections.sort(chatList, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));

            if(count == 0){
                chatAdapter.notifyDataSetChanged();
            }
            else{
                chatAdapter.notifyItemRangeInserted(chatList.size(), chatList.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatList.size() - 1);
            }

            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }

        binding.progressBar.setVisibility(View.GONE);

        if(conversationId == null){
            checkForConversation();
        }
    };

    private Bitmap getBitmap(String encodedPic){
        byte[] bytesList = Base64.decode(encodedPic, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytesList, 0, bytesList.length);
    }

    private void loadRecieverDetails(){
        reciever = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(reciever.name);
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> send());
    }

    private String getDateReadable(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversation(HashMap<String, Object> conversation){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    private void updateConversation(String msg){
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, msg,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversation(){
        if(chatList.size() != 0 ){
            checkForConversationRemotely(
                    preferanceManager.getString(Constants.KEY_USERID),
                    reciever.id
            );
            checkForConversationRemotely(
                    reciever.id,
                    preferanceManager.getString(Constants.KEY_USERID)
            );
        }
    }

    private void checkForConversationRemotely(String senderId, String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDERID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVERID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompletListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationOnCompletListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityReceiver();
    }
}