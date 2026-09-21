package com.nutrifit.app.data;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Process-scoped queue: writes survive Activity recreation and precede subsequent reads. */
public final class LocalRepository {
  private static LocalRepository instance;
  private static LocalRepository demoInstance;
  public final boolean isDemo;
  private final android.content.SharedPreferences session;
  public final ExecutorService io = Executors.newSingleThreadExecutor();
  public final AppDatabase db;
  public final ProfileStore profiles;
  public final LocalAccount account;
  public final LibraryStore library;
  public final android.content.SharedPreferences features;

  private LocalRepository(Context context,boolean demo) {
    Context original=context.getApplicationContext();
    session=original.getSharedPreferences("app_session",Context.MODE_PRIVATE);
    isDemo=demo;
    Context app = demo?new DemoContext(original):original;
    db = new AppDatabase(app);
    profiles = new ProfileStore(app);
    account = new LocalAccount(app);
    library = new LibraryStore(app, db);
    features = app.getSharedPreferences("features", Context.MODE_PRIVATE);
  }

  public boolean premium() {
    return features.getBoolean("demo_premium", false);
  }

  public void premium(boolean enabled) {
    if (!features.edit().putBoolean("demo_premium", enabled).commit())
      throw new IllegalStateException("write_failed");
  }

  public static synchronized LocalRepository get(Context context) {
    if(context.getApplicationContext().getSharedPreferences("app_session",Context.MODE_PRIVATE).getBoolean("guest",false)) {
      if(demoInstance==null)demoInstance=new LocalRepository(context,true);
      return demoInstance;
    }
    if (instance == null) instance = new LocalRepository(context,false);
    return instance;
  }

  public boolean hasAccess() {return isDemo?session.getBoolean("guest",false):account.signedIn();}

  public void signOut() {
    if(isDemo) {
      if(!session.edit().putBoolean("guest",false).commit())throw new IllegalStateException("session_write_failed");
    } else account.logout();
  }

  /** Call from the auth ViewModel IO queue; guest content survives process recreation. */
  public static synchronized void startDemo(Context context,String name,String language) throws Exception {
    if(demoInstance==null)demoInstance=new LocalRepository(context,true);
    LocalRepository demo=demoInstance;
    demo.library.initialize(language);
    if(demo.profiles.load()==null) {
      com.nutrifit.app.model.Profile profile=new com.nutrifit.app.model.Profile();
      profile.name=name;
      demo.profiles.save(profile);
      demo.library.favorite("recipe_001",true);
      demo.library.favorite("recipe_021",true);
      demo.premium(true);
    }
    if(!demo.session.edit().putBoolean("guest",true).commit())throw new IllegalStateException("session_write_failed");
  }
}
