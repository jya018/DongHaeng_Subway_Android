package com.example.myapplication.Activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class TimeTableFragment3 extends Fragment {

    String lineNm3;
    String name3;
    String line3;
    String[] sto3;
    String[] size3 = new String[15];
    int a=0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_time_table3, container, false);



        Bundle bundle = getArguments();
        if (bundle != null) {
            lineNm3 = bundle.getString("lineNm");
            name3 = bundle.getString("name");
        }

        try{
            String fileNm3 = lineNm3 + " " + name3 + " " + "놀거리" + ".txt";
            InputStream in3 = getResources().getAssets().open(fileNm3);
            InputStreamReader reader3 = new InputStreamReader(in3);
            BufferedReader br3 = new BufferedReader(reader3);

            while ((line3 = br3.readLine()) != null) {
                size3[a] = line3;
                a++;
            }
            sto3 = new String[a];
            for(int i=0;i<a;i++){
                sto3[i] = size3[i];
            }
            /*
            sto3 = new String[a];
            for(int i=0;i<a;i++){
                sto3[i] = size[i];
            }
            sto3[a] = null;*/
            br3.close(); reader3.close(); in3.close();

        }catch(IOException e){}





        ListView listView = view.findViewById(R.id.listView3);

        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                sto3
        );
        listView.setAdapter(listViewAdapter);
        return view;
    }

}




