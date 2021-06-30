package com.norgic.vdotokcall_mtm.utils

import android.app.Activity
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.norgic.vdotokcall_mtm.R
import java.util.concurrent.TimeUnit

fun Activity.hasNetworkAvailable(): Boolean {
    val service = Activity.CONNECTIVITY_SERVICE
    val manager = this.getSystemService(service) as ConnectivityManager?
    val network = manager?.activeNetworkInfo
    return (network != null)
}

fun Activity.isInternetAvailable(): Boolean {
    return ConnectivityStatus.isConnected(this)
}


fun View.disableDoubleClick() {
    isClickable = false
    postDelayed({ isClickable = true }, TimeUnit.SECONDS.toMillis(2))
}
fun View.disableDoubleClickForOneSec() {
    isClickable = false
    postDelayed({ isClickable = true }, TimeUnit.SECONDS.toMillis(1))
}

fun View.clicks(method:  () -> Unit) {
    setOnClickListener {
        method.invoke()
    }
}

fun View.performSingleClick(method: () -> Unit) {
    setOnClickListener {
        disableDoubleClick()
        method.invoke()
    }
}

fun View.performSingleClickShortDelay(method: () -> Unit) {
    setOnClickListener {
        disableDoubleClickForOneSec()
        method.invoke()
    }
}

fun View.enable() {
    isEnabled = true
}

fun View.disable() {
    isEnabled = false
}

