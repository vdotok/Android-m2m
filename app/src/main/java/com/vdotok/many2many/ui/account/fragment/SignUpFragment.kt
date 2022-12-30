package com.vdotok.many2many.ui.account.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.vdotok.many2many.R
import com.vdotok.many2many.databinding.LayoutFragmentSignupBinding
import com.vdotok.many2many.extensions.*
import com.vdotok.many2many.feature.account.viewmodel.AccountViewModel
import com.vdotok.many2many.network.HttpResponseCodes
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.ApplicationConstants.SDK_PROJECT_ID
import com.vdotok.many2many.utils.disable
import com.vdotok.many2many.utils.enable
import com.vdotok.many2many.utils.handleLoginResponse
import com.vdotok.many2many.utils.isInternetAvailable
import com.vdotok.network.models.LoginResponse
import com.vdotok.network.models.SignUpModel
import com.vdotok.network.network.Result


/**
 * Created By: VdoTok
 * Date & Time: On 5/3/21 At 1:26 PM in 2021
 *
 * This class display the sign-up form
 */
class SignUpFragment: Fragment() {

    private lateinit var binding: LayoutFragmentSignupBinding
    var email : ObservableField<String> = ObservableField<String>()
    var fullName : ObservableField<String> = ObservableField<String>()
    var password : ObservableField<String> = ObservableField<String>()
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
                it.checkedPassword(password.get().toString()) &&
                it.checkedEmail(email.get().toString())) {
                checkUserEmail(email.get().toString())
                binding.btnSignUp.disable()
            }
        }

       binding.btnSignIn.setOnClickListener {
           moveToLogin(it)
        }

        configureBackPress()
    }

    /**
     * Function to call checkEmail api to verify the email (that same email is not in  use by any other user )
     * @param email email object we will be sending to the server
     * */
    private fun checkUserEmail(email: String) {

        viewModel.checkEmailAlreadyExist(email).observe(viewLifecycleOwner) {
            when (it) {
                is Result.Loading -> {
                    binding.progressBar.toggleVisibility()
                }
                is Result.Success ->  {
                    binding.progressBar.toggleVisibility()
                    handleCheckFullNameResponse(it.data)
                    binding.btnSignUp.enable()
                }
                is Result.Failure -> {
                    binding.btnSignUp.enable()
                    binding.progressBar.toggleVisibility()
                    if (isInternetAvailable(this@SignUpFragment.requireContext()).not())
                        binding.root.showSnackBar(getString(R.string.no_network_available))
                    else
                        binding.root.showSnackBar(it.exception.message)
                }
            }

        }
    }

    private fun handleCheckFullNameResponse(response: LoginResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value -> {
                signUp()
            } else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }

    /**
     * Function to call signup Api to register user
     * */

    private fun signUp() {
        binding.btnSignUp.disable()
        viewModel.signUp(SignUpModel(fullName.get().toString(), email.get().toString(),
            password.get().toString(),project_id = SDK_PROJECT_ID)).observe(viewLifecycleOwner) {

            when (it) {
                Result.Loading -> {
                    binding.progressBar.toggleVisibility()
                }
                is Result.Success ->  {
                    binding.progressBar.toggleVisibility()
                    handleLoginResponse(requireContext(), it.data, prefs, binding.root)
                    binding.btnSignUp.enable()
                }
                is Result.Failure -> {
                    binding.btnSignUp.enable()
                    binding.progressBar.toggleVisibility()
                    if (isInternetAvailable(this@SignUpFragment.requireContext()).not())
                        binding.root.showSnackBar(getString(R.string.no_network_available))
                    else
                        binding.root.showSnackBar(it.exception.message)
                }
            }
        }

    }

    private fun moveToLogin(view: View) {
        Navigation.findNavController(view).navigateUp()
    }

    private fun configureBackPress() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveToLogin(binding.root)
                }
            })
    }
}