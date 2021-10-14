package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.idormy.sms.forwarder.R;

public class NetUtil {
    //没有网络
    public static final int NETWORK_NONE = 0;
    //移动网络
    public static final int NETWORK_MOBILE = 1;
    //无线网络
    public static final int NETWORK_WIFI = 2;

    static Boolean hasInit = false;
    @SuppressLint("StaticFieldLeak")
    static Context context;


    public static void init(Context context1) {
        //noinspection SynchronizeOnNonFinalField
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
        }
    }

    //获取网络启动
    public static int getNetWorkStatus() {
        //连接服务 CONNECTIVITY_SERVICE
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //网络信息 NetworkInfo
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            //判断是否是wifi
            if (activeNetworkInfo.getType() == (ConnectivityManager.TYPE_WIFI)) {
                //返回无线网络
                Toast.makeText(context, R.string.on_wireless_network, Toast.LENGTH_SHORT).show();
                return NETWORK_WIFI;
                //判断是否移动网络
            } else if (activeNetworkInfo.getType() == (ConnectivityManager.TYPE_MOBILE)) {
                Toast.makeText(context, R.string.on_mobile_network, Toast.LENGTH_SHORT).show();
                //返回移动网络
                return NETWORK_MOBILE;
            }
        } else {
            //没有网络
            Toast.makeText(context, R.string.no_network, Toast.LENGTH_SHORT).show();
            return NETWORK_NONE;
        }
        //默认返回  没有网络
        return NETWORK_NONE;
    }

    public static String getLocalIp(Context context) {
        if (NETWORK_WIFI != getNetWorkStatus()) return context.getString(R.string.not_connected_wifi);

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        if (ipAddress == 0) return context.getString(R.string.failed_to_get_ip);
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
                + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
    }
}
