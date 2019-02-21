package com.qimeng.huishou.gpiotest;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private static final String FILE_PATH = "/mnt/sdcard/jifen/";
    private static String str = format.format(new Date());

    public static void write(String string) throws IOException {
        String name = str + ".txt";
        File file = new File(FILE_PATH);
        // 首先判断文件夹是否存在
        if (!file.exists()) {
            if (!file.mkdirs()) {   // 文件夹不存在则创建文件
                Log.e("FileUtil", "文件夹创建失败");
            }
        } else {
            File fileWrite = new File(FILE_PATH + File.separator + name);
            // 首先判断文件是否存在
            if (!fileWrite.exists()) {
                if (!fileWrite.createNewFile()) {   // 文件不存在则创建文件
                    Log.e("FileUtil", "文件创建失败");
                    return;
                }
            }
            FileWriter fw = new FileWriter(fileWrite, true);
            PrintWriter pw = new PrintWriter(fw);
            pw.println(string);
            pw.flush();
            fw.flush();
            pw.close();
            fw.close();
            Log.e("FileUtil", "输入完成--》" + FILE_PATH + File.separator + name);
        }

    }

}