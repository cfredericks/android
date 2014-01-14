package com.cfred1985.camerafx;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

import com.cfred1985.util.camera.CameraSurfaceView;

public class MyMainActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_my_main);
        RelativeLayout mRelativeLayout = new RelativeLayout(getApplicationContext());
        setContentView(mRelativeLayout);
        SurfaceView surfaceView = new CameraSurfaceView(this, null);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        surfaceView.setLayoutParams(layoutParams);
        mRelativeLayout.addView(surfaceView);
    }
}
