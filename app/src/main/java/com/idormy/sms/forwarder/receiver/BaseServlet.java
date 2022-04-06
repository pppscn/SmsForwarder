package com.idormy.sms.forwarder.receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.util.IOUtils;
import com.idormy.sms.forwarder.model.LogModel;
import com.idormy.sms.forwarder.model.vo.ResVo;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.utils.CloneUtils;
import com.idormy.sms.forwarder.utils.HttpUtils;
import com.idormy.sms.forwarder.utils.LogUtils;
import com.idormy.sms.forwarder.utils.SettingUtils;
import com.idormy.sms.forwarder.utils.SimUtils;
import com.idormy.sms.forwarder.utils.SmsUtils;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    private static final String TAG = "BaseServlet";
    public static final int BUFFER_SIZE = 1 << 12;
    private static final long serialVersionUID = 1L;
    public static final long RULE_ID = -999L;
    public static final String SMSHUB_PATH = "/send_api";
    public static final String CLONE_PATH = "/clone";

    private final String path;
    @SuppressLint("StaticFieldLeak")
    private final Context context;

    public BaseServlet(String path, Context context) {
        this.path = path;
        this.context = context;
        SettingUtils.init(context);
    }

    public Context getContext() {
        return context;
    }

    public String getPath() {
        return path;
    }

    public static void addServlet(Server jettyServer, Context context) {
        ServletContextHandler contextHandler = new ServletContextHandler();
        addHolder(contextHandler, new BaseServlet(BaseServlet.CLONE_PATH, context));
        addHolder(contextHandler, new BaseServlet(BaseServlet.SMSHUB_PATH, context));
        // addholder(contextHandler, new BaseServlet("/", context));
        jettyServer.setHandler(contextHandler);
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

    //发送短信api
    private void send_api(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        PrintWriter writer = resp.getWriter();
        BufferedReader reader = req.getReader();
        try {
            String read = read(reader);
            Log.i(TAG, "Request message:" + read);
            ResVo<List<SmsHubVo>> result = ResVo.suessces(null);
            if (StringUtil.isNotBlank(read)) {
                ResVo<List<SmsHubVo>> obj = JSON.parseObject(read, new TypeReference<ResVo<List<SmsHubVo>>>() {
                }.getType());
                List<SmsHubVo> data = obj.getData();
                for (SmsHubVo vo : data) {
                    sendSms(TAG, vo);
                }
                result.setData(data);
            }
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

    //一键克隆——查询接口
    private void clone_api(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        PrintWriter writer = resp.getWriter();
        BufferedReader reader = req.getReader();

        try {
            resp.setContentType("application/json;charset=utf-8");
            String json = CloneUtils.exportSettings();
            writer.println(json);
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
        resp.addHeader("Content-Disposition", "attachment;filename=" + "SmsForwarder.json");
        ServletOutputStream outputStream = resp.getOutputStream();
        try {
            String json = CloneUtils.exportSettings();
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            String text = "Internal server error: " + e.getMessage();
            Log.e(TAG, text);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            IOUtils.close(outputStream);
        }
    }

    //短信列表api

    //联系人api

    //查询通话记录api

    //查询电量api

    //规则列表api（增删改查）

    //发送通道api（增删改查）

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String read(Reader reader) throws IOException {
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

    private void printErrMsg(HttpServletResponse resp, PrintWriter writer, Exception e) {
        String text = "Internal server error: " + e.getMessage();
        Log.e(TAG, text);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=utf-8");
        writer.println(JSON.toJSONString(ResVo.error(text)));
    }

    private static void sendSms(String tag, SmsHubVo vo) {
        boolean failure = true;
        String msg = "";
        Long logId = null;
        try {
            if (SmsHubVo.Action.send.code().equals(vo.getAction())) {
                vo.setType(SmsHubVo.Type.sms.code());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    int subscriptionIdBySimId = SimUtils.getSubscriptionIdBySimId(Integer.parseInt(vo.getChannel()) - 1);
                    msg = SmsUtils.sendSms(subscriptionIdBySimId, vo.getTarget(), vo.getContent());
                    String simInfo = "SIM" + (subscriptionIdBySimId + 1);
                    vo.setChannel(simInfo);
                    logId = LogUtils.addLog(new LogModel(vo.getType(), vo.getTarget(), vo.getContent(), simInfo, RULE_ID));
                    if (msg == null) {
                        failure = false;
                        HttpUtils.Toast(tag, "短信发送成功");
                        Log.i(tag, "短信发送成功");
                        vo.setAction(SmsHubVo.Action.suessces.code());
                        LogUtils.updateLog(logId, 2, SmsHubVo.Action.suessces.code());
                    }
                } else {
                    msg = "api<22";
                }
            }
        } catch (Exception e) {
            msg += e.getMessage();
            e.printStackTrace();
        }
        if (failure) {
            msg = "短信发送失败:" + msg;
            HttpUtils.Toast(tag, msg);
            Log.i(tag, msg);
            vo.setAction(SmsHubVo.Action.failure.code());
            vo.setErrMsg(msg);
            if (logId != null) {
                LogUtils.updateLog(logId, 0, msg);
            }
        }
    }
}
