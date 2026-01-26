package cn.ppps.forwarder.database.repository

interface Listener {
    fun onDelete(id: Long)
}