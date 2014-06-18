package com.catfish.superdex;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

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
                refineDex(MainActivity.this);
                testNewDex();
                DexPatcher.fixOdexTimeStampAndCrc("/data/dalvik-cache/data@app@com.tencent.mm-1.apk@classes.dex",
                        "/data/data/com.catfish.superdex/cache/newdex.dex");
            }
        }.start();
        finish();
    }

    private void refineDex(Context context) {
        String[] mergerargs = new String[3];
        mergerargs[0] = "/data/data/com.catfish.superdex/files/newdex.dex";
        mergerargs[1] = "/data/app/com.tencent.mm-1.apk";
        mergerargs[2] = context.getPackageCodePath();

        try {
            DexMerger.main(mergerargs);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DexPatcher.patchTargetDex(mergerargs[0], "Lcom/tencent/mm/app/MMApplication;->onCreate()V",
                "Lcom/catfish/superdex/MainActivity;->needleMethod()V");
    }

    private void testNewDex() {
        new DexClassLoader("/data/data/com.catfish.superdex/files/newdex.dex",
                "/data/data/com.catfish.superdex/cache/", null, ClassLoader.getSystemClassLoader());
    }
}
