package com.idormy.sms.forwarder.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.idormy.sms.forwarder.MainActivity;
import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.RuleActivity;
import com.idormy.sms.forwarder.SenderActivity;
import com.idormy.sms.forwarder.SettingActivity;
import com.idormy.sms.forwarder.sender.SenderUtil;
import com.idormy.sms.forwarder.utils.LogUtils;
import com.idormy.sms.forwarder.utils.RuleUtils;
import com.idormy.sms.forwarder.utils.SettingUtils;

@SuppressWarnings("FieldCanBeLocal")
public class StepBar extends LinearLayout {
    private final Context mContext;
    private TypedArray mTypedArray;
    //自定义参数
    private String current_step;
    private String help_tip;
    //控件
    private TextView txHelpTip;
    private TextView txStep1;
    private TextView txStep2;
    private TextView txStep3;
    private TextView txStep4;
    private TextView tvStep1;
    private TextView tvStep2;
    private TextView tvStep3;
    private TextView tvStep4;
    private ImageView ivStep_12_1;
    private ImageView ivStep_12_2;
    private ImageView ivStep_12_3;
    private ImageView ivStep_23_1;
    private ImageView ivStep_23_2;
    private ImageView ivStep_23_3;
    private ImageView ivStep_34_1;
    private ImageView ivStep_34_2;
    private ImageView ivStep_34_3;

    public StepBar(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    public StepBar(final Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initParams(context, attrs);
        initView();
    }

    public StepBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initParams(context, attrs);
        initView();
    }

    private void initParams(Context context, AttributeSet attrs) {
        mTypedArray = mContext.obtainStyledAttributes(attrs, R.styleable.StepBar);
        if (mTypedArray != null) {
            current_step = mTypedArray.getString(R.styleable.StepBar_current_step);
            help_tip = mTypedArray.getString(R.styleable.StepBar_help_tip);
            mTypedArray.recycle();
        }
    }

    private void initView() {
        //初始化界面
        View view = LayoutInflater.from(mContext).inflate(R.layout.step_bar, this);

        txHelpTip = findViewById(R.id.txHelpTip);
        if (txHelpTip != null) {
            txHelpTip.setText(help_tip);
            txHelpTip.setVisibility(MyApplication.showHelpTip ? View.VISIBLE : View.GONE);
        }

        //步骤1
        txStep1 = findViewById(R.id.txStep1);
        tvStep1 = findViewById(R.id.tvStep1);
        if (!current_step.equalsIgnoreCase("setting")) {
            tvStep1.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), SettingActivity.class);
                v.getContext().startActivity(intent);
            });
            txStep1.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), SettingActivity.class);
                v.getContext().startActivity(intent);
            });
        } else {
            tvStep1.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
        }

        //步骤2
        txStep2 = findViewById(R.id.txStep2);
        tvStep2 = findViewById(R.id.tvStep2);
        if (!current_step.equalsIgnoreCase("sender")) {
            tvStep2.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), SenderActivity.class);
                v.getContext().startActivity(intent);
            });
            txStep2.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), SenderActivity.class);
                v.getContext().startActivity(intent);
            });
        } else {
            tvStep2.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
        }

        //步骤3
        txStep3 = findViewById(R.id.txStep3);
        tvStep3 = findViewById(R.id.tvStep3);
        if (!current_step.equalsIgnoreCase("rule")) {
            tvStep3.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), RuleActivity.class);
                v.getContext().startActivity(intent);
            });
            txStep3.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), RuleActivity.class);
                v.getContext().startActivity(intent);
            });
        } else {
            tvStep3.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
        }

        //步骤4
        txStep4 = findViewById(R.id.txStep4);
        tvStep4 = findViewById(R.id.tvStep4);
        if (!current_step.equalsIgnoreCase("main")) {
            tvStep4.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), MainActivity.class);
                v.getContext().startActivity(intent);
            });
            txStep4.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), MainActivity.class);
                v.getContext().startActivity(intent);
            });
        } else {
            tvStep4.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
        }

        ivStep_12_1 = findViewById(R.id.ivStep_12_1);
        ivStep_12_2 = findViewById(R.id.ivStep_12_2);
        ivStep_12_3 = findViewById(R.id.ivStep_12_3);
        ivStep_23_1 = findViewById(R.id.ivStep_23_1);
        ivStep_23_2 = findViewById(R.id.ivStep_23_2);
        ivStep_23_3 = findViewById(R.id.ivStep_23_3);
        ivStep_34_1 = findViewById(R.id.ivStep_34_1);
        ivStep_34_2 = findViewById(R.id.ivStep_34_2);
        ivStep_34_3 = findViewById(R.id.ivStep_34_3);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void setHighlight() {
        SettingUtils.init(mContext);
        SenderUtil.init(mContext);
        RuleUtils.init(mContext);
        LogUtils.init(mContext);

        boolean Step1 = SettingUtils.getSwitchEnableSms() || SettingUtils.getSwitchEnablePhone() || SettingUtils.getSwitchEnableAppNotify();
        boolean Step2 = SenderUtil.countSender("1", null) > 0;
        boolean Step3 = RuleUtils.countRule("1", null, null) > 0;
        boolean Step4 = LogUtils.countLog("2", null, null) > 0;

        //页面提示文本
        if (txHelpTip != null) {
            txHelpTip.setVisibility(MyApplication.showHelpTip ? View.VISIBLE : View.GONE);
            if (MyApplication.showHelpTip && current_step.equals("main")) {
                txHelpTip.setText(Step1 ? R.string.log_tips : R.string.setting_tips);
            }
        }

        if (Step1) txStep1.setBackground(mContext.getResources().getDrawable(R.drawable.step_circle_current));
        if (Step2) txStep2.setBackground(mContext.getResources().getDrawable(R.drawable.step_circle_current));
        if (Step3) txStep3.setBackground(mContext.getResources().getDrawable(R.drawable.step_circle_current));
        if (Step4) txStep4.setBackground(mContext.getResources().getDrawable(R.drawable.step_circle_current));

        if (Step1 && Step2) {
            ivStep_12_1.setImageResource(R.drawable.step_rectangle_current);
            ivStep_12_2.setImageResource(R.drawable.step_rectangle_current);
            ivStep_12_3.setImageResource(R.drawable.step_rectangle_current);
        }

        if (Step2 && Step3) {
            ivStep_23_1.setImageResource(R.drawable.step_rectangle_current);
            ivStep_23_2.setImageResource(R.drawable.step_rectangle_current);
            ivStep_23_3.setImageResource(R.drawable.step_rectangle_current);
        }

        if (Step3 && Step4) {
            ivStep_34_1.setImageResource(R.drawable.step_rectangle_current);
            ivStep_34_2.setImageResource(R.drawable.step_rectangle_current);
            ivStep_34_3.setImageResource(R.drawable.step_rectangle_current);
        }
    }

}
