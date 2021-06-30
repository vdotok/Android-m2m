package com.norgic.vdotokcall_mtm.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.norgic.vdotokcall_mtm.interfaces.FragmentRefreshListener
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.ActivityDashBoardBinding
import com.norgic.vdotokcall_mtm.extensions.showSnackBar
import com.norgic.vdotokcall_mtm.models.AcceptCallModel
import com.norgic.vdotokcall_mtm.models.LoginResponse
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.utils.ApplicationConstants
import com.norgic.vdotokcall_mtm.utils.NetworkStatusLiveData
import com.norgic.vdotokcall_mtm.utils.ViewUtils.setStatusBarGradient
import com.razatech.callsdks.CallClient
import com.razatech.callsdks.commands.CallInfoResponse
import com.razatech.callsdks.commands.RegisterResponse
import com.razatech.callsdks.enums.*
import com.razatech.callsdks.interfaces.CallSDKListener
import com.razatech.callsdks.interfaces.StreamCallback
import com.razatech.callsdks.models.EnumConnectionStatus
import org.webrtc.VideoTrack

/**
 * Created By: Norgic
 * Date & Time: On 5/19/21 At 6:29 PM in 2021
 *
 * This class displays the connection between user and socket
 */
class DashBoardActivity : AppCompatActivity(), CallSDKListener, StreamCallback {

    private lateinit var binding: ActivityDashBoardBinding
    var localStream: VideoTrack? = null

    lateinit var callClient: CallClient
    private lateinit var prefs: Prefs
    var sessionId: String? = null

    private var internetConnectionRestored = false
    var mListener: FragmentRefreshListener? = null
    private lateinit var mLiveDataNetwork: NetworkStatusLiveData

    private val mLiveDataEndCall:  MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private val mLiveDataLeftParticipant:  MutableLiveData<String> by lazy {
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

        CallClient.getInstance(this)?.setConstants(ApplicationConstants.SDK_PROJECT_ID)
        CallClient.getInstance(this)?.let {
            callClient = it
            callClient.setListener(this, this)
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
        prefs.sdkAuthResponse?.let {
            if (callClient.isSocketConnected() == null || callClient.isSocketConnected() == false)
                callClient.connect(it.mediaServerMap.completeAddress)
        }
    }

    /**
     * Function to mute call
     * */
    fun muteUnMuteCall() {
        sessionId?.let { callClient.muteUnMuteMic(it) }
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
    override fun onConnect() {
        runOnUiThread { binding.root.showSnackBar("Connected!") }
    }

    override fun connectionStatus(enumConnectionStatus: EnumConnectionStatus) {
        when (enumConnectionStatus) {
            EnumConnectionStatus.CONNECTED -> {
                runOnUiThread {
                    callClient.register(
                        authToken = prefs.loginInfo?.authorizationToken!!,
                        refId = prefs.loginInfo?.refId!!
                    )
                }
            }
            EnumConnectionStatus.NOT_CONNECTED -> {

                prefs.sdkAuthResponse?.let {
                    callClient.connect(it.mediaServerMap.completeAddress)
                }

                runOnUiThread {
                    Toast.makeText(this, "Not Connected!", Toast.LENGTH_SHORT).show()
                }
            }
            EnumConnectionStatus.ERROR -> {

                prefs.sdkAuthResponse?.let {

                    callClient.connect(it.mediaServerMap.completeAddress)
                }
                runOnUiThread {
                    Toast.makeText(this, "Connection Error!", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
            }
        }
    }
    override fun onClose(reason: String, isAccepted: Boolean) {
        mListener?.onCallRejected(reason)
    }

    override fun audioVideoState(audioState: Int, videoState: Int, refId: String) {

        runOnUiThread {
            mListener?.onCameraAudioOff(audioState, videoState, refId)
        }

    }
    override fun incomingCall(
        from: String,
        sessionUUID: String,
        requestID: String,
        callType: CallType,
        mediaType: MediaType,
        sessionType: SessionType
    ) {
        val model = AcceptCallModel(from, sessionUUID, requestID, callType, mediaType, sessionType)
        mListener?.onIncomingCall(model)
    }

    override fun onDestroy() {
        callClient.disConnectSocket()
        super.onDestroy()
    }


    override fun tokenRequestSuccess(mcToken: String) {
        val userModel: LoginResponse? = prefs.loginInfo
        userModel?.mcToken = mcToken
        runOnUiThread {
            userModel?.let {
                prefs.loginInfo = it
            }
            binding.root.showSnackBar("Socket Connected!")
        }
    }

    fun acceptIncomingCall(
        from: String,
        sessionUUID: String,
        requestID: String,
        callType: CallType,
        mediaType: MediaType,
        sessionType: SessionType
    ) {
        prefs.loginInfo?.let {
            sessionId = callClient.acceptIncomingCall(
                it.refId!!,
                sessionUUID,
                requestID,
                arrayListOf(from),
                it.mcToken!!,
                callType,
                mediaType,
                sessionType
            )
        }

    }

    fun endCall() {
        localStream = null
        sessionId?.let {
            callClient.endCallSession(it)
        }
    }

    fun switchCallType(isVideoCall: Boolean) {
        sessionId?.let {
            callClient.switchCallType(
                it,
                mcToken = prefs.loginInfo?.mcToken!!,
                prefs.loginInfo?.refId!!,
                1,
                if (isVideoCall) 1 else 0
            )
        }
    }

    fun pauseVideo() {
        sessionId?.let { callClient.pauseVideo(it) }
    }

    fun resumeVideo() {
        sessionId?.let { callClient.resumeVideo(it) }
    }

    override fun invalidResponse(message: String) {}

    override fun responseMessages(message: String) {
        runOnUiThread { Toast.makeText(this, "Response: $message", Toast.LENGTH_SHORT).show() }
    }

    override fun endOutgoingCall(session: String) {
        Log.d("endOutgoingCall", "endOutgoingCall : "+ session)
        mLiveDataEndCall.postValue(true)
    }

    override fun onSessionReady() {
    }

    override fun callMissed() {
        sessionId?.let {
            if (callClient.getActiveSessionClient(it) == null)
                mListener?.onCallMissed()

        }
    }

    override fun callStatus(callInfoResponse: CallInfoResponse) {

        runOnUiThread {

            Toast.makeText(
                this,
                "Call Status: ${callInfoResponse.callStatus}", Toast.LENGTH_SHORT
            ).show()
        }
        when (callInfoResponse.callStatus) {
            CallStatus.CALL_CONNECTED -> {
                runOnUiThread {
                    mListener?.onStartCalling()
                }
            }
            CallStatus.OUTGOING_CALL_ENDED -> {
                mLiveDataEndCall.postValue(true)
            }
            CallStatus.CALL_MISSED -> {
                sessionId?.let {
                    if (callClient.getActiveSessionClient(it) == null)
                        mLiveDataEndCall.postValue(true)
                } ?: kotlin.run {
                    mListener?.onCallMissed()
                }
            }
            CallStatus.PARTICIPANT_LEFT_CALL -> {
                 mLiveDataLeftParticipant.postValue(callInfoResponse.refId)
            }
            CallStatus.NO_ANSWER_FROM_TARGET -> {
                mLiveDataLeftParticipant.postValue(callInfoResponse.refId)
            }
            else -> {}
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
                    binding.root.showSnackBar("Socket Connected!")
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
        sessionId = sessionId
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
                isInternetConnected == true && internetConnectionRestored -> connectClient()
                isInternetConnected == false -> {
                    internetConnectionRestored = true
                    mListener?.onCallEnd()
                }
                else -> {}
            }
        })
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