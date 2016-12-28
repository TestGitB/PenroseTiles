/*
 * [The "BSD licence"]
 * Copyright (c) 2011 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.wxy.tiling;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class PenroserActivity extends Activity {
    private static final String TAG = "PenroserActivity";
    public static final String PREFERENCE_NAME = "current_pref_activity";
    private SharedPreferences sharedPreferences = null;
    private PenroserGLView penroserView = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PenroserApp) getApplication()).attemptUpgrade();
        sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getFullScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        penroserView = new PenroserGLView(this);
        penroserView.onPause();
        Intent intent = getIntent();
        PenroserPreferences preferences = null;
        if (intent != null && intent.getAction() != null && (intent.getAction().equals("android.intent.action.VIEW") || intent.getAction().equals("android.nfc.action.NDEF_DISCOVERED"))) {
            Uri uri = intent.getData();
            preferences = buildPreferencesFroUri(uri);
        }
        if (preferences == null) {
            preferences = new PenroserPreferences(sharedPreferences, PREFERENCE_NAME);
        }
        penroserView.setPreferences(preferences);
        setContentView(penroserView);
    }

    private PenroserPreferences buildPreferencesFroUri(Uri uri) {
        try {
            return new PenroserPreferences(uri);
        } catch (Exception ex) {
            Log.e(TAG, "Error while parsing uri", ex);
            return null;
        }
    }

    @Override
    protected void onResume() {
        if (penroserView != null) {
            //work-around on 2.1. Needed because the wallpaper's visibility isn't changed until after we are displayed,
            //and if we try to resume the penroserView while the other one is still running, we end up getting into a
            //deadlock
            AsyncTask<Void, Void, Object> task = new AsyncTask<Void, Void, Object>() {
                @Override
                protected Object doInBackground(Void... params) {
                    while (PenroserLiveWallpaper.isAnyEngineVisible()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignore) {
                        }
                    }
                    PenroserActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            penroserView.onResume();
                        }
                    });
                    return null;
                }
            };
            task.execute();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (penroserView != null) {
            penroserView.onPause();
            penroserView.getPreferences().saveTo(sharedPreferences, PenroserActivity.PREFERENCE_NAME);
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.full_screen:
                toggleFullScreen();
                return true;
            case R.id.options:
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(this, PenroserGallery.class));
                intent.putExtra("preferences", penroserView.getPreferences());
                startActivityForResult(intent, 0);
                return true;
            case R.id.set_wallpaper:
                penroserView.getPreferences().saveTo(sharedPreferences, PenroserLiveWallpaper.PREFERENCE_NAME);
                WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(this).getWallpaperInfo();
                if (wallpaperInfo == null || wallpaperInfo.getComponent().compareTo(new ComponentName(this, PenroserLiveWallpaper.class)) != 0) {
                    Intent i = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                    startActivity(i);
                }
                finish();
                return true;
            case R.id.share_color_scheme:
                Intent i = new Intent(Intent.ACTION_SEND);
                i.putExtra(Intent.EXTRA_TEXT, penroserView.getPreferences().toPenroserHttpUri().toString());
                i.setType("text/plain");
                startActivity(Intent.createChooser(i, "Share via"));
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != -1) {
            PenroserPreferences preferences = data.getExtras().getParcelable("preferences");
            if (preferences != null) {
                preferences.saveTo(sharedPreferences, PREFERENCE_NAME);
                this.penroserView.setPreferences(preferences);
            }
        }
    }

    private boolean getFullScreen() {
        return sharedPreferences.getBoolean("full_screen", false);
    }

    private void setFullScreen(boolean fullScreen) {
        SharedPreferenceUtil.savePreference(sharedPreferences, "full_screen", fullScreen, false);
    }

    private void toggleFullScreen() {
        boolean fullScreen = getFullScreen();
        if (fullScreen) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setFullScreen(!fullScreen);
    }
}
