package com.idormy.sms.forwarder.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.idormy.sms.forwarder.R;

import java.util.regex.Pattern;

public class IPEditText extends LinearLayout {

    //控件
    private final EditText Edit1;
    private final EditText Edit2;
    private final EditText Edit3;
    private final EditText Edit4;
    private String ip1;
    private String ip2;
    private String ip3;
    private String ip4;

    public IPEditText(final Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化界面
        View view = LayoutInflater.from(context).inflate(R.layout.iptext, this);
        //绑定
        Edit1 = findViewById(R.id.edit1);
        Edit2 = findViewById(R.id.edit2);
        Edit3 = findViewById(R.id.edit3);
        Edit4 = findViewById(R.id.edit4);
        //初始化函数
        init(context);
    }

    private void init(final Context context) {
        /*
          监听文本，得到ip段，自动进入下一个输入框
         */
        Edit1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ip1 = s.toString().trim();
                int lenIp1 = ip1.length();
                if (lenIp1 > 0 && !Pattern.matches("^([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.?$", ip1)) {
                    ip1 = ip1.substring(0, lenIp1 - 1);
                    Edit1.setText(ip1);
                    Edit1.setSelection(ip1.length());
                    Toast.makeText(context, R.string.invalid_ip, Toast.LENGTH_LONG).show();
                    return;
                }
                //非空输入 . 跳到下一个输入框
                if (lenIp1 > 1 && ".".equals(ip1.substring(lenIp1 - 1))) {
                    ip1 = ip1.substring(0, lenIp1 - 1);
                    Edit1.setText(ip1);
                    Edit2.setFocusable(true);
                    Edit2.requestFocus();
                    return;
                }
                //已输3位数字，跳到下一个输入框
                if (lenIp1 > 2) {
                    Edit2.setFocusable(true);
                    Edit2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        Edit2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ip2 = s.toString().trim();
                int lenIp2 = ip2.length();
                if (lenIp2 > 0 && !Pattern.matches("^(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.?$", ip2)) {
                    ip2 = ip2.substring(0, lenIp2 - 1);
                    Edit2.setText(ip2);
                    Edit2.setSelection(ip2.length());
                    Toast.makeText(context, R.string.invalid_ip, Toast.LENGTH_LONG).show();
                    return;
                }
                //非空输入 . 跳到下一个输入框
                if (lenIp2 > 1 && ".".equals(ip2.substring(lenIp2 - 1))) {
                    ip2 = ip2.substring(0, lenIp2 - 1);
                    Edit2.setText(ip2);
                    Edit3.setFocusable(true);
                    Edit3.requestFocus();
                    return;
                }
                //已输3位数字，跳到下一个输入框
                if (lenIp2 > 2) {
                    Edit3.setFocusable(true);
                    Edit3.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Edit3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ip3 = s.toString().trim();
                int lenIp3 = ip3.length();
                if (lenIp3 > 0 && !Pattern.matches("^(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.?$", ip3)) {
                    ip3 = ip3.substring(0, lenIp3 - 1);
                    Edit3.setText(ip3);
                    Edit3.setSelection(ip3.length());
                    Toast.makeText(context, R.string.invalid_ip, Toast.LENGTH_LONG).show();
                    return;
                }
                //非空输入 . 跳到下一个输入框
                if (lenIp3 > 1 && ".".equals(ip3.substring(lenIp3 - 1))) {
                    ip3 = ip3.substring(0, lenIp3 - 1);
                    Edit3.setText(ip3);
                    Edit4.setFocusable(true);
                    Edit4.requestFocus();
                    return;
                }
                //已输3位数字，跳到下一个输入框
                if (lenIp3 > 2) {
                    Edit4.setFocusable(true);
                    Edit4.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Edit4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ip4 = s.toString().trim();
                int lenIp4 = ip4.length();
                if (lenIp4 > 0 && !Pattern.matches("^(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])$", ip4)) {
                    ip4 = ip4.substring(0, lenIp4 - 1);
                    Edit4.setText(ip4);
                    Edit4.setSelection(ip4.length());
                    Toast.makeText(context, R.string.invalid_ip, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        /*
           监听控件，空值时del键返回上一输入框
         */
        Edit2.setOnKeyListener((v, keyCode, event) -> {
            if (ip2 == null || ip2.isEmpty()) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    Edit1.setFocusable(true);
                    Edit1.requestFocus();
                    Edit1.setSelection(ip1.length());
                }
            }
            return false;
        });
        Edit3.setOnKeyListener((v, keyCode, event) -> {
            if (ip3 == null || ip3.isEmpty()) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    Edit2.setFocusable(true);
                    Edit2.requestFocus();
                    Edit2.setSelection(ip2.length());
                }
            }
            return false;
        });
        Edit4.setOnKeyListener((v, keyCode, event) -> {
            if (ip4 == null || ip4.isEmpty()) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    Edit3.setFocusable(true);
                    Edit3.requestFocus();
                    Edit3.setSelection(ip3.length());
                }
            }
            return false;
        });
    }

    /**
     * 成员函数，返回整个ip地址
     */
    public String getIP() {
        //文本
        String text;
        if (TextUtils.isEmpty(ip1) || TextUtils.isEmpty(ip2)
                || TextUtils.isEmpty(ip3) || TextUtils.isEmpty(ip4)) {
            text = null;
        } else {
            text = ip1 + "." + ip2 + "." + ip3 + "." + ip4;
        }
        return text;
    }

    /**
     * 成员函数，返回整个ip地址
     */
    public void setIP(String ip) {
        if (ip == null || ip.isEmpty()
                || !Pattern.matches("^([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$", ip)) {
            ip1 = "";
            ip2 = "";
            ip3 = "";
            ip4 = "";
        } else {
            String[] ips = ip.split("\\.");
            ip1 = ips[0];
            ip2 = ips[1];
            ip3 = ips[2];
            ip4 = ips[3];
        }

        Edit1.setText(ip1);
        Edit2.setText(ip2);
        Edit3.setText(ip3);
        Edit4.setText(ip4);
    }
}
