package com.sd.demo.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanResult as OldScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.ScanFilter
import androidx.bluetooth.ScanResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sd.demo.network.theme.AppTheme
import com.sd.lib.network.FNetwork
import com.sd.lib.network.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class SampleL2CapBlueTooth : ComponentActivity() {
    private val _scanResults = mutableStateListOf<ScanResult>()
    val scanResults: List<ScanResult> get() = _scanResults
    private val _oldScanResults = mutableStateListOf<OldScanResult>()
    val oldScanResults: List<OldScanResult> get() = _oldScanResults
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothSocket: BluetoothSocket? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 检查蓝牙扫描权限
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // 如果没有权限，向用户请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1100
            )
            return
        }

        startBleScan()
        setContent {
            AppTheme {
                ContentView(emptyList(), oldScanResults, btnClick) {
                    //建立连接
                    bluetoothDevice = it.device
                    val gattCallback = MyGattCallback {
                        //创建L2cap通道
                        bluetoothSocket = bluetoothDevice?.createInsecureL2capChannel(128)
                        if (bluetoothSocket == null) {
                            throw Exception("Failed to create L2CAP channel")
                        }
                        bluetoothSocket?.connect()
                        logMsg("L2cap") {
                            "bluetoothSocket.connect 成功"
                        }
                    }// 实现自定义的BluetoothGattCallback
                    bluetoothGatt = bluetoothDevice?.connectGatt(this, false, gattCallback)
                }
            }
        }
    }

    //发送
    val btnClick = {
        lifecycleScope.launch(Dispatchers.IO) {
            bluetoothSocket?.outputStream?.write(stringToByteArray("Hello,L2cap 445521"))
            logMsg("L2cap") {
                "outputstream" +  "Hello,L2cap 445521"
            }
            val inputStream = bluetoothSocket?.inputStream
            val buffer = ByteArray(125)  // 创建一个缓冲区
            inputStream?.read(buffer)
            logMsg("L2cap") {
                "inputStream: ${String(buffer)}"
            }
        }
    }

    fun stringToByteArray(input: String, charset: String = "UTF-8"): ByteArray {
        return input.toByteArray(charset(charset))
    }


    lateinit var bluetoothAdapter: BluetoothAdapter

    override fun onStop() {
        super.onStop()
        stopBleScan()
    }

    fun stopBleScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    //蓝牙扫描回调
    val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: OldScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val deviceName = device.name ?: "Unknown"
            val deviceAddress = device.address
            val rssi = result.rssi

            // 在此处处理扫描到的设备信息，如添加到列表、更新UI等
            if (deviceName != "Unknown") {
                logMsg("old scan") { "发现BLE设备: 名称 - $deviceName, 地址 - $deviceAddress, 信号强度 - $rssi " }
                val re = oldScanResults.find {
                    it.device.name == result.device.name
                }

                if (re == null) {
                    _oldScanResults.add(result)
                    if (deviceName == "go_bluetooth") {
                        stopBleScan()
                    }
                }
            }

        }

        override fun onBatchScanResults(results: MutableList<OldScanResult>) {
            super.onBatchScanResults(results)
            // 如果需要处理批量扫描结果，可以在此处进行
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            logMsg("old scan") { "BLE扫描失败，错误代码: $errorCode" }
            // 根据错误码处理扫描失败情况
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        try {
            // 获取蓝牙管理器
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            // 获取蓝牙适配器
            bluetoothAdapter = bluetoothManager.adapter
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // 或选择其他适合的扫描模式
                .build()
            //  val filters = listOf<ScanFilter>() // 可以添加过滤条件，如特定的服务UUID等

            if (bluetoothAdapter == null) {
                // 设备不支持蓝牙
                Log.e("Bluetooth", "Bluetooth not supported")
            } else if (!bluetoothAdapter.isEnabled) {
                // 蓝牙未开启，要求用户开启蓝牙
                Log.e("Bluetooth", "Bluetooth not enabled")
            } else {
                val bluetoothLeScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                if (bluetoothLeScanner != null) {
                    // 继续调用 startScan
                    bluetoothLeScanner.startScan(emptyList(), settings, scanCallback)
                } else {
                    Log.e("Bluetooth", "BluetoothLeScanner is null")
                }
            }

        } catch (e: Exception) {
            logMsg("ble scan") {
                "$e"
            }
        }

    }


    class MyGattCallback(val connect: (() -> Unit)) : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    logMsg("gatt") {
                        "已连接到GATT服务器。"
                    }
                   // gatt.discoverServices() // 连接成功后触发服务发现
                    connect.invoke()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    logMsg("gatt") {
                        "已断开与GATT服务器的连接。"
                    }
                    // 在此处处理断开连接的逻辑
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 在此处发现并处理服务及特征
                // 使用gatt.services访问可用服务

            } else {
                logMsg("gatt") {
                    "onServicesDiscovered 接收到状态码: $status"
                }
            }
        }
        // 根据需要覆盖其他相关函数，如onCharacteristicRead、onCharacteristicWrite等
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ContentView(
    list: List<ScanResult>,
    old: List<OldScanResult>,
    onBtnClick: () -> Job,
    onClick: (OldScanResult) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.weight(0.2f).fillMaxSize()
        ) {
            Button(
                contentPadding = PaddingValues(20.dp),
                onClick = { onBtnClick.invoke() },
                modifier = Modifier
                    .width(100.dp)
                    .height(60.dp)
                    .align(Alignment.Center)
            ) {
                Text("发送")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(10.dp),
        ) {
            items(
                items = old,
                //key = { it.rssi },
            ) { item ->
                OldItemView(scanResult = item, onClick = onClick)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun OldItemView(
    modifier: Modifier = Modifier,
    scanResult: OldScanResult,
    onClick: (OldScanResult) -> Unit  // 添加一个点击事件的回调
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick.invoke(scanResult) })  // 在 Card 上设置点击事件
    ) {
        Text(
            text = "发现OldBLE设备:\n 名称 - ${scanResult.device.name},\n 地址 - ${scanResult.device.address},\n 信号强度 - ${scanResult.rssi}",
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
private fun ItemView(
    modifier: Modifier = Modifier,
    scanResult: ScanResult,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "发现BLE设备:\n 名称 - ${scanResult.device.name},\n 地址 - ${scanResult.deviceAddress}, \n信号强度 - ${scanResult.rssi}",
            modifier = Modifier.padding(10.dp),
        )
    }
}