package com.qimeng.huishou.gpiotest;

import android.os.SystemClock;
import android.serialport.SerialPort;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class Code {
    private String div;
    private SerialPort port;
    private InputStream input;
    private OutputStream output;

    private Buff buff = new Buff();

    private boolean isRead = true;
    private DataListener listener;

    public interface DataListener {
        void onData(String string);
    }

    public void close() {
        port.close();
        isRead = false;
    }

    public Code(String div, DataListener listener) {
        this.div = div;
        this.listener = listener;
        try {
            port = new SerialPort(new File(div), 9600, 0);
            port.close();
            openDiv();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDiv() {
        try {
            port = new SerialPort(new File(div), 9600, 0);
            input = port.getInputStream();
            output = port.getOutputStream();
            if (input == null) {
                Log.e("Code", "连接失败");
            }
            startRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startRead() {
        new Thread(() -> {
            while (isRead) {
                byte[] received = new byte[1024];
                int size;
                while (isRead) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    try {

                        int available = input.available();

                        if (available > 0) {
                            size = input.read(received);
                            if (size > 0) {
                                buff.append(Arrays.copyOf(received, size));
                                byte[] code = buff.getCode();
                                if (code != null) {
                                    this.listener.onData(new String(code));
                                }
                            }
                        } else {
                            // 暂停一点时间，免得一直循环造成CPU占用率过高
                            SystemClock.sleep(1);
                        }
                    } catch (IOException e) {
                        Log.i("Read", "读取数据失败" + e.getMessage());
                    }
                }
            }
        }).start();
    }

}