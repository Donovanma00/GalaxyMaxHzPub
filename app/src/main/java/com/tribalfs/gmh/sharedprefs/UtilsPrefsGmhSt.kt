package com.tribalfs.gmh.sharedprefs

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.highestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.SingletonMaker
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt
import com.tribalfs.gmh.hertz.HzGravity
import org.json.JSONObject

private const val GMH_PREFS = "gmh_prefs"
private const val HZ_OVERLAY_LOC = "hz_ol_gr"
private const val HZ_OVERLAY_SIZE = "hz_ol_sz"
private const val BRIGHTNESS_THRESHOLD = "brightness_threshold"
private const val PSM_SO = "psm_so"
private const val DISPLAY_MODES = "display_modes"
private const val ADAPTIVES = "adaptives"
private const val HZ_CONFIG_SYNCED = "hz_conf_sncd"
private const val HZ_CONFIG_FETCHED = "hz_conf_ftcd"
private const val HZ_CONFIG_FETCH_MON = "hz_conf_ftcd_mon"
private const val PSM_CACHE = "psm_cache"
private const val SHOW_NS_TOOL = "show_ns"
private const val TOP_BOT_CHIP_ID = "tb_chip"
private const val LCR_CHIP_ID = "lcr_chip"
private const val TOGGLE_MINIMUM_HZ = "tg_min_hz"
private const val SKIP_RES = "sk_rs"
private const val KEY_INDICATOR_SPEED_UNIT = "ksu"
private const val SPEED_TO_SHOW = "its"
//private const val USING_SPAY = "usg_spay"
//private const val SETTINGS_LIST_DONE = "list_done"
private const val HELP_URL = "help_url"
private const val PREVENT_HIGH = "prev_hi"
private const val SENSOR_ON_KEY = "s_on"
internal const val MIN_HZ_ADAPT = "min_hz_adp"
internal const val PREF_MAX_REFRESH_RATE = "pref_ref_rate"
internal const val PREF_MAX_REFRESH_RATE_PSM = "pref_ref_rate_psm"
internal const val KEEP_RRM = "keep_rrm"
internal const val IS_HZ_ON = "hz_on"
internal const val HZ_OVERLAY_ON = "hz_ol_on"
internal const val HZ_NOTIF_ON = "hz_ntf_on"
internal const val FORCE_LOWEST_HZ_SO = "fc_lwst_hz"
internal const val APPLY_SENSORS_OFF = "ss_off"
internal const val DEEP_DOZ_OPT = "dp_dz_opt"
internal const val QUICK_DOZE = "qk_dz"
internal const val DISABLE_SYNC = "dis_syn"
internal const val RESTORE_SYNC = "res_syn"
internal const val REFRESH_RATE_MODE_PREF = "rrm_pref"
internal const val ADAPTIVE_DELAY = "adp_delay"
internal const val NS_STARTED = "kis"
internal const val BYTE_PER_SEC = 0
internal const val BIT_PER_SEC = 1
internal const val TOTAL_SPEED = 2
internal const val UPLOAD_SPEED = 0
internal const val DOWNLOAD_SPEED = 1
//internal const val NOT_USING = 0
//internal const val USING = 1


internal class UtilsPrefsGmhSt private constructor(val context: Context) {

    companion object : SingletonMaker<UtilsPrefsGmhSt, Context>(::UtilsPrefsGmhSt)

    val hzSharedPref: SharedPreferences by lazy {context.applicationContext.getSharedPreferences(
        GMH_PREFS,
        Context.MODE_PRIVATE
    )}

    private val hzSharedPrefEditor by lazy {hzSharedPref.edit()}

    /*var hzPrefSPayUsage: Int
        get() = hzSharedPref.getInt(USING_SPAY, NOT_USING)
        set(usingSpay) =   hzSharedPrefEditor.putInt(USING_SPAY, usingSpay).apply()*/

    var hzPrefAdaptiveDelay:Long
        get() =  hzSharedPref.getLong(ADAPTIVE_DELAY, 1750L)
        set(prefDelay) = hzSharedPref.edit().putLong(ADAPTIVE_DELAY, prefDelay).apply()

    var hzPrefMaxRefreshRate: Int
        get() =  hzSharedPref.getInt(PREF_MAX_REFRESH_RATE, -1).let{
            if (it != -1){
                it.coerceAtLeast(lowestHzCurMode)
            }else {
                highestHzForAllMode
            }
        }
        set(prefMaxRefreshRate) = hzSharedPref.edit().putInt(PREF_MAX_REFRESH_RATE, prefMaxRefreshRate).apply()

    var hzPrefMaxRefreshRatePsm: Int
        get() =  hzSharedPref.getInt(PREF_MAX_REFRESH_RATE_PSM, -1).let{
            if (it != -1){
                it.coerceAtLeast(lowestHzCurMode)
            }else {
                highestHzForAllMode
            }
        }
        set(prefMaxRefreshRate) = hzSharedPref.edit().putInt(PREF_MAX_REFRESH_RATE_PSM, prefMaxRefreshRate).apply()

    var gmhPrefPsmIsOffCache: Boolean
        get() = hzSharedPref.getBoolean(PSM_CACHE, false)
        set(cache) =   hzSharedPrefEditor.putBoolean(PSM_CACHE, cache).apply()


    var prefProfileFetched: Boolean
        get() = hzSharedPref.getBoolean(HZ_CONFIG_FETCHED, false)
        set(fetched) =   hzSharedPrefEditor.putBoolean(HZ_CONFIG_FETCHED, fetched).apply()


    var gmhRefetchProfile: Boolean
        get() {
            val i = hzSharedPref.getInt(HZ_CONFIG_FETCH_MON, 0)
            return if (i > 0) {
                hzSharedPrefEditor.putInt(HZ_CONFIG_FETCH_MON, i - 1).apply()
                false
            } else {
                true
            }

        }
        set(fetch)  {
            hzSharedPrefEditor.putInt(HZ_CONFIG_FETCH_MON, if (fetch) 0 else 30).apply()
        }


    var gmhPrefChipIdTb: Int
        get() { return hzSharedPref.getInt(TOP_BOT_CHIP_ID, R.id.ch_top_hz)}
        set(chipId) {hzSharedPrefEditor.putInt(TOP_BOT_CHIP_ID, chipId).apply()}


/*    var gmhPrefChipIdLrc: Int
        get() { return hzSharedPref.getInt(LCR_CHIP_ID, R.id.ch_left_hz)}
        set(chipId) {hzSharedPrefEditor.putInt(LCR_CHIP_ID, chipId).apply()}*/


    var gmhPrefMinHzForToggle: Int
        get() { return hzSharedPref.getInt(TOGGLE_MINIMUM_HZ, 1)}
        set(hz) {hzSharedPrefEditor.putInt(TOGGLE_MINIMUM_HZ, hz).apply()}

    var gmhPrefIsHzSynced: Boolean
        get() {
            return hzSharedPref.getBoolean(HZ_CONFIG_SYNCED, false)
        }
        set(apply) {hzSharedPrefEditor.putBoolean(HZ_CONFIG_SYNCED, apply).apply()}

    var gmhPrefPreventHigh: Boolean
        get() {
            return hzSharedPref.getBoolean(PREVENT_HIGH, false)
        }
        set(apply) {hzSharedPrefEditor.putBoolean(PREVENT_HIGH, apply).apply()}

    var gmhPrefRefreshRateModePref: String?
        get() {
            return hzSharedPref.getString(REFRESH_RATE_MODE_PREF, null)
        }
        set(mode){hzSharedPrefEditor.putString(REFRESH_RATE_MODE_PREF, mode).apply()}

    var gmhPrefHelpUrl: String?
        get() {
            return hzSharedPref.getString(HELP_URL, null)
        }
        set(url){hzSharedPrefEditor.putString(HELP_URL, url).apply()}

    var gmhPrefHzOverlaySize: Float
        get() {
            return hzSharedPref.getFloat(HZ_OVERLAY_SIZE, 18f)
        }
        set(size) { hzSharedPrefEditor.putFloat(HZ_OVERLAY_SIZE, size).apply()}

    var gmhPrefHzPosition: Int
        get() {
            return hzSharedPref.getInt(HZ_OVERLAY_LOC, HzGravity.TOP_LEFT)
        }
        set(loc) {hzSharedPrefEditor.putInt(HZ_OVERLAY_LOC, loc).apply()}

    var gmhPrefPsmOnSo: Boolean
        get() { return hzSharedPref.getBoolean(PSM_SO, true) }
        set(enable) {hzSharedPrefEditor.putBoolean(PSM_SO, enable).apply()}


    var gmhPrefQuickDozeIsOn: Boolean
        get() { return hzSharedPref.getBoolean(QUICK_DOZE, false) }
        set(enable) {hzSharedPrefEditor.putBoolean(QUICK_DOZE, enable).apply()}

    var gmhPrefDisableSyncIsOn: Boolean
        get() { return hzSharedPref.getBoolean(DISABLE_SYNC, false) }
        set(enable) {hzSharedPrefEditor.putBoolean(DISABLE_SYNC, enable).apply()}

    var gmhPrefRestoreSyncIsOn: Boolean
        get() { return hzSharedPref.getBoolean(RESTORE_SYNC, false) && gmhPrefDisableSyncIsOn}
        set(enable) {hzSharedPrefEditor.putBoolean(RESTORE_SYNC, enable).apply()}


    var gmhPrefGetSkippedRes: Set<String>?
        get() {
            return hzSharedPref.getStringSet(SKIP_RES, null)
        }
        set(value) { hzSharedPrefEditor.putStringSet(SKIP_RES, value).apply()}


    var gmhPrefGDozeModOpt: Int
        get() {
            return hzSharedPref.getInt(DEEP_DOZ_OPT, 80)
        }
        set(value) { hzSharedPrefEditor.putInt(DEEP_DOZ_OPT, value).apply()}

    var gmhPrefGAdaptBrightnessMin: Int
        get() {
            return hzSharedPref.getInt(BRIGHTNESS_THRESHOLD, 0)
        }
        set(value) { hzSharedPrefEditor.putInt(BRIGHTNESS_THRESHOLD, value).apply()}

    var gmhPrefKmsOnPsm: Boolean
        get() = hzSharedPref.getBoolean(KEEP_RRM, false) //should be false
        set(keep) = (hzSharedPrefEditor.putBoolean(KEEP_RRM, keep).apply())

    var gmhPrefForceLowestSoIsOn: Boolean
        get() {
            return hzSharedPref.getBoolean(FORCE_LOWEST_HZ_SO, false)
        }
        set(apply){hzSharedPrefEditor.putBoolean(FORCE_LOWEST_HZ_SO, apply).apply()}

    var gmhPrefSensorsOff: Boolean
        get() {
            return hzSharedPref.getBoolean(APPLY_SENSORS_OFF, false)
        }
        set(apply){hzSharedPrefEditor.putBoolean(APPLY_SENSORS_OFF, apply).apply()}

    var gmhPrefHzIsOn: Boolean
        get() {
            return hzSharedPref.getBoolean(IS_HZ_ON, false)
        }
        set(isOn){hzSharedPrefEditor.putBoolean(IS_HZ_ON, isOn).apply()}


    var gmhPrefHzOverlayIsOn: Boolean
        get() {
            return hzSharedPref.getBoolean(HZ_OVERLAY_ON, false)
        }
        set(isOn){hzSharedPrefEditor.putBoolean(HZ_OVERLAY_ON, isOn).apply()}


    var gmhPrefHzNotifIsOn: Boolean
        get() {
            return hzSharedPref.getBoolean(HZ_NOTIF_ON, true)
        }
        set(isOn){hzSharedPrefEditor.putBoolean(HZ_NOTIF_ON, isOn).apply()}


    var gmhPrefDisplayModesObjectInJson: JSONObject?
        @Keep get() {
            (hzSharedPref.getString(
                DISPLAY_MODES, null))?.let{
                return JSONObject(it)
            }?: return null
        }
        @Keep set(displayModesJson){
            hzSharedPrefEditor.putString(
                DISPLAY_MODES,
                displayModesJson.toString()
            ).apply()
        }


    var gmhPrefMinHzAdapt: Int
        get() { return hzSharedPref.getInt(MIN_HZ_ADAPT, UtilsDeviceInfoSt.instance(context).regularMinHz) }
        set(hz) {hzSharedPrefEditor.putInt(MIN_HZ_ADAPT, hz).apply()}

    var gmhPrefGetAdaptives:  MutableList<String>?
        @Keep get() {return hzSharedPref.getStringSet(ADAPTIVES, setOf<String>())?.toMutableList() }
        @Keep set(adaptiveModels) { hzSharedPrefEditor.putStringSet(ADAPTIVES, adaptiveModels?.toSet()).apply()}


    /*For Netspeed*/
    var gmhPrefShowNetSpeedTool: Boolean
        get() {
            return hzSharedPref.getBoolean(SHOW_NS_TOOL, false)
        }
        set(show){ hzSharedPrefEditor.putBoolean(SHOW_NS_TOOL, show).apply() }

    var gmhPrefNetSpeedIsOn: Boolean
        get() = hzSharedPref.getBoolean(NS_STARTED, false)
        set(value) = hzSharedPrefEditor.putBoolean(NS_STARTED, value).apply()

    var gmhPrefSpeedUnit: Int
        get() {
            return hzSharedPref.getInt(
                KEY_INDICATOR_SPEED_UNIT,
                BIT_PER_SEC
            )
        }
        set(speedUnit) {hzSharedPrefEditor.putInt(KEY_INDICATOR_SPEED_UNIT, speedUnit).apply()}

    var gmhPrefSpeedToShow: Int
        get() { return hzSharedPref.getInt(SPEED_TO_SHOW, TOTAL_SPEED) }
        set(speedType) {hzSharedPrefEditor.putInt(SPEED_TO_SHOW, speedType).apply()}

/*    var gmhPrefSettingListDone: Boolean
        get() = hzSharedPref.getBoolean(SETTINGS_LIST_DONE, false)
        set(value) = hzSharedPrefEditor.putBoolean(SETTINGS_LIST_DONE, value).apply()*/

    var gmhPrefSensorOnKey: CharSequence?
        get() {
            return hzSharedPref.getString(SENSOR_ON_KEY, null)
        }
        set(key){hzSharedPrefEditor.putString(SENSOR_ON_KEY, key as String).apply()}


    fun getEnabledAccessibilityFeatures(): List<String>{
        val featuresOn = mutableListOf<String>()
        if (gmhPrefForceLowestSoIsOn) {
            featuresOn.add("-${context.applicationContext.getString(R.string.force_hz_mod)}")
        }
        if (gmhPrefPsmOnSo) {
            featuresOn.add("-${context.applicationContext.getString(R.string.auto_psm)}")
        }
        if (gmhPrefDisableSyncIsOn) {
            featuresOn.add("-${context.applicationContext.getString(R.string.disable_sync_on_so)}")
        }
        if (gmhPrefSensorsOff) {
            featuresOn.add("-${context.applicationContext.getString(R.string.auto_sensors_off_exp)}")
        }
        if (gmhPrefKmsOnPsm) {
            featuresOn.add("-${context.applicationContext.getString(R.string.prr_psm)}")
        }
        if (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS
                && (!isOfficialAdaptive  || hzSharedPref.getInt(MIN_HZ_ADAPT, UtilsDeviceInfoSt.instance(context).regularMinHz) < UtilsDeviceInfoSt.instance(context).regularMinHz)
        ){
            featuresOn.add("-${context.applicationContext.getString(R.string.adaptive)} mod")
        }
        return featuresOn
    }
}