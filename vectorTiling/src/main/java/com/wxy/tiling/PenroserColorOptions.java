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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
 public class PenroserColorOptions extends Activity {
    private HalfRhombusButton halfRhombusButtons[] = new HalfRhombusButton[4];
    private PenroserGLView penroserView = null;
     private Handler handler = new Handler();
     private Integer copiedColor = null;
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         ((PenroserApp)getApplication()).attemptUpgrade();
         setContentView(R.layout.options);
         penroserView = (PenroserGLView)findViewById(R.id.penroser_view);
        penroserView.onPause();
         PenroserPreferences preferences = (PenroserPreferences)getIntent().getParcelableExtra("preferences");
        penroserView.setPreferences(preferences);
         for (HalfRhombusType rhombusType: HalfRhombusType.values()) {
            HalfRhombusButton button = (HalfRhombusButton)findViewById(rhombusType.viewId);
            button.setOnClickListener(rhombusClickListener);
            button.setColor(preferences.getColor(rhombusType));
             registerForContextMenu(button);
             halfRhombusButtons[rhombusType.index] = button;
        }
         setResult(-1);
         Button okButton = (Button)findViewById(R.id.ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("preferences", penroserView.getPreferences());
                setResult(0, intent);
                finish();
            }
        });
    }
     @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != -1) {
            int rhombusIndex = requestCode;
            PenroserPreferences preferences = data.getExtras().getParcelable("preferences");
             HalfRhombusType rhombusType = HalfRhombusType.fromIndex(rhombusIndex);
             int color = preferences.getColor(rhombusType);
            penroserView.setPreferences(preferences);
             HalfRhombusButton button = halfRhombusButtons[rhombusIndex];
            button.setColor(color);
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
                        } catch (InterruptedException ex) {
                        }
                    }
                    penroserView.onResume();
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
        }
        super.onPause();
    }
     @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
         MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.color_context, menu);
        if (copiedColor == null) {
            MenuItem pasteItem = menu.findItem(R.id.paste_color);
            pasteItem.setEnabled(false);
        }
    }
     @Override
    public boolean onContextItemSelected(MenuItem item) {
        HalfRhombusButton halfRhombusButton = (HalfRhombusButton)item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.edit_color:
                halfRhombusButton.performClick();
                return true;
            case R.id.copy_color:
                this.copiedColor = halfRhombusButton.getColor();
                return true;
            case R.id.paste_color:
                if (copiedColor != null) {
                    halfRhombusButton.setColor(this.copiedColor);
                     PenroserPreferences preferences = penroserView.getPreferences();
                    preferences.setColor(halfRhombusButton.getRhombusType(), this.copiedColor);
                    penroserView.setPreferences(preferences);
                }
                return true;
        }
        return super.onContextItemSelected(item);
    }
     private final View.OnClickListener rhombusClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.setClass(PenroserColorOptions.this, PenroserColorPicker.class);
            intent.putExtra("rhombus", ((HalfRhombusButton)v).getRhombusType());
            intent.putExtra("preferences", penroserView.getPreferences());
            startActivityForResult(intent, ((HalfRhombusButton)v).getRhombusType().index);
        }
    };
}