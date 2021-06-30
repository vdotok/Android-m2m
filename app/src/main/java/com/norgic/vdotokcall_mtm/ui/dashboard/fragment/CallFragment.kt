package com.norgic.vdotokcall_mtm.feature.dashBoard.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.LayoutCallingUserBinding
import com.norgic.vdotokcall_mtm.databinding.LayoutFragmentCallBinding
import com.norgic.vdotokcall_mtm.extensions.hide
import com.norgic.vdotokcall_mtm.extensions.show
import com.norgic.vdotokcall_mtm.fragments.CallMangerListenerFragment
import com.norgic.vdotokcall_mtm.models.AcceptCallModel
import com.norgic.vdotokcall_mtm.models.GroupModel
import com.norgic.vdotokcall_mtm.models.Participants
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.ui.dashboard.DashBoardActivity
import com.norgic.vdotokcall_mtm.ui.dashboard.adapter.GridUserAdapter
import com.norgic.vdotokcall_mtm.ui.dashboard.fragment.DialCallFragment
import com.norgic.vdotokcall_mtm.utils.TimeUtils.getTimeFromSeconds
import com.norgic.vdotokcall_mtm.utils.performSingleClick
import com.razatech.callsdks.CallClient
import kotlinx.android.synthetic.main.fragment_dial_call.view.*
import kotlinx.android.synthetic.main.layout_calling_user.view.*
import kotlinx.android.synthetic.main.layout_fragment_call.*
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.util.*


const val THRESHOLD_VALUE = 70.0f

/**
 * Created By: Norgic
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays the on connected call
 */
class CallFragment : CallMangerListenerFragment() {

    private var acceptCallModel: AcceptCallModel? = null
    private var isIncomingCall = false
    private lateinit var binding: LayoutFragmentCallBinding

    private lateinit var callClient: CallClient
    private var groupModel : GroupModel? = null
    private var name : String? = null
    private lateinit var prefs: Prefs

    private var userMap = mutableMapOf<String, LayoutCallingUserBinding>()
    private var userName : ObservableField<String> = ObservableField<String>()
    private var groupList = ArrayList<GroupModel>()

    private var isMuted = false
    private var isSpeakerOff = true
    private var isVideoCall = false
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
    private val listUser =  ArrayList<Participants>()

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

        (activity as DashBoardActivity).mListener = this

        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }
        groupList.clear()
        isVideoCall = arguments?.getBoolean(DialCallFragment.IS_VIDEO_CALL) ?: false

        arguments?.get(GroupModel.TAG)?.let {
            groupModel = it as GroupModel?
            isIncomingCall = arguments?.get("isIncoming") as Boolean
//            getUserName(groupModel,isVideoCall)
        } ?: kotlin.run {
            groupList = arguments?.get("grouplist") as ArrayList<GroupModel>
            name = (arguments?.get("userName") as CharSequence?).toString()
            acceptCallModel = arguments?.getParcelable(AcceptCallModel.TAG) as AcceptCallModel?
            isIncomingCall = true
        }

        getIncomingUserName(isVideoCall)
        displayUi(isVideoCall)

        binding.imgCallOff.performSingleClick {
            stopTimer()
            (activity as DashBoardActivity).endCall()
            Navigation.findNavController(binding.root).navigate(R.id.action_open_groupList)
        }

        binding.imgMute.setOnClickListener {

            isMuted = !isMuted
            if (isMuted) {
                binding.imgMute.setImageResource(R.drawable.ic_mute_mic1)
            } else {
                binding.imgMute.setImageResource(R.drawable.ic_unmute_mic)
            }
            (activity as DashBoardActivity).muteUnMuteCall()
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

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true // default to enabled
        ) { override fun handleOnBackPressed() {}
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner,  // LifecycleOwner
            callback
        )
        binding.ivCamSwitch.setOnClickListener { (activity as DashBoardActivity).switchCamera() }

        binding.imgCamera.setOnClickListener {
            if (isCallTypeAudio) {
                return@setOnClickListener
            }
            if (isVideoCall) {
                binding.localViewCard.hide()
                binding.localView.hide()
                (activity as DashBoardActivity).pauseVideo()
                binding.imgCamera.setImageResource(R.drawable.ic_video_off)
            } else {
                binding.localViewCard.show()
                binding.localView.show()
                refreshLocalCameraView(null)
                (activity as DashBoardActivity).resumeVideo()
                binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
            }
            isVideoCall = !isVideoCall
            (activity as DashBoardActivity).switchCallType(isVideoCall)
        }

        addTouchEventListener()

        screenWidth = context?.resources?.displayMetrics?.widthPixels!!
        screenHeight = context?.resources?.displayMetrics?.heightPixels!!

        (activity as DashBoardActivity).localStream?.let { onCameraStreamReceived(it) }

        binding.containerVideoFrame.container1.root.setTag("1")
        binding.containerVideoFrame.container2.root.setTag("2")
        binding.containerVideoFrame.container3.root.setTag("3")
        binding.containerVideoFrame.container4.root.setTag("4")

    }
    /**
     * Function to set user name when call connected from incoming call dial
     * @param videoCall videoCall to check whether its an audio or video call
     * */
    private fun getIncomingUserName(videoCall: Boolean) {
        if (!videoCall){
            binding.tvCallType.text = getString(R.string.audio_calling)
        } else {
            binding.tvCallType.text = getString(R.string.video_calling)
        }

    }

    /**
     * Function to set user name when call connected from outgoing call dial
     * @param videoCall videoCall to check whether its an audio or video call
     * @param groupModel groupModel object is to get group details
     * */
    private fun getUserName(groupModel: GroupModel?, videoCall: Boolean) {
       groupModel.let { it ->
            if (groupModel?.autoCreated == 1 && !videoCall) {
                binding.tvCallType.text = getString(R.string.audio_calling)
                it?.participants?.forEach { name->
                    if (name.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        userName.set(name.fullname)

                    }
                }
            } else if (groupModel?.autoCreated == 0 && !videoCall) {
                binding.tvCallType.text = getString(R.string.group_audio_calling)
                var participantNames = ""
                it?.participants?.forEach {
                    if (it.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        participantNames += it.fullname.plus("\n")
                    }
                }
                userName.set(participantNames)

            } else if (groupModel?.autoCreated == 1 && videoCall) {
                binding.tvCallType.text = getString(R.string.video_calling)
                it?.participants?.forEach { name->
                    if (name.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        userName.set(name.fullname)

                    }
                }
            } else {
                binding.tvCallType.text = getString(R.string.group_video_calling)
                userName.set(it?.groupTitle)

            }
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
        userName.set("A Group")

        if (!videoCall) {
            isCallTypeAudio = true
            binding.containerVideoFrame.container1.remoteView.hide()
            binding.containerVideoFrame.container2.remoteView.hide()
            binding.containerVideoFrame.container3.remoteView.hide()
            binding.containerVideoFrame.container4.remoteView.hide()

            binding.containerVideoFrame.container1.groupAudioCall.show()
            binding.containerVideoFrame.container2.groupAudioCall.show()
            binding.containerVideoFrame.container3.groupAudioCall.show()
            binding.containerVideoFrame.container4.groupAudioCall.show()

            binding.imgCamera.setImageResource(R.drawable.ic_video_off)
            binding.localViewCard.hide()
            binding.ivCamSwitch.hide()

        } else {
            isCallTypeAudio = false

            binding.containerVideoFrame.container1.groupAudioCall.hide()
            binding.containerVideoFrame.container2.groupAudioCall.hide()
            binding.containerVideoFrame.container3.groupAudioCall.hide()
            binding.containerVideoFrame.container4.groupAudioCall.hide()

            binding.containerVideoFrame.container1.remoteView.show()
            binding.containerVideoFrame.container2.remoteView.show()
            binding.containerVideoFrame.container3.remoteView.show()
            binding.containerVideoFrame.container4.remoteView.show()

            binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)

        }

//        updateCallUIViews(listUser)
    }

    private fun updateCallUIViews(listUser: List<Participants>?) {

        listUser?.let {
            if (it.size == 1) {
                binding.containerVideoFrame.centerPoint.hide()
                binding.containerVideoFrame.container2.root.hide()
                binding.containerVideoFrame.container3.root.hide()
                binding.containerVideoFrame.container4.root.hide()

                val params = binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = binding.containerVideoFrame.containerParent.id
                params.bottomToBottom = binding.containerVideoFrame.containerParent.id
                binding.containerVideoFrame.container1.root.layoutParams = params
                binding.containerVideoFrame.container1.root.requestLayout()

                ViewCompat.setElevation(binding.containerVideoFrame.container1.root, -1f)

            } else if (it.size == 2) {

                binding.containerVideoFrame.centerPoint.show()
                binding.containerVideoFrame.container2.root.show()
                binding.containerVideoFrame.container3.root.hide()
                binding.containerVideoFrame.container4.root.hide()

                val params = binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = binding.containerVideoFrame.containerParent.id
                params.bottomToBottom = binding.containerVideoFrame.centerPoint.id
                binding.containerVideoFrame.container1.root.layoutParams = params
                binding.containerVideoFrame.container1.root.requestLayout()


                val params2 = binding.containerVideoFrame.container2.root.layoutParams as ConstraintLayout.LayoutParams
                params2.endToEnd = binding.containerVideoFrame.containerParent.id
                binding.containerVideoFrame.container2.root.layoutParams = params2
                binding.containerVideoFrame.container2.root.requestLayout()

                ViewCompat.setElevation(binding.containerVideoFrame.container1.root, -1f)
                ViewCompat.setElevation(binding.containerVideoFrame.container2.root, -1f)

            } else if (it.size == 3) {

                binding.containerVideoFrame.centerPoint.show()
                binding.containerVideoFrame.container2.root.show()
                binding.containerVideoFrame.container3.root.show()
                binding.containerVideoFrame.container4.root.hide()

                val params = binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = binding.containerVideoFrame.centerPoint.id
                params.bottomToBottom = binding.containerVideoFrame.centerPoint.id
                binding.containerVideoFrame.container1.root.layoutParams = params
                binding.containerVideoFrame.container1.root.requestLayout()

                val params2 = binding.containerVideoFrame.container2.root.layoutParams as ConstraintLayout.LayoutParams
                params2.topToTop = binding.containerVideoFrame.centerPoint.id
                params2.startToStart = binding.containerVideoFrame.containerParent.id
                params2.endToEnd = binding.containerVideoFrame.containerParent.id
                binding.containerVideoFrame.container2.root.layoutParams = params2
                binding.containerVideoFrame.container2.root.requestLayout()

                ViewCompat.setElevation(binding.containerVideoFrame.container2.root, -1f)

            } else if (it.size == 4) {

                binding.containerVideoFrame.centerPoint.show()
                binding.containerVideoFrame.container2.root.show()
                binding.containerVideoFrame.container3.root.show()
                binding.containerVideoFrame.container4.root.show()

                val params = binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = binding.containerVideoFrame.centerPoint.id
                params.bottomToBottom = binding.containerVideoFrame.centerPoint.id
                binding.containerVideoFrame.container1.root.layoutParams = params
                binding.containerVideoFrame.container1.root.requestLayout()

                val params2 = binding.containerVideoFrame.container2.root.layoutParams as ConstraintLayout.LayoutParams
                params2.endToEnd = binding.containerVideoFrame.centerPoint.id
                binding.containerVideoFrame.container2.root.layoutParams = params2
                binding.containerVideoFrame.container2.root.requestLayout()
                ViewCompat.setElevation(binding.containerVideoFrame.container2.root, -1f)

            }
        }
        Log.e("user", "user-list size " + listUser?.size)

        refreshLocalCameraView(null)

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

    override fun onIncomingCall(model: AcceptCallModel) {}

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

                    val videoView =  container.remoteView

                    listUser.add(Participants(null, null, null, null, refId, null))
                    updateCallUIViews(listUser)

                    setUserNameUI(container, refId)
                    videoView.setMirror(true)
                    val rootEglBase = EglBase.create()
                    videoView.init(rootEglBase.eglBaseContext, null)
                    videoView.setZOrderMediaOverlay(false)
                    videoView.setEnableHardwareScaler(false)
                    ViewCompat.setElevation(videoView, -1f)
                    refreshLocalCameraView(videoView)

                    stream.addSink(videoView)

                    videoView.postDelayed({
                        isSpeakerOff = false
                        callClient.toggleSpeakerOnOff()
                    }, 1000)

                }
            } catch (e: Exception) {
                Log.i("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
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
                    userMap.put(refId, container)
                    listUser.add(Participants(null, null, null, null, refId, null))
                    updateCallUIViews(listUser)
                    container.groupAudioCall.show()
                    container.remoteView.hide()
                }
            } catch (e: Exception) {
                Log.i("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
            }

        }
        mainHandler?.post(myRunnable)
    }

    private fun setUserNameUI(container: LayoutCallingUserBinding, refId: String) {

        groupModel?.participants?.let {

            it.forEach {
                if (it.refId == refId) {
                    container.tvUserName.setText(it.fullname)
                }
            }

        } ?: run {
            groupList.forEach { group ->
                group.participants.forEach { participant ->
                    if (participant.refId == refId) {
                        container.tvUserName.setText(participant.fullname)
                    }
                }
            }
        }
    }


    private fun getContainerParticipantView() : LayoutCallingUserBinding {

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
                val videoView = binding.localView
                videoView.setMirror(true)
                val rootEglBase = EglBase.create()
                videoView.init(rootEglBase.eglBaseContext, null)

                ViewCompat.setElevation(binding.containerVideoFrame.root, -1f)
                ViewCompat.setElevation(videoView, 10f)

                videoView.setZOrderMediaOverlay(true)
                videoView.setEnableHardwareScaler(true)
                stream.addSink(videoView)
                callClient.setView(videoView)
                refreshLocalCameraView(videoView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mainHandler?.post(myRunnable)
    }

    override fun onCameraAudioOff(audioState: Int, videoState: Int, refId: String) {

        activity?.runOnUiThread {

            val view = userMap.get(refId)
            view?.let {

                if (videoState == 0) {
                    view.groupAudioCall.show()
                    view.remoteView.hide()
                } else {
                    view.groupAudioCall.hide()
                    view.remoteView.show()

                    userMap.get(refId)?.let {
                        refreshLocalCameraView(it.remoteView)
                    }
                }
            }
        }
    }

    private fun refreshLocalCameraView(view: SurfaceViewRenderer?) {

        if (binding.localViewCard.isVisible) {

            view?.setZOrderMediaOverlay(false)

            binding.localView.setEnableHardwareScaler(true)
            binding.localView.setZOrderMediaOverlay(true)
            binding.localView.requestFocus()

            animateView(
                ((screenWidth - (binding.localView.width + THRESHOLD_VALUE))),
                (screenHeight / 2 + binding.localView.height / 2).toFloat()
            )

            ViewCompat.setElevation(binding.containerVideoFrame.root, -1f)
            ViewCompat.setElevation(binding.localViewCard, 11f)
            ViewCompat.setElevation(binding.localView, 11f)

            binding.localView.show()

        }
    }

    override fun onCallMissed() {
        try {
            listUser.clear()
            userMap.clear()
            (this.activity as DashBoardActivity).sessionId = null
            Navigation.findNavController(binding.root).navigate(R.id.action_open_groupList)
        } catch (e: Exception) {}
    }

    override fun onCallEnd() {
        try {
            listUser.clear()
            userMap.clear()
            (this.activity as DashBoardActivity).sessionId = null
            Navigation.findNavController(binding.root).navigate(R.id.action_open_groupList)
        } catch (e: Exception) {}
    }

    override fun onParticipantLeftCall(refId: String?) {

        refId?.let {

//            (binding.recyclerView.adapter as GridUserAdapter).removeUser(refId)


            val view = userMap.get(refId)
            Log.e(
                "callfrag",
                "onParticipantLeftCall view tag = " + view?.root?.tag + " --- refID = " + refId + " === user : " + userMap.size
            )

            view?.let {

                val participant = listUser.find { it.refId == refId }
                participant?.let {
                    listUser.remove(participant)
                }

                view.remoteView.clearImage()
                view.remoteView.hide()

                view.imgCallOff.show()
                view.groupAudioCall.show()
                userMap.remove(refId)

                refreshLocalCameraView(null)

            }

        }
    }

    override fun onDestroyView() {
//        stopTimer()
        (activity as DashBoardActivity).endCall()
        super.onDestroyView()
    }

    override fun onCallRejected(reason: String) {}

    @SuppressLint("ClickableViewAccessibility")
    private fun addTouchEventListener() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.localViewCard.setOnTouchListener(View.OnTouchListener { view, event ->
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        rightDX = (binding.localViewCard.x - event.rawX).toInt()
                        rightDY = (binding.localViewCard.y - event.rawY).toInt()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val displacementX = event.rawX + rightDX
                        val displacementY = event.rawY + rightDY

                        binding.localViewCard.animate()
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


    private fun animateView(xPoint: Float, yPoint: Float){
        binding.localViewCard.animate()
            .x(xPoint)
            .y(yPoint)
            .setDuration(200)
            .start()
    }

}