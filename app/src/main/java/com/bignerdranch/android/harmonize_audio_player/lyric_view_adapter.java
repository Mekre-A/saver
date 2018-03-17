package com.bignerdranch.android.harmonize_audio_player;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

/**
 * Created by Mekre on 3/16/2018.
 */

public class lyric_view_adapter extends RecyclerView.Adapter<lyric_view_holder> {

    List<String> list = Collections.emptyList();
    Context context;

    public lyric_view_adapter(List<String> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public lyric_view_holder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.lyric_view_layout, parent, false);
        lyric_view_holder holder = new lyric_view_holder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(lyric_view_holder holder, int position) {

        holder.line.setText(list.get(position));

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
