package com.idormy.sms.forwarder.view;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.idormy.sms.forwarder.MainActivity;
import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.RuleActivity;
import com.idormy.sms.forwarder.SenderActivity;
import com.idormy.sms.forwarder.SettingActivity;

public class StepBar extends LinearLayout {
    //控件
    private final TextView txStep1;
    private final TextView txStep2;
    private final TextView txStep3;
    private final TextView txStep4;
    private final TextView tvStep1;
    private final TextView tvStep2;
    private final TextView tvStep3;
    private final TextView tvStep4;

    public StepBar(final Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化界面
        View view = LayoutInflater.from(context).inflate(R.layout.step_bar, this);
        //绑定
        txStep1 = findViewById(R.id.txStep1);
        txStep2 = findViewById(R.id.txStep2);
        txStep3 = findViewById(R.id.txStep3);
        txStep4 = findViewById(R.id.txStep4);
        tvStep1 = findViewById(R.id.tvStep1);
        tvStep2 = findViewById(R.id.tvStep2);
        tvStep3 = findViewById(R.id.tvStep3);
        tvStep4 = findViewById(R.id.tvStep4);
        //初始化函数
        init(context);
    }

    private void init(final Context context) {

        tvStep1.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SettingActivity.class);
            v.getContext().startActivity(intent);
        });
        txStep1.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SettingActivity.class);
            v.getContext().startActivity(intent);
        });

        tvStep2.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SenderActivity.class);
            v.getContext().startActivity(intent);
        });
        txStep2.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SenderActivity.class);
            v.getContext().startActivity(intent);
        });

        tvStep3.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RuleActivity.class);
            v.getContext().startActivity(intent);
        });
        txStep3.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RuleActivity.class);
            v.getContext().startActivity(intent);
        });

        tvStep4.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MainActivity.class);
            v.getContext().startActivity(intent);
        });
        txStep4.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MainActivity.class);
            v.getContext().startActivity(intent);
        });

    }

}
