package com.idormy.sms.forwarder.database.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.idormy.sms.forwarder.database.AppDatabase

class BaseViewModelFactory(private val context: Context?) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (context == null) throw IllegalArgumentException("Context CAN NOT BE null")

        when {
            modelClass.isAssignableFrom(FrpcViewModel::class.java) -> {
                val frpcDao = AppDatabase.getInstance(context).frpcDao()
                @Suppress("UNCHECKED_CAST")
                return FrpcViewModel(frpcDao) as T
            }

            modelClass.isAssignableFrom(MsgViewModel::class.java) -> {
                val msgDao = AppDatabase.getInstance(context).msgDao()
                @Suppress("UNCHECKED_CAST")
                return MsgViewModel(msgDao) as T
            }

            modelClass.isAssignableFrom(LogsViewModel::class.java) -> {
                val logDao = AppDatabase.getInstance(context).logsDao()
                @Suppress("UNCHECKED_CAST")
                return LogsViewModel(logDao) as T
            }

            modelClass.isAssignableFrom(RuleViewModel::class.java) -> {
                val ruleDao = AppDatabase.getInstance(context).ruleDao()
                @Suppress("UNCHECKED_CAST")
                return RuleViewModel(ruleDao) as T
            }

            modelClass.isAssignableFrom(SenderViewModel::class.java) -> {
                val senderDao = AppDatabase.getInstance(context).senderDao()
                @Suppress("UNCHECKED_CAST")
                return SenderViewModel(senderDao) as T
            }

            modelClass.isAssignableFrom(TaskViewModel::class.java) -> {
                val taskDao = AppDatabase.getInstance(context).taskDao()
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(taskDao) as T
            }
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }

}