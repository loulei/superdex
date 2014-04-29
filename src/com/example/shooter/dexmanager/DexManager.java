package com.example.shooter.dexmanager;

import java.util.List;

import android.util.Log;

import com.android.dex.ClassData;
import com.android.dex.ClassData.Method;
import com.android.dex.ClassDef;
import com.android.dex.Code;
import com.android.dex.Dex;
import com.android.dex.MethodId;
import com.android.dex.ProtoId;

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
        dumpMethods();
        dumpType();
        dumpClassDefs();
        dumpProtos();
        dumpStrings();
        ClassDef classDef = findTargetClass(classname);
        findTargetMethodIds(classDef, "loadLibrary");

        // Method m = findInitMethod(classDef);
        // Code code = mDex.readCode(m);
        // StringBuffer sb = new StringBuffer();
        // short[] insns = code.getInstructions();
        // for (short s : insns) {
        // sb.append(s);
        // }
        // Log.d(TAG, "code: " + sb.toString());

    }

    private Method findInitMethod(ClassDef classDef) {
        if (classDef == null) {
            return null;
        }
        ClassData classData = mDex.readClassData(classDef);
        Method[] ms = classData.getDirectMethods();
        List<MethodId> methodIds = mDex.methodIds();
        for (Method m : ms) {
            MethodId mid = methodIds.get(m.getMethodIndex());
            if (mDex.strings().get(mid.getNameIndex()).equals("<init>")) {
                Log.i(TAG, "find target method --- " + mid.toString());
                return m;
            }
        }
        return null;
    }

    private MethodId findTargetMethodIds(ClassDef classDef, String descriptor) {
        if (classDef == null || descriptor == null || descriptor.length() == 0) {
            return null;
        }
        ClassData classData = mDex.readClassData(classDef);
        Method[] ms = classData.getDirectMethods();
        List<MethodId> methodIds = mDex.methodIds();
        for (MethodId mid : methodIds) {
            if (mDex.strings().get(mid.getNameIndex()).equals(descriptor)) {
                Log.i(TAG, "find target method --- " + mid.toString());
                return mid;
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
