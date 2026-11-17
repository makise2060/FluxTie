package com.huanchengfly.tieba.post.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import java.util.Calendar

/**
 * 外部自动化触发签到入口（macrodroid / Tasker 等通过显式广播调用）。
 *
 *  exported 开放是刻意的：触发即「用户本来也会做的签到动作」，且有三重幂等守卫——
 *  未登录、今天已签（`signDay` 在 startSign 内写入）、非本应用 action 均直接忽略，
 *  恶意反复拉起最多完成一次签到，不会产生重复请求。
 *
 *  用法：`adb shell am broadcast -a com.makise.fluxtie.action.TRIGGER_SIGN -p com.makise.fluxtie`
 */
class SignTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER_SIGN) return
        if (!AccountUtil.isLoggedIn()) {
            Log.i(TAG, "trigger sign ignored: not logged in")
            return
        }
        if (context.appPreferences.signDay == Calendar.getInstance()[Calendar.DAY_OF_MONTH]) {
            Log.i(TAG, "trigger sign ignored: already signed today")
            return
        }
        Log.i(TAG, "trigger sign accepted")
        TiebaUtil.startSign(context)
    }

    companion object {
        val TAG: String = "SignTrigger"
        const val ACTION_TRIGGER_SIGN = "com.makise.fluxtie.action.TRIGGER_SIGN"
    }
}
