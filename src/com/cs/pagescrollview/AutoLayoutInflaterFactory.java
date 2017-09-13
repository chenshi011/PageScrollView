package com.cs.pagescrollview;

import android.content.Context;
import android.support.v4.view.LayoutInflaterFactory;
import android.util.AttributeSet;
import android.view.View;

public class AutoLayoutInflaterFactory implements LayoutInflaterFactory{

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        switch (name) {
        case "ScrollView":
            PageScrollView pageScrollView = new PageScrollView(context, attrs);
            pageScrollView.setFixLastPageHeight(true);
            return pageScrollView;
            
        default:
            break;
        }
        return null;
    }
}
