package com.catfish.shooter;

import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;

import com.catfish.shooter.dexmanager.DexManager;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refineDex();
        testNewDex();
    }

    private void refineDex() {
        DexManager dm = new DexManager("/data/dalvik-cache/data@app@com.example.victim-1.apk@classes.dex", this);
        dm.hackDexByMethod("Lcom/example/victim/MainApplication;->onCreate()V", true);
    }

    private void testNewDex() {
        ClassLoader cl = new DexClassLoader("/data/data/com.catfish.shooter/files/victim.dex", "/data/data/com.catfish.shooter/cache", null, this.getClassLoader());
        try {
            Class<?> clz = cl.loadClass("com.example.victim.MainApplication");
            Object app = clz.newInstance();

            Method m = clz.getDeclaredMethod("onCreate", (Class[]) null);
            m.invoke(app, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
