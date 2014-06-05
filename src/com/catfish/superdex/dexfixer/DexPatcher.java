package com.catfish.superdex.dexfixer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Util.ByteArrayAnnotatedOutput;

import android.util.Log;

import com.catfish.superdex.utils.LogUtil;

public class DexPatcher {

    public static void patchTargetDex(String dexFile, String tagetmethodname, String needlemethodname) {
        DexFile dex = null;
        try {
            dex = new DexFile(dexFile);
        } catch (IOException e) {
            Log.e(LogUtil.TAG, e.getMessage(), e);
            return;
        }
        EncodedMethod needleMethod = findMethod(dex, needlemethodname);
        EncodedMethod targetMethod = findMethod(dex, tagetmethodname);

        Instruction[] needleInsns = needleMethod.codeItem.getInstructions();
        Instruction[] targetInsns = targetMethod.codeItem.getInstructions();
        Instruction[] newIns = new Instruction[needleInsns.length + targetInsns.length];

        System.arraycopy(needleInsns, 0, newIns, 0, needleInsns.length);
        System.arraycopy(targetInsns, 0, newIns, needleInsns.length, targetInsns.length);
        targetMethod.codeItem.updateCode(newIns);

        int needleReg = needleMethod.codeItem.registerCount;
        int targetReg = targetMethod.codeItem.registerCount;
        targetMethod.codeItem.registerCount = needleReg > targetReg ? needleReg : targetReg;

        int needleOut = needleMethod.codeItem.outWords;
        int targetOut = targetMethod.codeItem.outWords;
        targetMethod.codeItem.outWords = needleOut > targetOut ? needleOut : targetOut;

        int needleIn = needleMethod.codeItem.inWords;
        int targetIn = targetMethod.codeItem.inWords;
        targetMethod.codeItem.inWords = needleIn > targetIn ? needleIn : targetIn;

        dex.setInplace(false);
        dex.place();

         createNewOdex(dex, dexFile);
    }

    private static EncodedMethod findMethod(DexFile dex, String methodname) {
        if (methodname == null || dex == null) {
            throw new IllegalArgumentException("methodname or dex can not be null");
        }
        List<ClassDefItem> classDefs = dex.ClassDefsSection.getItems();
        ClassDefItem targetClass = null;
        String[] strs = methodname.split("\\->");
        LogUtil.i("goto get class  --- " + strs[0]);
        for (ClassDefItem classDefItem : classDefs) {
            TypeIdItem typeIdItem = classDefItem.getClassType();
            String strValue = typeIdItem.getTypeDescriptor();
            if (strValue.equals(strs[0])) {
                LogUtil.i("found class  --- " + strs[0]);
                targetClass = classDefItem;
                break;
            }
        }
        if (targetClass != null) {
            EncodedMethod[] methods = targetClass.getClassData().getDirectMethods();
            for (EncodedMethod m : methods) {
                LogUtil.i("we have method --- " + m.method.getMethodString());
                if (m.method.getMethodString().equals(methodname)) {
                    LogUtil.i("found method --- " + methodname);
                    return m;
                }
            }
        }
        return null;
    }

    public static String createNewOdex(DexFile dex, String out) {
        byte[] data = new byte[dex.getFileSize()];

        ByteArrayAnnotatedOutput output = new ByteArrayAnnotatedOutput(data);
        dex.writeTo(output);
        DexFile.calcSignature(data);
        DexFile.calcChecksum(data);
        try {
            FileOutputStream os = new FileOutputStream(out);
            os.write(data);
            os.close();
        } catch (Exception e) {
            Log.e(LogUtil.TAG, e.getMessage(), e);
        }

        return out;
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
