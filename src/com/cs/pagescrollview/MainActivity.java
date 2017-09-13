package com.cs.pagescrollview;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.LayoutInflaterCompat;
import android.view.LayoutInflater;

public class MainActivity extends FragmentActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory(LayoutInflater.from(this), new AutoLayoutInflaterFactory());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
