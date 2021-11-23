package com.vdotok.many2many.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.vdotok.many2many.R
import com.vdotok.many2many.base.BaseActivity
import com.vdotok.many2many.databinding.ActivityDashBoardBinding
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.many2many.utils.ViewUtils.setStatusBarGradient
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo

/**
 * Created By: VdoTok
 * Date & Time: On 5/19/21 At 6:29 PM in 2021/
 *
 * This class displays the connection between user and socket
 */
class DashBoardActivity: BaseActivity() {

    private lateinit var binding: ActivityDashBoardBinding
    override fun getRootView() = binding.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarGradient(this, R.drawable.rectangle_white_bg)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dash_board)
        prefs = Prefs(this)

        addInternetConnectionObserver()
        askForPermissions()
    }

    /**
     * Function for asking permissions to user
     * */
    private fun askForPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) &&
            (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                ApplicationConstants.MY_PERMISSIONS_REQUEST
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                ApplicationConstants.MY_PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                ApplicationConstants.MY_PERMISSIONS_REQUEST_CAMERA
            )
        }
    }

    override fun onError(cause: String) {
        Log.e("OnError:", cause)
    }

    override fun onPublicURL(publicURL: String) {}

    override fun onClose(reason: String) {
        mListener?.onCallRejected(reason)
    }


    override fun incomingCall(callParams: CallParams) {
        sessionId?.let {
            if (callClient.getActiveSessionClient(it) != null) {
                callClient.sessionBusy(callParams.refId, callParams.sessionUUID)
            } else {
                mListener?.onIncomingCall(callParams)
            }
        } ?: kotlin.run {
            mListener?.onIncomingCall(callParams)
        }

    }

    override fun onDestroy() {
        callClient.disConnectSocket()
        super.onDestroy()
    }

    override fun onResume() {
        askForPermissions()
        super.onResume()
    }

    fun acceptIncomingCall(
        callParams: CallParams
    ) {
        prefs.loginInfo?.let {
            sessionId = callClient.acceptIncomingCall(
                it.refId!!,
                callParams
            )
        }
    }

    fun diaMany2ManyCall(callParams: CallParams) {
        sessionId = callClient.dialMany2ManyCall(callParams)
    }

    fun logout() {
        callClient.unRegister(
            ownRefId = prefs.loginInfo?.refId.toString()
        )
    }

    companion object {
        fun createDashboardActivity(context: Context) = Intent(
            context,
            DashBoardActivity::class.java
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        }
    }
}