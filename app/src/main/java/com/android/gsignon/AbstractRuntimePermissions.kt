package com.android.gsignon

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

/**
 * Created by Gowtham on 04-12-2017.
 */

abstract class AbstractRuntimePermissions : Activity() {

    abstract var mErrorString: HashMap<Int, Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mErrorString = HashMap()
    }

    abstract fun onPermissionsGranted(requestCode: Int)

    fun requestAppPermissions(requestedPermissions: kotlin.Array<String>, stringId: Int, requestCode: Int) {

        mErrorString[requestCode] = stringId

        var permissionCheck = PackageManager.PERMISSION_GRANTED
        var showRequestPermissions = false

        for (permission in requestedPermissions) {
            permissionCheck += ContextCompat.checkSelfPermission(this, permission)
            showRequestPermissions =
                showRequestPermissions || ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (showRequestPermissions) {
                /*Snackbar.make(findViewById(android.R.id.content), stringId, Snackbar.LENGTH_INDEFINITE)
                        .setAction("GRANT") { ActivityCompat.requestPermissions(this@AbstractRuntimePermissions, requestedPermissions, requestCode) }.show()*/
                ActivityCompat.requestPermissions(this@AbstractRuntimePermissions, requestedPermissions, requestCode)
            } else {
                ActivityCompat.requestPermissions(this, requestedPermissions, requestCode)
            }
        } else {
            onPermissionsGranted(requestCode)
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var permissionCheck = PackageManager.PERMISSION_GRANTED
        if (grantResults != null) {
            for (permission in grantResults) {
                permissionCheck += permission
            }
        }

        if (grantResults != null) {
            if (grantResults.isNotEmpty() && PackageManager.PERMISSION_GRANTED == permissionCheck) {
                onPermissionsGranted(requestCode)
            } else {
                mErrorString[requestCode]?.let {
                    /*Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_INDEFINITE)
                            .setAction("ENABLE"){
                                var intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.data = Uri.parse("package:$packageName")
                                intent.addCategory(Intent.CATEGORY_DEFAULT)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                startActivity(intent)
                            }.show()*/
                    var intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.parse("package:$packageName")
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(intent)
                }
            }
        }

    }

}