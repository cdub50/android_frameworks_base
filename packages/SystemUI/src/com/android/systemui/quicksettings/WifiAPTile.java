/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsController;

public class WifiAPTile extends QuickSettingsTile {

    private static WifiManager mWifiManager;
    public static WifiAPTile mInstance;

    public static QuickSettingsTile getInstance(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, final QuickSettingsController qsc, Handler handler) {
        if (mInstance == null) mInstance = new WifiAPTile(context, inflater, container, qsc);
        else {mInstance.updateTileState(); mInstance.updateQuickSettings(); qsc.registerAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION, mInstance);}
        return mInstance;
    }

    public WifiAPTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        updateTileState();
        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int state = mWifiManager.getWifiApState();
                switch (state) {
                    case WifiManager.WIFI_AP_STATE_ENABLING:
                    case WifiManager.WIFI_AP_STATE_ENABLED:
                        setSoftapEnabled(false);
                        break;
                    case WifiManager.WIFI_AP_STATE_DISABLING:
                    case WifiManager.WIFI_AP_STATE_DISABLED:
                        setSoftapEnabled(true);
                        break;
                }
            }
        };
        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                startSettingsActivity(intent);
                return true;
            }
        };
        qsc.registerAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION, this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        updateTileState();
        updateQuickSettings();
    }

    private void updateTileState() {
        int state = mWifiManager.getWifiApState();
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
            case WifiManager.WIFI_AP_STATE_ENABLED:
                mLabel = mContext.getString(R.string.quick_settings_label_enabled);
                mDrawable = R.drawable.ic_qs_wifi_ap_on;
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
            case WifiManager.WIFI_AP_STATE_DISABLED:
            default:
                mDrawable = R.drawable.ic_qs_wifi_ap_off;
                mLabel = mContext.getString(R.string.quick_settings_label_disabled);
                break;
        }
    }

    private void setSoftapEnabled(boolean enable) {
        final ContentResolver cr = mContext.getContentResolver();
        /**
         * Disable Wifi if enabling tethering
         */
        int wifiState = mWifiManager.getWifiState();
        if (enable && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                    (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
            mWifiManager.setWifiEnabled(false);
            Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 1);
        }

        // Turn on the Wifi AP
        mWifiManager.setWifiApEnabled(null, enable);

        /**
         *  If needed, restore Wifi on tether disable
         */
        if (!enable) {
            int wifiSavedState = 0;
            try {
                wifiSavedState = Settings.Global.getInt(cr, Settings.Global.WIFI_SAVED_STATE);
            } catch (Settings.SettingNotFoundException e) {
                // Do nothing here
            }
            if (wifiSavedState == 1) {
                mWifiManager.setWifiEnabled(true);
                Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 0);
            }
        }
    }

}
