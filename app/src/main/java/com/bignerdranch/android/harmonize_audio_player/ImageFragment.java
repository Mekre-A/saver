package com.bignerdranch.android.harmonize_audio_player;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Mekre on 3/16/2018.
 */

public class ImageFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState){
        View view  = inflator.inflate(R.layout.image_fragment,container,false);
        return view;
    }
}
