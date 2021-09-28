package com.idormy.sms.forwarder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.vo.LogVo;
import com.idormy.sms.forwarder.utils.aUtil;

import java.util.List;

@SuppressWarnings("unused")
public class LogAdapter extends ArrayAdapter<LogVo> {
    private final int resourceId;
    private List<LogVo> list;

    // 适配器的构造函数，把要适配的数据传入这里
    public LogAdapter(Context context, int textViewResourceId, List<LogVo> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
        list = objects;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public LogVo getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    // convertView 参数用于将之前加载好的布局进行缓存
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LogVo logVo = getItem(position); //获取当前项的TLog实例

        // 加个判断，以免ListView每次滚动时都要重新加载布局，以提高运行效率
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {

            // 避免ListView每次滚动时都要重新加载布局，以提高运行效率
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);

            // 避免每次调用getView()时都要重新获取控件实例
            viewHolder = new ViewHolder();
            viewHolder.tLogFrom = view.findViewById(R.id.tlog_from);
            viewHolder.tLogContent = view.findViewById(R.id.tlog_content);
            viewHolder.tLogRule = view.findViewById(R.id.tlog_rule);
            viewHolder.tLogTime = view.findViewById(R.id.tlog_time);
            viewHolder.senderImage = view.findViewById(R.id.tlog_sender_image);
            viewHolder.statusImage = view.findViewById(R.id.tlog_status_image);
            viewHolder.simImage = view.findViewById(R.id.tlog_sim_image);

            // 将ViewHolder存储在View中（即将控件的实例存储在其中）
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        // 获取控件实例，并调用set...方法使其显示出来
        if (logVo != null) {
            viewHolder.tLogFrom.setText(logVo.getFrom());
            viewHolder.tLogContent.setText(logVo.getContent());
            viewHolder.tLogRule.setText(logVo.getRule());
            viewHolder.tLogTime.setText(aUtil.friendlyTime(logVo.getTime()));
            viewHolder.senderImage.setImageResource(logVo.getSenderImageId());
            viewHolder.simImage.setImageResource(logVo.getSimImageId());
            viewHolder.statusImage.setImageResource(logVo.getStatusImageId());
        }

        return view;
    }

    public void add(List<LogVo> logVos) {
        if (list != null) {
            list = logVos;
            notifyDataSetChanged();
        }
    }

    public void del(List<LogVo> logVos) {
        if (list != null) {
            list = logVos;
            notifyDataSetChanged();
        }
    }

    public void update(List<LogVo> logVos) {
        if (list != null) {
            list = logVos;
            notifyDataSetChanged();
        }
    }

    public void onDateChange(List<LogVo> logVos) {
        list = logVos;
        notifyDataSetChanged();
    }

    // 定义一个内部类，用于对控件的实例进行缓存
    static class ViewHolder {
        TextView tLogFrom;
        TextView tLogContent;
        TextView tLogRule;
        TextView tLogTime;
        ImageView senderImage;
        ImageView simImage;
        ImageView statusImage;
    }
}