package com.nutrifit.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;

public class ProfileStore {
    private final SharedPreferences prefs;
    public ProfileStore(Context c) { prefs=c.getSharedPreferences("profile",Context.MODE_PRIVATE); }
    public Profile load() {
        if(!prefs.getBoolean("ready",false)) return null;
        Profile p=new Profile();
        p.name=prefs.getString("name",""); p.age=prefs.getInt("age",25);
        p.height=Double.parseDouble(prefs.getString("height","175"));
        p.weight=Double.parseDouble(prefs.getString("weight","75"));
        p.targetWeight=Double.parseDouble(prefs.getString("target","70"));
        p.male=prefs.getBoolean("male",true); p.activity=prefs.getInt("activity",0); p.goal=prefs.getInt("goal",1);
        p.likes=new HashSet<>(prefs.getStringSet("likes",new HashSet<>()));
        p.dislikes=new HashSet<>(prefs.getStringSet("dislikes",new HashSet<>()));
        p.allergies=new HashSet<>(prefs.getStringSet("allergies",new HashSet<>()));
        p.restrictions=new HashSet<>(prefs.getStringSet("restrictions",new HashSet<>()));
        return p;
    }
    public void save(Profile p) {
        prefs.edit().putBoolean("ready",true).putString("name",p.name).putInt("age",p.age)
            .putString("height",Double.toString(p.height)).putString("weight",Double.toString(p.weight))
            .putString("target",Double.toString(p.targetWeight)).putBoolean("male",p.male)
            .putInt("activity",p.activity).putInt("goal",p.goal).putStringSet("likes",p.likes)
            .putStringSet("dislikes",p.dislikes).putStringSet("allergies",p.allergies)
            .putStringSet("restrictions",p.restrictions).apply();
    }
}
