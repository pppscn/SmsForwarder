package com.idormy.sms.forwarder.server.controller

import android.content.ContentUris
import android.content.ContentValues
import android.provider.ContactsContract
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.entity.ContactInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.ContactQueryData
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.xuexiang.xutil.XUtil.getContentResolver
import com.yanzhenjie.andserver.annotation.*


@Suppress("PrivatePropertyName", "SameReturnValue")
@RestController
@RequestMapping(path = ["/contact"])
class ContactController {

    private val TAG: String = ContactController::class.java.simpleName

    //远程查话簿
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/query")
    fun query(@RequestBody bean: BaseRequest<ContactQueryData>): MutableList<ContactInfo> {
        val contactQueryData = bean.data
        Log.d(TAG, contactQueryData.toString())

        val limit = contactQueryData.pageSize
        val offset = (contactQueryData.pageNum - 1) * limit
        return PhoneUtils.getContactInfoList(limit, offset, contactQueryData.phoneNumber, contactQueryData.name)
    }

    //远程加话簿
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/add")
    fun add(@RequestBody bean: BaseRequest<ContactInfo>): String {
        val contactData = bean.data
        Log.d(TAG, contactData.toString())

        //创建一个空的ContentValues
        val values = ContentValues()
        //首先向RawContacts.CONTENT_URI执行一个空值插入，目的是获取系统返回的rawContactId
        val rawcontacturi = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values)
        val rawcontactid = ContentUris.parseId(rawcontacturi!!)

        //插入姓名数据
        values.clear()
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawcontactid)
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        values.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactData.name)
        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values)

        //插入电话数据
        for (phoneNumber in contactData.phoneNumber.split(";")) {
            values.clear()
            values.put(ContactsContract.Data.RAW_CONTACT_ID, rawcontactid)
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
            values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values)
        }

        return "success"
    }

}