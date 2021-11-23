package com.vdotok.many2many.ui.dashboard.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.vdotok.many2many.R
import com.vdotok.many2many.adapter.AllUserListAdapter
import com.vdotok.many2many.adapter.OnInboxItemClickCallbackListener
import com.vdotok.many2many.databinding.FragmentUserListBinding
import com.vdotok.many2many.dialogs.CreateGroupDialog
import com.vdotok.many2many.extensions.*
import com.vdotok.many2many.feature.account.viewmodel.GroupViewModel
import com.vdotok.many2many.feature.dashBoard.viewmodel.UserListViewModel
import com.vdotok.many2many.network.HttpResponseCodes
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.ApplicationConstants.API_ERROR
import com.vdotok.many2many.utils.ViewUtils.setStatusBarGradient
import com.vdotok.many2many.utils.isInternetAvailable
import com.vdotok.network.models.AllGroupsResponse
import com.vdotok.network.models.CreateGroupModel
import com.vdotok.network.models.GetAllUsersResponseModel
import com.vdotok.network.models.UserModel
import com.vdotok.network.network.Result
import retrofit2.HttpException

/**
 * Created By: VdoTok
 * Date & Time: On 6/17/21 At 1:29 PM in 2021
 *
 * This class displays the list of users that are  connected to
 */
class UserListFragment : Fragment(), OnInboxItemClickCallbackListener {

    private lateinit var binding: FragmentUserListBinding
    private lateinit var prefs: Prefs
    lateinit var adapter: AllUserListAdapter
    var edtSearch = ObservableField<String>()
    private val listViewModel : UserListViewModel by viewModels()
    private val viewModelGroup : GroupViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =  FragmentUserListBinding.inflate(inflater, container, false)

        setStatusBarGradient(this.requireActivity(), R.drawable.rectangle_white_bg)
        init()
        return binding.root
    }
    /**
     * Function for setOnClickListeners
     * */
    private fun init() {
        prefs = Prefs(context)

        initUserListAdapter()

        binding.search = edtSearch
        edtSearch.set("")
        binding.customToolbar.imgDone.setImageResource(R.drawable.ic_done)
        binding.customToolbar.tvTitle.text = getString(R.string.createGroupText)

        binding.customToolbar.imgDone.setOnClickListener {
            activity?.hideKeyboard()
            if (adapter.getSelectedUsers().isNotEmpty()) {
                onCreateGroupClick()
            } else {
                binding.root.showSnackBar(R.string.no_user_select)
            }

        }

        binding.customToolbar.imgArrowBack.setOnClickListener {
            activity?.hideKeyboard()
            activity?.onBackPressed()
        }

        textListenerForSearch()
        getAllUsers()
        addPullToRefresh()
    }
    /**
     * Function for refreshing the updated users list
     * */
    private fun addPullToRefresh() {
        binding.swipeRefreshLay.setOnRefreshListener {
            getAllUsers()
        }
    }

    private fun initUserListAdapter() {
        adapter = AllUserListAdapter(ArrayList(),this)
        binding.rcvUserList.adapter = adapter
    }

    private fun onCreateGroupClick() {
        val selectedUsersList: List<UserModel> = adapter.getSelectedUsers()

        if (selectedUsersList.isNotEmpty() && selectedUsersList.size == 1)
            createGroup(getGroupTitle(selectedUsersList))
        else
            activity?.supportFragmentManager?.let { CreateGroupDialog(this::createGroup).show(it, CreateGroupDialog.TAG) }
    }


    /**
     * Function to show creating group fragment
     * @param title title is the group title that is pass to create group
     * */
    private fun createGroup(title: String) {
        val selectedUsersList: List<UserModel> = adapter.getSelectedUsers()

        if(selectedUsersList.isNotEmpty()){

            val model = CreateGroupModel()
            model.groupTitle = title
            //model.auto_created -> set auto created group, set 1 for only single user, 0 for multiple users
            model.pariticpants = getParticipantsIds(selectedUsersList)

            when (selectedUsersList.size) {
                1 -> model.autoCreated = 1
                else -> model.autoCreated = 0
            }

            createGroupApiCall(model)
        }
    }

    /**
     * Function to create title of the group
     * @param selectedUsersList selectedUserList is the list of user pass to create group title
     * */
    private fun getGroupTitle(selectedUsersList: List<UserModel>): String {

        var title = prefs.loginInfo?.fullName.plus("-")

        //In this case, we have only one item in list
        selectedUsersList.forEach {
            title = title.plus(it.userName.toString())
        }
        return title
    }
    /**
     * Function to call api for creating a group on server
     * */
    private fun createGroupApiCall(model: CreateGroupModel) {

        viewModelGroup.createGroup(this.prefs, model).observe(viewLifecycleOwner, {
            try {
                when (it) {
                    is Result.Loading -> {
                        binding.progressBar.toggleVisibility()
                    }
                    is Result.Success -> {
                        binding.progressBar.toggleVisibility()
                        Snackbar.make(binding.root, R.string.group_created, Snackbar.LENGTH_LONG).show()
                        handleCreateGroupSuccess(it.data)
                    }
                    is Result.Failure -> {
                        binding.progressBar.toggleVisibility()
                        if (isInternetAvailable(activity as Context).not())
                            binding.root.showSnackBar(getString(R.string.no_network_available))
                        else
                            binding.root.showSnackBar(it.exception.message)
                    }
                }

            } catch (e: HttpException) {
                Log.e(API_ERROR, "AllUserList: ${e.printStackTrace()}")
            } catch (e: Throwable) {
                Log.e(API_ERROR, "AllUserList: ${e.printStackTrace()}")
            }
        })

//        activity?.let {
//            binding.progressBar.toggleVisibility()
//            val apiService: ApiService =
//                RetrofitBuilder.makeRetrofitService(it)
//            prefs.loginInfo?.authToken.let {
//                CoroutineScope(Dispatchers.IO).launch {
//                    val response = safeApiCall { apiService.createGroup(auth_token = "Bearer $it", model) }
//                    withContext(Dispatchers.Main) {
//                        try {
//                            when (response) {
//                                is Result.Success -> {
//                                    Snackbar.make(
//                                        binding.root,
//                                        R.string.group_created,
//                                        Snackbar.LENGTH_LONG
//                                    ).show()
//                                    handleCreateGroupSuccess(response.data)
//                                }
//                                is Result.Error -> {
//                                    if (response.error.responseCode == ApplicationConstants.HTTP_CODE_NO_NETWORK) {
//                                        binding.root.showSnackBar(getString(R.string.no_network_available))
//                                    } else {
//                                        binding.root.showSnackBar(response.error.message)
//                                    }
//                                }
//                            }
//                        } catch (e: HttpException) {
//                            Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
//                        } catch (e: Throwable) {
//                            Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
//                        }
//                        binding.progressBar.toggleVisibility()
//                    }
//                }
//            }
//        }
    }


    private fun handleCreateGroupSuccess(response: AllGroupsResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value.toInt() -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    activity?.hideKeyboard()
                    openGroupFragment()
                },1000)
            } else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }

    /**
     * Function for setting participants ids
     * @param selectedUsersList list of selected users to form a group with
     * @return Returns an ArrayList<Int> of selected user ids
     * */
    private fun getParticipantsIds(selectedUsersList: List<UserModel>): ArrayList<Int> {
        val list: ArrayList<Int> = ArrayList()
        selectedUsersList.forEach { userModel ->
            userModel.userId?.let { list.add(it.toInt()) }
        }
        return list
    }
    /**
     * Function to filter the search
     * */
    private fun textListenerForSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                adapter.filter?.filter(s)
            }
        })
    }
    /**
     * Function to call api for getting all user on server
     * */
    private fun getAllUsers() {

        listViewModel.getAllUsers(this.prefs).observe(viewLifecycleOwner, {
            try {
                when (it) {
                    is com.vdotok.network.network.Result.Loading -> {

                        binding.swipeRefreshLay.isRefreshing = false
                        binding.progressBar.toggleVisibility()

                    }
                    is com.vdotok.network.network.Result.Success -> {
                        binding.progressBar.toggleVisibility()
                        handleAllUsersResponse(it.data)

                    }
                    is com.vdotok.network.network.Result.Failure -> {
                        binding.swipeRefreshLay.isRefreshing = false
                        binding.progressBar.toggleVisibility()
                        Log.e(API_ERROR, it.exception.message ?: "")
                        if (isInternetAvailable(activity as Context).not())
                            binding.root.showSnackBar(getString(R.string.no_network_available))
                        else
                            binding.root.showSnackBar(it.exception.message)
                    }
                }

            } catch (e: HttpException) {
                Log.e(API_ERROR, "AllUserList: ${e.printStackTrace()}")
            } catch (e: Throwable) {
                Log.e(API_ERROR, "AllUserList: ${e.printStackTrace()}")
            }
        })


    }

    private fun handleAllUsersResponse(response: GetAllUsersResponseModel) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value -> {
                response.users.let { usersList ->
                    if (usersList.isEmpty()) {
                        binding.root.showSnackBar(getString(R.string.no_contacts))
                    } else {
                        populateDataToList(response)
                    }
                }
            }
            else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }
    /**
     * Function to display all users
     * */
    private fun populateDataToList(response: GetAllUsersResponseModel) {
        adapter.updateData(response.users)
    }

    /**
     * Callback for click listeners
     * */
    override fun onItemClick(position: Int) {
        val item = adapter.dataList[position]
        item.isSelected = item.isSelected.not()
        adapter.notifyItemChanged(position)
    }

    override fun searchResult(position: Int) {
        edtSearch.get()?.isNotEmpty()?.let {
            if (position == 0 && it){
                binding.check.show()
                binding.rcvUserList.hide()
            }else{
                binding.check.hide()
                binding.rcvUserList.show()
            }
        }
    }
    /**
     * Function for the navigation to other fragment
     * */
    private fun openGroupFragment() {
        Navigation.findNavController(binding.root).navigate(R.id.action_open_groupList)
    }


}