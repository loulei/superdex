package com.example.shooter.dexmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.android.dex.ClassData;
import com.android.dex.ClassData.Method;
import com.android.dex.ClassDef;
import com.android.dex.Dex;
import com.android.dex.MethodId;
import com.android.dex.ProtoId;
import com.example.shooter.utils.LogUtil;
import com.example.shooter.utils.ReflectImpl;

import dalvik.system.DexClassLoader;

public class DexManager {
    private static final String TAG = "catfish";
    Dex mDex = null;
    private ReflectImpl mReflect = null;

    public DexManager(Dex dex) {
        if (dex == null) {
            throw new IllegalArgumentException("dex input can not be null");
        }
        mDex = dex;
    }

    public Dex getDex() {
        return mDex;
    }

    public void prepare(Context context) {
        transferFiles(context, "bytecode.dex");
        ClassLoader cl = new DexClassLoader("/data/data/com.catfish.shooter/files/bytecode.dex", "/data/data/com.catfish.shooter/cache", null, context.getClassLoader());
        mReflect = new ReflectImpl(cl);
    }

    public void hackDex(String classname, String methoddesc) {
        ClassDef classDef = findTargetClass(classname);
        Method m = findTargetVirtualMethod(classDef, methoddesc);
        int methodindex = m.getMethodIndex();
        int classindex = 0;
        Iterable<ClassDef> clfs = mDex.classDefs();
        for (ClassDef c : clfs) {
            Log.d(TAG, "ClassDef table --- " + c.toString());
        }

        Object dex = mReflect.newInstance("org.jf.dexlib.DexFile", new Class[] { byte[].class }, new Object[] { mDex.getBytes() });
        Object classdefs = mReflect.reflectField("org.jf.dexlib.DexFile", "ClassDefsSection", dex);
        Object classdef = mReflect.reflectMethod("org.jf.dexlib.IndexSection", "getItemByIndex", new Class[] { int.class }, classdefs, new Object[] { classindex });
        Object classdata = mReflect.reflectMethod("org.jf.dexlib.ClassDefItem", "getClassData", (Class[]) null, classdef, null);
        Object[] methods = (Object[]) mReflect.reflectMethod("org.jf.dexlib.ClassDataItem", "getDirectMethods", (Class[]) null, classdata, null);
        Object codeItem = mReflect.reflectField("org.jf.dexlib.ClassDataItem$EncodedMethod", "codeItem", methods[methodindex]);
    }

    private Method findTargetVirtualMethod(ClassDef classDef, String descriptor) {
        if (classDef == null || descriptor == null || descriptor.length() == 0) {
            return null;
        }
        ClassData classData = mDex.readClassData(classDef);
        Method[] ms = classData.getVirtualMethods();
        for (Method m : ms) {
            if (mDex.methodIds().get(m.getMethodIndex()).toString().equals(descriptor)) {
                Log.i(TAG, "find target method --- " + descriptor);
                return m;
            }
        }
        return null;
    }

    private ClassDef findTargetClass(String classname) {
        Iterable<ClassDef> classdefs = mDex.classDefs();
        for (ClassDef c : classdefs) {
            if (mDex.typeNames().get(c.getTypeIndex()).equals(classname)) {
                Log.i(TAG, "find target class --- " + c.toString());
                return c;
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

    private void dumpStrings() {
        List<String> strings = mDex.strings();
        for (String s : strings) {
            Log.d(TAG, "string table --- " + s);
        }
    }

    private void dumpProtos() {
        List<ProtoId> protos = mDex.protoIds();
        for (ProtoId c : protos) {
            Log.d(TAG, "Proto table --- " + c.toString());
        }
    }

    private void dumpType() {
        List<String> types = mDex.typeNames();
        for (String c : types) {
            Log.d(TAG, "Type table --- " + c);
        }
    }

    private void dumpClassDefs() {
        Iterable<ClassDef> classdefs = mDex.classDefs();
        for (ClassDef c : classdefs) {
            Log.d(TAG, "ClassDef table --- " + c.toString());
        }
    }

    private void dumpMethods() {
        List<MethodId> methodIds = mDex.methodIds();
        for (MethodId m : methodIds) {
            Log.d(TAG, "MethodId table --- " + m.toString());
        }
    }
}
