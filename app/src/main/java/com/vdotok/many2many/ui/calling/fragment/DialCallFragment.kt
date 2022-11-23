package com.vdotok.many2many.ui.calling.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.vdotok.many2many.R
import com.vdotok.many2many.base.BaseActivity
import com.vdotok.many2many.base.BaseFragment
import com.vdotok.many2many.databinding.FragmentDialCallBinding
import com.vdotok.many2many.extensions.hide
import com.vdotok.many2many.extensions.launchPeriodicAsync
import com.vdotok.many2many.extensions.show
import com.vdotok.many2many.extensions.showSnackBar
import com.vdotok.many2many.models.CallNameModel
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.ui.calling.CallActivity
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.many2many.utils.performSingleClick
import com.vdotok.network.models.GroupModel
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.models.CallParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.webrtc.VideoTrack


/**
 * Created By: VdoTok
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays incoming and outgoing call
 */
class DialCallFragment : BaseFragment() {

    private var isIncomingCall: Boolean = false
    private lateinit var binding: FragmentDialCallBinding
    var groupModel : GroupModel? = null
    var username : String? = null

    var callParams : CallParams? = null
    private var groupList = ArrayList<GroupModel>()
    var isVideoCall: Boolean = false

    var userName : ObservableField<String> = ObservableField<String>()
    var incomingCallTitle : ObservableField<String> = ObservableField<String>()
    var player: MediaPlayer?= null
    private var timerFro30sec: Deferred<Unit> ?= null

    private lateinit var callClient: CallClient
    private lateinit var prefs: Prefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDialCallBinding.inflate(inflater, container, false)
        prefs = Prefs(activity)
        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }

        setArgumentsData()
        setBindingData()

        when {
            isIncomingCall -> setDataForIncomingCall()
            else -> setDataForDialCall()
        }

        if (isIncomingCall) {
            timerFro30sec = CoroutineScope(Dispatchers.IO).launchPeriodicAsync(1000 * 15) {
                test++
                if (test >= 2) {
                    activity?.runOnUiThread {
                      rejectCall()
                    }
                    timerFro30sec?.cancel()
                }
            }
        }

        return binding.root
    }

    private fun timeOutCall() {

        if (isIncomingCall) {
            prefs.loginInfo?.let {
                callParams?.let { it1 ->
                    callClient.callTimeout(it.refId.toString(), it1.sessionUUID)
                }
            }
            activity?.finish()
        }
    }

    var test = 0
    /**
     * Function to link binding data
     * */
    private fun setBindingData() {
        binding.username = userName
        binding.incomingCallTitle = incomingCallTitle
    }

    /**
     * Function to get ths pass data from other fragment
     * */
    private fun setArgumentsData() {
        groupList.clear()
        isIncomingCall = arguments?.get(IS_IN_COMING_CALL) as Boolean
        isVideoCall = arguments?.getBoolean(IS_VIDEO_CALL) ?: false

        arguments?.get(GroupModel.TAG)?.let {
            groupModel = it as GroupModel?
        } ?: kotlin.run {
            groupList = arguments?.getParcelableArrayList<GroupModel>(GROUP_LIST) as ArrayList<GroupModel>
            callParams = arguments?.get(ApplicationConstants.CALL_PARAMS) as CallParams?
            username = getCallTitle(callParams?.customDataPacket.toString())
        }
    }

    fun getCallTitle(customObject: String): String? {
        val name = Gson().fromJson(customObject, CallNameModel::class.java)
        return name.groupName
    }
    /**
     * Function to set data when outgoing call dial is implemented and setonClickListener
     * */
    private fun setDataForDialCall() {
        userName.set(groupModel?.groupTitle)
        binding.imgCallAccept.hide()
        binding.imgmic.show()
        binding.imgCamera.show()
        incomingCallTitle.set(getString(R.string.calling))

        binding.imgCallReject.performSingleClick {
            rejectCall()
        }

    }

    /**
     * Function to set data when incoming call dial is implemented and setonClickListener
     * */
    private fun setDataForIncomingCall() {

        player = MediaPlayer.create(this.requireContext(), Settings.System.DEFAULT_RINGTONE_URI)
        player?.start()

        userName.set(username)
        when (callParams?.mediaType) {
            MediaType.AUDIO -> {
                incomingCallTitle.set(getString(R.string.incoming_call))
            }
            else -> {
                binding.imgCallAccept.setImageResource(R.drawable.ic_call_video_rounded)
                incomingCallTitle.set(getString(R.string.incoming_video_call))
            }
        }

        binding.imgCallAccept.performSingleClick {
           acceptIncomingCall()
        }

        binding.imgCallReject.performSingleClick {
            rejectCall()
        }
    }

    private fun rejectCall() {
        timerFro30sec?.cancel()
        if (isIncomingCall) {
            prefs.loginInfo?.let {
                callParams?.let { it1 -> callClient.rejectIncomingCall(
                    it.refId!!,
                    it1.sessionUUID)
                }
            }
            activity?.finish()
        } else {
            (activity as CallActivity).endCall()
        }
    }
    /**
     * Function to be call when incoming dial call is accepted
     * */
    private fun acceptIncomingCall() {

        callParams?.let {
            (activity as CallActivity).acceptIncomingCall(it)
            openCallFragment()
        }
        timerFro30sec?.cancel()
    }

    override fun onDetach() {
        timerFro30sec?.cancel()
        super.onDetach()
        player?.stop()
    }

    override fun onDestroy() {
        timerFro30sec?.cancel()
        super.onDestroy()
        player?.stop()
    }

    /**
     * Function to pass data to oter fragment in case of incoming call dial
     * */
    private fun openCallFragment() {
        val bundle = Bundle()
        bundle.putParcelableArrayList(GROUP_LIST, groupList)
        bundle.putString(USER_NAME, userName.get())
        bundle.putBoolean(IS_VIDEO_CALL, callParams?.mediaType == MediaType.VIDEO)
        bundle.putParcelable(ApplicationConstants.CALL_PARAMS, callParams)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_call_fragment, bundle)
    }


    companion object {
        const val IS_VIDEO_CALL = "video_call"
        const val IS_IN_COMING_CALL = "in_coming_call"
        const val USER_NAME = "user_name"
        const val GROUP_LIST = "group_list"

        const val TAG = "DialCallFragment"
        @JvmStatic
        fun newInstance() = DialCallFragment()

    }

    override fun onIncomingCall(model: CallParams) {}

    override fun onStartCalling() {
        activity?.let {
            it.runOnUiThread {
                val bundle = Bundle()
                bundle.putParcelable(GroupModel.TAG, groupModel)
                bundle.putBoolean(IS_VIDEO_CALL, isVideoCall)
                bundle.putBoolean(IS_IN_COMING_CALL, false)
                Navigation.findNavController(binding.root).navigate(R.id.action_open_call_fragment, bundle)
            }
        }
    }

    override fun outGoingCall(toPeer: GroupModel) {
        closeFragmentWithMessage("Call Missed!")
    }

    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String) {}

    override fun onCameraStreamReceived(stream: VideoTrack) {}
    override fun onCameraAudioOff(audioState: Int, videoState: Int, refId: String) {}

    override fun onCallRejected(reason: String) {}

    override fun onParticipantLeftCall(refId: String?) {}

    override fun onCallerAlreadyBusy() {
        closeFragmentWithMessage("Target is busy!")
    }

    override fun noAnsFromTarget() {
        activity?.runOnUiThread {
            binding.root.showSnackBar("No answer from target")
        }
    }

    override fun onCallMissed() {
       closeFragmentWithMessage("Call Missed!")
    }

    override fun onInsuficientBalance() {
        closeFragmentWithMessage("Insufficient Balance!")
    }

    override fun onCallEnd() {
        activity?.finish()
    }

    private fun closeFragmentWithMessage(message: String?) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onCallEnd()
        }
    }
}