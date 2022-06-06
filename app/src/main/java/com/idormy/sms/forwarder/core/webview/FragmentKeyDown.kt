package com.idormy.sms.forwarder.core.webview

import android.view.KeyEvent

/**
 *
 *
 * @author xuexiang
 * @since 2019/1/4 下午11:32
 */
interface FragmentKeyDown {
    /**
     * fragment按键监听
     * @param keyCode
     * @param event
     * @return
     */
    fun onFragmentKeyDown(keyCode: Int, event: KeyEvent?): Boolean
}