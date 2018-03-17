package com.bignerdranch.android.harmonize_audio_player;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Mekre on 3/16/2018.
 */

public class lyric_view_holder extends RecyclerView.ViewHolder {

    TextView line;

    public lyric_view_holder(View itemView) {
        super(itemView);

        line  = (TextView)itemView.findViewById(R.id.singeLine);

    }
}
