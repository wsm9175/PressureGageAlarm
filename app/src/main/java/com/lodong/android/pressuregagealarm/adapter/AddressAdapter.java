package com.lodong.android.pressuregagealarm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.view.SettingAddressBookActivity;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
    private List<String> mList;
    private SettingAddressBookActivity.AddressLongClickListener listener;

    public AddressAdapter(SettingAddressBookActivity.AddressLongClickListener listener) {
        this.listener = listener;
    }

    public void setmList(List<String> mList) {
        this.mList = mList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView txtAddress;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtAddress = itemView.findViewById(R.id.txt_address);
        }

        public void onBind(String address, int position){
            txtAddress.setText(address);
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    listener.onLongClick(position);
                    return false;
                }
            });
        }
    }

    @NonNull
    @Override
    public AddressAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressAdapter.ViewHolder holder, int position) {
        holder.onBind(mList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }
}
