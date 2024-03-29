package com.vdotok.many2many.ui.calling.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.google.gson.Gson
import com.vdotok.many2many.R
import com.vdotok.many2many.VdoTok
import com.vdotok.many2many.VdoTok.Companion.getVdotok
import com.vdotok.many2many.base.BaseActivity
import com.vdotok.many2many.base.BaseFragment
import com.vdotok.many2many.databinding.LayoutCallingUserBinding
import com.vdotok.many2many.databinding.LayoutFragmentCallBinding
import com.vdotok.many2many.extensions.hide
import com.vdotok.many2many.extensions.show
import com.vdotok.many2many.models.CallNameModel
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.ui.dashboard.DashBoardActivity
import com.vdotok.many2many.utils.*
import com.vdotok.many2many.utils.TimeUtils.getTimeFromSeconds
import com.vdotok.network.models.GroupModel
import com.vdotok.network.models.Participants
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.models.CallParams
import kotlinx.android.synthetic.main.fragment_dial_call.view.*
import kotlinx.android.synthetic.main.layout_calling_user.view.*
import kotlinx.android.synthetic.main.layout_fragment_call.*
import org.webrtc.VideoTrack
import java.util.*


const val THRESHOLD_VALUE = 70.0f

/**
 * Created By: VdoTok
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays the on connected call
 */
class CallFragment : BaseFragment() {

    private var callParams: CallParams? = null
    private var isIncomingCall = false
    private lateinit var binding: LayoutFragmentCallBinding

    private lateinit var callClient: CallClient
    private var groupModel: GroupModel? = null
    private var name: String? = null
    private lateinit var prefs: Prefs

    private var userMap = mutableMapOf<String, LayoutCallingUserBinding>()
    private var userName: ObservableField<String> = ObservableField<String>()
    private var groupList = ArrayList<GroupModel>()

    private var isMuted = false
    private var isSpeakerOff = true
    private var isVideoCall = true
    private var isCallTypeAudio = false
    private var callDuration = 0
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    private var screenWidth = 0
    private var screenHeight = 0

    private var rightDX = 0
    private var rightDY = 0

    private var xPoint = 0.0f
    private var yPoint = 0.0f
    private val listUser = ArrayList<Participants>()
    var isCamSwitch = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = LayoutFragmentCallBinding.inflate(inflater, container, false)
        init()
        startTimer()

        return binding.root
    }


    /**
     * Function for setOnClickListeners and receiving data from outgoing and incoming call dial
     * */
    private fun init() {
        prefs = Prefs(this.requireContext())

        binding.username = userName

        BaseActivity.mListener = this

        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }
        groupList.clear()
        isVideoCall = arguments?.getBoolean(DialCallFragment.IS_VIDEO_CALL) ?: false

        arguments?.get(GroupModel.TAG)?.let {
            groupModel = it as GroupModel?
            isIncomingCall = arguments?.get(DialCallFragment.IS_IN_COMING_CALL) as Boolean
        } ?: kotlin.run {
            groupList =
                arguments?.getParcelableArrayList<GroupModel>(DialCallFragment.GROUP_LIST) as ArrayList<GroupModel>
            name = (arguments?.get(DialCallFragment.USER_NAME) as CharSequence?).toString()
            callParams = arguments?.getParcelable(ApplicationConstants.CALL_PARAMS) as CallParams?
            isIncomingCall = true
        }
        if (isIncomingCall) {
            userName.set(getCallTitle(callParams?.customDataPacket.toString()))
        } else {
            userName.set(groupModel?.groupTitle)
        }
        getIncomingUserName(isVideoCall)
        displayUi(isVideoCall)

        binding.imgCallOff.performSingleClick {
            stopTimer()
            if (isVideoCall) {
                releaseCallViews()
            }
            (activity as DashBoardActivity).endCall()
        }

        binding.imgMute.setOnClickListener {

            isMuted = !isMuted
            if (isMuted) {
                binding.imgMute.setImageResource(R.drawable.ic_mute_mic1)
            } else {
                binding.imgMute.setImageResource(R.drawable.ic_unmute_mic)
            }
            (activity as DashBoardActivity).muteUnMuteCall(isVideoCall)
        }

        binding.ivSpeaker.setOnClickListener {
            isSpeakerOff = isSpeakerOff.not()
            if (isSpeakerOff) {
                binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_off)
            } else {
                binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
            }
            callClient.toggleSpeakerOnOff()
        }

        val callback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {}
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner,  // LifecycleOwner
            callback
        )
        binding.ivCamSwitch.setOnClickListener {
            if (!isCamSwitch) {
                binding.localView.preview.setMirror(false)
            } else {
                binding.localView.preview.setMirror(true)
            }
            isCamSwitch = isCamSwitch.not()
            (activity as DashBoardActivity).switchCamera()
        }

        binding.imgCamera.setOnClickListener {
            if (isCallTypeAudio) {
                return@setOnClickListener
            }
            activity?.runOnUiThread {
                if (isVideoCall) {
                    binding.localView.hide()
                    binding.localView.showHideAvatar(true)
                    (activity as DashBoardActivity).pauseVideo()
                    (activity?.application as VdoTok).camView = false
                    binding.imgCamera.setImageResource(R.drawable.ic_video_off)
                } else {
                    binding.localView.show()
                    binding.localView.showHideAvatar(false)
                    (activity as DashBoardActivity).resumeVideo()
                    binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
                    (activity?.application as VdoTok).camView = true

                }
                isVideoCall = !isVideoCall
            }
        }

        addTouchEventListener()

        screenWidth = context?.resources?.displayMetrics?.widthPixels!!
        screenHeight = context?.resources?.displayMetrics?.heightPixels!!

        BaseActivity.localStream?.let { onCameraStreamReceived(it) }

        binding.containerVideoFrame.container1.root.setTag("1")
        binding.containerVideoFrame.container2.root.setTag("2")
        binding.containerVideoFrame.container3.root.setTag("3")
        binding.containerVideoFrame.container4.root.setTag("4")

    }

    private fun releaseCallViews() {
        binding.localView.release()
        binding.containerVideoFrame.container1.remoteView.release()
        binding.containerVideoFrame.container2.remoteView.release()
        binding.containerVideoFrame.container3.remoteView.release()
        binding.containerVideoFrame.container4.remoteView.release()
    }

    /**
     * Function to set user name when call connected from incoming call dial
     * @param videoCall videoCall to check whether its an audio or video call
     * */
    private fun getIncomingUserName(videoCall: Boolean) {
        if (!videoCall) {
            binding.tvCallType.text = getString(R.string.audio_calling)
        } else {
            binding.tvCallType.text = getString(R.string.video_calling)
        }

    }

    /**
     * Function to set ui related to audio and video
     * @param videoCall videoCall to check whether its an audio or video call
     * */
    private fun displayUi(videoCall: Boolean) {

        if (!isAdded) {
            return
        }

        if (!videoCall) {
            binding.localView.hide()
            isCallTypeAudio = true
            binding.containerVideoFrame.container1.remoteView.hide()
            binding.containerVideoFrame.container2.remoteView.hide()
            binding.containerVideoFrame.container3.remoteView.hide()
            binding.containerVideoFrame.container4.remoteView.hide()

            binding.containerVideoFrame.container1.groupAudioCall.show()
            binding.containerVideoFrame.container2.groupAudioCall.show()
            binding.containerVideoFrame.container3.groupAudioCall.show()
            binding.containerVideoFrame.container4.groupAudioCall.show()

            binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_off)
            binding.imgCamera.setImageResource(R.drawable.ic_video_off)
            binding.ivCamSwitch.hide()
            callClient.setSpeakerEnable(false)
            isSpeakerOff = true

        } else {
            isCallTypeAudio = false
            initiateView()
            binding.localView.show()
            binding.containerVideoFrame.container1.groupAudioCall.hide()
            binding.containerVideoFrame.container2.groupAudioCall.hide()
            binding.containerVideoFrame.container3.groupAudioCall.hide()
            binding.containerVideoFrame.container4.groupAudioCall.hide()

            binding.containerVideoFrame.container1.remoteView.show()
            binding.containerVideoFrame.container2.remoteView.show()
            binding.containerVideoFrame.container3.remoteView.show()
            binding.containerVideoFrame.container4.remoteView.show()

            binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
            binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
            callClient.setSpeakerEnable(true)
            isSpeakerOff = false
        }

        updateCallUIViews(listUser)
    }

    private fun initiateView() {
        getVdotok()?.rootEglBaseContext?.let { binding.localView.initiateCallView(it) }
        getVdotok()?.rootEglBaseContext?.let {
            binding.containerVideoFrame.container1.remoteView.initiateCallView(
                it
            )
        }
        getVdotok()?.rootEglBaseContext?.let {
            binding.containerVideoFrame.container2.remoteView.initiateCallView(
                it
            )
        }
        getVdotok()?.rootEglBaseContext?.let {
            binding.containerVideoFrame.container3.remoteView.initiateCallView(
                it
            )
        }
        getVdotok()?.rootEglBaseContext?.let {
            binding.containerVideoFrame.container4.remoteView.initiateCallView(
                it
            )
        }
    }

    private fun updateCallUIViews(listUser: List<Participants>?) {

        listUser?.let {
            if (it.size == 1) {
                binding.containerVideoFrame.container1.root.show()
                binding.containerVideoFrame.centerPoint.hide()
                binding.containerVideoFrame.container2.root.hide()
                binding.containerVideoFrame.container3.root.hide()
                binding.containerVideoFrame.container4.root.hide()

                val params =
                    binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = binding.containerVideoFrame.containerParent.id
                params.bottomToBottom = binding.containerVideoFrame.containerParent.id
                binding.containerVideoFrame.container1.root.layoutParams = params
                binding.containerVideoFrame.container1.root.requestLayout()

                ViewCompat.setElevation(binding.containerVideoFrame.container1.root, -1f)
                binding.containerVideoFrame.container1.remoteView.preview.setZOrderOnTop(false)

            } else if (it.size == 2) {

                binding.containerVideoFrame.container1.root.show()
                binding.containerVideoFrame.centerPoint.show()
                binding.containerVideoFrame.container2.root.show()
                binding.containerVideoFrame.container3.root.hide()
                binding.containerVideoFrame.container4.root.hide()

                val params =
                    binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = binding.containerVideoFrame.containerParent.id
                params.bottomToBottom = binding.containerVideoFrame.centerPoint.id
                binding.containerVideoFrame.container1.root.layoutParams = params
                binding.containerVideoFrame.container1.root.requestLayout()


                val params2 =
                    binding.containerVideoFrame.container2.root.layoutParams as ConstraintLayout.LayoutParams
                params2.endToEnd = binding.containerVideoFrame.containerParent.id
                binding.containerVideoFrame.container2.root.layoutParams = params2
                binding.containerVideoFrame.container2.root.requestLayout()

                ViewCompat.setElevation(binding.containerVideoFrame.container1.root, -1f)
                ViewCompat.setElevation(binding.containerVideoFrame.container2.root, -1f)
                binding.containerVideoFrame.container1.remoteView.preview.setZOrderOnTop(false)
                binding.containerVideoFrame.container2.remoteView.preview.setZOrderOnTop(false)

            } else if (it.size == 3) {

                binding.containerVideoFrame.container1.root.show()
                binding.containerVideoFrame.centerPoint.show()
                binding.containerVideoFrame.container2.root.show()
                binding.containerVideoFrame.container3.root.show()
                binding.containerVideoFrame.container4.root.hide()

                val params =
                    binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = binding.containerVideoFrame.centerPoint.id
                params.bottomToBottom = binding.containerVideoFrame.centerPoint.id
                binding.containerVideoFrame.container1.root.layoutParams = params
                binding.containerVideoFrame.container1.root.requestLayout()

                val params2 =
                    binding.containerVideoFrame.container2.root.layoutParams as ConstraintLayout.LayoutParams
                params2.topToTop = binding.containerVideoFrame.centerPoint.id
                params2.startToStart = binding.containerVideoFrame.containerParent.id
                params2.endToEnd = binding.containerVideoFrame.containerParent.id
                binding.containerVideoFrame.container2.root.layoutParams = params2
                binding.containerVideoFrame.container2.root.requestLayout()
                ViewCompat.setElevation(binding.containerVideoFrame.container2.root, -1f)
                binding.containerVideoFrame.container2.remoteView.preview.setZOrderOnTop(false)

            } else if (it.size == 4) {

                binding.containerVideoFrame.container1.root.show()
                binding.containerVideoFrame.centerPoint.show()
                binding.containerVideoFrame.container2.root.show()
                binding.containerVideoFrame.container3.root.show()
                binding.containerVideoFrame.container4.root.show()

                val params =
                    binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = binding.containerVideoFrame.centerPoint.id
                params.bottomToBottom = binding.containerVideoFrame.centerPoint.id
                binding.containerVideoFrame.container1.root.layoutParams = params
                binding.containerVideoFrame.container1.root.requestLayout()

                val params2 =
                    binding.containerVideoFrame.container2.root.layoutParams as ConstraintLayout.LayoutParams
                params2.endToEnd = binding.containerVideoFrame.centerPoint.id
                binding.containerVideoFrame.container2.root.layoutParams = params2
                binding.containerVideoFrame.container2.root.requestLayout()
                ViewCompat.setElevation(binding.containerVideoFrame.container2.root, -1f)
                binding.containerVideoFrame.container2.remoteView.preview.setZOrderOnTop(false)

            }
        }
        Log.e("user", "user-list size " + listUser?.size)

        ViewCompat.setElevation(binding.localView, 11f)
        binding.localView.preview.setZOrderMediaOverlay(true)
        binding.localView.preview.setZOrderOnTop(true)
    }

    /**
     * Function to start the timer when call is connected
     * */
    private fun startTimer() {
        if (timer != null) {
            stopTimer()
        }
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    callDuration = callDuration.plus(1)
                    binding.tvTime.text = getTimeFromSeconds(callDuration)
                }
            }
        }
        timer?.scheduleAtFixedRate(timerTask, 1000, 1000)
    }

    /**
     * Function to stop the timer when call is disconnected
     * */
    private fun stopTimer() {
        callDuration = 0
        timerTask?.cancel()
        timerTask = null
        timer?.purge()
        timer?.cancel()
        timer = null
        binding.tvTime.text = getTimeFromSeconds(callDuration)
    }

    companion object {
        const val VOICE_CALL = "VoiceCallFragment"

        @JvmStatic
        fun newInstance() = CallFragment()
    }

    override fun onIncomingCall(model: CallParams) {}

    override fun onStartCalling() {}

    override fun outGoingCall(toPeer: GroupModel) {}

    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String) {
        val mainHandler = activity?.mainLooper?.let { Handler(it) }
        val myRunnable = Runnable {

            try {
                var container = userMap.get(refId)

                if (container == null) {

                    container = getContainerParticipantView()
                    userMap.put(refId, container)

                    listUser.add(Participants(null, null, null, null, refId, null))
                    updateCallUIViews(listUser)

                    stream.addSink(container.remoteView.setView())
                } else {
                    container?.let {
                        container = getContainerParticipantView()
                        userMap[refId] = it
                        stream.addSink(it.remoteView.setView())
                    }
                }
            } catch (e: Exception) {
                Log.e("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
            }

        }
        mainHandler?.post(myRunnable)
    }

    override fun onRemoteStreamReceived(refId: String, sessionID: String) {
        val mainHandler = activity?.mainLooper?.let { Handler(it) }
        val myRunnable = Runnable {

            try {
                val view = userMap.get(refId)

                if (view == null) {

                    val container = getContainerParticipantView()
                    container.root.show()
                    userMap.put(refId, container)
                    listUser.add(Participants(null, null, null, null, refId, null))
                    updateCallUIViews(listUser)
                    setUserNameUI(container, refId)
                    container.groupAudioCall.show()
                    container.remoteView.hide()
                    binding.localView.hide()
                }
            } catch (e: Exception) {
                Log.e("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
            }

        }
        mainHandler?.post(myRunnable)
    }

    private fun setUserNameUI(container: LayoutCallingUserBinding, refId: String) {

        var fullName = groupModel?.participants?.find { it.refId == refId }?.fullname

        if (fullName == null) {
            groupList.forEach { group ->
                group.participants?.forEach {
                    if (it.refId == refId) {
                        fullName = it.fullname
                    }
                }
            }
        }

        if (!TextUtils.isEmpty(fullName))
            container.tvUserName.setText(fullName)

    }


    private fun getContainerParticipantView(): LayoutCallingUserBinding {

        return if (!userMap.containsValue(binding.containerVideoFrame.container1))
            binding.containerVideoFrame.container1
        else if (!userMap.containsValue(binding.containerVideoFrame.container2))
            binding.containerVideoFrame.container2
        else if (!userMap.containsValue(binding.containerVideoFrame.container3))
            binding.containerVideoFrame.container3
        else
            binding.containerVideoFrame.container4

    }


    override fun onCameraStreamReceived(stream: VideoTrack) {

        val mainHandler = activity?.let { Handler(it.mainLooper) }
        val myRunnable = Runnable {

            try {
                stream.addSink(binding.localView.setView())
                binding.localView.preview.setMirror(true)
                ViewCompat.setElevation(binding.localView, 11f)
                binding.localView.preview.setZOrderMediaOverlay(true)
                binding.localView.preview.setZOrderOnTop(true)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SocketLog", "onCameraStreamReceived: exception" + e.printStackTrace())
            }
        }
        mainHandler?.post(myRunnable)
    }

    override fun onCameraAudioOff(audioState: Int, videoState: Int, refId: String) {
        activity?.runOnUiThread {
            val view = userMap.get(refId)
            view?.let {
                if (videoState == 0) {
                    view.remoteView.showHideAvatar(true)
                } else {
                    view.remoteView.showHideAvatar(false)

                }
            }
        }
    }

    override fun onCallMissed() {
        try {
            listUser.clear()
            (this.activity as DashBoardActivity).sessionId = null
            Navigation.findNavController(binding.root).navigate(R.id.action_open_groupList)
        } catch (e: Exception) {
        }
    }

    override fun onCallEnd() {
        if (isVideoCall) {
            releaseCallViews()
        }
        try {
            listUser.clear()
            (this.activity as DashBoardActivity).sessionId = null
            Navigation.findNavController(binding.root).navigate(R.id.action_open_groupList)
        } catch (e: Exception) {
        }
    }

    override fun onParticipantLeftCall(refId: String?) {
        refId?.let {
            val view = userMap.get(refId)
            view?.let {
                val participant = listUser.find { it.refId == refId }
                participant?.let {
                    listUser.remove(participant)
                }
                view.remoteView.hide()
                view.imgCallOff.show()
                view.groupAudioCall.show()
                userMap.remove(refId)
            }
        }
    }

    override fun onDestroyView() {
        (activity as DashBoardActivity).endCall()
        super.onDestroyView()
    }

    override fun onCallRejected(reason: String) {
        (activity as DashBoardActivity).mLiveDataLeftParticipant.postValue(reason)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addTouchEventListener() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.localView.setOnTouchListener(View.OnTouchListener { view, event ->
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        rightDX = (binding.localView.x - event.rawX).toInt()
                        rightDY = (binding.localView.y - event.rawY).toInt()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val displacementX = event.rawX + rightDX
                        val displacementY = event.rawY + rightDY

                        binding.localView.animate()
                            .x(displacementX)
                            .y(displacementY)
                            .setDuration(0)
                            .start()

                        Handler(Looper.getMainLooper()).postDelayed({

                            xPoint = view.x + view.width
                            yPoint = view.y + view.height

                            when {
                                xPoint > screenWidth / 2 && yPoint < screenHeight / 2 -> {
                                    //First Quadrant
                                    animateView(
                                        ((screenWidth - (view.width + THRESHOLD_VALUE))),
                                        (screenHeight / 2 - (view.height + THRESHOLD_VALUE))
                                    )
                                }
                                xPoint < screenWidth / 2 && yPoint < screenHeight / 2 -> {
                                    //Second Quadrant
                                    animateView(
                                        THRESHOLD_VALUE,
                                        (screenHeight / 2 - (view.height + THRESHOLD_VALUE))
                                    )
                                }
                                xPoint < screenWidth / 2 && yPoint > screenHeight / 2 -> {
                                    //Third Quadrant
                                    animateView(
                                        THRESHOLD_VALUE,
                                        (screenHeight / 2 + view.height / 2).toFloat()
                                    )
                                }
                                else -> {
                                    //Fourth Quadrant
                                    animateView(
                                        ((screenWidth - (view.width + THRESHOLD_VALUE))),
                                        (screenHeight / 2 + view.height / 2).toFloat()
                                    )
                                }
                            }

                        }, 100)

                    }
                    else -> { // Note the block
                        return@OnTouchListener false
                    }
                }
                true
            })
        }, 1500)
    }

    private fun animateView(xPoint: Float, yPoint: Float) {
        binding.localView.animate()
            .x(xPoint)
            .y(yPoint)
            .setDuration(200)
            .start()
    }

    override fun onInsuficientBalance() {
        closeFragmentWithMessage("Insufficient Balance!")
    }

    private fun closeFragmentWithMessage(message: String?) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onCallEnd()
        }
    }

    fun getCallTitle(customObject: String): String? {
        val name = Gson().fromJson(customObject, CallNameModel::class.java)
        return name.groupName
    }


}