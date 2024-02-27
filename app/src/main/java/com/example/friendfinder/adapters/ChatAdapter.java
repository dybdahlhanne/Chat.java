package com.example.friendfinder.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.friendfinder.databinding.ItemContainerReceivedMessageBinding;
import com.example.friendfinder.databinding.ItemContainerSentMessageBinding;
import com.example.friendfinder.models.Chat;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final List<Chat> chatList;
    private final Bitmap recievedProfilePic;
    private final String senderId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECIEVED = 2;

    public ChatAdapter(List<Chat> chatList, Bitmap recievedProfilePic, String senderId) {
        this.chatList = chatList;
        this.recievedProfilePic = recievedProfilePic;
        this.senderId = senderId;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SENT){
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
        else{
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position) == VIEW_TYPE_SENT){
            ((SentMessageViewHolder) holder).setData(chatList.get(position));
        }
        else {
            ((ReceivedMessageViewHolder) holder).setData(chatList.get(position), recievedProfilePic);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatList.get(position).senderId.equals(senderId)){
            return VIEW_TYPE_SENT;
        }
        else{
            return VIEW_TYPE_RECIEVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder{

        private final ItemContainerSentMessageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding){
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(Chat chat){
            binding.textMessage.setText(chat.message);
            binding.textDateTime.setText(chat.dateTime);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding){
            super((itemContainerReceivedMessageBinding.getRoot()));
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(Chat chat, Bitmap recievedProfilePic){
            binding.textMessage.setText(chat.message);
            binding.textDateTime.setText(chat.dateTime);
            binding.imageProfile.setImageBitmap(recievedProfilePic);
        }
    }
}
