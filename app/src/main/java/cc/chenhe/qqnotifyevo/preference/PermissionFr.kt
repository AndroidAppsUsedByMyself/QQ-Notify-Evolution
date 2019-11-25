package cc.chenhe.qqnotifyevo.preference

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cc.chenhe.qqnotifyevo.AccessibilityMonitorService
import cc.chenhe.qqnotifyevo.R

class PermissionFr : PreferenceFragmentCompat() {

    private lateinit var ctx: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_permission, rootKey)
        refreshSummary()
    }

    override fun onResume() {
        super.onResume()
        refreshSummary()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "notf_permit" -> {
                openNotificationListenSettings()
                return true
            }
            "aces_permit" -> {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return true
            }
            "bet_permit" -> {
                ignoreBatteryOptimization(activity!!)
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun refreshSummary() {
        findPreference<Preference>("notf_permit")?.summary =
                getString(if (isNotificationListenerEnabled(ctx)) R.string.pref_enable_permit else R.string.pref_disable_permit)

        findPreference<Preference>("aces_permit")?.summary =
                getString(if (isAccessibilitySettingsOn(ctx)) R.string.pref_enable_permit else R.string.pref_disable_permit)

        findPreference<Preference>("bet_permit")?.summary =
                getString(if (isIgnoreBatteryOptimization(ctx)) R.string.pref_enable_permit else R.string.pref_disable_permit)
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val s = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return s != null && s.contains(context.packageName)
    }

    private fun isAccessibilitySettingsOn(context: Context): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/" + AccessibilityMonitorService::class.java.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.applicationContext.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }

        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(context.applicationContext.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isIgnoreBatteryOptimization(context: Context): Boolean {
        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        else true
    }

    private fun openNotificationListenSettings() {
        try {
            val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
     */
    @SuppressLint("BatteryLife")
    private fun ignoreBatteryOptimization(activity: Activity) {
        val powerManager = activity.getSystemService(POWER_SERVICE) as PowerManager
        val hasIgnored: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.packageName)
            Log.e("JHH", hasIgnored.toString() + "")
            if (!hasIgnored) {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + activity.packageName)).let {
                    activity.startActivity(it)
                }
            }
        }
    }
}
