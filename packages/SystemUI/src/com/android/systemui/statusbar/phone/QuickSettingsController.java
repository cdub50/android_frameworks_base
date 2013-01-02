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

package com.android.systemui.statusbar.phone;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.systemui.quicksettings.QuickSettingsTile;
import com.android.systemui.quicksettings.FavoriteContactTile;
import com.android.systemui.quicksettings.InputMethodTile;

import dalvik.system.DexClassLoader;

public class QuickSettingsController {
    private static String TAG = "QuickSettingsController";

    // Stores the broadcast receivers and content observers
    // quick tiles register for.
    public HashMap<String, ArrayList<QuickSettingsTile>> mReceiverMap
        = new HashMap<String, ArrayList<QuickSettingsTile>>();
    public HashMap<Uri, ArrayList<QuickSettingsTile>> mObserverMap
        = new HashMap<Uri, ArrayList<QuickSettingsTile>>();

    /**
     * START OF DATA MATCHING BLOCK
     *
     * THE FOLLOWING DATA MUST BE KEPT UP-TO-DATE WITH THE DATA IN
     * com.android.settings.cyanogenmod.QuickSettingsUtil IN THE
     * Settings PACKAGE.
     */
    public static final String TILE_AIRPLANE = "toggleAirplane";
    public static final String TILE_ALARM = "toggleAlarm";
    public static final String TILE_AUTOROTATE = "toggleAutoRotate";
    public static final String TILE_BATTERY = "toggleBattery";
    public static final String TILE_BLUETOOTH = "toggleBluetooth";
    public static final String TILE_BRIGHTNESS = "toggleBrightness";
    public static final String TILE_BUGREPORT = "toggleBugreport";
    public static final String TILE_FAVCONTACT = "toggleFavoriteContact";
    public static final String TILE_FCHARGE = "toggleFCharge";
    public static final String TILE_GPS = "toggleGPS";
    public static final String TILE_IME = "toggleIME";
    public static final String TILE_LOCKSCREEN = "toggleLockScreen";
    public static final String TILE_MOBILEDATA = "toggleMobileData";
    public static final String TILE_MOBILENETWORK = "toggleMobileNetwork";
    public static final String TILE_NETWORKMODE = "toggleNetworkMode";
    public static final String TILE_NFC = "toggleNfc";
    public static final String TILE_PROFILE = "toggleProfile";
    public static final String TILE_REBOOT = "toggleReboot";
    public static final String TILE_RINGER = "toggleSound";
    public static final String TILE_SCREENTIMEOUT = "toggleScreenTimeout";
    public static final String TILE_SETTINGS = "toggleSettings";
    public static final String TILE_SLEEP = "toggleSleepMode";
    public static final String TILE_SYNC = "toggleSync";
    public static final String TILE_TORCH = "toggleFlashlight";  // Keep old string for compatibility
    public static final String TILE_USER = "toggleUser";
    public static final String TILE_WIFI = "toggleWifi";
    public static final String TILE_WIFIAP = "toggleWifiAp";
    public static final String TILE_WIFIDISPLAY = "toggleWifiDisplay";
    // not yet supported
    public static final String TILE_LTE = "toggleLte";
    public static final String TILE_WIMAX = "toggleWimax";

    // Keep sorted according to titleResId's string value
    public static final LinkedHashMap<String, String> TILES_CLASSES = new LinkedHashMap<String, String>();
    static {
        TILES_CLASSES.put(TILE_AIRPLANE, "com.android.systemui.quicksettings.AirplaneModeTile");
        TILES_CLASSES.put(TILE_ALARM, "com.android.systemui.quicksettings.AlarmTile");
        TILES_CLASSES.put(TILE_AUTOROTATE, "com.android.systemui.quicksettings.AutoRotateTile");
        TILES_CLASSES.put(TILE_BATTERY, "com.android.systemui.quicksettings.BatteryTile");
        TILES_CLASSES.put(TILE_BLUETOOTH, "com.android.systemui.quicksettings.BluetoothTile");
        TILES_CLASSES.put(TILE_BRIGHTNESS, "com.android.systemui.quicksettings.BrightnessTile");
        TILES_CLASSES.put(TILE_BUGREPORT, "com.android.systemui.quicksettings.BugReportTile");
        TILES_CLASSES.put(TILE_FAVCONTACT, "com.android.systemui.quicksettings.FavoriteContactTile");
        TILES_CLASSES.put(TILE_FCHARGE, "com.android.systemui.quicksettings.FChargeTile");
        TILES_CLASSES.put(TILE_GPS, "com.android.systemui.quicksettings.GPSTile");
        TILES_CLASSES.put(TILE_IME, "com.android.systemui.quicksettings.InputMethodTile");
        TILES_CLASSES.put(TILE_LOCKSCREEN, "com.android.systemui.quicksettings.ToggleLockscreenTile");
        TILES_CLASSES.put(TILE_MOBILEDATA, "com.android.systemui.quicksettings.MobileDataTile");
        TILES_CLASSES.put(TILE_MOBILENETWORK, "com.android.systemui.quicksettings.MobileNetworkTile");
        TILES_CLASSES.put(TILE_NETWORKMODE, "com.android.systemui.quicksettings.MobileNetworkTypeTile");
        TILES_CLASSES.put(TILE_NFC, "com.android.systemui.quicksettings.NfcTile");
        TILES_CLASSES.put(TILE_PROFILE, "com.android.systemui.quicksettings.ProfileTile");
        TILES_CLASSES.put(TILE_REBOOT, "com.android.systemui.quicksettings.RebootTile");
        TILES_CLASSES.put(TILE_RINGER, "com.android.systemui.quicksettings.RingerModeTile");
        TILES_CLASSES.put(TILE_SCREENTIMEOUT, "com.android.systemui.quicksettings.ScreenTimeoutTile");
        TILES_CLASSES.put(TILE_SETTINGS, "com.android.systemui.quicksettings.PreferencesTile");
        TILES_CLASSES.put(TILE_SLEEP, "com.android.systemui.quicksettings.SleepScreenTile");
        TILES_CLASSES.put(TILE_SYNC, "com.android.systemui.quicksettings.SyncTile");
        TILES_CLASSES.put(TILE_TORCH, "com.android.systemui.quicksettings.TorchTile");
        TILES_CLASSES.put(TILE_USER, "com.android.systemui.quicksettings.UserTile");
        TILES_CLASSES.put(TILE_WIFI, "com.android.systemui.quicksettings.WiFiTile");
        TILES_CLASSES.put(TILE_WIFIAP, "com.android.systemui.quicksettings.WifiAPTile");
        TILES_CLASSES.put(TILE_WIFIDISPLAY, "com.android.systemui.quicksettings.WiFiDisplayTile");
    }

    private static final String TILE_DELIMITER = "|";
    private static final String TILES_DEFAULT = TILE_USER
            + TILE_DELIMITER + TILE_BRIGHTNESS
            + TILE_DELIMITER + TILE_SETTINGS
            + TILE_DELIMITER + TILE_WIFI
            + TILE_DELIMITER + TILE_MOBILENETWORK
            + TILE_DELIMITER + TILE_BATTERY
            + TILE_DELIMITER + TILE_AIRPLANE
            + TILE_DELIMITER + TILE_BLUETOOTH;
    /**
     * END OF DATA MATCHING BLOCK
     */
    static Class[] paramsTypes = {Context.class, LayoutInflater.class, QuickSettingsContainerView.class, QuickSettingsController.class, Handler.class};
    private final Context mContext;
    public PanelBar mBar;
    private final ViewGroup mContainerView;
    private final Handler mHandler;
    private BroadcastReceiver mReceiver;
    private ContentObserver mObserver;
    public PhoneStatusBar mStatusBarService;
    private String tiles;
    private ContentResolver resolver;
    private InputMethodTile IMETile;

    public QuickSettingsController(Context context, QuickSettingsContainerView container, PhoneStatusBar statusBarService) {
        mContext = context;
        mContainerView = container;
        mHandler = new Handler();
        mStatusBarService = statusBarService;
        resolver = mContext.getContentResolver();
    }

    void loadTiles() {
        // Read the stored list of tiles
        tiles = Settings.System.getString(resolver, Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            Log.i(TAG, "Default tiles being loaded");
            tiles = TILES_DEFAULT;
        }

        Log.i(TAG, "Tiles list: " + tiles);
        // Load the dynamic tiles
        // These toggles must be the last ones added to the view, as they will show
        // only when they are needed
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1) tiles += TILE_DELIMITER + TILE_ALARM;
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_BUGREPORT, 1) == 1) tiles += TILE_DELIMITER + TILE_BUGREPORT;
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_WIFI, 1) == 1) tiles += TILE_DELIMITER + TILE_WIFIDISPLAY;
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_IME, 1) == 1)  tiles += TILE_DELIMITER + TILE_IME;
    }

    private QuickSettingsTile createTile(boolean condition, String tile, LayoutInflater inflater) {
        QuickSettingsTile qs = null;
        if (condition){
           try{
               DexClassLoader classLoader = new DexClassLoader(
                       "/system/app/SystemUI.apk", mContext.getFilesDir().getAbsolutePath(),
                       null, getClass().getClassLoader());
               Class tileClass = classLoader.loadClass(TILES_CLASSES.get(tile));
               Method getInstance = tileClass.getMethod("getInstance", paramsTypes);
               Object[] args = {mContext, inflater,  (QuickSettingsContainerView) mContainerView, this, mHandler};
               qs = (QuickSettingsTile) getInstance.invoke(null, args);
           }
           catch(Exception e){
               Log.e(TAG, "Can't instanciate quick settings tile "+tile, e);
           }
        }
        return qs;
    }

    void addQuickSettings(LayoutInflater inflater){
        // Load the user configured tiles
        loadTiles();
        // reset fav contact instance counter
        if (!tiles.contains(TILE_FAVCONTACT)) FavoriteContactTile.resetContent(mContext);
        FavoriteContactTile.instanceCount = 0;
        // Split out the tile names and add to the list
        for (String tile : tiles.split("\\|")) {
            QuickSettingsTile qs = null;
            if (tile.equals(TILE_BLUETOOTH)) {
                qs = createTile(deviceSupportsBluetooth(), tile, inflater);
            }else if (tile.equals(TILE_WIFIAP) || tile.equals(TILE_MOBILENETWORK) || tile.equals(TILE_NETWORKMODE) || tile.equals(TILE_MOBILEDATA)) {
                qs = createTile(deviceSupportsTelephony(), tile, inflater);
            } else if (tile.equals(TILE_PROFILE)){
                qs = createTile(systemProfilesEnabled(resolver), tile, inflater);
            } else {
                qs = createTile(true, tile, inflater);
            }
            if (tile.equals(TILE_IME)) this.IMETile = (InputMethodTile) qs;
            if (qs != null) {
                qs.setupQuickSettingsTile();
            }
        }
        Log.e("\r\n\r\n"+TAG, "All tiles sucessfully created");
    }

    void setupQuickSettings() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        // Clear out old receiver
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        mReceiver = new QSBroadcastReceiver();
        mReceiverMap.clear();
        // Clear out old observer
        if (mObserver != null) {
            resolver.unregisterContentObserver(mObserver);
        }
        mObserver = new QuickSettingsObserver(mHandler);
        mObserverMap.clear();
        addQuickSettings(inflater);
        setupBroadcastReceiver();
        setupContentObserver();
    }

    void setupContentObserver() {
        for (Uri uri : mObserverMap.keySet()) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
    }

    private class QuickSettingsObserver extends ContentObserver {
        public QuickSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            for (QuickSettingsTile tile : mObserverMap.get(uri)) {
                tile.onChangeUri(resolver, uri);
            }
        }
    }

    void setupBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        for (String action : mReceiverMap.keySet()) {
            filter.addAction(action);
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    private void registerInMap(Object item, QuickSettingsTile tile, HashMap map) {
        if (map.keySet().contains(item)) {
            ArrayList list = (ArrayList) map.get(item);
            if (!list.contains(tile)) {
                list.add(tile);
            }
        } else {
            ArrayList<QuickSettingsTile> list = new ArrayList<QuickSettingsTile>();
            list.add(tile);
            map.put(item, list);
        }
    }

    public void registerAction(Object action, QuickSettingsTile tile) {
        registerInMap(action, tile, mReceiverMap);
    }

    public void registerObservedContent(Uri uri, QuickSettingsTile tile) {
        registerInMap(uri, tile, mObserverMap);
    }

    private class QSBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                for (QuickSettingsTile t : mReceiverMap.get(action)) {
                    t.onReceive(context, intent);
                }
            }
        }
    };

    boolean deviceSupportsTelephony() {
        PackageManager pm = mContext.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    boolean deviceSupportsBluetooth() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    boolean systemProfilesEnabled(ContentResolver resolver) {
        return (Settings.System.getInt(resolver, Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1);
    }

    void setBar(PanelBar bar) {
        mBar = bar;
    }

    public void setService(PhoneStatusBar phoneStatusBar) {
        mStatusBarService = phoneStatusBar;
    }

    public void setImeWindowStatus(boolean visible) {
        if (IMETile != null) {
            IMETile.toggleVisibility(visible);
        }
    }

    public void updateResources() {
        /*
         * this method is supposed to redraw all the tiles if needed
         * for future need. Commented out for now.
         * mContainerView.removeAllViews();
         * setupQuickSettings();
         * mContainerView.requestLayout();
         */
    }
}
