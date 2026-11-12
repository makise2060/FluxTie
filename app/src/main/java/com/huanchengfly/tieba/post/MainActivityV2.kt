package com.huanchengfly.tieba.post

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.plusAssign
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import androidx.compose.material.navigation.bottomSheet
import androidx.compose.material.navigation.rememberBottomSheetNavigator
import com.stoyanvuchev.systemuibarstweaker.SystemUIBarsTweaker
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.ClipBoardForumLink
import com.huanchengfly.tieba.post.components.ClipBoardLink
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.ClipBoardThreadLink
import com.huanchengfly.tieba.post.services.NotifyJobService
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.NavGraphs
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.MainPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.main.MAIN_TAB_ID_NOTIFICATION
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.huanchengfly.tieba.post.ui.utils.isBookPosture
import com.huanchengfly.tieba.post.ui.utils.isSeparating
import com.huanchengfly.tieba.post.update.UpdateManager
import com.huanchengfly.tieba.post.ui.widgets.compose.AppSplashOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UpdateDialogHost
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.JobServiceUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil
import com.huanchengfly.tieba.post.utils.StartupMetrics
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.compose.LaunchActivityForResult
import com.huanchengfly.tieba.post.utils.compose.LaunchActivityRequest
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.newIntentFilter
import com.huanchengfly.tieba.post.utils.registerPickMediasLauncher
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import com.huanchengfly.tieba.post.utils.requestPermission
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import com.ramcosta.composedestinations.utils.toDestinationsNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.huanchengfly.tieba.post.dataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

val LocalNotificationCountFlow =
    staticCompositionLocalOf<Flow<Int>> { throw IllegalStateException("not allowed here!") }
val LocalDevicePosture =
    staticCompositionLocalOf<State<DevicePosture>> { throw IllegalStateException("not allowed here!") }
val LocalNavController =
    staticCompositionLocalOf<NavHostController> { throw IllegalStateException("not allowed here!") }
val LocalDestination = compositionLocalOf<DestinationSpec<*>?> { null }

/**
 * 外部入口（通知、快捷方式）请求主界面切到底栏某个 tab。
 *
 * 用 StateFlow 而不是 GlobalEvent：通知点击可能发生在冷启动（进程已被回收）时，
 * 事件流没有重放，请求会丢；StateFlow 的当前值能在 MainPage 首次组合时补上。
 * MainPage 消费后置空，避免配置变更/回栈时重复切 tab。
 */
class MainTabRequestState {
    private val request = MutableStateFlow<String?>(null)
    val tabId: StateFlow<String?> = request.asStateFlow()

    fun request(tabId: String) {
        request.value = tabId
    }

    fun consume() {
        request.value = null
    }
}

val LocalMainTabRequest =
    staticCompositionLocalOf<MainTabRequestState> { throw IllegalStateException("not allowed here!") }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberBottomSheetNavigator(
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    skipHalfExpanded: Boolean = false
): BottomSheetNavigator {
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        animationSpec = animationSpec,
        skipHalfExpanded = skipHalfExpanded
    )
    return remember(sheetState) { BottomSheetNavigator(sheetState) }
}

@AndroidEntryPoint
class MainActivityV2 : BaseComposeActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val newMessageReceiver: BroadcastReceiver = NewMessageReceiver()

    @Inject
    lateinit var updateManager: UpdateManager

    private val notificationCountFlow: MutableSharedFlow<Int> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** 通知/快捷方式点击待消费的底栏 tab 请求 */
    private val mainTabRequest = MainTabRequestState()

    private val pickMediasLauncher =
        registerPickMediasLauncher {
            emitGlobalEvent(GlobalEvent.SelectedImages(it.id, it.uris))
        }

    private val mLaunchActivityForResultLauncher = registerForActivityResult(
        LaunchActivityForResult()
    ) {
        emitGlobalEvent(GlobalEvent.ActivityResult(it.requesterId, it.resultCode, it.intent))
    }

    private val devicePostureFlow: StateFlow<DevicePosture> by lazy {
        WindowInfoTracker.getOrCreate(this)
            .windowLayoutInfo(this)
            .flowWithLifecycle(lifecycle)
            .map { layoutInfo ->
                val foldingFeature =
                    layoutInfo.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .firstOrNull()
                when {
                    isBookPosture(foldingFeature) ->
                        DevicePosture.BookPosture(foldingFeature.bounds)

                    isSeparating(foldingFeature) ->
                        DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

                    else -> DevicePosture.NormalPosture
                }
            }
            .stateIn(
                scope = lifecycleScope,
                started = SharingStarted.Eagerly,
                initialValue = DevicePosture.NormalPosture
            )
    }

    private var direction: Direction? = null
    private var waitingNavCollectorToNavigate = AtomicBoolean(false)
    private var myNavController: NavHostController? = null
        set(value) {
            field = value
            if (value != null && waitingNavCollectorToNavigate.get() && direction != null) {
                launch {
                    value.currentDestinationFlow
                        .take(1)
                        .collect {
                            if (waitingNavCollectorToNavigate.get() && direction != null) {
                                value.toDestinationsNavigator().navigate(direction!!)
                                waitingNavCollectorToNavigate.set(false)
                                direction = null
                            }
                        }
                }
            }
        }

    private fun navigate(direction: Direction) {
        if (myNavController == null) {
            waitingNavCollectorToNavigate.set(true)
            this.direction = direction
        } else {
            myNavController?.toDestinationsNavigator()?.navigate(direction)
        }
    }

    private fun checkIntent(intent: Intent): Boolean {
        val data = intent.data
        // 通知点击：tblite://notifications/{initialTab} → 切底栏「消息」tab（issue #32）
        if (data?.scheme == "tblite" && data.host == "notifications") {
            openNotificationsTab()
            return true
        }
        return if (data?.scheme == "com.baidu.tieba" && data.host == "unidispatch") {
            val uri = data
            when (uri.path.orEmpty().lowercase()) {
                "/frs" -> {
                    val forumName = uri.getQueryParameter("kw") ?: return true
                    navigate(ForumPageDestination(forumName))
                }

                "/pb" -> {
                    val threadId = uri.getQueryParameter("tid")?.toLongOrNull() ?: return true
                    navigate(ThreadPageDestination(threadId))
                }
            }
            true
        } else return if (data?.host == "tieba.baidu.com") {
            val uri = data
            val path = uri.path.orEmpty().lowercase()
            when {
                path == "/f" -> {
                    val forumName = uri.getQueryParameter("kw") ?: return true
                    navigate(ForumPageDestination(forumName))
                }

                path.startsWith("/p/") -> {
                    val threadId = uri.pathSegments.getOrNull(1)?.toLongOrNull() ?: return true
                    navigate(ThreadPageDestination(threadId))
                }

                // 移动端跳转形态：/p?kz=<tid>、/mo/q/m?kz=<tid>（issue #19）
                path == "/p" || path == "/mo/q/m" -> {
                    val threadId = uri.getQueryParameter("kz")?.toLongOrNull()
                        ?: uri.getQueryParameter("tid")?.toLongOrNull()
                        ?: return true
                    navigate(ThreadPageDestination(threadId))
                }
            }
            true
        } else {
            false
        }
    }

    /**
     * 切到「消息」tab。
     *
     * 冷启动时 NavController 尚未创建，请求先落在 [MainTabRequestState] 里等 MainPage 首次组合消费；
     * 已运行时若当前停在帖子/设置等页面，先回主界面，否则 MainPage 不参与组合、收不到切 tab 请求。
     */
    private fun openNotificationsTab() {
        mainTabRequest.request(MAIN_TAB_ID_NOTIFICATION)
        myNavController?.popBackStack(MainPageDestination.route, inclusive = false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!checkIntent(intent)) {
            myNavController?.handleDeepLink(intent)
        }
        handleUpdateIntent(intent)
    }

    /** 通知点击「安装更新」入口（单例状态在手时直接进安装流程）。 */
    private fun handleUpdateIntent(intent: Intent) {
        if (intent.getBooleanExtra(UpdateManager.EXTRA_INSTALL_UPDATE, false)) {
            intent.removeExtra(UpdateManager.EXTRA_INSTALL_UPDATE)
            updateManager.installOrPrompt(this)
        }
    }

    private fun fetchAccount() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (AccountUtil.isLoggedIn()) {
                AccountUtil.fetchAccountFlow()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        toastShort(e.getErrorMessage())
                        e.printStackTrace()
                    }
                    .collect()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !AccountUtil.isLoggedIn()) return
        launch {
            // 用户拒绝过就不再主动请求（issue #24），需要时到系统设置里开启
            val denied = dataStore.data.first()[booleanPreferencesKey("notification_permission_denied")] ?: false
            if (denied) return@launch
            requestPermission {
                permissions = listOf(PermissionUtils.POST_NOTIFICATIONS)
                description = getString(R.string.desc_permission_post_notifications)
                onGranted = {
                    // 曾拒绝后又授权，重置标记以便再次拒绝时仍能提示
                    dataStore.putBoolean("notification_permission_denied", false)
                }
                onDenied = {
                    dataStore.putBoolean("notification_permission_denied", true)
                }
                onCancel = {
                    dataStore.putBoolean("notification_permission_denied", true)
                }
            }
        }
    }

    private fun initAutoSign() {
        runCatching {
            TiebaUtil.initAutoSign(this)
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching {
            ContextCompat.registerReceiver(
                this,
                newMessageReceiver,
                newIntentFilter(NotifyJobService.ACTION_NEW_MESSAGE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            startService(Intent(this, NotifyJobService::class.java))
            val builder = JobInfo.Builder(
                JobServiceUtil.getJobId(this),
                ComponentName(this, NotifyJobService::class.java)
            )
                .setPersisted(true)
                .setPeriodic(30 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            com.huanchengfly.tieba.post.ui.widgets.compose.SplashState.markSystemSplashDismissed()
            splashScreenViewProvider.view.animate()
                .alpha(0f)
                .setDuration(240L)
                .withEndAction {
                    splashScreenViewProvider.remove()
                }
                .start()
        }
        super.onCreate(savedInstanceState)
        StartupMetrics.mark("activity_created")
        window.decorView.setBackgroundColor(0)
        window.setBackgroundDrawable(ColorDrawable(0))
        launch {
            ClientUtils.setActiveTimestamp()
        }
        intent?.let {
            checkIntent(it)
            handleUpdateIntent(it)
        }
        launch {
            // 延迟到应用内启动屏结束后再请求，避免弹窗被遮挡/打断
            delay(2600)
            requestNotificationPermission()
        }
        launch {
            // 启动静默检查更新（内部等启动屏结束；开关关闭时不打扰）
            updateManager.autoCheckIfNeeded()
        }
    }

    override fun onCreateContent(systemUiController: SystemUIBarsTweaker) {
        super.onCreateContent(systemUiController)
        fetchAccount()
        initAutoSign()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && appPreferences.clearCacheOnExit) {
            // 退出时后台清理图片缓存（尽力而为，进程存活期间完成；issue #26）
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { ImageCacheUtil.clearImageAllCache(applicationContext) }
            }
        }
    }

    private fun openClipBoardLink(link: ClipBoardLink) {
        when (link) {
            is ClipBoardThreadLink -> {
                myNavController?.navigate(Uri.parse("tblite://thread/${link.threadId}"))
            }

            is ClipBoardForumLink -> {
                myNavController?.navigate(Uri.parse("tblite://forum/${link.forumName}"))
            }

            else -> {
//                launchUrl(this, link.url)
            }
        }
    }

    @Composable
    private fun ClipBoardDetectDialog() {
        val previewInfo by ClipBoardLinkDetector.previewInfoStateFlow.collectAsState()

        val dialogState = rememberDialogState()

        LaunchedEffect(previewInfo) {
            if (previewInfo != null) {
                dialogState.show()
            }
        }

        Dialog(
            dialogState = dialogState,
            title = {
                Text(text = stringResource(id = R.string.title_dialog_clip_board_tieba_url))
            },
            buttons = {
                DialogPositiveButton(text = stringResource(id = R.string.button_open)) {
                    previewInfo?.let {
                        openClipBoardLink(it.clipBoardLink)
                    }
                }
                DialogNegativeButton(text = stringResource(id = R.string.btn_close))
            },
        ) {
            previewInfo?.let {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    border = BorderStroke(1.dp, ExtendedTheme.colors.divider),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        it.icon?.let { icon ->
                            if (icon.type == QuickPreviewUtil.Icon.TYPE_DRAWABLE_RES) {
                                AvatarIcon(
                                    resId = icon.res,
                                    size = Sizes.Medium,
                                    contentDescription = null
                                )
                            } else {
                                Avatar(
                                    data = icon.url,
                                    size = Sizes.Medium,
                                    contentDescription = null
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            it.title?.let { title ->
                                Text(text = title, style = MaterialTheme.typography.subtitle1)
                            }
                            it.subtitle?.let { subtitle ->
                                Text(text = subtitle, style = MaterialTheme.typography.body2)
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val okSignAlertDialogState = rememberDialogState()
        ClipBoardDetectDialog()
        UpdateDialogHost(updateManager)
        AlertDialog(
            dialogState = okSignAlertDialogState,
            title = { Text(text = stringResource(id = R.string.title_dialog_oksign_battery_optimization)) },
            content = { Text(text = stringResource(id = R.string.message_dialog_oksign_battery_optimization)) },
            buttons = {
                DialogPositiveButton(
                    text = stringResource(id = R.string.button_go_to_ignore_battery_optimization),
                    onClick = {
                        requestIgnoreBatteryOptimizations()
                    }
                )
                DialogNegativeButton(
                    text = stringResource(id = R.string.button_cancel)
                )
                DialogNegativeButton(
                    text = stringResource(id = R.string.button_dont_remind_again),
                    onClick = {
                        appPreferences.ignoreBatteryOptimizationsDialog = true
                    }
                )
            },
        )
        LaunchedEffect(Unit) {
            if (appPreferences.autoSign && !isIgnoringBatteryOptimizations() && !appPreferences.ignoreBatteryOptimizationsDialog) {
                okSignAlertDialogState.show()
            }
            onGlobalEvent<GlobalEvent.StartSelectImages> {
                pickMediasLauncher.launch(
                    PickMediasRequest(it.id, it.maxCount, it.mediaType)
                )
            }
            onGlobalEvent<GlobalEvent.StartActivityForResult> {
                mLaunchActivityForResultLauncher.launch(
                    LaunchActivityRequest(
                        it.requesterId,
                        it.intent
                    )
                )
            }
        }
        TiebaLiteLocalProvider {
            TranslucentThemeBackground {
                val navController = rememberNavController()
                val engine = TiebaNavHostDefaults.rememberNavHostEngine()
                val navigator = TiebaNavHostDefaults.rememberBottomSheetNavigator()
                val currentDestination by navController.currentDestinationAsState()

                navController.navigatorProvider += navigator

                // 预测性返回开关:关闭时以高优先级回调接管返回,直接执行导航弹栈
                // (无手势预览),系统级 predictive back 预览由 manifest 的
                // enableOnBackInvokedCallback + navigation 库在开关开启时生效
                val predictiveBack by rememberPreferenceAsState(
                    key = booleanPreferencesKey("predictive_back"),
                    defaultValue = true
                )
                if (!predictiveBack) {
                    val backDispatcher = LocalOnBackPressedDispatcherOwner.current
                    val backCallback = remember(navController) {
                        object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                navController.navigateUp()
                            }
                        }
                    }
                    DisposableEffect(backDispatcher) {
                        backDispatcher?.onBackPressedDispatcher?.addCallback(
                            backDispatcher,
                            backCallback
                        )
                        onDispose { backCallback.remove() }
                    }
                }

                CompositionLocalProvider(
                    LocalNavController provides navController,
                    LocalDestination provides currentDestination,
                ) {
                    ModalBottomSheetLayout(
                        bottomSheetNavigator = navigator,
                        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        sheetBackgroundColor = ExtendedTheme.colors.windowBackground,
                        scrimColor = Color.Black.copy(alpha = 0.32f),
                    ) {
                        DestinationsNavHost(
                            navController = navController,
                            navGraph = NavGraphs.root,
                            engine = engine,
                        )
                    }
                }

                SideEffect {
                    myNavController = navController
                }
            }
        }

        AppSplashOverlay()
    }

    @Composable
    private fun TranslucentThemeBackground(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Surface(
            color = ExtendedTheme.colors.background,
            modifier = modifier
        ) {
            if (ThemeUtil.isTranslucentTheme(ExtendedTheme.colors.theme)) {
                val backgroundPath by rememberPreferenceAsMutableState(
                    key = stringPreferencesKey("translucent_theme_background_path"),
                    defaultValue = ""
                )
                val backgroundUri by remember { derivedStateOf { newFileUri(backgroundPath) } }
                AsyncImage(
                    imageUri = backgroundUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            content()
        }
    }

    @Composable
    fun TiebaLiteLocalProvider(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalNotificationCountFlow provides notificationCountFlow,
            LocalDevicePosture provides devicePostureFlow.collectAsState(),
            LocalMainTabRequest provides mainTabRequest,
        ) {
            content()
        }
    }

    private inner class NewMessageReceiver : BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == NotifyJobService.ACTION_NEW_MESSAGE) {
                val channel = intent.getStringExtra("channel")
                val count = intent.getIntExtra("count", 0)
                if (channel != null && channel == NotifyJobService.CHANNEL_TOTAL) {
                    lifecycleScope.launch {
                        notificationCountFlow.emit(count)
                    }
                }
            }
        }
    }
}

private object TiebaNavHostDefaults {
    // M3 Shared Axis X:短位移 + 淡化,固定时长,弃用全宽低刚度弹簧
    private val SlideSpec = tween<IntOffset>(300, easing = FastOutSlowInEasing)
    private val FadeInSpec = tween<Float>(240, delayMillis = 60, easing = LinearOutSlowInEasing)
    private val FadeOutSpec = tween<Float>(120, easing = FastOutLinearInEasing)

    @Composable
    @OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
    fun rememberNavHostEngine() = rememberAnimatedNavHostEngine(
        navHostContentAlignment = Alignment.TopStart,
        rootDefaultAnimations = RootNavGraphDefaultAnimations(
            enterTransition = {
                slideInHorizontally(SlideSpec) { it / 4 } + fadeIn(FadeInSpec)
            },
            exitTransition = {
                slideOutHorizontally(SlideSpec) { -it / 6 } + fadeOut(FadeOutSpec)
            },
            popEnterTransition = {
                slideInHorizontally(SlideSpec) { -it / 6 } + fadeIn(FadeInSpec)
            },
            popExitTransition = {
                slideOutHorizontally(SlideSpec) { it / 4 } + fadeOut(FadeOutSpec)
            },
        ),
    )

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun rememberBottomSheetNavigator(): BottomSheetNavigator = rememberBottomSheetNavigator(
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        skipHalfExpanded = true
    )
}