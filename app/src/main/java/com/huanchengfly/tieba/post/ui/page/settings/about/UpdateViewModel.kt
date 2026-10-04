package com.huanchengfly.tieba.post.ui.page.settings.about

import androidx.lifecycle.ViewModel
import com.huanchengfly.tieba.post.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 关于页薄 ViewModel：仅作为 UpdateManager 单例的页面入口（状态在 Manager 中，弹窗全局承载）。
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val manager: UpdateManager,
) : ViewModel() {

    fun check() {
        manager.checkManually()
    }
}
