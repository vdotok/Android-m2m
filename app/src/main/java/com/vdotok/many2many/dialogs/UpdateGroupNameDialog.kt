package com.vdotok.many2many.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.databinding.ObservableField
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.vdotok.many2many.R
import com.vdotok.many2many.databinding.UpdateGroupNameBinding
import com.vdotok.many2many.extensions.showSnackBar
import com.vdotok.many2many.extensions.toggleVisibility
import com.vdotok.many2many.feature.account.viewmodel.GroupViewModel
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.many2many.utils.isInternetAvailable
import com.vdotok.network.models.GroupModel
import com.vdotok.network.models.UpdateGroupNameModel
import retrofit2.HttpException

class UpdateGroupNameDialog(
    private val groupModel: GroupModel,
    private val callbacks: UpdateGroupCallbacks
) : DialogFragment() {

    private lateinit var binding: UpdateGroupNameBinding
    private lateinit var prefs: Prefs
    var edtGroupName = ObservableField<String>()
    private val viewModelGroup: GroupViewModel by viewModels()


    init {
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        prefs = Prefs(activity)

        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }

        binding = UpdateGroupNameBinding.inflate(inflater, container, false)
        binding.groupName = edtGroupName
        edtGroupName.set(groupModel.groupTitle)

        binding.imgClose.setOnClickListener {
            dismiss()
        }

        binding.btnDone.setOnClickListener {
            if (edtGroupName.get()?.isNotEmpty() == true) {
                val model = UpdateGroupNameModel()
                model.groupId = groupModel.id
                model.groupTitle = edtGroupName.get()
                editGroup(model)
            } else {
                binding.root.showSnackBar(R.string.group_name_empty)
            }
        }

        return binding.root
    }

    private fun editGroup(model: UpdateGroupNameModel) {
        viewModelGroup.updateGroupName(this.prefs, model).observe(viewLifecycleOwner) {
            try {
                when (it) {
                    is com.vdotok.network.network.Result.Success -> {
                        callbacks.groupRenameSuccess(it.data.groupModel)
                        binding.root.showSnackBar(getString(R.string.updated_group))
                        dismiss()
                    }
                    is com.vdotok.network.network.Result.Failure -> {
                        if (isInternetAvailable(activity as Context).not())
                            binding.root.showSnackBar(getString(R.string.no_network_available))
                        else
                            binding.root.showSnackBar(it.exception.message)
                    }
                    else -> {}
                }
                binding.progressBar.toggleVisibility()

            } catch (e: HttpException) {
                Log.e(ApplicationConstants.API_ERROR, "AllUserList: ${e.printStackTrace()}")
            } catch (e: Throwable) {
                Log.e(ApplicationConstants.API_ERROR, "AllUserList: ${e.printStackTrace()}")
            }
        }

    }

    interface UpdateGroupCallbacks {
        fun groupRenameSuccess(groupModel: GroupModel)
    }

    companion object {
        const val UPDATE_GROUP_TAG = "UPDATE_GROUP_DIALOG"
    }

}
