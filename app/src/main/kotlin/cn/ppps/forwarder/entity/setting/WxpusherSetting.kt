package cn.ppps.forwarder.entity.setting

import java.io.Serializable

data class WxpusherSetting(
    val appToken: String = "",
    val uids: String = "",
    val topicIds: String = "",
    val spt: String = "",
    val contentType: Int = 1,
    val summaryTemplate: String = "",
    val regexReplace: String = "",
) : Serializable {

    //SPT极简推送模式：自己发给自己，无需创建应用
    val isSimplePush: Boolean
        get() = spt.isNotBlank() && appToken.isBlank()

    fun getPushTypeCheckId(): Int {
        return if (isSimplePush) cn.ppps.forwarder.R.id.rb_simple_push else cn.ppps.forwarder.R.id.rb_standard_push
    }

    fun getContentTypeCheckId(): Int {
        return when (contentType) {
            2 -> cn.ppps.forwarder.R.id.rb_content_html
            3 -> cn.ppps.forwarder.R.id.rb_content_markdown
            else -> cn.ppps.forwarder.R.id.rb_content_text
        }
    }
}
