package com.idormy.sms.forwarder;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.utils.SettingUtil;


public class SettingActivity extends AppCompatActivity {
    private String TAG = "SettingActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oncreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Switch switch_add_extra = (Switch) findViewById(R.id.switch_add_extra);
        switchAddExtra(switch_add_extra);

        EditText et_add_extra_device_mark = (EditText) findViewById(R.id.et_add_extra_device_mark);
        editAddExtraDeviceMark(et_add_extra_device_mark);

        EditText et_add_extra_sim1 = (EditText) findViewById(R.id.et_add_extra_sim1);
        editAddExtraSim1(et_add_extra_sim1);

        EditText et_add_extra_sim2 = (EditText) findViewById(R.id.et_add_extra_sim2);
        editAddExtraSim2(et_add_extra_sim2);
    }

    //设置转发附加信息
    private void switchAddExtra(Switch switch_add_extra) {
        switch_add_extra.setChecked(SettingUtil.getSwitchAddExtra());

        switch_add_extra.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingUtil.switchAddExtra(isChecked);
                Log.d(TAG, "onCheckedChanged:" + isChecked);
            }
        });
    }

    //设置转发附加信息devicemark
    private void editAddExtraDeviceMark(final EditText et_add_extra_device_mark) {
        et_add_extra_device_mark.setText(SettingUtil.getAddExtraDeviceMark());

        et_add_extra_device_mark.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SettingUtil.setAddExtraDeviceMark(et_add_extra_device_mark.getText().toString());

            }
        });
    }

    //设置转发附加信息devicemark
    private void editAddExtraSim1(final EditText et_add_extra_sim1) {
        et_add_extra_sim1.setText(SettingUtil.getAddExtraSim1());

        et_add_extra_sim1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SettingUtil.setAddExtraSim1(et_add_extra_sim1.getText().toString());

            }
        });
    }

    //设置转发附加信息devicemark
    private void editAddExtraSim2(final EditText et_add_extra_sim2) {
        et_add_extra_sim2.setText(SettingUtil.getAddExtraSim2());

        et_add_extra_sim2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SettingUtil.setAddExtraSim2(et_add_extra_sim2.getText().toString());

            }
        });
    }


}
