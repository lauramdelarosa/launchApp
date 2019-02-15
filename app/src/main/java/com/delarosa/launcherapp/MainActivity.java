package com.delarosa.launcherapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.delarosa.launcherapp.utils.PreferencesAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String ARKBOX_LAUNCHER_FILTER = "LauncherDate";
    private static final ComponentName LAUNCHER_COMPONENT_NAME = new ComponentName("com.delarosa.launcherapp", "co.arkbox.launcher.Launcher");
    public static int OUTGOING_CALLS_REQUEST_CODE = 1;
    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 2;
    boolean booleanPermissionOutgoingCalls;
    boolean booleanPermissionOverlay;
    ProgressDialog progressDialog;
    private Button btnHideApp, btn_enable;
    private PreferencesAdapter preferencesAdapter;
    private boolean launcherServiceBound = false;
    private TextView currentDateandTime;

    /**
     * broadcast receiver escucha cuando el servicio @seeBeatLauncherService envia datos
     */
    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentDateandTime.setText(intent.getStringExtra("currentDateandTime"));
        }
    };

    /**
     * Messenger para comunicacion con LauncherService.
     */
    private Messenger launcherServiceMessenger = null;

    /**
     * Interaccion con interfaz de LauncherService.
     */
    private final ServiceConnection launcherServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            launcherServiceMessenger = new Messenger(service);
            launcherServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            launcherServiceMessenger = null;
            launcherServiceBound = false;
        }
    };

    /**
     * vincula el servicio AudioService
     */
    private void bindAudioService(Context context) {

        try {
            Intent intent = new Intent(context, BeatLauncherService.class);
            context.bindService(intent, launcherServiceConnection, Context.BIND_AUTO_CREATE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(ARKBOX_LAUNCHER_FILTER));
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(ARKBOX_LAUNCHER_FILTER));
        } catch (Exception e) {
            e.printStackTrace();
        }


        btnHideApp = findViewById(R.id.btn_hide);
        btnHideApp.setOnClickListener(this);
        btn_enable = findViewById(R.id.btn_enable);
        currentDateandTime = findViewById(R.id.currentDateandTime);
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Alert");
        progressDialog.setMessage("Please wait");
       /* preferencesAdapter = new PreferencesAdapter(MainActivity.this);
        preferencesAdapter.setPreferenceBoolean("ENABLESERVICE", false);*/

        if (!launcherServiceBound) {
            bindAudioService(MainActivity.this);
        }
        // se valida la version del sdk para soporte de barra transparente
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        changeStatusBarColor();

        //valida si la app esta visible en el menu
        if (isLauncherIconVisible()) {
            btnHideApp.setText(R.string.hide_app);
        } else {
            btnHideApp.setText(R.string.unhide_app);
        }

        askForCallPermission();
    }

    /**
     * habilita o deshabilita el servicio de lanzar la app arkbox y manda el resultado al servicio
     *
     * @param view
     */
    public void enableService(View view) {
        final String text;
        Bundle bundle = new Bundle();

        if (btn_enable.getText().equals("Habilitar")) {
            text = "Deshabilitar";
            bundle.putBoolean("launcherEnable", true);
        } else {
            text = "Habilitar";
            bundle.putBoolean("launcherEnable", false);
        }

        android.os.Message msg = android.os.Message.obtain(null, BeatLauncherService.ARKBOX_LAUNCHER_ENABLE, 0, 0);
        msg.setData(bundle);
        try {
            launcherServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        btn_enable.setText(text);
    }

    @Override
    protected void onDestroy() {
        //destruye la instancia del broadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    /**
     * este metodo cambia el color de la barra de notificacion a transparente
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * listener del boton (esconder App) valida el permiso de llamadas, cambia el nombre del boton y esconde o muestra la app
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_hide:

                progressDialog.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if (isLauncherIconVisible()) {
                            btnHideApp.setText(R.string.hide_app);
                        } else {
                            btnHideApp.setText(R.string.unhide_app);
                        }
                    }
                }, 10000);


                if (booleanPermissionOutgoingCalls) {

                    if (isLauncherIconVisible()) {
                        hideAppIcon();
                    } else {
                        unhideAppIcon();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please allow the permission", Toast.LENGTH_LONG).show();
                }
                break;

        }

    }

    /**
     * este metodo valida si la app es visible o no
     *
     * @return
     */
    private boolean isLauncherIconVisible() {
        try {
            int enabledSetting = getPackageManager().getComponentEnabledSetting(LAUNCHER_COMPONENT_NAME);
            return enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * este metodo oculta la app del menu de aplicaciones
     */
    private void hideAppIcon() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Importante!");
        builder.setMessage("esta seguro de que desea ocultar la app");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                getPackageManager().setComponentEnabledSetting(LAUNCHER_COMPONENT_NAME, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        });
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }

    /**
     * este metodo vuelve a mostrar la app en el menu
     */
    private void unhideAppIcon() {
        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        p.setComponentEnabledSetting(LAUNCHER_COMPONENT_NAME, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * pregunta por el permiso de llamada
     */
    private void askForCallPermission() {
        if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.PROCESS_OUTGOING_CALLS) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.PROCESS_OUTGOING_CALLS) != PackageManager.PERMISSION_GRANTED)) {

            if (!(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.PROCESS_OUTGOING_CALLS))) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS}, OUTGOING_CALLS_REQUEST_CODE);
            }


        } else {
            booleanPermissionOutgoingCalls = true;

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            }
        } else {
            booleanPermissionOverlay = true;
        }
    }

    /**
     * resultado del permiso de llamada
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == OUTGOING_CALLS_REQUEST_CODE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                booleanPermissionOutgoingCalls = true;


            } else {
                Toast.makeText(getApplicationContext(), "Please allow the permission", Toast.LENGTH_LONG).show();

            }
        }
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                booleanPermissionOverlay = true;


            } else {
                Toast.makeText(getApplicationContext(), "Please allow the permission", Toast.LENGTH_LONG).show();

            }
        }
    }

}