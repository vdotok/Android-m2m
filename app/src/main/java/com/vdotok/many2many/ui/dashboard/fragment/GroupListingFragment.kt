package com.vdotok.many2many.ui.dashboard.fragment

import android.app.AppOpsManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.ObservableField
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.google.gson.Gson
import com.vdotok.many2many.R
import com.vdotok.many2many.adapter.GroupsAdapter
import com.vdotok.many2many.base.BaseActivity
import com.vdotok.many2many.base.BaseFragment
import com.vdotok.many2many.databinding.FragmentGroupListingBinding
import com.vdotok.many2many.dialogs.UpdateGroupNameDialog
import com.vdotok.many2many.extensions.hide
import com.vdotok.many2many.extensions.show
import com.vdotok.many2many.extensions.showSnackBar
import com.vdotok.many2many.extensions.toggleVisibility
import com.vdotok.many2many.feature.account.viewmodel.GroupViewModel
import com.vdotok.many2many.models.CallNameModel
import com.vdotok.many2many.network.HttpResponseCodes
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.ui.account.AccountsActivity
import com.vdotok.many2many.ui.calling.CallActivity
import com.vdotok.many2many.ui.calling.fragment.DialCallFragment
import com.vdotok.many2many.ui.dashboard.DashBoardActivity
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.many2many.utils.isInternetAvailable
import com.vdotok.many2many.utils.showDeleteGroupAlert
import com.vdotok.network.models.AllGroupsResponse
import com.vdotok.network.models.DeleteGroupModel
import com.vdotok.network.models.GroupModel
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.enums.*
import com.vdotok.streaming.models.CallParams
import kotlinx.coroutines.*
import okhttp3.MediaType
import org.webrtc.VideoTrack
import retrofit2.HttpException
import java.util.*


/**
 * Created By: VdoTok
 * Date & Time: On 6/17/21 At 1:29 PM in 2021
 *
 * This class displays the list of groups that a user is connected to
 */
class GroupListingFragment : BaseFragment(), GroupsAdapter.InterfaceOnGroupMenuItemClick {

    private lateinit var binding: FragmentGroupListingBinding
    private lateinit var prefs: Prefs
    lateinit var adapter: GroupsAdapter
    private lateinit var callClient: CallClient
    var userName = ObservableField<String>()
    private var groupList = ArrayList<GroupModel>()
    var isVideoCall = false
    var user : String? = null
    var viewFrag: View? = null
    private val viewModelGroup : GroupViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        if (viewFrag == null) {

            binding = FragmentGroupListingBinding.inflate(inflater, container, false)
            prefs = Prefs(activity)

            viewFrag = binding.root
            init()
        }
        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }

        return binding.root
    }

    /**
     * Function for setOnClickListeners
     * */
    private fun init() {
        prefs = Prefs(this.context)
        binding.username = userName
        userName.set(prefs.loginInfo?.fullName)

        binding.customToolbar.tvTitle.text = getString(R.string.group_list_title)
        binding.customToolbar.imgArrowBack.hide()

        binding.customToolbar.imgDone.setOnClickListener {
          openUserListFragment()
        }

        binding.btnNewChat.setOnClickListener {
            openUserListFragment()
        }

        binding.btnRefresh.setOnClickListener {
            getAllGroups()
        }

        binding.tvLogout.setOnClickListener {
            (activity as DashBoardActivity).logout()
            prefs.deleteKeyValuePair(ApplicationConstants.LOGIN_INFO)
            startActivity(AccountsActivity.createAccountsActivity(this.requireContext()))
        }

        initUserListAdapter()
        getAllGroups()
        addPullToRefresh()
    }
    /**
     * Function for refreshing the updated group
     * */
    private fun addPullToRefresh() {
        binding.swipeRefreshLay.setOnRefreshListener {
            getAllGroups()
            (activity as DashBoardActivity).connectClient()
        }
    }

    private fun initUserListAdapter() {
        adapter = GroupsAdapter(
            prefs.loginInfo?.fullName!!,
            ArrayList(),
            this.requireContext(),
            this
        )
        binding.rcvUserList.adapter = adapter
    }

    /**
     * Function to call api for getting all group on server
     * */
    private fun getAllGroups() {
        viewModelGroup.getAllGroups(this.prefs).observe(viewLifecycleOwner, {
            try {
                when (it) {
                    is com.vdotok.network.network.Result.Success -> {
                        handleAllGroupsResponse(it.data)
                        binding.swipeRefreshLay.isRefreshing = false
                    }
                    is com.vdotok.network.network.Result.Failure -> {
                        if (isInternetAvailable(activity as Context).not())
                            binding.root.showSnackBar(getString(R.string.no_network_available))
                        else
                            binding.root.showSnackBar(it.exception.message)
                        binding.swipeRefreshLay.isRefreshing = false
                    }
                }
                binding.progressBar.toggleVisibility()


            } catch (e: HttpException) {
                Log.e(ApplicationConstants.API_ERROR, "AllUserList: ${e.printStackTrace()}")
            } catch (e: Throwable) {
                Log.e(ApplicationConstants.API_ERROR, "AllUserList: ${e.printStackTrace()}")
            }
        })

    }

    private fun handleAllGroupsResponse(response: AllGroupsResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value.toInt() -> {
                response.let { groupsResponse ->
                    if (groupsResponse.groups.isEmpty()) {
                        binding.groupChatListing.show()
                        binding.rcvUserList.hide()
                    } else {
                        groupList.clear()
                        groupsResponse.groups.forEach {
                            groupList.add(it)
                        }
                        binding.rcvUserList.show()
                        binding.groupChatListing.hide()
                        adapter.updateData(groupsResponse.groups)
                    }
                }
            }
            else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }

    /**
     * CallBacks for setOnClickListeners
     * */
    override fun onAudioCall(groupModel: GroupModel) {
        dialCall(groupModel, false)
    }

    override fun onVideoCall(groupModel: GroupModel) {
        dialCall(groupModel, true)
    }

    override fun onEditClick(groupModel: GroupModel) {
        activity?.supportFragmentManager.let { UpdateGroupNameDialog(groupModel, this::getAllGroups).show(
            it!!,
            UpdateGroupNameDialog.UPDATE_GROUP_TAG)
        }

    }

    override fun onResume() {
        BaseActivity.mListener = this
        if (callClient.isConnected() == true) {
            binding.tvLed.setImageResource(R.drawable.led_connected)
        } else {
            binding.tvLed.setImageResource(R.drawable.led_error)
        }
        super.onResume()
    }

    override fun onDeleteClick(position: Int) {
        dialogdeleteGroup(position)
    }
    /**
     * Function to display Alert dialog box
     * @param groupId groupId object we will be sending to the server to delete group on its basis
     * */
    private fun dialogdeleteGroup(groupId: Int) {
        showDeleteGroupAlert(this.activity, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val model = DeleteGroupModel()
                model.groupId = groupId
                deleteGroup(model)

            }
        })
    }
    /**
     * Function to call api for deleting a group from server
     * */
    private fun deleteGroup(model: DeleteGroupModel) {
        viewModelGroup.deleteGroup(this.prefs, model).observe(viewLifecycleOwner){
            try {
                when (it) {
                    is com.vdotok.network.network.Result.Success -> {
                        if (it.data.status == ApplicationConstants.SUCCESS_CODE) {
                            binding.root.showSnackBar(getString(R.string.group_deleted))
                            getAllGroups()
                        } else {
                            binding.root.showSnackBar(it.data.message)
                        }
                        binding.swipeRefreshLay.isRefreshing = false
                    }
                    is com.vdotok.network.network.Result.Failure -> {
                        if (isInternetAvailable(activity as Context).not())
                            binding.root.showSnackBar(getString(R.string.no_network_available))
                        else
                            binding.root.showSnackBar(it.exception.message)
                        binding.swipeRefreshLay.isRefreshing = false
                    }
                }
                binding.progressBar.toggleVisibility()

            } catch (e: HttpException) {
                Log.e(ApplicationConstants.API_ERROR, "AllUserList: ${e.printStackTrace()}")
            } catch (e: Throwable) {
                Log.e(ApplicationConstants.API_ERROR, "AllUserList: ${e.printStackTrace()}")
            }
        }
    }

    private fun dialCall(groupModel: GroupModel, isVideo: Boolean) {
        val refIdList = ArrayList<String>()
        groupModel.participants?.forEach { participant ->
            if (participant.refId != prefs.loginInfo?.refId)
                participant.refId?.let { refIdList.add(it) }
        }

        isVideoCall = isVideo

        if (callClient.isConnected() == true) {

            prefs.loginInfo?.let {

                (activity as DashBoardActivity).diaMany2ManyCall(
                    CallParams(
                        refId = it.refId!!,
                        toRefIds = refIdList,
                        mcToken = it.mcToken.toString(),
                        mediaType = if (isVideo) com.vdotok.streaming.enums.MediaType.VIDEO else com.vdotok.streaming.enums.MediaType.AUDIO,
                        callType = CallType.MANY_TO_MANY,
                        sessionType = SessionType.CALL,
                        isAppAudio = false,
                        customDataPacket = setCallTitleCustomObject(null, groupModel.groupTitle, "1")
                    )
                )
            }
            outGoingCall(groupModel)
        } else {
            (activity as DashBoardActivity).connectClient()
        }

    }

    override fun onStartCalling() {
    }

    override fun outGoingCall(toPeer: GroupModel) {
        activity?.let {
            it.runOnUiThread {
                openCallFragment(toPeer, isVideoCall)
            }
        }
    }

    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String) {
//        TODO("Not yet implemented")
    }

    override fun onCameraStreamReceived(stream: VideoTrack) {
//        TODO("Not yet implemented")
    }

    override fun onCameraAudioOff(audioState: Int, videoState: Int, refId: String) {}

    override fun onCallMissed() {
//        TODO("Not yet implemented")
    }

    override fun onCallRejected(reason: String) {
//        TODO("Not yet implemented")
    }

    override fun onConnectionSuccess() {
        binding.tvLed.setImageResource(R.drawable.led_connected)
    }

    override fun onConnectionFail() {
      binding.tvLed.setImageResource(R.drawable.led_error)
    }

    override fun onParticipantLeftCall(refId: String?) {
    }

    override fun onIncomingCall(model: CallParams) {
        activity?.runOnUiThread {
            val bundle = Bundle()
            bundle.putParcelableArrayList(DialCallFragment.GROUP_LIST, groupList)
            bundle.putString(DialCallFragment.USER_NAME, getUsername(model.refId))
            bundle.putBoolean(DialCallFragment.IS_IN_COMING_CALL, true)
            bundle.putBoolean(
                DialCallFragment.IS_VIDEO_CALL,
                model.mediaType == com.vdotok.streaming.enums.MediaType.VIDEO
            )
            startActivity(
                CallActivity.createIntent(
                    requireContext(),
                    null,
                    false,
                    false,
                    null,
                    model,
                    (requireActivity() as BaseActivity).sessionId,
                    groupList
                )
            )
        }
    }
    /**
     * Function to get UserName at incoming side
     * @param model model object is used to get username from the list of user achieved from server
     * */
    private fun getUsername(refId: String) : String? {
       groupList.let {
                it.forEach { name ->
                    name.participants?.forEach { username->
                        if (username.refId?.equals(refId) == true) {
                            user = username.fullname
                            return user
                        }
                    }
                }
            }
        return user
    }


    /**
     * Function to pass data at outgoing side call
     * @param toPeer toPeer object is the group data from server
     * @param isVideo isVideo object is to check if its an audio or video call
     * */
    private fun openCallFragment(toPeer: GroupModel, isVideo: Boolean) {
        startActivity(CallActivity.createIntent(requireContext(),
            toPeer, isVideo, false, null, null,
            (requireActivity() as BaseActivity).sessionId))
    }

    /**
     * Function for navigation between fragments
     * */
    private fun openUserListFragment() {
        Navigation.findNavController(binding.root).navigate(R.id.action_open_userList)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkNetworkStatPermission() {
        if (!checkNetPermissions())
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }


    fun setCallTitleCustomObject(calleName: String?, groupName: String?, autoCreated: String?): String {
        return Gson().toJson(CallNameModel(calleName,groupName,autoCreated), CallNameModel::class.java)
    }

    private fun checkNetPermissions(): Boolean {
        val appOps = activity?.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), activity?.packageName.toString()
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

}