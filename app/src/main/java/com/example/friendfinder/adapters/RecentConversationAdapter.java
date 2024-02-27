package com.example.friendfinder.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.friendfinder.databinding.ItemContainerRecentBinding;
import com.example.friendfinder.listeners.ConversationListener;
import com.example.friendfinder.models.Chat;
import com.example.friendfinder.models.User;

import java.util.List;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversationViewHolder>{

    private final List<Chat> chatList;
    private final ConversationListener conversationListener;

    public RecentConversationAdapter(List<Chat> chatList, ConversationListener conversationListener) {
        this.chatList = chatList;
        this.conversationListener = conversationListener;
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ConversationViewHolder(
                ItemContainerRecentBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        holder.setData(chatList.get(position));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {

        ItemContainerRecentBinding binding;

        ConversationViewHolder(ItemContainerRecentBinding itemContainerRecentBinding){
            super(itemContainerRecentBinding.getRoot());
            binding = itemContainerRecentBinding;
        }

        void setData(Chat chat){
            binding.imageProfile.setImageBitmap(getConversationImage(chat.conversationImage));
            binding.textName.setText(chat.conversationName);
            binding.textRecentMessage.setText(chat.message);
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chat.conversationId;
                user.name = chat.conversationName;
                user.image = chat.conversationImage;
                conversationListener.onConversationClicked(user);
            });
        }
    }

    private Bitmap getConversationImage(String encodedPic){
        byte[] bytes = Base64.decode(encodedPic, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
