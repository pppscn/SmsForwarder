package com.idormy.sms.forwarder;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
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
import com.idormy.sms.forwarder.model.vo.QYWXAppSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXGroupRobotSettingVo;
import com.idormy.sms.forwarder.model.vo.ServerChanSettingVo;
import com.idormy.sms.forwarder.model.vo.SmsSettingVo;
import com.idormy.sms.forwarder.model.vo.TelegramSettingVo;
import com.idormy.sms.forwarder.model.vo.WebNotifySettingVo;
import com.idormy.sms.forwarder.sender.SenderBarkMsg;
import com.idormy.sms.forwarder.sender.SenderDingdingMsg;
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

import static com.idormy.sms.forwarder.model.SenderModel.STATUS_ON;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_BARK;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_DINGDING;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_EMAIL;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_APP;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_GROUP_ROBOT;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SERVER_CHAN;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SMS;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_TELEGRAM;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_WEB_NOTIFY;

public class SenderActivity extends AppCompatActivity {

    public static final int NOTIFY = 0x9731993;
    private String TAG = "SenderActivity";
    // 用于存储数据
    private List<SenderModel> senderModels = new ArrayList<>();
    private SenderAdapter adapter;
    //消息处理者,创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY:
                    Toast.makeText(SenderActivity.this, msg.getData().getString("DATA"), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oncreate");
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
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
                    default:
                        Toast.makeText(SenderActivity.this, "异常的发送方类型，自动删除！", Toast.LENGTH_LONG).show();
                        if (senderModel != null) {
                            SenderUtil.delSender(senderModel.getId());
                            initSenders();
                            adapter.del(senderModels);
                        }
                        break;
                }

            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                //定义AlertDialog.Builder对象，当长按列表项的时候弹出确认删除对话框
                AlertDialog.Builder builder = new AlertDialog.Builder(SenderActivity.this);

                builder.setMessage("确定删除?");
                builder.setTitle("提示");

                //添加AlertDialog.Builder对象的setPositiveButton()方法
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SenderUtil.delSender(senderModels.get(position).getId());
                        initSenders();
                        adapter.del(senderModels);
                        Toast.makeText(getBaseContext(), "删除列表项", Toast.LENGTH_SHORT).show();
                    }
                });

                //添加AlertDialog.Builder对象的setNegativeButton()方法
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                builder.create().show();
                return true;
            }
        });
    }

    // 初始化数据
    private void initSenders() {
        senderModels = SenderUtil.getSender(null, null);
    }

    public void addSender(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SenderActivity.this);
        builder.setTitle("选择发送方类型");
        builder.setItems(R.array.add_sender_menu, new DialogInterface.OnClickListener() {//添加列表
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
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
                    default:
                        Toast.makeText(SenderActivity.this, "暂不支持这种转发！", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
        builder.show();
        Log.d(TAG, "setDingDing show" + senderModels.size());
    }

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
        if (dingDingSettingVo != null && dingDingSettingVo.getAtMobils() != null)
            editTextDingdingAtMobiles.setText(dingDingSettingVo.getAtMobils());
        final Switch switchDingdingAtAll = view1.findViewById(R.id.switchDingdingAtAll);
        if (dingDingSettingVo != null && dingDingSettingVo.getAtAll() != null)
            switchDingdingAtAll.setChecked(dingDingSettingVo.getAtAll());

        Button buttondingdingok = view1.findViewById(R.id.buttondingdingok);
        Button buttondingdingdel = view1.findViewById(R.id.buttondingdingdel);
        Button buttondingdingtest = view1.findViewById(R.id.buttondingdingtest);
        alertDialog71
                .setTitle(R.string.setdingdingtitle)
                .setIcon(R.mipmap.dingding)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttondingdingok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (senderModel == null) {
                    SenderModel newSenderModel = new SenderModel();
                    newSenderModel.setName(editTextDingdingName.getText().toString());
                    newSenderModel.setType(TYPE_DINGDING);
                    newSenderModel.setStatus(STATUS_ON);
                    DingDingSettingVo dingDingSettingVonew = new DingDingSettingVo(
                            editTextDingdingToken.getText().toString(),
                            editTextDingdingSecret.getText().toString(),
                            editTextDingdingAtMobiles.getText().toString(),
                            switchDingdingAtAll.isChecked());
                    newSenderModel.setJsonSetting(JSON.toJSONString(dingDingSettingVonew));
                    SenderUtil.addSender(newSenderModel);
                    initSenders();
                    adapter.add(senderModels);
//                    adapter.add(newSenderModel);
                } else {
                    senderModel.setName(editTextDingdingName.getText().toString());
                    senderModel.setType(TYPE_DINGDING);
                    senderModel.setStatus(STATUS_ON);
                    DingDingSettingVo dingDingSettingVonew = new DingDingSettingVo(
                            editTextDingdingToken.getText().toString(),
                            editTextDingdingSecret.getText().toString(),
                            editTextDingdingAtMobiles.getText().toString(),
                            switchDingdingAtAll.isChecked());
                    senderModel.setJsonSetting(JSON.toJSONString(dingDingSettingVonew));
                    SenderUtil.updateSender(senderModel);
                    initSenders();
                    adapter.update(senderModels);
//                    adapter.update(senderModel,position);
                }


                show.dismiss();


            }
        });
        buttondingdingdel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
//                    adapter.del(position);

                }
                show.dismiss();
            }
        });
        buttondingdingtest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String token = editTextDingdingToken.getText().toString();
                String secret = editTextDingdingSecret.getText().toString();
                String atMobiles = editTextDingdingAtMobiles.getText().toString();
                Boolean atAll = switchDingdingAtAll.isChecked();
                if (token != null && !token.isEmpty()) {
                    try {
                        SenderDingdingMsg.sendMsg(0, handler, token, secret, atMobiles, atAll, "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "token 不能为空", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

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

        final Switch switchEmailSSl = view1.findViewById(R.id.switchEmailSSl);
        if (emailSettingVo != null) switchEmailSSl.setChecked(emailSettingVo.getSsl());
        final EditText editTextEmailFromAdd = view1.findViewById(R.id.editTextEmailFromAdd);
        if (emailSettingVo != null) editTextEmailFromAdd.setText(emailSettingVo.getFromEmail());
        final EditText editTextEmailNickname = view1.findViewById(R.id.editTextEmailNickname);
        if (emailSettingVo != null) editTextEmailNickname.setText(emailSettingVo.getNickname());
        final EditText editTextEmailPsw = view1.findViewById(R.id.editTextEmailPsw);
        if (emailSettingVo != null) editTextEmailPsw.setText(emailSettingVo.getPwd());
        final EditText editTextEmailToAdd = view1.findViewById(R.id.editTextEmailToAdd);
        if (emailSettingVo != null) editTextEmailToAdd.setText(emailSettingVo.getToEmail());

        Button buttonemailok = view1.findViewById(R.id.buttonemailok);
        Button buttonemaildel = view1.findViewById(R.id.buttonemaildel);
        Button buttonemailtest = view1.findViewById(R.id.buttonemailtest);
        alertDialog71
                .setTitle(R.string.setemailtitle)
                .setIcon(R.mipmap.email)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonemailok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (senderModel == null) {
                    SenderModel newSenderModel = new SenderModel();
                    newSenderModel.setName(editTextEmailName.getText().toString());
                    newSenderModel.setType(TYPE_EMAIL);
                    newSenderModel.setStatus(STATUS_ON);
                    EmailSettingVo emailSettingVonew = new EmailSettingVo(
                            editTextEmailHost.getText().toString(),
                            editTextEmailPort.getText().toString(),
                            switchEmailSSl.isChecked(),
                            editTextEmailFromAdd.getText().toString(),
                            editTextEmailNickname.getText().toString(),
                            editTextEmailPsw.getText().toString(),
                            editTextEmailToAdd.getText().toString()
                    );
                    newSenderModel.setJsonSetting(JSON.toJSONString(emailSettingVonew));
                    SenderUtil.addSender(newSenderModel);
                    initSenders();
                    adapter.add(senderModels);
                } else {
                    senderModel.setName(editTextEmailName.getText().toString());
                    senderModel.setType(TYPE_EMAIL);
                    senderModel.setStatus(STATUS_ON);
                    EmailSettingVo emailSettingVonew = new EmailSettingVo(
                            editTextEmailHost.getText().toString(),
                            editTextEmailPort.getText().toString(),
                            switchEmailSSl.isChecked(),
                            editTextEmailFromAdd.getText().toString(),
                            editTextEmailNickname.getText().toString(),
                            editTextEmailPsw.getText().toString(),
                            editTextEmailToAdd.getText().toString()
                    );
                    senderModel.setJsonSetting(JSON.toJSONString(emailSettingVonew));
                    SenderUtil.updateSender(senderModel);
                    initSenders();
                    adapter.update(senderModels);
                }

                show.dismiss();


            }
        });
        buttonemaildel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                }
                show.dismiss();
            }
        });
        buttonemailtest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String host = editTextEmailHost.getText().toString();
                String port = editTextEmailPort.getText().toString();
                Boolean ssl = switchEmailSSl.isChecked();
                String fromemail = editTextEmailFromAdd.getText().toString();
                String pwd = editTextEmailPsw.getText().toString();
                String toemail = editTextEmailToAdd.getText().toString();

                String nickname = editTextEmailNickname.getText().toString();
                if (nickname == null || nickname.equals("")) {
                    nickname = "SmsForwarder";
                }

                if (!host.isEmpty() && !port.isEmpty() && !fromemail.isEmpty() && !pwd.isEmpty() && !toemail.isEmpty()) {
                    try {
                        SenderMailMsg.sendEmail(0, handler, host, port, ssl, fromemail, nickname, pwd, toemail, "SmsForwarder Title", "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "邮箱参数不完整", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

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

        Button buttonBarkOk = view1.findViewById(R.id.buttonBarkOk);
        Button buttonBarkDel = view1.findViewById(R.id.buttonBarkDel);
        Button buttonBarkTest = view1.findViewById(R.id.buttonBarkTest);
        alertDialog71
                .setTitle(R.string.setbarktitle)
                .setIcon(R.mipmap.bark)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonBarkOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (senderModel == null) {
                    SenderModel newSenderModel = new SenderModel();
                    newSenderModel.setName(editTextBarkName.getText().toString());
                    newSenderModel.setType(TYPE_BARK);
                    newSenderModel.setStatus(STATUS_ON);
                    BarkSettingVo barkSettingVoNew = new BarkSettingVo(
                            editTextBarkServer.getText().toString()
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
                            editTextBarkServer.getText().toString()
                    );
                    senderModel.setJsonSetting(JSON.toJSONString(barkSettingVoNew));
                    SenderUtil.updateSender(senderModel);
                    initSenders();
                    adapter.update(senderModels);
                }

                show.dismiss();

            }
        });
        buttonBarkDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                }
                show.dismiss();
            }
        });
        buttonBarkTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String barkServer = editTextBarkServer.getText().toString();
                if (!barkServer.isEmpty()) {
                    try {
                        SenderBarkMsg.sendMsg(0, handler, barkServer, "19999999999", "【京东】验证码为387481（切勿将验证码告知他人），请在页面中输入完成验证，如有问题请点击 ihelp.jd.com 联系京东客服");
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "bark-server 不能为空", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

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

        buttonServerChanOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (senderModel == null) {
                    SenderModel newSenderModel = new SenderModel();
                    newSenderModel.setName(editTextServerChanName.getText().toString());
                    newSenderModel.setType(TYPE_SERVER_CHAN);
                    newSenderModel.setStatus(STATUS_ON);
                    ServerChanSettingVo serverchanSettingVoNew = new ServerChanSettingVo(
                            editTextServerChanSendKey.getText().toString()
                    );
                    newSenderModel.setJsonSetting(JSON.toJSONString(serverchanSettingVoNew));
                    SenderUtil.addSender(newSenderModel);
                    initSenders();
                    adapter.add(senderModels);
                } else {
                    senderModel.setName(editTextServerChanName.getText().toString());
                    senderModel.setType(TYPE_SERVER_CHAN);
                    senderModel.setStatus(STATUS_ON);
                    ServerChanSettingVo serverchanSettingVoNew = new ServerChanSettingVo(
                            editTextServerChanSendKey.getText().toString()
                    );
                    senderModel.setJsonSetting(JSON.toJSONString(serverchanSettingVoNew));
                    SenderUtil.updateSender(senderModel);
                    initSenders();
                    adapter.update(senderModels);
                }

                show.dismiss();

            }
        });
        buttonServerChanDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                }
                show.dismiss();
            }
        });
        buttonServerChanTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String serverchanServer = editTextServerChanSendKey.getText().toString();
                if (!serverchanServer.isEmpty()) {
                    try {
                        SenderServerChanMsg.sendMsg(0, handler, serverchanServer, "19999999999", "【京东】验证码为387481（切勿将验证码告知他人），请在页面中输入完成验证，如有问题请点击 ihelp.jd.com 联系京东客服");
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "Server酱·Turbo版的 SendKey 不能为空", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

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
        final EditText editTextWebNotifySecret = view1.findViewById(R.id.editTextWebNotifySecret);
        if (webNotifySettingVo != null) editTextWebNotifySecret.setText(webNotifySettingVo.getSecret());
        final RadioGroup radioGroupWebNotifyMethod = (RadioGroup) view1.findViewById(R.id.radioGroupWebNotifyMethod);
        if (webNotifySettingVo != null) radioGroupWebNotifyMethod.check(webNotifySettingVo.getWebNotifyMethodCheckId());

        Button buttonbebnotifyok = view1.findViewById(R.id.buttonbebnotifyok);
        Button buttonbebnotifydel = view1.findViewById(R.id.buttonbebnotifydel);
        Button buttonbebnotifytest = view1.findViewById(R.id.buttonbebnotifytest);
        alertDialog71
                .setTitle(R.string.setwebnotifytitle)
                .setIcon(R.mipmap.webhook)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();

        buttonbebnotifyok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (senderModel == null) {
                    SenderModel newSenderModel = new SenderModel();
                    newSenderModel.setName(editTextWebNotifyName.getText().toString());
                    newSenderModel.setType(TYPE_WEB_NOTIFY);
                    newSenderModel.setStatus(STATUS_ON);
                    WebNotifySettingVo webNotifySettingVoNew = new WebNotifySettingVo(
                            editTextWebNotifyWebServer.getText().toString(),
                            editTextWebNotifySecret.getText().toString(),
                            (radioGroupWebNotifyMethod.getCheckedRadioButtonId() == R.id.radioWebNotifyMethodGet ? "GET" : "POST")
                    );
                    newSenderModel.setJsonSetting(JSON.toJSONString(webNotifySettingVoNew));
                    SenderUtil.addSender(newSenderModel);
                    initSenders();
                    adapter.add(senderModels);
                } else {
                    senderModel.setName(editTextWebNotifyName.getText().toString());
                    senderModel.setType(TYPE_WEB_NOTIFY);
                    senderModel.setStatus(STATUS_ON);
                    WebNotifySettingVo webNotifySettingVoNew = new WebNotifySettingVo(
                            editTextWebNotifyWebServer.getText().toString(),
                            editTextWebNotifySecret.getText().toString(),
                            (radioGroupWebNotifyMethod.getCheckedRadioButtonId() == R.id.radioWebNotifyMethodGet ? "GET" : "POST")
                    );
                    senderModel.setJsonSetting(JSON.toJSONString(webNotifySettingVoNew));
                    SenderUtil.updateSender(senderModel);
                    initSenders();
                    adapter.update(senderModels);
                }

                show.dismiss();

            }
        });
        buttonbebnotifydel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                }
                show.dismiss();
            }
        });
        buttonbebnotifytest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String webServer = editTextWebNotifyWebServer.getText().toString();
                String secret = editTextWebNotifySecret.getText().toString();
                String method = radioGroupWebNotifyMethod.getCheckedRadioButtonId() == R.id.radioWebNotifyMethodGet ? "GET" : "POST";
                if (!webServer.isEmpty()) {
                    try {
                        SenderWebNotifyMsg.sendMsg(0, handler, webServer, secret, method, "SmsForwarder Title", "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "WebServer 不能为空", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

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

        buttonQyWxGroupRobotOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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

            }
        });
        buttonQyWxGroupRobotDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                }
                show.dismiss();
            }
        });
        buttonQyWxGroupRobotTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String webHook = editTextQYWXGroupRobotWebHook.getText().toString();
                if (!webHook.isEmpty()) {
                    try {
                        SenderQyWxGroupRobotMsg.sendMsg(0, handler, webHook, "SmsForwarder Title", "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "webHook 不能为空", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //企业微信应用
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
        final Switch switchQYWXAppAtAll = view1.findViewById(R.id.switchQYWXAppAtAll);
        if (QYWXAppSettingVo != null) {
            editTextQYWXAppCorpID.setText(QYWXAppSettingVo.getCorpID());
            editTextQYWXAppAgentID.setText(QYWXAppSettingVo.getAgentID());
            editTextQYWXAppSecret.setText(QYWXAppSettingVo.getSecret());
            editTextQYWXAppToUser.setText(QYWXAppSettingVo.getToUser());
            switchQYWXAppAtAll.setChecked(QYWXAppSettingVo.getAtAll());
            linearLayoutQYWXAppToUser.setVisibility((Boolean) QYWXAppSettingVo.getAtAll() ? View.GONE : View.VISIBLE);
        }
        switchQYWXAppAtAll.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    linearLayoutQYWXAppToUser.setVisibility(View.GONE);
                    editTextQYWXAppToUser.setText("@all");
                } else {
                    linearLayoutQYWXAppToUser.setVisibility(View.VISIBLE);
                    editTextQYWXAppToUser.setText("");
                }
                Log.d(TAG, "onCheckedChanged:" + isChecked);
            }
        });

        Button buttonQYWXAppok = view1.findViewById(R.id.buttonQYWXAppOk);
        Button buttonQYWXAppdel = view1.findViewById(R.id.buttonQYWXAppDel);
        Button buttonQYWXApptest = view1.findViewById(R.id.buttonQYWXAppTest);
        alertDialog71
                .setTitle(R.string.setqywxapptitle)
                .setIcon(R.mipmap.qywxapp)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonQYWXAppok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String toUser = editTextQYWXAppToUser.getText().toString();
                if (toUser == null || toUser.isEmpty()) {
                    Toast.makeText(SenderActivity.this, "指定成员 不能为空 或者 选择@all", Toast.LENGTH_LONG).show();
                    editTextQYWXAppToUser.setFocusable(true);
                    editTextQYWXAppToUser.requestFocus();
                    return;
                }

                if (senderModel == null) {
                    SenderModel newSenderModel = new SenderModel();
                    newSenderModel.setName(editTextQYWXAppName.getText().toString());
                    newSenderModel.setType(TYPE_QYWX_APP);
                    newSenderModel.setStatus(STATUS_ON);
                    QYWXAppSettingVo QYWXAppSettingVonew = new QYWXAppSettingVo(
                            editTextQYWXAppCorpID.getText().toString(),
                            editTextQYWXAppAgentID.getText().toString(),
                            editTextQYWXAppSecret.getText().toString(),
                            editTextQYWXAppToUser.getText().toString(),
                            switchQYWXAppAtAll.isChecked());
                    newSenderModel.setJsonSetting(JSON.toJSONString(QYWXAppSettingVonew));
                    SenderUtil.addSender(newSenderModel);
                    initSenders();
                    adapter.add(senderModels);
                } else {
                    senderModel.setName(editTextQYWXAppName.getText().toString());
                    senderModel.setType(TYPE_QYWX_APP);
                    senderModel.setStatus(STATUS_ON);
                    QYWXAppSettingVo QYWXAppSettingVonew = new QYWXAppSettingVo(
                            editTextQYWXAppCorpID.getText().toString(),
                            editTextQYWXAppAgentID.getText().toString(),
                            editTextQYWXAppSecret.getText().toString(),
                            editTextQYWXAppToUser.getText().toString(),
                            switchQYWXAppAtAll.isChecked());
                    senderModel.setJsonSetting(JSON.toJSONString(QYWXAppSettingVonew));
                    SenderUtil.updateSender(senderModel);
                    initSenders();
                    adapter.update(senderModels);
                }

                show.dismiss();
            }
        });
        buttonQYWXAppdel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                }
                show.dismiss();
            }
        });
        buttonQYWXApptest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cropID = editTextQYWXAppCorpID.getText().toString();
                String agentID = editTextQYWXAppAgentID.getText().toString();
                String secret = editTextQYWXAppSecret.getText().toString();
                String toUser = editTextQYWXAppToUser.getText().toString();
                //Boolean atAll = switchQYWXAppAtAll.isChecked();
                if (toUser != null && !toUser.isEmpty()) {
                    try {
                        SenderQyWxAppMsg.sendMsg(0, handler, cropID, agentID, secret, toUser, "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())), true);
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "指定成员 不能为空 或者 选择@all", Toast.LENGTH_LONG).show();
                }
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

        buttonTelegramOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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

            }
        });
        buttonTelegramDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                }
                show.dismiss();
            }
        });
        buttonTelegramTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String apiToken = editTextTelegramApiToken.getText().toString();
                String chatId = editTextTelegramChatId.getText().toString();
                if (!apiToken.isEmpty() && !chatId.isEmpty()) {
                    try {
                        SenderTelegramMsg.sendMsg(0, handler, apiToken, chatId, "19999999999", "【京东】验证码为387481（切勿将验证码告知他人），请在页面中输入完成验证，如有问题请点击 ihelp.jd.com 联系京东客服");
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "机器人的ApiToken 和 被通知人的ChatId 都不能为空", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //Sms
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
        final RadioGroup radioGroupSmsSimSlot = (RadioGroup) view1.findViewById(R.id.radioGroupSmsSimSlot);
        if (smsSettingVo != null) radioGroupSmsSimSlot.check(smsSettingVo.getSmsSimSlotCheckId());
        final EditText editTextSmsMobiles = view1.findViewById(R.id.editTextSmsMobiles);
        if (smsSettingVo != null) editTextSmsMobiles.setText(smsSettingVo.getMobiles());
        final Switch switchSmsOnlyNoNetwork = view1.findViewById(R.id.switchSmsOnlyNoNetwork);
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

        buttonSmsOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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

            }
        });
        buttonSmsDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (senderModel != null) {
                    SenderUtil.delSender(senderModel.getId());
                    initSenders();
                    adapter.del(senderModels);
                }
                show.dismiss();
            }
        });
        buttonSmsTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int simSlot = 0;
                if (R.id.btnSmsSimSlot2 == radioGroupSmsSimSlot.getCheckedRadioButtonId()) {
                    simSlot = 1;
                }
                String mobiles = editTextSmsMobiles.getText().toString();
                Boolean onlyNoNetwork = switchSmsOnlyNoNetwork.isChecked();
                if (!mobiles.isEmpty() && !mobiles.isEmpty()) {
                    try {
                        SenderSmsMsg.sendMsg(0, handler, simSlot, mobiles, onlyNoNetwork, "19999999999", "【京东】验证码为387481（切勿将验证码告知他人），请在页面中输入完成验证，如有问题请点击 ihelp.jd.com 联系京东客服");
                    } catch (Exception e) {
                        Toast.makeText(SenderActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenderActivity.this, "接收手机号不能为空", Toast.LENGTH_LONG).show();
                }
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
