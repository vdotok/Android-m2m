package com.vdotok.many2many.ui.account.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.vdotok.many2many.R
import com.vdotok.many2many.databinding.LayoutFragmentSignupBinding
import com.vdotok.many2many.extensions.*
import com.vdotok.many2many.models.QRCodeModel
import com.vdotok.many2many.network.HttpResponseCodes
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.ui.account.viewmodel.AccountViewModel
import com.vdotok.many2many.utils.*
import com.vdotok.many2many.utils.ApplicationConstants.SDK_PROJECT_ID
import com.vdotok.network.models.LoginResponse
import com.vdotok.network.models.SignUpModel
import com.vdotok.network.network.Result
import com.vdotok.network.utils.Constants


/**
 * Created By: VdoTok
 * Date & Time: On 5/3/21 At 1:26 PM in 2021
 *
 * This class display the sign-up form
 */
class SignUpFragment : Fragment() {

    private lateinit var binding: LayoutFragmentSignupBinding
    var email: ObservableField<String> = ObservableField<String>()
    var fullName: ObservableField<String> = ObservableField<String>()
    var password: ObservableField<String> = ObservableField<String>()
    private lateinit var prefs: Prefs
    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = LayoutFragmentSignupBinding.inflate(inflater, container, false)

        binding.userEmail = email
        binding.fullName = fullName
        binding.password = password

        init()

        return binding.root
    }

    /**
     * Function for setOnClickListeners and to check validation
     * */
    private fun init() {
        prefs = Prefs(activity)

        binding.btnSignUp.setOnClickListener {
            if (it.checkedUserName(fullName.get().toString()) &&
                it.checkedEmail(email.get().toString()) &&
                it.checkedPassword(password.get().toString())
            ) {
                binding.btnSignUp.disable()
                checkUserEmail(email.get().toString())
            }
        }

        binding.btnSignIn.setOnClickListener {
            moveToLogin(it)
        }

        binding.scanner.performSingleClick {
            activity?.runOnUiThread {
                qrCodeScannerLauncher.launch(IntentIntegrator.forSupportFragment(this))
            }
        }

        configureBackPress()
    }

    /**
     * Function to call checkEmail api to verify the email (that same email is not in  use by any other user )
     * @param email email object we will be sending to the server
     * */
    private fun checkUserEmail(email: String) {
        activity?.let {
            if (!prefs.userProjectId.isNullOrEmpty() && !prefs.userBaseUrl.isNullOrEmpty()) {
                viewModel.checkEmailAlreadyExist(email).observe(viewLifecycleOwner) {

                    when (it) {
                        Result.Loading -> {
                            binding.progressBar.toggleVisibility()
                        }
                        is Result.Success -> {
                            binding.progressBar.toggleVisibility()
                            handleCheckFullNameResponse(it.data)
                            binding.btnSignUp.enable()
                        }
                        is Result.Failure -> {
                            binding.progressBar.toggleVisibility()
                            if (isInternetAvailable(this@SignUpFragment.requireContext()).not())
                                binding.root.showSnackBar(getString(R.string.no_network_available))
                            else
                                binding.root.showSnackBar(it.exception.message)
                            binding.btnSignUp.enable()
                        }
                    }

                }
            } else {
                binding.root.showSnackBar(getString(R.string.api_url_empty))
                binding.btnSignUp.enable()
            }
        }
    }

    private fun handleCheckFullNameResponse(response: LoginResponse) {
        when (response.status) {
            HttpResponseCodes.SUCCESS.value -> {
                signUp()
            }
            else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }

    /**
     * Function to call signup Api to register user
     * */

    private fun signUp() {
        binding.btnSignUp.disable()
        if (!prefs.userProjectId.isNullOrEmpty() && !prefs.userBaseUrl.isNullOrEmpty()) {
            viewModel.signUp(
                SignUpModel(
                    fullName.get().toString(), email.get().toString(),
                    password.get().toString(), project_id = prefs.userProjectId.toString()
                )
            ).observe(viewLifecycleOwner) {
                when (it) {
                    Result.Loading -> {
                        binding.progressBar.toggleVisibility()
                    }
                    is Result.Success -> {
                        binding.progressBar.toggleVisibility()
                        handleLoginResponse(requireContext(), it.data, prefs, binding.root)
                    }
                    is Result.Failure -> {
                        binding.progressBar.toggleVisibility()
                        if (isInternetAvailable(this@SignUpFragment.requireContext()).not())
                            binding.root.showSnackBar(getString(R.string.no_network_available))
                        else
                            binding.root.showSnackBar(it.exception.message)
                    }
                }
                binding.btnSignUp.enable()
            }
        } else {
            binding.root.showSnackBar(getString(R.string.api_url_empty))
            binding.btnSignUp.enable()
        }
    }

    private val qrCodeScannerLauncher = registerForActivityResult(QrCodeScannerContract()) {
        if (!it.contents.isNullOrEmpty()) {
            Log.d("RESULT_INTENT", it.contents)
            val data: QRCodeModel? = Gson().fromJson(it.contents, QRCodeModel::class.java)
            prefs.userProjectId = data?.project_id.toString()
            prefs.userBaseUrl = data?.tenant_api_url.toString()
            if (!prefs.userProjectId.isNullOrEmpty() && !prefs.userBaseUrl.isNullOrEmpty()) {
                SDK_PROJECT_ID = prefs.userProjectId.toString()
                Constants.BASE_URL = prefs.userBaseUrl.toString()
            }
            Log.d("RESULT_INTENT", data.toString())
        } else {
            binding.root.showSnackBar("QR CODE is not correct!!!")
        }
    }


    private fun moveToLogin(view: View) {
        Navigation.findNavController(view).navigateUp()
    }

    private fun configureBackPress() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveToLogin(binding.root)
                }
            })
    }
}