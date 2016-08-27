package com.threeosix.exp.shakemotiondetection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.threeosix.exp.shakemotiondetection.R;
import com.threeosix.exp.shakemotiondetection.utils.SharedPrefsUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by taufik on 8/28/16.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPrefsUtils.setBooleanPreference(this, "calibrating_done", false);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.button_start_app)
    public void startApp(){
        Intent mainControlIntent = new Intent(this, MainControlActivity.class);
        startActivity(mainControlIntent);
    }

    @OnClick(R.id.button_configure_app)
    public void configureApp(){
        Intent configureIntent = new Intent(this, DeviceListActivity.class);
        startActivity(configureIntent);
    }

}
