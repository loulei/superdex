package com.catfish.shooter;

import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.catfish.shooter.dexfixer.DexFixer;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("catfish", "hook success");
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refineDex();
        testNewDex();
        DexFixer.fixTimeStampAndCrc("/data/dalvik-cache/data@app@com.example.victim-2.apk@classes.dex",
                "/data/data/com.catfish.shooter/cache/data@app@com.example.victim-2.apk@classes.dex");
    }

    private void refineDex() {
        new DexFixer(this)
        .prepareForDex("/data/dalvik-cache/data@app@com.example.victim-2.apk@classes.dex")
        .insertDexBySuperClass("/data/data/com.catfish.shooter/files/classes.dex", "Lcom/example/victim/MainActivity;");
//        .insertDexByMethod("Lcom/example/victim/MainApplication;->onCreate()V",
//        .insertDexByMethod("Lcom/example/victim/MainActivity;->onCreate(Landroid/os/Bundle;)V",
//                "invoke-super {v1,v2} Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V\n" +
//                "const/high16 v0 0x7f03\n" +
//                "invoke-virtual {v1,v0} Lcom/example/victim/MainActivity;->setContentView(I)V\n" +
//                "const-string v0 \"catfish\"\n"
//                        + "const-string v1 \"victim APP ONCREATE\"\n"
//                        + "invoke-static {v0,v1} Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I\n");
//                        + "return-void");
    }

    private void testNewDex() {
        ClassLoader cl = new DexClassLoader("/data/data/com.catfish.shooter/files/data@app@com.example.victim-2.apk@classes.dex", "/data/data/com.catfish.shooter/cache", null, ClassLoader.getSystemClassLoader());
        try {
            Class<?> clz = cl.loadClass("com.example.victim.MainActivity");
//            Class<?> clz = cl.loadClass("com.example.victim.MainApplication");
            Object app = clz.newInstance();

//            Method m = clz.getDeclaredMethod("onCreate", (Class[]) null);
            Method m = clz.getDeclaredMethod("onCreate", Bundle.class);
            m.setAccessible(true);
//            m.invoke(app, null);
            m.invoke(app, new Bundle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
