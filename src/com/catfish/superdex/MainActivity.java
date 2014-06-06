package com.catfish.superdex;

import java.io.IOException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.android.dx.merge.DexMerger;
import com.catfish.superdex.dexpatcher.DexPatcher;

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
        new Thread() {
            public void run() {
                refineDex();
                testNewDex();
            }
        }.start();
    }

    private void refineDex() {
        String[] mergerargs = new String[3];
        mergerargs[0] = "/data/data/com.catfish.superdex/files/newdex.dex";
        mergerargs[1] = "/data/data/com.catfish.superdex/files/catfish.apk";
        mergerargs[2] = "/data/data/com.catfish.superdex/files/classes.dex";

        try {
            DexMerger.main(mergerargs);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DexPatcher.patchTargetDex(mergerargs[0], "Lcom/catfish/center/ui/CatfishCenterActivity;-><init>()V",
                "Lcom/catfish/superdex/MainActivity;->main()V");
    }

    private void testNewDex() {
        ClassLoader cl = new DexClassLoader("/data/data/com.catfish.superdex/files/newdex.dex",
                "/data/data/com.catfish.superdex/cache", null, ClassLoader.getSystemClassLoader());
        try {
            Class<?> clz = cl.loadClass("com.catfish.center.ui.CatfishCenterActivity");
            Object app = clz.newInstance();

            Method m = clz.getDeclaredMethod("onCreate", Bundle.class);
            m.setAccessible(true);
            m.invoke(app, new Bundle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main() {
        Log.e("catfish", "invoke done!");
    }
}
