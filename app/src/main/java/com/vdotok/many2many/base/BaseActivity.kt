package com.vdotok.many2many.base

import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.NetworkStatusLiveData
import com.vdotok.network.models.LoginResponse
import com.vdotok.many2many.extensions.showSnackBar
import com.vdotok.many2many.ui.calling.CallActivity
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.commands.CallInfoResponse
import com.vdotok.streaming.commands.RegisterResponse
import com.vdotok.streaming.enums.CallStatus
import com.vdotok.streaming.enums.EnumConnectionStatus
import com.vdotok.streaming.enums.RegistrationStatus
import com.vdotok.streaming.interfaces.CallSDKListener
import com.vdotok.streaming.models.*
import com.vdotok.streaming.utils.checkInternetAvailable
import org.webrtc.VideoTrack


/**
 * Created By: VdoTok
 * Date & Time: On 11/1/21 At 6:11 PM in 2021
 */
abstract class BaseActivity: AppCompatActivity(), CallSDKListener {

    lateinit var callClient: CallClient
    lateinit var prefs: Prefs
    var sessionId: String? = null
    private lateinit var mLiveDataNetwork: NetworkStatusLiveData
    private var isInternetConnectionRestored = false
    private var isResumeState = false
    private var reConnectStatus = false

    abstract fun getRootView(): View

    companion object {
        var mListener: FragmentCallback? = null
        var localStream: VideoTrack? = null
        var remoteStream: VideoTrack? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
    }

    override fun onResume() {
        isResumeState = true
        super.onResume()
        initCallClient()
    }

    fun initCallClient() {

        CallClient.getInstance(this)?.setConstants(ApplicationConstants.SDK_PROJECT_ID)
        CallClient.getInstance(this)?.let {
            callClient = it
            callClient.setListener(this)
        }
        connectClient()
    }
    // when socket is disconnected
    override fun onClose(reason: String) {
        connectClient()
    }

    fun connectClient() {
        prefs.loginInfo?.mediaServer?.let {
            if (callClient.isConnected() == null || callClient.isConnected() == false)
                callClient.connect(getMediaServerAddress(it), it.endPoint)
        }
    }


    private fun getMediaServerAddress(mediaServer: LoginResponse.MediaServerMap): String {
        return "https://${mediaServer.host}:${mediaServer.port}"
    }


    override fun onError(cause: String) {}

    override fun onPublicURL(publicURL: String) {}


    override fun audioVideoState(state: SessionStateInfo) {
        runOnUiThread {
            mListener?.onAudioVideoStateChanged(state.audioState!!, state.videoState!!)
        }
    }

    override fun onSessionReady(mediaProjection: MediaProjection?) {

    }

    override fun multiSessionCreated(sessionIds: Pair<String, String>) {

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
                        sessionId?.let {
                            if (callClient.getActiveSessionClient(it) == null && this@BaseActivity is CallActivity) {
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
                        if (callClient.getActiveSessionClient(it) == null && this@BaseActivity is CallActivity)
                            mLiveDataEndCall.postValue(true)
                    } ?: kotlin.run {
                        mListener?.onCallMissed()
                    }
//                    mListener?.onCallMissed()
                }
                CallStatus.NO_ANSWER_FROM_TARGET -> {
                    mListener?.noAnsFromTarget()
                }
                CallStatus.TARGET_IS_BUSY -> {
                    mListener?.onCallerAlreadyBusy()
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

    override fun connectionStatus(enumConnectionStatus: EnumConnectionStatus) {
        when (enumConnectionStatus) {
            EnumConnectionStatus.CONNECTED -> {
                mListener?.onConnectionSuccess()
                prefs.loginInfo?.let {
                    if (it.authorizationToken != null && it.refId != null) {
                        runOnUiThread {
                            callClient.register(
                                authToken = prefs.loginInfo?.authorizationToken!!,
                                refId = prefs.loginInfo?.refId!!,if (reConnectStatus) 1 else 0
                            )
                            reConnectStatus = false

                        }
                    }
                }
            }

            EnumConnectionStatus.NOT_CONNECTED -> {
                mListener?.onConnectionFail()
                connectClient()

                runOnUiThread {
                    Toast.makeText(this, "Not Connected!", Toast.LENGTH_SHORT).show()
                }
            }
            EnumConnectionStatus.ERROR -> {
                mListener?.onConnectionFail()
                connectClient()
                runOnUiThread {
                    Toast.makeText(this, "Connection Error!", Toast.LENGTH_SHORT).show()
                }
            }
            EnumConnectionStatus.CLOSED -> {
                runOnUiThread {
                    Toast.makeText(this, "Connection Closed!", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
            }
        }
    }

    override fun incomingCall(callParams: CallParams) {
    }

    override fun onDestroy() {
        isResumeState = false
        mLiveDataNetwork.removeObservers(this)
        super.onDestroy()
    }

    override fun registrationStatus(registerResponse: RegisterResponse) {

        when (registerResponse.registrationStatus) {
            RegistrationStatus.REGISTER_SUCCESS -> {

                val userModel: LoginResponse? = prefs.loginInfo
                userModel?.mcToken = registerResponse.mcToken.toString()
                runOnUiThread {
                    userModel?.let {
                        prefs.loginInfo = it
                    }
                    Log.e("register", "message: ${registerResponse.responseMessage}")

                    getRootView().showSnackBar("Socket Connected!")
                    callClient.initiateReInviteProcess()
                }
            }
            RegistrationStatus.UN_REGISTER,
            RegistrationStatus.REGISTER_FAILURE,
            RegistrationStatus.INVALID_REGISTRATION -> {
                Handler(Looper.getMainLooper()).post {
                    Log.e("register", "message: ${registerResponse.responseMessage}")
                }
            }
        }
    }


    /**
     * Callback method to get Video Stream of user
     * */
//    override fun onRemoteStream(stream: VideoTrack, refId: String, sessionID: String) {
//        sessionId = sessionID
//        mListener?.onRemoteStreamReceived(stream, refId, sessionID)
//    }
//
//    override fun onRemoteStream(refId: String, sessionID: String) {
//
//        mListener?.onRemoteStreamReceived(refId, sessionID)
//    }
//
//    override fun onCameraStream(stream: VideoTrack) {
//        localStream = stream
//        mListener?.onCameraStreamReceived(stream)
//    }


    // Stream callbacks
    override fun onRemoteStream(stream: VideoTrack, refId: String, sessionID: String) {
        remoteStream = stream
        mListener?.onRemoteStreamReceived(stream, refId, sessionID)
    }

    override fun onCameraStream(stream: VideoTrack) {
        localStream = stream
        mListener?.onCameraStreamReceived(stream)
    }

    override fun onRemoteStream(refId: String, sessionID: String) {
        mListener?.onRemoteStreamReceived(refId, sessionID)
    }

    override fun sendCurrentDataUsage(sessionKey: String, usage: Usage) {
        prefs.loginInfo?.refId?.let { refId ->
            Log.e("statsSdk", "currentSentUsage: ${usage.currentSentBytes}, currentReceivedUsage: ${usage.currentReceivedBytes}")
            callClient.sendEndCallLogs(
                refId = refId,
                sessionKey = sessionKey,
                stats = PartialCallLogs(
                    upload_bytes = usage.currentSentBytes.toString(),
                    download_bytes = usage.currentReceivedBytes.toString()
                )
            )
        }
    }

    override fun sendEndDataUsage(sessionKey: String, sessionDataModel: SessionDataModel) {
        prefs.loginInfo?.refId?.let { refId ->
            Log.e("statsSdk", "sessionData: $sessionDataModel")
            callClient.sendEndCallLogs(
                refId = refId,
                sessionKey = sessionKey,
                stats = sessionDataModel
            )
        }
    }

    override fun sessionHold(sessionUUID: String) {
    }

    fun addInternetConnectionObserver() {
        mLiveDataNetwork = NetworkStatusLiveData(this.application)

        mLiveDataNetwork.observe(this, { isInternetConnected ->
            when {
                isInternetConnected == true && isInternetConnectionRestored && !isResumeState -> {
                    Log.e("Internet", "internet connection restored!")
                    performSocketReconnection()
                }
                isInternetConnected == false -> {
                    isInternetConnectionRestored = true
                    reConnectStatus = true
                    isResumeState = false
                    Log.e("Internet", "internet connection lost!")
                }
                else -> {
                }
            }
        })
    }

    private fun performSocketReconnection() {
        prefs.loginInfo?.let { loginResponse ->
            if (checkInternetAvailable(this)) {
                loginResponse.mediaServer?.let {
                    callClient.connect(getMediaServerAddress(it), it.endPoint)
                }
            } else {
                Snackbar.make(
                    getRootView(),
                    "Socket Connection failed! No Internet!",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } ?: kotlin.run {
            Snackbar.make(
                getRootView(),
                "No user data found please re-login",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }



}
