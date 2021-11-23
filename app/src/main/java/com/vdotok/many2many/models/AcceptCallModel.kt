package com.vdotok.many2many.models

import android.os.Parcelable
import com.vdotok.streaming.enums.*
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AcceptCallModel(

    var from: String = "",

    var sessionUUID: String = "",

    var requestID: String = "",

    var deviceType: CallType,

    var mediaType: MediaType,

    var sessionType: SessionType

): Parcelable