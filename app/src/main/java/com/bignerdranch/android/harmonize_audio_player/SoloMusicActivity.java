package com.bignerdranch.android.harmonize_audio_player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.MediaController;
import android.widget.Toast;

import org.json.JSONObject;

public class SoloMusicActivity extends AppCompatActivity {

    public static final String CONTROL = "fromSolo";
    TextView textview1;
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
            }
            else if(controlMessage.equals("currentPositionSeek")){
                Integer newPosition = intent.getExtras().getInt("duration");
                seek.setProgress(newPosition);
            }
            else{
                if(playPause.equals("notPlaying")){
                    play.setBackgroundResource(R.drawable.ic_pause_black_24dp);
                    playPause = "playing";
                }
                else{
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


        seek = (SeekBar)findViewById(R.id.seek_bar);


        int position = intentData.getInt("musicPosition");
        String musicName = intentData.getString("musicName");

        textview1 = (TextView) findViewById(R.id.musicName);
        Log.d("Hey","drum" + position);
        textview1.setText(musicName);
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
                    Toast.makeText(repeat.getContext(), "Repeating Current Music",
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

}
