package com.delarosa.launcherapp;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.delarosa.launcherapp.utils.PreferencesAdapter;
import com.delarosa.launcherapp.utils.Utils;


/**
 * esta clase es un broadcastReceiver del teclado del telefono... cuando el usuario digite la clave secreta muestra la app escondida
 */
public class LaunchAppReceiver extends BroadcastReceiver {
    private static final ComponentName LAUNCHER_COMPONENT_NAME = new ComponentName("com.delarosa.launcherapp", "co.arkbox.launcher.Launcher");
    PreferencesAdapter preferencesAdapter;

    @Override
    public void onReceive(Context context, Intent intent) {
        String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        String hashPhoneNumber = Utils.sha256(phoneNumber);

        preferencesAdapter = new PreferencesAdapter(context);
        String LAUNCHER_NUMBER = preferencesAdapter.getPreferenceString("LAUNCHER_NUMBER");

        if (LAUNCHER_NUMBER.equals(hashPhoneNumber)) {
            setResultData(null);

            if (!isLauncherIconVisible(context)) {
                Intent appIntent = new Intent(context, MainActivity.class);
                appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(appIntent);
            }


        }

    }

    /**
     * valida si el icono esta visible en el menu de apps
     * @param context
     * @return
     */
    private boolean isLauncherIconVisible(Context context) {
        int enabledSetting = context.getPackageManager().getComponentEnabledSetting(LAUNCHER_COMPONENT_NAME);
        return enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

}