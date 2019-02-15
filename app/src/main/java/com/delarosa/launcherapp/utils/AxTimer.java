package com.delarosa.launcherapp.utils;

import android.os.Handler;
import android.os.Looper;


import java.util.Observable;
import org.apache.log4j.Logger;

/**
 * Herramienta utilizada para el control de tareas periodicas
 */
public class AxTimer extends Observable {

    private static Logger logger = Logger.getLogger(AxTimer.class);

    private Handler handler = new Handler(Looper.getMainLooper());
    /**
     * Periodo por defecto
     */
    private long period = 1000;
    /**
     * Controlaor del hilo de ejecucion del periodo
     */
    private final Runnable timerTask = new Runnable() {
        @Override
        public void run() {

            synchronized (this) {

                try {
                    setChanged();
                    notifyObservers();
                } catch (SecurityException e) {
                    logger.error("", e);
                }


            }

            handler.postDelayed(timerTask, period);
        }

    };
    /**
     * Bandera que indica si el timer esta iniciado o no
     */
    private boolean started;


    public AxTimer() {

    }

    public AxTimer(int period) {

        this.period = period;

    }

    public AxTimer(int period, boolean autoStart) {
        this.period = period;
        if (autoStart) {
            start();
        }
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    /**
     * Inicia la ejecucion del timer
     */
    public void start() {
        handler.removeCallbacks(timerTask);
        handler.postDelayed(timerTask, period);

        started = true;

    }

    /**
     * Detiene el timer
     */
    public void stop() {

        handler.removeCallbacks(timerTask);

    }

    /**
     * Bandera que indica si el timer esta iniciado o no
     *
     * @return estado del timer
     */
    public boolean isStarted() {

        return started;

    }

    /**
     * Libera los recursos asociados al timer
     */
    public void dispose() {
        try {
            stop();
            deleteObservers();
            started = false;
        } catch (Exception e) {

            logger.error("", e);
        }
    }
}
