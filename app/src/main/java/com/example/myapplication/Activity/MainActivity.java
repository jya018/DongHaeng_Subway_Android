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

    private boolean isExit; //Back키 두번 누르면 종료
    private boolean isFabOpen; //FloatingAction Open/Close 여부
    private Animation fab_open, fab_close, rotate_forward, rotate_backward; //FloatingAction 애니메이션
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
            // 퍼미션 체크
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
        lineMapView.setMaximumScale(2.0f); //최대확대크기 설정
        lineMapView.setMediumScale(1.5f); //중간확대크기 설정
        lineMapView.setMinimumScale(0.78f); //최소축소크기 설정
        lineMapView.setScale(0.78f, true); //앱 시작할 때 기본 크기 설정
        lineMapView.setOnViewTapListener(new OnLineMapViewTab());

        // 검색바 이벤트 리스너 추가
        searchList = findViewById(R.id.search_list);
        searchListAdapter = new SearchListAdapter(this);
        searchList.setAdapter(searchListAdapter);
        searchList.setOnItemClickListener(new OnSearchListItemClick());
        searchView = findViewById(R.id.search);
        searchView.setOnQueryTextListener(new OnSearchViewQueryText());

        // 현재 선택된 출발역, 도착역을 표시하는 텍스트뷰
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
        subwayMapTouchPoint = new SubwayMapTouchPoint(MainActivity.this); //역 터치 좌표 초기화
        stationMatrix = new StationMatrix(DBOpenHelper.getReadableDatabase());
        stnIdx = stationMatrix.getStnIdx();
        dijkstraTime=initRoute(new Route(db,4));    //시간
        dijkstraFee=initRoute(new Route(db,5 ));    //요금

        // activity_main 의 플로팅 액션 버튼 동그라미
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
                animateFAB(); // 버튼 클릭시 FloatingAction 애니메이션 시작
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
            searchList.setAdapter(searchListAdapter); //리스트뷰 갱신
            return;
        }

        if (isExit) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "종료하려면 한번 더 눌러주세요.", Toast.LENGTH_SHORT).show();
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

    // 역 터치 dialog
    private void displayStationTouchDialog(final Station stn) {
        // Dialog 생성, 메뉴 리스너 설정
        dialog = new StationMenuDialog(MainActivity.this, subwayMapTouchPoint.getLineNms(stnIdx, stn), stn.getName());
        dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    /* 이 switch문에서는 위에서 받은 역 정보(stn)를 팝업메뉴에서 선택한 아이템에 따라
                     * lineMapView의 startStn(출발역), endStn(도착역), viaStn(경유역)에 '저장'하거나
                     * '역 정보 액티비티'(StnInfoPagerActivity)를 호출할 수 있어
                     *
                     * switch문 아래에 있는 '경로 안내 액티비티'(RouteGuidancePagerActivity)는
                     * 출발역(SubwayMapTouchPoint.startStn), 도착역(SubwayMapTouchPoint.endStn)이 둘 다 설정된 경우에만 호출되도록 했고,
                     * 출발역, 도착역 둘 다 설정하기 전에 경유역(SubwayMapTouchPoint.viaStn)을 설정하면 출발역-경유역-도착역 경로를 알려줘 **/
                    case R.id.start: //출발역
                        subwayMapTouchPoint.startStn = stn; //SubwayMapTouchPoint map의 startStn에 출발역 정보를 저장한다
                        startStnTextView.setText(String.format("  %s: %s   ", MainActivity.this.getString(R.string.start_station), stn.getName()));
                        startStnTextView.setVisibility(View.VISIBLE);
                        if (subwayMapTouchPoint.endStn == null) { //SubwayMapTouchPoint map의 endStn이 선택되었는지 검사하여
                            dialog.cancel();
                            return; //null이면 다이얼로그를 종료하고,
                        }
                        break; //null이 아니면 switch문 다음에서 '경로 안내 액티비티'(RouteGuidancePagerActivity)를 호출한다

                    case R.id.end: //도착역 (R.id.start와 비슷함)
                        subwayMapTouchPoint.endStn = stn;
                        endStnTextView.setText(String.format("  %s: %s   ", MainActivity.this.getString(R.string.end_station), stn.getName()));
                        endStnTextView.setVisibility(View.VISIBLE);
                        if (subwayMapTouchPoint.startStn == null) {
                            dialog.cancel();
                            return;
                        }
                        break;


                case R.id.info: //정보
                    // 역 정보 Activity 호출
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

            /* 출발역(startStn), 도착역(endStn) 모두 입력되었다면,
               경로탐색 쓰레드 실행 **/
                findRoute();
                dialog.cancel();
            }
        });
        dialog.show(); //dialog를 보여준다
    }

    private class OnLineMapViewTab implements OnViewTapListener {
        @Override
        public void onViewTap(View view, float x, float y) {
            // 터치한 좌표(x,y)를 사용하여 'stn 객체'에 터치한 역의 정보(StnPoint: 터치좌표, 역호선, 역이름)를 저장한다
            Station stn = subwayMapTouchPoint.getStation(
                    stnIdx,
                    lineMapView.getDisplayRect().left, lineMapView.getDisplayRect().top,
                    lineMapView.getScale(), x, y);
            if (stn != null) //터치한 위치에 역이 있는 경우 Dialog를 띄운다
                displayStationTouchDialog(stn);
        }
    }

    //각 역마다 초기화된 경로를 다익스트라에 삽입
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

    //다익스트라
    private void findRoute(){
        String start = subwayMapTouchPoint.startStn.getName();  //출발역
        String end = subwayMapTouchPoint.endStn.getName();      //도착역
        ArrayList<String> route  = (ArrayList<String>) dijkstraTime.getDijkstras(start,end);  //다익스트라(시간) 메소드 => 리스트 반환

        route.add(start);
        Collections.reverse(route);  //출력결과가 반대로 나와서 뒤집어준다.
        ArrayList<String> trans = findTransfer(route, end); //환승역을 찾아 반환
        ArrayList<String[]> transLineNms = findTransLineNm(trans);  //각 환승역들의 호선이름 반환
        int time = (int)dijkstraTime.getWeight(start,route);  //리스트의 담긴 역들 간의 총시간 반환
        int fee = (int)dijkstraFee.getWeight(start,route);  //리스트의 담긴 역들 간의 총요금 반환


        ArrayList<String> route2  = (ArrayList<String>) dijkstraFee.getDijkstras(start,end);  //다익스트라(요금) 메소드 => 리스트 반환
        route2.add(start);
        Collections.reverse(route2);  //출력결과가 반대로 나와서 뒤집어준다.
        ArrayList<String> trans2 = findTransfer(route2, end);   //환승역을 찾아 반환.
        ArrayList<String[]> transLineNms2 = findTransLineNm(trans2);  //각 환승역들의 호선이름 반환.
        int time2 = (int)dijkstraTime.getWeight(start,route2);  //리스트의 담긴 역들 간의 총시간 반환
        int fee2 = (int)dijkstraFee.getWeight(start,route2);  //리스트의 담긴 역들 간의 총요금 반환

        intent = new Intent(getApplicationContext(),RouteActivity.class);
        intent.putExtra("startStn",start);
        intent.putExtra("endStn",end);
        intent.putExtra("alarmtime1",time);
        intent.putExtra("alarmtime2",time2);
        intent.putExtra("route1Stntime","약 "+time+"분");
        intent.putExtra("route1Stnfee",fee+"원\n");
        intent.putExtra("route1Stn", route);
        intent.putExtra("trans1Stn", trans);
        intent.putExtra("transLineNms", transLineNms);
        intent.putExtra("route2Stntime","약 "+time2+"분");
        intent.putExtra("route2Stnfee",fee2+"원\n");
        intent.putExtra("route2Stn", route2);
        intent.putExtra("trans2Stn", trans2);
        intent.putExtra("transLineNms2", transLineNms2);
        intent.putExtra("startStnNms",subwayMapTouchPoint.getLineNms(stnIdx, subwayMapTouchPoint.startStn));
        intent.putExtra("endStnNms",subwayMapTouchPoint.getLineNms(stnIdx, subwayMapTouchPoint.endStn));

        startActivity(intent);//액티비티 띄우기

        //출발역, 도착역 초기화
        subwayMapTouchPoint.startStn=null;
        subwayMapTouchPoint.endStn=null;
        startStnTextView.setVisibility(View.INVISIBLE);
        endStnTextView.setVisibility(View.INVISIBLE);

    }

    // SearchListView의 Adapter
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
            searchList.setAdapter(searchListAdapter); //리스트뷰 갱신
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
                searchList.setAdapter(searchListAdapter); //리스트뷰 갱신
                displayStationTouchDialog(mStn);
            }
        }

    }

    // FloatingAction 애니메이션 Open/Close
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
    //환승역을 찾아 반환
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

    //각 환승역들의 호선이름 반환
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
            tts.speak("출발역을 다시 말해주세요", TextToSpeech.QUEUE_FLUSH, null);
            //Toast.makeText(getApplicationContext(), "도착역을 말해주세요", Toast.LENGTH_SHORT).show();
        }
        if(vc == 2) {
            tts.speak("도착역을 말해주세요", TextToSpeech.QUEUE_FLUSH, null);
            //Toast.makeText(getApplicationContext(), "도착역을 말해주세요", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getApplicationContext(),"음성인식을 시작합니다. 출발역과 도착역을 차례로 말해주세요",Toast.LENGTH_SHORT).show();
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
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }

            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줍니다.
            matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            /*
            for(int i = 0; i < matches.size() ; i++){
                voiceTextView.setText(matches.get(i));
                replyAnswer(matches.get(0), voiceTextView);
            }
            */

            replyAnswer(matches.get(0));
            if(vc>=3){
                tts.speak("검색한 경로입니다", TextToSpeech.QUEUE_FLUSH, null);
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
                        if(matches.equals("성진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "성진");
                            setstart(stn);
                        }
                        if(matches.equals("성중")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "성중");
                            setstart(stn);
                        }
                        if(matches.equals("기중")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "기중");
                            setstart(stn);
                        }
                        if(matches.equals("고기")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "고기");
                            setstart(stn);
                        }
                        if(matches.equals("성단")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "성단");
                            setstart(stn);
                        }
                        if(matches.equals("시진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "시진");
                            setstart(stn);
                        }
                        if(matches.equals("무진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "무진");
                            setstart(stn);
                        }
                        if(matches.equals("무주")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "무주");
                            setstart(stn);
                        }
                        if(matches.equals("성남")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "성남");
                            setstart(stn);
                        }

                        if(matches.equals("홍원")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "홍원");
                            setstart(stn);
                        }
                        if(matches.equals("성반")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "성반");
                            setstart(stn);
                        }
                        if(matches.equals("홍삼")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "홍삼");
                            setstart(stn);
                        }
                        if(matches.equals("김진도")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "김진도");
                            setstart(stn);
                        }
                        if(matches.equals("홍사")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "홍사");
                            setstart(stn);
                        }
                        if(matches.equals("무리")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "무리");
                            setstart(stn);
                        }
                        if(matches.equals("북진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "북진");
                            setstart(stn);
                        }
                        if(matches.equals("설진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "설진");
                            setstart(stn);
                        }
                        if(matches.equals("고지")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "고지");
                            setstart(stn);
                        }
                        if(matches.equals("충기")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "충기");
                            setstart(stn);
                        }

                        if(matches.equals("김천입구")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C호선", "김천입구");
                            setstart(stn);
                        }
                        if(matches.equals("김가네")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C호선", "김가네");
                            setstart(stn);
                        }
                        if(matches.equals("군자시")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C호선", "군자시");
                            setstart(stn);
                        }

                        if(matches.equals("잠수")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D호선", "잠수");
                            setstart(stn);
                        }
                        if(matches.equals("덕담")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D호선", "덕담");
                            setstart(stn);
                        }
                        if(matches.equals("북악")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D호선", "북악");
                            setstart(stn);
                        }
                        if(matches.equals("둔기")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D호선", "둔기");
                            setstart(stn);
                        }

                        if(matches.equals("까치")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "까치");
                            setstart(stn);
                        }
                        if(matches.equals("남앙")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "남앙");
                            setstart(stn);
                        }
                        if(matches.equals("일견")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "일견");
                            setstart(stn);
                        }
                        if(matches.equals("의정")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "의정");
                            setstart(stn);
                        }
                        if(matches.equals("역사")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "역사");
                            setstart(stn);
                        }

                        if(matches.equals("족기")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "족기");
                            setstart(stn);
                        }
                        if(matches.equals("소래")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "소래");
                            setstart(stn);
                        }
                        if(matches.equals("냉수")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "냉수");
                            setstart(stn);
                        }
                        if(matches.equals("감산")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "감산");
                            setstart(stn);
                        }
                        if(matches.equals("개화")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "개화");
                            setstart(stn);
                        }

                        if(matches.equals("율곡")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G호선", "율곡");
                            setstart(stn);
                        }
                        if(matches.equals("원홍")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G호선", "원홍");
                            setstart(stn);
                        }
                        if(matches.equals("이리요")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G호선", "이리요");
                            setstart(stn);
                        }
                        if(matches.equals("의자")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G호선", "의자");
                            setstart(stn);
                        }
                        break;
                    case 2:
                        if(matches.equals("성진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "성진");
                            setend(stn);
                        }
                        if(matches.equals("성중")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "성중");
                            setend(stn);
                        }
                        if(matches.equals("기중")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "기중");
                            setend(stn);
                        }
                        if(matches.equals("고기")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "고기");
                            setend(stn);
                        }
                        if(matches.equals("성단")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "성단");
                            setend(stn);
                        }
                        if(matches.equals("시진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "시진");
                            setend(stn);
                        }
                        if(matches.equals("무진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "무진");
                            setend(stn);
                        }
                        if(matches.equals("무주")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "무주");
                            setend(stn);
                        }
                        if(matches.equals("성남")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"A호선", "성남");
                            setend(stn);
                        }

                        if(matches.equals("홍원")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "홍원");
                            setend(stn);
                        }
                        if(matches.equals("성반")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "성반");
                            setend(stn);
                        }
                        if(matches.equals("홍삼")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "홍삼");
                            setend(stn);
                        }
                        if(matches.equals("김진도")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "김진도");
                            setend(stn);
                        }
                        if(matches.equals("홍사")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "홍사");
                            setend(stn);
                        }
                        if(matches.equals("무리")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "무리");
                            setend(stn);
                        }
                        if(matches.equals("북진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "북진");
                            setend(stn);
                        }
                        if(matches.equals("설진")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "설진");
                            setend(stn);
                        }
                        if(matches.equals("고지")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "고지");
                            setend(stn);
                        }
                        if(matches.equals("충기")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"B호선", "충기");
                            setend(stn);
                        }

                        if(matches.equals("김천입구")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C호선", "김천입구");
                            setend(stn);
                        }
                        if(matches.equals("김가네")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C호선", "김가네");
                            setend(stn);
                        }
                        if(matches.equals("군자시")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"C호선", "군자시");
                            setend(stn);
                        }

                        if(matches.equals("잠수")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D호선", "잠수");
                            setend(stn);
                        }
                        if(matches.equals("덕담")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D호선", "덕담");
                            setend(stn);
                        }
                        if(matches.equals("북악")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D호선", "북악");
                            setend(stn);
                        }
                        if(matches.equals("둔기")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"D호선", "둔기");
                            setend(stn);
                        }

                        if(matches.equals("까치")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "까치");
                            setend(stn);
                        }
                        if(matches.equals("남앙")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "남앙");
                            setend(stn);
                        }
                        if(matches.equals("일견")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "일견");
                            setend(stn);
                        }
                        if(matches.equals("의정")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "의정");
                            setend(stn);
                        }
                        if(matches.equals("역사")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"E호선", "역사");
                            setend(stn);
                        }

                        if(matches.equals("족기")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "족기");
                            setend(stn);
                        }
                        if(matches.equals("소래")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "소래");
                            setend(stn);
                        }
                        if(matches.equals("냉수")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "냉수");
                            setend(stn);
                        }
                        if(matches.equals("감산")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "감산");
                            setend(stn);
                        }
                        if(matches.equals("개화")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"F호선", "개화");
                            setend(stn);
                        }

                        if(matches.equals("율곡")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G호선", "율곡");
                            setend(stn);
                        }
                        if(matches.equals("원홍")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G호선", "원홍");
                            setend(stn);
                        }
                        if(matches.equals("이리요")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G호선", "이리요");
                            setend(stn);
                        }
                        if(matches.equals("의자")){
                            Station stn = subwayMapTouchPoint.getStation(stationMatrix.getStnIdx(),"G호선", "의자");
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
