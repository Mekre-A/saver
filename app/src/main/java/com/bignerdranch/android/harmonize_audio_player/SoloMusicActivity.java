package com.bignerdranch.android.harmonize_audio_player;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.MediaController;
import android.widget.Toast;

public class SoloMusicActivity extends AppCompatActivity {

    public static final String CONTROL = "fromSolo";
    TextView textview1;
    ImageView next;
    ImageView prev;
    ImageView play;
    ImageView repeat;
    String playPause  = "notPlaying";
    Boolean repeating = false;
    ImageView silent;

    private void sendControlIntent(String message){
        Log.i("Hey",message);
        Intent broadcastIntent = new Intent(CONTROL);
        broadcastIntent.putExtra("control",message);
        sendBroadcast(broadcastIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solo_music);

        Bundle intentData = getIntent().getExtras();

        if(intentData == null){
            return;
        }

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
                if(!repeating){
                    sendControlIntent("repeat");
                    Toast.makeText(repeat.getContext(), "Repeating Current Music",
                            Toast.LENGTH_SHORT).show();
                    repeating = true;
                    repeat.setBackgroundResource(R.drawable.ic_repeat_one_black_24dp);
                }
                else{
                    sendControlIntent("stopRepeat");
                    Toast.makeText(repeat.getContext(), "Repeating Canceled",
                            Toast.LENGTH_SHORT).show();
                    repeating = false;
                    repeat.setBackgroundResource(R.drawable.ic_repeat_black_24dp);
                }


            }

        });
        silent = (ImageView) findViewById(R.id.mute);
        silent.setOnClickListener(new ImageView.OnClickListener(){
            public void onClick(View v){

            }
        });

    }

}
