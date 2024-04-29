package com.idormy.sms.forwarder.utils

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

@Suppress("DEPRECATION")
object LocationUtils {

    private const val LOCATION_MODE_OFF = 0

    private fun hasLocationPermission(context: Context): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("LocationUtils", "hasLocationPermission: $hasPermission")
        return hasPermission
    }

    fun isLocationEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            Log.d("LocationUtils", "isLocationEnabled: ${locationManager?.isLocationEnabled}")
            locationManager?.isLocationEnabled == true
        } else {
            try {
                val locationMode = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE
                )
                Log.d("LocationUtils", "isLocationEnabled: locationMode=$locationMode")
                locationMode != LOCATION_MODE_OFF
            } catch (e: Settings.SettingNotFoundException) {
                false
            }
        }
    }

    fun hasLocationCapability(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        // 检查是否有位置权限
        if (!hasLocationPermission(context)) {
            Log.e("LocationUtils", "hasLocationCapability: no location permission")
            return false
        }

        // 检查是否有定位能力
        val hasGpsProvider = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val hasNetworkProvider = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        val hasPassiveProvider = locationManager?.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) == true

        Log.d("LocationUtils", "hasLocationCapability: hasGpsProvider=$hasGpsProvider, hasNetworkProvider=$hasNetworkProvider, hasPassiveProvider=$hasPassiveProvider")
        return hasGpsProvider || hasNetworkProvider || hasPassiveProvider
    }
}
