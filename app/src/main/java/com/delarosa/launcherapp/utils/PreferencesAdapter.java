package com.delarosa.launcherapp.utils;


import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesAdapter {

    Context mContext;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    public PreferencesAdapter(Context context) {

        mContext = context;
        preferences = mContext.getSharedPreferences("configuracion", Context.MODE_PRIVATE);
    }

    public String getPreferenceString(String key) {
        String valor = preferences.getString(key, "");
        return valor;
    }

    public boolean getPreferenceBoolean(String key) {
        boolean valor = preferences.getBoolean(key, false);
        return valor;
    }


    public void setPreferenceString(String key, String valor) {
        editor = preferences.edit();
        editor.putString(key, valor);
        editor.commit();

    }

    public void setPreferenceBoolean(String key, boolean valor) {
        editor = preferences.edit();
        editor.putBoolean(key, valor);
        editor.commit();

    }

}