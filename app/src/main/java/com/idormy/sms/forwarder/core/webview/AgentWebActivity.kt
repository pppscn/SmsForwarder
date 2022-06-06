package com.idormy.sms.forwarder.core.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xrouter.facade.Postcard
import com.xuexiang.xrouter.facade.callback.NavCallback
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.widget.slideback.SlideBack

/**
 * 壳浏览器
 *
 * @author xuexiang
 * @since 2019/1/5 上午12:15
 */
class AgentWebActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agent_web)
        SlideBack.with(this)
            .haveScroll(true)
            .callBack { finish() }
            .register()
        val uri = intent.data
        if (uri != null) {
            XRouter.getInstance().build(uri).navigation(this, object : NavCallback() {
                override fun onArrival(postcard: Postcard) {
                    finish()
                }

                override fun onLost(postcard: Postcard) {
                    loadUrl(uri.toString())
                }
            })
        } else {
            val url = intent.getStringExtra(AgentWebFragment.KEY_URL)
            loadUrl(url)
        }
    }

    private fun loadUrl(url: String?) {
        if (url != null) {
            openFragment(url)
        } else {
            XToastUtils.error(getString(R.string.data_error))
            finish()
        }
    }

    private var mAgentWebFragment: AgentWebFragment? = null
    private fun openFragment(url: String) {
        val ft = supportFragmentManager.beginTransaction()
        ft.add(
            R.id.container_frame_layout,
            AgentWebFragment.getInstance(url).also { mAgentWebFragment = it })
        ft.commit()
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }*/

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val agentWebFragment = mAgentWebFragment
        return if (agentWebFragment != null) {
            if ((agentWebFragment as FragmentKeyDown).onFragmentKeyDown(keyCode, event)) {
                true
            } else {
                super.onKeyDown(keyCode, event)
            }
        } else super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        SlideBack.unregister(this)
        super.onDestroy()
    }

    companion object {
        /**
         * 请求浏览器
         *
         * @param url
         */
        fun goWeb(context: Context?, url: String?) {
            val intent = Intent(context, AgentWebActivity::class.java)
            intent.putExtra(AgentWebFragment.KEY_URL, url)
            context?.startActivity(intent)
        }
    }
}