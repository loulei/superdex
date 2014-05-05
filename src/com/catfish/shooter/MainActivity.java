package com.catfish.shooter;

import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;

import com.catfish.shooter.dexfixer.DexFixer;

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
        new DexFixer(this)
        .prepareForDex("/data/dalvik-cache/data@app@com.example.victim-1.apk@classes.dex")
        .fixDexByMethod("Lcom/example/victim/MainApplication;->onCreate()V",
                "const-string v0 \"catfish\"\n"
                        + "const-string v1 \"victim APP ONCREATE\"\n"
                        + "invoke-static {v0,v1} Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I\n"
                        + "return-void");
    }

    private void testNewDex() {
        ClassLoader cl = new DexClassLoader("/data/data/com.catfish.shooter/files/data@app@com.example.victim-1.apk@classes.dex", "/data/data/com.catfish.shooter/cache", null, this.getClassLoader());
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
