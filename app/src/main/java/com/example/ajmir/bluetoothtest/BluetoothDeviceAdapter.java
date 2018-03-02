package com.example.ajmir.bluetoothtest;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    List<BluetoothDeviceData> mDataset;

    public BluetoothDeviceAdapter() {
        clear();
    }

    public void addItem(BluetoothDeviceData data) {
        mDataset.add(data);
        notifyItemInserted(mDataset.size());
    }

    public void clear() {
        mDataset = new ArrayList<>(10);
        notifyDataSetChanged();
    }

    // region RecyclerView.Adapter

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BluetoothDeviceData data = mDataset.get(position);
        holder.textView.setText(data.bleDevice.getName() + " - " + data.bleDevice.getMacAddress());
        holder.itemView.setOnClickListener(data.onClickListener);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // endregion

    // region ViewHolder

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.bluetooth_device_item_text_view)
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    // endregion
}
