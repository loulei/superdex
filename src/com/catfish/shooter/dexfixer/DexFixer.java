package com.catfish.shooter.dexfixer;

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

public class DexFixer {
    private Object mDexFile = null;
    private ReflectImpl mReflect = null;
    private static final String DEX_LIB = "bytecode.dex";
    private Context mContext = null;
    private String mDexPath = null;

    public DexFixer(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = context;
    }

    public DexFixer prepareForDex(String dexPath) {
        if (dexPath == null || dexPath.length() == 0) {
            throw new IllegalArgumentException("dex input can not be null");
        }

        mDexPath = dexPath;
        prepare(dexPath, mContext);
        return this;
    }

    private void prepare(String dexPath, Context context) {
        String dexLib = transferFiles(context, DEX_LIB);

        ClassLoader cl = new DexClassLoader(dexLib, context.getCacheDir().getAbsolutePath(), null, ClassLoader.getSystemClassLoader());
        mReflect = new ReflectImpl(cl);

        mDexFile = mReflect.newInstance("org.jf.dexlib.DexFile", new Class[] { String.class }, new Object[] { dexPath });
    }

    public void fixDexByMethod(String methoddesc, String instruction) {
        if (mDexFile == null) {
            throw new RuntimeException("you need call prepareForDex first");
        }
        Object encodedmethod = findEncodedMethod(methoddesc);
        Object codeItem = mReflect.reflectField(encodedmethod.getClass().getName(), "codeItem", encodedmethod);

        Object parser = mReflect.newInstance("mao.dalvik.Parser", new Class<?>[] { codeItem.getClass() }, new Object[] { codeItem });
        StringBuilder sb = new StringBuilder(4096);
        Object writer = mReflect.newInstance("org.jf.util.IndentingWriter", new Class<?>[] { StringBuilder.class }, new Object[] { sb });
        mReflect.reflectMethod("mao.dalvik.Parser", "dump", new Class<?>[] { writer.getClass() }, parser, new Object[] { writer });
        LogUtil.d("method insns --- " + sb.toString());

        mReflect.reflectMethod("mao.dalvik.Parser", "parse", new Class<?>[] { mDexFile.getClass(), String.class }, parser, new Object[] { mDexFile, instruction });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "place", null, mDexFile, null);

        int size = (Integer) mReflect.reflectMethod(mDexFile.getClass().getName(), "getFileSize", null, mDexFile, null);
        byte[] data = new byte[size];
        Object output = mReflect.newInstance("org.jf.dexlib.Util.ByteArrayAnnotatedOutput", new Class[] { byte[].class }, new Object[] { data });
        Class<?> AnnotatedOutput = mReflect.reflectClass("org.jf.dexlib.Util.AnnotatedOutput");
        mReflect.reflectMethod(mDexFile.getClass().getName(), "writeTo", new Class[] { AnnotatedOutput }, mDexFile, new Object[] { output });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "calcSignature", new Class[] { byte[].class }, mDexFile, new Object[] { data });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "calcChecksum", new Class[] { byte[].class }, mDexFile, new Object[] { data });

        createNewDex(data);
    }

    private void createNewDex(byte[] dexbytes) {
        String[] dexpaths = mDexPath.split("/");
        String outFile = mContext.getFilesDir().getAbsolutePath() + "/" + dexpaths[dexpaths.length - 1];
        try {
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(dexbytes);
            os.close();
        } catch (Exception e) {
            Log.e(LogUtil.TAG, e.getMessage(), e);
        }
    }

    private Object findEncodedMethod(String methoddesc) {
        if (methoddesc == null) {
            throw new IllegalArgumentException("method description can not be null");
        }
        Object ClassDataSection = mReflect.reflectField(mDexFile.getClass().getName(), "ClassDataSection", mDexFile);
        List<?> data = (List<?>) mReflect.reflectMethod("org.jf.dexlib.Section", "getItems", null, ClassDataSection, null);
        for (Object cdi : data) {
            Object[] ms = (Object[]) mReflect.reflectMethod(cdi.getClass().getName(), "getVirtualMethods", null, cdi, null);
            if (ms != null) {
                for (Object m : ms) {
                    Object method = mReflect.reflectField(m.getClass().getName(), "method", m);
                    String desc = (String) mReflect.reflectMethod(method.getClass().getName(), "getMethodString", null, method, null);
                    if (methoddesc.equals(desc)) {
                        LogUtil.i("found method --- " + methoddesc);
                        return m;
                    }
                }
            }
            ms = (Object[]) mReflect.reflectMethod(cdi.getClass().getName(), "getDirectMethods", null, cdi, null);
            if (ms != null) {
                for (Object m : ms) {
                    Object method = mReflect.reflectField(m.getClass().getName(), "method", m);
                    String desc = (String) mReflect.reflectMethod(method.getClass().getName(), "getMethodString", null, method, null);
                    if (methoddesc.equals(desc)) {
                        LogUtil.i("found method --- " + methoddesc);
                        return m;
                    }
                }
            }
        }
        return null;
    }

    private final String transferFiles(Context context, String filename) {
        AssetManager assetManager = context.getAssets();
        try {
            String path = context.getFilesDir() + "/";
            File file = new File(path + filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            if (file.lastModified() > new File(mContext.getPackageCodePath()).lastModified()) {
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
