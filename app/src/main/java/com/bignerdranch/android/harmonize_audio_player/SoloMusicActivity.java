package com.bignerdranch.android.harmonize_audio_player;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.MediaController;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SoloMusicActivity extends AppCompatActivity {

    public static final String CONTROL = "fromSolo";
    TextView textview1;
    TextView textview2;
    ImageView next;
    ImageView prev;
    ImageView play;
    ImageView repeat;
    public static String  playPause  = "notPlaying";
    String repeating = "notRepeating";
    ImageView silent;
    Boolean muted = false;
    SeekBar seek;
    int seekbarChange = 0;
    FrameLayout fragmentLayout;



    private void sendControlIntent(String message){
        Log.i("Hey",message);
        Intent broadcastIntent = new Intent(CONTROL);
        broadcastIntent.putExtra("control",message);
        if(seekbarChange != 0){
            broadcastIntent.putExtra("seekBarChange",seekbarChange);
        }
        sendBroadcast(broadcastIntent);
    }

    private Handler handler = new Handler();


    private BroadcastReceiver getContentFromService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String controlMessage = intent.getExtras().getString("FromService");
            if(controlMessage.equals("nothingHere")){
                Log.d("hello","time " + intent.getExtras().getInt("duration") );
                seek.setMax(intent.getExtras().getInt("duration"));
            }
            else if(controlMessage.equals("sendMusicName")){
                textview1.setText(intent.getExtras().getString("MusicName"));
                textview2.setText(intent.getExtras().getString("musicArtist"));

            }
            else if(controlMessage.equals("currentPositionSeek")){
                Integer newPosition = intent.getExtras().getInt("duration");
                seek.setProgress(newPosition);
            }
            else if(controlMessage.equals("changingLayout")){
                textview1.setText(intent.getExtras().getString("resumingMusic"));
            }
            else{
                if(playPause.equals("notPlaying")){
                    Log.i("life","bitch upstairs");
                    play.setBackgroundResource(R.drawable.ic_pause_black_24dp);
                    playPause = "playing";
                }
                else{
                    Log.i("life","bitch from solo");
                    play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                    playPause = "notPlaying";
                }

            }
        }
    };

    private void register_getContentFromService() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter("SEND DATA");
        registerReceiver(getContentFromService, filter);
    }



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solo_music);
        register_getContentFromService();

        Bundle intentData = getIntent().getExtras();

        if(intentData == null){
            return;
        }
        SoloMusicActivity.this.runOnUiThread(new Runnable(){

            public void run(){
                sendControlIntent("getCurrentPosition");
                handler.postDelayed(this,1000);

            }

        });

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        final ImageFragment fragmentImage = new ImageFragment();
        final LyricsFragment lyricsFragment = new LyricsFragment();

       transaction.add(R.id.fragmentHolder, fragmentImage,"image");
        transaction.commit();

        fragmentLayout = (FrameLayout)findViewById(R.id.fragmentHolder);
        fragmentLayout.setOnLongClickListener(new FrameLayout.OnLongClickListener(){

            public boolean onLongClick(View v){

                if(getFragmentManager().findFragmentByTag("image") != null ){
                    Log.i("This","actually works");
                    getFragmentManager().beginTransaction().replace(R.id.fragmentHolder,lyricsFragment,"lyric").commit();
                }
                else{
                    getFragmentManager().beginTransaction().replace(R.id.fragmentHolder,fragmentImage,"image").commit();
                }
                return false;
            }
        });


        seek = (SeekBar)findViewById(R.id.seek_bar);


        int position = intentData.getInt("musicPosition");
        String musicName = intentData.getString("musicName");

        textview1 = (TextView) findViewById(R.id.musicName);
        textview2 = (TextView) findViewById(R.id.musicArtist);

        Log.d("Hey","drum" + position);
        textview1.setText(musicName);
        textview2.setText(MainActivity.musicArtist);
        playPause = "playing";


        play = (ImageView) findViewById(R.id.play_button);
        play.setBackgroundResource(R.drawable.ic_pause_black_24dp);
        play.setOnClickListener(new ImageView.OnClickListener(){

            public void onClick(View v){

                if(playPause.equals("notPlaying")){
                    sendControlIntent("Play");
                    play.setBackgroundResource(R.drawable.ic_pause_black_24dp);
                    playPause = "playing";
                }
                else{
                    sendControlIntent("Pause");
                    play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                    playPause = "notPlaying";
                }
            }
        });

        next = (ImageView) findViewById(R.id.next_button);
        next.setOnClickListener(new ImageView.OnClickListener() {
                                    public void onClick(View V) {
                                        sendControlIntent("Next");


                                    }
                                }

        );

        prev = (ImageView) findViewById(R.id.prev_button);
        prev.setOnClickListener(new ImageView.OnClickListener(){
            public void onClick(View v){
                sendControlIntent("Prev");

            }
        }
        );

        repeat = (ImageView) findViewById(R.id.repeat);
        repeat.setOnClickListener(new ImageView.OnClickListener(){
            public void onClick(View v){
                if(repeating.equals("notRepeating")){
                    sendControlIntent("repeat");
                    Toast.makeText(repeat.getContext(), "Repeating Current Music",
                            Toast.LENGTH_SHORT).show();
                    repeating = "repeating";
                    repeat.setBackgroundResource(R.drawable.ic_repeat_one_black_24dp);
                }
                else if(repeating.equals("shuffle")){
                    sendControlIntent("stopRepeat");
                    Toast.makeText(repeat.getContext(), "Repeating Off",
                            Toast.LENGTH_SHORT).show();
                    repeating = "notRepeating";
                    repeat.setBackgroundResource(R.drawable.ic_repeat_black_24dp);

                }
                else {
                    sendControlIntent("shuffle");
                    Toast.makeText(repeat.getContext(), "Shuffling",
                            Toast.LENGTH_SHORT).show();
                    repeating = "shuffle";
                    repeat.setBackgroundResource(R.drawable.ic_shuffle_black_24dp);

                }


            }

        });
        silent = (ImageView) findViewById(R.id.mute);
        silent.setOnClickListener(new ImageView.OnClickListener(){
            public void onClick(View v){
                if(!muted){
                    silent.setBackgroundResource(R.drawable.ic_volume_mute_black_24dp);
                    sendControlIntent("silenceMusic");
                    muted = true;
                }
                else{
                    silent.setBackgroundResource(R.drawable.ic_volume_up_black_24dp);
                    sendControlIntent("unSilenceMusic");
                    muted = false;
                }

            }
        });
seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser){
            Log.d("progress","current " + progress);
            seekbarChange = progress;
            sendControlIntent("seekChange");
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
});
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
            outState.putBoolean("playPause",MediaPlayerService.isPlaying);
            outState.putString("audibility",MediaPlayerService.audibility);
            if(MediaPlayerService.shuffle ){
                outState.putString("shuffle","shuffle");
            }
            else{
                if(MediaPlayerService.repeat.equals("stopRepeat")){
                    outState.putString("shuffle","stopRepeat");
                }
                else{
                    outState.putString("shuffle","repeat");
                }
            }

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null){
            if(!savedInstanceState.getBoolean("playPause")) {
                Log.i("life","faatBitch");
                play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
            }
            else{
                play.setBackgroundResource(R.drawable.ic_pause_black_24dp);
            }

            if(savedInstanceState.getString("audibility").equals("notSilent")) {
                silent.setBackgroundResource(R.drawable.ic_volume_up_black_24dp);
            }
            else{
                silent.setBackgroundResource(R.drawable.ic_volume_mute_black_24dp);
            }

            if(savedInstanceState.getString("shuffle").equals("shuffle")) {
                repeat.setBackgroundResource(R.drawable.ic_shuffle_black_24dp);
            }
            else{
                if(savedInstanceState.getString("shuffle").equals("stopRepeat")){
                    repeat.setBackgroundResource(R.drawable.ic_repeat_black_24dp);

                }
                else{
                    repeat.setBackgroundResource(R.drawable.ic_repeat_one_black_24dp);

                }

            }
            sendControlIntent("changingLayout");
            Log.i("yeah","im here");

        }
        else{
            if(MediaPlayerService.audibility.equals("notSilent")) {
                silent.setBackgroundResource(R.drawable.ic_volume_up_black_24dp);
            }
            else{
                silent.setBackgroundResource(R.drawable.ic_volume_mute_black_24dp);
            }
            if(MediaPlayerService.shuffle.equals("shuffle")) {
                repeat.setBackgroundResource(R.drawable.ic_shuffle_black_24dp);
            }
            else{
                if(MediaPlayerService.repeat.equals("stopRepeat")){
                    repeat.setBackgroundResource(R.drawable.ic_repeat_black_24dp);

                }
                else{
                    repeat.setBackgroundResource(R.drawable.ic_repeat_one_black_24dp);

                }

            }

        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(MediaPlayerService.audibility.equals("notSilent")) {
            silent.setBackgroundResource(R.drawable.ic_volume_up_black_24dp);
            muted = false;
        }
        else{
            silent.setBackgroundResource(R.drawable.ic_volume_mute_black_24dp);
            muted = true;
        }
        if(MediaPlayerService.shuffle.equals("shuffle")) {
            repeat.setBackgroundResource(R.drawable.ic_shuffle_black_24dp);
            repeating = "shuffle";
        }
        else{
            if(MediaPlayerService.repeat.equals("stopRepeat")){
                repeat.setBackgroundResource(R.drawable.ic_repeat_black_24dp);
                repeating = "notRepeating";

            }
            else{
                repeat.setBackgroundResource(R.drawable.ic_repeat_one_black_24dp);
                repeating = "repeating";

            }

        }

    }

}
