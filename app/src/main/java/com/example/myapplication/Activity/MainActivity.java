package com.example.myapplication.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.DB.DBOpenHelper;
import com.example.myapplication.DB.Station;
import com.example.myapplication.DB.Transfer;
import com.example.myapplication.Dialog.StationMenuDialog;
import com.example.myapplication.R;
import com.example.myapplication.Route.Dijkstras;
import com.example.myapplication.Route.Route;
import com.example.myapplication.Route.StationMatrix;
import com.example.myapplication.Route.Vertex;
import com.example.myapplication.Utiles.SubwayLine;
import com.example.myapplication.Utiles.SubwayMapTouchPoint;
import com.github.chrisbanes.photoview.OnViewTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;

import static android.view.View.GONE;
import static java.lang.Thread.sleep;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MainActivity extends AppCompatActivity {
    final int PERMISSION = 1;
    SpeechRecognizer mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
    TextToSpeech tts;
    ArrayList<String> matches;
    int vc = 1;

    private SubwayMapTouchPoint subwayMapTouchPoint;
    private StationMatrix stationMatrix;
    private ArrayList<Station> stnIdx;
    private PhotoView lineMapView;
    private Dijkstras dijkstraTime = new Dijkstras(),dijkstraFee = new Dijkstras();
    private TextView startStnTextView, endStnTextView;
    private StationMenuDialog dialog;
    private Transfer transfer = new Transfer();

    private SearchListAdapter searchListAdapter;
    private SearchView searchView;
    private ListView searchList;
    Intent intent;

    private boolean isExit; //Back??? ?????? ????????? ??????
    private boolean isFabOpen; //FloatingAction Open/Close ??????
    private Animation fab_open, fab_close, rotate_forward, rotate_backward; //FloatingAction ???????????????
    private FloatingActionButton fab,fab_favorite, fab_sitelink, fab_settings; //FloatingActionButton


    private void getHashKey(){
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");

        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getHashKey();
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, Splash.class);
        startActivity(intent);

        if (Build.VERSION.SDK_INT >= 21) {
            // ????????? ??????
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO}, PERMISSION);
        }
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts.setLanguage(Locale.KOREAN);
            }
        });

        isExit = false;

        lineMapView = findViewById(R.id.photoView);
        lineMapView.setImageResource(R.drawable.sample);
        lineMapView.setMaximumScale(2.0f); //?????????????????? ??????
        lineMapView.setMediumScale(1.5f); //?????????????????? ??????
        lineMapView.setMinimumScale(0.78f); //?????????????????? ??????
        lineMapView.setScale(0.78f, true); //??? ????????? ??? ?????? ?????? ??????
        lineMapView.setOnViewTapListener(new OnLineMapViewTab());

        // ????????? ????????? ????????? ??????
        searchList = findViewById(R.id.search_list);
        searchListAdapter = new SearchListAdapter(this);
        searchList.setAdapter(searchListAdapter);
        searchList.setOnItemClickListener(new OnSearchListItemClick());
        searchView = findViewById(R.id.search);
        searchView.setOnQueryTextListener(new OnSearchViewQueryText());

        // ?????? ????????? ?????????, ???????????? ???????????? ????????????
        startStnTextView = findViewById(R.id.startStnTextView);
        endStnTextView = findViewById(R.id.endStnTextView);
        startStnTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(GONE);
                subwayMapTouchPoint.startStn = null;
            }
        });

        endStnTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(GONE);
                subwayMapTouchPoint.endStn = null;
            }
        });

        DBOpenHelper DBOpenHelper = new DBOpenHelper(MainActivity.this );
        SQLiteDatabase db = DBOpenHelper.getWritableDatabase();
        DBOpenHelper.setDatabase(db);
        DBOpenHelper.initDatabase(db,true);
        subwayMapTouchPoint = new SubwayMapTouchPoint(MainActivity.this); //??? ?????? ?????? ?????????
        stationMatrix = new StationMatrix(DBOpenHelper.getReadableDatabase());
        stnIdx = stationMatrix.getStnIdx();
        dijkstraTime=initRoute(new Route(db,4));    //??????
        dijkstraFee=initRoute(new Route(db,5 ));    //??????

        // activity_main ??? ????????? ?????? ?????? ????????????
        fab = findViewById(R.id.fab);
        fab_favorite = findViewById(R.id.fab_favorite);
        fab_favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TermActivity.class);
                startActivity(intent);
            }
        });
        fab_sitelink = findViewById(R.id.fab_sitelink);
        fab_sitelink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SitelinkActivity.class);
                startActivity(intent);
            }
        });
        fab_settings = findViewById(R.id.fab_settings);
        fab_settings.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, MainActivity.this.getPackageName());
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

                mRecognizer.setRecognitionListener(listener);
                mRecognizer.startListening(intent);

            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFAB(); // ?????? ????????? FloatingAction ??????????????? ??????
            }
        });

        fab_open = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(this, R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(this, R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(this, R.anim.rotate_backward);

    }

    @Override
    public void onBackPressed() {
        if (searchListAdapter.getCount() != 0) {
            searchView.setQuery("", false);
            searchListAdapter.clear();
            searchList.setAdapter(searchListAdapter); //???????????? ??????
            return;
        }

        if (isExit) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "??????????????? ?????? ??? ???????????????.", Toast.LENGTH_SHORT).show();
            isExit = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isExit = false;
                }
            }).start();
        }
    }

    // ??? ?????? dialog
    private void displayStationTouchDialog(final Station stn) {
        // Dialog ??????, ?????? ????????? ??????
        dialog = new StationMenuDialog(MainActivity.this, subwayMapTouchPoint.getLineNms(stnIdx, stn), stn.getName());
        dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    /* ??? switch???????????? ????????? ?????? ??? ??????(stn)??? ?????????????????? ????????? ???????????? ??????
                     * lineMapView??? startStn(?????????), endStn(?????????), viaStn(?????????)??? '??????'?????????
                     * '??? ?????? ????????????'(StnInfoPagerActivity)??? ????????? ??? ??????
                     *
                     * switch??? ????????? ?????? '?????? ?????? ????????????'(RouteGuidancePagerActivity)???
                     * ?????????(SubwayMapTouchPoint.startStn), ?????????(SubwayMapTouchPoint.endStn)??? ??? ??? ????????? ???????????? ??????????????? ??????,
                     * ?????????, ????????? ??? ??? ???????????? ?????? ?????????(SubwayMapTouchPoint.viaStn)??? ???????????? ?????????-?????????-????????? ????????? ????????? **/
                    case R.id.start: //?????????
                        subwayMapTouchPoint.startStn = stn; //SubwayMapTouchPoint map??? startStn??? ????????? ????????? ????????????
                        startStnTextView.setText(String.format("  %s: %s   ", MainActivity.this.getString(R.string.start_station), stn.getName()));
                        startStnTextView.setVisibility(View.VISIBLE);
                        if (subwayMapTouchPoint.endStn == null) { //SubwayMapTouchPoint map??? endStn??? ?????????????????? ????????????
                            dialog.cancel();
                            return; //null?????? ?????????????????? ????????????,
                        }
                        break; //null??? ????????? switch??? ???????????? '?????? ?????? ????????????'(RouteGuidancePagerActivity)??? ????????????

                    case R.id.end: //????????? (R.id.start??? ?????????)
                        subwayMapTouchPoint.endStn = stn;
                        endStnTextView.setText(String.format("  %s: %s   ", MainActivity.this.getString(R.string.end_station), stn.getName()));
                        endStnTextView.setVisibility(View.VISIBLE);
                        if (subwayMapTouchPoint.startStn == null) {
                            dialog.cancel();
                            return;
                        }
                        break;


                case R.id.info: //??????
                    // ??? ?????? Activity ??????
                    Intent intent = new Intent(MainActivity.this, StnInfoPagerActivity.class);
                    intent.putExtra("lines", subwayMapTouchPoint.getLineNms(stnIdx, stn));
                    intent.putExtra("name", stn.getName());
                    MainActivity.this.startActivity(intent);
                    dialog.cancel();
                    return;

                    default:
                        dialog.cancel();
                        return;
                }

            /* ?????????(startStn), ?????????(endStn) ?????? ??????????????????,
               ???????????? ????????? ?????? **/
                findRoute();
                dialog.cancel();
            }
        });
        dialog.show(); //dialog??? ????????????
    }

    private class OnLineMapViewTab implements OnViewTapListener {
        @Override
        public void onViewTap(View view, float x, float y) {
            // ????????? ??????(x,y)??? ???????????? 'stn ??????'??? ????????? ?????? ??????(StnPoint: ????????????, ?????????, ?????????)??? ????????????
            Station stn = subwayMapTouchPoint.getStation(
                    stnIdx,
                    lineMapView.getDisplayRect().left, lineMapView.getDisplayRect().top,
                    lineMapView.getScale(), x, y);
            if (stn != null) //????????? ????????? ?????? ?????? ?????? Dialog??? ?????????
                displayStationTouchDialog(stn);
        }
    }

    //??? ????????? ???????????? ????????? ?????????????????? ??????
    private Dijkstras initRoute(Route route){
        Dijkstras dijkstras = new Dijkstras();
        String stn[] = route.getStn();
        ArrayList<Vertex[]> vertex = route.getVertexList();
        int size=0;
        for(Vertex[] v : vertex){
            dijkstras.addVertex(stn[size++], Arrays.asList(v));
        }
        return dijkstras;
    }

    //???????????????
    private void findRoute(){
        String start = subwayMapTouchPoint.startStn.getName();  //?????????
        String end = subwayMapTouchPoint.endStn.getName();      //?????????
        ArrayList<String> route  = (ArrayList<String>) dijkstraTime.getDijkstras(start,end);  //???????????????(??????) ????????? => ????????? ??????

        route.add(start);
        Collections.reverse(route);  //??????????????? ????????? ????????? ???????????????.
        ArrayList<String> trans = findTransfer(route, end); //???????????? ?????? ??????
        ArrayList<String[]> transLineNms = findTransLineNm(trans);  //??? ??????????????? ???????????? ??????
        int time = (int)dijkstraTime.getWeight(start,route);  //???????????? ?????? ?????? ?????? ????????? ??????
        int fee = (int)dijkstraFee.getWeight(start,route);  //???????????? ?????? ?????? ?????? ????????? ??????


        ArrayList<String> route2  = (ArrayList<String>) dijkstraFee.getDijkstras(start,end);  //???????????????(??????) ????????? => ????????? ??????
        route2.add(start);
        Collections.reverse(route2);  //??????????????? ????????? ????????? ???????????????.
        ArrayList<String> trans2 = findTransfer(route2, end);   //???????????? ?????? ??????.
        ArrayList<String[]> transLineNms2 = findTransLineNm(trans2);  //??? ??????????????? ???????????? ??????.
        int time2 = (int)dijkstraTime.getWeight(start,route2);  //???????????? ?????? ?????? ?????? ????????? ??????
        int fee2 = (int)dijkstraFee.getWeight(start,route2);  //???????????? ?????? ?????? ?????? ????????? ??????

        intent = new Intent(getApplicationContext(),RouteActivity.class);
        intent.putExtra("startStn",start);
        intent.putExtra("endStn",end);
        intent.putExtra("alarmtime1",time);
        intent.putExtra("alarmtime2",time2);
        intent.putExtra("route1Stntime","??? "+time+"???");
        intent.putExtra("route1Stnfee",fee+"???\n");
        intent.putExtra("route1Stn", route);
        intent.putExtra("trans1Stn", trans);
        intent.putExtra("transLineNms", transLineNms);
        intent.putExtra("route2Stntime","??? "+time2+"???");
        intent.putExtra("route2Stnfee",fee2+"???\n");
        intent.putExtra("route2Stn", route2);
        intent.putExtra("trans2Stn", trans2);
        intent.putExtra("transLineNms2", transLineNms2);
        intent.putExtra("startStnNms",subwayMapTouchPoint.getLineNms(stnIdx, subwayMapTouchPoint.startStn));
        intent.putExtra("endStnNms",subwayMapTouchPoint.getLineNms(stnIdx, subwayMapTouchPoint.endStn));

        startActivity(intent);//???????????? ?????????

        //?????????, ????????? ?????????
        subwayMapTouchPoint.startStn=null;
        subwayMapTouchPoint.endStn=null;
        startStnTextView.setVisibility(View.INVISIBLE);
        endStnTextView.setVisibility(View.INVISIBLE);

    }

    // SearchListView??? Adapter
    private class SearchListAdapter extends BaseAdapter {
        private ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        private Context context;
        private int id = R.layout.item_station;
        private ArrayList<MyItem> myItems = new ArrayList<>();

        SearchListAdapter(Context context) {
            this.context = context;
        }

        void add(String lineNm, String Name, int X, int Y) {
            for (MyItem item : myItems) {
                if (X == item.x && Y == item.y) {
                    item.lienNms.add(lineNm);
                    return;
                }
            }
            myItems.add(new MyItem(lineNm, Name, X, Y));
        }

        void clear() {
            myItems.clear();
        }

        @Override
        public int getCount() {
            return myItems.size();
        }

        @Override
        public MyItem getItem(int position) {
            return myItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(id, parent, false);

                holder = new Holder();
                holder.lineSymLayout = convertView.findViewById(R.id.lineSymLayout);
                holder.Name = convertView.findViewById(R.id.Name);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
                holder.lineSymLayout.removeAllViews();
            }

            MyItem stn = myItems.get(position);
            for (String lineNm : stn.lienNms) {
                ImageView imageView = new ImageView(context);
                imageView.setImageResource(SubwayLine.getResId(lineNm));
                holder.lineSymLayout.addView(imageView, params);
            }
            holder.Name.setText(stn.Name);

            return convertView;
        }

        private class Holder {
            LinearLayout lineSymLayout;
            TextView Name;
        }

        private class MyItem {
            private ArrayList<String> lienNms = new ArrayList<>();
            private String Name;
            private int x, y;

            private MyItem(String lienNm, String Name, int x, int y) {
                lienNms.add(lienNm);
                this.Name = Name;
                this.x = x;
                this.y = y;
            }

            public String getName() {
                return Name;
            }

        }

    }

    private class OnSearchViewQueryText implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            if (searchListAdapter.getCount() == 0)
                return false;

            AdapterView.OnItemClickListener listener = searchList.getOnItemClickListener();
            if (listener != null)
                listener.onItemClick(null, null, 0, 0);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String mText) {
            searchListAdapter.clear();
            if (mText.equals(""))
                return false;
            for (Station stn : stnIdx)
                if (stn.getName().startsWith(mText))
                    searchListAdapter.add(stn.getLineNm(), stn.getName(), stn.getX(), stn.getY());
            searchList.setAdapter(searchListAdapter); //???????????? ??????
            return true;
        }

    }

    private class OnSearchListItemClick implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String Name = searchListAdapter.getItem(position).getName();
            Station mStn = null;
            for (Station stn : stnIdx)
                if (Name.equals(stn.getName()))
                    mStn = stn;

            if (mStn != null) {
                searchView.setQuery("", false);
                searchListAdapter.clear();
                searchList.setAdapter(searchListAdapter); //???????????? ??????
                displayStationTouchDialog(mStn);
            }
        }

    }

    // FloatingAction ??????????????? Open/Close
    private void animateFAB() {
        if (isFabOpen) {
            fab.startAnimation(rotate_backward);
            fab_favorite.startAnimation(fab_close);
            fab_sitelink.startAnimation(fab_close);
            fab_settings.startAnimation(fab_close);
            fab_favorite.setClickable(false);
            fab_sitelink.setClickable(false);
            fab_settings.setClickable(false);
            isFabOpen = false;
        } else {
            fab.startAnimation(rotate_forward);
            fab_favorite.startAnimation(fab_open);
            fab_sitelink.startAnimation(fab_open);
            fab_settings.startAnimation(fab_open);
            fab_favorite.setClickable(true);
            fab_sitelink.setClickable(true);
            fab_settings.setClickable(true);
            isFabOpen = true;
        }
    }
    //???????????? ?????? ??????
    private ArrayList<String> findTransfer(ArrayList<String> route, String end){
        ArrayList<String> trans = new ArrayList<>();
        for(int i=1; i<route.size();i++){
            ArrayList<String> prevStn = subwayMapTouchPoint.getLineNms(stnIdx, route.get(i-1));
            if(transfer.getTransfer(route.get(i))) {
                if(route.get(i).equals(end)) break;
                trans.add(route.get(i));
                ArrayList<String> nxtStn = subwayMapTouchPoint.getLineNms(stnIdx, route.get(i+1));
                for(String prev : prevStn) {
                    for (String nxt : nxtStn)
                        if (prev.equals(nxt)) trans.remove(route.get(i));
                }
            }
        }
        return trans;
    }

    //??? ??????????????? ???????????? ??????
    private ArrayList<String[]> findTransLineNm(ArrayList<String> trans){
        ArrayList<String[]> transLineNms = new ArrayList<>();
        for(String station : trans){
            ArrayList<String> lineNm = subwayMapTouchPoint.getLineNms(stnIdx,station);
            String[] lineNmArr= new String[lineNm.size()];
            int size=0;
            for(String number : lineNm) lineNmArr[size++]=number;
            transLineNms.add(lineNmArr);
        }
        return transLineNms;
    }

    public void restart(){

        if(vc == 1) {
            tts.speak("???????????? ?????? ???????????????", TextToSpeech.QUEUE_FLUSH, null);
            //Toast.makeText(getApplicationContext(), "???????????? ???????????????", Toast.LENGTH_SHORT).show();
        }
        if(vc == 2) {
            tts.speak("???????????? ???????????????", TextToSpeech.QUEUE_FLUSH, null);
            //Toast.makeText(getApplicationContext(), "???????????? ???????????????", Toast.LENGTH_SHORT).show();
        }
        /*
        hd.postDelayed(new Runnable() {

            @Override
            public void run() {
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                mRecognizer.setRecognitionListener(listener);
                mRecognizer.startListening(intent);
            }
        }, 5000);
        */
    }

    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(),"??????????????? ???????????????. ???????????? ???????????? ????????? ???????????????",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "????????? ??????";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "??????????????? ??????";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "????????? ??????";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "???????????? ??????";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "????????? ????????????";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "?????? ??? ??????";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER??? ??????";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "????????? ?????????";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "????????? ????????????";
                    break;
                default:
                    message = "??? ??? ?????? ?????????";
                    break;
            }

            Toast.makeText(getApplicationContext(), "????????? ?????????????????????. : " + message,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            // ?????? ?????? ArrayList??? ????????? ?????? textView??? ????????? ???????????????.
            matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            /*
            for(int i = 0; i < matches.size() ; i++){
                voiceTextView.setText(matches.get(i));
                replyAnswer(matches.get(0), voiceTextView);
            }
            */

            replyAnswer(matches.get(0));
            if(vc>=3){
                tts.speak("????????? ???????????????", TextToSpeech.QUEUE_FLUSH, null);
                findRoute();
                vc = 1;
            }
            else if(vc <= 2 ){
                restart();
            }
        }

        private void replyAnswer(String matches) {
            try {
                switch(vc){
                    case 1:
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setstart(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("?????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "?????????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setstart(stn);
                        }

                        if(matches.equals("????????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C??????", "????????????");
                            setstart(stn);
                        }
                        if(matches.equals("?????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C??????", "?????????");
                            setstart(stn);
                        }
                        if(matches.equals("?????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C??????", "?????????");
                            setstart(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D??????", "??????");
                            setstart(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setstart(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setstart(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G??????", "??????");
                            setstart(stn);
                        }
                        if(matches.equals("?????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G??????", "?????????");
                            setstart(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G??????", "??????");
                            setstart(stn);
                        }
                        break;
                    case 2:
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A??????", "??????");
                            setend(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("?????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "?????????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B??????", "??????");
                            setend(stn);
                        }

                        if(matches.equals("????????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C??????", "????????????");
                            setend(stn);
                        }
                        if(matches.equals("?????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C??????", "?????????");
                            setend(stn);
                        }
                        if(matches.equals("?????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C??????", "?????????");
                            setend(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D??????", "??????");
                            setend(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E??????", "??????");
                            setend(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F??????", "??????");
                            setend(stn);
                        }

                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G??????", "??????");
                            setend(stn);
                        }
                        if(matches.equals("?????????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G??????", "?????????");
                            setend(stn);
                        }
                        if(matches.equals("??????")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G??????", "??????");
                            setend(stn);
                        }
                        break;
                }

            } catch (Exception e) {

            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}

        public  void setstart(Station stn){
            subwayMapTouchPoint.startStn = stn;
            startStnTextView.setText(String.format("  %s: %s   ", MainActivity.this.getString(R.string.start_station), stn.getName()));
            startStnTextView.setVisibility(View.VISIBLE);
            vc++;
        }
        public  void setend(Station stn){
            subwayMapTouchPoint.endStn = stn;
            endStnTextView.setText(String.format("  %s: %s   ", getString(R.string.end_station), stn.getName()));
            endStnTextView.setVisibility(View.VISIBLE);
            vc++;
        }
    };
}
