package com.example.shooter.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

public class ReflectImpl {
    ClassLoader mClassLoader = null;

    public ReflectImpl(ClassLoader cl) {
        if (cl == null) {
            throw new IllegalArgumentException("class loader can not be null");
        }
        mClassLoader = cl;
    }

    public Class<?> reflectClass(String classname) {
        try {
            return Class.forName(classname, true, mClassLoader);
        } catch (ClassNotFoundException e) {
            LogUtil.d(e.getMessage(), e);
        }
        return null;
    }

    public Object reflectField(String classname, String fieldname, Object receiver) {
        Class<?> clz = reflectClass(classname);
        if (clz == null) {
            Log.e(LogUtil.TAG, "did not find class");
            return null;
        }
        try {
            Field f = clz.getDeclaredField(fieldname);
            f.setAccessible(true);
            return f.get(receiver);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object reflectMethod(String classname, String methodname, Class<?>[] types, Object receiver, Object[] paramethers) {
        Class<?> clz = reflectClass(classname);
        if (clz == null) {
            Log.e(LogUtil.TAG, "did not find class");
            return null;
        }
        try {
            Method m = clz.getDeclaredMethod(methodname, types);
            m.setAccessible(true);
            return m.invoke(receiver, paramethers);
        } catch (NoSuchMethodException e) {
            LogUtil.d(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LogUtil.d(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LogUtil.d(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            LogUtil.d(e.getMessage(), e);
        }
        return null;
    }

    public Object newInstance(String classname, Class<?>[] types, Object[] paramethers) {
        Class<?> clz = reflectClass(classname);
        if (clz == null) {
            Log.e(LogUtil.TAG, "did not find class");
            return null;
        }
        if (types == null || types.length == 0) {
            try {
                return clz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        try {
            Constructor<?> c = clz.getConstructor(types);
            c.setAccessible(true);
            return c.newInstance(paramethers);
        } catch (NoSuchMethodException e) {
            LogUtil.d(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LogUtil.d(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            LogUtil.d(e.getMessage(), e);
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }
}
