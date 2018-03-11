package com.bignerdranch.android.harmonize_audio_player;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.MediaController;

public class SoloMusicActivity extends AppCompatActivity {

    public static final String CONTROL = "fromSolo";
    TextView textview1;
    Button next;
    Button prev;
    Button pause;
    Button play;

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



        play = (Button)findViewById(R.id.play_button);
        play.setOnClickListener(new Button.OnClickListener(){

            public void onClick(View v){
                sendControlIntent("Play");
            }
        });

        pause = (Button)findViewById(R.id.pause_button);
        pause.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v){
                sendControlIntent("Pause");
            }
        });

        next = (Button)findViewById(R.id.next_button);
        next.setOnClickListener(new Button.OnClickListener() {
                                    public void onClick(View V) {
                                        sendControlIntent("Next");
                                    }
                                }

        );

        prev = (Button)findViewById(R.id.prev_button);
        prev.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v){
                sendControlIntent("Prev");
            }
        }
        );

    }

}
