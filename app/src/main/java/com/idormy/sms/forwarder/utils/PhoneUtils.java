package com.idormy.sms.forwarder.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"deprecation", "rawtypes", "unchecked", "CommentedOutCode", "SynchronizeOnNonFinalField", "unused", "SameReturnValue"})
public class PhoneUtils {
    static Boolean hasInit = false;
    @SuppressLint("StaticFieldLeak")
    static Context context;
    private static final String TAG = "PhoneUtils";

    /**
     * 构造类
     */
    private PhoneUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static void init(Context context1) {
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
        }
    }

    /**
     * 判断设备是否是手机
     *
     * @return {@code true}: 是<br>{@code false}: 否
     */
    public static boolean isPhone() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null && tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    /**
     * 判断设备是否root
     *
     * @return the boolean{@code true}: 是<br>{@code false}: 否
     */
    public static boolean isDeviceRoot() {
        String su = "su";
        String[] locations = {"/system/bin/", "/system/xbin/", "/sbin/", "/system/sd/xbin/", "/system/bin/failsafe/",
                "/data/local/xbin/", "/data/local/bin/", "/data/local/"};
        for (String location : locations) {
            if (new File(location + su).exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取设备系统版本号
     *
     * @return 设备系统版本号
     */
    public static int getSDKVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    /**
     * 获取设备AndroidID
     *
     * @return AndroidID
     */
    @SuppressLint("HardwareIds")
    public static String getAndroidID() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * 获取IMEI码
     * <p>需添加权限 {@code <uses-permission android:name="android.permission.READ_PHONE_STATE"/>}</p>
     *
     * @return IMEI码
     */
    @SuppressLint("HardwareIds")
    public static String getIMEI() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            return tm != null ? tm.getDeviceId() : null;
        } catch (Exception ignored) {

        }
        return getUniquePseudoID();
    }

    /**
     * 通过读取设备的ROM版本号、厂商名、CPU型号和其他硬件信息来组合出一串15位的号码
     * 其中“Build.SERIAL”这个属性来保证ID的独一无二，当API < 9 无法读取时，使用AndroidId
     *
     * @return 伪唯一ID
     */
    public static String getUniquePseudoID() {
        String m_szDevIDShort = "35" +
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10;

        String serial;
        try {
            serial = Objects.requireNonNull(Build.class.getField("SERIAL").get(null)).toString();
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception e) {
            //获取失败，使用AndroidId
            serial = getAndroidID();
            if (TextUtils.isEmpty(serial)) {
                serial = "serial";
            }
        }

        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    /**
     * 获取IMSI码
     * <p>需添加权限 {@code <uses-permission android:name="android.permission.READ_PHONE_STATE"/>}</p>
     *
     * @return IMSI码
     */
    @SuppressLint("HardwareIds")
    public static String getIMSI() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            return tm != null ? tm.getSubscriberId() : null;
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 判断sim卡是否准备好
     *
     * @return {@code true}: 是<br>{@code false}: 否
     */
    public static boolean isSimCardReady() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null && tm.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    /**
     * 获取Sim卡运营商名称
     * <p>中国移动、如中国联通、中国电信</p>
     *
     * @return sim卡运营商名称
     */
    public static String getSimOperatorName() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null ? tm.getSimOperatorName() : null;
    }

    /**
     * 获取Sim卡运营商名称
     * <p>中国移动、如中国联通、中国电信</p>
     *
     * @return 移动网络运营商名称
     */
    public static String getSimOperatorByMnc() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String operator = tm != null ? tm.getSimOperator() : null;
        if (operator == null) {
            return null;
        }
        switch (operator) {
            case "46000":
            case "46002":
            case "46007":
                return "中国移动";
            case "46001":
                return "中国联通";
            case "46003":
                return "中国电信";
            default:
                return operator;
        }
    }

    /**
     * 获取Sim卡序列号
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     *
     * @return 序列号
     */
    public static String getSimSerialNumber() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("HardwareIds") String serialNumber = tm != null ? tm.getSimSerialNumber() : null;

            return serialNumber != null ? serialNumber : "";
        } catch (Exception ignored) {
        }

        return "";
    }

    /**
     * 获取Sim卡的国家代码
     *
     * @return 国家代码
     */
    public static String getSimCountryIso() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null ? tm.getSimCountryIso() : null;
    }

    /**
     * 读取电话号码
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     * OR
     * {@link android.Manifest.permission#READ_SMS}
     * <p>
     *
     * @return 电话号码
     */
    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getPhoneNumber() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            return tm != null ? tm.getLine1Number() : null;
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 获得卡槽数，默认为1
     *
     * @return 返回卡槽数
     */
    @SuppressLint("ObsoleteSdkInt")
    public static int getSimCount() {
        int count = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                SubscriptionManager mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (mSubscriptionManager != null) {
                    count = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
                    return count;
                }
            } catch (Exception ignored) {
            }
        }
        try {
            count = Integer.parseInt(getReflexMethod(context));
        } catch (MethodNotFoundException ignored) {
        }
        return count;
    }

    /**
     * 获取Sim卡使用的数量
     *
     * @return 0, 1, 2
     */
    @SuppressLint("ObsoleteSdkInt")
    public static int getSimUsedCount() {
        int count = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                SubscriptionManager mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return count;
                }
                count = mSubscriptionManager.getActiveSubscriptionInfoCount();
                return count;
            } catch (Exception ignored) {
            }
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getSimState() == TelephonyManager.SIM_STATE_READY) {
            count = 1;
        }
        try {
            if (Integer.parseInt(getReflexMethodWithId(context, "getSimState", "1")) == TelephonyManager.SIM_STATE_READY) {
                count = 2;
            }
        } catch (MethodNotFoundException ignored) {
        }
        return count;
    }

    /**
     * 获取多卡信息
     *
     * @return 多Sim卡的具体信息
     */
    @SuppressLint("ObsoleteSdkInt")
    public static List<SimInfo> getSimMultiInfo() {
        List<SimInfo> infos = new ArrayList<>();
        Log.d(TAG, "Build.VERSION.SDK_INT = " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Build.VERSION_CODES.LOLLIPOP_MR1 = " + Build.VERSION_CODES.LOLLIPOP_MR1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.d(TAG, "1.版本超过5.1，调用系统方法");
            //1.版本超过5.1，调用系统方法
            SubscriptionManager mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            List<SubscriptionInfo> activeSubscriptionInfoList = null;
            if (mSubscriptionManager != null) {
                try {
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE);
                    activeSubscriptionInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
                } catch (Exception ignored) {
                }
            }
            if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.size() > 0) {
                //1.1.1 有使用的卡，就遍历所有卡
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    SimInfo simInfo = new SimInfo();
                    simInfo.mCarrierName = subscriptionInfo.getCarrierName();
                    simInfo.mIccId = subscriptionInfo.getIccId();
                    simInfo.mSimSlotIndex = subscriptionInfo.getSimSlotIndex();
                    simInfo.mNumber = subscriptionInfo.getNumber();
                    simInfo.mCountryIso = subscriptionInfo.getCountryIso();
                    simInfo.mSubscriptionId = subscriptionInfo.getSubscriptionId();
                    try {
                        simInfo.mImei = getReflexMethodWithId(context, "getDeviceId", String.valueOf(simInfo.mSimSlotIndex));
                        simInfo.mImsi = getReflexMethodWithId(context, "getSubscriberId", String.valueOf(subscriptionInfo.getSubscriptionId()));
                    } catch (MethodNotFoundException ignored) {
                    }
                    Log.d(TAG, String.valueOf(simInfo));
                    infos.add(simInfo);
                }
            }
        } else {
            Log.d(TAG, "2.版本低于5.1的系统，首先调用数据库，看能不能访问到");
            //2.版本低于5.1的系统，首先调用数据库，看能不能访问到
            Uri uri = Uri.parse("content://telephony/siminfo"); //访问raw_contacts表
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(uri, new String[]{"_id", "icc_id", "sim_id", "display_name", "carrier_name", "name_source", "color", "number", "display_number_format", "data_roaming", "mcc", "mnc"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SimInfo simInfo = new SimInfo();
                    simInfo.mCarrierName = cursor.getString(cursor.getColumnIndex("carrier_name"));
                    simInfo.mIccId = cursor.getString(cursor.getColumnIndex("icc_id"));
                    simInfo.mSimSlotIndex = cursor.getInt(cursor.getColumnIndex("sim_id"));
                    simInfo.mNumber = cursor.getString(cursor.getColumnIndex("number"));
                    simInfo.mCountryIso = cursor.getString(cursor.getColumnIndex("mcc"));
                    String id = cursor.getString(cursor.getColumnIndex("_id"));

                    try {
                        simInfo.mImei = getReflexMethodWithId(context, "getDeviceId", String.valueOf(simInfo.mSimSlotIndex));
                        simInfo.mImsi = getReflexMethodWithId(context, "getSubscriberId", String.valueOf(id));
                    } catch (MethodNotFoundException ignored) {
                    }
                    Log.d(TAG, String.valueOf(simInfo));
                    infos.add(simInfo);
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        /*Log.d(TAG, "3.通过反射读取卡槽信息，最后通过IMEI去重");
        //3.通过反射读取卡槽信息，最后通过IMEI去重
        for (int i = 0; i < getSimCount(); i++) {
            infos.add(getReflexSimInfo(context, i));
        }
        List<SimInfo> simInfos = removeDuplicateWithOrder(infos);
        if (simInfos.size() < getSimCount()) {
            for (int i = simInfos.size(); i < getSimCount(); i++) {
                simInfos.add(new SimInfo());
            }
        }
        return simInfos;*/

        return infos;
    }

    @Nullable
    public static String getSecondIMSI() {
        int maxCount = 20;
        if (TextUtils.isEmpty(getIMSI())) {
            return null;
        }
        for (int i = 0; i < maxCount; i++) {
            String imsi = null;
            try {
                imsi = getReflexMethodWithId(context, "getSubscriberId", String.valueOf(i));
            } catch (MethodNotFoundException e) {
                Log.d(TAG, String.valueOf(e));
            }
            if (!TextUtils.isEmpty(imsi) && !Objects.equals(imsi, getIMSI())) {
                return imsi;
            }
        }
        return null;
    }

    /**
     * 通过反射获得SimInfo的信息
     * 当index为0时，读取默认信息
     *
     * @param index 位置,用来当subId和phoneId
     * @return {@link SimInfo} sim信息
     */
    @NonNull
    private static SimInfo getReflexSimInfo(Context context, int index) {
        SimInfo simInfo = new SimInfo();
        simInfo.mSimSlotIndex = index;
        try {
            simInfo.mImei = getReflexMethodWithId(context, "getDeviceId", String.valueOf(simInfo.mSimSlotIndex));
            //slotId,比较准确
            simInfo.mImsi = getReflexMethodWithId(context, "getSubscriberId", String.valueOf(simInfo.mSimSlotIndex));
            //subId,很不准确
            simInfo.mCarrierName = getReflexMethodWithId(context, "getSimOperatorNameForPhone", String.valueOf(simInfo.mSimSlotIndex));
            //PhoneId，基本准确
            simInfo.mCountryIso = getReflexMethodWithId(context, "getSimCountryIso", String.valueOf(simInfo.mSimSlotIndex));
            //subId，很不准确
            simInfo.mIccId = getReflexMethodWithId(context, "getSimSerialNumber", String.valueOf(simInfo.mSimSlotIndex));
            //subId，很不准确
            simInfo.mNumber = getReflexMethodWithId(context, "getLine1Number", String.valueOf(simInfo.mSimSlotIndex));
            //subId，很不准确
        } catch (MethodNotFoundException ignored) {
        }
        return simInfo;
    }

    /**
     * 通过反射调取@hide的方法
     *
     * @return 返回方法调用的结果
     * @throws MethodNotFoundException 方法没有找到
     */
    private static String getReflexMethod(Context context) throws MethodNotFoundException {
        String result = null;
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            Method getSimID = telephonyClass.getMethod("getPhoneCount");
            Object ob_phone = getSimID.invoke(telephony);
            if (ob_phone != null) {
                result = ob_phone.toString();
            }
        } catch (Exception e) {
            Log.d(TAG, String.valueOf(e.fillInStackTrace()));
            throw new MethodNotFoundException("getPhoneCount");
        }
        return result;
    }

    /**
     * 通过反射调取@hide的方法
     *
     * @param predictedMethodName 方法名
     * @param id                  参数
     * @return 返回方法调用的结果
     * @throws MethodNotFoundException 方法没有找到
     */
    private static String getReflexMethodWithId(Context context, String predictedMethodName, String id) throws MethodNotFoundException {
        String result = null;
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);
            Class<?>[] parameterTypes = getSimID.getParameterTypes();
            Object[] obParameter = new Object[parameterTypes.length];
            if (parameterTypes[0].getSimpleName().equals("int")) {
                obParameter[0] = Integer.valueOf(id);
            } else if (parameterTypes[0].getSimpleName().equals("long")) {
                obParameter[0] = Long.valueOf(id);
            } else {
                obParameter[0] = id;
            }
            Object ob_phone = getSimID.invoke(telephony, obParameter);
            if (ob_phone != null) {
                result = ob_phone.toString();
            }
        } catch (Exception e) {
            Log.d(TAG, String.valueOf(e.fillInStackTrace()));
            throw new MethodNotFoundException(predictedMethodName);
        }
        return result;
    }

    // 删除ArrayList中重复元素，保持顺序
    public static List removeDuplicateWithOrder(List list) {
        Set set = new HashSet();
        List newList = new ArrayList();
        for (Object element : list) {
            if (set.add(element))
                newList.add(element);
        }
        list.clear();
        list.addAll(newList);
        return list;
    }

    // 检查权限是否获取（android6.0及以上系统可能默认关闭权限，且没提示）
    @SuppressLint("InlinedApi")
    public static void CheckPermission(PackageManager pm, Context that) {
        //PackageManager pm = getPackageManager();
        boolean permission_internet = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.INTERNET", that.getPackageName()));
        boolean permission_receive_boot = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.RECEIVE_BOOT_COMPLETED", that.getPackageName()));
        boolean permission_foreground_service = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.FOREGROUND_SERVICE", that.getPackageName()));
        boolean permission_read_external_storage = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_EXTERNAL_STORAGE", that.getPackageName()));
        boolean permission_write_external_storage = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", that.getPackageName()));
        boolean permission_receive_sms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.RECEIVE_SMS", that.getPackageName()));
        boolean permission_read_sms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_SMS", that.getPackageName()));
        boolean permission_send_sms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.SEND_SMS", that.getPackageName()));
        boolean permission_read_phone_state = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_PHONE_STATE", that.getPackageName()));
        boolean permission_read_phone_numbers = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_PHONE_NUMBERS", that.getPackageName()));
        boolean permission_read_call_log = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_CALL_LOG", that.getPackageName()));
        boolean permission_read_contacts = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_CONTACTS", that.getPackageName()));
        boolean permission_battery_stats = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.BATTERY_STATS", that.getPackageName()));

        if (!(permission_internet && permission_receive_boot && permission_foreground_service &&
                permission_read_external_storage && permission_write_external_storage &&
                permission_receive_sms && permission_read_sms && permission_send_sms &&
                permission_read_call_log && permission_read_contacts &&
                permission_read_phone_state && permission_read_phone_numbers && permission_battery_stats)) {
            ActivityCompat.requestPermissions((Activity) that, new String[]{
                    Manifest.permission.INTERNET,
                    Manifest.permission.RECEIVE_BOOT_COMPLETED,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.BATTERY_STATS,
            }, 0x01);
        }
    }

    /**
     * SIM 卡信息
     */
    public static class SimInfo {
        /**
         * 运营商信息：中国移动 中国联通 中国电信
         */
        public CharSequence mCarrierName;
        /**
         * 卡槽ID，SimSerialNumber
         */
        public CharSequence mIccId;
        /**
         * 卡槽id， -1 - 没插入、 0 - 卡槽1 、1 - 卡槽2
         */
        public int mSimSlotIndex;
        /**
         * 号码
         */
        public CharSequence mNumber;
        /**
         * 城市
         */
        public CharSequence mCountryIso;
        /**
         * 设备唯一识别码
         */
        public CharSequence mImei = getIMEI();
        /**
         * SIM的编号
         */
        public CharSequence mImsi;
        /**
         * SIM的 Subscription Id (SIM插入顺序)
         */
        public int mSubscriptionId;

        /**
         * 通过 IMEI 判断是否相等
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof SimInfo && (TextUtils.isEmpty(((SimInfo) obj).mImei) || ((SimInfo) obj).mImei.equals(mImei));
        }

        @NonNull
        @Override
        public String toString() {
            return "SimInfo{" +
                    "mCarrierName=" + mCarrierName +
                    ", mIccId=" + mIccId +
                    ", mSimSlotIndex=" + mSimSlotIndex +
                    ", mNumber=" + mNumber +
                    ", mCountryIso=" + mCountryIso +
                    ", mImei=" + mImei +
                    ", mImsi=" + mImsi +
                    ", mSubscriptionId=" + mSubscriptionId +
                    '}';
        }
    }

    /**
     * 反射未找到方法
     */
    private static class MethodNotFoundException extends Exception {

        public static final long serialVersionUID = -3241033488141442594L;

        MethodNotFoundException(String info) {
            super(info);
        }
    }
}
