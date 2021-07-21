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


public class TimeTableFragment2 extends Fragment {

    String lineNm2;
    String name2;
    String line2;
    String[] sto2;
    String[] size2 = new String[15];
    int a=0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_time_table2, container, false);



        Bundle bundle = getArguments();
        if (bundle != null) {
            lineNm2 = bundle.getString("lineNm");
            name2 = bundle.getString("name");
        }

        try{
            String fileNm2 = lineNm2 + " " + name2 + " " + "음식점" + ".txt";
            InputStream in2 = getResources().getAssets().open(fileNm2);
            InputStreamReader reader2 = new InputStreamReader(in2);
            BufferedReader br2 = new BufferedReader(reader2);

            while ((line2 = br2.readLine()) != null) {
                size2[a] = line2;
                a++;
            }
            sto2 = new String[a];
            for(int i=0;i<a;i++){
                sto2[i] = size2[i];
            }
            br2.close(); reader2.close(); in2.close();

        }catch(IOException e){}





        ListView listView = view.findViewById(R.id.listView2);

        ArrayAdapter<String> listViewAdapter2 = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                sto2
        );
        listView.setAdapter(listViewAdapter2);
        return view;
    }

}
