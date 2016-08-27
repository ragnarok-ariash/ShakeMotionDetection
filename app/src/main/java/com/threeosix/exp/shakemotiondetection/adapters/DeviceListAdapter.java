package com.threeosix.exp.shakemotiondetection.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.threeosix.exp.shakemotiondetection.R;
import com.threeosix.exp.shakemotiondetection.interfaces.ControlDeviceListener;
import com.threeosix.exp.shakemotiondetection.models.DeviceModel;

import java.util.ArrayList;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by taufik on 8/28/16.
 */
public class DeviceListAdapter extends BaseAdapter {
    private ArrayList<DeviceModel> devices;
    private Context context;
    private ControlDeviceListener callback;

    public DeviceListAdapter(Context context, ArrayList<DeviceModel> devices) {
        this.devices = devices;
    }

    public void setCallback(ControlDeviceListener callback) {
        this.callback = callback;
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
            convertView = View.inflate(parent.getContext(), R.layout.adapter_device_list, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else
            holder = (ViewHolder)convertView.getTag();

        holder.loadView((DeviceModel)getItem(position));

        return convertView;
    }

    class ViewHolder{
        @BindView(R.id.text_device_name)
        TextView tvDeviceName;

        @BindView(R.id.text_device_info)
        TextView tvDeviceInfo;

        @BindString(R.string.kind_motion)
        String motionType;
        @BindString(R.string.kind_toggle)
        String toggleType;

        private int deviceIdx;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }

        void loadView(DeviceModel device){
            deviceIdx = devices.indexOf(device);
            this.tvDeviceName.setText(device.getDeviceName());
            this.tvDeviceInfo.setText("pin: " + device.getDevicePin()
                    + " (" + device.getDeviceDegree() + ")" + (char) 0x00B0 + " | "
                    + (device.getDeviceKind() == DeviceModel.DeviceKind.MOTION ? motionType : toggleType));
        }

        @OnClick(R.id.icon_delete_control_device)
        void deleteControlDevice(){
            if (callback!=null){
                callback.removeControlledDevice(devices.get(deviceIdx));
            }
        }
    }
}
