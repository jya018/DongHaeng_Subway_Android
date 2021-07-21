package com.example.myapplication.Activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.Route.RouteSymbol;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.message.template.ButtonObject;
import com.kakao.message.template.ContentObject;
import com.kakao.message.template.FeedTemplate;
import com.kakao.message.template.LinkObject;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;

import com.example.myapplication.Utiles.SubwayLine;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class RouteActivity extends AppCompatActivity {


    private TextView startStnTextView1, endStnTextView1, route1time, route1fee;
    private TextView startStnTextView2, endStnTextView2, route2time, route2fee;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);


        TabHost tabHost1 = (TabHost) findViewById(R.id.tabHost1);
        tabHost1.setup();


        // 첫 번째 Tab. (탭 표시 텍스트:"TAB 1"), (페이지 뷰:"content1")
        TabHost.TabSpec ts1 = tabHost1.newTabSpec("Tab Spec 1");
        ts1.setContent(R.id.content1);
        ts1.setIndicator("최단시간");
        tabHost1.addTab(ts1);

        // 두 번째 Tab. (탭 표시 텍스트:"TAB 2"), (페이지 뷰:"content2")
        TabHost.TabSpec ts2 = tabHost1.newTabSpec("Tab Spec 2");
        ts2.setContent(R.id.content2);
        ts2.setIndicator("최소요금");
        tabHost1.addTab(ts2);

        startStnTextView1 = findViewById(R.id.startStnText1);
        endStnTextView1 = findViewById(R.id.endStnText1);
        route1time = findViewById(R.id.route1time);
        route1fee = findViewById(R.id.route1fee);

        startStnTextView2 = findViewById(R.id.startStnText2);
        endStnTextView2 = findViewById(R.id.endStnText2);
        route2time = findViewById(R.id.route2time);
        route2fee = findViewById(R.id.route2fee);

        startStnTextView1.setText(getIntent().getStringExtra("startStn"));
        endStnTextView1.setText(getIntent().getStringExtra("endStn"));
        route1time.setText(getIntent().getStringExtra("route1Stntime"));
        route1fee.setText(getIntent().getStringExtra("route1Stnfee"));
        startStnTextView2.setText(getIntent().getStringExtra("startStn"));
        endStnTextView2.setText(getIntent().getStringExtra("endStn"));
        route2time.setText(getIntent().getStringExtra("route2Stntime"));
        route2fee.setText(getIntent().getStringExtra("route2Stnfee"));

        LinearLayout start1stnNm = findViewById(R.id.startstnNm1);
        ViewGroup.LayoutParams start1params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ArrayList<String> start1lines = (ArrayList<String>) getIntent().getSerializableExtra("startStnNms");
        for (String lineNm : start1lines) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(SubwayLine.getResId(lineNm));
            start1stnNm.addView(imageView, start1params);
        }

        LinearLayout end1stnNm = findViewById(R.id.endstnNm1);
        ViewGroup.LayoutParams end1params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ArrayList<String> end1lines = (ArrayList<String>) getIntent().getSerializableExtra("endStnNms");
        for (String lineNm : end1lines) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(SubwayLine.getResId(lineNm));
            end1stnNm.addView(imageView, end1params);
        }

        LinearLayout start2stnNm = findViewById(R.id.startstnNm2);
        ViewGroup.LayoutParams start2params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ArrayList<String> start2lines = (ArrayList<String>) getIntent().getSerializableExtra("startStnNms");
        for (String lineNm : start2lines) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(SubwayLine.getResId(lineNm));
            start2stnNm.addView(imageView, start2params);
        }

        LinearLayout end2stnNm = findViewById(R.id.endstnNm2);
        ViewGroup.LayoutParams end2params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ArrayList<String> end2lines = (ArrayList<String>) getIntent().getSerializableExtra("endStnNms");
        for (String lineNm : end2lines) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(SubwayLine.getResId(lineNm));
            end2stnNm.addView(imageView, end2params);
        }

        //최소시간 경로
        LinearLayout routetimeStn = findViewById(R.id.routetimeStn);
        ViewGroup.LayoutParams routeparams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        List<String> route1Stn = (List<String>) getIntent().getSerializableExtra("route1Stn");  //경로
        List<String> trans1Stn = (List<String>) getIntent().getSerializableExtra("trans1Stn");  //환승역들
        List<String[]> transLineNms1 = (List<String[]>) getIntent().getSerializableExtra("transLineNms");  //환승역들의 호선이름들
        int symCount=0; //호선이름의 순서
        for (String str : route1Stn) {
            TextView TextView = new TextView(this);
            //출발역이거나 환승역, 도착역일 경우 볼드체, 호선심볼 추가
            if(str.equals(startStnTextView1.getText())|| trans1Stn.contains(str)|| str.equals(endStnTextView1.getText())) {
                ImageView[] imageView;  //심볼을 담을 이미지뷰 배열
                TextView.setTypeface(null, Typeface.BOLD);
                int index=0;    //이미지뷰 배열 인덱스
                //출발역일 때
                if(str.equals(startStnTextView1.getText())){
                    imageView = new ImageView[start1lines.size()];
                    for (String lineNm : start1lines) {
                        imageView[index]= new ImageView(this);
                        imageView[index++].setImageResource(SubwayLine.getResId(lineNm));
                    }
                    //도착역일 때
                }else if(str.equals(endStnTextView1.getText())){
                    imageView = new ImageView[end1lines.size()];
                    for (String lineNm : end1lines) {
                        imageView[index]= new ImageView(this);
                        imageView[index++].setImageResource(SubwayLine.getResId(lineNm));
                    }
                    //환승역일 때
                }else{
                    imageView = new ImageView[transLineNms1.get(symCount).length];
                    for(int i=0; i<transLineNms1.get(symCount).length;i++){
                        imageView[index]= new ImageView(this);
                        imageView[index++].setImageResource(SubwayLine.getResId(transLineNms1.get(symCount)[i]));
                    }
                    symCount++;
                }
                TextView.setText("\t"+str);
                RouteSymbol route = new RouteSymbol(this, imageView, TextView); //심볼과 역이름을 담은 레이아웃
                routetimeStn.addView(route, routeparams);
            }
            else {
                TextView.setText("\t"+str);
                routetimeStn.addView(TextView, routeparams);
            }
            TextView.setHeight(100);
        }

        //최소요금 경로
        LinearLayout routefeeStn = findViewById(R.id.routefeeStn);
        ViewGroup.LayoutParams route2params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        List<String> route2Stn = (List<String>) getIntent().getSerializableExtra("route2Stn");  //경로
        List<String> trans2Stn = (List<String>) getIntent().getSerializableExtra("trans2Stn");  //환승역들
        List<String[]> transLineNms2 = (List<String[]>) getIntent().getSerializableExtra("transLineNms2");  //환승역들의 호선이름들
        int symCount2=0;
        for (String str : route2Stn) {
            TextView TextView = new TextView(this);
            //출발역이거나 환승역, 도착역일 경우 볼드체,호선심볼추가
            if(str.equals(startStnTextView2.getText())|| trans2Stn.contains(str)|| str.equals(endStnTextView2.getText())) {
                ImageView[] imageView;  //심볼을 담을 이미지뷰 배열
                TextView.setTypeface(null, Typeface.BOLD);
                int index=0;    //이미지뷰 배열 인덱스
                //출발역일 때
                if(str.equals(startStnTextView2.getText())){
                    imageView = new ImageView[start2lines.size()];
                    for (String lineNm : start2lines) {
                        imageView[index]= new ImageView(this);
                        imageView[index++].setImageResource(SubwayLine.getResId(lineNm));
                    }
                    //도착역일 때
                }else if(str.equals(endStnTextView2.getText())){
                    imageView = new ImageView[end2lines.size()];
                    for (String lineNm : end2lines) {
                        imageView[index]= new ImageView(this);
                        imageView[index++].setImageResource(SubwayLine.getResId(lineNm));
                    }
                    //환승역일 때
                }else{
                    imageView = new ImageView[transLineNms2.get(symCount2).length];
                    for(int i=0; i<transLineNms2.get(symCount2).length;i++){
                        imageView[index]= new ImageView(this);
                        imageView[index++].setImageResource(SubwayLine.getResId(transLineNms2.get(symCount2)[i]));
                    }
                    symCount2++;
                }
                TextView.setText("\t"+str);
                RouteSymbol route = new RouteSymbol(this, imageView, TextView); //심볼과 역이름을 담은 레이아웃
                routefeeStn.addView(route, routeparams);
            }else {
                TextView.setText("\t"+str);
                routefeeStn.addView(TextView, routeparams);
            }
            TextView.setHeight(100);
        }

    }
    public void btnClick(View view){

        String time1string = getIntent().getStringExtra("route1Stntime");

        FeedTemplate params = FeedTemplate
                .newBuilder(ContentObject.newBuilder("동행_지하철어플리케이션",
                        "https://image.genie.co.kr/Y/IMAGE/IMG_ALBUM/081/191/791/81191791_1555664874860_1_600x600.JPG",
                        LinkObject.newBuilder().setWebUrl("https://developers.kakao.com")
                                .setMobileWebUrl("https://developers.kakao.com").build())
                        .setDescrption("목적지에 " + time1string + " 후 도착합니다.")
                        .build())
                .addButton(new ButtonObject("웹에서 보기", LinkObject.newBuilder().setWebUrl("https://developers.kakao.com").setMobileWebUrl("https://developers.kakao.com").build()))
                .addButton(new ButtonObject("앱에서 보기", LinkObject.newBuilder()
                        .setWebUrl("https://developers.kakao.com")
                        .setMobileWebUrl("https://developers.kakao.com")
                        .setAndroidExecutionParams("key1=value1")
                        .setIosExecutionParams("key1=value1")
                        .build()))
                .build();

        Map<String, String> serverCallbackArgs = new HashMap<String, String>();
        serverCallbackArgs.put("user_id", "${current_user_id}");
        serverCallbackArgs.put("product_id", "${shared_product_id}");


        KakaoLinkService.getInstance().sendDefault(this, params, new ResponseCallback <KakaoLinkResponse>() {
            @Override
            public void onFailure(ErrorResult errorResult) {}

            @Override
            public void onSuccess(KakaoLinkResponse result) {
            }
        });

    }

    public void btnClick2(View view){

        String time2string = getIntent().getStringExtra("route2Stntime");

        FeedTemplate params = FeedTemplate
                .newBuilder(ContentObject.newBuilder("동행_지하철어플리케이션",
                        "https://image.genie.co.kr/Y/IMAGE/IMG_ALBUM/081/191/791/81191791_1555664874860_1_600x600.JPG",
                        LinkObject.newBuilder().setWebUrl("https://developers.kakao.com")
                                .setMobileWebUrl("https://developers.kakao.com").build())
                        .setDescrption("목적지에 " + time2string + " 후 도착예정입니다.")
                        .build())
                .addButton(new ButtonObject("웹에서 보기", LinkObject.newBuilder().setWebUrl("https://developers.kakao.com").setMobileWebUrl("https://developers.kakao.com").build()))
                .addButton(new ButtonObject("앱에서 보기", LinkObject.newBuilder()
                        .setWebUrl("https://developers.kakao.com")
                        .setMobileWebUrl("https://developers.kakao.com")
                        .setAndroidExecutionParams("key1=value1")
                        .setIosExecutionParams("key1=value1")
                        .build()))
                .build();

        Map<String, String> serverCallbackArgs = new HashMap<String, String>();
        serverCallbackArgs.put("user_id", "${current_user_id}");
        serverCallbackArgs.put("product_id", "${shared_product_id}");


        KakaoLinkService.getInstance().sendDefault(this, params, new ResponseCallback <KakaoLinkResponse>() {
            @Override
            public void onFailure(ErrorResult errorResult) {}

            @Override
            public void onSuccess(KakaoLinkResponse result) {
            }
        });

    }


    public void Alarm1(View view){
        Intent intent = getIntent();
        int time1 = intent.getExtras().getInt("alarmtime1");
        Toast.makeText(getApplicationContext(), "알림이 설정되었습니다.", Toast.LENGTH_LONG).show();

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                fun1();
            }
        }, (time1*60000)-30000); //도착예정시간 30초전에 알림이 울린다.
    }

    public void fun1(){
        String endstation = getIntent().getStringExtra("endStn");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("동행_지하철어플리케이션");
        builder.setContentText(endstation+"에 곧 도착 예정입니다.");

        builder.setColor(Color.RED);
        // 사용자가 탭을 클릭하면 자동 제거
        builder.setAutoCancel(true);

        // 알림 표시
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
        }

        // id값은
        // 정의해야하는 각 알림의 고유한 int값
        notificationManager.notify(1, builder.build());

    }

    public void Alarm2(View view){
        Toast.makeText(getApplicationContext(), "알림이 설정되었습니다.", Toast.LENGTH_LONG).show();
        Intent intent = getIntent();
        int time2 = intent.getExtras().getInt("alarmtime2");

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                fun2();
            }
        }, (time2*60000)-30000); //도착예정시간 30초전에 알림이 울린다.
    }

    public void fun2(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
        String endstation = getIntent().getStringExtra("endStn");

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("동행_지하철어플리케이션");
        builder.setContentText(endstation+"에 곧 도착 예정입니다.");

        builder.setColor(Color.RED);
        // 사용자가 탭을 클릭하면 자동 제거
        builder.setAutoCancel(true);

        // 알림 표시
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
        }

        // id값은
        // 정의해야하는 각 알림의 고유한 int값
        notificationManager.notify(1, builder.build());

    }
}


