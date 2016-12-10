package com.smallnew.pullfling.lib;

/**
 * Created by MRWANG on 2016/12/10.
 */

public class Log {
    public static final boolean isTest = true;
    public static void e(String tag,String msg){
        if(isTest)
            android.util.Log.e(tag,msg);
    }

    public static void i(String tag,String msg){
        if(isTest)
            android.util.Log.i(tag,msg);
    }

    public static void d(String tag,String msg){
        if(isTest)
            android.util.Log.d(tag,msg);
    }

    public static void w(String tag,String msg){
        if(isTest)
            android.util.Log.w(tag,msg);
    }
}
