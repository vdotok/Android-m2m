package com.vdotok.many2many.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import com.vdotok.many2many.R
import com.vdotok.many2many.databinding.ActivityDashBoardBinding
import com.vdotok.many2many.interfaces.FragmentRefreshListener
import com.vdotok.many2many.models.LoginResponse
import com.vdotok.many2many.models.MediaServerMap
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.many2many.utils.NetworkStatusLiveData
import com.vdotok.many2many.utils.ViewUtils.setStatusBarGradient
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.commands.CallInfoResponse
import com.vdotok.streaming.commands.RegisterResponse
import com.vdotok.streaming.enums.CallStatus
import com.vdotok.streaming.enums.EnumConnectionStatus
import com.vdotok.streaming.enums.RegistrationStatus
import com.vdotok.streaming.interfaces.CallSDKListener
import com.vdotok.streaming.models.*
import org.webrtc.VideoTrack

/**
 * Created By: VdoTok
 * Date & Time: On 5/19/21 At 6:29 PM in 2021
 *
 * This class displays the connection between user and socket
 */
class DashBoardActivity : AppCompatActivity(), CallSDKListener {

    private lateinit var binding: ActivityDashBoardBinding
    var localStream: VideoTrack? = null

    lateinit var callClient: CallClient
    private lateinit var prefs: Prefs
    var sessionId: String? = null
    var handler: Handler? = null

    private var internetConnectionRestored = false
    var mListener: FragmentRefreshListener? = null
    private lateinit var mLiveDataNetwork: NetworkStatusLiveData

    private val mLiveDataEndCall: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private val mLiveDataLeftParticipant: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarGradient(this, R.drawable.rectangle_white_bg)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dash_board)
        prefs = Prefs(this)

        initCallClient()
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

    /**
     * function to connect socket successfully
     * */

    private fun initCallClient() {
        handler = Handler(Looper.getMainLooper())
        CallClient.getInstance(this)?.setConstants(ApplicationConstants.SDK_PROJECT_ID)
        CallClient.getInstance(this)?.let {
            callClient = it
            callClient.setListener(this)
        }

        connectClient()

        mLiveDataEndCall.observe(this, {
            if (it) {
                mListener?.onCallEnd()
            }
        })
        mLiveDataLeftParticipant.observe(this, {
            if (!TextUtils.isEmpty(it)) {
                mListener?.onParticipantLeftCall(it)
            }
        })


    }


    fun connectClient() {
        prefs.loginInfo?.mediaServerMap?.let {
            if (callClient.isConnected() == null || callClient.isConnected() == false)
                callClient.connect(
                    getMediaServerAddress(it),
                    it.endPoint
                )
        }
    }

    private fun getMediaServerAddress(mediaServer: MediaServerMap): String {
        return "https://${mediaServer.host}:${mediaServer.port}"
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

    /**
     * Callback method when socket server is connected successfully
     * */
//    override fun onConnect() {
//        runOnUiThread { binding.root.showSnackBar("Connected!") }
//    }

    override fun onError(cause: String) {
        Log.e("OnError:", cause)
    }

    override fun onPublicURL(publicURL: String) {}

    override fun connectionStatus(enumConnectionStatus: EnumConnectionStatus) {
        when (enumConnectionStatus) {
            EnumConnectionStatus.CONNECTED -> {
                mListener?.onConnectionSuccess()
                runOnUiThread {
                    callClient.register(
                        authToken = prefs.loginInfo?.authorizationToken!!,
                        refId = prefs.loginInfo?.refId!!,0
                    )
                }
            }
            EnumConnectionStatus.NOT_CONNECTED -> {
                mListener?.onConnectionFail()
                prefs.loginInfo?.mediaServerMap?.let {
                    callClient.connect(
                        getMediaServerAddress(it),
                        it.endPoint
                    )
                }

                runOnUiThread {
                    Toast.makeText(this, "Not Connected!", Toast.LENGTH_SHORT).show()
                }
            }
            EnumConnectionStatus.ERROR -> {
                mListener?.onConnectionFail()
                prefs.loginInfo?.mediaServerMap?.let {
                    callClient.connect(
                        getMediaServerAddress(it),
                        it.endPoint
                    )
                }

                runOnUiThread {
                    Toast.makeText(this, "Connection Error!", Toast.LENGTH_SHORT).show()
                }
            }
            EnumConnectionStatus.SOCKET_PING -> {
                handler?.removeCallbacks(runnableConnectClient)
                handler?.postDelayed(runnableConnectClient, 20000)

            }
            else -> {
            }
        }
    }

    val runnableConnectClient by lazy {
        object : Runnable {
            override fun run() {
                connectClient()
            }
        }
    }

    override fun onClose(reason: String) {
        mListener?.onCallRejected(reason)
    }

    override fun audioVideoState(state: SessionStateInfo) {
        runOnUiThread {
            mListener?.onCameraAudioOff(state.audioState!!, state.videoState!!, state.refID!!)
        }

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
        handler?.removeCallbacks(runnableConnectClient)
        callClient.disConnectSocket()
        super.onDestroy()
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

    fun endCall() {
        localStream = null


        sessionId?.let {
            callClient.endCallSession(arrayListOf(it))
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

    override fun participantCount(participantCount: Int) {
//        TODO("Not yet implemented")
    }

    override fun onSessionReady(
        mediaProjection: MediaProjection?,
        isInternalAudioIncluded: Boolean
    ) {
    }

    override fun callStatus(callInfoResponse: CallInfoResponse) {
        runOnUiThread {

            when (callInfoResponse.callStatus) {
                CallStatus.CALL_CONNECTED -> {
                    runOnUiThread {
                        mListener?.onStartCalling()
                    }
                }
                CallStatus.OUTGOING_CALL_ENDED,
                CallStatus.NO_SESSION_EXISTS -> {
                    mLiveDataEndCall.postValue(true)
                }
                CallStatus.TARGET_IS_BUSY -> {
                    //received 2 cmd busy and cancel
                    mListener?.onCallerAlreadyBusy()
                }
                CallStatus.NO_ANSWER_FROM_TARGET -> {
                    //received 1 cmd no answer from target
                    mListener?.noAnsFromTarget()
                }
                CallStatus.CALL_MISSED -> {
                    sessionId?.let {
                        if (callClient.getActiveSessionClient(it) == null)
                            mLiveDataEndCall.postValue(true)
                    } ?: kotlin.run {
                        mListener?.onCallMissed()
                    }
                }
                CallStatus.CALL_REJECTED,
                CallStatus.PARTICIPANT_LEFT_CALL -> {
                    mLiveDataLeftParticipant.postValue(callInfoResponse.callParams?.toRefIds?.get(0))
                }
                else -> {
                }
            }
        }


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
//                    binding.root.showSnackBar("Socket Connected!")
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
    override fun onRemoteStream(stream: VideoTrack, refId: String, sessionID: String) {
        sessionId = sessionID
        mListener?.onRemoteStreamReceived(stream, refId, sessionID)
    }

    override fun onRemoteStream(refId: String, sessionID: String) {

        mListener?.onRemoteStreamReceived(refId, sessionID)
    }

    override fun onCameraStream(stream: VideoTrack) {
        localStream = stream
        mListener?.onCameraStreamReceived(stream)
    }

    private fun addInternetConnectionObserver() {
        mLiveDataNetwork = NetworkStatusLiveData(this.application)

        mLiveDataNetwork.observe(this, { isInternetConnected ->
            when {
                isInternetConnected == true && internetConnectionRestored -> {
                    connectClient()
                    mListener?.onConnectionSuccess()
                }
                isInternetConnected == false -> {
                    internetConnectionRestored = true
                    mListener?.onCallEnd()
                    mListener?.onConnectionFail()
                }
                else -> {
                }
            }
        })
    }

    override fun memoryUsageDetails(memoryUsage: Long) {
//        TODO("Not yet implemented")
    }

    override fun sendCurrentDataUsage(sessionKey: String, usage: Usage) {
        prefs.loginInfo?.refId?.let { refId ->
            Log.e(
                "statsSdk",
                "currentSentUsage: ${usage.currentSentBytes}, currentReceivedUsage: ${usage.currentReceivedBytes}"
            )
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

    fun logout() {
        callClient.unRegister(
            ownRefId = prefs.loginInfo?.refId.toString()
        )
    }

    companion object {
        const val TAG = "DASHBOARD_ACTIVITY"
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