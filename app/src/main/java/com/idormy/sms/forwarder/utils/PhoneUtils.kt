package com.idormy.sms.forwarder.utils

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.CallInfo
import com.idormy.sms.forwarder.entity.ContactInfo
import com.idormy.sms.forwarder.entity.SimInfo
import com.idormy.sms.forwarder.entity.SmsInfo
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.app.IntentUtils
import com.xuexiang.xutil.data.DateUtils
import com.xuexiang.xutil.resource.ResUtils
import java.text.SimpleDateFormat
import java.util.*

@Suppress("PropertyName")
class PhoneUtils private constructor() {

    companion object {
        const val TAG = "PhoneUtils"

        //获取多卡信息
        @SuppressLint("Range")
        fun getSimMultiInfo(): MutableMap<Int, SimInfo> {
            val infoList = HashMap<Int, SimInfo>()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    println("1.版本超过5.1，调用系统方法")
                    val mSubscriptionManager = XUtil.getContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    ActivityCompat.checkSelfPermission(XUtil.getContext(), permission.READ_PHONE_STATE)
                    val activeSubscriptionInfoList: List<SubscriptionInfo>? = mSubscriptionManager.activeSubscriptionInfoList
                    if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.isNotEmpty()) {
                        //1.1.1 有使用的卡，就遍历所有卡
                        for (subscriptionInfo in activeSubscriptionInfoList) {
                            val simInfo = SimInfo()
                            simInfo.mCarrierName = subscriptionInfo.carrierName.toString()
                            simInfo.mIccId = subscriptionInfo.iccId.toString()
                            simInfo.mSimSlotIndex = subscriptionInfo.simSlotIndex
                            simInfo.mNumber = subscriptionInfo.number.toString()
                            simInfo.mCountryIso = subscriptionInfo.countryIso.toString()
                            simInfo.mSubscriptionId = subscriptionInfo.subscriptionId
                            println(simInfo.toString())
                            infoList[simInfo.mSimSlotIndex] = simInfo
                        }
                    }
                } else {
                    println("2.版本低于5.1的系统，首先调用数据库，看能不能访问到")
                    val uri = Uri.parse("content://telephony/siminfo") //访问raw_contacts表
                    val resolver: ContentResolver = XUtil.getContext().contentResolver
                    val cursor = resolver.query(uri, arrayOf("_id", "icc_id", "sim_id", "display_name", "carrier_name", "name_source", "color", "number", "display_number_format", "data_roaming", "mcc", "mnc"), null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            val simInfo = SimInfo()
                            simInfo.mCarrierName = cursor.getString(cursor.getColumnIndex("carrier_name"))
                            simInfo.mIccId = cursor.getString(cursor.getColumnIndex("icc_id"))
                            simInfo.mSimSlotIndex = cursor.getInt(cursor.getColumnIndex("sim_id"))
                            simInfo.mNumber = cursor.getString(cursor.getColumnIndex("number"))
                            simInfo.mCountryIso = cursor.getString(cursor.getColumnIndex("mcc"))
                            //val id = cursor.getString(cursor.getColumnIndex("_id"))
                            println(simInfo.toString())
                            infoList[simInfo.mSimSlotIndex] = simInfo
                        } while (cursor.moveToNext())
                        cursor.close()
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            Log.e(TAG, infoList.toString())
            return infoList
        }

        //获取设备名称
        fun getDeviceName(): String {
            return try {
                Settings.Secure.getString(XUtil.getContentResolver(), "bluetooth_name")
            } catch (e: Exception) {
                e.printStackTrace()
                Build.BRAND + " " + Build.MODEL
            }
        }

        /**
         * 发送短信
         * <p>需添加权限 {@code <uses-permission android:name="android.permission.SEND_SMS" />}</p>
         *
         * @param subId 发送卡的subId，传入 -1 则 SmsManager.getDefault()
         * @param mobileList 接收号码列表
         * @param message     短信内容
         */
        @Suppress("DEPRECATION")
        @SuppressLint("SoonBlockedPrivateApi", "DiscouragedPrivateApi")
        @RequiresPermission(permission.SEND_SMS)
        fun sendSms(subId: Int, mobileList: String, message: String): String? {
            val mobiles = mobileList.replace("；", ";").replace("，", ";").replace(",", ";")
            Log.d(TAG, "subId = $subId, mobiles = $mobiles, message = $message")
            val mobileArray = mobiles.split(";".toRegex()).toTypedArray()
            for (mobile in mobileArray) {
                try {
                    val sendFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
                    val sendPI = PendingIntent.getBroadcast(XUtil.getContext(), 0, Intent(), sendFlags)

                    val smsManager = if (subId > -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
                    // Android 5.1.1 以下使用反射指定卡槽
                    if (subId > -1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                        Log.d(TAG, "Android 5.1.1 以下使用反射指定卡槽")
                        val clz = SmsManager::class.java
                        val field = clz.getDeclaredField("mSubId") // 反射拿到变量
                        field.isAccessible = true // 修改权限为可读写
                        field.set(smsManager, subId)
                    }

                    // 切割长短信
                    if (message.length >= 70) {
                        val deliverFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) PendingIntent.FLAG_IMMUTABLE else 0
                        val deliverPI = PendingIntent.getBroadcast(XUtil.getContext(), 0, Intent("DELIVERED_SMS_ACTION"), deliverFlags)

                        val sentPendingIntents = ArrayList<PendingIntent>()
                        val deliveredPendingIntents = ArrayList<PendingIntent>()
                        val divideContents = smsManager.divideMessage(message)

                        for (i in divideContents.indices) {
                            sentPendingIntents.add(i, sendPI)
                            deliveredPendingIntents.add(i, deliverPI)
                        }
                        smsManager.sendMultipartTextMessage(mobile, null, divideContents, sentPendingIntents, deliveredPendingIntents)
                    } else {
                        smsManager.sendTextMessage(mobile, null, message, sendPI, null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.message.toString())
                    return e.message.toString()
                }
            }

            return null
        }

        //获取通话记录列表
        fun getCallInfoList(type: Int, limit: Int, offset: Int, phoneNumber: String?): MutableList<CallInfo> {
            val callInfoList: MutableList<CallInfo> = mutableListOf()
            try {
                var selection = "1=1"
                val selectionArgs = ArrayList<String>()
                if (type > 0) {
                    selection += " and " + CallLog.Calls.TYPE + " = ?"
                    selectionArgs.add("$type")
                }
                if (!TextUtils.isEmpty(phoneNumber)) {
                    selection += " and " + CallLog.Calls.NUMBER + " like ?"
                    selectionArgs.add("%$phoneNumber%")
                }
                Log.d(TAG, "selection = $selection")
                Log.d(TAG, "selectionArgs = $selectionArgs")

                //为了兼容性这里全部取出后手动分页
                val cursor = Core.app.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs.toTypedArray(),
                    CallLog.Calls.DEFAULT_SORT_ORDER // + " limit $limit offset $offset"
                ) ?: return callInfoList
                Log.i(TAG, "cursor count:" + cursor.count)

                // 避免超过总数后循环取出
                if (cursor.count == 0 || offset >= cursor.count) return callInfoList

                if (cursor.moveToFirst()) {
                    val indexName = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val indexNumber = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val indexDate = cursor.getColumnIndex(CallLog.Calls.DATE)
                    val indexDuration = cursor.getColumnIndex(CallLog.Calls.DURATION)
                    val indexType = cursor.getColumnIndex(CallLog.Calls.TYPE)
                    val indexViaNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cursor.getColumnIndex("via_number") != -1) cursor.getColumnIndex("via_number") else -1
                    val isSimId = false
                    var indexSimId = -1
                    if (cursor.getColumnIndex("simid") != -1) {
                        indexSimId = cursor.getColumnIndex("simid")
                    } else if (cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID) != -1
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    ) {
                        indexSimId = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                    }
                    var curOffset = 0
                    do {
                        if (curOffset >= offset) {
                            val callInfo = CallInfo(
                                cursor.getString(indexName) ?: "",  //姓名
                                cursor.getString(indexNumber) ?: "",  //号码
                                cursor.getLong(indexDate),  //获取通话日期
                                cursor.getInt(indexDuration),  //获取通话时长，值为多少秒
                                cursor.getInt(indexType),  //获取通话类型：1.呼入 2.呼出 3.未接
                                if (indexViaNumber != -1) cursor.getString(indexViaNumber) else "",  //来源号码
                                if (indexSimId != -1) getSimId(cursor.getInt(indexSimId), isSimId) else -1 //卡槽id
                            )
                            Log.d(TAG, callInfo.toString())
                            callInfoList.add(callInfo)
                            if (limit == 1) {
                                cursor.close()
                                return callInfoList
                            }
                        }
                        curOffset++
                        if (curOffset >= offset + limit) break
                    } while (cursor.moveToNext())
                    if (!cursor.isClosed) cursor.close()
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "getCallInfoList:", e)
            }

            return callInfoList
        }

        //获取后一条通话记录
        @SuppressLint("Range")
        fun getLastCallInfo(callType: Int, phoneNumber: String?): CallInfo? {
            val callInfoList = getCallInfoList(callType, 1, 0, phoneNumber)
            if (callInfoList.isNotEmpty()) return callInfoList[0]
            return null
        }

        //获取联系人列表
        fun getContactInfoList(limit: Int, offset: Int, phoneNumber: String?, name: String?): MutableList<ContactInfo> {
            val contactInfoList: MutableList<ContactInfo> = mutableListOf()

            try {
                var selection = "1=1"
                val selectionArgs = ArrayList<String>()
                if (!TextUtils.isEmpty(phoneNumber)) {
                    selection += " and replace(replace(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ",' ',''),'-','') like ?"
                    selectionArgs.add("%$phoneNumber%")
                }
                if (!TextUtils.isEmpty(name)) {
                    selection += " and " + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like ?"
                    selectionArgs.add("%$name%")
                }
                Log.d(TAG, "selection = $selection")
                Log.d(TAG, "selectionArgs = $selectionArgs")

                val cursor = Core.app.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs.toTypedArray(),
                    ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY
                ) ?: return contactInfoList
                Log.i(TAG, "cursor count:" + cursor.count)

                // 避免超过总数后循环取出
                if (cursor.count == 0 || offset >= cursor.count) return contactInfoList

                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val mobileNoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    do {
                        val contactInfo = ContactInfo(
                            cursor.getString(displayNameIndex),  //姓名
                            cursor.getString(mobileNoIndex),  //号码
                        )
                        Log.d(TAG, contactInfo.toString())
                        contactInfoList.add(contactInfo)
                        if (limit == 1) {
                            cursor.close()
                            return contactInfoList
                        }
                    } while (cursor.moveToNext())
                    if (!cursor.isClosed) cursor.close()
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "getContactInfoList:", e)
            }

            return contactInfoList
        }

        //获取联系人姓名
        fun getContactByNumber(phoneNumber: String?): MutableList<ContactInfo> {
            val contactInfoList = mutableListOf<ContactInfo>()
            if (TextUtils.isEmpty(phoneNumber)) return contactInfoList
            return getContactInfoList(1, 0, phoneNumber, null)
        }

        //获取通话记录转发内容
        fun getCallMsg(callInfo: CallInfo): String {
            val sb = StringBuilder()
            sb.append(ResUtils.getString(R.string.linkman)).append(callInfo.name).append("\n")
            if (!TextUtils.isEmpty(callInfo.viaNumber)) sb.append(ResUtils.getString(R.string.via_number)).append(callInfo.viaNumber).append("\n")
            if (callInfo.dateLong > 0L) sb.append(ResUtils.getString(R.string.call_date)).append(DateUtils.millis2String(callInfo.dateLong, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()))).append("\n")
            if (callInfo.duration > 0) sb.append(ResUtils.getString(R.string.call_duration)).append(callInfo.duration).append("s\n")
            sb.append(ResUtils.getString(R.string.mandatory_type))
            //通话类型：1.呼入 2.呼出 3.未接
            when (callInfo.type) {
                1 -> sb.append(ResUtils.getString(R.string.received_call))
                2 -> sb.append(ResUtils.getString(R.string.local_outgoing_call))
                else -> sb.append(ResUtils.getString(R.string.missed_call))
            }
            return sb.toString()
        }

        // 获取用户短信列表
        fun getSmsInfoList(type: Int, limit: Int, offset: Int, keyword: String): MutableList<SmsInfo> {
            val smsInfoList: MutableList<SmsInfo> = mutableListOf()
            try {
                var selection = "1=1"
                val selectionArgs = ArrayList<String>()
                if (type > 0) {
                    selection += " and type = ?"
                    selectionArgs.add("$type")
                }
                if (!TextUtils.isEmpty(keyword)) {
                    selection += " and body like ?"
                    selectionArgs.add("%$keyword%")
                }
                Log.d(TAG, "selection = $selection")
                Log.d(TAG, "selectionArgs = $selectionArgs")

                // 避免超过总数后循环取出
                val cursorTotal = Core.app.contentResolver.query(
                    Uri.parse("content://sms/"),
                    null,
                    selection,
                    selectionArgs.toTypedArray(),
                    "date desc"
                ) ?: return smsInfoList
                if (offset >= cursorTotal.count) return smsInfoList

                val cursor = Core.app.contentResolver.query(
                    Uri.parse("content://sms/"),
                    null,
                    selection,
                    selectionArgs.toTypedArray(),
                    "date desc limit $limit offset $offset"
                ) ?: return smsInfoList

                Log.i(TAG, "cursor count:" + cursor.count)
                if (cursor.count == 0) return smsInfoList

                if (cursor.moveToFirst()) {
                    val indexAddress = cursor.getColumnIndex("address")
                    val indexBody = cursor.getColumnIndex("body")
                    val indexDate = cursor.getColumnIndex("date")
                    val indexType = cursor.getColumnIndex("type")
                    val isSimId = false
                    var indexSimId = -1
                    if (cursor.getColumnIndex("sim_id") != -1) {
                        indexSimId = cursor.getColumnIndex("sim_id")
                    }
                    do {
                        val smsInfo = SmsInfo()
                        val phoneNumber = cursor.getString(indexAddress)
                        // 根据手机号码查询用户名
                        val contacts = getContactByNumber(phoneNumber)
                        smsInfo.name = if (contacts.isNotEmpty()) contacts[0].name else ResUtils.getString(R.string.unknown_number)
                        // 联系人号码
                        smsInfo.number = phoneNumber
                        // 短信内容
                        smsInfo.content = cursor.getString(indexBody)
                        // 短信时间
                        smsInfo.date = cursor.getLong(indexDate)
                        // 短信类型: 1=接收, 2=发送
                        smsInfo.type = cursor.getInt(indexType)
                        // 卡槽id
                        smsInfo.simId = if (indexSimId != -1) getSimId(cursor.getInt(indexSimId), isSimId) else -1
                        smsInfoList.add(smsInfo)
                    } while (cursor.moveToNext())
                    if (!cursor.isClosed) cursor.close()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return smsInfoList
        }

        /**
         * 跳至拨号界面
         *
         * @param phoneNumber 电话号码
         */
        fun dial(phoneNumber: String?) {
            XUtil.getContext().startActivity(IntentUtils.getDialIntent(phoneNumber, true))
        }

        /**
         * 拨打电话
         *
         * 需添加权限 `<uses-permission android:name="android.permission.CALL_PHONE" />`
         *
         * @param phoneNumber 电话号码
         */
        fun call(phoneNumber: String?) {
            XUtil.getContext().startActivity(IntentUtils.getCallIntent(phoneNumber, true))
        }

        /**
         * 将 subscription_id 转成 卡槽ID： 0=Sim1, 1=Sim2, -1=获取失败
         *
         * TODO: 这里有坑，每个品牌定制系统的字段不太一样，不一定能获取到卡槽ID
         * 测试通过：MIUI   测试失败：原生 Android 11（Google Pixel 2 XL）
         *
         * @param mId SubscriptionId
         * @param isSimId 是否已经是SimId无需转换（待做机型兼容）
         */
        private fun getSimId(mId: Int, isSimId: Boolean): Int {
            Log.i(TAG, "mId = $mId, isSimId = $isSimId")
            if (isSimId) return mId

            //获取卡槽信息
            if (App.SimInfoList.isEmpty()) {
                App.SimInfoList = getSimMultiInfo()
            }
            Log.i(TAG, "SimInfoList = " + App.SimInfoList.toString())

            val simSlot = -1
            if (App.SimInfoList.isEmpty()) return simSlot
            for (simInfo in App.SimInfoList.values) {
                if (simInfo.mSubscriptionId == mId && simInfo.mSimSlotIndex != -1) {
                    Log.i(TAG, "simInfo = $simInfo")
                    return simInfo.mSimSlotIndex
                }
            }
            return simSlot
        }

    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}