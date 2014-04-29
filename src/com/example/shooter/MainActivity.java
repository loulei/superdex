package com.example.shooter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;

import com.android.dex.Dex;
import com.example.shooter.dexmanager.DexManager;

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
        Dex dex = null;
        try {
            dex = new Dex(new File("/data/app/com.example.victim-1.apk"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        DexManager dm = new DexManager(dex);
        dm.hackDex("Lcom/example/victim/MainApplication;");

        try {
            File newdex = new File(this.getFilesDir(), "classes-1.dex");
            if (!newdex.exists()) {
                newdex.createNewFile();
            }
            dm.getDex().writeTo(newdex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void testNewDex() {
        ClassLoader cl = new DexClassLoader("/data/data/com.example.shooter/files/classes-1.dex", "/data/data/com.example.shooter/cache", null, this.getClassLoader());
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
