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
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.bluetooth.ScanResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.sd.demo.network.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SamplemDNSDiscover : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.1f)
                            .fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 50.dp, end = 50.dp, top = 50.dp), // 使按钮横向填满父布局
                            horizontalArrangement = Arrangement.SpaceBetween, // 左右间隔
                            verticalAlignment = Alignment.CenterVertically // 垂直居中
                        ) {
                            Button(onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    /* 扫描按钮点击事件 */
                                    val addr = mDnsService("peer")
                                    NsdMangerUtil.stop()
                                    isNdsService = false
                                }
                            }) {
                                Text(text = "扫描")
                            }
                            Button(onClick = {
                                /* 另一个按钮的点击事件 */
                                NsdMangerUtil.stop()
                            }) {
                                Text(text = "停止")
                            }
                            Button(onClick = {
                                /* 另一个按钮的点击事件 */
                                _mDnsList.clear()
                                serviceName.clear()
                            }) {
                                Text(text = "清空")
                            }
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
                            items = mDnsList,
                            //key = { it.rssi },
                        ) { item ->
                            ItemView(scanResult = item)
                        }
                    }
                }

            }
        }
    }

    @Composable
    private fun ItemView(
        modifier: Modifier = Modifier,
        scanResult: String,
    ) {
        Card(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "发现设备:\n 名称 - ${scanResult}",
                modifier = Modifier.padding(10.dp),
            )
        }
    }

    var isNdsService = false
    var mDnsAddrList = emptyList<String>()
    private val _mDnsList = mutableStateListOf<String>()
    val mDnsList: List<String> get() = _mDnsList
    var serviceName = mutableListOf<String>()
    suspend fun mDnsService(peer: String?): String {
        //acquireMultiLock(context)
        var addrPeer = "" // 初始化 addrPeer 为空字符串
        val future = CompletableFuture<String>() // 创建一个 CompletableFuture

        NsdMangerUtil.initNsdManger(this) { services ->
            run {
                services.forEach { info ->
                    if (serviceName.contains(info.serviceName)) {
                        return@forEach
                    } else {
                        val listAddr = mutableListOf<String>()
                        info.attributes.forEach {
                            it.value?.let {
                                listAddr.add(String(it, StandardCharsets.UTF_8))
                            }
                        }

                        mDnsAddrList = listAddr.map {
                            it.split("p2p").first()
                        }.toList()


                        serviceName.add(info.serviceName)
                        addrPeer = JSONObject().apply {
                            put("ID", info.serviceName)
                            put("Addrs", JSONArray(mDnsAddrList))
                        }.toString()
                        _mDnsList.add(addrPeer)

                        XLog.tag("nsd")
                            .e("NsdMangerUtil.initNsdManger addrPeer: ${info.serviceName} addr ==${addrPeer}")
                    }

                    // future.complete(addrPeer)
                    //   return@run
//                        if (info.serviceName == peer) {
//
//                        }
                }
            }

        }
        NsdMangerUtil.start()

        return try {
            withContext(Dispatchers.IO) {
                async(Dispatchers.IO) {
                    future.get(5, TimeUnit.SECONDS)
                }.await()
            } // 等待最多3秒
        } catch (e: TimeoutException) {
            "" //超时
        } catch (e: Exception) {
            ""
        }
    }

    override fun onStop() {
        super.onStop()
    }
}