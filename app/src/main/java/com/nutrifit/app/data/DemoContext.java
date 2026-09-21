package com.nutrifit.app.data;

import android.content.*;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;

/** Separate files for guest exploration; never opens the personal database or preferences. */
public final class DemoContext extends ContextWrapper {
  public DemoContext(Context context) {super(context);}
  @Override public Context getApplicationContext(){return this;}
  @Override public File getDatabasePath(String name){return super.getDatabasePath("demo_"+name);}
  @Override public SQLiteDatabase openOrCreateDatabase(String n,int m,SQLiteDatabase.CursorFactory f){return super.openOrCreateDatabase("demo_"+n,m,f);}
  @Override public SQLiteDatabase openOrCreateDatabase(String n,int m,SQLiteDatabase.CursorFactory f,DatabaseErrorHandler h){return super.openOrCreateDatabase("demo_"+n,m,f,h);}
  @Override public boolean deleteDatabase(String n){return super.deleteDatabase("demo_"+n);}
  @Override public SharedPreferences getSharedPreferences(String n,int m){return super.getSharedPreferences("demo_"+n,m);}
  @Override public boolean deleteSharedPreferences(String n){return super.deleteSharedPreferences("demo_"+n);}
}
