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


public class TimeTableFragment extends Fragment {

    String lineNm;
    String name;
    String line;
    String[] sto;
    String[] size = new String[15];
    int a=0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_time_table, container, false);



        Bundle bundle = getArguments();
        if (bundle != null) {
            lineNm = bundle.getString("lineNm");
            name = bundle.getString("name");
        }

        try{
        String fileNm = lineNm + " " + name + " " + "카페" + ".txt";
        InputStream in = getResources().getAssets().open(fileNm);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(reader);

            while ((line = br.readLine()) != null) {
                size[a] = line;
                a++;
            }

            sto = new String[a];
            for(int i=0;i<a;i++){
                sto[i] = size[i];
            }
            br.close(); reader.close(); in.close();

        }catch(IOException e){}





        ListView listView = view.findViewById(R.id.listView);

        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(
            getActivity(),
            android.R.layout.simple_list_item_1,
            sto
        );
        listView.setAdapter(listViewAdapter);
        return view;
    }

}




