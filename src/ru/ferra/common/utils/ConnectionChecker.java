package ru.ferra.common.utils;

import ru.ferra.R;
import ru.ferra.ui.Application;
import ru.ferra.ui.Settings;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public class ConnectionChecker {
    public static boolean isNetworkActive(Context context) {
        NetworkInfo localNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        return (localNetworkInfo != null) && localNetworkInfo.isConnected();
    }

    public static boolean isWiFiActive(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifi.isWifiEnabled();
    }

    public static boolean isNetworkAvailable(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isWiFiOnly = settings.getBoolean(Settings.SETTINGS_WIFI_USE, false);

        if (!isNetworkActive(context)) {
        	showNetworkError(R.string.network_error);

            return false;
        }
        if (isWiFiOnly && !isWiFiActive(context)) {
        	showNetworkError(R.string.wifi_error);

            return false;
        }
        
        return true;
    }

    public static void resetLastError(){
    	Application.resetLastError();
    }

    private static void showNetworkError(int idErrorMessage){
    		Application.showError(idErrorMessage);
    }
}
