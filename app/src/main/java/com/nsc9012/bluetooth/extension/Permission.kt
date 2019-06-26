package com.nsc9012.bluetooth.extension

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat

fun Activity.hasPermission(permission: String) = ContextCompat.checkSelfPermission(
    this,
    permission
) == PackageManager.PERMISSION_GRANTED
