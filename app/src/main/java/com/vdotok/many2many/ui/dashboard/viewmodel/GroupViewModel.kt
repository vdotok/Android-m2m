package com.vdotok.many2many.feature.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.ApplicationConstants
import com.vdotok.network.di.module.RetrofitModule
import com.vdotok.network.models.*
import com.vdotok.network.network.Result
import com.vdotok.network.repository.AccountRepository
import com.vdotok.network.repository.GroupRepository
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


class GroupViewModel: ViewModel() {

    fun getAllGroups(prefs: Prefs) = liveData {

            val service = RetrofitModule.provideRetrofitService()
            val repo = GroupRepository(service)
            emit(Result.Loading)
            emit(repo.getAllGroups("Bearer ${prefs.loginInfo?.authToken}"))
    }


    fun createGroup(prefs: Prefs, model: CreateGroupModel) = liveData {

        val service = RetrofitModule.provideRetrofitService()
        val repo = GroupRepository(service)
        emit(Result.Loading)
        emit(repo.createGroup("Bearer ${prefs.loginInfo?.authToken}", model))
    }

    fun deleteGroup(prefs: Prefs, model: DeleteGroupModel) = liveData {
        val service = RetrofitModule.provideRetrofitService()
        val repo = GroupRepository(service)
        emit(Result.Loading)
        emit(repo.deleteGroup("Bearer ${prefs.loginInfo?.authToken}", model))
    }

    fun updateGroupName(prefs: Prefs, model: UpdateGroupNameModel) = liveData {
        val service = RetrofitModule.provideRetrofitService()
        val repo = GroupRepository(service)
        emit(Result.Loading)
        emit(repo.updateGroupName("Bearer ${prefs.loginInfo?.authToken}", model))
    }

}

//@POST("API/v0/AllGroups")
//suspend fun getAllGroups(@Header("Authorization") auth_token: String): Response<AllGroupsResponse>
//
//@POST("API/v0/CreateGroup")
//suspend fun createGroup(
//    @Header("Authorization") auth_token: String,
//    @Body model: CreateGroupModel
//): Response<AllGroupsResponse>
//
//@POST("API/v0/DeleteGroup")
//suspend fun deleteGroup(
//    @Header("Authorization") auth_token: String,
//    @Body model: DeleteGroupModel
//): Response<DeleteGroupResponseModel>
//
//@POST("API/v0/RenameGroup")
//suspend fun updateGroupName(
//    @Header("Authorization") auth_token: String,
//    @Body model: UpdateGroupNameModel
//): Response<UpdateGroupNameResponseModel>