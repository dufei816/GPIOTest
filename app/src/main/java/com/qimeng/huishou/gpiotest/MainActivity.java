package com.qimeng.huishou.gpiotest;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("CheckResult")
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_test)
    Button btnTest;

    @BindView(R.id.tv_text)
    TextView tvText;

    private static final String USB = "USB_PERMISSION";
    private Entity entity;
    private boolean isRead = false;
    private boolean putData = true;

    private Gson gson = new Gson();

    private LinkedBlockingQueue<Entity> putEntitys = new LinkedBlockingQueue<>(30);

    private int weight = 0;


    Thread putTask = new Thread(() -> {
        while (putData) {
            try {
                Entity entity = putEntitys.take();
                String string = gson.toJson(entity);
                FileUtil.write(string);
                runOnUiThread(() -> tvText.append("\n" + string));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });


    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(USB)) {
                UsbDevice mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    myUsb = new USB(mUsbDevice, manager, myCodeListener);
                } else {
                    Log.e("USB", "获取权限失败");
                }
            }

        }
    };

    private com.qimeng.huishou.gpiotest.USB.CodeListener myCodeListener = new USB.CodeListener() {
        @Override
        public void onError() {
            Log.e("USB", "连接失败");

        }

        @Override
        public void onSuccess() {
            Log.e("USB", "连接成功");
            putTask.start();
        }

        @Override
        public void onCode(String code) {
            if(entity == null) return;
            entity.setWeight(Integer.valueOf(code));
        }
    };

    private UsbManager manager;
    private USB myUsb;
    private Code code;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
        code.close();
        myUsb.close();
        isRead = false;
        putData = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        weight = 0;
        IntentFilter filter = new IntentFilter();
        filter.addAction(USB);
        registerReceiver(myReceiver, filter);

        code = new Code("/dev/ttyS1", string -> {
            if (string.equals("Admin")) {
                try {
                    if(entity == null) return;
                    putEntitys.put(new Entity(entity.getCode(), entity.getWeight() - weight));
                    weight += entity.getWeight();
                    isRead = false;
                    entity = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                readCode(string);
            }
        });

        btnTest.setOnClickListener(view -> {
            if (myUsb != null) {
                myUsb.sendData("Weight");
            }
        });
        manager = (UsbManager) getSystemService(USB_SERVICE);
        new Thread(()->initArduino()).start();
    }

    private synchronized void readCode(String string) {
        if (entity != null) return;
        Observable.just(string)
                .subscribeOn(Schedulers.newThread())
                .filter(str -> str.contains("code="))
                .map(str -> str.substring(str.indexOf("code="), str.indexOf("&card")).split("=")[1])
                .subscribe(str -> {
                    isRead = true;
                    entity = new Entity(str, -1);
                    while (isRead) {
                        myUsb.sendData("Weight");
                        Thread.sleep(500);
                    }
                });
    }

    private void initArduino() {
        HashMap<String, UsbDevice> map = manager.getDeviceList();
        ArrayList<UsbDevice> list = new ArrayList<>(map.values());
        if (list.size() > 0) {
            boolean init = false;
            for (UsbDevice device : list) {
                if (67 == device.getProductId()) {
                    init = true;
                    if (!manager.hasPermission(device)) {
                        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
                                0, new Intent(USB), 0);
                        manager.requestPermission(device, mPermissionIntent);
                    } else {
                        myUsb = new USB(device, manager, myCodeListener);
                    }
                }
            }
            if (!init) {
                try {
                    Thread.sleep(3000);
                    initArduino();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
