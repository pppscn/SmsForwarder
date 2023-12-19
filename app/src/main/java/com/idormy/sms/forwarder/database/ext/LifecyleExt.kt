package com.idormy.sms.forwarder.database.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LifecycleOwner.observe(liveData: LiveData<T>?, observer: (T) -> Unit) =
    liveData?.observe(this, Observer(observer))