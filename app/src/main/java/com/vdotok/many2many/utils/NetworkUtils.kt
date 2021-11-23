package com.vdotok.many2many.utils

import android.content.Context
import android.view.View
import com.vdotok.many2many.extensions.showSnackBar
import com.vdotok.many2many.network.HttpResponseCodes
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.ui.dashboard.DashBoardActivity
import com.vdotok.network.models.LoginResponse


fun isInternetAvailable(context: Context): Boolean {
    return ConnectivityStatus(context).isConnected()
}

fun handleLoginResponse(context: Context, response: LoginResponse, prefs: Prefs, view: View) {
    when(response.status) {
        HttpResponseCodes.SUCCESS.value -> {
            prefs.loginInfo = response

            context.startActivity(DashBoardActivity.createDashboardActivity(context))
        }
        else -> {
            view.showSnackBar(response.message)
        }
    }
}
