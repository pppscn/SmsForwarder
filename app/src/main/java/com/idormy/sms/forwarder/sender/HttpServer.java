package com.idormy.sms.forwarder.sender;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.hjq.toast.ToastUtils;
import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.receiver.BaseServlet;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.NetUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SmsHubActionHandler;

import org.eclipse.jetty.server.Server;


public class HttpServer {
    private static Boolean hasInit = false;
    private static Server jettyServer;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static long ts = 0L;


    @SuppressLint("HandlerLeak")
    public static void init(Context context) {
        //noinspection SynchronizeOnNonFinalField
        synchronized (hasInit) {
            if (hasInit) return;

            hasInit = true;
            HttpServer.context = context;
            SmsHubActionHandler.init(context);
            jettyServer = new Server(Define.HTTP_SERVER_PORT);
            BaseServlet.addServlet(jettyServer, context);
        }
    }

    /**
     * Checks if the Jetty is running
     * boolean - true when server is running/starting/stopping, false otherwise
     */
    public synchronized static Boolean asRunning() {
        if (jettyServer != null) {
            return jettyServer.isRunning() && !jettyServer.isStopping();
        }
        return false;
    }

    public synchronized static boolean update() {
        //非WiFi网络下不可启用
        if (NetUtil.NETWORK_WIFI != NetUtil.getNetWorkStatus()) {
            ToastUtils.show(R.string.no_wifi_network);
            if (asRunning()) stop();
            return false;
        }
        long l = System.currentTimeMillis();
        if (l - ts < 3000 && asRunning()) {
            ToastUtils.show(R.string.tips_wait_3_seconds);
            return false;
        }
        if (asRunning().equals(SettingUtil.getSwitchEnableHttpServer())) {
            return false;
        }
        if (SettingUtil.getSwitchEnableHttpServer()) {
            SmsHubVo.getDevInfoMap(true);
            start();
            ts = System.currentTimeMillis();
            ToastUtils.show(R.string.server_has_started);
        } else {
            stop();
            ToastUtils.show(R.string.server_has_stopped);
        }
        return true;
    }

    /**
     * Checks if Jetty is stopping
     * boolean - True when server is stopping
     */
    private synchronized static Boolean asStopp() {
        if (jettyServer != null) {
            return !(jettyServer.isRunning() || jettyServer.isStopping());
        } else {
            return true;
        }
    }

    private static void start() {
        stop();
        Log.i("HttpServer", "start");
        //new Thread(() -> {
        try {
            //Start Jetty
            jettyServer.start();
            //jettyServer.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //}).start();
    }

    private static void stop() {
        if (Boolean.FALSE.equals(asStopp())) {
            try {
                if (jettyServer != null) {
                    jettyServer.stop();
                    //jettyServer = new Server(port);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
