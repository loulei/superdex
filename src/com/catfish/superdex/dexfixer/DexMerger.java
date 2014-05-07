package com.catfish.superdex.dexfixer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import android.util.Log;

import com.catfish.superdex.utils.LogUtil;
import com.catfish.superdex.utils.ReflectImpl;

import dalvik.system.DexClassLoader;

public class DexMerger {

    private static String sTargetDexPath = null;
    private Object mTargetDexFile = null;
    private ReflectImpl mReflect = null;
    private String mPatchDexPath = null;

    public DexMerger(String targetDexPath, String patchDexPath) {
        if (targetDexPath == null || patchDexPath == null) {
            throw new IllegalArgumentException("dex path input can not be null");
        }
        sTargetDexPath = targetDexPath;
        mPatchDexPath = patchDexPath;

//        mReflect = new ReflectImpl(getClass().getClassLoader());
        ClassLoader cl = new DexClassLoader("/data/data/com.catfish.superdex/files/bytecode.dex", "/data/data/com.catfish.superdex/cache/", null, ClassLoader.getSystemClassLoader());
        mReflect = new ReflectImpl(cl);

        mTargetDexFile = mReflect.newInstance("org.jf.dexlib.DexFile", new Class[] { String.class }, new Object[] { targetDexPath });
        mergeDex(mPatchDexPath);
    }

    public void patchTargetDex(String tagetclassname, String patchclassname) {

        Object proxyClass = findClassDefItem(patchclassname);
        Object targetClass = findClassDefItem(tagetclassname);
        Object proxyType = mReflect.reflectField(proxyClass.getClass().getName(), "classType", proxyClass);
        mReflect.setReflectField(targetClass.getClass().getName(), "superType", targetClass, proxyType);
    }

    private void mergeDex(String proxyDex) {
        Object toolActivity = mReflect.newInstance("mao.bytecode.ClassListActivity", null, null);
        mReflect.reflectMethod(toolActivity.getClass().getName(), "init", null, toolActivity, null);
        mReflect.setReflectField(toolActivity.getClass().getName(), "dexFile", toolActivity, mTargetDexFile);
        mReflect.reflectMethod(toolActivity.getClass().getName(), "mergerDexFile", new Class[] { String.class }, toolActivity, new Object[] { proxyDex });

        mReflect.reflectMethod(mTargetDexFile.getClass().getName(), "setInplace", new Class<?>[] { boolean.class }, mTargetDexFile, new Object[] { false });
        mReflect.reflectMethod(mTargetDexFile.getClass().getName(), "place", null, mTargetDexFile, null);
    }

    private Object findClassDefItem(String tagetclassname) {
        if (tagetclassname == null) {
            throw new IllegalArgumentException("tagetclassname can not be null");
        }
        Object ClassDefsSection = mReflect.reflectField(mTargetDexFile.getClass().getName(), "ClassDefsSection", mTargetDexFile);
        List<?> data = (List<?>) mReflect.reflectMethod("org.jf.dexlib.Section", "getItems", null, ClassDefsSection, null);
        for (Object ClassDefItem : data) {
            Object TypeIdItem = mReflect.reflectField(ClassDefItem.getClass().getName(), "classType", ClassDefItem);
            Object StringIdItem = mReflect.reflectField(TypeIdItem.getClass().getName(), "typeDescriptor", TypeIdItem);
            String strValue = (String) mReflect.reflectMethod(StringIdItem.getClass().getName(), "getStringValue", null, StringIdItem, null);
            if (strValue.equals(tagetclassname)) {
                LogUtil.i("found class  --- " + tagetclassname);
                return ClassDefItem;
            }
        }
        return null;
    }

    public String createNewOdex(String newDexPath, String optDirectory) {
        mReflect.reflectMethod(mTargetDexFile.getClass().getName(), "setInplace", new Class<?>[] { boolean.class }, mTargetDexFile, new Object[] { false });
        mReflect.reflectMethod(mTargetDexFile.getClass().getName(), "place", null, mTargetDexFile, null);
        int size = (Integer) mReflect.reflectMethod(mTargetDexFile.getClass().getName(), "getFileSize", null, mTargetDexFile, null);
        byte[] data = new byte[size];
        Object output = mReflect.newInstance("org.jf.dexlib.Util.ByteArrayAnnotatedOutput", new Class[] { byte[].class }, new Object[] { data });
        Class<?> AnnotatedOutput = mReflect.reflectClass("org.jf.dexlib.Util.AnnotatedOutput");
        mReflect.reflectMethod(mTargetDexFile.getClass().getName(), "writeTo", new Class[] { AnnotatedOutput }, mTargetDexFile, new Object[] { output });
        mReflect.reflectMethod(mTargetDexFile.getClass().getName(), "calcSignature", new Class[] { byte[].class }, mTargetDexFile, new Object[] { data });
        mReflect.reflectMethod(mTargetDexFile.getClass().getName(), "calcChecksum", new Class[] { byte[].class }, mTargetDexFile, new Object[] { data });

        String[] dexpaths = sTargetDexPath.split("/");
        String outFile = newDexPath + dexpaths[dexpaths.length - 1];
        try {
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
        } catch (Exception e) {
            Log.e(LogUtil.TAG, e.getMessage(), e);
        }

        return outFile;
    }

    public static void fixOdexTimeStampAndCrc(String srcODex, String targetODex) {
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
