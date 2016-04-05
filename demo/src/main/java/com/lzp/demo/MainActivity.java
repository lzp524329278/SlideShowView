package com.lzp.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lzp.slideshowview.SlideShowView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SlideShowView slideShowView = (SlideShowView) findViewById(R.id.slideshowView);
        slideShowView.setUrlLoading(new SlideShowView.UrlLoading() {
            @Override
            public String[] loading() {
                return new String[]{
                        "http://www.lzpweb.cn/appmain_subject_2.png",
                        "http://www.lzpweb.cn/appmain_subject_3.png",
                        "http://www.lzpweb.cn/appmain_subject_4.png"
                };
            }
        });
        slideShowView.setTimeInterval(3000);//设置轮播间隔
        slideShowView.init();//初始化，初始化完成后会自动启动
       // slideShowView.startPlay();
       // slideShowView.stopPlay();//停止轮播
       // slideShowView.startPlay();//启动轮播
       // slideShowView.startPlay(3000);//以指定的轮播时间间隔启动
    }
}
