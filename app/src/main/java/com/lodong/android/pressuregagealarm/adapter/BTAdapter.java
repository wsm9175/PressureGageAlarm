package com.lodong.android.pressuregagealarm.adapter;

import static android.graphics.Color.parseColor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.viewmodel.MainViewModel;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class BTAdapter extends RecyclerView.Adapter<BTAdapter.ViewHolder> {
    private ArrayList<BluetoothDevice> mList;
    private MainViewModel.BluetoothDeviceClickListener clickListener;
    private int rowIndex;

    public BTAdapter(MainViewModel.BluetoothDeviceClickListener clickListener){
        this.clickListener = clickListener;
    }

    public void setMList(ArrayList<BluetoothDevice> mList){
        this.mList = mList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView txtName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_name);
        }

        @SuppressLint("MissingPermission")
        public void onBind(BluetoothDevice bluetoothDevice, int position){
            String name = bluetoothDevice.getName();
            String address = bluetoothDevice.getAddress();

            txtName.setText(name);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onClick(address);

                }
            });

            itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        v.setBackgroundColor(Color.parseColor("#f0f0f0"));
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    {
                        v.setBackgroundColor(Color.TRANSPARENT);
                    }
                    return false;
                }
            });

        }
    }

    @NonNull
    @Override
    public BTAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BTAdapter.ViewHolder holder, int position) {
        holder.onBind(mList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }
}
