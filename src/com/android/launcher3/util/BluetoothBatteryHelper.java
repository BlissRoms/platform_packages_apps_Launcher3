/*
 * Copyright (C) 2026 The BlissRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package com.android.launcher3.util;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BluetoothBatteryHelper {

    private static final String TAG = "BluetoothBatteryHelper";

    public static final int TYPE_HEADPHONE = 0;
    public static final int TYPE_WATCH = 1;
    public static final int TYPE_SPEAKER = 2;

    public static class BtDeviceInfo {
        public final String name;
        public final String address;
        public final int deviceType;
        public final int batteryLevel;

        public BtDeviceInfo(String name, String address, int deviceType, int batteryLevel) {
            this.name = name;
            this.address = address;
            this.deviceType = deviceType;
            this.batteryLevel = batteryLevel;
        }
    }

    public interface OnBluetoothDevicesChangedListener {
        void onBluetoothDevicesChanged();
    }

    private final Handler mHandler;
    private final Map<String, BtDeviceInfo> mDevices = new LinkedHashMap<>();
    private OnBluetoothDevicesChangedListener mListener;
    private boolean mRegistered = false;
    private Context mContext;

    private BluetoothA2dp mA2dpProxy;
    private BluetoothHeadset mHeadsetProxy;

    private final BluetoothProfile.ServiceListener mProfileListener =
            new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                mA2dpProxy = (BluetoothA2dp) proxy;
            } else if (profile == BluetoothProfile.HEADSET) {
                mHeadsetProxy = (BluetoothHeadset) proxy;
            }
            scanConnectedDevices();
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                mA2dpProxy = null;
            } else if (profile == BluetoothProfile.HEADSET) {
                mHeadsetProxy = null;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !hasBluetoothConnectPermission()) return;
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    mDevices.clear();
                    notifyChanged();
                } else if (state == BluetoothAdapter.STATE_ON) {
                    BluetoothAdapter adapter = getAdapter(context);
                    if (adapter != null) {
                        adapter.getProfileProxy(context, mProfileListener,
                                BluetoothProfile.A2DP);
                        adapter.getProfileProxy(context, mProfileListener,
                                BluetoothProfile.HEADSET);
                    }
                }
                return;
            }

            if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)
                    || BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                scanConnectedDevices();
                return;
            }

            BluetoothDevice device = intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                if (device != null) addDeviceIfSupported(device);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                if (device != null) removeDevice(device);
            } else if ("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED".equals(action)) {
                if (device != null) updateDeviceBattery(device);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                if (device != null) {
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.BOND_NONE);
                    if (bondState == BluetoothDevice.BOND_NONE) {
                        removeDevice(device);
                    }
                }
            }
        }
    };

    public BluetoothBatteryHelper(Handler handler) {
        mHandler = handler;
    }

    public void setListener(OnBluetoothDevicesChangedListener listener) {
        mListener = listener;
    }

    public void register(Context context) {
        if (mRegistered) return;
        mContext = context.getApplicationContext();

        BluetoothAdapter adapter = getAdapter(context);
        if (adapter == null || !adapter.isEnabled()) return;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED");
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
        mRegistered = true;

        adapter.getProfileProxy(context, mProfileListener, BluetoothProfile.A2DP);
        adapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET);
    }

    public void unregister(Context context) {
        if (!mRegistered) return;
        try {
            context.unregisterReceiver(mReceiver);
        } catch (Exception ignored) {}
        mRegistered = false;

        BluetoothAdapter adapter = getAdapter(context);
        if (adapter != null) {
            if (mA2dpProxy != null) {
                adapter.closeProfileProxy(BluetoothProfile.A2DP, mA2dpProxy);
                mA2dpProxy = null;
            }
            if (mHeadsetProxy != null) {
                adapter.closeProfileProxy(BluetoothProfile.HEADSET, mHeadsetProxy);
                mHeadsetProxy = null;
            }
        }
        mDevices.clear();
    }

    public List<BtDeviceInfo> getConnectedDevices() {
        return Collections.unmodifiableList(new ArrayList<>(mDevices.values()));
    }

    public boolean hasConnectedDevices() {
        return !mDevices.isEmpty();
    }

    private boolean hasBluetoothConnectPermission() {
        return mContext != null
                && mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void scanConnectedDevices() {
        mDevices.clear();
        try {
            if (mA2dpProxy != null) {
                for (BluetoothDevice device : mA2dpProxy.getConnectedDevices()) {
                    addDeviceIfSupportedSilent(device);
                }
            }
            if (mHeadsetProxy != null) {
                for (BluetoothDevice device : mHeadsetProxy.getConnectedDevices()) {
                    addDeviceIfSupportedSilent(device);
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission", e);
            mDevices.clear();
        }
        notifyChanged();
    }

    private boolean addDeviceIfSupportedSilent(BluetoothDevice device) {
        int type = classifyDevice(device);
        if (type < 0) return false;
        int battery = getBatteryLevel(device);
        String name = device.getName();
        if (name == null || name.isEmpty()) name = device.getAddress();
        mDevices.put(device.getAddress(),
                new BtDeviceInfo(name, device.getAddress(), type, battery));
        return true;
    }

    private void addDeviceIfSupported(BluetoothDevice device) {
        if (addDeviceIfSupportedSilent(device)) {
            notifyChanged();
        }
    }

    private void removeDevice(BluetoothDevice device) {
        if (mDevices.remove(device.getAddress()) != null) {
            notifyChanged();
        }
    }

    private void updateDeviceBattery(BluetoothDevice device) {
        BtDeviceInfo existing = mDevices.get(device.getAddress());
        if (existing == null) return;
        int battery = getBatteryLevel(device);
        mDevices.put(device.getAddress(),
                new BtDeviceInfo(existing.name, existing.address, existing.deviceType, battery));
        notifyChanged();
    }

    private int classifyDevice(BluetoothDevice device) {
        BluetoothClass btClass = device.getBluetoothClass();
        if (btClass == null) return -1;
        int deviceClass = btClass.getDeviceClass();

        switch (deviceClass) {
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                return TYPE_HEADPHONE;
            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
            case BluetoothClass.Device.COMPUTER_WEARABLE:
                return TYPE_WATCH;
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
            case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
            case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                return TYPE_SPEAKER;
            default:
                if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE) {
                    return TYPE_WATCH;
                }
                return -1;
        }
    }

    private int getBatteryLevel(BluetoothDevice device) {
        try {
            return device.getBatteryLevel();
        } catch (Exception e) {
            return -1;
        }
    }

    private BluetoothAdapter getAdapter(Context context) {
        BluetoothManager bm = context.getSystemService(BluetoothManager.class);
        return bm != null ? bm.getAdapter() : null;
    }

    private void notifyChanged() {
        if (mListener != null) {
            mHandler.post(() -> {
                if (mListener != null) mListener.onBluetoothDevicesChanged();
            });
        }
    }
}
