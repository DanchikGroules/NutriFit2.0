package com.nutrifit.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class AppDatabase extends SQLiteOpenHelper {
    public AppDatabase(Context context) { super(context,"nutrifit.db",null,1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE diary (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, meal TEXT NOT NULL, name TEXT NOT NULL, grams REAL NOT NULL CHECK(grams>0), kcal REAL NOT NULL, protein REAL NOT NULL, fat REAL NOT NULL, carbs REAL NOT NULL)");
        db.execSQL("CREATE INDEX diary_day ON diary(day)");
        db.execSQL("CREATE TABLE weights (day TEXT PRIMARY KEY, kg REAL NOT NULL CHECK(kg>0))");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        throw new IllegalStateException("Добавьте миграцию базы перед изменением версии");
    }
    public void add(String day,String meal,Food food,double grams) {
        if (!Double.isFinite(grams) || grams<=0 || grams>2000) throw new IllegalArgumentException("Порция: от 1 до 2000 г");
        ContentValues v=new ContentValues();
        v.put("day",day); v.put("meal",meal); v.put("name",food.name); v.put("grams",grams);
        v.put("kcal",food.kcal*grams/100); v.put("protein",food.protein*grams/100);
        v.put("fat",food.fat*grams/100); v.put("carbs",food.carbs*grams/100);
        getWritableDatabase().insertOrThrow("diary",null,v);
    }
    public List<Entry> diary(String day) {
        List<Entry> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,meal,name,grams,kcal,protein,fat,carbs FROM diary WHERE day=? ORDER BY id DESC",new String[]{day})) {
            while(c.moveToNext()) list.add(new Entry(c.getLong(0),c.getString(1),c.getString(2),c.getDouble(3),c.getDouble(4),c.getDouble(5),c.getDouble(6),c.getDouble(7)));
        }
        return list;
    }
    public void delete(long id) { getWritableDatabase().delete("diary","id=?",new String[]{Long.toString(id)}); }
    public void saveWeight(String day,double kg) {
        if (!Double.isFinite(kg) || kg<35 || kg>250) throw new IllegalArgumentException("Вес: 35–250 кг");
        ContentValues v=new ContentValues(); v.put("day",day); v.put("kg",kg);
        getWritableDatabase().insertWithOnConflict("weights",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<Weight> weights() {
        List<Weight> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT day,kg FROM weights ORDER BY day",null)) {
            while(c.moveToNext()) list.add(new Weight(c.getString(0),c.getDouble(1)));
        }
        return list;
    }
    public static final class Entry {
        public final long id;
        public final String meal,name;
        public final double grams,kcal,protein,fat,carbs;
        Entry(long id,String meal,String name,double grams,double kcal,double protein,double fat,double carbs) {
            this.id=id; this.meal=meal; this.name=name; this.grams=grams;
            this.kcal=kcal; this.protein=protein; this.fat=fat; this.carbs=carbs;
        }
    }
    public static final class Weight {
        public final String day; public final double kg;
        Weight(String day,double kg) { this.day=day; this.kg=kg; }
    }
}
