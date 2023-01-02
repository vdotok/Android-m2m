package com.vdotok.many2many.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.vdotok.many2many.VdoTok

class OnClearFromRecentService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("ClearFromRecentService", "Service Started")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ClearFromRecentService", "Service Destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.e("ClearFromRecentService", "END")
        (application as VdoTok).callClient.recentSession()?.let {
            (application as VdoTok).callClient.endCallSession(arrayListOf(it))
        }
        (application as VdoTok).callClient.disConnectSocket()
        stopSelf()
    }
}