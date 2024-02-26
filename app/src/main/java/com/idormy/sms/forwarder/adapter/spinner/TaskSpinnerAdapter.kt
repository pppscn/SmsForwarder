package com.idormy.sms.forwarder.adapter.spinner

import android.annotation.SuppressLint
import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.STATUS_OFF
import com.xuexiang.xui.utils.CollectionUtils
import com.xuexiang.xui.widget.spinner.editspinner.BaseEditSpinnerAdapter
import com.xuexiang.xui.widget.spinner.editspinner.EditSpinnerFilter
import com.xuexiang.xutil.resource.ResUtils.getDrawable

@Suppress("unused", "NAME_SHADOWING", "DEPRECATION")
class TaskSpinnerAdapter<T> : BaseEditSpinnerAdapter<T>, EditSpinnerFilter {
    /**
     * 选项的文字颜色
     */
    private var mTextColor = 0

    /**
     * 选项的文字大小
     */
    private var mTextSize = 0f

    /**
     * 背景颜色
     */
    private var mBackgroundSelector = 0

    /**
     * 过滤关键词的选中颜色
     */
    private var mFilterColor = "#F15C58"
    private var mIsFilterKey = false

    /**
     * 构造方法
     *
     * @param data 选项数据
     */
    constructor(data: List<T>?) : super(data)

    /**
     * 构造方法
     *
     * @param data 选项数据
     */
    constructor(data: Array<T>?) : super(data)

    override fun getEditSpinnerFilter(): EditSpinnerFilter {
        return this
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.context).inflate(R.layout.item_spinner_with_icon, parent, false)
            holder = ViewHolder(convertView, mTextColor, mTextSize, mBackgroundSelector)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        val item = CollectionUtils.getListItem(mDataSource, mIndexs[position]) as TaskSpinnerItem
        holder.iconView.setImageDrawable(item.icon)
        holder.statusView.setImageDrawable(
            getDrawable(
                when (item.status) {
                    STATUS_OFF -> R.drawable.ic_stop
                    else -> R.drawable.ic_start
                }
            )
        )
        //holder.titleView.text = Html.fromHtml(item.toString())
        holder.titleView.text = Html.fromHtml(getItem(position))
        return convertView
    }

    override fun onFilter(keyword: String): Boolean {
        mDisplayData.clear()
        Log.d("TaskSpinnerAdapter", "keyword = $keyword")
        Log.d("TaskSpinnerAdapter", "mIndexs.indices = ${mIndexs.indices}")
        if (TextUtils.isEmpty(keyword)) {
            initDisplayData(mDataSource)
            for (i in mIndexs.indices) {
                mIndexs[i] = i
            }
        } else {
            try {
                for (i in mDataSource.indices) {
                    if (getDataSourceString(i).contains(keyword, ignoreCase = true)) {
                        mIndexs[mDisplayData.size] = i
                        if (mIsFilterKey) {
                            mDisplayData.add(getDataSourceString(i).replaceFirst(keyword.toRegex(), "<font color=\"$mFilterColor\">$keyword</font>"))
                        } else {
                            mDisplayData.add(getDataSourceString(i))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("TaskSpinnerAdapter", "onFilter error: ${e.message}")
            }
        }
        Log.d("TaskSpinnerAdapter", "mDisplayData = $mDisplayData")
        notifyDataSetChanged()
        return mDisplayData.size > 0
    }

    fun setTextColor(@ColorInt textColor: Int): TaskSpinnerAdapter<*> {
        mTextColor = textColor
        return this
    }

    fun setTextSize(textSize: Float): TaskSpinnerAdapter<*> {
        mTextSize = textSize
        return this
    }

    fun setBackgroundSelector(@DrawableRes backgroundSelector: Int): TaskSpinnerAdapter<*> {
        mBackgroundSelector = backgroundSelector
        return this
    }

    fun setFilterColor(filterColor: String): TaskSpinnerAdapter<*> {
        mFilterColor = filterColor
        return this
    }

    fun setIsFilterKey(isFilterKey: Boolean): TaskSpinnerAdapter<*> {
        mIsFilterKey = isFilterKey
        return this
    }

    @SuppressLint("ObsoleteSdkInt")
    private class ViewHolder(convertView: View, @ColorInt textColor: Int, textSize: Float, @DrawableRes backgroundSelector: Int) {
        val iconView: ImageView = convertView.findViewById(R.id.iv_icon)
        val statusView: ImageView = convertView.findViewById(R.id.iv_status)
        val titleView: TextView = convertView.findViewById(R.id.tv_title)

        init {
            if (textColor > 0) titleView.setTextColor(textColor)
            if (textSize > 0F) titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            if (backgroundSelector != 0) titleView.setBackgroundResource(backgroundSelector)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val config = convertView.resources.configuration
                if (config.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                    titleView.textDirection = View.TEXT_DIRECTION_RTL
                }
            }
        }
    }

    fun getItemSource(position: Int): T {
        return mDataSource[mIndexs[position]]
    }
}
