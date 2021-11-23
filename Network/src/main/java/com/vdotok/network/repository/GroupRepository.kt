package com.vdotok.network.repository

import com.vdotok.network.models.*
import com.vdotok.network.network.*
import com.vdotok.network.network.api.ApiService
import com.vdotok.network.network.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject

class GroupRepository @Inject constructor(
        private val apiService: ApiService,
        private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getAllGroups(authToken: String): Result<AllGroupsResponse> {
        return safeApiCall(dispatcher) {
            apiService.getAllGroups(authToken)
        }
    }

    suspend fun createGroup(authToken: String, model: CreateGroupModel): Result<AllGroupsResponse> {
        return safeApiCall(dispatcher) {
            apiService.createGroup(authToken, model)
        }
    }

    suspend fun deleteGroup(authToken: String, model: DeleteGroupModel): Result<DeleteGroupResponseModel> {
        return safeApiCall(dispatcher) {
            apiService.deleteGroup(authToken, model)
        }
    }

    suspend fun updateGroupName(authToken: String, model: UpdateGroupNameModel): Result<UpdateGroupNameResponseModel> {
        return safeApiCall(dispatcher) {
            apiService.updateGroupName(authToken, model)
        }
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