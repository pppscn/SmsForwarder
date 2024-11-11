package com.idormy.sms.forwarder.widget

import android.content.Context
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.http.entity.TipInfo
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.SharedPreference
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xui.widget.dialog.BaseDialog
import com.xuexiang.xutil.resource.ResUtils.getString
import com.zzhoujay.richtext.RichText

/**
 * 小贴士弹窗
 *
 * @author xuexiang
 * @since 2019-08-22 17:02
 */
@Suppress("SameReturnValue")
class GuideTipsDialog(context: Context?, tips: List<TipInfo>) :
    BaseDialog(context, R.layout.dialog_guide_tips), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {
    private var mTips: List<TipInfo>? = null
    private var mIndex = -1
    private var mTvPrevious: TextView? = null
    private var mTvNext: TextView? = null
    private var mTvTitle: TextView? = null
    private var mTvContent: TextView? = null

    /**
     * 初始化弹窗
     */
    private fun initViews() {
        mTvTitle = findViewById(R.id.tv_title)
        mTvContent = findViewById(R.id.tv_content)
        val cbIgnore = findViewById<AppCompatCheckBox>(R.id.cb_ignore)
        val ivClose = findViewById<ImageView>(R.id.iv_close)
        mTvPrevious = findViewById(R.id.tv_previous)
        mTvNext = findViewById(R.id.tv_next)
        if (cbIgnore != null) {
            cbIgnore.isChecked = isIgnoreTips
            cbIgnore.setOnCheckedChangeListener(this)
        }
        ivClose?.setOnClickListener(this)
        mTvPrevious!!.setOnClickListener(this)
        mTvNext!!.setOnClickListener(this)
        mTvPrevious!!.isEnabled = false
        mTvNext!!.isEnabled = true
        setCancelable(false)
        setCanceledOnTouchOutside(true)
    }

    /**
     * 更新提示信息
     *
     * @param tips 提示信息
     */
    private fun updateTips(tips: List<TipInfo>) {
        mTips = tips
        if (mTips!!.isNotEmpty() && mTvContent != null) {
            mIndex = 0
            showRichText(mTips!![mIndex])
        }
    }

    /**
     * 切换提示信息
     *
     * @param index 索引
     */
    private fun switchTipInfo(index: Int) {
        if (mTips != null && mTips!!.isNotEmpty() && mTvContent != null) {
            if (index >= 0 && index <= mTips!!.size - 1) {
                showRichText(mTips!![index])
                when (index) {
                    0 -> {
                        mTvPrevious!!.isEnabled = false
                        mTvNext!!.isEnabled = true
                    }

                    mTips!!.size - 1 -> {
                        mTvPrevious!!.isEnabled = true
                        mTvNext!!.isEnabled = false
                    }

                    else -> {
                        mTvPrevious!!.isEnabled = true
                        mTvNext!!.isEnabled = true
                    }
                }
            }
        }
    }

    /**
     * 显示富文本
     *
     * @param tipInfo 提示信息
     */
    private fun showRichText(tipInfo: TipInfo) {
        mTvTitle!!.text = tipInfo.title
        RichText.fromHtml(tipInfo.content)
            .bind(this)
            .into(mTvContent)
    }

    @SingleClick(300)
    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.iv_close) {
            dismiss()
        } else if (id == R.id.tv_previous) {
            if (mIndex > 0) {
                mIndex--
                switchTipInfo(mIndex)
            }
        } else if (id == R.id.tv_next) {
            if (mIndex < mTips!!.size - 1) {
                mIndex++
                switchTipInfo(mIndex)
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        setIsIgnoreTips(isChecked)
    }

    override fun onDetachedFromWindow() {
        RichText.clear(this)
        super.onDetachedFromWindow()
    }

    companion object {
        private const val KEY_IS_IGNORE_TIPS =
            "com.idormy.sms.forwarder.widget.key_is_ignore_tips_"

        /**
         * 显示提示
         *
         * @param context 上下文
         */
        @JvmStatic
        fun showTips(context: Context?) {
            if (!isIgnoreTips) {
                showTipsForce(context)
            }
        }

        /**
         * 强制显示提示
         *
         * @param context 上下文
         */
        @JvmStatic
        fun showTipsForce(context: Context?) {
            XHttp.get(getString(R.string.url_tips))
                .keepJson(true)
                .ignoreHttpsCert()
                .timeStamp(true) //url自动追加时间戳，避免缓存
                .execute(object : SimpleCallBack<String>() {
                    override fun onError(e: ApiException) {
                        e.printStackTrace()
                    }

                    override fun onSuccess(json: String) {
                        try {
                            val gson = Gson()
                            val jsonObject = gson.fromJson(json, JsonObject::class.java)
                            if (jsonObject.isJsonObject
                                && jsonObject.has("Code") && jsonObject["Code"].asInt == 0
                                && jsonObject.has("Data") && jsonObject["Data"].isJsonArray
                            ) {
                                val dataJsonArray = jsonObject["Data"].asJsonArray
                                val listType = object : TypeToken<List<TipInfo>>() {}.type
                                val tips = gson.fromJson<List<TipInfo>>(dataJsonArray, listType)
                                GuideTipsDialog(context, tips).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
        }

        fun setIsIgnoreTips(isIgnore: Boolean): Boolean {
            this.isIgnoreTips = isIgnore
            return true
        }

        var isIgnoreTips: Boolean by SharedPreference(KEY_IS_IGNORE_TIPS + AppUtils.getAppVersionCode(), false)

    }

    init {
        initViews()
        updateTips(tips)
    }
}
