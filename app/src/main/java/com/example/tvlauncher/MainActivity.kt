package com.example.tvlauncher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : Activity() {

    companion object {
        /** Quantidade de colunas da grade. */
        private const val COLUMNS = 4

        /** Pacotes que você não quer ver na grade. Ex.: setOf("com.amazon.tv.launcher") */
        private val HIDDEN_PACKAGES = setOf<String>()
    }

    private lateinit var recycler: RecyclerView
    private lateinit var wifiButton: ImageButton
    private val adapter = AppAdapter(::launch, ::showAppInfo)
    private val worker = Executors.newSingleThreadExecutor()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = loadApps()
    }

    @Suppress("DEPRECATION")
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = updateWifiIcon()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.apps)
        recycler.layoutManager = GridLayoutManager(this, COLUMNS)
        recycler.adapter = adapter
        recycler.itemAnimator = null

        wifiButton = findViewById(R.id.wifi)
        wifiButton.setOnClickListener { openWifiSettings() }
        findViewById<ImageButton>(R.id.settings).setOnClickListener { openSettings() }

        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, pkgFilter)

        loadApps()
    }

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        updateWifiIcon()
    }

    override fun onStop() {
        unregisterReceiver(networkReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        worker.shutdown()
        super.onDestroy()
    }

    /** Botão Home pressionado com o launcher já aberto: volta ao início da grade. */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        focusFirstApp()
    }

    /** Um launcher não deve fechar com o botão Voltar. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        recycler.scrollToPosition(0)
    }

    // ---------- Apps ----------

    private fun loadApps() {
        worker.execute {
            val pm = packageManager
            val found = LinkedHashMap<String, AppEntry>()

            for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
                val query = Intent(Intent.ACTION_MAIN).addCategory(category)
                for (info in pm.queryIntentActivities(query, 0)) {
                    val ai = info.activityInfo ?: continue
                    val pkg = ai.packageName
                    if (pkg == packageName || pkg in HIDDEN_PACKAGES || found.containsKey(pkg)) continue

                    val launch = Intent(Intent.ACTION_MAIN)
                        .addCategory(category)
                        .setClassName(pkg, ai.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                    found[pkg] = AppEntry(
                        label = info.loadLabel(pm).toString(),
                        packageName = pkg,
                        intent = launch,
                        banner = ai.loadBanner(pm),
                        icon = info.loadIcon(pm)
                    )
                }
            }

            val sorted = found.values.sortedBy { it.label.lowercase() }
            runOnUiThread {
                adapter.submit(sorted)
                focusFirstApp()
            }
        }
    }

    private fun focusFirstApp() {
        recycler.scrollToPosition(0)
        recycler.post { recycler.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    private fun launch(app: AppEntry) {
        try {
            startActivity(app.intent)
        } catch (e: Exception) {
            // App removido ou sem atividade válida; atualiza a lista.
            loadApps()
        }
    }

    /** Pressionar e segurar abre as informações do app (onde dá para desinstalar). */
    private fun showAppInfo(app: AppEntry) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", app.packageName, null))
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { }
    }

    // ---------- Barra superior ----------

    private fun openWifiSettings() {
        try {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (_: Exception) {
            openSettings()
        }
    }

    private fun openSettings() {
        try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) { }
    }

    private fun updateWifiIcon() {
        var connected = true
        if (Build.VERSION.SDK_INT >= 23) {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            connected = caps != null &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        }
        wifiButton.alpha = if (connected) 1f else 0.4f
    }
}
