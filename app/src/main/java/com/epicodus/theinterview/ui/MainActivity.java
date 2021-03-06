package com.epicodus.theinterview.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.epicodus.theinterview.Constants;
import com.epicodus.theinterview.R;
import com.epicodus.theinterview.adapters.FirebaseChatViewHolder;
import com.epicodus.theinterview.models.Chat;
import com.epicodus.theinterview.models.User;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @Bind(R.id.chatList) RecyclerView mChatList;
    @Bind(R.id.activityTitle) TextView mActivityTitle;
    @Bind(R.id.startButton) Button mStartButton;

    private DatabaseReference mChatReference;
    private FirebaseRecyclerAdapter mFirebaseAdapter;
    private ProgressDialog mAuthProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        createAuthProgressDialog();

        Typeface gravitas = Typeface.createFromAsset(getAssets(), "fonts/gravitas-one.regular.ttf");
        mActivityTitle.setTypeface(gravitas);
        mStartButton.setTypeface(gravitas);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user.getUid();

        mChatReference = FirebaseDatabase
                .getInstance()
                .getReference(Constants.FIREBASE_CHAT_REFERENCE)
                .child(uid);

        setUpFirebaseAdapter();

        mStartButton.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        if (id == R.id.feedback){
            Intent intent = new Intent(this, FeedbackActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFirebaseAdapter.cleanup();
    }

    @Override
    public void onClick(View v) {
        if (v == mStartButton){
            final ArrayList<String> userIds = new ArrayList<>();
            final ArrayList<String> chatUserList = new ArrayList<>();

            mAuthProgressDialog.show();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            final String uid = user.getUid();
            chatUserList.add(uid);

            DatabaseReference mUserRef = FirebaseDatabase
                    .getInstance()
                    .getReference(Constants.FIREBASE_USER_REFERENCE);

            //get array of users in firebase
            mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                        String userId = snapshot.getValue(User.class).getuId();
                        if (!userId.equals(uid)){
                            userIds.add(snapshot.getValue(User.class).getuId());
                        }

                    }

                    //generate random user from user array
                    String mRandomUser = generateRandomUser(userIds);

                    chatUserList.add(mRandomUser);

                    //randomly generate hiring manager from array list
                    String hiringManager = generateRandomUser(chatUserList);
                    int hiringManagerPosition = chatUserList.indexOf(hiringManager);

                    String interviewee;
                    if (hiringManagerPosition == 0) {
                        interviewee = chatUserList.get(1);
                    } else {
                        interviewee = chatUserList.get(0);
                    }

                    Chat chat = new Chat(hiringManager, interviewee);

                    //get database reference to save chat for hiring manager user
                    DatabaseReference hmRef = FirebaseDatabase
                            .getInstance()
                            .getReference(Constants.FIREBASE_CHAT_REFERENCE)
                            .child(hiringManager);

                    //get key for database reference
                    DatabaseReference pushRef = hmRef.push();
                    String pushId = pushRef.getKey();
                    chat.setHiringManagerChatId(pushId);

                    //get database reference to save chat for interviewee user
                    DatabaseReference iRef = FirebaseDatabase
                            .getInstance()
                            .getReference(Constants.FIREBASE_CHAT_REFERENCE)
                            .child(interviewee);

                    //get key for database reference
                    DatabaseReference tempRef = iRef.push();
                    String tempId = tempRef.getKey();
                    chat.setIntervieweeChatId(tempId);

                    //save chat for each user
                    pushRef.setValue(chat);
                    tempRef.setValue(chat);

                    Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    intent.putExtra("chat", Parcels.wrap(chat));
                    mAuthProgressDialog.dismiss();
                    startActivity(intent);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String generateRandomUser(ArrayList<String> userIds) {
        Random randomNum = new Random();
        String randomUserId = userIds.get(randomNum.nextInt(userIds.size()));
        return randomUserId;
    }

    private void createAuthProgressDialog() {
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle("Loading");
        mAuthProgressDialog.setMessage("Setting up interview");
        mAuthProgressDialog.setCancelable(false);
    }

    private void setUpFirebaseAdapter(){
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Chat, FirebaseChatViewHolder>(Chat.class, R.layout.chat_list_item, FirebaseChatViewHolder.class, mChatReference) {
            @Override
            protected void populateViewHolder(FirebaseChatViewHolder viewHolder, Chat model, int position) {
                viewHolder.bindChat(model);
            }
        };

        mChatList.setHasFixedSize(true);
        mChatList.setLayoutManager(new LinearLayoutManager(this));
        mChatList.setAdapter(mFirebaseAdapter);
    }

//    private String checkRandomUsersChatList(ArrayList<String> userIds){
//        final ArrayList<Chat> mChats = new ArrayList<>();
//
//        final String randomUser = generateRandomUser(userIds);
//
//        //get database reference for random user's chat list
//        DatabaseReference mChatRef = FirebaseDatabase
//                .getInstance()
//                .getReference(Constants.FIREBASE_CHAT_REFERENCE)
//                .child(randomUser);
//
//        //get array of random user's chats
//        mChatRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
//                    mChats.add(snapshot.getValue(Chat.class));
//                }
//
//                //check if user has active chat
//                for (int i = 0; i < mChats.size(); i++){
//                    if (mChats.get(i).isActive()){
//
//                       break;
//                    }
//                }
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
//        return randomUser;
//    }
}
