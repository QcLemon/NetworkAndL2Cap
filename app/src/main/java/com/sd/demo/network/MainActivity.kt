package com.sd.demo.network

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.FileSizeBackupStrategy
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import com.sd.demo.network.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    CoroutineScope(Dispatchers.IO).launch {
      val config = LogConfiguration.Builder()
        .tag("mdns")
        .enableThreadInfo()             // 允许打印线程信息，默认禁止
        .enableStackTrace(2)            // 允许打印深度为 2 的调用栈信息，默认禁止
        .enableBorder()
        //.borderFormatter(LogBorderFormatter())
        .build()

      val androidPrinter =  AndroidPrinter(true)
      XLog.init(config, androidPrinter)

      XLog.tag("MainActivity").d( "Application onCreate called")
    }

    setContent {
      AppTheme {
        Content(
          listActivity = listOf(
            SampleCurrentNetwork::class.java,
            SampleAllNetworks::class.java,
            SampleWaitNetwork::class.java,
            SampleL2CapBlueTooth::class.java,
            SamplemDNSDiscover::class.java,
          ),
          onClickActivity = {
            startActivity(Intent(this, it))
          },
        )
      }
    }
  }
}

@Composable
private fun Content(
  listActivity: List<Class<out Activity>>,
  onClickActivity: (Class<out Activity>) -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding(),
    verticalArrangement = Arrangement.spacedBy(5.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    items(listActivity) { item ->
      Button(onClick = { onClickActivity(item) }) {
        Text(text = item.simpleName)
      }
    }
  }
}

inline fun logMsg(tag: String ="network-demo",block: () -> String) {
  Log.i(tag, block())
}