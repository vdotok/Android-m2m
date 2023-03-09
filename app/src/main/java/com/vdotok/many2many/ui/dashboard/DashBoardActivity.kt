package com.vdotok.many2many.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.vdotok.many2many.R
import com.vdotok.many2many.VdoTok
import com.vdotok.many2many.base.BaseActivity
import com.vdotok.many2many.databinding.ActivityDashBoardBinding
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.ui.calling.CallActivity
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.many2many.utils.ViewUtils.setStatusBarGradient
import com.vdotok.streaming.commands.CallInfoResponse
import com.vdotok.streaming.enums.CallStatus
import com.vdotok.streaming.enums.PermissionType
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

    private lateinit var navController: NavController
    var sessionIdList = arrayListOf<String>()
    private var audioManager: AudioManager? = null
    var dialCallOpen: Boolean = false
    var isCallInitiator: Boolean = false

    val mLiveDataEndCall: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val mLiveDataLeftParticipant: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    override fun getRootView() = binding.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarGradient(this, R.drawable.rectangle_white_bg)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dash_board)
        prefs = Prefs(this)

        addInternetConnectionObserver()
        askForPermissions()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.chat_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        mLiveDataEndCall.observe(this) {
            if (it) {
                mListener?.onCallEnd()
            }
        }
        mLiveDataLeftParticipant.observe(this) {
            if (!TextUtils.isEmpty(it)) {
                mListener?.onParticipantLeftCall(it)
            }
        }
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


    private val listener =
        NavController.OnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {
                R.id.dialFragment -> {
                    dialCallOpen = true
                }
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
        runOnUiThread {
            Handler(Looper.getMainLooper()).postDelayed({
                (application as VdoTok).mediaTypeCheck = callParams.mediaType
                sessionIdList.add(callParams.sessionUUID)
                callParams.sessionUUID.let {
                    if (callClient.getActiveSessionClient(it) != null || dialCallOpen) {
                        callClient.sessionBusy(callParams.refId, callParams.sessionUUID)
                    } else {
                        mListener?.onIncomingCall(callParams)
                        sessionId = callParams.sessionUUID
                    }
                }
            },1000)
        }
    }

    override fun permissionError(permissionErrorList: ArrayList<PermissionType>) {
//        TODO("Not yet implemented")
    }

    override fun sessionReconnecting(sessionID: String) {
//        TODO("Not yet implemented")
    }

    override fun onResume() {
        navController.addOnDestinationChangedListener(listener)
        askForPermissions()
        super.onResume()
    }

    override fun onDestroy() {
        navController.removeOnDestinationChangedListener(listener)
        super.onDestroy()
    }

    override fun callStatus(callInfoResponse: CallInfoResponse) {
        runOnUiThread {
            when (callInfoResponse.callStatus) {
                CallStatus.CALL_CONNECTED -> {
                    mListener?.onStartCalling()
                }
                CallStatus.SERVICE_SUSPENDED,
                CallStatus.OUTGOING_CALL_ENDED,
                CallStatus.NO_SESSION_EXISTS -> {
                    callInfoResponse.callParams?.sessionUUID?.let {
                        if (callClient.getActiveSessionClient(it) == null) {
                            turnMicOff()
                            turnSpeakerOff()
                            mLiveDataEndCall.postValue(true)
                        }
                    }
                }
                CallStatus.CALL_REJECTED,
                CallStatus.PARTICIPANT_LEFT_CALL  -> {
                    callInfoResponse.callParams?.refId?.let {
                        if (it.isNotEmpty())
                            mListener?.onCallRejected(it)
                        else {
                            callInfoResponse.callParams?.toRefIds?.get(0)?.let {
                                mListener?.onCallRejected(it)
                            }
                        }
                    }
                }
                CallStatus.CALL_MISSED -> {
                    sessionId?.let {
                        if (callClient.getActiveSessionClient(it) == null)
                            mLiveDataEndCall.postValue(true)
                    } ?: kotlin.run {
                        mListener?.onCallMissed()
                        mLiveDataEndCall.postValue(true)
                    }
//                    mListener?.onCallMissed()
                }
                CallStatus.NO_ANSWER_FROM_TARGET -> {
                    mListener?.noAnsFromTarget()
                }
                CallStatus.TARGET_IS_BUSY,
                CallStatus.SESSION_BUSY -> {

                    if (sessionIdList.contains(callInfoResponse.callParams?.sessionUUID)) {
                        sessionIdList.remove(callInfoResponse.callParams?.sessionUUID)
                    }
                    if (sessionIdList.isEmpty() && dialCallOpen) {
                        if (isCallInitiator)
                            mListener?.onCallerAlreadyBusy()
                        else
                            mLiveDataEndCall.postValue(true)
                    }
                }
                CallStatus.SESSION_TIMEOUT -> {
                    mListener?.onCallTimeout()
                }
                CallStatus.INSUFFICIENT_BALANCE ->{
                    mListener?.onInsuficientBalance()
                }
                else -> {
                }
            }
        }
    }

    fun endCall() {
        turnMicOff()
        turnSpeakerOff()
        localStream = null
        sessionId?.let {
            callClient.endCallSession(arrayListOf(it))
        }
        if (!callClient.isConnected())
            mListener?.onCallEnd()
    }

    fun turnMicOff() {
        if (!callClient.isAudioEnabled(sessionId.toString())){
            callClient.muteUnMuteMic(prefs.loginInfo?.refId.toString(),sessionId.toString())
        }
    }

    fun turnSpeakerOff() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.let {
            it.isSpeakerphoneOn = false
        }
    }

    fun pauseVideo() {
        sessionId?.let {
            callClient.pauseVideo(
                sessionKey = sessionId.toString(),
                refId = prefs.loginInfo?.refId!!
            )
        }
    }

    fun resumeVideo() {
        sessionId?.let {
            callClient.resumeVideo(
                sessionKey = sessionId.toString(),
                refId = prefs.loginInfo?.refId!!
            )
        }
    }

    /**
     * Function to mute call
     * */
    fun muteUnMuteCall(isVideoCall: Boolean) {
        sessionId?.let {
            callClient.muteUnMuteMic(
                sessionKey = sessionId.toString(),
            )
        }
    }

    /**
     * Function to switch Camera
     * */
    fun switchCamera() {
        sessionId?.let { callClient.switchCamera(it) }
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
        isCallInitiator = true
        (application as VdoTok).mediaTypeCheck = callParams.mediaType
        sessionId = callClient.dialMany2ManyCall(callParams)
        sessionId?.let { sessionIdList.add(it) }
    }

    fun logout() {
        callClient.disConnectSocket()
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