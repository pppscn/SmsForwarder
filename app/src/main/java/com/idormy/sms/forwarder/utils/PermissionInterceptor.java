package com.idormy.sms.forwarder.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AlertDialog;

import com.hjq.permissions.IPermissionInterceptor;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.ToastUtils;
import com.idormy.sms.forwarder.R;

import java.util.ArrayList;
import java.util.List;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/XXPermissions
 * time   : 2021/01/04
 * desc   : 权限申请拦截器
 */
@SuppressWarnings({"deprecation", "CommentedOutCode"})
public final class PermissionInterceptor implements IPermissionInterceptor {

//    @Override
//    public void requestPermissions(Activity activity, OnPermissionCallback callback, List<String> allPermissions) {
//        // 这里的 Dialog 只是示例，没有用 DialogFragment 来处理 Dialog 生命周期
//        new AlertDialog.Builder(activity)
//                .setTitle(R.string.common_permission_hint)
//                .setMessage(R.string.common_permission_message)
//                .setPositiveButton(R.string.common_permission_granted, new DialogInterface.OnClickListener() {
//
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                        PermissionFragment.beginRequest(activity, new ArrayList<>(allPermissions), PermissionInterceptor.this, callback);
//                    }
//                })
//                .setNegativeButton(R.string.common_permission_denied, new DialogInterface.OnClickListener() {
//
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .show();
//    }

    @Override
    public void grantedPermissions(Activity activity, List<String> allPermissions, List<String> grantedPermissions,
                                   boolean all, OnPermissionCallback callback) {
        if (callback != null) {
            callback.onGranted(grantedPermissions, all);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void deniedPermissions(Activity activity, List<String> allPermissions, List<String> deniedPermissions,
                                  boolean never, OnPermissionCallback callback) {
        if (callback != null) {
            callback.onDenied(deniedPermissions, never);
        }

        if (never) {
            showPermissionDialog(activity, deniedPermissions);
            return;
        }

        if (deniedPermissions.size() == 1 && Permission.ACCESS_BACKGROUND_LOCATION.equals(deniedPermissions.get(0))) {
            ToastUtils.show(R.string.common_permission_fail_4);
            return;
        }

        ToastUtils.show(R.string.common_permission_fail_1);

        if (callback == null) {
            return;
        }
        callback.onDenied(deniedPermissions, never);
    }

    /**
     * 显示授权对话框
     */
    protected void showPermissionDialog(Activity activity, List<String> permissions) {
        // 这里的 Dialog 只是示例，没有用 DialogFragment 来处理 Dialog 生命周期
        new AlertDialog.Builder(activity)
                .setTitle(R.string.common_permission_alert)
                .setCancelable(false)
                .setMessage(getPermissionHint(activity, permissions))
                .setPositiveButton(R.string.common_permission_goto, (dialog, which) -> {
                    dialog.dismiss();
                    XXPermissions.startPermissionActivity(activity, permissions);
                })
                .show();
    }

    /**
     * 根据权限获取提示
     */
    protected String getPermissionHint(Context context, List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return context.getString(R.string.common_permission_fail_2);
        }

        List<String> hints = new ArrayList<>();
        for (String permission : permissions) {
            switch (permission) {
                case Permission.READ_EXTERNAL_STORAGE:
                case Permission.WRITE_EXTERNAL_STORAGE:
                case Permission.MANAGE_EXTERNAL_STORAGE: {
                    String hint = context.getString(R.string.common_permission_storage);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.CAMERA: {
                    String hint = context.getString(R.string.common_permission_camera);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.RECORD_AUDIO: {
                    String hint = context.getString(R.string.common_permission_microphone);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.ACCESS_FINE_LOCATION:
                case Permission.ACCESS_COARSE_LOCATION:
                case Permission.ACCESS_BACKGROUND_LOCATION: {
                    String hint;
                    if (!permissions.contains(Permission.ACCESS_FINE_LOCATION) &&
                            !permissions.contains(Permission.ACCESS_COARSE_LOCATION)) {
                        hint = context.getString(R.string.common_permission_location_background);
                    } else {
                        hint = context.getString(R.string.common_permission_location);
                    }
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.BLUETOOTH_SCAN:
                case Permission.BLUETOOTH_CONNECT:
                case Permission.BLUETOOTH_ADVERTISE: {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        String hint = context.getString(R.string.common_permission_bluetooth);
                        if (!hints.contains(hint)) {
                            hints.add(hint);
                        }
                    }
                    break;
                }
                case Permission.READ_PHONE_STATE:
                case Permission.CALL_PHONE:
                case Permission.ADD_VOICEMAIL:
                case Permission.USE_SIP:
                case Permission.READ_PHONE_NUMBERS:
                case Permission.ANSWER_PHONE_CALLS: {
                    String hint = context.getString(R.string.common_permission_phone);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.GET_ACCOUNTS:
                case Permission.READ_CONTACTS:
                case Permission.WRITE_CONTACTS: {
                    String hint = context.getString(R.string.common_permission_contacts);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.READ_CALENDAR:
                case Permission.WRITE_CALENDAR: {
                    String hint = context.getString(R.string.common_permission_calendar);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.READ_CALL_LOG:
                case Permission.WRITE_CALL_LOG:
                case Permission.PROCESS_OUTGOING_CALLS: {
                    String hint = context.getString(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                            R.string.common_permission_call_log : R.string.common_permission_phone);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.BODY_SENSORS: {
                    String hint = context.getString(R.string.common_permission_sensors);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.ACTIVITY_RECOGNITION: {
                    String hint = context.getString(R.string.common_permission_activity_recognition);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.SEND_SMS:
                case Permission.RECEIVE_SMS:
                case Permission.READ_SMS:
                case Permission.RECEIVE_WAP_PUSH:
                case Permission.RECEIVE_MMS: {
                    String hint = context.getString(R.string.common_permission_sms);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.REQUEST_INSTALL_PACKAGES: {
                    String hint = context.getString(R.string.common_permission_install);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.SYSTEM_ALERT_WINDOW: {
                    String hint = context.getString(R.string.common_permission_window);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.WRITE_SETTINGS: {
                    String hint = context.getString(R.string.common_permission_setting);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.NOTIFICATION_SERVICE: {
                    String hint = context.getString(R.string.common_permission_notification);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case Permission.PACKAGE_USAGE_STATS: {
                    String hint = context.getString(R.string.common_permission_task);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        if (!hints.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String text : hints) {
                if (builder.length() == 0) {
                    builder.append(text);
                } else {
                    builder.append("、")
                            .append(text);
                }
            }
            builder.append(" ");
            return context.getString(R.string.common_permission_fail_3, builder.toString());
        }

        return context.getString(R.string.common_permission_fail_2);
    }
}