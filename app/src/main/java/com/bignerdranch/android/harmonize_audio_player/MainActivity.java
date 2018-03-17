package com.bignerdranch.android.harmonize_audio_player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;



import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MediaPlayerService player;

    private static final int requestCode = 101;

    boolean serviceBound  = false;

    ArrayList<Audio> audioList;

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.bignerdranch.android.harmonize_audio_player";

    private ServiceConnection serviceConnection  = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;

            player  = binder.getService();
            serviceBound = true;

            Toast.makeText(MainActivity.this,"Service Bound",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    private boolean loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                // Save to audioList
                audioList.add(new Audio(data, title, album, artist));
            }
            cursor.close();
            return true;
        }
        else{
            return false;
        }

    }

    private BroadcastReceiver playSelectedAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int position = intent.getIntExtra("position",0);

            Log.d("hello","now " + position);

            playAudio(position);

        }
    };


    private void register_playSelectedMusic() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(View_Holder.Broadcast_SELECTED_AUDIO
        );
        registerReceiver(playSelectedAudio, filter);
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void makeRequest(){
        ActivityCompat.requestPermissions(this,new String[]{com.bignerdranch.android.harmonize_audio_player.Manifest.permission.READ_EXTERNAL_STORAGE},requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int request,String permission[], int[] grantResults){
        switch (request){

            case requestCode:{
                if(grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){

                }
                else{
                    initiateUI();
                    register_playSelectedMusic();

                }
                return;
            }
        }
    }
    protected void initiateUI(){
        if (loadAudio()) {
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
            Recycler_View_Adapter adapter = new Recycler_View_Adapter(audioList, getApplication());
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

        } else {

        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int permission = ContextCompat.checkSelfPermission(this, com.bignerdranch.android.harmonize_audio_player.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest();
        } else {
            initiateUI();
            register_playSelectedMusic();
        }
    }
}
