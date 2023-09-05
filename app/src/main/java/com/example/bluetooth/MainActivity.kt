package com.example.bluetooth

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetooth.databinding.ActivityMainBinding
import dalvik.system.PathClassLoader
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bluetoothManager: BluetoothManager
    lateinit var receiver: BluetoothReceiver
    lateinit var receiver2: DiscoverabilityReceiver
    var deviceCount = 0
    var permission: Boolean = false
    val REQUEST_ACCESS_COARSE_LOCATION = 101

    private lateinit var deviceAdapter: DeviceAdapter
    private val devicesList = ArrayList<String>()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        receiver = BluetoothReceiver()
        receiver2 = DiscoverabilityReceiver()
        enableDisableBluetooth()
        binding.btDiscover.setOnClickListener {
            discoverability()
        }
        binding.btGetPairedDevices.setOnClickListener {
            getPairedDevices()
        }
        binding.btDiscoverDevices.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when (ContextCompat.checkSelfPermission(
                    baseContext, Manifest.permission.ACCESS_COARSE_LOCATION
                )) {
                    PackageManager.PERMISSION_DENIED -> androidx.appcompat.app.AlertDialog.Builder(
                        this
                    )
                        .setTitle("Runtime Permission")
                        .setMessage("Give Permission")
                        .setNeutralButton("Okay", DialogInterface.OnClickListener { dialog, which ->
                            if (ContextCompat.checkSelfPermission(
                                    baseContext,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) !=
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                                    REQUEST_ACCESS_COARSE_LOCATION
                                )
                            }
                        })
                        .show()
                        .findViewById<TextView>(R.id.message)!!.movementMethod =
                        LinkMovementMethod.getInstance()

                    PackageManager.PERMISSION_GRANTED -> {
                        Log.d("discoverDevices", "Permission Granted")
                    }
                }
            }
            discoverDevices()
        }

        val recyclerView: RecyclerView = binding.deviceRecycler
        deviceAdapter = DeviceAdapter(devicesList)
        val layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = deviceAdapter

        val listener = object : OnDeviceClickListener {
            override fun onDeviceClick(address: String) {
               pairWithDevice(address)
            }
        }

        deviceAdapter.attachListener(listener)
    }

    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(discoverDeviceReceiver, filter)
        bluetoothAdapter.startDiscovery()
    }

    private val discoverDeviceReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            var action = ""
            if (intent != null) {
                action = intent.action.toString()
            }
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    Log.d("discoverDevices1", "STATE CHANGED")
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d("discoverDevices2", "Discovery Started")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("discoverDevices3", "Disvcoery Fininshed")
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        Log.d("discoverDevices4", "${device.name}  ${device.address}")
                        deviceCount++
                        devicesList.add("${device.name}->${device.address}")
                        deviceAdapter.notifyDataSetChanged()

                        when(device.bondState){
                            BluetoothDevice.BOND_NONE->{
                                Log.d("Bluetooth bond status", "${device.name} bond none")
                            }
                            BluetoothDevice.BOND_BONDING->{
                                Log.d("Bluetooth bond status", "${device.name} bond bonding")
                            }
                            BluetoothDevice.BOND_BONDED->{
                                Log.d("Bluetooth bond status", "${device.name} bonded")
                            }
                        }
                    }
                }
            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun getPairedDevices() {
        var arr = bluetoothAdapter.bondedDevices
        Log.d("bondedDevices", arr.size.toString())
        Log.d("bondedDevices", arr.toString())
        for (device in arr) {
            Log.d("bondedDevices", device.name + "   " + device.address + "   " + device.bondState)
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun enableDisableBluetooth() {
        binding.btONOFF.setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(intent)

                val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(receiver, intentFilter)
            }
            if (bluetoothAdapter.isEnabled) {
                bluetoothAdapter.disable()

                val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(receiver, intentFilter)
            }
        }
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun discoverability() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 20)
        startActivity(discoverableIntent)

        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        registerReceiver(receiver2, intentFilter)
    }
    @SuppressLint("MissingPermission")
    private fun pairWithDevice(macAddress: String){
        val remoteDevice = bluetoothAdapter.getRemoteDevice(macAddress)
        try {
            remoteDevice.createBond()
            Log.d("pairStatus", "pair succes")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("pairStatus", "pair failed")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}