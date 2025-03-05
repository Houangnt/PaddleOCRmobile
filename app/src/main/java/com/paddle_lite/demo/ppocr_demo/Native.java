package com.baidu.paddle.lite.demo.ppocr_demo;

import android.content.Context;
import android.util.Log;

import com.paddle_lite.demo.common.SDKExceptions;
import com.paddle_lite.demo.common.Utils;

public class Native {
    static {
        System.loadLibrary("Native");
    }

    private long ctx = 0;
    private boolean run_status = false;

    public boolean init(Context mContext,
                        String detModelPath,
                        String clsModelPath,
                        String recModelPath,
                        String configPath,
                        String labelPath,
                        int cputThreadNum,
                        String cpuPowerMode) {
        ctx = nativeInit(
                detModelPath,
                clsModelPath,
                recModelPath,
                configPath,
                labelPath,
                cputThreadNum,
                cpuPowerMode);
        return ctx == 0;
    }


    public boolean release() {
        if (ctx == 0) {
            return false;
        }
        return nativeRelease(ctx);
    }

    public boolean process(String imagePath) {
        if (ctx == 0) {
            Log.d("MainActivity", imagePath);
            return false;
        }
        Log.d("MainActivity", "native process");
        return nativeProcessImg(ctx, imagePath);
    }
    public static native long nativeInit(String detModelPath,
                                         String clsModelPath,
                                         String recModelPath,
                                         String configPath,
                                         String labelPath,
                                         int cputThreadNum,
                                         String cpuPowerMode);

    public static native boolean nativeRelease(long ctx);
    public static native boolean nativeProcess(long ctx, int inTextureId, int outTextureId, int textureWidth, int textureHeight, String savedImagePath);


    public static native boolean nativeProcessImg(long ctx, String imagePath);
}
