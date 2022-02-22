package com.idormy.sms.forwarder;

import static com.idormy.sms.forwarder.model.SenderModel.STATUS_OFF;
import static com.idormy.sms.forwarder.model.SenderModel.STATUS_ON;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_BARK;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_DINGDING;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_EMAIL;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_FEISHU;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_GOTIFY;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_PUSHPLUS;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_APP;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_GROUP_ROBOT;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SERVER_CHAN;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SMS;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_TELEGRAM;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_WEB_NOTIFY;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.ToastUtils;
import com.idormy.sms.forwarder.adapter.SenderAdapter;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.vo.BarkSettingVo;
import com.idormy.sms.forwarder.model.vo.DingDingSettingVo;
import com.idormy.sms.forwarder.model.vo.EmailSettingVo;
import com.idormy.sms.forwarder.model.vo.FeiShuSettingVo;
import com.idormy.sms.forwarder.model.vo.GotifySettingVo;
import com.idormy.sms.forwarder.model.vo.PushPlusSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXAppSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXGroupRobotSettingVo;
import com.idormy.sms.forwarder.model.vo.ServerChanSettingVo;
import com.idormy.sms.forwarder.model.vo.SmsSettingVo;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.model.vo.TelegramSettingVo;
import com.idormy.sms.forwarder.model.vo.WebNotifySettingVo;
import com.idormy.sms.forwarder.sender.SenderBarkMsg;
import com.idormy.sms.forwarder.sender.SenderDingdingMsg;
import com.idormy.sms.forwarder.sender.SenderFeishuMsg;
import com.idormy.sms.forwarder.sender.SenderGotifyMsg;
import com.idormy.sms.forwarder.sender.SenderMailMsg;
import com.idormy.sms.forwarder.sender.SenderPushPlusMsg;
import com.idormy.sms.forwarder.sender.SenderQyWxAppMsg;
import com.idormy.sms.forwarder.sender.SenderQyWxGroupRobotMsg;
import com.idormy.sms.forwarder.sender.SenderServerChanMsg;
import com.idormy.sms.forwarder.sender.SenderSmsMsg;
import com.idormy.sms.forwarder.sender.SenderTelegramMsg;
import com.idormy.sms.forwarder.sender.SenderUtil;
import com.idormy.sms.forwarder.sender.SenderWebNotifyMsg;
import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.RuleUtil;
import com.idormy.sms.forwarder.view.ClearEditText;
import com.idormy.sms.forwarder.view.StepBar;

import java.lang.reflect.Method;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("deprecation")
public class SenderActivity extends AppCompatActivity {

    public static final int NOTIFY = 0x9731993;
    private final String TAG = "SenderActivity";
    // 用于存储数据
    private List<SenderModel> senderModels = new ArrayList<>();
    private SenderAdapter adapter;
    //消息处理者,创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == NOTIFY) {
                ToastUtils.delayedShow(msg.getData().getString("DATA"), 3000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        LogUtil.init(this);
        RuleUtil.init(this);
        SenderUtil.init(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        // 先拿到数据并放在适配器上
        initSenders(); //初始化数据
        adapter = new SenderAdapter(SenderActivity.this, R.layout.item_sender, senderModels);

        // 将适配器上的数据传递给listView
        ListView listView = findViewById(R.id.list_view_sender);
        listView.setAdapter(adapter);

        // 为ListView注册一个监听器，当用户点击了ListView中的任何一个子项时，就会回调onItemClick()方法
        // 在这个方法中可以通过position参数判断出用户点击的是那一个子项
        listView.setOnItemClickListener((parent, view, position, id) -> {
            SenderModel senderModel = senderModels.get(position);
            Log.d(TAG, "onItemClick: " + senderModel);

            switch (senderModel.getType()) {
                case TYPE_DINGDING:
                    setDingDing(senderModel, false);
                    break;
                case TYPE_EMAIL:
                    setEmail(senderModel, false);
                    break;
                case TYPE_BARK:
                    setBark(senderModel, false);
                    break;
                case TYPE_WEB_NOTIFY:
                    setWebNotify(senderModel, false);
                    break;
                case TYPE_QYWX_GROUP_ROBOT:
                    setQYWXGroupRobot(senderModel, false);
                    break;
                case TYPE_QYWX_APP:
                    setQYWXApp(senderModel, false);
                    break;
                case TYPE_SERVER_CHAN:
                    setServerChan(senderModel, false);
                    break;
                case TYPE_TELEGRAM:
                    setTelegram(senderModel, false);
                    break;
                case TYPE_SMS:
                    setSms(senderModel, false);
                    break;
                case TYPE_FEISHU:
                    setFeiShu(senderModel, false);
                    break;
                case TYPE_PUSHPLUS:
                    setPushPlus(senderModel, false);
                    break;
                case TYPE_GOTIFY:
                    setGotify(senderModel, false);
                    break;
                default:
                    ToastUtils.delayedShow(R.string.invalid_sender, 3000);
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                    break;
            }

        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            //定义AlertDialog.Builder对象，当长按列表项的时候弹出确认删除对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(SenderActivity.this);
            builder.setTitle(R.string.delete_sender_title);
            builder.setMessage(R.string.delete_sender_tips);

            //添加AlertDialog.Builder对象的setPositiveButton()方法
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                SenderUtil.delSender(senderModels.get(position).getId());
                initSenders();
                adapter.del(senderModels);
                ToastUtils.show(R.string.delete_sender_toast);
            });

            builder.setNeutralButton(R.string.clone, (dialog, which) -> {
                SenderModel senderModel = senderModels.get(position);
                switch (senderModel.getType()) {
                    case TYPE_DINGDING:
                        setDingDing(senderModel, true);
                        break;
                    case TYPE_EMAIL:
                        setEmail(senderModel, true);
                        break;
                    case TYPE_BARK:
                        setBark(senderModel, true);
                        break;
                    case TYPE_WEB_NOTIFY:
                        setWebNotify(senderModel, true);
                        break;
                    case TYPE_QYWX_GROUP_ROBOT:
                        setQYWXGroupRobot(senderModel, true);
                        break;
                    case TYPE_QYWX_APP:
                        setQYWXApp(senderModel, true);
                        break;
                    case TYPE_SERVER_CHAN:
                        setServerChan(senderModel, true);
                        break;
                    case TYPE_TELEGRAM:
                        setTelegram(senderModel, true);
                        break;
                    case TYPE_SMS:
                        setSms(senderModel, true);
                        break;
                    case TYPE_FEISHU:
                        setFeiShu(senderModel, true);
                        break;
                    case TYPE_PUSHPLUS:
                        setPushPlus(senderModel, true);
                        break;
                    case TYPE_GOTIFY:
                        setGotify(senderModel, true);
                        break;
                    default:
                        ToastUtils.delayedShow(R.string.invalid_sender, 3000);
                        SenderUtil.delSender(senderModel.getId());
                        initSenders();
                        adapter.del(senderModels);
                        break;
                }
            });

            //添加AlertDialog.Builder对象的setNegativeButton()方法
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {

            });

            builder.create().show();
            return true;
        });


        //计算浮动按钮位置
        FloatingActionButton btnFloat = findViewById(R.id.btnAddSender);
        CommonUtil.calcMarginBottom(this, btnFloat, listView, null);

        //添加发送通道
        btnFloat.setOnClickListener(v -> {

            @SuppressLint("InflateParams") View dialog_menu = LayoutInflater.from(SenderActivity.this).inflate(R.layout.alert_dialog_menu, null);
            // 设置style 控制默认dialog带来的边距问题
            final Dialog dialog = new Dialog(this, R.style.dialog_menu);
            dialog.setContentView(dialog_menu);
            dialog.show();

            GridView gridview = dialog.findViewById(R.id.MemuGridView);
            final List<HashMap<String, Object>> item = getMenuData();
            SimpleAdapter simpleAdapter = new SimpleAdapter(this, item, R.layout.item_menu, new String[]{"ItemImageView", "ItemTextView"}, new int[]{R.id.ItemImageView, R.id.ItemTextView});
            gridview.setAdapter(simpleAdapter);

            // 添加点击事件
            gridview.setOnItemClickListener((arg0, arg1, position, arg3) -> {
                dialog.dismiss();

                switch (position) {
                    case TYPE_DINGDING:
                        setDingDing(null, false);
                        break;
                    case TYPE_EMAIL:
                        setEmail(null, false);
                        break;
                    case TYPE_BARK:
                        setBark(null, false);
                        break;
                    case TYPE_WEB_NOTIFY:
                        setWebNotify(null, false);
                        break;
                    case TYPE_QYWX_GROUP_ROBOT:
                        setQYWXGroupRobot(null, false);
                        break;
                    case TYPE_QYWX_APP:
                        setQYWXApp(null, false);
                        break;
                    case TYPE_SERVER_CHAN:
                        setServerChan(null, false);
                        break;
                    case TYPE_TELEGRAM:
                        setTelegram(null, false);
                        break;
                    case TYPE_SMS:
                        setSms(null, false);
                        break;
                    case TYPE_FEISHU:
                        setFeiShu(null, false);
                        break;
                    case TYPE_PUSHPLUS:
                        setPushPlus(null, false);
                        break;
                    case TYPE_GOTIFY:
                        setGotify(null, false);
                        break;
                    default:
                        ToastUtils.delayedShow(R.string.not_supported, 3000);
                        break;
                }
            });
        });

        //步骤完成状态校验
        StepBar stepBar = findViewById(R.id.stepBar);
        stepBar.setHighlight();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        overridePendingTransition(0, 0);
        super.onPause();
    }

    // 初始化数据
    private void initSenders() {
        senderModels = SenderUtil.getSender(null, null);
    }

    // 获取发送通道菜单
    private List<HashMap<String, Object>> getMenuData() {
        //定义图标数组
        int[] imageRes = {
                R.mipmap.dingding,
                R.mipmap.email,
                R.mipmap.bark,
                R.mipmap.webhook,
                R.mipmap.qywx,
                R.mipmap.qywxapp,
                R.mipmap.serverchan,
                R.mipmap.telegram,
                R.mipmap.sms,
                R.mipmap.feishu,
                R.mipmap.pushplus,
                R.mipmap.gotify,
        };
        //定义标题数组
        String[] itemName = {
                getString(R.string.dingding),
                getString(R.string.email),
                getString(R.string.bark),
                getString(R.string.webhook),
                getString(R.string.qywx),
                getString(R.string.qywxapp),
                getString(R.string.serverchan),
                getString(R.string.telegram),
                getString(R.string.sms_menu),
                getString(R.string.feishu),
                getString(R.string.pushplus),
                getString(R.string.gotify),
        };
        List<HashMap<String, Object>> data = new ArrayList<>();
        int length = itemName.length;
        for (int i = 0; i < length; i++) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("ItemImageView", imageRes[i]);
            map.put("ItemTextView", itemName[i]);
            data.add(map);
        }
        return data;
    }

    //钉钉机器人
    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void setDingDing(final SenderModel senderModel, final boolean isClone) {
        DingDingSettingVo dingDingSettingVo = null;
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                dingDingSettingVo = JSON.parseObject(jsonSettingStr, DingDingSettingVo.class);
            }
        }
        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_dingding, null);

        final EditText editTextDingdingName = view1.findViewById(R.id.editTextDingdingName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchDingdingEnable = view1.findViewById(R.id.switchDingdingEnable);
        if (senderModel != null) {
            editTextDingdingName.setText(senderModel.getName());
            switchDingdingEnable.setChecked(senderModel.getStatusChecked());
        }

        final ClearEditText editTextDingdingToken = view1.findViewById(R.id.editTextDingdingToken);
        final ClearEditText editTextDingdingSecret = view1.findViewById(R.id.editTextDingdingSecret);
        final EditText editTextDingdingAtMobiles = view1.findViewById(R.id.editTextDingdingAtMobiles);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchDingdingAtAll = view1.findViewById(R.id.switchDingdingAtAll);
        final LinearLayout linearLayoutDingdingAtMobiles = view1.findViewById(R.id.linearLayoutDingdingAtMobiles);
        if (dingDingSettingVo != null) {
            editTextDingdingToken.setText(dingDingSettingVo.getToken());
            editTextDingdingSecret.setText(dingDingSettingVo.getSecret());
            editTextDingdingAtMobiles.setText(dingDingSettingVo.getAtMobiles());
            if (dingDingSettingVo.getAtAll() != null) {
                switchDingdingAtAll.setChecked(dingDingSettingVo.getAtAll());
                linearLayoutDingdingAtMobiles.setVisibility(dingDingSettingVo.getAtAll() ? View.GONE : View.VISIBLE);
            }
        }

        switchDingdingAtAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                linearLayoutDingdingAtMobiles.setVisibility(View.GONE);
                editTextDingdingAtMobiles.setText("@all");
            } else {
                linearLayoutDingdingAtMobiles.setVisibility(View.VISIBLE);
                editTextDingdingAtMobiles.setText("");
            }
            Log.d(TAG, "onCheckedChanged:" + isChecked);
        });

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setdingdingtitle)
                .setIcon(R.mipmap.dingding)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonOk.setOnClickListener(view -> {
            String senderName = editTextDingdingName.getText().toString().trim();
            int senderStatus = switchDingdingEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            String token = editTextDingdingToken.getText().trim();
            String secret = editTextDingdingSecret.getText().trim();
            String atMobiles = editTextDingdingAtMobiles.getText().toString().trim();
            Boolean atAll = switchDingdingAtAll.isChecked();

            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }
            if (CommonUtil.checkUrl(token, true)) {
                ToastUtils.delayedShow(R.string.invalid_token, 3000);
                return;
            }

            DingDingSettingVo dingDingSettingVoNew = new DingDingSettingVo(token, secret, atMobiles, atAll);
            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_DINGDING);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(dingDingSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_DINGDING);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(dingDingSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String token = editTextDingdingToken.getText().trim();
            if (CommonUtil.checkUrl(token, true)) {
                ToastUtils.delayedShow(R.string.invalid_token, 3000);
                return;
            }

            String secret = editTextDingdingSecret.getText().trim();
            String atMobiles = editTextDingdingAtMobiles.getText().toString().trim();
            Boolean atAll = switchDingdingAtAll.isChecked();
            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderDingdingMsg.sendMsg(0, handler, null, token, secret, atMobiles, atAll, smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });
    }

    //邮箱
    @SuppressLint("SimpleDateFormat")
    private void setEmail(final SenderModel senderModel, final boolean isClone) {
        EmailSettingVo emailSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                emailSettingVo = JSON.parseObject(jsonSettingStr, EmailSettingVo.class);
            }
        }

        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_email, null);

        final EditText editTextEmailName = view1.findViewById(R.id.editTextEmailName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchEmailEnable = view1.findViewById(R.id.switchEmailEnable);
        if (senderModel != null) {
            editTextEmailName.setText(senderModel.getName());
            switchEmailEnable.setChecked(senderModel.getStatusChecked());
        }

        final EditText editTextEmailHost = view1.findViewById(R.id.editTextEmailHost);
        final EditText editTextEmailPort = view1.findViewById(R.id.editTextEmailPort);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchEmailSSl = view1.findViewById(R.id.switchEmailSSl);
        final EditText editTextEmailFromAdd = view1.findViewById(R.id.editTextEmailFromAdd);
        final EditText editTextEmailNickname = view1.findViewById(R.id.editTextEmailNickname);
        final ClearEditText editTextEmailPsw = view1.findViewById(R.id.editTextEmailPsw);
        final EditText editTextEmailToAdd = view1.findViewById(R.id.editTextEmailToAdd);
        final EditText editTextEmailTitle = view1.findViewById(R.id.editTextEmailTitle);
        final RadioGroup radioGroupEmailProtocol = view1.findViewById(R.id.radioGroupEmailProtocol);
        if (emailSettingVo != null) {
            radioGroupEmailProtocol.check(emailSettingVo.getEmailProtocolCheckId());
            editTextEmailHost.setText(emailSettingVo.getHost());
            editTextEmailPort.setText(emailSettingVo.getPort());
            switchEmailSSl.setChecked(emailSettingVo.getSsl());
            editTextEmailFromAdd.setText(emailSettingVo.getFromEmail());
            editTextEmailNickname.setText(emailSettingVo.getNickname());
            editTextEmailPsw.setText(emailSettingVo.getPwd());
            editTextEmailToAdd.setText(emailSettingVo.getToEmail());
            editTextEmailTitle.setText(emailSettingVo.getTitle());
        }

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setemailtitle)
                .setIcon(R.mipmap.email)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonOk.setOnClickListener(view -> {
            String senderName = editTextEmailName.getText().toString().trim();
            int senderStatus = switchEmailEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            String protocol = radioGroupEmailProtocol.getCheckedRadioButtonId() == R.id.radioEmailProtocolSmtp ? "SMTP" : "IMAP";
            String host = editTextEmailHost.getText().toString().trim();
            String port = editTextEmailPort.getText().toString().trim();
            boolean ssl = switchEmailSSl.isChecked();
            String fromEmail = editTextEmailFromAdd.getText().toString().trim();
            String pwd = editTextEmailPsw.getText().trim();
            String toEmail = editTextEmailToAdd.getText().toString().trim();

            String title = editTextEmailTitle.getText().toString().trim();
            if (title.isEmpty()) title = "SmsForwarder Title";

            String nickname = editTextEmailNickname.getText().toString().trim();
            if (nickname.isEmpty()) nickname = "SmsForwarder";
            if (host.isEmpty() || port.isEmpty() || fromEmail.isEmpty() || pwd.isEmpty() || toEmail.isEmpty()) {
                ToastUtils.delayedShow(R.string.invalid_email, 3000);
                return;
            }

            EmailSettingVo emailSettingVoNew = new EmailSettingVo(protocol, host, port, ssl, fromEmail, nickname, pwd, toEmail, title);
            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_EMAIL);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(emailSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_EMAIL);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(emailSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String protocol = radioGroupEmailProtocol.getCheckedRadioButtonId() == R.id.radioEmailProtocolSmtp ? "SMTP" : "IMAP";
            String host = editTextEmailHost.getText().toString().trim();
            String port = editTextEmailPort.getText().toString().trim();
            boolean ssl = switchEmailSSl.isChecked();
            String fromEmail = editTextEmailFromAdd.getText().toString().trim();
            String pwd = editTextEmailPsw.getText().trim();
            String toEmail = editTextEmailToAdd.getText().toString().trim();

            String title = editTextEmailTitle.getText().toString().trim();
            if (title.isEmpty()) title = "SmsForwarder Title";

            String nickname = editTextEmailNickname.getText().toString().trim();
            if (nickname.isEmpty()) nickname = "SmsForwarder";

            if (host.isEmpty() || port.isEmpty() || fromEmail.isEmpty() || pwd.isEmpty() || toEmail.isEmpty()) {
                ToastUtils.delayedShow(R.string.invalid_email, 3000);
                return;
            }

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderMailMsg.sendEmail(0, handler, protocol, host, port, ssl, fromEmail, nickname, pwd, toEmail, smsVo.getTitleForSend(title), smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });


        Button buttonInsertSender = view1.findViewById(R.id.bt_insert_sender);
        buttonInsertSender.setOnClickListener(view -> {
            editTextEmailTitle.setFocusable(true);
            editTextEmailTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextEmailTitle, getString(R.string.tag_from));
        });

        Button buttonInsertExtra = view1.findViewById(R.id.bt_insert_extra);
        buttonInsertExtra.setOnClickListener(view -> {
            editTextEmailTitle.setFocusable(true);
            editTextEmailTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextEmailTitle, getString(R.string.tag_card_slot));
        });

        Button buttonInsertTime = view1.findViewById(R.id.bt_insert_time);
        buttonInsertTime.setOnClickListener(view -> {
            editTextEmailTitle.setFocusable(true);
            editTextEmailTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextEmailTitle, getString(R.string.tag_receive_time));
        });

        Button buttonInsertDeviceName = view1.findViewById(R.id.bt_insert_device_name);
        buttonInsertDeviceName.setOnClickListener(view -> {
            editTextEmailTitle.setFocusable(true);
            editTextEmailTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextEmailTitle, getString(R.string.tag_device_name));
        });

    }

    //Bark
    private void setBark(final SenderModel senderModel, final boolean isClone) {
        BarkSettingVo barkSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                barkSettingVo = JSON.parseObject(jsonSettingStr, BarkSettingVo.class);
            }
        }

        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_bark, null);

        final EditText editTextBarkName = view1.findViewById(R.id.editTextBarkName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchBarkEnable = view1.findViewById(R.id.switchBarkEnable);
        if (senderModel != null) {
            editTextBarkName.setText(senderModel.getName());
            switchBarkEnable.setChecked(senderModel.getStatusChecked());
        }

        final ClearEditText editTextBarkServer = view1.findViewById(R.id.editTextBarkServer);
        final EditText editTextBarkTitle = view1.findViewById(R.id.editTextBarkTitle);
        final EditText editTextBarkIcon = view1.findViewById(R.id.editTextBarkIcon);
        final EditText editTextBarkSound = view1.findViewById(R.id.editTextBarkSound);
        final EditText editTextBarkBadge = view1.findViewById(R.id.editTextBarkBadge);
        final EditText editTextBarkUrl = view1.findViewById(R.id.editTextBarkUrl);
        final RadioGroup radioGroupBarkLevel = view1.findViewById(R.id.radioGroupBarkLevel);
        if (barkSettingVo != null) {
            editTextBarkServer.setText(barkSettingVo.getServer());
            editTextBarkTitle.setText(barkSettingVo.getTitle());
            editTextBarkIcon.setText(barkSettingVo.getIcon());
            editTextBarkSound.setText(barkSettingVo.getSound());
            editTextBarkBadge.setText(barkSettingVo.getBadge());
            editTextBarkUrl.setText(barkSettingVo.getUrl());
            radioGroupBarkLevel.check(barkSettingVo.getLevelId());
        }

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setbarktitle)
                .setIcon(R.mipmap.bark)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonOk.setOnClickListener(view -> {
            String senderName = editTextBarkName.getText().toString().trim();
            int senderStatus = switchBarkEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            //推送地址
            String barkServer = editTextBarkServer.getText().trim();
            if (!CommonUtil.checkUrl(barkServer, false)) {
                ToastUtils.delayedShow(R.string.invalid_bark_server, 3000);
                return;
            }

            String icon = editTextBarkIcon.getText().toString().trim(); //消息图标
            String title = editTextBarkTitle.getText().toString().trim(); //标题模板
            int levelId = radioGroupBarkLevel.getCheckedRadioButtonId(); //时效性
            String sound = editTextBarkSound.getText().toString().trim(); //声音
            String badge = editTextBarkBadge.getText().toString().trim(); //角标
            String url = editTextBarkUrl.getText().toString().trim(); //链接
            BarkSettingVo barkSettingVoNew = new BarkSettingVo(barkServer, icon, title, levelId, sound, badge, url);
            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_BARK);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(barkSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_BARK);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(barkSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String barkServer = editTextBarkServer.getText().trim();
            String icon = editTextBarkIcon.getText().toString().trim(); //消息图标
            String title = editTextBarkTitle.getText().toString().trim(); //标题模板
            int levelId = radioGroupBarkLevel.getCheckedRadioButtonId(); //时效性
            String sound = editTextBarkSound.getText().toString().trim(); //声音
            String badge = editTextBarkBadge.getText().toString().trim(); //角标
            String url = editTextBarkUrl.getText().toString().trim(); //链接
            BarkSettingVo barkSettingVoNew = new BarkSettingVo(barkServer, icon, title, levelId, sound, badge, url);
            if (CommonUtil.checkUrl(barkServer, false)) {
                try {
                    SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                    SenderBarkMsg.sendMsg(0, handler, null, barkSettingVoNew, smsVo.getTitleForSend(title), smsVo.getSmsVoForSend(), getString(R.string.test_group_name));
                } catch (Exception e) {
                    ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                    e.printStackTrace();
                }
            } else {
                ToastUtils.delayedShow(R.string.invalid_bark_server, 3000);
            }
        });


        Button buttonInsertSender = view1.findViewById(R.id.bt_insert_sender);
        buttonInsertSender.setOnClickListener(view -> {
            editTextBarkTitle.setFocusable(true);
            editTextBarkTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextBarkTitle, getString(R.string.tag_from));
        });

        Button buttonInsertExtra = view1.findViewById(R.id.bt_insert_extra);
        buttonInsertExtra.setOnClickListener(view -> {
            editTextBarkTitle.setFocusable(true);
            editTextBarkTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextBarkTitle, getString(R.string.tag_card_slot));
        });

        Button buttonInsertTime = view1.findViewById(R.id.bt_insert_time);
        buttonInsertTime.setOnClickListener(view -> {
            editTextBarkTitle.setFocusable(true);
            editTextBarkTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextBarkTitle, getString(R.string.tag_receive_time));
        });

        Button buttonInsertDeviceName = view1.findViewById(R.id.bt_insert_device_name);
        buttonInsertDeviceName.setOnClickListener(view -> {
            editTextBarkTitle.setFocusable(true);
            editTextBarkTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextBarkTitle, getString(R.string.tag_device_name));
        });

    }

    //webhook
    @SuppressLint("SimpleDateFormat")
    private void setWebNotify(final SenderModel senderModel, final boolean isClone) {
        WebNotifySettingVo webNotifySettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                webNotifySettingVo = JSON.parseObject(jsonSettingStr, WebNotifySettingVo.class);
            }
        }

        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_webnotify, null);

        final EditText editTextWebNotifyName = view1.findViewById(R.id.editTextWebNotifyName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchWebNotifyEnable = view1.findViewById(R.id.switchWebNotifyEnable);
        if (senderModel != null) {
            editTextWebNotifyName.setText(senderModel.getName());
            switchWebNotifyEnable.setChecked(senderModel.getStatusChecked());
        }

        final EditText editTextWebNotifyWebServer = view1.findViewById(R.id.editTextWebNotifyWebServer);
        final EditText editTextWebNotifyWebParams = view1.findViewById(R.id.editTextWebNotifyWebParams);
        final ClearEditText editTextWebNotifySecret = view1.findViewById(R.id.editTextWebNotifySecret);
        final RadioGroup radioGroupWebNotifyMethod = view1.findViewById(R.id.radioGroupWebNotifyMethod);
        if (webNotifySettingVo != null) {
            editTextWebNotifyWebServer.setText(webNotifySettingVo.getWebServer());
            editTextWebNotifyWebParams.setText(webNotifySettingVo.getWebParams());
            editTextWebNotifySecret.setText(webNotifySettingVo.getSecret());
            radioGroupWebNotifyMethod.check(webNotifySettingVo.getWebNotifyMethodCheckId());
        }

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setwebnotifytitle)
                .setIcon(R.mipmap.webhook)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonOk.setOnClickListener(view -> {
            String senderName = editTextWebNotifyName.getText().toString().trim();
            int senderStatus = switchWebNotifyEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            String webServer = editTextWebNotifyWebServer.getText().toString().trim();
            String secret = editTextWebNotifySecret.getText().trim();
            String method = radioGroupWebNotifyMethod.getCheckedRadioButtonId() == R.id.radioWebNotifyMethodGet ? "GET" : "POST";
            String webParams = editTextWebNotifyWebParams.getText().toString().trim();

            if (!CommonUtil.checkUrl(webServer, false)) {
                ToastUtils.delayedShow(R.string.invalid_webserver, 3000);
                return;
            }

            WebNotifySettingVo webNotifySettingVoNew = new WebNotifySettingVo(webServer, secret, method, webParams);
            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_WEB_NOTIFY);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(webNotifySettingVoNew));
                SenderUtil.addSender(newSenderModel);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_WEB_NOTIFY);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(webNotifySettingVoNew));
                SenderUtil.updateSender(senderModel);
            }
            initSenders();
            adapter.update(senderModels);
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String webServer = editTextWebNotifyWebServer.getText().toString().trim();
            String secret = editTextWebNotifySecret.getText().trim();
            String method = radioGroupWebNotifyMethod.getCheckedRadioButtonId() == R.id.radioWebNotifyMethodGet ? "GET" : "POST";
            String webParams = editTextWebNotifyWebParams.getText().toString().trim();

            if (!CommonUtil.checkUrl(webServer, false)) {
                ToastUtils.delayedShow(R.string.invalid_webserver, 3000);
                return;
            }

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderWebNotifyMsg.sendMsg(0, handler, null, webServer, webParams, secret, method, smsVo, "", "");
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });
    }

    //企业微信群机器人
    @SuppressLint("SimpleDateFormat")
    private void setQYWXGroupRobot(final SenderModel senderModel, final boolean isClone) {
        QYWXGroupRobotSettingVo qywxGroupRobotSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                qywxGroupRobotSettingVo = JSON.parseObject(jsonSettingStr, QYWXGroupRobotSettingVo.class);
            }
        }

        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_qywxgrouprobot, null);

        final EditText editTextQYWXGroupRobotName = view1.findViewById(R.id.editTextQYWXGroupRobotName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchQYWXGroupRobotEnable = view1.findViewById(R.id.switchQYWXGroupRobotEnable);
        if (senderModel != null) {
            editTextQYWXGroupRobotName.setText(senderModel.getName());
            switchQYWXGroupRobotEnable.setChecked(senderModel.getStatusChecked());
        }

        final ClearEditText editTextQYWXGroupRobotWebHook = view1.findViewById(R.id.editTextQYWXGroupRobotWebHook);
        if (qywxGroupRobotSettingVo != null) {
            editTextQYWXGroupRobotWebHook.setText(qywxGroupRobotSettingVo.getWebHook());
        }

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setqywxgrouprobottitle)
                .setIcon(R.mipmap.qywx)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonOk.setOnClickListener(view -> {
            String senderName = editTextQYWXGroupRobotName.getText().toString().trim();
            int senderStatus = switchQYWXGroupRobotEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            String webHook = editTextQYWXGroupRobotWebHook.getText().trim();
            if (!CommonUtil.checkUrl(webHook, false)) {
                ToastUtils.delayedShow(R.string.invalid_webhook, 3000);
                return;
            }

            QYWXGroupRobotSettingVo qywxGroupRobotSettingVoNew = new QYWXGroupRobotSettingVo(webHook);
            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_QYWX_GROUP_ROBOT);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(qywxGroupRobotSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_QYWX_GROUP_ROBOT);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(qywxGroupRobotSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String webHook = editTextQYWXGroupRobotWebHook.getText().trim();
            if (!CommonUtil.checkUrl(webHook, false)) {
                ToastUtils.delayedShow(R.string.invalid_webhook, 3000);
                return;
            }

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderQyWxGroupRobotMsg.sendMsg(0, handler, null, webHook, smsVo.getMobile(), smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });
    }

    //企业微信应用
    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void setQYWXApp(final SenderModel senderModel, final boolean isClone) {
        QYWXAppSettingVo QYWXAppSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                QYWXAppSettingVo = JSON.parseObject(jsonSettingStr, QYWXAppSettingVo.class);
            }
        }
        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_qywxapp, null);

        final EditText editTextQYWXAppName = view1.findViewById(R.id.editTextQYWXAppName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchQYWXAppEnable = view1.findViewById(R.id.switchQYWXAppEnable);
        if (senderModel != null) {
            editTextQYWXAppName.setText(senderModel.getName());
            switchQYWXAppEnable.setChecked(senderModel.getStatusChecked());
        }

        final EditText editTextQYWXAppCorpID = view1.findViewById(R.id.editTextQYWXAppCorpID);
        final EditText editTextQYWXAppAgentID = view1.findViewById(R.id.editTextQYWXAppAgentID);
        final ClearEditText editTextQYWXAppSecret = view1.findViewById(R.id.editTextQYWXAppSecret);
        final LinearLayout linearLayoutQYWXAppToUser = view1.findViewById(R.id.linearLayoutQYWXAppToUser);
        final EditText editTextQYWXAppToUser = view1.findViewById(R.id.editTextQYWXAppToUser);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchQYWXAppAtAll = view1.findViewById(R.id.switchQYWXAppAtAll);
        if (QYWXAppSettingVo != null) {
            editTextQYWXAppCorpID.setText(QYWXAppSettingVo.getCorpID());
            editTextQYWXAppAgentID.setText(QYWXAppSettingVo.getAgentID());
            editTextQYWXAppSecret.setText(QYWXAppSettingVo.getSecret());
            editTextQYWXAppToUser.setText(QYWXAppSettingVo.getToUser());
            switchQYWXAppAtAll.setChecked(QYWXAppSettingVo.getAtAll());
            linearLayoutQYWXAppToUser.setVisibility(QYWXAppSettingVo.getAtAll() ? View.GONE : View.VISIBLE);
        }
        switchQYWXAppAtAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                linearLayoutQYWXAppToUser.setVisibility(View.GONE);
                editTextQYWXAppToUser.setText("@all");
            } else {
                linearLayoutQYWXAppToUser.setVisibility(View.VISIBLE);
                editTextQYWXAppToUser.setText("");
            }
            Log.d(TAG, "onCheckedChanged:" + isChecked);
        });

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setqywxapptitle)
                .setIcon(R.mipmap.qywxapp)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonOk.setOnClickListener(view -> {
            String senderName = editTextQYWXAppName.getText().toString().trim();
            int senderStatus = switchQYWXAppEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            String toUser = editTextQYWXAppToUser.getText().toString().trim();
            if (toUser.isEmpty()) {
                ToastUtils.delayedShow(R.string.invalid_at_mobiles, 3000);
                editTextQYWXAppToUser.setFocusable(true);
                editTextQYWXAppToUser.requestFocus();
                return;
            }

            QYWXAppSettingVo QYWXAppSettingVoNew = new QYWXAppSettingVo(
                    editTextQYWXAppCorpID.getText().toString().trim(),
                    editTextQYWXAppAgentID.getText().toString().trim(),
                    editTextQYWXAppSecret.getText().trim(),
                    editTextQYWXAppToUser.getText().toString().trim(),
                    switchQYWXAppAtAll.isChecked());
            if (!QYWXAppSettingVoNew.checkParms()) {
                ToastUtils.delayedShow(R.string.invalid_webcom_app_parm, 3000);
                return;
            }

            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_QYWX_APP);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(QYWXAppSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_QYWX_APP);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(QYWXAppSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            QYWXAppSettingVo QYWXAppSettingVoNew = new QYWXAppSettingVo(
                    editTextQYWXAppCorpID.getText().toString().trim(),
                    editTextQYWXAppAgentID.getText().toString().trim(),
                    editTextQYWXAppSecret.getText().trim(),
                    editTextQYWXAppToUser.getText().toString().trim(),
                    switchQYWXAppAtAll.isChecked());
            if (!QYWXAppSettingVoNew.checkParms()) {
                ToastUtils.delayedShow(R.string.invalid_webcom_app_parm, 3000);
                return;
            }
            if (QYWXAppSettingVoNew.getToUser().isEmpty()) {
                ToastUtils.delayedShow(R.string.invalid_at_mobiles, 3000);
                return;
            }

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderQyWxAppMsg.sendMsg(0, handler, null, senderModel, QYWXAppSettingVoNew, smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });
    }

    //Server酱·Turbo版
    private void setServerChan(final SenderModel senderModel, final boolean isClone) {
        ServerChanSettingVo serverchanSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                serverchanSettingVo = JSON.parseObject(jsonSettingStr, ServerChanSettingVo.class);
            }
        }

        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_serverchan, null);

        final EditText editTextServerChanName = view1.findViewById(R.id.editTextServerChanName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchServerChanEnable = view1.findViewById(R.id.switchServerChanEnable);
        if (senderModel != null) {
            editTextServerChanName.setText(senderModel.getName());
            switchServerChanEnable.setChecked(senderModel.getStatusChecked());
        }

        final ClearEditText editTextServerChanSendKey = view1.findViewById(R.id.editTextServerChanSendKey);
        if (serverchanSettingVo != null)
            editTextServerChanSendKey.setText(serverchanSettingVo.getSendKey());

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setserverchantitle)
                .setIcon(R.mipmap.serverchan)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonOk.setOnClickListener(view -> {
            String senderName = editTextServerChanName.getText().toString().trim();
            int senderStatus = switchServerChanEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            String serverChanServer = editTextServerChanSendKey.getText().trim();
            if (TextUtils.isEmpty(serverChanServer)) {
                ToastUtils.delayedShow(R.string.invalid_sendkey, 3000);
                return;
            }
            ServerChanSettingVo serverChanSettingVoNew = new ServerChanSettingVo(serverChanServer);

            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_SERVER_CHAN);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(serverChanSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_SERVER_CHAN);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(serverChanSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String serverChanServer = editTextServerChanSendKey.getText().trim();
            if (TextUtils.isEmpty(serverChanServer)) {
                ToastUtils.delayedShow(R.string.invalid_sendkey, 3000);
                return;
            }

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderServerChanMsg.sendMsg(0, handler, null, serverChanServer, smsVo.getMobile(), smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });
    }

    //Telegram机器人
    private void setTelegram(final SenderModel senderModel, final boolean isClone) {
        TelegramSettingVo telegramSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                telegramSettingVo = JSON.parseObject(jsonSettingStr, TelegramSettingVo.class);
            }
        }

        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_telegram, null);

        final EditText editTextTelegramName = view1.findViewById(R.id.editTextTelegramName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchTelegramEnable = view1.findViewById(R.id.switchTelegramEnable);
        if (senderModel != null) {
            editTextTelegramName.setText(senderModel.getName());
            switchTelegramEnable.setChecked(senderModel.getStatusChecked());
        }

        final ClearEditText editTextTelegramApiToken = view1.findViewById(R.id.editTextTelegramApiToken);
        final EditText editTextTelegramChatId = view1.findViewById(R.id.editTextTelegramChatId);
        final RadioGroup radioGroupTelegramMethod = view1.findViewById(R.id.radioGroupTelegramMethod);

        final RadioGroup radioGroupProxyType = view1.findViewById(R.id.radioGroupProxyType);
        final EditText editTextProxyHost = view1.findViewById(R.id.editTextProxyHost);
        final EditText editTextProxyPort = view1.findViewById(R.id.editTextProxyPort);

        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchProxyAuthenticator = view1.findViewById(R.id.switchProxyAuthenticator);
        final EditText editTextProxyUsername = view1.findViewById(R.id.editTextProxyUsername);
        final ClearEditText editTextProxyPassword = view1.findViewById(R.id.editTextProxyPassword);

        final LinearLayout layoutProxyHost = view1.findViewById(R.id.layoutProxyHost);
        final LinearLayout layoutProxyPort = view1.findViewById(R.id.layoutProxyPort);
        final LinearLayout layoutProxyAuthenticator = view1.findViewById(R.id.layoutProxyAuthenticator);

        switchProxyAuthenticator.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCheckedChanged:" + isChecked);
            layoutProxyAuthenticator.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        radioGroupProxyType.setOnCheckedChangeListener((group, checkedId) -> {
            if (group != null && checkedId > 0) {
                if (checkedId == R.id.btnProxyNone) {
                    layoutProxyHost.setVisibility(View.GONE);
                    layoutProxyPort.setVisibility(View.GONE);
                    layoutProxyAuthenticator.setVisibility(View.GONE);
                } else {
                    layoutProxyHost.setVisibility(View.VISIBLE);
                    layoutProxyPort.setVisibility(View.VISIBLE);
                    layoutProxyAuthenticator.setVisibility(switchProxyAuthenticator.isChecked() ? View.VISIBLE : View.GONE);
                }
                group.check(checkedId);
            }
        });

        if (telegramSettingVo != null) {
            editTextTelegramApiToken.setText(telegramSettingVo.getApiToken());
            editTextTelegramChatId.setText(telegramSettingVo.getChatId());
            radioGroupTelegramMethod.check(telegramSettingVo.getMethodCheckId());

            radioGroupProxyType.check(telegramSettingVo.getProxyTypeCheckId());
            layoutProxyAuthenticator.setVisibility(telegramSettingVo.getProxyAuthenticator() ? View.VISIBLE : View.GONE);

            switchProxyAuthenticator.setChecked(telegramSettingVo.getProxyAuthenticator());
            if (Proxy.Type.DIRECT == telegramSettingVo.getProxyType()) {
                layoutProxyHost.setVisibility(View.GONE);
                layoutProxyPort.setVisibility(View.GONE);
            } else {
                layoutProxyHost.setVisibility(View.VISIBLE);
                layoutProxyPort.setVisibility(View.VISIBLE);
            }
            editTextProxyHost.setText(telegramSettingVo.getProxyHost());
            editTextProxyPort.setText(telegramSettingVo.getProxyPort());

            editTextProxyUsername.setText(telegramSettingVo.getProxyUsername());
            editTextProxyPassword.setText(telegramSettingVo.getProxyPassword());
        }

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.settelegramtitle)
                .setIcon(R.mipmap.telegram)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonOk.setOnClickListener(view -> {
            String senderName = editTextTelegramName.getText().toString().trim();
            int senderStatus = switchTelegramEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            String apiToken = editTextTelegramApiToken.getText().trim();
            String chatId = editTextTelegramChatId.getText().toString().trim();
            if (apiToken.isEmpty() || chatId.isEmpty()) {
                ToastUtils.delayedShow(R.string.invalid_apiToken_or_chatId, 3000);
                return;
            }

            int proxyTypeId = radioGroupProxyType.getCheckedRadioButtonId();
            String proxyHost = editTextProxyHost.getText().toString().trim();
            String proxyPort = editTextProxyPort.getText().toString().trim();
            if (proxyTypeId != R.id.btnProxyNone && (TextUtils.isEmpty(proxyHost) || TextUtils.isEmpty(proxyPort))) {
                ToastUtils.delayedShow(R.string.invalid_host_or_port, 3000);
                return;
            }

            boolean proxyAuthenticator = switchProxyAuthenticator.isChecked();
            String proxyUsername = editTextProxyUsername.getText().toString().trim();
            String proxyPassword = editTextProxyPassword.getText().trim();
            if (proxyAuthenticator && TextUtils.isEmpty(proxyUsername) && TextUtils.isEmpty(proxyPassword)) {
                ToastUtils.delayedShow(R.string.invalid_username_or_password, 3000);
                return;
            }

            String method = radioGroupTelegramMethod.getCheckedRadioButtonId() == R.id.radioTelegramMethodGet ? "GET" : "POST";
            TelegramSettingVo telegramSettingVoNew = new TelegramSettingVo(apiToken, chatId, proxyTypeId, proxyHost, proxyPort, proxyAuthenticator, proxyUsername, proxyPassword, method);

            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_TELEGRAM);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(telegramSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_TELEGRAM);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(telegramSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String apiToken = editTextTelegramApiToken.getText().trim();
            String chatId = editTextTelegramChatId.getText().toString().trim();
            if (apiToken.isEmpty() || chatId.isEmpty()) {
                ToastUtils.delayedShow(R.string.invalid_apiToken_or_chatId, 3000);
                return;
            }

            int proxyTypeId = radioGroupProxyType.getCheckedRadioButtonId();
            String proxyHost = editTextProxyHost.getText().toString().trim();
            String proxyPort = editTextProxyPort.getText().toString().trim();
            if (proxyTypeId != R.id.btnProxyNone && (TextUtils.isEmpty(proxyHost) || TextUtils.isEmpty(proxyPort))) {
                ToastUtils.delayedShow(R.string.invalid_host_or_port, 3000);
                return;
            }

            boolean proxyAuthenticator = switchProxyAuthenticator.isChecked();
            String proxyUsername = editTextProxyUsername.getText().toString().trim();
            String proxyPassword = editTextProxyPassword.getText().trim();
            if (proxyAuthenticator && TextUtils.isEmpty(proxyUsername) && TextUtils.isEmpty(proxyPassword)) {
                ToastUtils.delayedShow(R.string.invalid_username_or_password, 3000);
                return;
            }

            String method = radioGroupTelegramMethod.getCheckedRadioButtonId() == R.id.radioTelegramMethodGet ? "GET" : "POST";

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                TelegramSettingVo telegramSettingVoNew = new TelegramSettingVo(apiToken, chatId, proxyTypeId, proxyHost, proxyPort, proxyAuthenticator, proxyUsername, proxyPassword, method);
                SenderTelegramMsg.sendMsg(0, handler, null, telegramSettingVoNew, smsVo.getMobile(), smsVo.getSmsVoForSend(), telegramSettingVoNew.getMethod());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });
    }

    //短信
    private void setSms(final SenderModel senderModel, final boolean isClone) {
        if (!isClone) {
            XXPermissions.with(this)
                    // 接收短信
                    .permission(Permission.RECEIVE_SMS)
                    // 发送短信
                    .permission(Permission.SEND_SMS)
                    // 读取短信
                    .permission(Permission.READ_SMS)
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (!all) {
                                ToastUtils.show(R.string.toast_granted_part);
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                ToastUtils.show(R.string.toast_denied_never);
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(SenderActivity.this, permissions);
                            } else {
                                ToastUtils.show(R.string.toast_denied);
                            }
                        }
                    });
        }

        SmsSettingVo smsSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            Log.d(TAG, "jsonSettingStr = " + jsonSettingStr);
            if (jsonSettingStr != null) {
                smsSettingVo = JSON.parseObject(jsonSettingStr, SmsSettingVo.class);
            }
        }

        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_sms, null);

        final EditText editTextSmsName = view1.findViewById(R.id.editTextSmsName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchSmsEnable = view1.findViewById(R.id.switchSmsEnable);
        if (senderModel != null) {
            editTextSmsName.setText(senderModel.getName());
            switchSmsEnable.setChecked(senderModel.getStatusChecked());
        }

        final RadioGroup radioGroupSmsSimSlot = view1.findViewById(R.id.radioGroupSmsSimSlot);
        final EditText editTextSmsMobiles = view1.findViewById(R.id.editTextSmsMobiles);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchSmsOnlyNoNetwork = view1.findViewById(R.id.switchSmsOnlyNoNetwork);
        if (smsSettingVo != null) {
            radioGroupSmsSimSlot.check(smsSettingVo.getSmsSimSlotCheckId());
            editTextSmsMobiles.setText(smsSettingVo.getMobiles());
            switchSmsOnlyNoNetwork.setChecked(smsSettingVo.getOnlyNoNetwork());
        }

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setsmstitle)
                .setIcon(R.mipmap.sms)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonOk.setOnClickListener(view -> {
            String senderName = editTextSmsName.getText().toString().trim();
            int senderStatus = switchSmsEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            Boolean onlyNoNetwork = switchSmsOnlyNoNetwork.isChecked();
            String mobiles = editTextSmsMobiles.getText().toString().trim();
            if (TextUtils.isEmpty(mobiles)) {
                ToastUtils.delayedShow(R.string.invalid_phone_num, 3000);
                return;
            }

            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_SMS);
                newSenderModel.setStatus(senderStatus);
                SmsSettingVo smsSettingVoNew = new SmsSettingVo(
                        newSenderModel.getSmsSimSlotId(radioGroupSmsSimSlot.getCheckedRadioButtonId()),
                        mobiles,
                        onlyNoNetwork
                );
                newSenderModel.setJsonSetting(JSON.toJSONString(smsSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_SMS);
                senderModel.setStatus(senderStatus);
                SmsSettingVo smsSettingVoNew = new SmsSettingVo(
                        senderModel.getSmsSimSlotId(radioGroupSmsSimSlot.getCheckedRadioButtonId()),
                        mobiles,
                        onlyNoNetwork
                );
                senderModel.setJsonSetting(JSON.toJSONString(smsSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            int simSlot = R.id.btnSmsSimSlot2 == radioGroupSmsSimSlot.getCheckedRadioButtonId() ? 1 : 0;
            Boolean onlyNoNetwork = switchSmsOnlyNoNetwork.isChecked();
            String mobiles = editTextSmsMobiles.getText().toString().trim();
            if (TextUtils.isEmpty(mobiles)) {
                ToastUtils.delayedShow(R.string.invalid_phone_num, 3000);
                return;
            }

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderSmsMsg.sendMsg(0, handler, simSlot, mobiles, onlyNoNetwork, smsVo.getMobile(), smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });
    }

    //飞书机器人
    @SuppressLint("SimpleDateFormat")
    private void setFeiShu(final SenderModel senderModel, final boolean isClone) {
        FeiShuSettingVo feiShuSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                feiShuSettingVo = JSON.parseObject(jsonSettingStr, FeiShuSettingVo.class);
            }
        }
        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_feishu, null);

        final EditText editTextFeishuName = view1.findViewById(R.id.editTextFeishuName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchFeishuEnable = view1.findViewById(R.id.switchFeishuEnable);
        if (senderModel != null) {
            editTextFeishuName.setText(senderModel.getName());
            switchFeishuEnable.setChecked(senderModel.getStatusChecked());
        }

        final EditText editTextFeishuWebhook = view1.findViewById(R.id.editTextFeishuWebhook);
        final ClearEditText editTextFeishuSecret = view1.findViewById(R.id.editTextFeishuSecret);
        final RadioGroup radioGroupFeishuMsgType = view1.findViewById(R.id.radioGroupFeishuMsgType);
        final EditText editTextFeishuTitle = view1.findViewById(R.id.editTextFeishuTitle);
        final LinearLayout layoutTitleTemplate = view1.findViewById(R.id.layoutTitleTemplate);

        if (feiShuSettingVo != null) {
            editTextFeishuWebhook.setText(feiShuSettingVo.getWebhook());
            editTextFeishuSecret.setText(feiShuSettingVo.getSecret());
            radioGroupFeishuMsgType.check(feiShuSettingVo.getMsgTypeCheckId());
            editTextFeishuTitle.setText(feiShuSettingVo.getTitleTemplate());
            if ("text".equals(feiShuSettingVo.getMsgType())) {
                layoutTitleTemplate.setVisibility(View.GONE);
            } else {
                layoutTitleTemplate.setVisibility(View.VISIBLE);
            }
        }

        radioGroupFeishuMsgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (group != null && checkedId > 0) {
                layoutTitleTemplate.setVisibility(checkedId == R.id.radioFeishuMsgTypeText ? View.GONE : View.VISIBLE);
                group.check(checkedId);
            }
        });

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setfeishutitle)
                .setIcon(R.mipmap.feishu)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonOk.setOnClickListener(view -> {
            String senderName = editTextFeishuName.getText().toString().trim();
            int senderStatus = switchFeishuEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            String webHook = editTextFeishuWebhook.getText().toString().trim();
            String secret = editTextFeishuSecret.getText().trim();
            String msgType = radioGroupFeishuMsgType.getCheckedRadioButtonId() == R.id.radioFeishuMsgTypeText ? "text" : "interactive";
            String titleTemplate = editTextFeishuTitle.getText().toString().trim();
            if (!CommonUtil.checkUrl(webHook, false)) {
                ToastUtils.delayedShow(R.string.invalid_webhook, 3000);
                return;
            }

            if (TextUtils.isEmpty(titleTemplate)) titleTemplate = "【{{设备名称}}】来自{{来源号码}}的通知";

            FeiShuSettingVo feiShuSettingVoNew = new FeiShuSettingVo(webHook, secret, msgType, titleTemplate);
            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_FEISHU);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(feiShuSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_FEISHU);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(feiShuSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String webHook = editTextFeishuWebhook.getText().toString().trim();
            String secret = editTextFeishuSecret.getText().trim();
            String msgType = radioGroupFeishuMsgType.getCheckedRadioButtonId() == R.id.radioFeishuMsgTypeText ? "text" : "interactive";
            String titleTemplate = editTextFeishuTitle.getText().toString().trim();
            if (!CommonUtil.checkUrl(webHook, false)) {
                ToastUtils.delayedShow(R.string.invalid_webhook, 3000);
                return;
            }

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderFeishuMsg.sendMsg(0, handler, null, webHook, secret, msgType, smsVo.getMobile(), new Date(), smsVo.getTitleForSend(titleTemplate), smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });

        Button buttonInsertSender = view1.findViewById(R.id.bt_insert_sender);
        buttonInsertSender.setOnClickListener(view -> {
            editTextFeishuTitle.setFocusable(true);
            editTextFeishuTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextFeishuTitle, getString(R.string.tag_from));
        });

        Button buttonInsertExtra = view1.findViewById(R.id.bt_insert_extra);
        buttonInsertExtra.setOnClickListener(view -> {
            editTextFeishuTitle.setFocusable(true);
            editTextFeishuTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextFeishuTitle, getString(R.string.tag_card_slot));
        });

        Button buttonInsertTime = view1.findViewById(R.id.bt_insert_time);
        buttonInsertTime.setOnClickListener(view -> {
            editTextFeishuTitle.setFocusable(true);
            editTextFeishuTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextFeishuTitle, getString(R.string.tag_receive_time));
        });

        Button buttonInsertDeviceName = view1.findViewById(R.id.bt_insert_device_name);
        buttonInsertDeviceName.setOnClickListener(view -> {
            editTextFeishuTitle.setFocusable(true);
            editTextFeishuTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextFeishuTitle, getString(R.string.tag_device_name));
        });
    }

    //推送加
    @SuppressLint("SimpleDateFormat")
    private void setPushPlus(final SenderModel senderModel, final boolean isClone) {
        PushPlusSettingVo pushPlusSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                pushPlusSettingVo = JSON.parseObject(jsonSettingStr, PushPlusSettingVo.class);
            }
        }
        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_pushplus, null);

        final EditText editTextPushPlusName = view1.findViewById(R.id.editTextPushPlusName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchPushPlusEnable = view1.findViewById(R.id.switchPushPlusEnable);
        if (senderModel != null) {
            editTextPushPlusName.setText(senderModel.getName());
            switchPushPlusEnable.setChecked(senderModel.getStatusChecked());
        }

        final ClearEditText editTextPushPlusToken = view1.findViewById(R.id.editTextPushPlusToken);
        final EditText editTextPushPlusTopic = view1.findViewById(R.id.editTextPushPlusTopic);
        final EditText editTextPushPlusTemplate = view1.findViewById(R.id.editTextPushPlusTemplate);
        final EditText editTextPushPlusChannel = view1.findViewById(R.id.editTextPushPlusChannel);
        final EditText editTextPushPlusWebhook = view1.findViewById(R.id.editTextPushPlusWebhook);
        final EditText editTextPushPlusCallbackUrl = view1.findViewById(R.id.editTextPushPlusCallbackUrl);
        final EditText editTextPushPlusValidTime = view1.findViewById(R.id.editTextPushPlusValidTime);
        final EditText editTextPushPlusTitle = view1.findViewById(R.id.editTextPushPlusTitle);

        if (pushPlusSettingVo != null) {
            editTextPushPlusToken.setText(pushPlusSettingVo.getToken());
            editTextPushPlusTopic.setText(pushPlusSettingVo.getTopic());
            editTextPushPlusTemplate.setText(pushPlusSettingVo.getTemplate());
            editTextPushPlusChannel.setText(pushPlusSettingVo.getChannel());
            editTextPushPlusWebhook.setText(pushPlusSettingVo.getWebhook());
            editTextPushPlusCallbackUrl.setText(pushPlusSettingVo.getCallbackUrl());
            editTextPushPlusValidTime.setText(pushPlusSettingVo.getValidTime());
            editTextPushPlusTitle.setText(pushPlusSettingVo.getTitleTemplate());
        }

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setpushplustitle)
                .setIcon(R.mipmap.pushplus)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonOk.setOnClickListener(view -> {
            String senderName = editTextPushPlusName.getText().toString().trim();
            int senderStatus = switchPushPlusEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            PushPlusSettingVo pushPlusSettingVoNew = new PushPlusSettingVo(
                    editTextPushPlusToken.getText().trim(),
                    editTextPushPlusTopic.getText().toString().trim(),
                    editTextPushPlusTemplate.getText().toString().trim(),
                    editTextPushPlusChannel.getText().toString().trim(),
                    editTextPushPlusWebhook.getText().toString().trim(),
                    editTextPushPlusCallbackUrl.getText().toString().trim(),
                    editTextPushPlusValidTime.getText().toString().trim(),
                    editTextPushPlusTitle.getText().toString().trim()
            );
            if (TextUtils.isEmpty(pushPlusSettingVoNew.getToken())) {
                ToastUtils.delayedShow(R.string.invalid_token, 3000);
                return;
            }

            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_PUSHPLUS);
                newSenderModel.setStatus(senderStatus);

                newSenderModel.setJsonSetting(JSON.toJSONString(pushPlusSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_PUSHPLUS);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(pushPlusSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {

            String title = editTextPushPlusTitle.getText().toString().trim();
            if (title.isEmpty()) title = "SmsForwarder Title";

            PushPlusSettingVo pushPlusSettingVoNew = new PushPlusSettingVo(
                    editTextPushPlusToken.getText().trim(),
                    editTextPushPlusTopic.getText().toString().trim(),
                    editTextPushPlusTemplate.getText().toString().trim(),
                    editTextPushPlusChannel.getText().toString().trim(),
                    editTextPushPlusWebhook.getText().toString().trim(),
                    editTextPushPlusCallbackUrl.getText().toString().trim(),
                    editTextPushPlusValidTime.getText().toString().trim(),
                    title
            );

            if (TextUtils.isEmpty(pushPlusSettingVoNew.getToken())) {
                ToastUtils.delayedShow(R.string.invalid_token, 3000);
                return;
            }

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderPushPlusMsg.sendMsg(0, handler, null, pushPlusSettingVoNew, smsVo.getTitleForSend(title), smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }
        });

        Button buttonInsertSender = view1.findViewById(R.id.bt_insert_sender);
        buttonInsertSender.setOnClickListener(view -> {
            editTextPushPlusTitle.setFocusable(true);
            editTextPushPlusTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextPushPlusTitle, getString(R.string.tag_from));
        });

        Button buttonInsertExtra = view1.findViewById(R.id.bt_insert_extra);
        buttonInsertExtra.setOnClickListener(view -> {
            editTextPushPlusTitle.setFocusable(true);
            editTextPushPlusTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextPushPlusTitle, getString(R.string.tag_card_slot));
        });

        Button buttonInsertTime = view1.findViewById(R.id.bt_insert_time);
        buttonInsertTime.setOnClickListener(view -> {
            editTextPushPlusTitle.setFocusable(true);
            editTextPushPlusTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextPushPlusTitle, getString(R.string.tag_receive_time));
        });

        Button buttonInsertDeviceName = view1.findViewById(R.id.bt_insert_device_name);
        buttonInsertDeviceName.setOnClickListener(view -> {
            editTextPushPlusTitle.setFocusable(true);
            editTextPushPlusTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextPushPlusTitle, getString(R.string.tag_device_name));
        });
    }

    //Gotify
    @SuppressLint("SimpleDateFormat")
    private void setGotify(final SenderModel senderModel, final boolean isClone) {
        GotifySettingVo gotifySettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                gotifySettingVo = JSON.parseObject(jsonSettingStr, GotifySettingVo.class);
            }
        }

        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_gotify, null);

        final EditText editTextGotifyName = view1.findViewById(R.id.editTextGotifyName);
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchGotifyEnable = view1.findViewById(R.id.switchGotifyEnable);
        if (senderModel != null) {
            editTextGotifyName.setText(senderModel.getName());
            switchGotifyEnable.setChecked(senderModel.getStatusChecked());
        }

        final ClearEditText editTextGotifyWebServer = view1.findViewById(R.id.editTextGotifyWebServer);
        final EditText editTextGotifyTitle = view1.findViewById(R.id.editTextGotifyTitle);
        final EditText editTextGotifyPriority = view1.findViewById(R.id.editTextGotifyPriority);
        if (gotifySettingVo != null) {
            editTextGotifyWebServer.setText(gotifySettingVo.getWebServer());
            editTextGotifyTitle.setText(gotifySettingVo.getTitle());
            editTextGotifyPriority.setText(gotifySettingVo.getPriority());
        }

        Button buttonOk = view1.findViewById(R.id.buttonOk);
        Button buttonDel = view1.findViewById(R.id.buttonDel);
        Button buttonTest = view1.findViewById(R.id.buttonTest);
        alertDialog71
                .setTitle(R.string.setgotifytitle)
                .setIcon(R.mipmap.gotify)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonOk.setOnClickListener(view -> {
            String senderName = editTextGotifyName.getText().toString().trim();
            int senderStatus = switchGotifyEnable.isChecked() ? STATUS_ON : STATUS_OFF;
            if (TextUtils.isEmpty(senderName)) {
                ToastUtils.delayedShow(R.string.invalid_name, 3000);
                return;
            }

            String webServer = editTextGotifyWebServer.getText().trim();
            if (!CommonUtil.checkUrl(webServer, false)) {
                ToastUtils.delayedShow(R.string.invalid_webserver, 3000);
                return;
            }

            String title = editTextGotifyTitle.getText().toString().trim();
            if (title.isEmpty()) title = "SmsForwarder Title";

            String priority = editTextGotifyPriority.getText().toString().trim();

            GotifySettingVo gotifySettingVoNew = new GotifySettingVo(webServer, title, priority);

            if (isClone || senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(senderName);
                newSenderModel.setType(TYPE_GOTIFY);
                newSenderModel.setStatus(senderStatus);
                newSenderModel.setJsonSetting(JSON.toJSONString(gotifySettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(senderName);
                senderModel.setType(TYPE_GOTIFY);
                senderModel.setStatus(senderStatus);
                senderModel.setJsonSetting(JSON.toJSONString(gotifySettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonTest.setOnClickListener(view -> {
            String webServer = editTextGotifyWebServer.getText().trim();
            if (!CommonUtil.checkUrl(webServer, false)) {
                ToastUtils.delayedShow(R.string.invalid_webserver, 3000);
                return;
            }

            String title = editTextGotifyTitle.getText().toString().trim();
            if (title.isEmpty()) title = "SmsForwarder Title";

            String priority = editTextGotifyPriority.getText().toString().trim();

            GotifySettingVo gotifySettingVoNew = new GotifySettingVo(webServer, title, priority);

            try {
                SmsVo smsVo = new SmsVo(getString(R.string.test_phone_num), getString(R.string.test_sender_sms), new Date(), getString(R.string.test_sim_info));
                SenderGotifyMsg.sendMsg(0, handler, null, gotifySettingVoNew, smsVo.getTitleForSend(title), smsVo.getSmsVoForSend());
            } catch (Exception e) {
                ToastUtils.delayedShow(getString(R.string.failed_to_fwd) + e.getMessage(), 3000);
                e.printStackTrace();
            }

        });

        Button buttonInsertSender = view1.findViewById(R.id.bt_insert_sender);
        buttonInsertSender.setOnClickListener(view -> {
            editTextGotifyTitle.setFocusable(true);
            editTextGotifyTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextGotifyTitle, getString(R.string.tag_from));
        });

        Button buttonInsertExtra = view1.findViewById(R.id.bt_insert_extra);
        buttonInsertExtra.setOnClickListener(view -> {
            editTextGotifyTitle.setFocusable(true);
            editTextGotifyTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextGotifyTitle, getString(R.string.tag_card_slot));
        });

        Button buttonInsertTime = view1.findViewById(R.id.bt_insert_time);
        buttonInsertTime.setOnClickListener(view -> {
            editTextGotifyTitle.setFocusable(true);
            editTextGotifyTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextGotifyTitle, getString(R.string.tag_receive_time));
        });

        Button buttonInsertDeviceName = view1.findViewById(R.id.bt_insert_device_name);
        buttonInsertDeviceName.setOnClickListener(view -> {
            editTextGotifyTitle.setFocusable(true);
            editTextGotifyTitle.requestFocus();
            CommonUtil.insertOrReplaceText2Cursor(editTextGotifyTitle, getString(R.string.tag_device_name));
        });

    }

    //启用menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //menu点击事件
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.to_app_list:
                intent = new Intent(this, AppListActivity.class);
                break;
            case R.id.to_clone:
                intent = new Intent(this, CloneActivity.class);
                break;
            case R.id.to_about:
                intent = new Intent(this, AboutActivity.class);
                break;
            case R.id.to_help:
                Uri uri = Uri.parse("https://gitee.com/pp/SmsForwarder/wikis/pages");
                intent = new Intent(Intent.ACTION_VIEW, uri);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        startActivity(intent);
        return true;
    }

    //设置menu图标显示
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        Log.d(TAG, "onMenuOpened, featureId=" + featureId);
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "onMenuOpened", e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

}
