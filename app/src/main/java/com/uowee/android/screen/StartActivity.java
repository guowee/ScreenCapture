package com.uowee.android.screen;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.uowee.android.screen.capture.CaptureActivity;
import com.uowee.android.screen.live.RtmpActivity;
import com.uowee.android.screen.record.RecorderActivity;
import com.uowee.android.screen.test.TestActivity;
import com.uowee.android.screen.video.RtspActicity;

public class StartActivity extends ListActivity {
    private String[] mTitles = new String[]{
            CaptureActivity.class.getSimpleName(),
            RecorderActivity.class.getSimpleName(),
            RtspActicity.class.getSimpleName(),
            RtmpActivity.class.getSimpleName(),
            TestActivity.class.getSimpleName()
    };
    private Class[] mActivities = new Class[]{
            CaptureActivity.class,
            RecorderActivity.class,
            RtspActicity.class,
            RtmpActivity.class,
            TestActivity.class
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mTitles));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        startActivity(new Intent(this, mActivities[position]));
    }
}
