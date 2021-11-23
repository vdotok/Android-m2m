package com.vdotok.many2many.feature.dashBoard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.network.di.module.RetrofitModule
import com.vdotok.network.network.Result
import com.vdotok.network.repository.UserListRepository


class UserListViewModel: ViewModel() {

    fun getAllUsers(prefs: Prefs) = liveData {
        val service = RetrofitModule.provideRetrofitService()
        val repo = UserListRepository(service)
        emit(Result.Loading)
        prefs.loginInfo?.authToken?.let {
            emit(repo.getAllUsers("Bearer ${prefs.loginInfo?.authToken}"))
        } ?: kotlin.run {
            emit(Result.Failure(Exception("Prefs hasn't auth token")))
        }
    }

}