package cn.ppps.forwarder.utils

import android.content.Context
import android.location.Criteria
import android.location.LocationManager

/**
 * 定位 provider 选择。
 *
 * 原实现把「精度」与「电耗」一起交给 `getBestProvider`，由库自行挑选 provider。
 * 但 `getBestProvider` 会把 `accuracy` 与 `powerRequirement` 同时作为筛选条件，
 * 两者存在交集为空的情况（例如「精确位置 + 低电耗」），
 * 此时库会退回默认行为，实际使用的 provider 与用户设置不一致。
 * 另外 `fused` provider 会自行决定是否拉起 GNSS，导致「模糊位置」形同虚设。
 *
 * 这里改为按「精度」显式指定 provider，电耗不再参与筛选。
 *
 * 设置值对照（SettingsFragment）：
 *  位置精度：精确 = ACCURACY_FINE(1)，模糊 = ACCURACY_COARSE(2)，不要求 = NO_REQUIREMENT(0)
 */
object LocationProviderPicker {

    private fun lm(context: Context): LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private fun hasProvider(context: Context, provider: String): Boolean = try {
        lm(context)?.allProviders?.contains(provider) == true
    } catch (e: Exception) {
        false
    }

    private fun isEnabled(context: Context, provider: String): Boolean = try {
        lm(context)?.isProviderEnabled(provider) == true
    } catch (e: Exception) {
        false
    }

    private fun networkOrNull(context: Context): String? =
        if (hasProvider(context, LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else null

    private fun gpsOrNull(context: Context): String? =
        if (isEnabled(context, LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else null

    /**
     * provider 是否真的可用：系统里有它，并且没被用户关掉。
     *
     * 只查 [LocationManager.getProviders] 不够——GPS 开关关闭时 provider 仍然存在，
     * 对它注册监听不会报错但永远收不到回调，只会白挂着耗电。
     */
    fun isReady(context: Context, provider: String): Boolean =
        hasProvider(context, provider) && isEnabled(context, provider)

    /** 是否「只要大致位置」。只有显式选「模糊位置」才算。 */
    fun coarseOnly(): Boolean = SettingUtils.locationAccuracy == Criteria.ACCURACY_COARSE

    /**
     * 该用哪个 provider。
     *
     *  - 模糊位置：只用 network，绝不点亮 GNSS
     *  - 精确 / 不要求：GPS 优先，GPS 不可用时退 network
     *
     * 返回 null 表示交回系统决定（正常情况下不会发生）。
     */
    fun primary(context: Context): String? {
        if (coarseOnly()) return networkOrNull(context)
        return gpsOrNull(context) ?: networkOrNull(context)
    }
}
