package com.booboot.vndbandroid.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.booboot.vndbandroid.util.SettingsManager;
import com.booboot.vndbandroid.util.Utils;

public class URLActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(SettingsManager.getTheme(this));

        Uri data = getIntent().getData();
        String url = data.toString().substring(getPackageName().length() + 3);
        Utils.openURL(this, url);
        finish();
    }
}
