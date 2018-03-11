package com.bignerdranch.android.harmonize_audio_player;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.IntentFilter;


/**
 * Created by Mekre on 2/23/2018.
 */

public class View_Holder extends RecyclerView.ViewHolder {

    CardView cv;
    TextView title;
    TextView description;
    TextView titleForSoloActivity;
    ImageView imageView;
    public static final String Broadcast_SELECTED_AUDIO = "play_Selected_audio";





    View_Holder(final View itemView) {
        super(itemView);
        cv = (CardView) itemView.findViewById(R.id.cardView);
        title = (TextView) itemView.findViewById(R.id.title);
        description = (TextView) itemView.findViewById(R.id.description);
//        imageView = (ImageView) itemView.findViewById(R.id.imageView);
        itemView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                int position = getAdapterPosition();

                Intent broadcastIntent = new Intent(Broadcast_SELECTED_AUDIO);
                broadcastIntent.putExtra("position",position);
                itemView.getContext().sendBroadcast(broadcastIntent);
                titleForSoloActivity = (TextView) itemView.findViewById(R.id.title);

                Intent i = new Intent(itemView.getContext(),SoloMusicActivity.class);
                i.putExtra("musicPosition",position);
                i.putExtra("musicName",titleForSoloActivity.getText().toString());
                itemView.getContext().startActivity(i);

            }
        });
    }

}
