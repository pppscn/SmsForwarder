package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.Frpc
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.FrpcViewModel
import com.idormy.sms.forwarder.databinding.FragmentFrpcEditBinding
import com.idormy.sms.forwarder.utils.EVENT_FRPC_UPDATE_CONFIG
import com.idormy.sms.forwarder.utils.INTENT_FRPC_EDIT_FILE
import com.idormy.sms.forwarder.utils.XToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.utils.ThemeUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.button.switchbutton.SwitchButton
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.edittext.materialedittext.MaterialEditText
import com.xuexiang.xutil.resource.ResUtils.getColors

@Suppress("DEPRECATION")
@Page(name = "Frp内网穿透·编辑配置")
class FrpcEditFragment : BaseFragment<FragmentFrpcEditBinding?>() {

    private var titleBar: TitleBar? = null
    private var frpc: Frpc? = null
    private val viewModel by viewModels<FrpcViewModel> { BaseViewModelFactory(context) }

    override fun initViews() {
        val pairCompleteMap: MutableMap<Char, Char> = HashMap()
        pairCompleteMap['{'] = '}'
        pairCompleteMap['['] = ']'
        pairCompleteMap['('] = ')'
        pairCompleteMap['<'] = '>'
        pairCompleteMap['"'] = '"'

        binding!!.editText.enablePairComplete(true)
        binding!!.editText.enablePairCompleteCenterCursor(true)
        binding!!.editText.setPairCompleteMap(pairCompleteMap)

        binding!!.editText.setEnableLineNumber(true)
        binding!!.editText.setLineNumberTextColor(Color.LTGRAY)
        binding!!.editText.setLineNumberTextSize(24f)
        binding!!.editText.textSize = 14f
    }

    override fun viewBindingInflate(inflater: LayoutInflater, container: ViewGroup): FragmentFrpcEditBinding {
        return FragmentFrpcEditBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setTitle(R.string.menu_frpc)
        titleBar!!.setActionTextColor(ThemeUtils.resolveColor(context, R.attr.colorAccent))
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_save) {
            @SuppressLint("ResourceAsColor")
            @SingleClick
            override fun performAction(view: View) {
                if (frpc == null) return

                val dialogFrpc = View.inflate(requireContext(), R.layout.dialog_frpc_save, null)
                val tvName = dialogFrpc.findViewById<MaterialEditText>(R.id.tv_name)
                val sbAutorun = dialogFrpc.findViewById<SwitchButton>(R.id.sb_autorun)

                tvName.setText(frpc!!.name)
                sbAutorun.setCheckedImmediately(frpc!!.autorun == 1)

                frpc!!.config = binding!!.editText.text.toString()

                if (TextUtils.isEmpty(frpc!!.config)) {
                    XToastUtils.error(R.string.tips_input_config_content)
                    return
                }

                MaterialDialog.Builder(context!!)
                    .iconRes(R.drawable.ic_menu_frpc)
                    .title(R.string.title_save_config)
                    .customView(dialogFrpc, true)
                    .cancelable(false)
                    .autoDismiss(false)
                    .neutralText(R.string.action_quit)
                    .neutralColor(getColors(R.color.red))
                    .onNeutral { dialog: MaterialDialog?, _: DialogAction? ->
                        dialog?.dismiss()
                        activity?.onBackPressed()
                    }
                    .negativeText(R.string.action_back)
                    .negativeColor(getColors(R.color.colorBlueGrey))
                    .onNegative { dialog: MaterialDialog?, _: DialogAction? ->
                        dialog?.dismiss()
                    }
                    .positiveText(R.string.action_save)
                    .onPositive { dialog: MaterialDialog?, _: DialogAction? ->
                        try {
                            frpc!!.autorun = if (sbAutorun.isChecked) 1 else 0
                            frpc!!.name = tvName.text.toString()
                            if (TextUtils.isEmpty(frpc!!.name)) {
                                XToastUtils.error(R.string.tips_input_config_name)
                                return@onPositive
                            }

                            if (TextUtils.isEmpty(frpc!!.uid)) {
                                viewModel.insert(frpc!!)
                            } else {
                                viewModel.update(frpc!!)
                            }

                            dialog?.dismiss()
                            LiveEventBus.get<Frpc>(EVENT_FRPC_UPDATE_CONFIG).post(frpc)
                            XToastUtils.success(R.string.tipSaveSuccess)

                            activity?.onBackPressed()
                        } catch (e: Exception) {
                            XToastUtils.error(e.message.toString())
                        }
                    }.show()
            }
        })
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_restore) {
            @SingleClick
            override fun performAction(view: View) {
                binding!!.editText.setText(frpc?.config!!)
                XToastUtils.success(R.string.tipRestoreSuccess)
            }
        })
        return titleBar
    }

    override fun initListeners() {
        LiveEventBus.get(INTENT_FRPC_EDIT_FILE, Frpc::class.java).observeSticky(this) { value: Frpc ->
            frpc = value
            binding!!.editText.setText(value.config)
            titleBar!!.setTitle(if (TextUtils.isEmpty(value.name)) getString(R.string.noName) else value.name)
        }
    }

}