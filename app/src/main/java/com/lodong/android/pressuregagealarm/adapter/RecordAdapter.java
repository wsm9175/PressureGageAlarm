package com.lodong.android.pressuregagealarm.adapter;

import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.entity.EventEntity;
import com.lodong.android.pressuregagealarm.view.SettingAddressBookActivity;

import java.util.Date;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
    private List<EventEntity> mList;

    public RecordAdapter() {}

    public void setmList(List<EventEntity> mList) {
        this.mList = mList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView txtTime, txtContent;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTime = itemView.findViewById(R.id.txt_time);
            txtContent = itemView.findViewById(R.id.txt_content);
        }

        public void onBind(EventEntity entity){
            long occurTime = entity.getOccurTime();
            String displayTime = getTime(occurTime);
            String contents = entity.getEvent();

            txtTime.setText(displayTime);
            txtContent.setText(contents);
        }
    }

    @NonNull
    @Override
    public RecordAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordAdapter.ViewHolder holder, int position) {
        holder.onBind(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    private String getTime(long time) {
        Date mDate;
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        mDate = new Date(time);
        return mFormat.format(mDate);
    }
}
