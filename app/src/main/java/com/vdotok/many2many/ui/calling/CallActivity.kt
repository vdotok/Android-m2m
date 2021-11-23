package com.vdotok.many2many.ui.calling

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.text.TextUtils
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import com.vdotok.many2many.R
import com.vdotok.many2many.base.BaseActivity
import com.vdotok.many2many.databinding.ActivityCallBinding
import com.vdotok.many2many.models.AcceptCallModel
import com.vdotok.many2many.ui.calling.fragment.DialCallFragment
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.network.models.GroupModel
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo


class CallActivity : BaseActivity() {

    private lateinit var binding: ActivityCallBinding
    private var audioManager: AudioManager? = null

    val mLiveDataEndCall: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val mLiveDataLeftParticipant: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    override fun getRootView() = binding.root


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)
        addInternetConnectionObserver()

        sessionId = intent.extras?.getString(Session_ID)

        findNavController(R.id.nav_host_fragment)
            .setGraph(
                R.navigation.call_navigation,
                intent.extras
            )

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

    fun acceptIncomingCall(callParams: CallParams) {
        prefs.loginInfo?.let {
            sessionId = callClient.acceptIncomingCall(
                it.refId!!, callParams
            )
        }
    }

    fun endCall() {
        turnSpeakerOff()
        localStream = null
        sessionId?.let {
            callClient.endCallSession(arrayListOf(it))
        }

        finish()
    }

    override fun audioVideoState(state: SessionStateInfo) {
        runOnUiThread {
            mListener?.onCameraAudioOff(state.audioState!!, state.videoState!!, state.refID!!)
        }
    }

    override fun onClose(reason: String) {
        mListener?.onCallRejected(reason)
    }


    fun turnSpeakerOff() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.let {
            it.isSpeakerphoneOn = false
        }
    }

    override fun incomingCall(callParams: CallParams) {
    }


//    fun endCall() {
//        turnSpeakerOff()
//        localStream = null
//        sessionId?.let {
//            callClient.endCallSession(arrayListOf(it))
//        }
//    }


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



    companion object {

//        const val VIDEO_CALL = "video_call"
//        const val IN_COMING_CALL = "incoming_call"
        const val Session_ID = "session_id"

        fun createIntent(context: Context, group: GroupModel?, isVideo: Boolean, isIncoming: Boolean, model: AcceptCallModel?,
                         callParams: CallParams?, sessionId: String?, groupList: ArrayList<GroupModel>?= null)  = Intent(
            context,
            CallActivity::class.java
        ).apply {
            this.putExtras(Bundle().apply {
                putParcelable(GroupModel.TAG, group)
                putBoolean(DialCallFragment.IS_VIDEO_CALL, isVideo)
//                putParcelable(AcceptCallModel.TAG, model)
                putBoolean(DialCallFragment.IS_IN_COMING_CALL, isIncoming)

                groupList?.let {
                    putParcelableArrayList(DialCallFragment.GROUP_LIST, groupList)
                }
                callParams?.let {
                    putParcelable(ApplicationConstants.CALL_PARAMS, it)
                    putBoolean(DialCallFragment.IS_IN_COMING_CALL, true)
                }
                putString(Session_ID, sessionId)
            })
        }
    }

}