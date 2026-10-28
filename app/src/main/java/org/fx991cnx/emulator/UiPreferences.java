package org.fx991cnx.emulator;

import android.content.Context;
import android.content.SharedPreferences;

final class UiPreferences {
 static final String[] THEME_FILES={"blue","ivory","pink","black"};
 private final SharedPreferences preferences;
 UiPreferences(Context context){preferences=context.getSharedPreferences("appearance_and_feedback",Context.MODE_PRIVATE);}
 int theme(){return Math.max(0,Math.min(3,preferences.getInt("theme",3)));}
 boolean vibration(){return preferences.getBoolean("vibration",true);}
 int strength(){return Math.max(0,Math.min(2,preferences.getInt("vibration_strength",0)));}
 boolean sound(){return preferences.getBoolean("sound",false);}
 int volume(){return Math.max(0,Math.min(100,preferences.getInt("sound_volume",35)));}
 void vibrationLevel(int level){preferences.edit().putBoolean("vibration",level>0).putInt("vibration_strength",Math.max(0,level-1)).apply();}
 void soundLevel(int level){preferences.edit().putBoolean("sound",level>0).putInt("sound_volume",new int[]{0,20,50,85}[level]).apply();}
 void theme(int value){preferences.edit().putInt("theme",value).apply();}
}
