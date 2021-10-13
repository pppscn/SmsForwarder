package com.idormy.sms.forwarder;

import static com.idormy.sms.forwarder.model.SenderModel.STATUS_ON;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_BARK;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_DINGDING;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_EMAIL;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_FEISHU;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_APP;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_GROUP_ROBOT;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SERVER_CHAN;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SMS;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_TELEGRAM;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_WEB_NOTIFY;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.adapter.SenderAdapter;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.vo.BarkSettingVo;
import com.idormy.sms.forwarder.model.vo.DingDingSettingVo;
import com.idormy.sms.forwarder.model.vo.EmailSettingVo;
import com.idormy.sms.forwarder.model.vo.FeiShuSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXAppSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXGroupRobotSettingVo;
import com.idormy.sms.forwarder.model.vo.ServerChanSettingVo;
import com.idormy.sms.forwarder.model.vo.SmsSettingVo;
import com.idormy.sms.forwarder.model.vo.TelegramSettingVo;
import com.idormy.sms.forwarder.model.vo.WebNotifySettingVo;
import com.idormy.sms.forwarder.sender.SenderBarkMsg;
import com.idormy.sms.forwarder.sender.SenderDingdingMsg;
import com.idormy.sms.forwarder.sender.SenderFeishuMsg;
import com.idormy.sms.forwarder.sender.SenderMailMsg;
import com.idormy.sms.forwarder.sender.SenderQyWxAppMsg;
import com.idormy.sms.forwarder.sender.SenderQyWxGroupRobotMsg;
import com.idormy.sms.forwarder.sender.SenderServerChanMsg;
import com.idormy.sms.forwarder.sender.SenderSmsMsg;
import com.idormy.sms.forwarder.sender.SenderTelegramMsg;
import com.idormy.sms.forwarder.sender.SenderUtil;
import com.idormy.sms.forwarder.sender.SenderWebNotifyMsg;
import com.umeng.analytics.MobclickAgent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
                Toast.makeText(SenderActivity.this, msg.getData().getString("DATA"), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        SenderUtil.init(SenderActivity.this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        //是否关闭页面提示
        TextView help_tip = findViewById(R.id.help_tip);
        help_tip.setVisibility(MyApplication.showHelpTip ? View.VISIBLE : View.GONE);

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
                    setDingDing(senderModel);
                    break;
                case TYPE_EMAIL:
                    setEmail(senderModel);
                    break;
                case TYPE_BARK:
                    setBark(senderModel);
                    break;
                case TYPE_WEB_NOTIFY:
                    setWebNotify(senderModel);
                    break;
                case TYPE_QYWX_GROUP_ROBOT:
                    setQYWXGroupRobot(senderModel);
                    break;
                case TYPE_QYWX_APP:
                    setQYWXApp(senderModel);
                    break;
                case TYPE_SERVER_CHAN:
                    setServerChan(senderModel);
                    break;
                case TYPE_TELEGRAM:
                    setTelegram(senderModel);
                    break;
                case TYPE_SMS:
                    setSms(senderModel);
                    break;
                case TYPE_FEISHU:
                    setFeiShu(senderModel);
                    break;
                default:
                    Toast.makeText(SenderActivity.this, R.string.invalid_sender, Toast.LENGTH_LONG).show();
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
                Toast.makeText(getBaseContext(), R.string.delete_sender_toast, Toast.LENGTH_SHORT).show();
            });

            //添加AlertDialog.Builder对象的setNegativeButton()方法
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {

            });

            builder.create().show();
            return true;
        });
    }

    // 初始化数据
    private void initSenders() {
        senderModels = SenderUtil.getSender(null, null);
    }

    public void addSender(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SenderActivity.this);
        builder.setTitle(R.string.add_sender_title);
        //添加列表
        builder.setItems(R.array.add_sender_menu, (dialogInterface, which) -> {
            switch (which) {
                case TYPE_DINGDING:
                    setDingDing(null);
                    break;
                case TYPE_EMAIL:
                    setEmail(null);
                    break;
                case TYPE_BARK:
                    setBark(null);
                    break;
                case TYPE_WEB_NOTIFY:
                    setWebNotify(null);
                    break;
                case TYPE_QYWX_GROUP_ROBOT:
                    setQYWXGroupRobot(null);
                    break;
                case TYPE_QYWX_APP:
                    setQYWXApp(null);
                    break;
                case TYPE_SERVER_CHAN:
                    setServerChan(null);
                    break;
                case TYPE_TELEGRAM:
                    setTelegram(null);
                    break;
                case TYPE_SMS:
                    setSms(null);
                    break;
                case TYPE_FEISHU:
                    setFeiShu(null);
                    break;
                default:
                    Toast.makeText(SenderActivity.this, R.string.not_supported, Toast.LENGTH_LONG).show();
                    break;
            }
        });
        builder.show();
        Log.d(TAG, "setDingDing show" + senderModels.size());
    }

    //钉钉机器人
    @SuppressLint("SimpleDateFormat")
    private void setDingDing(final SenderModel senderModel) {
        DingDingSettingVo dingDingSettingVo = null;
        //try phrase json setting
        if (senderModel != null) {
            String jsonSettingStr = senderModel.getJsonSetting();
            if (jsonSettingStr != null) {
                dingDingSettingVo = JSON.parseObject(jsonSettingStr, DingDingSettingVo.class);
            }
        }
        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(SenderActivity.this);
        View view1 = View.inflate(SenderActivity.this, R.layout.alert_dialog_setview_dingding, null);

        final EditText editTextDingdingName = view1.findViewById(R.id.editTextDingdingName);
        if (senderModel != null)
            editTextDingdingName.setText(senderModel.getName());
        final EditText editTextDingdingToken = view1.findViewById(R.id.editTextDingdingToken);
        if (dingDingSettingVo != null)
            editTextDingdingToken.setText(dingDingSettingVo.getToken());
        final EditText editTextDingdingSecret = view1.findViewById(R.id.editTextDingdingSecret);
        if (dingDingSettingVo != null)
            editTextDingdingSecret.setText(dingDingSettingVo.getSecret());
        final EditText editTextDingdingAtMobiles = view1.findViewById(R.id.editTextDingdingAtMobiles);
        if (dingDingSettingVo != null && dingDingSettingVo.getAtMobiles() != null)
            editTextDingdingAtMobiles.setText(dingDingSettingVo.getAtMobiles());
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchDingdingAtAll = view1.findViewById(R.id.switchDingdingAtAll);
        if (dingDingSettingVo != null && dingDingSettingVo.getAtAll() != null)
            switchDingdingAtAll.setChecked(dingDingSettingVo.getAtAll());

        Button buttonDingdingOk = view1.findViewById(R.id.buttonDingdingOk);
        Button buttonDingdingDel = view1.findViewById(R.id.buttonDingdingDel);
        Button buttonDingdingTest = view1.findViewById(R.id.buttonDingdingTest);
        alertDialog71
                .setTitle(R.string.setdingdingtitle)
                .setIcon(R.mipmap.dingding)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonDingdingOk.setOnClickListener(view -> {

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextDingdingName.getText().toString());
                newSenderModel.setType(TYPE_DINGDING);
                newSenderModel.setStatus(STATUS_ON);
                DingDingSettingVo dingDingSettingVoNew = new DingDingSettingVo(
                        editTextDingdingToken.getText().toString(),
                        editTextDingdingSecret.getText().toString(),
                        editTextDingdingAtMobiles.getText().toString(),
                        switchDingdingAtAll.isChecked());
                newSenderModel.setJsonSetting(JSON.toJSONString(dingDingSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextDingdingName.getText().toString());
                senderModel.setType(TYPE_DINGDING);
                senderModel.setStatus(STATUS_ON);
                DingDingSettingVo dingDingSettingVoNew = new DingDingSettingVo(
                        editTextDingdingToken.getText().toString(),
                        editTextDingdingSecret.getText().toString(),
                        editTextDingdingAtMobiles.getText().toString(),
                        switchDingdingAtAll.isChecked());
                senderModel.setJsonSetting(JSON.toJSONString(dingDingSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }


            show.dismiss();


        });
        buttonDingdingDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonDingdingTest.setOnClickListener(view -> {
            String token = editTextDingdingToken.getText().toString();
            String secret = editTextDingdingSecret.getText().toString();
            String atMobiles = editTextDingdingAtMobiles.getText().toString();
            Boolean atAll = switchDingdingAtAll.isChecked();
            if (!token.isEmpty()) {
                try {
                    SenderDingdingMsg.sendMsg(0, handler, token, secret, atMobiles, atAll, R.string.test_content + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_token, Toast.LENGTH_LONG).show();
            }
        });
    }

    //邮箱
    @SuppressLint("SimpleDateFormat")
    private void setEmail(final SenderModel senderModel) {
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
        if (senderModel != null) editTextEmailName.setText(senderModel.getName());
        final EditText editTextEmailHost = view1.findViewById(R.id.editTextEmailHost);
        if (emailSettingVo != null) editTextEmailHost.setText(emailSettingVo.getHost());
        final EditText editTextEmailPort = view1.findViewById(R.id.editTextEmailPort);
        if (emailSettingVo != null) editTextEmailPort.setText(emailSettingVo.getPort());

        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchEmailSSl = view1.findViewById(R.id.switchEmailSSl);
        if (emailSettingVo != null) switchEmailSSl.setChecked(emailSettingVo.getSsl());
        final EditText editTextEmailFromAdd = view1.findViewById(R.id.editTextEmailFromAdd);
        if (emailSettingVo != null) editTextEmailFromAdd.setText(emailSettingVo.getFromEmail());
        final EditText editTextEmailNickname = view1.findViewById(R.id.editTextEmailNickname);
        if (emailSettingVo != null) editTextEmailNickname.setText(emailSettingVo.getNickname());
        final EditText editTextEmailPsw = view1.findViewById(R.id.editTextEmailPsw);
        if (emailSettingVo != null) editTextEmailPsw.setText(emailSettingVo.getPwd());
        final EditText editTextEmailToAdd = view1.findViewById(R.id.editTextEmailToAdd);
        if (emailSettingVo != null) editTextEmailToAdd.setText(emailSettingVo.getToEmail());

        Button buttonEmailOk = view1.findViewById(R.id.buttonEmailOk);
        Button buttonEmailDel = view1.findViewById(R.id.buttonEmailDel);
        Button buttonEmailTest = view1.findViewById(R.id.buttonEmailTest);
        alertDialog71
                .setTitle(R.string.setemailtitle)
                .setIcon(R.mipmap.email)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonEmailOk.setOnClickListener(view -> {

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextEmailName.getText().toString());
                newSenderModel.setType(TYPE_EMAIL);
                newSenderModel.setStatus(STATUS_ON);
                EmailSettingVo emailSettingVoNew = new EmailSettingVo(
                        editTextEmailHost.getText().toString(),
                        editTextEmailPort.getText().toString(),
                        switchEmailSSl.isChecked(),
                        editTextEmailFromAdd.getText().toString(),
                        editTextEmailNickname.getText().toString(),
                        editTextEmailPsw.getText().toString(),
                        editTextEmailToAdd.getText().toString()
                );
                newSenderModel.setJsonSetting(JSON.toJSONString(emailSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextEmailName.getText().toString());
                senderModel.setType(TYPE_EMAIL);
                senderModel.setStatus(STATUS_ON);
                EmailSettingVo emailSettingVoNew = new EmailSettingVo(
                        editTextEmailHost.getText().toString(),
                        editTextEmailPort.getText().toString(),
                        switchEmailSSl.isChecked(),
                        editTextEmailFromAdd.getText().toString(),
                        editTextEmailNickname.getText().toString(),
                        editTextEmailPsw.getText().toString(),
                        editTextEmailToAdd.getText().toString()
                );
                senderModel.setJsonSetting(JSON.toJSONString(emailSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }

            show.dismiss();


        });
        buttonEmailDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonEmailTest.setOnClickListener(view -> {
            String host = editTextEmailHost.getText().toString();
            String port = editTextEmailPort.getText().toString();
            boolean ssl = switchEmailSSl.isChecked();
            String fromEmail = editTextEmailFromAdd.getText().toString();
            String pwd = editTextEmailPsw.getText().toString();
            String toEmail = editTextEmailToAdd.getText().toString();

            String nickname = editTextEmailNickname.getText().toString();
            if (nickname.equals("")) {
                nickname = "SmsForwarder";
            }

            if (!host.isEmpty() && !port.isEmpty() && !fromEmail.isEmpty() && !pwd.isEmpty() && !toEmail.isEmpty()) {
                try {
                    SenderMailMsg.sendEmail(0, handler, host, port, ssl, fromEmail, nickname, pwd, toEmail, "SmsForwarder Title", R.string.test_content + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_email, Toast.LENGTH_LONG).show();
            }
        });
    }

    //Bark
    private void setBark(final SenderModel senderModel) {
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
        if (senderModel != null) editTextBarkName.setText(senderModel.getName());
        final EditText editTextBarkServer = view1.findViewById(R.id.editTextBarkServer);
        if (barkSettingVo != null) editTextBarkServer.setText(barkSettingVo.getServer());
        final EditText editTextBarkIcon = view1.findViewById(R.id.editTextBarkIcon);
        if (barkSettingVo != null) editTextBarkIcon.setText(barkSettingVo.getIcon());

        Button buttonBarkOk = view1.findViewById(R.id.buttonBarkOk);
        Button buttonBarkDel = view1.findViewById(R.id.buttonBarkDel);
        Button buttonBarkTest = view1.findViewById(R.id.buttonBarkTest);
        alertDialog71
                .setTitle(R.string.setbarktitle)
                .setIcon(R.mipmap.bark)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonBarkOk.setOnClickListener(view -> {

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextBarkName.getText().toString());
                newSenderModel.setType(TYPE_BARK);
                newSenderModel.setStatus(STATUS_ON);
                BarkSettingVo barkSettingVoNew = new BarkSettingVo(
                        editTextBarkServer.getText().toString(),
                        editTextBarkIcon.getText().toString()
                );
                newSenderModel.setJsonSetting(JSON.toJSONString(barkSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextBarkName.getText().toString());
                senderModel.setType(TYPE_BARK);
                senderModel.setStatus(STATUS_ON);
                BarkSettingVo barkSettingVoNew = new BarkSettingVo(
                        editTextBarkServer.getText().toString(),
                        editTextBarkIcon.getText().toString()
                );
                senderModel.setJsonSetting(JSON.toJSONString(barkSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }

            show.dismiss();

        });
        buttonBarkDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonBarkTest.setOnClickListener(view -> {
            String barkServer = editTextBarkServer.getText().toString();
            String barkIcon = editTextBarkIcon.getText().toString();
            if (!barkServer.isEmpty()) {
                try {
                    SenderBarkMsg.sendMsg(0, handler, barkServer, barkIcon, getString(R.string.test_phone_num), getString(R.string.test_sms), getString(R.string.test_group_name));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_bark_server, Toast.LENGTH_LONG).show();
            }
        });
    }

    //Server酱·Turbo版
    private void setServerChan(final SenderModel senderModel) {
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
        if (senderModel != null) editTextServerChanName.setText(senderModel.getName());
        final EditText editTextServerChanSendKey = view1.findViewById(R.id.editTextServerChanSendKey);
        if (serverchanSettingVo != null)
            editTextServerChanSendKey.setText(serverchanSettingVo.getSendKey());

        Button buttonServerChanOk = view1.findViewById(R.id.buttonServerChanOk);
        Button buttonServerChanDel = view1.findViewById(R.id.buttonServerChanDel);
        Button buttonServerChanTest = view1.findViewById(R.id.buttonServerChanTest);
        alertDialog71
                .setTitle(R.string.setserverchantitle)
                .setIcon(R.mipmap.serverchan)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonServerChanOk.setOnClickListener(view -> {

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextServerChanName.getText().toString());
                newSenderModel.setType(TYPE_SERVER_CHAN);
                newSenderModel.setStatus(STATUS_ON);
                ServerChanSettingVo serverChanSettingVoNew = new ServerChanSettingVo(
                        editTextServerChanSendKey.getText().toString()
                );
                newSenderModel.setJsonSetting(JSON.toJSONString(serverChanSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextServerChanName.getText().toString());
                senderModel.setType(TYPE_SERVER_CHAN);
                senderModel.setStatus(STATUS_ON);
                ServerChanSettingVo serverChanSettingVoNew = new ServerChanSettingVo(
                        editTextServerChanSendKey.getText().toString()
                );
                senderModel.setJsonSetting(JSON.toJSONString(serverChanSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }

            show.dismiss();

        });
        buttonServerChanDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonServerChanTest.setOnClickListener(view -> {
            String serverChanServer = editTextServerChanSendKey.getText().toString();
            if (!serverChanServer.isEmpty()) {
                try {
                    SenderServerChanMsg.sendMsg(0, handler, serverChanServer, getString(R.string.test_phone_num), getString(R.string.test_sms));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_sendkey, Toast.LENGTH_LONG).show();
            }
        });
    }

    //webhook
    @SuppressLint("SimpleDateFormat")
    private void setWebNotify(final SenderModel senderModel) {
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
        if (senderModel != null) editTextWebNotifyName.setText(senderModel.getName());
        final EditText editTextWebNotifyWebServer = view1.findViewById(R.id.editTextWebNotifyWebServer);
        if (webNotifySettingVo != null) editTextWebNotifyWebServer.setText(webNotifySettingVo.getWebServer());
        final EditText editTextWebNotifyWebParams = view1.findViewById(R.id.editTextWebNotifyWebParams);
        if (webNotifySettingVo != null) editTextWebNotifyWebParams.setText(webNotifySettingVo.getWebParams());
        final EditText editTextWebNotifySecret = view1.findViewById(R.id.editTextWebNotifySecret);
        if (webNotifySettingVo != null) editTextWebNotifySecret.setText(webNotifySettingVo.getSecret());
        final RadioGroup radioGroupWebNotifyMethod = view1.findViewById(R.id.radioGroupWebNotifyMethod);
        if (webNotifySettingVo != null) radioGroupWebNotifyMethod.check(webNotifySettingVo.getWebNotifyMethodCheckId());

        Button buttonWebNotifyOk = view1.findViewById(R.id.buttonWebNotifyOk);
        Button buttonWebNotifyDel = view1.findViewById(R.id.buttonWebNotifyDel);
        Button buttonWebNotifyTest = view1.findViewById(R.id.buttonWebNotifyTest);
        alertDialog71
                .setTitle(R.string.setwebnotifytitle)
                .setIcon(R.mipmap.webhook)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonWebNotifyOk.setOnClickListener(view -> {
            WebNotifySettingVo webNotifySettingVoNew = new WebNotifySettingVo(
                    editTextWebNotifyWebServer.getText().toString(),
                    editTextWebNotifySecret.getText().toString(),
                    (radioGroupWebNotifyMethod.getCheckedRadioButtonId() == R.id.radioWebNotifyMethodGet ? "GET" : "POST"),
                    editTextWebNotifyWebParams.getText().toString()
            );
            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextWebNotifyName.getText().toString());
                newSenderModel.setType(TYPE_WEB_NOTIFY);
                newSenderModel.setStatus(STATUS_ON);
                newSenderModel.setJsonSetting(JSON.toJSONString(webNotifySettingVoNew));
                SenderUtil.addSender(newSenderModel);
            } else {
                senderModel.setName(editTextWebNotifyName.getText().toString());
                senderModel.setType(TYPE_WEB_NOTIFY);
                senderModel.setStatus(STATUS_ON);
                senderModel.setJsonSetting(JSON.toJSONString(webNotifySettingVoNew));
                SenderUtil.updateSender(senderModel);
            }
            initSenders();
            adapter.update(senderModels);
            show.dismiss();
        });
        buttonWebNotifyDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonWebNotifyTest.setOnClickListener(view -> {
            String webServer = editTextWebNotifyWebServer.getText().toString();
            String webParams = editTextWebNotifyWebParams.getText().toString();
            String secret = editTextWebNotifySecret.getText().toString();
            String method = radioGroupWebNotifyMethod.getCheckedRadioButtonId() == R.id.radioWebNotifyMethodGet ? "GET" : "POST";
            if (!webServer.isEmpty()) {
                try {
                    SenderWebNotifyMsg.sendMsg(0, handler, webServer, webParams, secret, method, "SmsForwarder Title", R.string.test_content + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_webserver, Toast.LENGTH_LONG).show();
            }
        });
    }

    //企业微信群机器人
    @SuppressLint("SimpleDateFormat")
    private void setQYWXGroupRobot(final SenderModel senderModel) {
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
        if (senderModel != null) editTextQYWXGroupRobotName.setText(senderModel.getName());
        final EditText editTextQYWXGroupRobotWebHook = view1.findViewById(R.id.editTextQYWXGroupRobotWebHook);
        if (qywxGroupRobotSettingVo != null)
            editTextQYWXGroupRobotWebHook.setText(qywxGroupRobotSettingVo.getWebHook());

        Button buttonQyWxGroupRobotOk = view1.findViewById(R.id.buttonQyWxGroupRobotOk);
        Button buttonQyWxGroupRobotDel = view1.findViewById(R.id.buttonQyWxGroupRobotDel);
        Button buttonQyWxGroupRobotTest = view1.findViewById(R.id.buttonQyWxGroupRobotTest);
        alertDialog71
                .setTitle(R.string.setqywxgrouprobottitle)
                .setIcon(R.mipmap.qywx)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonQyWxGroupRobotOk.setOnClickListener(view -> {

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextQYWXGroupRobotName.getText().toString());
                newSenderModel.setType(TYPE_QYWX_GROUP_ROBOT);
                newSenderModel.setStatus(STATUS_ON);
                QYWXGroupRobotSettingVo qywxGroupRobotSettingVoNew = new QYWXGroupRobotSettingVo(
                        editTextQYWXGroupRobotWebHook.getText().toString()
                );
                newSenderModel.setJsonSetting(JSON.toJSONString(qywxGroupRobotSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextQYWXGroupRobotName.getText().toString());
                senderModel.setType(TYPE_QYWX_GROUP_ROBOT);
                senderModel.setStatus(STATUS_ON);
                QYWXGroupRobotSettingVo qywxGroupRobotSettingVoNew = new QYWXGroupRobotSettingVo(
                        editTextQYWXGroupRobotWebHook.getText().toString()
                );
                senderModel.setJsonSetting(JSON.toJSONString(qywxGroupRobotSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }

            show.dismiss();

        });
        buttonQyWxGroupRobotDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonQyWxGroupRobotTest.setOnClickListener(view -> {
            String webHook = editTextQYWXGroupRobotWebHook.getText().toString();
            if (!webHook.isEmpty()) {
                try {
                    SenderQyWxGroupRobotMsg.sendMsg(0, handler, webHook, "SmsForwarder Title", R.string.test_content + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_webhook, Toast.LENGTH_LONG).show();
            }
        });
    }

    //企业微信应用
    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void setQYWXApp(final SenderModel senderModel) {
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
        if (senderModel != null)
            editTextQYWXAppName.setText(senderModel.getName());
        final EditText editTextQYWXAppCorpID = view1.findViewById(R.id.editTextQYWXAppCorpID);
        final EditText editTextQYWXAppAgentID = view1.findViewById(R.id.editTextQYWXAppAgentID);
        final EditText editTextQYWXAppSecret = view1.findViewById(R.id.editTextQYWXAppSecret);
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

        Button buttonQYWXAppOk = view1.findViewById(R.id.buttonQYWXAppOk);
        Button buttonQYWXAppDel = view1.findViewById(R.id.buttonQYWXAppDel);
        Button buttonQYWXAppTest = view1.findViewById(R.id.buttonQYWXAppTest);
        alertDialog71
                .setTitle(R.string.setqywxapptitle)
                .setIcon(R.mipmap.qywxapp)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonQYWXAppOk.setOnClickListener(view -> {
            String toUser = editTextQYWXAppToUser.getText().toString();
            if (toUser.isEmpty()) {
                Toast.makeText(SenderActivity.this, R.string.invalid_at_mobiles, Toast.LENGTH_LONG).show();
                editTextQYWXAppToUser.setFocusable(true);
                editTextQYWXAppToUser.requestFocus();
                return;
            }

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextQYWXAppName.getText().toString());
                newSenderModel.setType(TYPE_QYWX_APP);
                newSenderModel.setStatus(STATUS_ON);
                QYWXAppSettingVo QYWXAppSettingVoNew = new QYWXAppSettingVo(
                        editTextQYWXAppCorpID.getText().toString(),
                        editTextQYWXAppAgentID.getText().toString(),
                        editTextQYWXAppSecret.getText().toString(),
                        editTextQYWXAppToUser.getText().toString(),
                        switchQYWXAppAtAll.isChecked());
                newSenderModel.setJsonSetting(JSON.toJSONString(QYWXAppSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextQYWXAppName.getText().toString());
                senderModel.setType(TYPE_QYWX_APP);
                senderModel.setStatus(STATUS_ON);
                QYWXAppSettingVo QYWXAppSettingVoNew = new QYWXAppSettingVo(
                        editTextQYWXAppCorpID.getText().toString(),
                        editTextQYWXAppAgentID.getText().toString(),
                        editTextQYWXAppSecret.getText().toString(),
                        editTextQYWXAppToUser.getText().toString(),
                        switchQYWXAppAtAll.isChecked());
                senderModel.setJsonSetting(JSON.toJSONString(QYWXAppSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }

            show.dismiss();
        });
        buttonQYWXAppDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonQYWXAppTest.setOnClickListener(view -> {
            String cropID = editTextQYWXAppCorpID.getText().toString();
            String agentID = editTextQYWXAppAgentID.getText().toString();
            String secret = editTextQYWXAppSecret.getText().toString();
            String toUser = editTextQYWXAppToUser.getText().toString();
            //Boolean atAll = switchQYWXAppAtAll.isChecked();
            if (!toUser.isEmpty()) {
                try {
                    SenderQyWxAppMsg.sendMsg(0, handler, cropID, agentID, secret, toUser, R.string.test_content + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())), true);
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_at_mobiles, Toast.LENGTH_LONG).show();
            }
        });
    }

    //Telegram机器人
    private void setTelegram(final SenderModel senderModel) {
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
        if (senderModel != null) editTextTelegramName.setText(senderModel.getName());
        final EditText editTextTelegramApiToken = view1.findViewById(R.id.editTextTelegramApiToken);
        if (telegramSettingVo != null)
            editTextTelegramApiToken.setText(telegramSettingVo.getApiToken());
        final EditText editTextTelegramChatId = view1.findViewById(R.id.editTextTelegramChatId);
        if (telegramSettingVo != null)
            editTextTelegramChatId.setText(telegramSettingVo.getChatId());

        Button buttonTelegramOk = view1.findViewById(R.id.buttonTelegramOk);
        Button buttonTelegramDel = view1.findViewById(R.id.buttonTelegramDel);
        Button buttonTelegramTest = view1.findViewById(R.id.buttonTelegramTest);
        alertDialog71
                .setTitle(R.string.settelegramtitle)
                .setIcon(R.mipmap.telegram)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonTelegramOk.setOnClickListener(view -> {

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextTelegramName.getText().toString());
                newSenderModel.setType(TYPE_TELEGRAM);
                newSenderModel.setStatus(STATUS_ON);
                TelegramSettingVo telegramSettingVoNew = new TelegramSettingVo(
                        editTextTelegramApiToken.getText().toString(),
                        editTextTelegramChatId.getText().toString()
                );
                newSenderModel.setJsonSetting(JSON.toJSONString(telegramSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextTelegramName.getText().toString());
                senderModel.setType(TYPE_TELEGRAM);
                senderModel.setStatus(STATUS_ON);
                TelegramSettingVo telegramSettingVoNew = new TelegramSettingVo(
                        editTextTelegramApiToken.getText().toString(),
                        editTextTelegramChatId.getText().toString()
                );
                senderModel.setJsonSetting(JSON.toJSONString(telegramSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }

            show.dismiss();

        });
        buttonTelegramDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonTelegramTest.setOnClickListener(view -> {
            String apiToken = editTextTelegramApiToken.getText().toString();
            String chatId = editTextTelegramChatId.getText().toString();
            if (!apiToken.isEmpty() && !chatId.isEmpty()) {
                try {
                    SenderTelegramMsg.sendMsg(0, handler, apiToken, chatId, getString(R.string.test_phone_num), getString(R.string.test_sms));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_apiToken_or_chatId, Toast.LENGTH_LONG).show();
            }
        });
    }

    //短信
    private void setSms(final SenderModel senderModel) {
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
        if (senderModel != null) editTextSmsName.setText(senderModel.getName());
        final RadioGroup radioGroupSmsSimSlot = view1.findViewById(R.id.radioGroupSmsSimSlot);
        if (smsSettingVo != null) radioGroupSmsSimSlot.check(smsSettingVo.getSmsSimSlotCheckId());
        final EditText editTextSmsMobiles = view1.findViewById(R.id.editTextSmsMobiles);
        if (smsSettingVo != null) editTextSmsMobiles.setText(smsSettingVo.getMobiles());
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch switchSmsOnlyNoNetwork = view1.findViewById(R.id.switchSmsOnlyNoNetwork);
        if (smsSettingVo != null) switchSmsOnlyNoNetwork.setChecked(smsSettingVo.getOnlyNoNetwork());

        Button buttonSmsOk = view1.findViewById(R.id.buttonSmsOk);
        Button buttonSmsDel = view1.findViewById(R.id.buttonSmsDel);
        Button buttonSmsTest = view1.findViewById(R.id.buttonSmsTest);
        alertDialog71
                .setTitle(R.string.setsmstitle)
                .setIcon(R.mipmap.sms)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonSmsOk.setOnClickListener(view -> {

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextSmsName.getText().toString());
                newSenderModel.setType(TYPE_SMS);
                newSenderModel.setStatus(STATUS_ON);
                SmsSettingVo smsSettingVoNew = new SmsSettingVo(
                        newSenderModel.getSmsSimSlotId(radioGroupSmsSimSlot.getCheckedRadioButtonId()),
                        editTextSmsMobiles.getText().toString(),
                        switchSmsOnlyNoNetwork.isChecked()
                );
                newSenderModel.setJsonSetting(JSON.toJSONString(smsSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextSmsName.getText().toString());
                senderModel.setType(TYPE_SMS);
                senderModel.setStatus(STATUS_ON);
                SmsSettingVo smsSettingVoNew = new SmsSettingVo(
                        senderModel.getSmsSimSlotId(radioGroupSmsSimSlot.getCheckedRadioButtonId()),
                        editTextSmsMobiles.getText().toString(),
                        switchSmsOnlyNoNetwork.isChecked()
                );
                senderModel.setJsonSetting(JSON.toJSONString(smsSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }

            show.dismiss();

        });
        buttonSmsDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });
        buttonSmsTest.setOnClickListener(view -> {
            int simSlot = 0;
            if (R.id.btnSmsSimSlot2 == radioGroupSmsSimSlot.getCheckedRadioButtonId()) {
                simSlot = 1;
            }
            String mobiles = editTextSmsMobiles.getText().toString();
            Boolean onlyNoNetwork = switchSmsOnlyNoNetwork.isChecked();
            if (!mobiles.isEmpty()) {
                try {
                    SenderSmsMsg.sendMsg(0, handler, simSlot, mobiles, onlyNoNetwork, getString(R.string.test_phone_num), getString(R.string.test_sms));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_phone_num, Toast.LENGTH_LONG).show();
            }
        });
    }

    //飞书机器人
    @SuppressLint("SimpleDateFormat")
    private void setFeiShu(final SenderModel senderModel) {
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
        if (senderModel != null)
            editTextFeishuName.setText(senderModel.getName());
        final EditText editTextFeishuWebhook = view1.findViewById(R.id.editTextFeishuWebhook);
        if (feiShuSettingVo != null)
            editTextFeishuWebhook.setText(feiShuSettingVo.getWebhook());
        final EditText editTextFeishuSecret = view1.findViewById(R.id.editTextFeishuSecret);
        if (feiShuSettingVo != null)
            editTextFeishuSecret.setText(feiShuSettingVo.getSecret());

        Button buttonFeishuOk = view1.findViewById(R.id.buttonFeishuOk);
        Button buttonFeishuDel = view1.findViewById(R.id.buttonFeishuDel);
        Button buttonFeishuTest = view1.findViewById(R.id.buttonFeishuTest);
        alertDialog71
                .setTitle(R.string.setfeishutitle)
                .setIcon(R.mipmap.feishu)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonFeishuOk.setOnClickListener(view -> {

            if (senderModel == null) {
                SenderModel newSenderModel = new SenderModel();
                newSenderModel.setName(editTextFeishuName.getText().toString());
                newSenderModel.setType(TYPE_FEISHU);
                newSenderModel.setStatus(STATUS_ON);
                FeiShuSettingVo feiShuSettingVoNew = new FeiShuSettingVo(
                        editTextFeishuWebhook.getText().toString(),
                        editTextFeishuSecret.getText().toString());
                newSenderModel.setJsonSetting(JSON.toJSONString(feiShuSettingVoNew));
                SenderUtil.addSender(newSenderModel);
                initSenders();
                adapter.add(senderModels);
            } else {
                senderModel.setName(editTextFeishuName.getText().toString());
                senderModel.setType(TYPE_FEISHU);
                senderModel.setStatus(STATUS_ON);
                FeiShuSettingVo feiShuSettingVoNew = new FeiShuSettingVo(
                        editTextFeishuWebhook.getText().toString(),
                        editTextFeishuSecret.getText().toString());
                senderModel.setJsonSetting(JSON.toJSONString(feiShuSettingVoNew));
                SenderUtil.updateSender(senderModel);
                initSenders();
                adapter.update(senderModels);
            }
            show.dismiss();
        });

        buttonFeishuDel.setOnClickListener(view -> {
            if (senderModel != null) {
                SenderUtil.delSender(senderModel.getId());
                initSenders();
                adapter.del(senderModels);
            }
            show.dismiss();
        });

        buttonFeishuTest.setOnClickListener(view -> {
            String token = editTextFeishuWebhook.getText().toString();
            String secret = editTextFeishuSecret.getText().toString();
            if (!token.isEmpty()) {
                try {
                    SenderFeishuMsg.sendMsg(0, handler, token, secret, getString(R.string.test_content) + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                } catch (Exception e) {
                    Toast.makeText(SenderActivity.this, getString(R.string.failed_to_fwd) + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SenderActivity.this, R.string.invalid_webhook, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}
