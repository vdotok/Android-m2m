package com.norgic.vdotokcall_mtm.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.ActivitySplashBinding
import com.norgic.vdotokcall_mtm.extensions.showSnackBar
import com.norgic.vdotokcall_mtm.extensions.toggleVisibility
import com.norgic.vdotokcall_mtm.models.AuthenticationRequest
import com.norgic.vdotokcall_mtm.models.AuthenticationResponse
import com.norgic.vdotokcall_mtm.network.HttpResponseCodes
import com.norgic.vdotokcall_mtm.network.Result
import com.norgic.vdotokcall_mtm.network.RetrofitBuilder
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.ui.account.AccountsActivity.Companion.createAccountsActivity
import com.norgic.vdotokcall_mtm.ui.dashboard.DashBoardActivity.Companion.createDashboardActivity
import com.norgic.vdotokcall_mtm.utils.ApplicationConstants.API_ERROR
import com.norgic.vdotokcall_mtm.utils.ApplicationConstants.HTTP_CODE_NO_NETWORK
import com.norgic.vdotokcall_mtm.utils.ApplicationConstants.SDK_AUTH_TOKEN
import com.norgic.vdotokcall_mtm.utils.ApplicationConstants.SDK_PROJECT_ID
import com.norgic.vdotokcall_mtm.utils.ViewUtils.setStatusBarGradient
import com.norgic.vdotokcall_mtm.utils.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarGradient(this, R.drawable.ic_account_gradient_bg)
        init()

    }

    private fun init() {
        prefs = Prefs(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        initSdkAuth()
    }

    private fun initSdkAuth() {
        binding.progressBar.toggleVisibility()
        val service = RetrofitBuilder.makeSdkAuthRetrofitService(this)
        CoroutineScope(Dispatchers.IO).launch {
            val response = safeApiCall { service.authSDK (
                AuthenticationRequest(
                    SDK_AUTH_TOKEN,
                    SDK_PROJECT_ID)
            ) }

            withContext(Dispatchers.Main) {
                try {
                    when (response) {
                        is Result.Success -> {
                            handleSdkAuthResponse(response = response.data)
                        }
                        is Result.Error -> {
                            if (response.error.responseCode == HTTP_CODE_NO_NETWORK) {
                                binding.root.showSnackBar(getString(R.string.no_network_available))
                                Handler(Looper.getMainLooper()).postDelayed({
                                    finish()
                                }, 2000)

                            } else {
                                binding.root.showSnackBar(response.error.message)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    finish()
                                }, 2000)
                            }
                        }
                    }
                } catch (e: HttpException) {
                    Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
                } catch (e: Throwable) {
                    Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
                }
                binding.progressBar.toggleVisibility()
            }
        }
    }

    private fun handleSdkAuthResponse(response: AuthenticationResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.valueInInt -> {
                prefs.sdkAuthResponse = response
                performAuthOperations()
            }
            else -> {
                binding.root.showSnackBar(response.message)
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 2000)
            }
        }
    }

    private fun performAuthOperations() {
        prefs.loginInfo?.let {
            startActivity(createDashboardActivity(this))
            finish()
        }?: kotlin.run {
            moveToAccountsActivity()
        }
    }

    private fun moveToAccountsActivity() {
        startActivity(createAccountsActivity(this))
        finish()
    }
}