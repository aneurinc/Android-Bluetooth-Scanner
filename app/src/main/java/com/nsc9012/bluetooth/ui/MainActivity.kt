package com.nsc9012.bluetooth.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.nsc9012.bluetooth.R
import com.nsc9012.bluetooth.extension.hasPermission
import com.nsc9012.bluetooth.extension.logd

class MainActivity : AppCompatActivity() {

    companion object {
        const val ENABLE_BLUETOOTH = 1
        const val REQUEST_ENABLE_DISCOVERY = 2
        const val REQUEST_ACCESS_COARSE_LOCATION = 3
    }

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val deviceList = ArrayList<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBluetooth()
    }

    private fun initBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            initBluetoothUI()
        } else {
            // Bluetooth isn't enabled - prompt user to turn it on
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, ENABLE_BLUETOOTH)
        }
    }

    private fun initBluetoothUI() {
        enableDiscovery()
    }

    private fun enableDiscovery() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        startActivityForResult(intent, REQUEST_ENABLE_DISCOVERY)
    }

    private fun monitorDiscovery() {
        val discoveryMonitor = BluetoothDiscoveryMonitor()
        registerReceiver(discoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(discoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
    }

    private fun startDiscovery() {
        if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (bluetoothAdapter.isEnabled && !bluetoothAdapter.isDiscovering) {
                registerReceiver(BluetoothDiscoveryResult(), IntentFilter(BluetoothDevice.ACTION_FOUND))
                beginDiscovery()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_ACCESS_COARSE_LOCATION
            )
        }
    }

    private fun beginDiscovery() {
        logd("Starting discovery")
        deviceList.clear()
        monitorDiscovery()
        bluetoothAdapter.startDiscovery()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ACCESS_COARSE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    registerReceiver(BluetoothDiscoveryResult(), IntentFilter(BluetoothDevice.ACTION_FOUND))
                    beginDiscovery()
                } else {
                    // User denied permission...
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ENABLE_BLUETOOTH -> if (resultCode == Activity.RESULT_OK) {
                initBluetoothUI()
            }
            REQUEST_ENABLE_DISCOVERY -> if (resultCode == Activity.RESULT_CANCELED) {
                logd("Discovery cancelled by user.")
            } else {
                startDiscovery()
            }
        }
    }

    /* Broadcast receiver to listen for discovery results. */
    private inner class BluetoothDiscoveryResult : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val remoteDeviceName = intent?.getStringExtra(BluetoothDevice.EXTRA_NAME)
            val remoteDevice: BluetoothDevice = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
            deviceList.add(remoteDevice)
            logd("Discovered $remoteDeviceName")
        }
    }

    /* Broadcast receiver to listen for discovery updates. */
    private inner class BluetoothDiscoveryMonitor : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == intent?.action) {
                // Discovery has started
                logd("Discovery started...")
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == intent?.action) {
                // Discovery is complete
                logd("Discovery complete.")
            }
        }
    }
}
