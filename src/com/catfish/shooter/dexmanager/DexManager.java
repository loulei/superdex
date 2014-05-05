package com.catfish.shooter.dexmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.catfish.shooter.utils.LogUtil;
import com.catfish.shooter.utils.ReflectImpl;

import dalvik.system.DexClassLoader;

public class DexManager {
    private Object mDexFile = null;
    private ReflectImpl mReflect = null;

    public DexManager(String dexPath, Context context) {
        if (dexPath == null || dexPath.length() == 0) {
            throw new IllegalArgumentException("dex input can not be null");
        }
        transferFiles(context, "bytecode.dex");
        ClassLoader cl = new DexClassLoader("/data/data/com.catfish.shooter/files/bytecode.dex", "/data/data/com.catfish.shooter/cache", null, ClassLoader.getSystemClassLoader());
        mReflect = new ReflectImpl(cl);
        mDexFile = mReflect.newInstance("org.jf.dexlib.DexFile", new Class[] { String.class }, new Object[] { dexPath });
    }

    public void hackDexByMethod(String methoddesc, boolean isVirtual) {
        Object encodedmethod = findEncodedMethod(methoddesc, isVirtual);
        Object codeItem = mReflect.reflectField(encodedmethod.getClass().getName(), "codeItem", encodedmethod);

        Class CodeItem = mReflect.reflectClass("org.jf.dexlib.CodeItem");
        Object parser = mReflect.newInstance("mao.dalvik.Parser", new Class<?>[] { CodeItem }, new Object[] { codeItem });
        StringBuilder sb = new StringBuilder(4096);
        Object writer = mReflect.newInstance("org.jf.util.IndentingWriter", new Class<?>[] { StringBuilder.class }, new Object[] { sb });
        mReflect.reflectMethod("mao.dalvik.Parser", "dump", new Class<?>[] { writer.getClass() }, parser, new Object[] { writer });
        LogUtil.d("method insns --- " + sb.toString());

        mReflect.reflectMethod("mao.dalvik.Parser", "parse", new Class<?>[] { mDexFile.getClass(), String.class }, parser, new Object[] { mDexFile,
                "const-string v0 \"catfish\"\n" +
                "const-string v1 \"victim APP ONCREATE\"\n" +
                "invoke-static {v0,v1} Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I\n" +
                "return-void" });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "place", null, mDexFile, null);

        int size = (Integer) mReflect.reflectMethod(mDexFile.getClass().getName(), "getFileSize", null, mDexFile, null);
        byte[] data = new byte[size];
        Object output = mReflect.newInstance("org.jf.dexlib.Util.ByteArrayAnnotatedOutput", new Class[] { byte[].class }, new Object[] { data });
        Class AnnotatedOutput = mReflect.reflectClass("org.jf.dexlib.Util.AnnotatedOutput");
        mReflect.reflectMethod(mDexFile.getClass().getName(), "writeTo", new Class[] { AnnotatedOutput }, mDexFile, new Object[] { output });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "calcSignature", new Class[] { byte[].class }, mDexFile, new Object[] { data });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "calcChecksum", new Class[] { byte[].class }, mDexFile, new Object[] { data });

        testOutputByteArry(data);
    }

    private void testOutputByteArry(byte[] dexbytes) {
        String dexPath = "/data/data/com.catfish.shooter/files/victim.dex";
        try {
            FileOutputStream os = new FileOutputStream(dexPath);
            os.write(dexbytes);
            os.close();
        } catch (Exception e) {
            Log.e(LogUtil.TAG, e.getMessage(), e);
        }
        Object dexfile = mReflect.newInstance("org.jf.dexlib.DexFile", new Class[] { String.class }, new Object[] { dexPath });

        Object encodedmethod = null;
        Object ClassDataSection = mReflect.reflectField(dexfile.getClass().getName(), "ClassDataSection", dexfile);
        List<Object> data = (List<Object>) mReflect.reflectMethod("org.jf.dexlib.Section", "getItems", null, ClassDataSection, null);
        for (Object cdi : data) {
            Object[] ms = null;
            ms = (Object[]) mReflect.reflectMethod(cdi.getClass().getName(), "getVirtualMethods", null, cdi, null);
            for (Object m : ms) {
                Object method = mReflect.reflectField(m.getClass().getName(), "method", m);
                String desc = (String) mReflect.reflectMethod(method.getClass().getName(), "getMethodString", null, method, null);
                if ("Lcom/example/victim/MainApplication;->onCreate()V".equals(desc)) {
                    LogUtil.i("found method --- " + "Lcom/example/victim/MainApplication;->onCreate()V");
                    encodedmethod = m;
                }
            }
        }

        Object codeItem = mReflect.reflectField(encodedmethod.getClass().getName(), "codeItem", encodedmethod);

        Class CodeItem = mReflect.reflectClass("org.jf.dexlib.CodeItem");
        Object parser = mReflect.newInstance("mao.dalvik.Parser", new Class<?>[] { CodeItem }, new Object[] { codeItem });
        StringBuilder sb = new StringBuilder(4096);
        Object writer = mReflect.newInstance("org.jf.util.IndentingWriter", new Class<?>[] { StringBuilder.class }, new Object[] { sb });
        mReflect.reflectMethod("mao.dalvik.Parser", "dump", new Class<?>[] { writer.getClass() }, parser, new Object[] { writer });
        Log.e(LogUtil.TAG, "method insns --- " + sb.toString());

    }

    private Object findEncodedMethod(String methoddesc, boolean isVirtual) {
        if (methoddesc == null) {
            throw new IllegalArgumentException("method description can not be null");
        }
        Object ClassDataSection = mReflect.reflectField(mDexFile.getClass().getName(), "ClassDataSection", mDexFile);
        List<Object> data = (List<Object>) mReflect.reflectMethod("org.jf.dexlib.Section", "getItems", null, ClassDataSection, null);
        for (Object cdi : data) {
            Object[] ms = null;
            if (isVirtual) {
                ms = (Object[]) mReflect.reflectMethod(cdi.getClass().getName(), "getVirtualMethods", null, cdi, null);
            } else {
                ms = (Object[]) mReflect.reflectMethod(cdi.getClass().getName(), "getDirectMethods", null, cdi, null);
            }
            if (ms == null) {
                return null;
            }
            for (Object m : ms) {
                Object method = mReflect.reflectField(m.getClass().getName(), "method", m);
                String desc = (String) mReflect.reflectMethod(method.getClass().getName(), "getMethodString", null, method, null);
                if (methoddesc.equals(desc)) {
                    LogUtil.i("found method --- " + methoddesc);
                    return m;
                }
            }
        }
        return null;
    }

    private final void transferFiles(Context context, String filename) {
        AssetManager assetManager = context.getAssets();
        try {
            String path = context.getFilesDir() + "/";
            File file = new File(path + filename);
            if (!file.exists()) {
                file.createNewFile();
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
        } catch (IOException e) {
            Log.e(LogUtil.TAG, "transfer files failed", e);
        } catch (Exception e) {
            Log.e(LogUtil.TAG, "transfer files failed", e);
        }
    }
}
