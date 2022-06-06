package com.idormy.sms.forwarder.database.repository

interface Listener {
    fun onDelete(id: Long)
}