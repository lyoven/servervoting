package android.servervoting;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MobileConnection {

    public static void set(Context context, boolean enabled) throws Exception {
        // Requires: android.permission.CHANGE_NETWORK_STATE
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // Gingerbread to KitKat inclusive
            final Field serviceField = connMgr.getClass().getDeclaredField("mService");
            serviceField.setAccessible(true);
            final Object connService = serviceField.get(connMgr);
            try {
                final Method setMobileDataEnabled = connService.getClass()
                        .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabled.setAccessible(true);
                setMobileDataEnabled.invoke(connService, Boolean.valueOf(enabled));
            } catch (NoSuchMethodException e) {
                // Support for CyanogenMod 11+
                final Method setMobileDataEnabled = connService.getClass()
                        .getDeclaredMethod("setMobileDataEnabled", String.class, Boolean.TYPE);
                setMobileDataEnabled.setAccessible(true);
                setMobileDataEnabled.invoke(connService, context.getPackageName(), Boolean.valueOf(enabled));
            }
        } else {
            throw new UnsupportedOperationException("Unsupported Android version. Only supports below 'Lollipop'.");
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        Log.d("isMobileConnected", "mobile network is currently: " + networkInfo.isConnected());
        return networkInfo.isConnected();
    }

}
