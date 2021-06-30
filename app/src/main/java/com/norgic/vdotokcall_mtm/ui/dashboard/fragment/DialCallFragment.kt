package com.norgic.vdotokcall_mtm.ui.dashboard.fragment

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
import androidx.fragment.app.FragmentManager
import androidx.navigation.Navigation
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.FragmentDialCallBinding
import com.norgic.vdotokcall_mtm.extensions.hide
import com.norgic.vdotokcall_mtm.extensions.show
import com.norgic.vdotokcall_mtm.fragments.CallMangerListenerFragment
import com.norgic.vdotokcall_mtm.models.AcceptCallModel
import com.norgic.vdotokcall_mtm.models.GroupModel
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.ui.dashboard.DashBoardActivity
import com.norgic.vdotokcall_mtm.utils.performSingleClick
import com.razatech.callsdks.CallClient
import com.razatech.callsdks.enums.MediaType
import org.webrtc.VideoTrack
import java.lang.Exception


/**
 * Created By: Norgic
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays incoming and outgoing call
 */
class DialCallFragment : CallMangerListenerFragment() {

    private var isIncomingCall: Boolean = false
    private lateinit var binding: FragmentDialCallBinding
    var groupModel : GroupModel? = null
    var username : String? = null

    var acceptCallModel : AcceptCallModel? = null
    private var groupList = ArrayList<GroupModel>()
    var isVideoCall: Boolean = false

    var userName : ObservableField<String> = ObservableField<String>()
    var incomingCallTitle : ObservableField<String> = ObservableField<String>()
    var player: MediaPlayer?= null

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

        return binding.root
    }
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
        arguments?.get(GroupModel.TAG)?.let {
            isVideoCall = arguments?.getBoolean(IS_VIDEO_CALL) ?: false
            groupModel = it as GroupModel?
            isIncomingCall = arguments?.get("isIncoming") as Boolean
        } ?: kotlin.run{
            groupList = arguments?.get("grouplist") as ArrayList<GroupModel>
            username = arguments?.get("userName") as String?
            acceptCallModel = arguments?.get(AcceptCallModel.TAG) as AcceptCallModel?
            isIncomingCall =  arguments?.get("isIncoming") as Boolean
        }
    }
    /**
     * Function to set data when outgoing call dial is implemented and setonClickListener
     * */
    private fun setDataForDialCall() {

        getUsername()
        binding.imgCallAccept.hide()
        binding.imgmic.show()
        binding.imgCamera.show()
        incomingCallTitle.set(getString(R.string.calling))

        binding.imgCallReject.performSingleClick {
            (activity as DashBoardActivity).endCall()
            Navigation.findNavController(binding.imgCallReject).navigate(R.id.action_move_to_groups)
        }

    }


    /**
     * Function to set user/users  name when outgoing call dial is implemented
     * */
    private fun getUsername() {
      groupModel.let { it ->
            if(groupModel?.autoCreated == 1){
                it?.participants?.forEach { name->
                    if (name.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        userName.set(name.fullname)

                    }
                }
            } else {
                var participantNames = ""
                it?.participants?.forEach {
                    if (it.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        participantNames += it.fullname.plus("\n")
                    }
                }
                userName.set(participantNames)
            }
        }
    }

    /**
     * Function to set data when incoming call dial is implemented and setonClickListener
     * */
    private fun setDataForIncomingCall() {

        player = MediaPlayer.create(this.requireContext(), Settings.System.DEFAULT_RINGTONE_URI)
        player?.start()

        userName.set(username)
        when (acceptCallModel?.mediaType) {
            MediaType.AUDIO -> {
                incomingCallTitle.set(getString(R.string.incoming_call))
            }
            else -> {
                binding.imgCallAccept.setImageResource(R.drawable.ic_call_video_rounded)
                incomingCallTitle.set(getString(R.string.incoming_video_call))
            }
        }

        binding.imgCallAccept.performSingleClick {
           openAudioCallFragment()
        }

        binding.imgCallReject.performSingleClick {
            prefs.loginInfo?.let {
                acceptCallModel?.let { it1 -> callClient.rejectIncomingCall(
                    it.refId!!,
                    it1.sessionUUID
                )
                }
            }
            Navigation.findNavController(binding.imgCallReject).navigate(R.id.action_move_to_groups)
        }
    }
    /**
     * Function to be call when incoming dial call is accepted
     * */
    private fun openAudioCallFragment() {

        acceptCallModel?.let {

            (activity as DashBoardActivity).acceptIncomingCall(
                it.from,
                it.sessionUUID,
                it.requestID,
                it.deviceType,
                it.mediaType,
                it.sessionType
            )
            openCallFragment()
        }
    }

    override fun onDetach() {
        super.onDetach()
        player?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.stop()
    }

    /**
     * Function to pass data to oter fragment in case of incoming call dial
     * */
    private fun openCallFragment() {
        val bundle = Bundle()
        bundle.putParcelableArrayList("grouplist", groupList)
        bundle.putString("userName", userName.get())
        bundle.putBoolean(IS_VIDEO_CALL, acceptCallModel?.mediaType == MediaType.VIDEO)
        bundle.putParcelable(AcceptCallModel.TAG, acceptCallModel)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_call_fragment, bundle)
    }


    companion object {
        const val IS_VIDEO_CALL = "IS_VIDEO_CALL"

        const val TAG = "DialCallFragment"
        @JvmStatic
        fun newInstance() = DialCallFragment()

    }

    override fun onIncomingCall(model: AcceptCallModel) {

    }

    override fun onStartCalling() {
        activity?.let {
            it.runOnUiThread {
                val bundle = Bundle()
                bundle.putParcelable(GroupModel.TAG, groupModel)
                bundle.putBoolean(IS_VIDEO_CALL, isVideoCall)
                bundle.putBoolean("isIncoming", false)
                Navigation.findNavController(binding.root).navigate(
                    R.id.action_open_call_fragment,
                    bundle
                )
            }
        }
    }

    override fun outGoingCall(toPeer: GroupModel) {

    }

    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String) {}

    override fun onCameraStreamReceived(stream: VideoTrack) {}
    override fun onCameraAudioOff(audioState: Int, videoState: Int, refId: String) {}

    override fun onCallRejected(reason: String) {
//        closeFragmentWithMessage(reason)
    }

    override fun onParticipantLeftCall(refId: String?) {

    }

    override fun onCallMissed() {
       closeFragmentWithMessage("call missed!")
    }

    override fun onCallEnd() {
        activity?.runOnUiThread {
            try {
                Navigation.findNavController(binding.root).navigate(R.id.action_move_to_groups)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun closeFragmentWithMessage(message: String?) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            Navigation.findNavController(binding.root).navigate(R.id.action_move_to_groups)
        }
    }
}