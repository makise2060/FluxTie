package com.huanchengfly.tieba.post.components.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dpToPx
import com.huanchengfly.tieba.post.utils.PermissionUtils


class RequestPermissionTipDialog(context: Context, permission: PermissionUtils.PermissionData) :
    AlertDialog(context, R.style.Dialog_RequestPermissionTip) {
    val title: TextView
    val message: TextView

    /** 用户点击"去授权"后回调，此时才发起真正的系统权限请求 */
    var onConfirm: (() -> Unit)? = null

    init {
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        setView(View.inflate(context, R.layout.dialog_request_permission_tip, null).also {
            title = it.findViewById(R.id.request_permission_tip_dialog_title)
            message = it.findViewById(R.id.request_permission_tip_dialog_message)
        })
        val permissionName = PermissionUtils.transformText(context, permission.permissions).first()
        title.text = context.getString(R.string.title_request_permission_tip_dialog, permissionName)
        message.text =
            context.getString(R.string.message_request_permission_tip_dialog, permission.desc)
        setButton(
            BUTTON_POSITIVE,
            context.getString(R.string.button_request_grant),
            android.content.DialogInterface.OnClickListener { _, _ -> onConfirm?.invoke() }
        )
        setButton(
            BUTTON_NEGATIVE,
            context.getString(R.string.button_cancel),
            android.content.DialogInterface.OnClickListener { _, _ -> }
        )
    }

    override fun show() {
        super.show()
        window?.let {
            it.attributes = it.attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                it.decorView.setPadding(16f.dpToPx(), 0, 16f.dpToPx(), 0)
            }
            it.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        }
    }
}
