package com.norgic.vdotokcall_mtm.network

import com.norgic.vdotokcall_mtm.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Created By: Norgic
 * Date & Time: On 5/5/21 At 12:57 PM in 2021
 */
interface ApiService {

    @POST("API/v0/Login")
    suspend fun loginUser(@Body model: LoginUserModel): Response<LoginResponse>

    @POST("API/v0/SignUp")
    suspend fun signUp(@Body model: SignUpModel): Response<LoginResponse>

    @POST("API/v0/CheckEmail")
    suspend fun checkEmail(@Body model: CheckUserModel): Response<LoginResponse>

    @POST("API/v0/AllUsers")
    suspend fun getAllUsers(@Header("Authorization") auth_token: String): Response<GetAllUsersResponseModel>

    @POST("API/v0/AllGroups")
    suspend fun getAllGroups(@Header("Authorization") auth_token: String): Response<AllGroupsResponse>

    @POST("API/v0/CreateGroup")
    suspend fun createGroup(@Header("Authorization") auth_token: String, @Body model: CreateGroupModel): Response<AllGroupsResponse>

    @POST("API/v0/DeleteGroup")
    suspend fun deleteGroup(@Header("Authorization") auth_token: String, @Body model: DeleteGroupModel): Response<DeleteGroupResponseModel>

    @POST("API/v0/RenameGroup")
    suspend fun updateGroupName(@Header("Authorization") auth_token: String, @Body model: UpdateGroupNameModel): Response<UpdateGroupNameResponseModel>

}