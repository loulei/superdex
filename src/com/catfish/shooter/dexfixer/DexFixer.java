package com.catfish.shooter.dexfixer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
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

    public void replaceSuperClass(String proxyDex, String tagetclassname) {
        Object toolActivity = mReflect.newInstance("mao.bytecode.ClassListActivity", null, null);
        LogUtil.i("toolActivity=" + toolActivity);

        mReflect.reflectMethod(toolActivity.getClass().getName(), "init", null, toolActivity, null);

        mReflect.setReflectField(toolActivity.getClass().getName(), "dexFile", toolActivity, mDexFile);
        mReflect.reflectMethod(toolActivity.getClass().getName(), "mergerDexFile", new Class[] { String.class }, toolActivity, new Object[] { proxyDex });

        createNewDex();
    }

    public void insertDexByMethod(String methoddesc, String instruction) {
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

        // sb.insert(0, instruction);
        LogUtil.i("new method insns --- " + sb.toString());
        mReflect.reflectMethod("mao.dalvik.Parser", "parse", new Class<?>[] { mDexFile.getClass(), String.class }, parser, new Object[] { mDexFile, instruction });

        createNewDex();
    }

    private void createNewDex() {
        mReflect.reflectMethod(mDexFile.getClass().getName(), "setInplace", new Class<?>[] { boolean.class }, mDexFile, new Object[] { false });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "place", null, mDexFile, null);
        int size = (Integer) mReflect.reflectMethod(mDexFile.getClass().getName(), "getFileSize", null, mDexFile, null);
        byte[] data = new byte[size];
        Object output = mReflect.newInstance("org.jf.dexlib.Util.ByteArrayAnnotatedOutput", new Class[] { byte[].class }, new Object[] { data });
        Class<?> AnnotatedOutput = mReflect.reflectClass("org.jf.dexlib.Util.AnnotatedOutput");
        mReflect.reflectMethod(mDexFile.getClass().getName(), "writeTo", new Class[] { AnnotatedOutput }, mDexFile, new Object[] { output });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "calcSignature", new Class[] { byte[].class }, mDexFile, new Object[] { data });
        mReflect.reflectMethod(mDexFile.getClass().getName(), "calcChecksum", new Class[] { byte[].class }, mDexFile, new Object[] { data });

        String[] dexpaths = mDexPath.split("/");
        String outFile = mContext.getFilesDir().getAbsolutePath() + "/" + dexpaths[dexpaths.length - 1];
        try {
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
        } catch (Exception e) {
            Log.e(LogUtil.TAG, e.getMessage(), e);
        }
    }

    private Object findEncodedMethod(String methoddesc) {
        if (methoddesc == null) {
            throw new IllegalArgumentException("method description can not be null");
        }
        Object ClassDefsSection = mReflect.reflectField(mDexFile.getClass().getName(), "ClassDefsSection", mDexFile);
        List<?> data = (List<?>) mReflect.reflectMethod("org.jf.dexlib.Section", "getItems", null, ClassDefsSection, null);
        for (Object ClassDefItem : data) {
            Object ClassDataItem = mReflect.reflectMethod(ClassDefItem.getClass().getName(), "getClassData", null, ClassDefItem, null);
            Object[] ms = (Object[]) mReflect.reflectMethod(ClassDataItem.getClass().getName(), "getVirtualMethods", null, ClassDataItem, null);
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
            ms = (Object[]) mReflect.reflectMethod(ClassDataItem.getClass().getName(), "getDirectMethods", null, ClassDataItem, null);
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
                // return file.getAbsolutePath();
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

    public static void fixTimeStampAndCrc(String srcODex, String targetODex) {
        try {
            RandomAccessFile srcFile = new RandomAccessFile(srcODex, "rw");
            RandomAccessFile tarFile = new RandomAccessFile(targetODex, "rw");

            int offset = getDepsOffset(srcFile);
            LogUtil.d("offset: --- " + Integer.toHexString(offset));

            srcFile.seek(offset);
            int modeTime = srcFile.readInt();
            int crc = srcFile.readInt();
            LogUtil.d("modeTime: --- " + Integer.toHexString(modeTime));
            LogUtil.d("crc: --- " + Integer.toHexString(crc));

            offset = getDepsOffset(tarFile);
            LogUtil.d("offset: --- " + Integer.toHexString(offset));

            tarFile.seek(offset);
            tarFile.writeInt(modeTime);
            tarFile.writeInt(crc);

            srcFile.close();
            tarFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // for little-end
    private static int getDepsOffset(RandomAccessFile file) {
        int result = 0;
        try {
            file.seek(16);
            int tesffo = file.readInt();
            result |= ((tesffo << 24) & 0xFF000000);
            result |= ((tesffo << 8) & 0xFF0000);
            result |= ((tesffo >> 8) & 0xFF00);
            result |= ((tesffo >> 24) & 0xFF);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
