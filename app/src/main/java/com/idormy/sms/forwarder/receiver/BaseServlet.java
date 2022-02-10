package com.idormy.sms.forwarder.receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.util.IOUtils;
import com.idormy.sms.forwarder.model.vo.ResVo;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.utils.BackupDbTask;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SmsHubActionHandler;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet
@MultipartConfig
public class BaseServlet extends HttpServlet {

    public static final int BUFFER_SIZE = 1 << 12;
    public static final String CLONE_PATH = "/clone";
    public static final String SMSHUB_PATH = "/send_api";
    private static final long serialVersionUID = 1L;
    private static final String TAG = "BaseServlet";
    public static final SmsHubActionHandler.SmsHubMode smsHubMode = SmsHubActionHandler.SmsHubMode.server;

    public BaseServlet(String path, Context context) {
        this.path = path;
        this.context = context;
        SettingUtil.init(context);
    }

    private final String path;
    @SuppressLint("StaticFieldLeak")
    private final Context context;

    public static void addServlet(Server jettyServer, Context context) {
        ServletContextHandler contextHandler = new ServletContextHandler();
        addHolder(contextHandler, new BaseServlet(BaseServlet.CLONE_PATH, context));
        addHolder(contextHandler, new BaseServlet(BaseServlet.SMSHUB_PATH, context));
        // addholder(contextHandler, new BaseServlet("/", context));
        jettyServer.setHandler(contextHandler);
    }

    public static String read(Reader reader) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];
        int size;
        StringBuilder sb = new StringBuilder();
        while ((size = reader.read(buffer)) != -1) {
            char[] chars = new char[size];
            System.arraycopy(buffer, 0, chars, 0, size);
            sb.append(chars);
        }
        return sb.toString();
    }

    private static void addHolder(ServletContextHandler servletContextHandler, BaseServlet baseServlet) {
        ServletHolder servletHolder = new ServletHolder(baseServlet);
        servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(baseServlet.getContext().getCacheDir().getAbsolutePath() + File.pathSeparator + "jettyServer"));
        servletContextHandler.addServlet(servletHolder, baseServlet.getPath());
    }

    public Context getContext() {
        return context;
    }

    public String getPath() {
        return path;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String msg = "HTTP method POST is not supported by this URL";
        if (CLONE_PATH.equals(path)) {
            clone_api(req, resp);
        } else if (SMSHUB_PATH.equals(path)) {
            send_api(req, resp);
        } else if ("1.1".endsWith(req.getProtocol())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String msg = "HTTP method GET is not supported by this URL";
        if (CLONE_PATH.equals(path)) {
            clone(req, resp);
        } else if ("1.1".endsWith(req.getProtocol())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }


    private void send_api(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        PrintWriter writer = resp.getWriter();
        BufferedReader reader = req.getReader();
        try {
            String read = read(reader);
            Log.i(TAG, "Request message:" + read);
            SmsHubVo smsHubVo = SmsHubVo.heartbeatInstance(SmsHubActionHandler.getData(smsHubMode));
            ResVo<List<SmsHubVo>> result = ResVo.suessces(null);
            if (StringUtil.isNotBlank(read)) {
                ResVo<List<SmsHubVo>> obj = JSON.parseObject(read, new TypeReference<ResVo<List<SmsHubVo>>>() {
                }.getType());
                List<SmsHubVo> data = obj.getData();
                for (SmsHubVo vo : data) {
                    SmsHubActionHandler.handle(TAG, vo);
                }
                result.setData(data);
            }
            result.setHeartbeat(smsHubVo);
            resp.setContentType("application/json;charset=utf-8");
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (SmsHubVo datum : result.getData()) {
                if (SmsHubVo.Action.failure.code().equals(datum.getAction())) {
                    sb.append(",").append("第").append(i++).append("条处理失败:").append(datum.getErrMsg());
                }
                i++;
            }
            if (sb.length() > 0) {
                result.setError(sb.substring(1));
            }
            String text = JSON.toJSONString(result);
            writer.println(text);
        } catch (Exception e) {
            e.printStackTrace();
            printErrMsg(resp, writer, e);
        } finally {
            IOUtils.close(reader);
            IOUtils.close(writer);
        }
    }

    private void printErrMsg(HttpServletResponse resp, PrintWriter writer, Exception e) {
        String text = "Internal server error: " + e.getMessage();
        Log.e(TAG, text);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=utf-8");
        writer.println(JSON.toJSONString(ResVo.error(text)));
    }

    //一键克隆——查询接口
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void clone_api(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        PrintWriter writer = resp.getWriter();
        BufferedReader reader = req.getReader();
        try {
            //备份文件
            BackupDbTask task = new BackupDbTask(context);
            String backup_version = task.doInBackground(BackupDbTask.COMMAND_BACKUP);
            Log.d(TAG, "backup_version = " + backup_version);

            Map msgMap = new HashMap();
            msgMap.put("versionCode", SettingUtil.getVersionCode());
            msgMap.put("versionName", SettingUtil.getVersionName());
            msgMap.put("enableSms", SettingUtil.getSwitchEnableSms());
            msgMap.put("enablePhone", SettingUtil.getSwitchEnablePhone());
            msgMap.put("callType1", SettingUtil.getSwitchCallType1());
            msgMap.put("callType2", SettingUtil.getSwitchCallType2());
            msgMap.put("callType3", SettingUtil.getSwitchCallType3());
            msgMap.put("enableAppNotify", SettingUtil.getSwitchEnableAppNotify());
            msgMap.put("cancelAppNotify", SettingUtil.getSwitchCancelAppNotify());
            msgMap.put("smsHubApiUrl", SettingUtil.getSmsHubApiUrl());
            msgMap.put("batteryLevelAlarmMin", SettingUtil.getBatteryLevelAlarmMin());
            msgMap.put("batteryLevelAlarmMax", SettingUtil.getBatteryLevelAlarmMax());
            msgMap.put("batteryLevelAlarmOnce", SettingUtil.getBatteryLevelAlarmOnce());
            msgMap.put("retryTimes", SettingUtil.getRetryTimes());
            msgMap.put("delayTime", SettingUtil.getDelayTime());
            msgMap.put("enableSmsTemplate", SettingUtil.getSwitchSmsTemplate());
            msgMap.put("smsTemplate", SettingUtil.getSmsTemplate());
            msgMap.put("backupVersion", backup_version);

            resp.setContentType("application/json;charset=utf-8");
            String text = JSON.toJSONString(msgMap);
            writer.println(text);
        } catch (Exception e) {
            e.printStackTrace();
            printErrMsg(resp, writer, e);
        } finally {
            IOUtils.close(reader);
            IOUtils.close(writer);
        }
    }

    //一键克隆——下载接口
    private void clone(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        File file = new File(context.getCacheDir().getPath() + File.separator + BackupDbTask.BACKUP_FILE);
        resp.addHeader("Content-Disposition", "attachment;filename=" + BackupDbTask.BACKUP_FILE);
        ServletOutputStream outputStream = resp.getOutputStream();
        InputStream inputStream = new FileInputStream(file);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int size;
            while ((size = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, size);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String text = "Internal server error: " + e.getMessage();
            Log.e(TAG, text);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            IOUtils.close(inputStream);
            IOUtils.close(outputStream);
        }
    }

}
