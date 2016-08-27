package com.threeosix.exp.shakemotiondetection.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.threeosix.exp.shakemotiondetection.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by taufik on 8/27/16.
 */
public class BluetoothListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> devices;
    private Context context;

    public BluetoothListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(parent.getContext(), R.layout.layout_simple_text, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else
            holder = (ViewHolder)convertView.getTag();

        holder.loadView((BluetoothDevice)getItem(position));

        return convertView;
    }

    class ViewHolder{
        @BindView(R.id.text)
        TextView simpleTextView;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }

        void loadView(BluetoothDevice device){
            this.simpleTextView.setText(device.getName()+"("+device.getAddress()+")");
        }
    }


}
