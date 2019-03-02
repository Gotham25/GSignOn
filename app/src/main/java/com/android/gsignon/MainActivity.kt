package com.android.gsignon

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle

class MainActivity : AbstractRuntimePermissions() {

    override var mErrorString: HashMap<Int, Int> = HashMap()
    private val requestPermission: Int = 10

    override fun onPermissionsGranted(requestCode: Int) {
        //Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_LONG).show()
        startActivity(Intent(this@MainActivity, SignOnActivity::class.java))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val requestedPermissions = arrayOf(Manifest.permission.INTERNET)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Build version greater than or equal to Marshmallow. Ask runtime permissions
            requestAppPermissions(requestedPermissions, R.string.permission_error, requestPermission)
        } else {
            //Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_LONG).show()
            startActivity(Intent(this@MainActivity, SignOnActivity::class.java))
        }
    }

}
