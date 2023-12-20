package com.idormy.sms.forwarder.entity

import android.util.Patterns
import com.google.gson.annotations.SerializedName
import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable
import java.util.*

data class ContactInfo(
    val name: String = "",
    @SerializedName("phone_number")
    val phoneNumber: String = "",
) : Serializable {

    val firstLetter: String
        get() {
            return if (name.matches(Patterns.PHONE.toRegex())) "#" else name[0].toString().uppercase(Locale.getDefault())
        }

    override fun toString(): String {
        return String.format(getString(R.string.contact_info), name, phoneNumber)
    }
}