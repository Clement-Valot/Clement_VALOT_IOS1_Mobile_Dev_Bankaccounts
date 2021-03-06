@file:Suppress("DEPRECATION")

package com.example.bankaccounts

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import androidx.lifecycle.LiveData
import java.lang.IllegalArgumentException


//Class that checks whether or not the client is connected to the internet
class NetworkConnection(private val context: Context): LiveData<Boolean>() {
    private var CManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onActive(){
        super.onActive()
        updateConnection()
        when{
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                CManager.registerDefaultNetworkCallback(connectivityManagerCallback())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                lollipopNetworkRequest()
            }
            else -> {
                context.registerReceiver(
                    networkReceiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                )
            }
        }
    }

    override fun onInactive(){
        super.onInactive()
        try{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CManager.unregisterNetworkCallback(connectivityManagerCallback())
            }
            else {
                context.unregisterReceiver(networkReceiver)
            }
        }
        catch(e: IllegalArgumentException){
            e.printStackTrace()
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun lollipopNetworkRequest(){
        val requestBuilder = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        CManager.registerNetworkCallback(
                requestBuilder.build(),
                connectivityManagerCallback()
        )
    }

    private fun connectivityManagerCallback(): ConnectivityManager.NetworkCallback {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            networkCallback = object: ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    super.onLost(network)
                    postValue(false)
                }

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    postValue(true)
                }
            }
            return networkCallback
        }else{
            throw IllegalAccessError("Error")
        }
    }

    private val networkReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?){
            updateConnection()
        }
    }

    private fun updateConnection(){
        val activeNetwork: NetworkInfo? = CManager.activeNetworkInfo
        postValue(activeNetwork?.isConnected ==true)
    }
}