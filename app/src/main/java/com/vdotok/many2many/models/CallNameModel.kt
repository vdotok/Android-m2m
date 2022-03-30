package com.vdotok.many2many.models

import android.os.Parcelable
import com.vdotok.streaming.enums.*
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CallNameModel(
    var calleName: String? = null,
    val groupName: String? = null,
    var groupAutoCreatedValue :String? = null
): Parcelable