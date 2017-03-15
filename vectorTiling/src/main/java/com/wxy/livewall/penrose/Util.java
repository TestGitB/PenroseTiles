package com.wxy.livewall.penrose;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

import static android.content.Context.MODE_PRIVATE;


class Util {
    /*This is the number of levels to use for the static vbo data*/
    /*package*/ static final int VBO_LEVEL = 5;

    /*The default initial scale, based on the number of levels down that we are pre-generating*/
    static final float DEFAULT_INITIAL_SCALE = (float) (500 * Math.pow((Math.sqrt(5) + 1) / 2, VBO_LEVEL - 5));
    //TODO: need to move this to GLContext
    static final HalfRhombusPool halfRhombusPool = new HalfRhombusPool();
    static final Random random = new Random();

    private static boolean needsUpgrade = true;
    static void attemptUpgrade(Context context) {
        if (needsUpgrade) {
            SharedPreferences oldPreferences = context.getSharedPreferences("penroser_live_wallpaper_prefs", MODE_PRIVATE);
            SharedPreferences newPreferences = context.getSharedPreferences("preferences", MODE_PRIVATE);
            if (oldPreferences.contains("left_skinny_color")) {
                PenroserPreferences preferences = new PenroserPreferences();
                for (HalfRhombusType rhombusType : HalfRhombusType.values()) {
                    preferences.setColor(rhombusType, ColorUtil.swapOrder(oldPreferences.getInt(rhombusType.colorKey, rhombusType.defaultColor)));
                }
                preferences.saveTo(newPreferences, PenroserLiveWallpaper.PREFERENCE_NAME);
                oldPreferences.edit().clear().apply();
            }
            oldPreferences = context.getSharedPreferences("penroser_activity_prefs", MODE_PRIVATE);
            boolean clearPref = false;
            if (oldPreferences.contains("full_screen")) {
                SharedPreferenceUtil.savePreference(newPreferences, "full_screen", oldPreferences.getBoolean("full_screen", false), false);
                clearPref = true;
            }
            if (clearPref) {
                oldPreferences.edit().clear().apply();
            }
            if (newPreferences.getInt("first_run", 1) != 0) {
                SharedPreferenceUtil.savePreference(newPreferences, "saved", "[" +
                        "{\"scale\":1,\"left_skinny_color\":0,\"left_fat_color\":7509713,\"right_fat_color\":0,\"right_skinny_color\":7509713}, " +
                        "{\"scale\":1,\"left_skinny_color\":2112,\"left_fat_color\":33331,\"right_fat_color\":9498,\"right_skinny_color\":11382}, " +
                        "{\"scale\":0.367832458,\"left_skinny_color\":13920,\"left_fat_color\":0,\"right_fat_color\":0,\"right_skinny_color\":27554}" +
                        "]");
                SharedPreferenceUtil.savePreference(newPreferences, "first_run", 0, 1);
            }
            needsUpgrade = false;
        }
    }
}
