package com.sd.demo.network

import android.content.Context
import android.content.Context.NSD_SERVICE
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.elvishew.xlog.XLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object NsdMangerUtil {
    private var nsdManager: NsdManager? = null
    private const val SERVICE_TYPE = "_p2proxy._udp"
    private var mDiscoveryListener: DiscoveryListener? = null
    lateinit var infoCallBack: (MutableList<NsdServiceInfo>) -> Unit
    var desList: MutableList<NsdServiceInfo> = ArrayList()
    var lock: WifiManager.MulticastLock? = null

    fun initNsdManger(context: Context, infoCallBack: (MutableList<NsdServiceInfo>) -> Unit) {
        this.infoCallBack = infoCallBack
        CoroutineScope(Dispatchers.IO).launch {
            nsdManager = context.getSystemService(NSD_SERVICE) as? NsdManager
            val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            lock = wifi.createMulticastLock("mDNSLock")
            lock?.setReferenceCounted(true)
            lock?.acquire()
        }
    }

    fun start() {
        desList.clear()
        XLog.tag("xm--------").d( "start")
        createDiscoverListener()
    }

    fun stop() {
        try {
            nsdManager?.stopServiceDiscovery(mDiscoveryListener)
            lock?.release()
            lock = null
//        mDiscoveryListener = null
//        nsdManager = null
            XLog.tag("xm--------").d( "stop")
            desList.clear()
        } catch (e: Exception) {
            XLog.tag("xm--------").d( "stop" + e)
        }
    }

    private fun createDiscoverListener() {
        mDiscoveryListener = object : DiscoveryListener {
                private var mmNsdServiceInfo: NsdServiceInfo? = null
                override fun onStartDiscoveryFailed(s: String, i: Int) {
                }

                override fun onStopDiscoveryFailed(s: String, i: Int) {
                }

                override fun onDiscoveryStarted(s: String) {
                }

                override fun onDiscoveryStopped(s: String) {
                }

                override fun onServiceFound(nsdServiceInfo: NsdServiceInfo) {
                    mmNsdServiceInfo = nsdServiceInfo
                    //这里的nsdServiceInfo只能获取到名字,ip和端口都不能获取到,要想获取到需要调用NsdManager.resolveService方法
                    XLog.tag("xm--------").d("onServiceFound" + nsdServiceInfo.serviceName)

                    nsdManager!!.resolveService(
                        mmNsdServiceInfo,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, i: Int) {
                            }

                            override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
                                try {
                                    var didHad = false
                                    for (info in desList) {
                                        if (info.serviceName == nsdServiceInfo.serviceName) {
                                            didHad = true
                                            break
                                        }
                                    }
                                    if (!didHad) {
                                        desList.add(nsdServiceInfo)
                                    }
                                    infoCallBack.invoke(desList)
                                }  catch (e: Exception) {
                                    XLog.tag("xm--------").d("onServiceFound error" + e.message)
                                }
                            }
                        })
                }

                override fun onServiceLost(nsdServiceInfo: NsdServiceInfo) {
                }
            }
        try {
            CoroutineScope(Dispatchers.IO).launch {
                nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
            }
        } catch (e: Exception) {
            XLog.tag("xm--------").d( "mDnsService ---- discoverServices error ${e.message}")
        }
    }

    fun parseTXTBytes(txtData: ByteArray): String {
        var result = ""
        var index = 0

        while (index < txtData.size) {
            // 读取当前块的长度
            val length = txtData[index].toInt()
            index += 1 // 移动到数据块开始位置

            // 检查数据长度是否合法
            if (index + length > txtData.size) {
                println("Error: Invalid length at index $index")
                break
            }

            // 提取对应长度的数据块
            val chunk = txtData.sliceArray(index until index + length)
            index += length // 移动到下一个块的长度位置

            // 将数据块转换为字符串
            result = String(chunk, Charsets.UTF_8)
        }

        return result
    }
}