package com.bignerdranch.android.harmonize_audio_player;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Mekre on 3/16/2018.
 */

public class lyric_view_holder extends RecyclerView.ViewHolder {

    TextView line;
    CardView cv;

    public lyric_view_holder(View itemView) {
        super(itemView);

        cv = (CardView) itemView.findViewById(R.id.cardViewLyric);
        line  = (TextView)itemView.findViewById(R.id.singeLine);

    }
}
