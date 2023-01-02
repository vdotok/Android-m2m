package com.vdotok.many2many.ui.account.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.vdotok.many2many.R
import com.vdotok.many2many.databinding.LayoutFragmentLoginBinding
import com.vdotok.many2many.extensions.checkValidation
import com.vdotok.many2many.extensions.checkedPassword
import com.vdotok.many2many.extensions.showSnackBar
import com.vdotok.many2many.extensions.toggleVisibility
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.*
import com.vdotok.network.network.Result
import com.vdotok.many2many.feature.account.viewmodel.AccountViewModel


/**
 * Created By: VdoTok
 * Date & Time: On 5/3/21 At 1:26 PM in 2021
 *
 * This class displays the sign-in form to get in application
 */
class LoginFragment: Fragment() {

    private lateinit var binding: LayoutFragmentLoginBinding
    var email : ObservableField<String> = ObservableField<String>()
    var password : ObservableField<String> = ObservableField<String>()
    private lateinit var prefs: Prefs
    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = LayoutFragmentLoginBinding.inflate(inflater, container, false)

        binding.userEmail = email
        binding.password = password

        init()

        return binding.root
    }

    /**
     * Function to Set the click Listeners and to initialize the data for UI
     * */
    private fun init() {

        prefs = Prefs(activity)

        binding.btnSignIn.setOnClickListener {
            if (it.checkedPassword(password.get().toString()) && checkValidation(it, email.get().toString())) {
                loginUser(email.get().toString(), password.get().toString())
                binding.btnSignIn.disable()
            }
        }
        binding.btnSignUp.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_move_to_signup_user)
        }

    }

    /**
     * Function to call login User API
     * @param email email object we will be sending to the server
     * @param password password object we will be sending to the server
     * */
    private fun loginUser(email: String, password: String) {

        viewModel.loginUser(email, password).observe(viewLifecycleOwner) {
            when (it) {
                is Result.Loading -> {
                    binding.progressBar.toggleVisibility()
                }
                is Result.Success ->  {
                    binding.btnSignIn.enable()
                    binding.progressBar.toggleVisibility()
                    handleLoginResponse(requireContext(), it.data, prefs, binding.root)
                }
                is Result.Failure -> {
                    binding.btnSignIn.enable()
                    binding.progressBar.toggleVisibility()
//                    if (it.exception.responseCode == ApplicationConstants.HTTP_CODE_NO_NETWORK) {
//                        binding.root.showSnackBar(getString(R.string.no_network_available))
//                    else
                        binding.root.showSnackBar(it.exception.message)
                }
            }
        }

    }

}