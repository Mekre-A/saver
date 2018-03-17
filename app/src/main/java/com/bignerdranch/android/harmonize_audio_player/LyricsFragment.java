package com.bignerdranch.android.harmonize_audio_player;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mekre on 3/16/2018.
 */

public class LyricsFragment extends Fragment {

    TextView lyricView;
    List<String> lyricTimes = new ArrayList<String>();

    @Override
    public void onCreate(Bundle state){
        super.onCreate(state);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstanceState){

        View view = inflater.inflate(R.layout.lyrics_fragment,container,false);

        lyricView = (TextView) view.findViewById(R.id.lyricView);

        File file = new File(Environment.getExternalStorageDirectory(),"Lyrics/Lyrics.json");

        StringBuilder lyric = new StringBuilder();

        // Collect JSON formatted lyric and time from file and add it into a stringbuilder object

        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine())!= null){

                lyric.append(line);
                lyric.append('\n');
            }
            br.close();
        }

        catch (IOException e){

        }

        lyricView.setText(lyric);

        //Collect individual lines and put them into an array

        try {
            JSONArray json = new JSONArray(lyric.toString());

            JSONObject obj;
            for(int i = 0; i < json.length();i++){
                obj = json.getJSONObject(i);
                Log.i("heyhey","lifeeeeeee \n" + obj);

                lyricTimes.add(obj.getString((String)obj.keys().next()));
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

        //populate each line of lyric into a recyclerview by passing the array to the adapter

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        lyric_view_adapter adapter = new lyric_view_adapter(lyricTimes,getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }


}
