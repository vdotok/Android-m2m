package com.norgic.vdotokcall_mtm.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.ActivityAccountsBinding
import com.norgic.vdotokcall_mtm.utils.ViewUtils.setStatusBarGradient


/**
 * Created By: Norgic
 * Date & Time: On 5/19/21 At 6:29 PM in 2021
 */
class AccountsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarGradient(this, R.drawable.ic_account_gradient_bg)
        init()

    }

    private fun init() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_accounts)
    }

    companion object{
        fun createAccountsActivity(context: Context) =
            Intent(context, AccountsActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            }
    }
}