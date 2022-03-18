package com.vdotok.many2many.utils


/**
 * Created By: VdoTok
 * Date & Time: On 5/5/21 At 5:06 PM in 2021
 */
object ApplicationConstants {
//    SDK AUTH PARAMS
    const val SDK_PROJECT_ID = "Add Your project id here"

//    PREFS CONSTANTS
    const val LOGIN_INFO = "savedLoginInfo"

//    API ERROR LOG TAGS
    const val API_ERROR = "API_ERROR"
    const val HTTP_CODE_NO_NETWORK = 600
    const val SUCCESS_CODE = 200

//    GROUP CONSTANTS
    const val MAX_PARTICIPANTS = 4 // max limit is 4 so including current user we can add up to 3 more users in a group

    //    DIRECTORY PATHS
    const val FILE_NAME = "FILE_NAME"
    const val DOCS_DIRECTORY = "/VdoTok-MTM/docs"
    const val MAIN_DOCS_DIRECTORY_NAME = "VdoTok-MTM"
    const val CALL_LOGS_FILE_NAME = "VdoTok_Call_Logs"
    const val CALL_PARAMS = "call_params"

    // This error code means a local error occurred while parsing the received json.

    const val MY_PERMISSIONS_REQUEST_CAMERA = 100
    const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101
    const val MY_PERMISSIONS_REQUEST = 102

}