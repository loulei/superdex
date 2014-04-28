package com.example.shooter.dexmanager;

import java.util.List;

import android.util.Log;

import com.android.dex.ClassData;
import com.android.dex.ClassData.Method;
import com.android.dex.ClassDef;
import com.android.dex.Code;
import com.android.dex.Dex;
import com.android.dex.MethodId;

public class DexManager {
    private static final String TAG = "catfish";
    Dex mDex = null;

    public DexManager(Dex dex) {
        if (dex == null) {
            throw new IllegalArgumentException("dex input can not be null");
        }
        mDex = dex;
    }

    public Dex getDex() {
        return mDex;
    }

    public void hackDex(String classname) {
//        dumpMethods();
        ClassDef classDef = findTargetClass(classname);
        Method m = findInitMethod(classDef, methodname);
//        Code code = mDex.readCode(m);
//        StringBuffer sb = new StringBuffer();
//        short[] insns = code.getInstructions();
//        for (short s : insns) {
//            sb.append(s);
//        }
//        Log.d(TAG, "code: " + sb.toString());
    }

    private Method findInitMethod(ClassDef classDef, String methodname) {
        if (classDef == null) {
            return null;
        }
        ClassData classData = mDex.readClassData(classDef);
        Method[] ms = classData.getDirectMethods();
        List<MethodId> methodIds = mDex.methodIds();
        for (Method m : ms) {
            MethodId mid = methodIds.get(m.getMethodIndex());
            Log.d(TAG, "method list --- " + mDex.strings().get(mid.getNameIndex()));
            if (mDex.strings().get(mid.getNameIndex()).equals(methodname)) {
                Log.i(TAG, "find target method --- " + mid.toString());
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

    private void dumpStrings() {
        List<String> strings = mDex.strings();
        for (String s : strings) {
            Log.d(TAG, "string table --- " + s);
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
