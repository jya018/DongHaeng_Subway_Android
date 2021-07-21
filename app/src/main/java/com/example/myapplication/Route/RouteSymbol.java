package com.example.myapplication.Route;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.R;

public class RouteSymbol extends LinearLayout {
    public RouteSymbol(Context context, ImageView[] imageViews, TextView stnName) {
        super(context);

        LayoutInflater inflater =(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.route_symbol,this,true);

        LinearLayout routeMain = findViewById(R.id.routeSymbol);
        ViewGroup.LayoutParams routeparams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for(ImageView image : imageViews){
            routeMain.addView(image, routeparams);
        }
        routeMain.addView(stnName);
    }


}
