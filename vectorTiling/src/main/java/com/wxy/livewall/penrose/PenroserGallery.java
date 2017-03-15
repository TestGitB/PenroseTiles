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
package com.wxy.livewall.penrose;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;

import com.wxy.tiling.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PenroserGallery extends Activity {
    private PenroserGLView penroserView = null;
    private Gallery gallery = null;
    private SharedPreferences sharedPreferences;
    private PenroserPreferences currentPreferences;
    private List<PenroserStaticView> savedPreferences = new ArrayList<>();

    private PenroserPreferences[] parseSavedPreferences(String savedPreferencesJson) {
        if (savedPreferencesJson == null) {
            return new PenroserPreferences[0];
        }
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(savedPreferencesJson);
        } catch (JSONException ex) {
            return new PenroserPreferences[0];
        }
        List<PenroserPreferences> prefs = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                PenroserPreferences preferences = new PenroserPreferences(jsonObject);
                prefs.add(preferences);
            } catch (JSONException ex) {
                Log.e("!", "JsonException:" + ex.getMessage());
            }
        }
        PenroserPreferences[] prefArray = new PenroserPreferences[prefs.size()];
        for (int i = 0; i < prefs.size(); i++) {
            prefArray[i] = prefs.get(i);
        }
        return prefArray;
    }

    private String deparseSavedPreferences() {
        JSONArray jsonArray = new JSONArray();
        for (PenroserStaticView penroserStaticView : savedPreferences) {
            jsonArray.put(penroserStaticView.getPreferences().toJsonObject());
        }
        return jsonArray.toString();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        ((PenroserApp) getApplication()).attemptUpgrade();
        Util.attemptUpgrade(getApplicationContext());
        currentPreferences = getIntent().getExtras().getParcelable("preferences");
        setContentView(R.layout.gallery);
        penroserView = (PenroserGLView) findViewById(R.id.penroser_view);
        penroserView.onPause();
        gallery = (Gallery) findViewById(R.id.gallery);
        penroserView.setPreferences(currentPreferences);
        sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
        String savedPrefsJson = sharedPreferences.getString("saved", null);
        registerForContextMenu(gallery);
        PenroserStaticView previousStaticView = null;
        for (PenroserPreferences preferences : parseSavedPreferences(savedPrefsJson)) {
            PenroserStaticView staticView = new PenroserStaticView(this, previousStaticView);
            previousStaticView = staticView;
            staticView.setPreferences(preferences);
            savedPreferences.add(staticView);
        }
        Button okButton = (Button) findViewById(R.id.ok);
        Button editButton = (Button) findViewById(R.id.edit);
        final SquareTextView currentColorsView = new SquareTextView(this);
        currentColorsView.setGravity(Gravity.CENTER);
        currentColorsView.setBackgroundColor( getResources().getColor(android.R.color.darker_gray));
        currentColorsView.setText(R.string.current);
        currentColorsView.setTextColor(Color.BLACK);
        setResult(-1);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                PenroserPreferences preferences = penroserView.getPreferences();
                intent.putExtra("preferences", preferences);
                setResult(0, intent);
                String prefJson = deparseSavedPreferences();
                SharedPreferenceUtil.savePreference(sharedPreferences, "saved", prefJson);
                finish();
            }
        });
        editButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(PenroserGallery.this, PenroserColorOptions.class));
                intent.putExtra("preferences", penroserView.getPreferences());
                startActivityForResult(intent, 0);
            }
        });
        gallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    penroserView.setPreferences(currentPreferences);
                } else {
                    penroserView.setPreferences(savedPreferences.get(position - 1).getPreferences());
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openContextMenu(view);
            }
        });
        gallery.setAdapter(new BaseAdapter() {
            public int getCount() {
                return savedPreferences.size() + 1;
            }

            public Object getItem(int position) {
                if (position == 0) {
                    return currentPreferences;
                } else if (position <= savedPreferences.size()) {
                    return savedPreferences.get(position - 1).getPreferences();
                }
                return null;
            }

            public long getItemId(int position) {
                return position;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                View view;
                if (position == 0) {
                    view = currentColorsView;
                } else {
                    view = savedPreferences.get(position - 1);
                }
                Gallery.LayoutParams params = new Gallery.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT);
                view.setLayoutParams(params);
                return view;
            }
        });
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
                    runOnUiThread(new Runnable() {
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
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            //clicked the edit button
            if (resultCode != -1) {
                currentPreferences = data.getExtras().getParcelable("preferences");
                this.gallery.setSelection(0);
            }
        } else if (requestCode == 1) {
            if (resultCode != -1) {
                int position = gallery.getSelectedItemPosition();
                PenroserPreferences preferences = data.getExtras().getParcelable("preferences");
                //clicked the edit entry in a gallery context menu
                savedPreferences.get(position - 1).setPreferences(preferences);
                penroserView.setPreferences(preferences);
            }
        }
    }
}