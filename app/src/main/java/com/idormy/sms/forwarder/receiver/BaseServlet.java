package com.idormy.sms.forwarder.receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.IOUtils;
import com.idormy.sms.forwarder.CloneActivity;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.utils.SmsHubActionHandler;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.*;
import java.util.List;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet
@MultipartConfig
public class BaseServlet extends HttpServlet {

    public static final int BUFFER_SIZE = 1 << 12;
    public static final String CLONE_PATH = "/";
    public static final String SMSHUB_PATH = "/send_api";
    private static final long serialVersionUID = 1L;
    private static final String TAG = "BaseServlet";
    private static final SmsHubActionHandler.SmsHubMode smsHubMode = SmsHubActionHandler.SmsHubMode.server;

    public BaseServlet(String path, Context context) {
        this.path = path;
        this.context = context;
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
        if (CLONE_PATH.equals(path)) {
            clone(req, resp);
        } else if (SMSHUB_PATH.equals(path)) {
            send_api(req, resp);
        } else {
            notFound(req, resp);
        }
    }

    private void notFound(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        try {
            String text = "NOT FOUND";
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writer.println(text);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(writer);
        }
    }

    private void send_api(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        PrintWriter writer = resp.getWriter();
        BufferedReader reader = req.getReader();
        try {
            String read = read(reader);
            Log.i(TAG, "请求内容:" + read);
            List<SmsHubVo> smsHubVos = JSON.parseArray(read, SmsHubVo.class);
            if (smsHubVos.size() == 1 && SmsHubVo.Action.heartbeat.code().equals(smsHubVos.get(0).getAction())) {
                smsHubVos.clear();
                SmsHubVo smsHubVo = SmsHubVo.heartbeatInstance();
                smsHubVos.add(smsHubVo);
                List<SmsHubVo> data = SmsHubActionHandler.getData(smsHubMode);
                if (data != null && data.size() > 0) {
                    smsHubVo.setChildren(data);
                }
            } else {
                for (SmsHubVo vo : smsHubVos) {
                    SmsHubActionHandler.handle(TAG, vo);
                }
                List<SmsHubVo> data = SmsHubActionHandler.getData(smsHubMode);
                if (data != null && data.size() > 0) {
                    SmsHubVo smsHubVo = SmsHubVo.heartbeatInstance();
                    smsHubVo.setChildren(data);
                    smsHubVos.add(smsHubVo);
                }
            }
            resp.setContentType("application/json;charset=utf-8");
            String text = JSON.toJSONString(smsHubVos);
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
        String text = "服务器内部错误:" + e.getMessage();
        Log.e(TAG, text);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        writer.println(text);
    }

    private void clone(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        File file = context.getDatabasePath(CloneActivity.DATABASE_NAME);
        resp.addHeader("Content-Disposition", "attachment;filename=" + CloneActivity.DATABASE_NAME);
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
            String text = "服务器内部错误:" + e.getMessage();
            Log.e(TAG, text);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            IOUtils.close(inputStream);
            IOUtils.close(outputStream);
        }
    }

}
