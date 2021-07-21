package com.example.myapplication.Activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.DB.Elevator;
import com.example.myapplication.DB.Station;
import com.example.myapplication.R;
import com.example.myapplication.Utiles.SubwayLine;


public class StnInfoFragment extends Fragment {
    private Station stn;
    // private Congestion congestion;
    private Elevator elevators;
    private int bgResId;

    /* View */
    //private NaverMapFragment mapFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            stn = bundle.getParcelable("Station"); //역 정보
            //  congestion = bundle.getParcelable("Congestion"); //혼잡도
            elevators = bundle.getParcelable("EvInfos"); //엘리베이터
            bgResId = SubwayLine.getBgResId(stn.getLineNm());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stn_info, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        /* View */
        View view = getView();
        if (view != null) {
            // 테두리 설정
            TableLayout basicContent = view.findViewById(R.id.basicContent);
            basicContent.setBackground(getResources().getDrawable(bgResId,null));
            LinearLayout mapContent = view.findViewById(R.id.mapContent);
            mapContent.setBackground(getResources().getDrawable(bgResId,null));
            RelativeLayout etcContent = view.findViewById(R.id.etcContent);
            etcContent.setBackground(getResources().getDrawable(bgResId,null));

            // 기본정보
            TextView toilet = view.findViewById(R.id.toilet);
            TextView door = view.findViewById(R.id.door);
            TextView contact = view.findViewById(R.id.contact);
            TextView elevator = view.findViewById(R.id.elevator);
            TextView escalator = view.findViewById(R.id.escalator);
            TextView wheelChairLift = view.findViewById(R.id.wheelChairLift);

            toilet.setText(String.format("%s", stn.getToilet()));
            door.setText(String.format("%s", stn.getDoor()));
            contact.setText(String.format("%s", stn.getCallNum()));
            elevator.setText(String.format("%s", stn.getElevator()));
            escalator.setText(String.format("%s", stn.getEscalator()));
            wheelChairLift.setText(String.format("%s", stn.getWheelLift()));

            TextView floor = view.findViewById(R.id.floor);
            TextView location = view.findViewById(R.id.location);

            floor.setText(String.format("엘리베이터 층 : %s", elevators.getFloor()));
            location.setText(String.format("엘리베이터 위치 : %s ", elevators.getLocation()));

            TextView timeTableBtn = view.findViewById(R.id.timetableBtn);
            timeTableBtn.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), TimeTablePagerActivity.class);
                intent.putExtra("lineNm", stn.getLineNm());
                intent.putExtra("name", stn.getName());
                intent.putExtra("bgResId", bgResId);
                if (getActivity() != null)
                    getActivity().startActivity(intent);
            });
        }
    }
}
