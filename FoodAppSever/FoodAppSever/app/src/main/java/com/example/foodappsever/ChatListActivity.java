package com.example.foodappsever;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.foodappsever.callback.IRecyclerClickListener;
import com.example.foodappsever.common.Common;
import com.example.foodappsever.model.ChatInfoModel;
import com.example.foodappsever.view_holder.ChatListViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChatListActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler_chat_list)
    RecyclerView recycler_chat_list;
    FirebaseDatabase database;
    DatabaseReference chatRef;

    FirebaseRecyclerAdapter<ChatInfoModel, ChatListViewHolder> adapter;
    FirebaseRecyclerOptions<ChatInfoModel> options;
    LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        initViews();
        loadListChat();
    }

    private void loadListChat() {
        adapter=new FirebaseRecyclerAdapter<ChatInfoModel, ChatListViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ChatListViewHolder holder, int position, @NonNull ChatInfoModel model) {
                holder.txt_email.setText(new StringBuilder(model.getCreateName()));
                holder.txt_chat_message.setText(new StringBuilder(model.getLastMessage()));

                holder.setListener((view, pos) -> {
                    //Toast.makeText(ChatListActivity.this,model.getLastMessage(),Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(ChatListActivity.this,ChatDetailActivity.class);
                    intent.putExtra(Common.KEY_ROOM_ID,adapter.getRef(position).getKey());
                    intent.putExtra(Common.KEY_CHAT_USER,model.getCreateName());
                    startActivity(intent);
                });
            }

            @NonNull
            @Override
            public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ChatListViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_message_list_item,parent,false));
            }
        };
        recycler_chat_list.setAdapter(adapter);
    }

    private void initViews() {
        ButterKnife.bind(this);
        database=FirebaseDatabase.getInstance();
        chatRef=database.getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.CHAT_REF);
        Query query=chatRef;
        options=new FirebaseRecyclerOptions.Builder<ChatInfoModel>()
                .setQuery(query,ChatInfoModel.class)
                .build();
        layoutManager=new LinearLayoutManager(this);
        recycler_chat_list.setLayoutManager(layoutManager);
        recycler_chat_list.addItemDecoration(new DividerItemDecoration(this,layoutManager.getOrientation()));
        toolbar.setTitle(Common.currentServerUser.getRestaurant());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(view -> {
            finish();
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(adapter!=null)
            adapter.startListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(adapter!=null)
            adapter.startListening();
    }

    @Override
    protected void onStop() {
        if(adapter!=null)
            adapter.stopListening();
        super.onStop();
    }
}