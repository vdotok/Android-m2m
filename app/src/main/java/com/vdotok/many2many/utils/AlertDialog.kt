package com.vdotok.many2many.utils

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.vdotok.many2many.R


fun showDeleteGroupAlert(activity: FragmentActivity?, dialogListener: DialogInterface.OnClickListener) {
    activity?.let {
        val alertDialog = AlertDialog.Builder(it)
            .setMessage(activity.getString(R.string.delete_group))
            .setPositiveButton(R.string.delete, dialogListener)
            .setNegativeButton(R.string.cancel, null).create()
        alertDialog.show()
    }
}
