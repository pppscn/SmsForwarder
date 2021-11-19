package com.idormy.sms.forwarder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.AppInfo;

import java.util.List;

public class AppAdapter extends ArrayAdapter<AppInfo> {
    private final int resourceId;
    private List<AppInfo> list;

    // 适配器的构造函数，把要适配的数据传入这里
    public AppAdapter(Context context, int textViewResourceId, List<AppInfo> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
        list = objects;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        AppInfo item = list.get(position);
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AppInfo appInfo = getItem(position); //获取当前项的TLog实例

        // 加个判断，以免ListView每次滚动时都要重新加载布局，以提高运行效率
        View view;
        AppAdapter.ViewHolder viewHolder;
        if (convertView == null) {

            // 避免ListView每次滚动时都要重新加载布局，以提高运行效率
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);

            // 避免每次调用getView()时都要重新获取控件实例
            viewHolder = new AppAdapter.ViewHolder();
            viewHolder.appName = view.findViewById(R.id.appName);
            viewHolder.pkgName = view.findViewById(R.id.pkgName);
            viewHolder.appIcon = view.findViewById(R.id.appIcon);
            viewHolder.verName = view.findViewById(R.id.verName);
            viewHolder.verCode = view.findViewById(R.id.verCode);

            // 将ViewHolder存储在View中（即将控件的实例存储在其中）
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (AppAdapter.ViewHolder) view.getTag();
        }

        // 获取控件实例，并调用set...方法使其显示出来
        if (appInfo != null) {
            viewHolder.appName.setText(appInfo.getAppName());
            viewHolder.pkgName.setText(appInfo.getPkgName());
            viewHolder.appIcon.setBackground(appInfo.getAppIcon());
            viewHolder.verName.setText(appInfo.getVerName());
            viewHolder.verCode.setText(appInfo.getVerCode() + "");
        }

        return view;
    }

    public void add(List<AppInfo> appModels) {
        if (list != null) {
            list = appModels;
            notifyDataSetChanged();
        }
    }

    public void del(List<AppInfo> appModels) {
        if (list != null) {
            list = appModels;
            notifyDataSetChanged();
        }
    }

    public void update(List<AppInfo> appModels) {
        if (list != null) {
            list = appModels;
            notifyDataSetChanged();
        }
    }

    // 定义一个内部类，用于对控件的实例进行缓存
    static class ViewHolder {
        TextView appName;
        TextView pkgName;
        ImageView appIcon;
        TextView verName;
        TextView verCode;
    }
}
