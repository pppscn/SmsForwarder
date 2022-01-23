package com.idormy.sms.forwarder.sender;

import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.utils.LogUtil;

import java.io.IOException;
import java.io.InterruptedIOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryIntercepter implements Interceptor {
    static final String TAG = "RetryIntercepter";
    public int executionCount;//最大重试次数
    private final long retryInterval;//重试的间隔
    private final long logId;//更新记录ID

    RetryIntercepter(Builder builder) {
        this.executionCount = builder.executionCount;
        this.retryInterval = builder.retryInterval;
        this.logId = builder.logId;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = doRequest(chain, request);
        int retryNum = 0;
        while ((response == null || !response.isSuccessful()) && retryNum <= executionCount) {
            Log.w(TAG, "第 " + retryNum + " 次请求");
            if (retryNum > 0) {
                final long nextInterval = retryNum * getRetryInterval();
                try {
                    Log.w(TAG, "等待 " + nextInterval + " 秒后重试！");
                    //noinspection BusyWait
                    Thread.sleep(nextInterval * 1000);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException(e.getMessage());
                }
            }
            retryNum++;
            response = doRequest(chain, request);
        }

        if (response == null) throw new InterruptedIOException("服务端无应答");
        return response;
    }

    private Response doRequest(Chain chain, Request request) {
        Response response = null;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            LogUtil.updateLog(logId, 1, e.getMessage());
            Log.w(TAG, e.getMessage());
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