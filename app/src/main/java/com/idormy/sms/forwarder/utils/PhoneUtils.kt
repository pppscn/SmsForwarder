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
import android.telephony.TelephonyManager
import android.text.TextUtils
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
import com.xuexiang.xutil.resource.ResUtils.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.min

@Suppress("DEPRECATION")
class PhoneUtils private constructor() {

    companion object {
        const val TAG = "PhoneUtils"

        /** 获取 sim 卡槽数量，注意不是 sim 卡的数量。*/
        fun getSimSlotCount() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            (App.context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).activeModemCount
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            (App.context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).phoneCount
        else
            -1

        //获取多卡信息
        @SuppressLint("Range")
        fun getSimMultiInfo(): MutableMap<Int, SimInfo> {
            val infoList = HashMap<Int, SimInfo>()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    Log.d(TAG, "1.版本超过5.1，调用系统方法")
                    val mSubscriptionManager = XUtil.getContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    ActivityCompat.checkSelfPermission(
                        XUtil.getContext(), permission.READ_PHONE_STATE
                    )
                    val activeSubscriptionInfoList: List<SubscriptionInfo>? = mSubscriptionManager.activeSubscriptionInfoList
                    if (!activeSubscriptionInfoList.isNullOrEmpty()) {
                        //1.1.1 有使用的卡，就遍历所有卡
                        for (subscriptionInfo in activeSubscriptionInfoList) {
                            val simInfo = SimInfo()
                            simInfo.mCarrierName = subscriptionInfo.carrierName?.toString()
                            simInfo.mIccId = subscriptionInfo.iccId?.toString()
                            simInfo.mSimSlotIndex = subscriptionInfo.simSlotIndex
                            simInfo.mNumber = subscriptionInfo.number?.toString()
                            simInfo.mCountryIso = subscriptionInfo.countryIso?.toString()
                            simInfo.mSubscriptionId = subscriptionInfo.subscriptionId
                            Log.d(TAG, simInfo.toString())
                            infoList[simInfo.mSimSlotIndex] = simInfo
                        }
                    }
                } else {
                    Log.d(TAG, "2.版本低于5.1的系统，首先调用数据库，看能不能访问到")
                    val uri = Uri.parse("content://telephony/siminfo") //访问raw_contacts表
                    val resolver: ContentResolver = XUtil.getContext().contentResolver
                    val cursor = resolver.query(
                        uri, arrayOf(
                            "_id", "icc_id", "sim_id", "display_name", "carrier_name", "name_source", "color", "number", "display_number_format", "data_roaming", "mcc", "mnc"
                        ), null, null, null
                    )
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            val simInfo = SimInfo()
                            simInfo.mCarrierName = cursor.getString(cursor.getColumnIndex("carrier_name"))
                            simInfo.mIccId = cursor.getString(cursor.getColumnIndex("icc_id"))
                            simInfo.mSimSlotIndex = cursor.getInt(cursor.getColumnIndex("sim_id"))
                            simInfo.mNumber = cursor.getString(cursor.getColumnIndex("number"))
                            simInfo.mCountryIso = cursor.getString(cursor.getColumnIndex("mcc"))
                            //val id = cursor.getString(cursor.getColumnIndex("_id"))
                            Log.d(TAG, simInfo.toString())
                            infoList[simInfo.mSimSlotIndex] = simInfo
                        } while (cursor.moveToNext())
                        cursor.close()
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.e(TAG, "getSimMultiInfo:", e)
            }
            //仍然获取不到/只获取到一个->取出备注
            /*if (infoList.isEmpty() || infoList.size == 1) {
                Log.d(TAG, "3.直接取出备注框的数据作为信息")
                //为空，两个卡都没有获取到信息
                if (infoList.isEmpty()) {
                    //卡1备注信息不为空
                    val etExtraSim1 = SettingUtils.extraSim1
                    if (!TextUtils.isEmpty(etExtraSim1)) {
                        val simInfo1 = SimInfo()
                        //卡1
                        simInfo1.mSimSlotIndex = 0
                        simInfo1.mNumber = etExtraSim1
                        simInfo1.mSubscriptionId = SettingUtils.subidSim1
                        //把卡放入
                        infoList[simInfo1.mSimSlotIndex] = simInfo1
                    }
                    //卡2备注信息不为空
                    val etExtraSim2 = SettingUtils.extraSim2
                    if (!TextUtils.isEmpty(etExtraSim2)) {
                        val simInfo2 = SimInfo()
                        simInfo2.mSimSlotIndex = 1
                        simInfo2.mNumber = etExtraSim2
                        simInfo2.mSubscriptionId = SettingUtils.subidSim2
                        //把卡放入
                        infoList[simInfo2.mSimSlotIndex] = simInfo2
                    }

                    //有一张卡,判断是卡几
                } else {
                    var infoListIndex = -1
                    for (obj in infoList) {
                        infoListIndex = obj.key
                    }
                    //获取到卡1，且卡2备注信息不为空
                    if (infoListIndex == 0 && !TextUtils.isEmpty(SettingUtils.extraSim2)) {
                        //获取到卡1信息，卡2备注不为空，创建卡2实体
                        val simInfo2 = SimInfo()
                        simInfo2.mSimSlotIndex = 1
                        simInfo2.mNumber = SettingUtils.extraSim2
                        simInfo2.mSubscriptionId = SettingUtils.subidSim1
                        infoList[simInfo2.mSimSlotIndex] = simInfo2
                    } else if (infoListIndex == 1 && !TextUtils.isEmpty(SettingUtils.extraSim1)) {
                        //获取到卡2信息，卡1备注不为空，创建卡1实体
                        val simInfo1 = SimInfo()
                        simInfo1.mSimSlotIndex = 0
                        simInfo1.mNumber = SettingUtils.extraSim1
                        simInfo1.mSubscriptionId = SettingUtils.subidSim1
                        infoList[simInfo1.mSimSlotIndex] = simInfo1
                    }
                }
            }*/
            Log.i(TAG, infoList.toString())
            return infoList
        }

        //获取设备名称
        fun getDeviceName(): String {
            return try {
                Settings.Secure.getString(XUtil.getContentResolver(), "bluetooth_name")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "getDeviceName:", e)
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
            if (TextUtils.isEmpty(mobileList) || TextUtils.isEmpty(message)) {
                Log.e(TAG, "mobileList or message is empty!")
                return "mobileList or message is empty!"
            }

            val mobiles = mobileList.replace("；", ";").replace("，", ";").replace(",", ";")
            Log.d(TAG, "subId = $subId, mobiles = $mobiles, message = $message")
            val mobileArray = mobiles.split(";".toRegex()).toTypedArray()
            for (mobile in mobileArray) {
                Log.d(TAG, "mobile = $mobile")
                if (!isValidPhoneNumber(mobile)) {
                    Log.e(TAG, "mobile ($mobile) is invalid!")
                    continue
                }

                try {
                    val sendFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
                    val sendPI = PendingIntent.getBroadcast(XUtil.getContext(), 0, Intent(), sendFlags)

                    val smsManager = if (subId > -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) SmsManager.getSmsManagerForSubscriptionId(
                        subId
                    ) else SmsManager.getDefault()
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
                        val deliverPI = PendingIntent.getBroadcast(
                            XUtil.getContext(), 0, Intent("DELIVERED_SMS_ACTION"), deliverFlags
                        )

                        val sentPendingIntents = ArrayList<PendingIntent>()
                        val deliveredPendingIntents = ArrayList<PendingIntent>()
                        val divideContents = smsManager.divideMessage(message)

                        for (i in divideContents.indices) {
                            sentPendingIntents.add(i, sendPI)
                            deliveredPendingIntents.add(i, deliverPI)
                        }
                        smsManager.sendMultipartTextMessage(
                            mobile, null, divideContents, sentPendingIntents, deliveredPendingIntents
                        )
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
        fun getCallInfoList(
            type: Int, limit: Int, offset: Int, phoneNumber: String?
        ): MutableList<CallInfo> {
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
                    CallLog.Calls.CONTENT_URI, null, selection, selectionArgs.toTypedArray(), CallLog.Calls.DEFAULT_SORT_ORDER // + " limit $limit offset $offset"
                ) ?: return callInfoList
                Log.i(TAG, "cursor count:" + cursor.count)

                // 避免超过总数后循环取出
                if (cursor.count == 0 || offset >= cursor.count) {
                    cursor.close()
                    return callInfoList
                }

                if (cursor.moveToFirst()) {
                    Log.d(TAG, "Call ColumnNames=${cursor.columnNames.contentToString()}")
                    val indexName = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val indexNumber = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val indexDate = cursor.getColumnIndex(CallLog.Calls.DATE)
                    val indexDuration = cursor.getColumnIndex(CallLog.Calls.DURATION)
                    val indexType = cursor.getColumnIndex(CallLog.Calls.TYPE)
                    val indexViaNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cursor.getColumnIndex("via_number") != -1) cursor.getColumnIndex("via_number") else -1
                    var indexSimId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID) else -1
                    var indexSubId = indexSimId

                    /**
                     * TODO:卡槽识别，这里需要适配机型
                     * MIUI系统：simid 字段实际为 subscription_id
                     * EMUI系统：subscription_id 实际为 sim_id
                     */
                    var isSimId = false
                    val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
                    Log.i(TAG, "manufacturer = $manufacturer")
                    if (manufacturer.contains(Regex(pattern = "xiaomi|redmi"))) {
                        if (cursor.getColumnIndex("simid") != -1) indexSimId = cursor.getColumnIndex("simid")
                        indexSubId = indexSimId
                    } else if (manufacturer.contains(Regex(pattern = "huawei|honor"))) {
                        indexSubId = -1 //TODO:暂时不支持华为
                        isSimId = true
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
                                if (indexSimId != -1) getSimId(cursor.getInt(indexSimId), isSimId) else -1,  //卡槽ID： 0=Sim1, 1=Sim2, -1=获取失败
                                if (indexSubId != -1) cursor.getInt(indexSubId) else 0,  //卡槽主键
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
        fun getContactInfoList(
            limit: Int, offset: Int, phoneNumber: String?, name: String?, isFuzzy: Boolean = true
        ): MutableList<ContactInfo> {
            val contactInfoList: MutableList<ContactInfo> = mutableListOf()

            try {
                var selection = "1=1"
                val selectionArgs = ArrayList<String>()
                if (!TextUtils.isEmpty(phoneNumber)) {
                    selection += " and replace(replace(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ",' ',''),'-','') like ?"
                    if (isFuzzy) {
                        selectionArgs.add("%$phoneNumber%")
                    } else {
                        selectionArgs.add("%$phoneNumber")
                    }
                }
                if (!TextUtils.isEmpty(name)) {
                    selection += " and " + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like ?"
                    selectionArgs.add("%$name%")
                }
                Log.d(TAG, "selection = $selection")
                Log.d(TAG, "selectionArgs = $selectionArgs")

                val cursor = Core.app.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, selection, selectionArgs.toTypedArray(), ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY
                ) ?: return contactInfoList
                Log.i(TAG, "cursor count:" + cursor.count)

                // 避免超过总数后循环取出
                if (cursor.count == 0 || offset >= cursor.count) {
                    cursor.close()
                    return contactInfoList
                }

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

        // 获取号码归属地
        fun getPhoneArea(phoneNumber: String): String {
            val client = OkHttpClient()
            val url = "https://cx.shouji.360.cn/phonearea.php?number=$phoneNumber"
            val request = Request.Builder().url(url).build()

            var result = getString(R.string.unknown_area) // 默认值

            // 使用协程来执行网络请求
            runBlocking {
                val job = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val responseData = response.body()?.string()
                            Log.i(TAG, "getPhoneArea: $responseData")
                            if (responseData != null) {
                                val jsonObject = JSONObject(responseData)
                                val data = jsonObject.getJSONObject("data")
                                val province = data.getString("province")
                                val city = data.getString("city")
                                val sp = data.getString("sp")
                                result = "$province $city $sp"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                job.join() // 等待协程执行完毕
            }

            return result
        }

        //获取联系人姓名
        fun getContactByNumber(phoneNumber: String?): MutableList<ContactInfo> {
            val contactInfoList = mutableListOf<ContactInfo>()
            if (TextUtils.isEmpty(phoneNumber)) return contactInfoList

            // 去除国际区号、空格、括号、横线等字符
            val normalizedInputNumber = if (phoneNumber!!.startsWith("+") && phoneNumber.length > 4) {
                phoneNumber.substring(4).replace("[^0-9]".toRegex(), "")
            } else {
                phoneNumber.replace("[^0-9]".toRegex(), "")
            }

            contactInfoList.addAll(getContactInfoList(99, 0, normalizedInputNumber, null, false))
            if (contactInfoList.isEmpty() || contactInfoList.size == 1) {
                return contactInfoList
            }

            // 计算每个联系人的匹配长度和优先级
            val scoredContacts = contactInfoList.map { contact ->
                //去除空格、括号、横线等字符
                val normalizedContactNumber = contact.phoneNumber.replace("[^0-9]".toRegex(), "")
                val matchLength = calculateMatchLength(normalizedInputNumber, normalizedContactNumber)
                // 优先级规则：
                // 1. 完全匹配（输入手机号与联系人手机号完全一致）：优先级 2
                // 2. 匹配长度等于输入手机号长度：优先级 1
                // 3. 其他情况：优先级 0
                val priority = when {
                    normalizedInputNumber == normalizedContactNumber -> 2
                    matchLength == normalizedInputNumber.length -> 1
                    else -> 0
                }
                contact to Pair(matchLength, priority)
            }.sortedWith(compareByDescending<Pair<ContactInfo, Pair<Int, Int>>> { it.second.first } // 按匹配长度降序
                .thenByDescending { it.second.second }) // 按优先级降序

            // 返回匹配长度最长且优先级最高的联系人列表
            val maxMatchLength = scoredContacts.first().second.first
            val maxPriority = scoredContacts.first().second.second
            return scoredContacts
                .filter { it.second.first == maxMatchLength && it.second.second == maxPriority }
                .map { it.first }
                .toMutableList()
        }

        // 计算从右向左的匹配长度
        private fun calculateMatchLength(number1: String, number2: String): Int {
            var matchLength = 0
            val minLength = min(number1.length, number2.length)

            // 从右向左逐位比较
            for (i in 1..minLength) {
                if (number1[number1.length - i] == number2[number2.length - i]) {
                    matchLength++
                } else {
                    break // 遇到不匹配的字符，停止比较
                }
            }

            return matchLength
        }

        //获取通话记录转发内容
        fun getCallMsg(callInfo: CallInfo): String {
            val sb = StringBuilder()
            sb.append(getString(R.string.contact)).append(callInfo.name).append("\n")
            if (!TextUtils.isEmpty(callInfo.viaNumber)) sb.append(getString(R.string.via_number)).append(callInfo.viaNumber).append("\n")
            if (callInfo.dateLong > 0L) sb.append(getString(R.string.call_date)).append(
                DateUtils.millis2String(
                    callInfo.dateLong, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                )
            ).append("\n")
            if (callInfo.duration > 0) {
                if (callInfo.type == 3) {
                    sb.append(getString(R.string.ring_duration))
                } else {
                    sb.append(getString(R.string.call_duration))
                }
                sb.append(callInfo.duration).append("s\n")
            }
            sb.append(getString(R.string.mandatory_type))
            //通话类型：1.来电挂机 2.去电挂机 3.未接来电 4.来电提醒 5.来电接通 6.去电拨出
            when (callInfo.type) {
                1 -> sb.append(getString(R.string.incoming_call_ended))
                2 -> sb.append(getString(R.string.outgoing_call_ended))
                3 -> sb.append(getString(R.string.missed_call))
                4 -> sb.append(getString(R.string.incoming_call_received))
                5 -> sb.append(getString(R.string.incoming_call_answered))
                6 -> sb.append(getString(R.string.outgoing_call_started))
                else -> sb.append(getString(R.string.unknown_call))
            }
            return sb.toString()
        }

        // 获取用户短信列表
        fun getSmsInfoList(
            type: Int, limit: Int, offset: Int, keyword: String
        ): MutableList<SmsInfo> {
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
                    Uri.parse("content://sms/"), null, selection, selectionArgs.toTypedArray(), "date desc"
                ) ?: return smsInfoList
                if (offset >= cursorTotal.count) {
                    cursorTotal.close()
                    return smsInfoList
                }

                val cursor = Core.app.contentResolver.query(
                    Uri.parse("content://sms/"), null, selection, selectionArgs.toTypedArray(), "date desc limit $limit offset $offset"
                ) ?: return smsInfoList

                Log.i(TAG, "cursor count:" + cursor.count)
                if (cursor.count == 0) {
                    cursor.close()
                    return smsInfoList
                }

                if (cursor.moveToFirst()) {
                    Log.d(TAG, "SMS ColumnNames=${cursor.columnNames.contentToString()}")
                    val indexAddress = cursor.getColumnIndex("address")
                    val indexBody = cursor.getColumnIndex("body")
                    val indexDate = cursor.getColumnIndex("date")
                    val indexType = cursor.getColumnIndex("type")
                    var indexSimId = cursor.getColumnIndex("sim_id")
                    var indexSubId = cursor.getColumnIndex("sub_id")

                    /**
                     * TODO:卡槽识别，这里需要适配机型
                     * MIUI系统：sim_id 字段实际为 subscription_id
                     * EMUI系统：sub_id 实际为 sim_id
                     */
                    var isSimId = false
                    val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
                    Log.i(TAG, "manufacturer = $manufacturer")
                    if (manufacturer.contains(Regex(pattern = "xiaomi|redmi"))) {
                        indexSubId = cursor.getColumnIndex("sim_id")
                    } else if (manufacturer.contains(Regex(pattern = "huawei|honor"))) {
                        indexSimId = cursor.getColumnIndex("sub_id")
                        isSimId = true
                    }

                    do {
                        val smsInfo = SmsInfo()
                        val phoneNumber = cursor.getString(indexAddress)
                        // 根据手机号码查询用户名
                        val contacts = getContactByNumber(phoneNumber)
                        smsInfo.name = if (contacts.isNotEmpty()) contacts[0].name else getString(R.string.unknown_number)
                        // 联系人号码
                        smsInfo.number = phoneNumber
                        // 短信内容
                        smsInfo.content = cursor.getString(indexBody)
                        // 短信时间
                        smsInfo.date = cursor.getLong(indexDate)
                        // 短信类型: 1=接收, 2=发送
                        smsInfo.type = cursor.getInt(indexType)
                        // 卡槽ID： 0=Sim1, 1=Sim2, -1=获取失败
                        smsInfo.simId = if (indexSimId != -1) getSimId(cursor.getInt(indexSimId), isSimId) else -1
                        // 卡槽主键
                        smsInfo.subId = if (indexSubId != -1) cursor.getInt(indexSubId) else 0

                        smsInfoList.add(smsInfo)
                    } while (cursor.moveToNext())

                    if (!cursorTotal.isClosed) cursorTotal.close()
                    if (!cursor.isClosed) cursor.close()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.e(TAG, "getSmsInfoList:", e)
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

            if (SettingUtils.subidSim1 > 0 || SettingUtils.subidSim2 > 0) {
                return if (mId == SettingUtils.subidSim1) 0 else 1
            } else {
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

        //判断是否是手机号码(宽松判断)
        private fun isValidPhoneNumber(phoneNumber: String): Boolean {
            val regex = Regex("^\\+?\\d{3,20}$")
            return regex.matches(phoneNumber)
        }

    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}
