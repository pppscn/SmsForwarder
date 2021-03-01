package com.idormy.sms.forwarder;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.adapter.SenderAdapter;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.vo.BarkSettingVo;
import com.idormy.sms.forwarder.model.vo.DingDingSettingVo;
import com.idormy.sms.forwarder.model.vo.EmailSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXGroupRobotSettingVo;
import com.idormy.sms.forwarder.model.vo.WebNotifySettingVo;
import com.idormy.sms.forwarder.utils.SenderBarkMsg;
import com.idormy.sms.forwarder.utils.SenderDingdingMsg;
import com.idormy.sms.forwarder.utils.SenderMailMsg;
import com.idormy.sms.forwarder.utils.SenderQyWxGroupRobotMsg;
import com.idormy.sms.forwarder.utils.SenderUtil;
import com.idormy.sms.forwarder.utils.SenderWebNotifyMsg;
import com.umeng.analytics.MobclickAgent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.idormy.sms.forwarder.model.SenderModel.STATUS_ON;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_BARK;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_DINGDING;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_EMAIL;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_GROUP_ROBOT;
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

        // 先拿到数据并放在适配器上
        initSenders(); //初始化数据
        adapter = new SenderAdapter(SenderActivity.this, R.layout.sender_item, senderModels);

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
                    case TYPE_WEB_NOTIFY:
                        setWebNotify(senderModel);
                        break;
                    case TYPE_QYWX_GROUP_ROBOT:
                        setQYWXGroupRobot(senderModel);
                        break;
                    case TYPE_BARK:
                        setBark(senderModel);
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
                    case TYPE_WEB_NOTIFY:
                        setWebNotify(null);
                        break;
                    case TYPE_QYWX_GROUP_ROBOT:
                        setQYWXGroupRobot(null);
                        break;
                    case TYPE_BARK:
                        setBark(null);
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
        View view1 = View.inflate(SenderActivity.this, R.layout.activity_alter_dialog_setview_dingding, null);

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
                        SenderDingdingMsg.sendMsg(handler, token, secret, atMobiles, atAll, "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
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
        View view1 = View.inflate(SenderActivity.this, R.layout.activity_alter_dialog_setview_email, null);

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
                if (!host.isEmpty() && !port.isEmpty() && !fromemail.isEmpty() && !pwd.isEmpty() && !toemail.isEmpty()) {
                    try {
                        SenderMailMsg.sendEmail(handler, host, port, ssl, fromemail, pwd, toemail, "SmsForwarder Title", "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
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
        View view1 = View.inflate(SenderActivity.this, R.layout.activity_alter_dialog_setview_webnotify, null);

        final EditText editTextWebNotifyName = view1.findViewById(R.id.editTextWebNotifyName);
        if (senderModel != null) editTextWebNotifyName.setText(senderModel.getName());
        final EditText editTextWebNotifyToken = view1.findViewById(R.id.editTextWebNotifyToken);
        if (webNotifySettingVo != null)
            editTextWebNotifyToken.setText(webNotifySettingVo.getToken());
        final EditText editTextWebNotifySecret = view1.findViewById(R.id.editTextWebNotifySecret);
        if (webNotifySettingVo != null)
            editTextWebNotifySecret.setText(webNotifySettingVo.getSecret());

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
                            editTextWebNotifyToken.getText().toString(),
                            editTextWebNotifySecret.getText().toString()
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
                            editTextWebNotifyToken.getText().toString(),
                            editTextWebNotifySecret.getText().toString()
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
                String token = editTextWebNotifyToken.getText().toString();
                String secret = editTextWebNotifySecret.getText().toString();
                if (!token.isEmpty()) {
                    try {
                        SenderWebNotifyMsg.sendMsg(handler, token, secret, "SmsForwarder Title", "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
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
        View view1 = View.inflate(SenderActivity.this, R.layout.activity_alter_dialog_setview_qywxgrouprobot, null);

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
                        SenderQyWxGroupRobotMsg.sendMsg(handler, webHook, "SmsForwarder Title", "测试内容(content)@" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
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
        View view1 = View.inflate(SenderActivity.this, R.layout.activity_alter_dialog_setview_bark, null);

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
                        SenderBarkMsg.sendMsg(handler, barkServer, "19999999999", "【京东】验证码为387481（切勿将验证码告知他人），请在页面中输入完成验证，如有问题请点击 ihelp.jd.com 联系京东客服", "18888888888");
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
