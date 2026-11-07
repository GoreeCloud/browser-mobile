package com.goreecloud.browsermobile

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.goreecloud.browsermobile.core.BrowsingLibrary
import com.goreecloud.browsermobile.core.SiteIcons
import com.goreecloud.browsermobile.core.WebspacesManager
import com.goreecloud.browsermobile.core.BrowserPreferences
import com.goreecloud.browsermobile.core.NavigationPolicy
import com.goreecloud.browsermobile.core.BrowserChromeScrollPolicy
import com.goreecloud.browsermobile.core.FilterListEngine
import com.goreecloud.browsermobile.core.FilterListStatus
import com.goreecloud.browsermobile.core.FilterSource
import com.goreecloud.browsermobile.core.VisitPolicy
import com.goreecloud.browsermobile.core.VisitedSite
import com.goreecloud.browsermobile.core.SiteExceptions
import com.goreecloud.browsermobile.core.SearchPolicy
import com.goreecloud.browsermobile.core.UrlCleaner
import com.goreecloud.browsermobile.ui.TabOverviewScreen
import com.goreecloud.browsermobile.ui.TabOverviewItem
import com.goreecloud.browsermobile.ui.TabOverviewColors
import com.goreecloud.browsermobile.ui.TabPreviewKind
import com.goreecloud.browsermobile.ui.TabPreviewCapture
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private enum class Surface { NEW_TAB, HOME, WEB }
private enum class Panel { TABS, PRIVACY, WEBSPACES, BOOKMARKS, HISTORY, DOWNLOADS }
private enum class DestinationMode { RECENT, FAVORITES, MOST }

private class BrowserTab {
    val id: String = UUID.randomUUID().toString()
    var surface by mutableStateOf(Surface.NEW_TAB)
    var url by mutableStateOf("")
    @Volatile var requestTopLevelUrl: String = ""
    var title by mutableStateOf("New Tab")
    // Volatile, bounded preview; never written to disk or synced.
    var thumbnail by mutableStateOf<Bitmap?>(null)
    var webspaceId by mutableStateOf("standard")
    var pinnedWebspace by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var progress by mutableIntStateOf(0)
    var blocked by mutableIntStateOf(0)
    var error by mutableStateOf<String?>(null)
    var webView: WebView? = null
}

private data class GcPalette(
    val canvas: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentOn: Color,
    val outline: Color,
    val success: Color
)

private val DarkGc = GcPalette(
    canvas = Color(0xFF0B1123), surface = Color(0xFF17213A),
    surfaceAlt = Color(0xFF202B47), text = Color(0xFFF5F6FF),
    muted = Color(0xFFAFBDD6), accent = Color(0xFFB3BAFF),
    accentOn = Color(0xFF182143), outline = Color(0xFF34405F),
    success = Color(0xFF8BE7CB)
)
private val LightGc = GcPalette(
    canvas = Color(0xFFF4F6FE), surface = Color.White,
    surfaceAlt = Color(0xFFE9EDFA), text = Color(0xFF19223E),
    muted = Color(0xFF5D6880), accent = Color(0xFF4D5ED6),
    accentOn = Color.White, outline = Color(0xFFD4DDF0),
    success = Color(0xFF077D63)
)

class MainActivity : ComponentActivity() {
    private var incomingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUrl = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString
        setContent {
            val preferences = remember { BrowserPreferences(applicationContext) }
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val palette = if (isDark) DarkGc else LightGc
            // Android 15+ draws behind the system bars. Match icon contrast to our canvas.
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
            MaterialTheme(colorScheme = if (isDark) darkColorScheme(
                primary = palette.accent, background = palette.canvas,
                surface = palette.surface, onSurface = palette.text
            ) else lightColorScheme(
                primary = palette.accent, background = palette.canvas,
                surface = palette.surface, onSurface = palette.text
            )) {
                BrowserApp(
                    activity = this,
                    palette = palette,
                    preferences = preferences,
                    incomingUrl = incomingUrl,
                    consumeIncomingUrl = { incomingUrl = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrl = intent.takeIf { it.action == Intent.ACTION_VIEW }?.dataString
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(
    activity: Activity,
    tab: BrowserTab,
    preferences: BrowserPreferences,
    filterLists: FilterListEngine,
    library: BrowsingLibrary,
    webspaces: WebspacesManager,
    icons: SiteIcons,
    onFaviconSaved: () -> Unit,
    onUrlCleaned: (Int) -> Unit,
    onPageNavigation: () -> Unit
): WebView {
    val webView = WebView(activity)
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        mediaPlaybackRequiresUserGesture = true
        safeBrowsingEnabled = true
        setGeolocationEnabled(false)
    }
    webView.isSaveEnabled = true
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, preferences.thirdPartyCookies)
    val mainHandler = Handler(Looper.getMainLooper())

    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val action = NavigationPolicy.navigationAction(
                request.url.toString(), request.method, preferences.cleanUrls
            )
            if (action.removedParameters > 0) onUrlCleaned(action.removedParameters)
            action.destination?.let(view::loadUrl)
            return action.intercept
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            if (preferences.blockAdsForRequests && !request.isForMainFrame &&
                !preferences.isSiteExempt(tab.requestTopLevelUrl) &&
                filterLists.shouldBlock(request.url.toString())) {
                mainHandler.post { tab.blocked++ }
                return WebResourceResponse(
                    "text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0))
                )
            }
            return null
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            onPageNavigation()
            tab.thumbnail = null
            tab.loading = true
            tab.error = null
            if (url != null && NavigationPolicy.isHttpUrl(url)) {
                tab.url = url
                tab.requestTopLevelUrl = url
                if (!tab.pinnedWebspace) tab.webspaceId = webspaces.destination(url)
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            tab.loading = false
            if (!url.isNullOrBlank() && NavigationPolicy.isHttpUrl(url)) {
                tab.url = url
                tab.requestTopLevelUrl = url
            }
            tab.title = view.title?.takeIf { it.isNotBlank() } ?: "Website"
            if (!url.isNullOrBlank() && url.startsWith("https://", true)) {
                library.record(url)
            }
        }

        override fun onReceivedError(
            view: WebView, request: WebResourceRequest, error: WebResourceError
        ) {
            if (request.isForMainFrame) {
                tab.loading = false
                tab.error = error.description?.toString() ?: "This page could not load"
            }
        }

        @Suppress("DEPRECATION")
        override fun onReceivedSslError(
            view: WebView, handler: SslErrorHandler, error: SslError
        ) {
            handler.cancel()
            tab.loading = false
            tab.error = "Secure connection failed. This page was blocked."
        }
    }
    webView.webChromeClient = object : WebChromeClient() {
        override fun onReceivedIcon(view: WebView, icon: Bitmap) {
            val url = view.url ?: return
            if (library.historyEnabled && icons.save(url, icon)) onFaviconSaved()
        }
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            tab.progress = newProgress
        }
        override fun onReceivedTitle(view: WebView, title: String?) {
            if (!title.isNullOrBlank()) tab.title = title
        }
        override fun onPermissionRequest(request: PermissionRequest) {
            request.deny() // Secure-by-default: no unsolicited camera, microphone, or other grants.
        }
    }
    tab.webView = webView
    if (tab.url.isNotBlank()) webView.loadUrl(tab.url)
    return webView
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserApp(
    activity: Activity,
    palette: GcPalette,
    preferences: BrowserPreferences,
    incomingUrl: String?,
    consumeIncomingUrl: () -> Unit
) {
    val filterLists = remember { FilterListEngine(activity.applicationContext, preferences.blockAds) }
    DisposableEffect(filterLists) { onDispose { filterLists.close() } }
    val library = remember { BrowsingLibrary(activity.applicationContext) }
    val icons = remember { SiteIcons(activity.applicationContext) }
    val webspaces = remember { WebspacesManager(activity.applicationContext) }
    var faviconRevision by remember { mutableIntStateOf(0) }
    val tabs = remember { mutableStateListOf(BrowserTab()) }
    var activeId by remember { mutableStateOf(tabs.first().id) }
    val current = tabs.firstOrNull { it.id == activeId } ?: tabs.first()
    var address by remember { mutableStateOf("") }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var cleanedCount by remember { mutableIntStateOf(0) }
    val focus = LocalFocusManager.current
    val scrollPolicy = remember { BrowserChromeScrollPolicy() }
    var chromeVisible by remember { mutableStateOf(true) }
    var addressFocused by remember { mutableStateOf(false) }
    val showChrome = current.surface != Surface.WEB || chromeVisible || addressFocused

    fun revealChrome() {
        chromeVisible = true
        scrollPolicy.reset()
    }

    fun captureCurrentPreview() {
        if (current.surface == Surface.WEB) {
            TabPreviewCapture.capture(current.webView)?.let { current.thumbnail = it }
        }
    }

    fun openOverview() {
        revealChrome()
        captureCurrentPreview()
        focus.clearFocus()
        panel = Panel.TABS
    }

    fun openUrl(input: String) {
        val resolved = NavigationPolicy.resolve(input, preferences.cleanUrls)
        if (resolved.isBlank()) return
        revealChrome()
        current.error = null
        current.thumbnail = null
        current.url = resolved
        if (!current.pinnedWebspace) current.webspaceId = webspaces.destination(resolved)
        current.requestTopLevelUrl = resolved
        current.surface = Surface.WEB
        current.webView?.loadUrl(resolved)
        focus.clearFocus()
    }

    fun openNewTab(webspace: String? = null) {
        if (tabs.size >= 12) {
            openOverview()
            return
        }
        captureCurrentPreview()
        val tab = BrowserTab().apply {
            webspaceId = webspace ?: current.webspaceId
            pinnedWebspace = webspace != null
        }
        tabs.add(tab)
        activeId = tab.id
        panel = null
    }

    fun openWebspace(id: String) {
        val found = tabs.firstOrNull { it.webspaceId == id }
        if (found != null) {
            captureCurrentPreview()
            activeId = found.id
            panel = null
        } else openNewTab(id)
    }

    fun closeTab(tab: BrowserTab) {
        if (tabs.size == 1) {
            tab.thumbnail = null
            tab.webView?.destroy()
            tab.webView = null
            tab.url = ""
            tab.pinnedWebspace = false
            tab.requestTopLevelUrl = ""
            tab.title = "New Tab"
            tab.surface = Surface.NEW_TAB
        } else {
            val index = tabs.indexOf(tab)
            tab.thumbnail = null
            tabs.remove(tab)
            if (activeId == tab.id) activeId = tabs[(index - 1).coerceAtLeast(0)].id
            tab.webView?.stopLoading()
            tab.webView?.destroy()
            tab.webView = null
        }
    }

    LaunchedEffect(current.id, current.url, current.surface) {
        address = if (current.surface == Surface.WEB) current.url else ""
    }
    LaunchedEffect(current.id, current.surface) {
        revealChrome()
    }
    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank()) {
            openUrl(incomingUrl)
            consumeIncomingUrl()
        }
    }

    BackHandler(enabled = panel != null || current.surface == Surface.WEB) {
        when {
            panel != null -> panel = null
            current.surface == Surface.WEB && current.webView?.canGoBack() == true -> {
                revealChrome()
                current.webView?.goBack()
            }
            else -> {
                captureCurrentPreview()
                current.surface = Surface.HOME
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(palette.canvas)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chrome occupies no vertical space once a webpage scrolls downward.
            // Built-in Home/New Tab always show the address bar.
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                BrowserAddressBar(
                    palette = palette,
                    urlIsSecure = current.surface == Surface.WEB &&
                        current.url.startsWith("https://", ignoreCase = true),
                    address = address,
                    onAddressChange = { address = it },
                    onSubmit = { openUrl(address) },
                    onFocus = { focused ->
                        addressFocused = focused
                        if (focused) revealChrome()
                    }
                )
            }
            if (showChrome && current.surface == Surface.WEB && current.loading) {
                LinearProgressIndicator(
                    progress = { current.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = palette.accent,
                    trackColor = palette.surfaceAlt
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().then(
                    // The webpage reaches behind the floating dock. Internal pages
                    // retain clearance so their controls are never obscured.
                    if (current.surface == Surface.WEB) Modifier
                    else Modifier.padding(bottom = 82.dp)
                )
            ) {
                when (current.surface) {
                    Surface.NEW_TAB -> NewTabPage(
                        palette = palette, openUrl = ::openUrl,
                        library = library, icons = icons, faviconRevision = faviconRevision,
                        onBookmarks = { panel = Panel.BOOKMARKS },
                        onHistory = { panel = Panel.HISTORY },
                        onDownloads = { panel = Panel.DOWNLOADS },
                        onSettings = { panel = Panel.PRIVACY }
                    )
                    Surface.HOME -> HomePage(
                        palette = palette, preferences = preferences,
                        library = library, icons = icons, faviconRevision = faviconRevision,
                        blocked = tabs.sumOf { it.blocked },
                        cleaned = cleanedCount,
                        openUrl = ::openUrl,
                        onPrivacy = { panel = Panel.PRIVACY }
                    )
                    Surface.WEB -> {
                        key(current.id) {
                            AndroidView(
                                factory = { context ->
                                    val view = current.webView
                                        ?: configureWebView(
                                            activity, current, preferences, filterLists,
                                            library, webspaces, icons,
                                            onFaviconSaved = { faviconRevision++ },
                                            onUrlCleaned = { cleanedCount += it },
                                            onPageNavigation = {
                                                if (activeId == current.id) revealChrome()
                                            }
                                        )
                                    (view.parent as? ViewGroup)?.removeView(view)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(
                                        view, preferences.thirdPartyCookies
                                    )
                                    view
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { view ->
                                    if (current.url.isNotBlank() && view.url == null) {
                                        view.loadUrl(current.url)
                                    }
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(
                                        view, preferences.thirdPartyCookies
                                    )
                                    // Native scroll events: no injection into website JavaScript.
                                    view.setOnScrollChangeListener { _, _, newY, _, oldY ->
                                        if (activeId == current.id &&
                                            current.surface == Surface.WEB &&
                                            panel == null && !addressFocused
                                        ) {
                                            chromeVisible = scrollPolicy.visibleAfterScroll(
                                                newY, oldY, chromeVisible
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        current.error?.let { message ->
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Lock, null, tint = palette.accent,
                                    modifier = Modifier.size(34.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Page unavailable", color = palette.text,
                                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text(message, color = palette.muted, fontSize = 14.sp)
                                Spacer(Modifier.height(18.dp))
                                Button(onClick = { current.webView?.reload() }) { Text("Retry") }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showChrome,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .shadow(14.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(palette.surface.copy(alpha = 0.93f), RoundedCornerShape(32.dp))
                .border(1.dp, palette.outline.copy(alpha = 0.72f), RoundedCornerShape(32.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockButton(Icons.Default.ArrowBack, "Back", palette,
                enabled = current.surface == Surface.WEB && current.webView?.canGoBack() == true) {
                current.webView?.goBack()
            }
            DockButton(Icons.Default.ArrowForward, "Forward", palette,
                enabled = current.surface == Surface.WEB && current.webView?.canGoForward() == true) {
                current.webView?.goForward()
            }
            DockButton(Icons.Default.Home, "Home", palette) {
                captureCurrentPreview()
                current.surface = Surface.HOME
            }
            DockButton(Icons.Default.Add, "New tab", palette) {
                openNewTab()
            }
            TabCountDock(tabs.size, palette) { openOverview() }
            BrowserOverflowMenu(
                tab = current,
                preferences = preferences,
                palette = palette,
                activity = activity,
                onPrivacy = { panel = Panel.PRIVACY },
                onWebspaces = { panel = Panel.WEBSPACES },
                onHome = {
                    captureCurrentPreview()
                    current.surface = Surface.HOME
                }
            )
        }
        }
        if (panel == Panel.TABS) {
            TabOverviewScreen(
                tabs = tabs.map { tab ->
                    TabOverviewItem(
                        id = tab.id,
                        title = when (tab.surface) {
                            Surface.NEW_TAB -> "New Tab"
                            Surface.HOME -> "Home"
                            Surface.WEB -> tab.title
                        },
                        address = when (tab.surface) {
                            Surface.NEW_TAB -> "New Tab"
                            Surface.HOME -> "goree://home"
                            Surface.WEB -> Uri.parse(tab.url).host ?: "Website"
                        },
                        webspace = webspaces.entries.firstOrNull { it.id == tab.webspaceId }?.name
                            ?: "Standard",
                        kind = when (tab.surface) {
                            Surface.NEW_TAB -> TabPreviewKind.NEW_TAB
                            Surface.HOME -> TabPreviewKind.HOME
                            Surface.WEB -> TabPreviewKind.WEB
                        },
                        selected = tab.id == activeId,
                        thumbnail = if (tab.surface == Surface.WEB) tab.thumbnail else null
                    )
                },
                colors = TabOverviewColors(
                    canvas = palette.canvas, surface = palette.surface,
                    surfaceAlt = palette.surfaceAlt, text = palette.text,
                    muted = palette.muted, accent = palette.accent,
                    outline = palette.outline
                ),
                onSelect = { id ->
                    if (id != activeId) captureCurrentPreview()
                    activeId = id
                    panel = null
                },
                onClose = { id -> tabs.firstOrNull { it.id == id }?.let(::closeTab) },
                onNewTab = { openNewTab() },
                onDismiss = { panel = null }
            )
        }
    }

    if (panel != null && panel != Panel.TABS) {
        ModalBottomSheet(
            onDismissRequest = { panel = null },
            containerColor = palette.surface, contentColor = palette.text
        ) {
            when (panel) {
                Panel.TABS -> Unit
                Panel.BOOKMARKS -> BookmarksPanel(
                    preferences = preferences, palette = palette,
                    openUrl = { url -> panel = null; openUrl(url) }
                )
                Panel.HISTORY -> RecentSitesPanel(
                    library = library, palette = palette,
                    openUrl = { url -> panel = null; openUrl(url) }
                )
                Panel.DOWNLOADS -> DeviceDownloadsPanel(activity, palette)
                Panel.PRIVACY -> PrivacyPanel(
                    activity, current, preferences, filterLists, palette, cleanedCount, library, icons
                )
                Panel.WEBSPACES -> WebspacesPanel(
                    webspaces = webspaces, tabs = tabs, current = current, palette = palette,
                    open = ::openWebspace,
                    onRemove = { id ->
                        tabs.filter { it.webspaceId == id }.forEach { tab ->
                            tab.webspaceId = "standard"
                            tab.pinnedWebspace = false
                        }
                        webspaces.remove(id)
                    }
                )
                null -> Unit
            }
        }
    }
}

@Composable
private fun BrowserAddressBar(
    palette: GcPalette,
    urlIsSecure: Boolean,
    address: String,
    onAddressChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocus: (Boolean) -> Unit
) {
    val pill = RoundedCornerShape(29.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .shadow(11.dp, pill)
            .clip(pill)
            .background(palette.surface.copy(alpha = 0.97f))
            .border(1.dp, palette.outline.copy(alpha = 0.76f), pill)
            .heightIn(min = 62.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.padding(start = 8.dp).size(44.dp)
                .background(palette.surfaceAlt.copy(alpha = 0.82f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (urlIsSecure) Icons.Default.Lock else Icons.Default.Search,
                contentDescription = if (urlIsSecure) "Secure HTTPS connection" else "Search",
                tint = palette.accent, modifier = Modifier.size(21.dp)
            )
        }
        TextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.weight(1f).onFocusChanged { onFocus(it.isFocused) },
            singleLine = true,
            placeholder = {
                Text("Search or enter address", fontSize = 14.sp, color = palette.muted,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = palette.text),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
                cursorColor = palette.accent
            )
        )
        Box(
            modifier = Modifier.padding(end = 6.dp).size(49.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF355FD8), palette.accent, Color(0xFF873BE0))
                    )
                )
                .clickable(onClickLabel = "Go to address", onClick = onSubmit),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun DockButton(
    icon: ImageVector, label: String, palette: GcPalette,
    enabled: Boolean = true, action: () -> Unit
) {
    IconButton(onClick = action, enabled = enabled, modifier = Modifier.size(49.dp)) {
        Icon(
            imageVector = icon, contentDescription = label,
            modifier = Modifier.size(23.dp),
            tint = if (enabled) palette.text else palette.muted.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun NewTabPage(
    palette: GcPalette,
    library: BrowsingLibrary,
    icons: SiteIcons,
    faviconRevision: Int,
    openUrl: (String) -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    val recent = if (library.historyEnabled)
        library.sites.sortedByDescending { it.lastVisitedMs }.take(2)
    else emptyList()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(154.dp)) {
            NewTabAmbient(palette)
            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(palette.success, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("A FRESH START", color = palette.muted, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp)
                }
                Spacer(Modifier.height(15.dp))
                Text("Where to next?", color = palette.text,
                    fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.75).sp)
                Spacer(Modifier.height(9.dp))
                Text("Your next discovery starts here.",
                    fontSize = 13.sp, lineHeight = 19.sp, color = palette.muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = DarkGc.surface,
            shadowElevation = 7.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(
                        Color(0xFF263965), Color(0xFF182641), Color(0xFF14243B))))
                    .heightIn(min = 93.dp)
                    .padding(horizontal = 17.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(47.dp)
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Language, contentDescription = null,
                        tint = Color(0xFF87E9DE), modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Explore the web", color = Color.White,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text("Search with Startpage", fontSize = 11.sp,
                        color = Color(0xFFD0DBF1), maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { openUrl(SearchPolicy.HOME_URL) },
                    shape = RoundedCornerShape(15.dp),
                    contentPadding = PaddingValues(horizontal = 13.dp),
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Color(0xFF1B2947))
                ) {
                    Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null,
                        modifier = Modifier.size(17.dp))
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("YOUR SHORTCUTS", color = palette.muted, fontSize = 11.sp,
            letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth < 340.dp || LocalDensity.current.fontScale >= 1.2f) 2 else 4
            val tiles = listOf(
                Triple("Bookmarks", Icons.Default.Bookmark, onBookmarks),
                Triple("History", Icons.Default.History, onHistory),
                Triple("Downloads", Icons.Default.Download, onDownloads),
                Triple("Settings", Icons.Default.Settings, onSettings)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(columns).forEach { segment ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        segment.forEach { (label, icon, action) ->
                            val tint = when (label) {
                                "History" -> palette.success
                                "Settings" -> palette.muted
                                else -> palette.accent
                            }
                            QuickAccessTile(label, icon, palette,
                                tint = tint, modifier = Modifier.weight(1f), action = action)
                        }
                        repeat(columns - segment.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(30.dp))
            Text("PICK UP WHERE YOU LEFT OFF", color = palette.muted,
                fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val vertical = maxWidth < 340.dp || LocalDensity.current.fontScale >= 1.2f
                if (vertical) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        recent.forEach { site ->
                            RecentNewTabSite(site, icons, faviconRevision, palette,
                                Modifier.fillMaxWidth()) { openUrl(site.url) }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        recent.forEach { site ->
                            RecentNewTabSite(site, icons, faviconRevision, palette,
                                Modifier.weight(1f)) { openUrl(site.url) }
                        }
                        if (recent.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun NewTabAmbient(palette: GcPalette) {
    val dark = palette.canvas == DarkGc.canvas
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.92f, h * 0.48f)
        val radius = h * 0.76f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    palette.accent.copy(alpha = if (dark) 0.14f else 0.12f),
                    palette.success.copy(alpha = if (dark) 0.08f else 0.06f),
                    Color.Transparent
                ),
                center = center, radius = radius
            ),
            radius = radius, center = center
        )
        val orbitRadius = h * 0.42f
        drawCircle(
            color = palette.accent.copy(alpha = if (dark) 0.18f else 0.13f),
            radius = orbitRadius, center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = palette.success.copy(alpha = if (dark) 0.23f else 0.18f),
            radius = orbitRadius * 0.66f,
            center = Offset(center.x + orbitRadius * 0.12f, center.y),
            style = Stroke(width = 1.5.dp.toPx())
        )
        val arc = Path().apply {
            moveTo(w * 0.71f, h * 0.84f)
            cubicTo(w * 0.82f, h * 0.90f, w * 0.89f, h * 0.74f, w, h * 0.77f)
        }
        drawPath(
            path = arc,
            color = palette.success.copy(alpha = if (dark) 0.28f else 0.17f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun QuickAccessTile(
    title: String,
    icon: ImageVector,
    palette: GcPalette,
    tint: Color,
    modifier: Modifier = Modifier,
    action: () -> Unit
) {
    Surface(
        onClick = action,
        modifier = modifier.heightIn(min = 104.dp),
        color = palette.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.8.dp, palette.outline.copy(alpha = 0.66f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(39.dp)
                    .background(tint.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint,
                    modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(11.dp))
            Text(title, color = palette.text, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RecentNewTabSite(
    site: VisitedSite,
    icons: SiteIcons,
    faviconRevision: Int,
    palette: GcPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val favicon = remember(site.host, faviconRevision) { icons.load(site.host) }
    Surface(
        onClick = onClick, modifier = modifier,
        color = palette.surfaceAlt.copy(alpha = 0.65f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.7.dp, palette.outline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.heightIn(min = 58.dp)
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(31.dp)
                    .background(palette.surface, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (favicon != null) {
                    Image(bitmap = favicon.asImageBitmap(), contentDescription = null,
                        modifier = Modifier.size(23.dp))
                } else {
                    Icon(Icons.Default.Language, contentDescription = null,
                        tint = palette.accent, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(9.dp))
            Text(site.host, color = palette.text,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForward, contentDescription = null,
                tint = palette.muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BookmarksPanel(
    preferences: BrowserPreferences,
    palette: GcPalette,
    openUrl: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeading("Bookmarks", "Your saved websites · stored only on this device", palette)
        if (preferences.savedSites.isEmpty()) {
            Text("Save a website from the three-dot menu to find it here.",
                color = palette.muted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 23.dp, vertical = 24.dp))
        } else {
            preferences.savedSites.forEach { url ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(palette.surfaceAlt),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bookmark, null, tint = palette.accent,
                        modifier = Modifier.padding(start = 12.dp).size(21.dp))
                    Column(
                        modifier = Modifier.weight(1f).clickable { openUrl(url) }
                            .padding(horizontal = 13.dp, vertical = 14.dp)
                    ) {
                        Text(VisitPolicy.host(url) ?: "Website", color = palette.text,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(url, color = palette.muted, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { preferences.toggleBookmark(url) }) {
                        Icon(Icons.Default.Close, "Remove bookmark", tint = palette.muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun RecentSitesPanel(
    library: BrowsingLibrary,
    palette: GcPalette,
    openUrl: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeading("Recently visited websites", "Host-only local history · no page URLs", palette)
        when {
            !library.historyEnabled -> Text(
                "Website history is disabled in Privacy settings.",
                color = palette.muted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 19.dp))
            library.sites.isEmpty() -> Text(
                "Recently visited websites will appear here.",
                color = palette.muted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 19.dp))
            else -> {
                library.sites.sortedByDescending { it.lastVisitedMs }.forEach { site ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(palette.surfaceAlt)
                            .clickable { openUrl(site.url) }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Language, null, tint = palette.accent,
                            modifier = Modifier.size(23.dp))
                        Spacer(Modifier.width(13.dp))
                        Text(site.host, color = palette.text, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                        Text(if (site.visits == 1) "1 visit"
                             else site.visits.toString() + " visits",
                            color = palette.muted, fontSize = 11.sp)
                    }
                }
                TextButton(
                    onClick = library::clear,
                    modifier = Modifier.padding(start = 18.dp, top = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear website history")
                }
            }
        }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun DeviceDownloadsPanel(activity: Activity, palette: GcPalette) {
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(bottom = 25.dp)) {
        SheetHeading("Downloads", "View files in Android's download manager", palette)
        Text(
            "Browser Mobile does not yet manage its own downloaded files. " +
                "Use Android Downloads to see files saved by your device.",
            color = palette.muted, fontSize = 13.sp, lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        )
        Button(
            onClick = {
                try {
                    activity.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                    error = null
                } catch (_: Exception) {
                    error = "Android Downloads is unavailable on this device."
                }
            },
            modifier = Modifier.padding(horizontal = 22.dp)
        ) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open device downloads")
        }
        error?.let {
            Text(it, color = palette.muted, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))
        }
    }
}


@Composable
private fun HomePage(
    palette: GcPalette,
    preferences: BrowserPreferences,
    library: BrowsingLibrary,
    icons: SiteIcons,
    faviconRevision: Int,
    blocked: Int,
    cleaned: Int,
    openUrl: (String) -> Unit,
    onPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 19.dp, vertical = 12.dp)
    ) {
        val today = remember {
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        }
        Text(today.uppercase(), color = palette.muted, letterSpacing = 1.8.sp, fontSize = 10.sp)
        Spacer(Modifier.height(16.dp))
        QuickDestinations(library, preferences, icons, faviconRevision, palette, openUrl)
        Spacer(Modifier.height(21.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Privacy at a glance", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = palette.text, modifier = Modifier.weight(1f))
            TextButton(onClick = onPrivacy) { Text("Manage", color = palette.accent) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrivacyStat(
                modifier = Modifier.weight(1f),
                count = blocked.toString(),
                name = "Requests blocked",
                icon = Icons.Default.Shield,
                palette = palette
            )
            PrivacyStat(
                modifier = Modifier.weight(1f),
                count = cleaned.toString(),
                name = "URL fields cleaned",
                icon = Icons.Default.Lock,
                palette = palette
            )
        }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun PrivacyStat(
    modifier: Modifier,
    count: String,
    name: String,
    icon: ImageVector,
    palette: GcPalette
) {
    Column(
        modifier = modifier.background(palette.surface, RoundedCornerShape(22.dp))
            .border(1.dp, palette.outline, RoundedCornerShape(22.dp))
            .padding(15.dp)
    ) {
        Icon(icon, null, tint = palette.success, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(9.dp))
        Text(count, color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(name, color = palette.muted, fontSize = 11.sp)
    }
}

@Composable
private fun SheetHeading(title: String, subtitle: String, palette: GcPalette) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = palette.text)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, fontSize = 12.sp, color = palette.muted)
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector, label: String, palette: GcPalette, action: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = action)
            .padding(horizontal = 24.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(21.dp), tint = palette.accent)
        Spacer(Modifier.width(17.dp))
        Text(label, fontSize = 15.sp, color = palette.text)
    }
}

@Composable
private fun PrivacyToggle(
    title: String,
    subtitle: String,
    enabled: Boolean,
    palette: GcPalette,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = palette.muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = enabled, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = palette.accent,
                checkedThumbColor = palette.accentOn
            )
        )
    }
}

@Composable
private fun PrivacyPanel(
    activity: Activity,
    tab: BrowserTab,
    preferences: BrowserPreferences,
    filterLists: FilterListEngine,
    palette: GcPalette,
    cleanedCount: Int,
    library: BrowsingLibrary,
    icons: SiteIcons
) {
    val message = remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeading("Privacy center", "Visible controls, no hidden promises", palette)
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp)
                .fillMaxWidth().background(palette.surfaceAlt, RoundedCornerShape(20.dp))
                .padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${tab.blocked}", color = palette.text, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
                Text("Blocked on this tab", color = palette.muted, fontSize = 11.sp)
            }
            Column {
                Text("$cleanedCount", color = palette.text, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
                Text("Tracking fields removed", color = palette.muted, fontSize = 11.sp)
            }
        }
        PrivacyToggle(
            "Built-in ad & tracker blocking",
            "OISD Small and StevenBlack Unified are enabled by default.",
            preferences.blockAds, palette, { enabled ->
                preferences.updateBlockAds(enabled)
                if (enabled) filterLists.refresh()
            }
        )
        if (tab.surface == Surface.WEB) {
            val siteHost = SiteExceptions.hostForPage(tab.url)
            if (siteHost != null) {
                val exempted = siteHost in preferences.siteExceptionHosts
                PrivacyToggle(
                    "Allow ads on this site",
                    (if (exempted) "Filtering paused for " else "Block ads on ") + siteHost +
                        " · reloads this tab when changed",
                    exempted, palette,
                    { allow ->
                        preferences.setSiteExempt(tab.url, allow)
                        tab.webView?.reload()
                    }
                )
            }
        }
        PrivacyToggle(
            "URL cleaner",
            "Removes recognized marketing parameters from links.",
            preferences.cleanUrls, palette, preferences::updateCleanUrls
        )
        PrivacyToggle(
            "Remember visited websites",
            "Keeps up to 100 website hosts locally for Recent and Most Visited; no URLs or search terms.",
            library.historyEnabled, palette, { enabled ->
                library.updateHistoryEnabled(enabled)
                if (!enabled) {
                    library.clear()
                    icons.clear()
                    message.value = "Local website history and favicons cleared"
                }
            }
        )
        SheetAction(Icons.Default.Delete, "Clear recent websites and favicons", palette) {
            library.clear()
            icons.clear()
            message.value = "Local website history and saved favicons cleared"
        }
        HorizontalDivider(color = palette.outline, modifier = Modifier.padding(horizontal = 20.dp))
        Text("FILTER SUBSCRIPTIONS", color = palette.muted, fontSize = 10.sp,
            letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 23.dp, top = 17.dp))
        Text(
            "OISD Small + StevenBlack Unified are the default pair. Other lists are opt-in. " +
                "Downloaded rules stay on this device and apply to page subresources only.",
            color = palette.muted, fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 10.dp)
        )
        FilterSource.entries.forEach { source ->
            val state = filterLists.states[source] ?: FilterListStatus(source.enabledByDefault)
            val readiness = when {
                !state.enabled -> "Off"
                state.updating && !state.hasCache -> "Downloading…"
                state.updateFailed && state.hasCache -> "Using saved list · update failed"
                state.updateFailed -> "Unavailable · retry download"
                state.hasCache -> state.domains.toString() + " domains loaded"
                else -> "Waiting for first download"
            }
            PrivacyToggle(
                source.title,
                source.summary + " · " + readiness,
                state.enabled, palette,
                { enabled -> filterLists.setEnabled(source, enabled) }
            )
        }
        Text(
            filterLists.activeRuleCount.toString() + " active domains · " +
                preferences.siteExceptionHosts.size + " site exceptions. Some ads cannot be blocked.",
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 8.dp),
            color = palette.muted, fontSize = 12.sp
        )
        SheetAction(Icons.Default.Refresh, "Update enabled lists now", palette) {
            filterLists.refresh(force = true)
        }
        HorizontalDivider(color = palette.outline, modifier = Modifier.padding(horizontal = 20.dp))
        Text("COOKIES & SITE DATA", color = palette.muted, fontSize = 10.sp,
            letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 23.dp, top = 17.dp))
        PrivacyToggle(
            "Accept cookies",
            "WebView cookie storage for sites you visit.",
            preferences.acceptCookies, palette, preferences::updateAcceptCookies
        )
        PrivacyToggle(
            "Third-party cookies",
            "Off by default; can break some sign-ins.",
            preferences.thirdPartyCookies, palette, preferences::updateThirdPartyCookies
        )
        SheetAction(Icons.Default.Delete, "Clear all cookies and web storage", palette) {
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                message.value = "Cookies and web storage cleared"
            }
        }
        HorizontalDivider(color = palette.outline, modifier = Modifier.padding(horizontal = 20.dp))
        Text("DNS MANAGER", color = palette.muted, fontSize = 10.sp,
            letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 23.dp, top = 17.dp))
        Text(
            "Android WebView uses system DNS. Configure Private DNS in Android settings; " +
                "per-browser DNS-over-HTTPS is not available in this version.",
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 10.dp),
            color = palette.muted, fontSize = 12.sp, lineHeight = 18.sp
        )
        SheetAction(Icons.Default.Settings, "Open Android Private DNS settings", palette) {
            try {
                activity.startActivity(Intent("android.settings.PRIVATE_DNS_SETTINGS"))
            } catch (_: Exception) {
                activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
        }
        if (message.value.isNotBlank()) {
            Text(message.value, modifier = Modifier.padding(23.dp), color = palette.success,
                fontSize = 12.sp)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun QuickDestinations(
    library: BrowsingLibrary, preferences: BrowserPreferences, icons: SiteIcons,
    faviconRevision: Int, palette: GcPalette, openUrl: (String) -> Unit
) {
    var mode by remember { mutableStateOf(DestinationMode.RECENT) }
    val favorites = preferences.savedSites.mapNotNull { url ->
        val host = VisitPolicy.host(url) ?: return@mapNotNull null
        val entry = library.sites.firstOrNull { it.host == host }
        VisitedSite(host, entry?.visits ?: 0, entry?.lastVisitedMs ?: 0)
    }.distinctBy { it.host }.take(8)
    val sites = when (mode) {
        DestinationMode.RECENT -> library.sites.sortedByDescending { it.lastVisitedMs }.take(8)
        DestinationMode.FAVORITES -> favorites
        DestinationMode.MOST -> library.sites.sortedWith(
            compareByDescending<VisitedSite> { it.visits }.thenByDescending { it.lastVisitedMs }
        ).take(8)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Quick destinations", fontSize = 21.sp,
            fontWeight = FontWeight.Bold, color = palette.text,
            modifier = Modifier.weight(1f))
        Text("ON THIS DEVICE", color = palette.muted,
            fontSize = 9.sp, letterSpacing = 1.1.sp)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(DestinationMode.RECENT to "Recent",
            DestinationMode.FAVORITES to "Favorites",
            DestinationMode.MOST to "Most visited").forEach { (option, label) ->
            FilterChip(
                selected = mode == option, onClick = { mode = option },
                label = { Text(label, fontSize = 11.sp) },
                shape = RoundedCornerShape(13.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = palette.surface,
                    labelColor = palette.muted,
                    selectedContainerColor = palette.surfaceAlt,
                    selectedLabelColor = palette.accent
                ),
                border = BorderStroke(1.dp, palette.outline)
            )
        }
    }
    Spacer(Modifier.height(9.dp))
    if (sites.isEmpty()) {
        Text(
            if (mode == DestinationMode.FAVORITES)
                "Bookmark a website to add a favorite destination."
            else "Websites you visit will appear here. Only website hosts are saved on this device.",
            color = palette.muted, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 17.dp)
        )
    }
    sites.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            pair.forEach { site ->
                val favicon = remember(site.host, faviconRevision) { icons.load(site.host) }
                Column(
                    modifier = Modifier.weight(1f)
                        .background(palette.surface, RoundedCornerShape(19.dp))
                        .border(1.dp, palette.outline, RoundedCornerShape(19.dp))
                        .clickable { openUrl(site.url) }
                        .padding(14.dp)
                ) {
                    if (favicon != null) {
                        Image(bitmap = favicon.asImageBitmap(),
                            contentDescription = site.host + " favicon",
                            modifier = Modifier.size(31.dp))
                    } else {
                        Icon(Icons.Default.Language, contentDescription = "Website",
                            tint = palette.accent, modifier = Modifier.size(31.dp))
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(site.host, color = palette.text, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(if (site.visits == 1) "1 visit"
                         else if (site.visits > 1) site.visits.toString() + " visits"
                         else "Bookmarked",
                        color = palette.muted, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabCountDock(count: Int, palette: GcPalette, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(47.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Tab, contentDescription = "Tabs: $count",
                modifier = Modifier.size(28.dp), tint = palette.text)
            Text(count.toString(), fontSize = if (count >= 10) 10.sp else 12.sp,
                fontWeight = FontWeight.ExtraBold, color = palette.accent,
                modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun WebspacesPanel(
    webspaces: WebspacesManager, tabs: List<BrowserTab>,
    current: BrowserTab, palette: GcPalette,
    open: (String) -> Unit, onRemove: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeading("Webspaces", "Organize tabs and route sites", palette)
        Text("Tab groups currently share WebView cookies; accounts are not isolated.",
            color = palette.muted, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 8.dp))
        webspaces.entries.forEach { space ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 4.dp)
                    .background(palette.surfaceAlt, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Language, null, tint = palette.accent)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { open(space.id) }
                    .padding(vertical = 10.dp)) {
                    Text(space.name, color = palette.text, fontWeight = FontWeight.SemiBold)
                    val count = tabs.count { it.webspaceId == space.id }
                    Text(count.toString() + if (count == 1) " tab · Open" else " tabs · Open",
                        color = palette.muted, fontSize = 11.sp)
                }
                if (current.surface == Surface.WEB) {
                    IconButton(onClick = {
                        current.webspaceId = space.id
                        current.pinnedWebspace = true
                    }) {
                        Icon(Icons.Default.ArrowForward,
                            "Move current tab to " + space.name, tint = palette.accent)
                    }
                    TextButton(onClick = {
                        webspaces.assign(current.url, space.id)
                        current.webspaceId = space.id
                        current.pinnedWebspace = false
                    }) { Text("Route", fontSize = 11.sp) }
                }
                if (space.id.startsWith("custom-")) {
                    IconButton(onClick = { onRemove(space.id) }) {
                        Icon(Icons.Default.Delete, "Remove Webspace", tint = palette.muted)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(30) },
                label = { Text("Custom Webspace") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (webspaces.create(name) != null) name = "" }) {
                Text("Add")
            }
        }
        if (current.surface == Surface.WEB &&
            VisitPolicy.host(current.url) in webspaces.assignments) {
            TextButton(
                onClick = {
                    webspaces.removeAssignment(current.url)
                    current.webspaceId = webspaces.destination(current.url)
                    current.pinnedWebspace = false
                }, modifier = Modifier.padding(start = 18.dp)
            ) { Text("Remove this site's routing rule") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BrowserOverflowMenu(
    tab: BrowserTab, preferences: BrowserPreferences,
    palette: GcPalette, activity: Activity,
    onPrivacy: () -> Unit, onWebspaces: () -> Unit, onHome: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val canAct = tab.surface == Surface.WEB
    Box {
        DockButton(Icons.Default.MoreVert, "More options", palette) { expanded = true }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(262.dp),
            offset = DpOffset((-7).dp, (-7).dp),
            shape = RoundedCornerShape(22.dp),
            containerColor = palette.surface,
            tonalElevation = 0.dp,
            shadowElevation = 18.dp,
            border = BorderStroke(1.dp, palette.outline)
        ) {
            Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("QUICK ACTIONS", color = palette.muted,
                        fontSize = 10.sp, letterSpacing = 1.7.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Tune, contentDescription = null,
                        tint = palette.muted, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(3.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MenuActionTile(Icons.Default.Refresh, "Reload", canAct, palette,
                        modifier = Modifier.weight(1f)) {
                        expanded = false
                        tab.webView?.reload()
                    }
                    MenuActionTile(Icons.Default.Bookmark,
                        if (tab.url in preferences.savedSites) "Unsave" else "Save",
                        canAct, palette, modifier = Modifier.weight(1f)) {
                        expanded = false
                        preferences.toggleBookmark(tab.url)
                    }
                    MenuActionTile(Icons.Default.Share, "Share", canAct, palette,
                        modifier = Modifier.weight(1f)) {
                        expanded = false
                        if (NavigationPolicy.isHttpUrl(tab.url)) {
                            activity.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, tab.url)
                                }, "Share link"
                            ))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = palette.outline.copy(alpha = 0.6f))
                Spacer(Modifier.height(5.dp))
                MenuNavigationItem(Icons.Default.Language, "Webspaces", palette) {
                    expanded = false
                    onWebspaces()
                }
                MenuNavigationItem(Icons.Default.Shield, "Privacy & DNS", palette) {
                    expanded = false
                    onPrivacy()
                }
                MenuNavigationItem(Icons.Default.Home, "Go Home", palette) {
                    expanded = false
                    onHome()
                }
            }
        }
    }
}

@Composable
private fun MenuActionTile(
    icon: ImageVector, title: String, enabled: Boolean, palette: GcPalette,
    modifier: Modifier = Modifier, action: () -> Unit
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceAlt.copy(alpha = 0.85f))
            .clickable(enabled = enabled, onClick = action)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null,
            tint = if (enabled) palette.accent else palette.muted.copy(alpha = 0.4f),
            modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(5.dp))
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            color = if (enabled) palette.text else palette.muted.copy(alpha = 0.55f))
    }
}

@Composable
private fun MenuNavigationItem(
    icon: ImageVector, title: String, palette: GcPalette, action: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .clickable(onClick = action)
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(33.dp).background(
                palette.surfaceAlt, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = palette.accent,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Text(title, color = palette.text, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = null,
            tint = palette.muted, modifier = Modifier.size(14.dp))
    }
}
