package com.example.myapplication.Activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.myapplication.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class TimeTablePagerActivity extends AppCompatActivity {


    //페이지 넘겨서 페이지 실행
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_table_pager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager viewPager = findViewById(R.id.viewPager);
        //라인이름,역이름을 받아서 넣는다?

        String lineNm = getIntent().getStringExtra("lineNm");
        String name = getIntent().getStringExtra("name");
        int bgResId = getIntent().getIntExtra("bgResId", -1);

        // 상단 TextView 텍스트 출력
        //맨위에 몇호선 무슨역
        TextView stnNmView = findViewById(R.id.stnNm);
        stnNmView.setText(String.format("%s %s", lineNm, name));
 /*
        //밑에 꺽세표(왼쪽 오른쪽)
        TextView upStnNmView = findViewById(R.id.upwardStnNm);
        String upStnNms = DownLine.getNextStnNms(lineNm, name, 1);
        upStnNmView.setText(String.format("%s",upStnNms));


        TextView downStnNmView = findViewById(R.id.downwardStnNm);
        String downStnNms = DownLine.getNextStnNms(lineNm, name, 2);
        downStnNmView.setText(String.format("%s",downStnNms));
*/




            //번들에 lineNm과 name을 넣어둔다.
        Bundle[] bundle = new Bundle[3];
        for (int i = 0; i < bundle.length; i++) {
            bundle[i] = new Bundle();
            bundle[i].putString("lineNm", lineNm);
            bundle[i].putString("name", name);
            //bundle[i].putString("activity",activity);
            bundle[i].putInt("weekTag", i + 1);
        }



        //3개를 만드는 이유는 cafe, store, activity 3가지의 페이지를 만들기 위해서이다.
        TimeTableFragment cafeFragment = new TimeTableFragment();
        TimeTableFragment2 storeFragment = new TimeTableFragment2();
        TimeTableFragment3 activityFragment = new TimeTableFragment3();

        cafeFragment.setArguments(bundle[0]);   //0에 lineNm,name,weekTag 값이 들어간다.
        storeFragment.setArguments(bundle[0]);
        activityFragment.setArguments(bundle[0]);

        //3개의 페이지에 lineNm, name, weekTag를 각각 넣어준다.
        TTPagerAdapter vpAdapter = new TTPagerAdapter(getSupportFragmentManager());

        vpAdapter.addFragment(cafeFragment, getString(R.string.cafe));
        vpAdapter.addFragment(storeFragment, getString(R.string.store));
        vpAdapter.addFragment(activityFragment, getString(R.string.activity));
        viewPager.setAdapter(vpAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

        if (bgResId != -1) {
            tabLayout.setBackground(getResources().getDrawable(bgResId, null));
            viewPager.setBackground(getResources().getDrawable(bgResId, null));
        }


    }
    private class TTPagerAdapter extends FragmentPagerAdapter {
        private ArrayList<Fragment> fragments = new ArrayList<>();
        private ArrayList<String> titles = new ArrayList<>();

        TTPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }

}
