package com.example.myapplication;

import android.app.Activity;
import android.app.TabActivity;
import android.os.Bundle;
import android.widget.TabHost;

@SuppressWarnings("deprecation") // API 미지원 경고 삭제
public class mainAct extends TabActivity {	//TabActivity 상속
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        TabHost tabhost = getTabHost();	//탭호스트 생성

        //탭 스펙에 컨텐츠 탑재
        TabHost.TabSpec tabSpecDog= tabhost.newTabSpec("리스트").setIndicator("lay1");
        tabSpecDog.setContent(R.id.list);
        tabhost.addTab(tabSpecDog); //탭호스트에 등록
        TabHost.TabSpec tabSpecCat= tabhost.newTabSpec("lay2").setIndicator("lay2");
        tabSpecCat.setContent(R.id.mylist);
        tabhost.addTab(tabSpecCat);
        TabHost.TabSpec tabSpecRabbit= tabhost.newTabSpec("lay3").setIndicator("lay3");
        tabSpecRabbit.setContent(R.id.news);
        tabhost.addTab(tabSpecRabbit);
        TabHost.TabSpec tabSpecHorse= tabhost.newTabSpec("lay4").setIndicator("lay4");
        tabSpecHorse.setContent(R.id.setting);
        tabhost.addTab(tabSpecHorse);
        tabhost.setCurrentTab(0); //탭 초기값 설정
    }
}