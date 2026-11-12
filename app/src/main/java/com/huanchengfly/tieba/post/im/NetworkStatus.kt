package com.huanchengfly.tieba.post.im

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 当前可用于访问 IM 服务的网络状态。
 *
 * 用 NetworkCallback 而不是过时的 activeNetworkInfo：回调在网络切换/恢复时主动唤醒重连，
 * 避免离线期间持续进行无意义的握手。
 */
class NetworkStatus(context: Context) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _online = MutableStateFlow(isCurrentlyOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            update()
        }

        override fun onLost(network: Network) {
            update()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            update()
        }
    }

    init {
        runCatching {
            manager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                callback,
            )
        }.onFailure { Log.w("NetworkStatus", "网络回调注册失败", it) }
    }

    fun isCurrentlyOnline(): Boolean {
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun update() {
        _online.value = isCurrentlyOnline()
    }
}
