package com.idormy.sms.forwarder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.RuleModel;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.sender.SenderUtil;

import java.util.List;

public class RuleAdapter extends ArrayAdapter<RuleModel> {
    private final int resourceId;
    private List<RuleModel> list;

    // 适配器的构造函数，把要适配的数据传入这里
    public RuleAdapter(Context context, int textViewResourceId, List<RuleModel> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
        list = objects;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public RuleModel getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        RuleModel item = list.get(position);
        if (item == null) {
            return 0;
        }
        return item.getId();
    }

    // convertView 参数用于将之前加载好的布局进行缓存
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RuleModel ruleModel = getItem(position); //获取当前项的TLog实例

        // 加个判断，以免ListView每次滚动时都要重新加载布局，以提高运行效率
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {

            // 避免ListView每次滚动时都要重新加载布局，以提高运行效率
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);

            // 避免每次调用getView()时都要重新获取控件实例
            viewHolder = new ViewHolder();
            viewHolder.ruleMatch = view.findViewById(R.id.rule_match);
            viewHolder.ruleSender = view.findViewById(R.id.rule_sender);
            viewHolder.ruleSenderImage = view.findViewById(R.id.rule_sender_image);

            // 将ViewHolder存储在View中（即将控件的实例存储在其中）
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        // 获取控件实例，并调用set...方法使其显示出来
        if (ruleModel != null) {
            List<SenderModel> senderModel = SenderUtil.getSender(ruleModel.getSenderId(), null);
            viewHolder.ruleMatch.setText(ruleModel.getRuleMatch());
            if (!senderModel.isEmpty()) {
                viewHolder.ruleSender.setText(senderModel.get(0).getName());
                viewHolder.ruleSenderImage.setImageResource(senderModel.get(0).getImageId());
            } else {
                viewHolder.ruleSender.setText("");
            }
        }

        return view;
    }

    public void add(List<RuleModel> ruleModels) {
        if (list != null) {
            list = ruleModels;
            notifyDataSetChanged();
        }
    }

    public void del(List<RuleModel> ruleModels) {
        if (list != null) {
            list = ruleModels;
            notifyDataSetChanged();
        }
    }

    public void update(List<RuleModel> ruleModels) {
        if (list != null) {
            list = ruleModels;
            notifyDataSetChanged();
        }
    }

    // 定义一个内部类，用于对控件的实例进行缓存
    static class ViewHolder {
        TextView ruleMatch;
        TextView ruleSender;
        ImageView ruleSenderImage;
    }

}