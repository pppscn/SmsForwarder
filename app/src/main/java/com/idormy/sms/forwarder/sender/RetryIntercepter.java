package com.idormy.sms.forwarder.sender;

import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.utils.LogUtils;

import java.io.IOException;
import java.io.InterruptedIOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryIntercepter implements Interceptor {
    static final String TAG = "RetryIntercepter";
    private final long retryInterval;//重试的间隔
    private final long logId;//更新记录ID
    public final int executionCount;//最大重试次数

    RetryIntercepter(Builder builder) {
        this.executionCount = builder.executionCount;
        this.retryInterval = builder.retryInterval;
        this.logId = builder.logId;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        int retryTimes = 0;
        Request request = chain.request();
        Response response;
        do {
            if (retryTimes > 0 && getRetryInterval() > 0) {
                final long delayTime = retryTimes * getRetryInterval();
                try {
                    Log.w(TAG, "第 " + retryTimes + " 次重试，休眠 " + delayTime + " 秒");
                    //noinspection BusyWait
                    Thread.sleep(delayTime * 1000);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException(e.getMessage());
                }
            }

            response = doRequest(chain, request, retryTimes);
            retryTimes++;
        } while ((response == null || !response.isSuccessful()) && retryTimes <= executionCount);

        if (response == null) throw new InterruptedIOException("服务端无应答，结束重试");
        return response;
    }

    private Response doRequest(Chain chain, Request request, int retryTimes) {
        Response response = null;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            String resp = retryTimes > 0 ? "第" + retryTimes + "次重试：" + e.getMessage() : e.getMessage();
            LogUtils.updateLog(logId, 1, resp);
            Log.w(TAG, resp);
        }
        return response;
    }

    /**
     * retry间隔时间
     */
    public long getRetryInterval() {
        return this.retryInterval;
    }

    public static final class Builder {
        private int executionCount;
        private long retryInterval;
        private long logId;

        public Builder() {
            executionCount = 3;
            retryInterval = 1000;
            logId = 0;
        }

        public RetryIntercepter.Builder executionCount(int executionCount) {
            this.executionCount = executionCount;
            return this;
        }

        public RetryIntercepter.Builder retryInterval(long retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        public RetryIntercepter.Builder logId(long logId) {
            this.logId = logId;
            return this;
        }

        public RetryIntercepter build() {
            return new RetryIntercepter(this);
        }
    }

}