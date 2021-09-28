package com.idormy.sms.forwarder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.SenderModel;

import java.util.List;

@SuppressWarnings("unused")
public class SenderAdapter extends ArrayAdapter<SenderModel> {
    private final int resourceId;
    private List<SenderModel> list;

    // 适配器的构造函数，把要适配的数据传入这里
    public SenderAdapter(Context context, int textViewResourceId, List<SenderModel> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
        list = objects;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public SenderModel getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        SenderModel item = list.get(position);
        if (item == null) {
            return 0;
        }
        return item.getId();
    }

    // convertView 参数用于将之前加载好的布局进行缓存
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SenderModel senderModel = getItem(position); //获取当前项的TLog实例

        // 加个判断，以免ListView每次滚动时都要重新加载布局，以提高运行效率
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {

            // 避免ListView每次滚动时都要重新加载布局，以提高运行效率
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);

            // 避免每次调用getView()时都要重新获取控件实例
            viewHolder = new ViewHolder();
            viewHolder.senderImage = view.findViewById(R.id.sender_image);
            viewHolder.senderName = view.findViewById(R.id.sender_name);

            // 将ViewHolder存储在View中（即将控件的实例存储在其中）
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        // 获取控件实例，并调用set...方法使其显示出来
        if (senderModel != null) {
            viewHolder.senderImage.setImageResource(senderModel.getImageId());
            viewHolder.senderName.setText(senderModel.getName());
        }

        return view;
    }

    public void add(SenderModel senderModel) {
        if (list != null) {
            list.add(senderModel);
            notifyDataSetChanged();
        }
    }

    public void del(int position) {
        if (list != null) {
            list.remove(position);
            notifyDataSetChanged();
        }
    }

    public void update(SenderModel senderModel, int position) {
        if (list != null) {
            list.set(position, senderModel);
            notifyDataSetChanged();
        }
    }

    public void add(List<SenderModel> senderModels) {
        if (list != null) {
            list = senderModels;
            notifyDataSetChanged();
        }
    }

    public void del(List<SenderModel> senderModels) {
        if (list != null) {
            list = senderModels;
            notifyDataSetChanged();
        }
    }

    public void update(List<SenderModel> senderModels) {
        if (list != null) {
            list = senderModels;
            notifyDataSetChanged();
        }
    }

    // 定义一个内部类，用于对控件的实例进行缓存
    static class ViewHolder {
        ImageView senderImage;
        TextView senderName;
    }

}