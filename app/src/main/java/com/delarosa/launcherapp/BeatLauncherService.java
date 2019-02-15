package com.delarosa.launcherapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import org.apache.log4j.Logger;


import com.delarosa.launcherapp.utils.AxTimer;
import com.delarosa.launcherapp.utils.PreferencesAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.content.ContentValues.TAG;

public class BeatLauncherService extends Service {

    /**
     * por medio de esta variable se actualiza @seelauncherEnable
     */
    public static final int ARKBOX_LAUNCHER_ENABLE = 2;
    /**
     * por medio de esta variable llegan los datos del beat
     */
    private static final int ARKBOX_WATCHER_BEAT = 1;
    /**
     * escribe en un archivo log
     */
    private static Logger logger;
    final Messenger launcherMessenger = new Messenger(new IncomingHandler(this));
    /**
     * Revisa la actividad de Arkbox, es decir, cuando Arkbox está visible. En caso de que no lo esté
     * este timer se encargará de fijar la bandera arkboxActive con el fin de indicar el estado
     */
    private final AxTimer arkboxActivityCheckerTimer;
    private final AxTimer idleTimeCheckerTimer;
    private PreferencesAdapter preferencesAdapter;
    //region variables del beat
    private boolean lockSettings = false;
    private String lockPassword;
    private Boolean autoLaunch = false;
    private int idleTime;
    /**
     * indica el estado del boton de habilitar o deshabilitar el lanzamiento de la app arkbox
     */
    private boolean launcherEnable = false;
    //endregion
    /**
     * Indica el estado de visibilidad de la app Arkbox
     */
    private boolean arkboxActive = false;

    /**
     * observer que lanza arkbox.
     */
    private Observer idleTimerObserver = new Observer() {
        @Override
        public void update(Observable observable, Object o) {
            if (autoLaunch) {
                launchArkbox();
            }
        }
    };

    /**
     * constructor del servicio, aqui se inicializan las variables requeridas
     */
    public BeatLauncherService() {
        logger = Logger.getLogger(BeatLauncherService.class);
        arkboxActivityCheckerTimer = new AxTimer();
        idleTimeCheckerTimer = new AxTimer();
    }

    /**
     * metodo lanza arkbox siempre y cuando este inactivo
     */
    private void launchArkbox() {

        try {
            try {
                //Sólo se lanza la actividad cuando se detecta inactividad y esta habilitada la funcion para lanzar la app
                if (!arkboxActive && launcherEnable) {
                    saveLastLauncherDate();
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("co.arkbox");
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launchIntent);
                    }

                    arkboxActive = true;
                    arkboxActivityCheckerTimer.start();

                    logger.info("Arkbox launched!");
                }
            } catch (Exception ex) {
                logger.error("Error trying to opening Arkbox", ex);

            }

        } catch (Exception localException) {
            logger.error("", localException);
        }

    }

    /**
     * guarda la ultima fecha de lanzamiento de la app Arkbox
     */
    private void saveLastLauncherDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String currentDateAndTime = sdf.format(new Date());
        preferencesAdapter.setPreferenceString("currentDateandTime", currentDateAndTime);
        sendDateBroadcast(currentDateAndTime);
    }

    /**
     * conecta el servicio con el que lo necesite y obtiene una instancia de la conexion.
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        return launcherMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        arkboxActivityCheckerTimer.setPeriod(10000);
        arkboxActivityCheckerTimer.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                Log.i(TAG, "Arkbox inactive");
                arkboxActive = false;
            }
        });
        arkboxActivityCheckerTimer.start();


        idleTimeCheckerTimer.setPeriod(10000);
        idleTimeCheckerTimer.addObserver(idleTimerObserver);
        idleTimeCheckerTimer.start();

        Log.d("BeatLauncherService", "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("BeatLauncherService", "onDestroy");
    }

    /**
     * envia la fecha a un broadcast que lo recibe MainActivity para mostrar la fecha
     *
     * @param currentDateandTime
     */
    private void sendDateBroadcast(String currentDateandTime) {
        Intent intent = new Intent("LauncherDate");
        intent.putExtra("currentDateandTime", currentDateandTime);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("BeatLauncherService", "sendDateBroadcast");
    }

    class IncomingHandler extends Handler {
        Context context;

        public IncomingHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ARKBOX_WATCHER_BEAT:
                    arkboxActive = true;
                    Bundle data = msg.getData();
                    lockSettings = data.getBoolean("LockSettings");
                    lockPassword = data.getString("LockPassword");
                    autoLaunch = data.getBoolean("AutoLaunch");
                    idleTime = data.getInt("IdleTime");
                    preferencesAdapter = new PreferencesAdapter(context);
                    preferencesAdapter.setPreferenceString("LAUNCHER_NUMBER", lockPassword);
                    break;
                case ARKBOX_LAUNCHER_ENABLE:
                    Bundle dataEnable = msg.getData();
                    launcherEnable = dataEnable.getBoolean("launcherEnable");
                    break;
                default:
                    super.handleMessage(msg);

            }
        }

    }
}