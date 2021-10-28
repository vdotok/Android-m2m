package com.vdotok.many2many.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.vdotok.many2many.models.LoginResponse
import com.vdotok.many2many.utils.ApplicationConstants.FILE_NAME
import com.vdotok.many2many.utils.ApplicationConstants.LOGIN_INFO

/**
 * Created By: VdoTok
 * Date & Time: On 1/20/21 At 3:31 PM in 2021
 *
 * This class is mainly used to locally store and use data in the application
 * @param context the context of the application or the activity from where it is called
 */
class Prefs(context: Context?) {
    private val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var loginInfo: LoginResponse?
        get(){
            val gson = Gson()
            val json = mPrefs.getString(LOGIN_INFO, "")
            return gson.fromJson(json, LoginResponse::class.java)
        }
        set(loginObject) {
            val mEditor: SharedPreferences.Editor = mPrefs.edit()
            val gson = Gson()
            val json = gson.toJson(loginObject)
            mEditor.putString(LOGIN_INFO, json)
            mEditor.apply()
        }

    var fileInfo: String?
        get(){
            return mPrefs.getString(FILE_NAME, "")
        }
        set(fileInfo) {
            val mEditor: SharedPreferences.Editor = mPrefs.edit()
            mEditor.putString(FILE_NAME, fileInfo)
            mEditor.apply()
        }

    fun saveFileName(KEY_NAME: String, status: String) {

        val editor: SharedPreferences.Editor = mPrefs.edit()

        editor.putString(KEY_NAME, status)

        editor.commit()
    }
    fun getFilename(KEY_NAME: String): String? {

        return mPrefs.getString(KEY_NAME, null)
    }


    /**
     * Function to save a list of any type in prefs
     * */
    fun <T> setList(key: String?, list: List<T>?) {
        val gson = Gson()
        val json = gson.toJson(list)
        set(key, json)
    }

    /**
     * Function to save a simple key value pair in prefs
     * */
    operator fun set(key: String?, value: String?) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.putString(key, value)
        prefsEditor.apply()
    }

    /**
     * Function to get list of all groups saved in prefs
     * */
//    fun getGroupList(): List<GroupModel>? {
//            val gson = Gson()
//            val groupList: List<GroupModel>
//            val string: String = mPrefs.getString(GROUP_MODEL_KEY, null).toString()
//            val type: Type = object : TypeToken<List<GroupModel>>() {}.type
//            groupList = gson.fromJson(string, type)
//            return groupList
//    }

    /**
     * Function to save updated list of groups in prefs
     * */
//    fun saveUpdateGroupList(list: List<GroupModel>){
//        setList(GROUP_MODEL_KEY, list)
//    }

    /**
     * Function to clear all prefs from storage
     * */
    fun clearAll(){
        mPrefs.edit().clear().apply()
    }

    /**
     * Function to delete a specific prefs value from storage
     * */
    fun deleteKeyValuePair(key: String?) {
        mPrefs.edit().remove(key).apply()
    }
}