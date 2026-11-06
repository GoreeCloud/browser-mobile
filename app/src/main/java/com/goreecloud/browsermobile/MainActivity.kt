package com.goreecloud.browsermobile

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.goreecloud.browsermobile.core.BrowsingLibrary
import com.goreecloud.browsermobile.core.SiteIcons
import com.goreecloud.browsermobile.core.WebspacesManager
import com.goreecloud.browsermobile.core.BrowserPreferences
import com.goreecloud.browsermobile.core.NavigationPolicy
import com.goreecloud.browsermobile.core.FilterListEngine
import com.goreecloud.browsermobile.core.FilterListStatus
import com.goreecloud.browsermobile.core.FilterSource
import com.goreecloud.browsermobile.core.VisitPolicy
import com.goreecloud.browsermobile.core.VisitedSite
import com.goreecloud.browsermobile.core.SiteExceptions
import com.goreecloud.browsermobile.core.SearchPolicy
import com.goreecloud.browsermobile.core.UrlCleaner
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private enum class Surface { NEW_TAB, HOME, WEB }
private enum class Panel { TABS, PRIVACY, WEBSPACES }
private enum class DestinationMode { RECENT, FAVORITES, MOST }

private class BrowserTab {
    val id: String = UUID.randomUUID().toString()
    var surface by mutableStateOf(Surface.NEW_TAB)
    var url by mutableStateOf("")
    @Volatile var requestTopLevelUrl: String = ""
    var title by mutableStateOf("New Tab")
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
    onUrlCleaned: (Int) -> Unit
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

    fun openUrl(input: String) {
        val resolved = NavigationPolicy.resolve(input, preferences.cleanUrls)
        if (resolved.isBlank()) return
        current.error = null
        current.url = resolved
        if (!current.pinnedWebspace) current.webspaceId = webspaces.destination(resolved)
        current.requestTopLevelUrl = resolved
        current.surface = Surface.WEB
        current.webView?.loadUrl(resolved)
        focus.clearFocus()
    }

    fun openNewTab(webspace: String? = null) {
        if (tabs.size >= 12) {
            panel = Panel.TABS
            return
        }
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
            activeId = found.id
            panel = null
        } else openNewTab(id)
    }

    fun closeTab(tab: BrowserTab) {
        if (tabs.size == 1) {
            tab.webView?.destroy()
            tab.webView = null
            tab.url = ""
            tab.pinnedWebspace = false
            tab.requestTopLevelUrl = ""
            tab.title = "New Tab"
            tab.surface = Surface.NEW_TAB
        } else {
            val index = tabs.indexOf(tab)
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
    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank()) {
            openUrl(incomingUrl)
            consumeIncomingUrl()
        }
    }

    BackHandler(enabled = panel != null || current.surface == Surface.WEB) {
        when {
            panel != null -> panel = null
            current.surface == Surface.WEB && current.webView?.canGoBack() == true ->
                current.webView?.goBack()
            else -> current.surface = Surface.HOME
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(palette.canvas)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 21.dp, end = 15.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(31.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF9D8BFF), Color(0xFF62D9CE))),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "GoreeCloud Browser Mobile app mark",
                        modifier = Modifier.size(31.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("GoreeCloud", color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("BROWSER MOBILE", color = palette.muted, fontSize = 9.sp, letterSpacing = 2.sp)
                }
                // The privacy controls remain available from the overflow menu.
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.outline, RoundedCornerShape(19.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (current.surface == Surface.WEB) Icons.Default.Lock else Icons.Default.Search,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.padding(start = 15.dp).size(18.dp)
                )
                TextField(
                    value = address, onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search privately or enter address", fontSize = 13.sp, color = palette.muted) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = palette.text),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { openUrl(address) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = palette.text, unfocusedTextColor = palette.text,
                        cursorColor = palette.accent
                    )
                )
                IconButton(onClick = { openUrl(address) }, modifier = Modifier.size(46.dp)) {
                    Icon(Icons.Default.ArrowForward, "Go", tint = palette.accent)
                }
            }
            if (current.surface == Surface.WEB && current.loading) {
                LinearProgressIndicator(
                    progress = { current.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = palette.accent,
                    trackColor = palette.surfaceAlt
                )
            } else Spacer(Modifier.height(2.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (current.surface) {
                    Surface.NEW_TAB -> NewTabPage(
                        palette = palette, openUrl = ::openUrl,
                        onHome = { current.surface = Surface.HOME }
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
                                            library, webspaces, icons, { faviconRevision++ }
                                        ) { cleanedCount += it }
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 7.dp)
                    .background(palette.surface, RoundedCornerShape(26.dp))
                    .border(1.dp, palette.outline, RoundedCornerShape(26.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockButton(Icons.Default.ArrowBack, "Back", palette, enabled =
                    current.surface == Surface.WEB && current.webView?.canGoBack() == true) {
                    current.webView?.goBack()
                }
                DockButton(Icons.Default.ArrowForward, "Forward", palette, enabled =
                    current.surface == Surface.WEB && current.webView?.canGoForward() == true) {
                    current.webView?.goForward()
                }
                DockButton(Icons.Default.Home, "Home", palette) { current.surface = Surface.HOME }
                DockButton(Icons.Default.Add, "New tab", palette, accent = true) { openNewTab() }
                TabCountDock(tabs.size, palette) { panel = Panel.TABS }
                BrowserOverflowMenu(
                    tab = current,
                    preferences = preferences,
                    palette = palette,
                    activity = activity,
                    onPrivacy = { panel = Panel.PRIVACY },
                    onWebspaces = { panel = Panel.WEBSPACES },
                    onHome = { current.surface = Surface.HOME }
                )
            }
            Spacer(Modifier.height(5.dp))
        }
    }

    if (panel != null) {
        ModalBottomSheet(
            onDismissRequest = { panel = null },
            containerColor = palette.surface, contentColor = palette.text
        ) {
            when (panel) {
                Panel.TABS -> {
                    SheetHeading("Your tabs", "${tabs.size} open · maximum 12", palette)
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 7.dp)) {
                        items(tabs.toList(), key = { it.id }) { tab ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(bottom = 9.dp)
                                    .background(
                                        if (tab.id == activeId) palette.surfaceAlt else palette.canvas,
                                        RoundedCornerShape(17.dp)
                                    )
                                    .clickable { activeId = tab.id; panel = null }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, null, tint = palette.accent)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tab.title, color = palette.text, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(webspaces.entries.firstOrNull { it.id == tab.webspaceId }?.name
                                        ?: "Standard", color = palette.accent, fontSize = 10.sp)
                                    Text(tab.url.ifEmpty { tab.surface.name.replace('_', ' ') },
                                        color = palette.muted, fontSize = 12.sp, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { closeTab(tab) }) {
                                    Icon(Icons.Default.Close, "Close tab", tint = palette.muted)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { openNewTab() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("New tab")
                    }
                }
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
private fun DockButton(
    icon: ImageVector,
    label: String,
    palette: GcPalette,
    enabled: Boolean = true,
    accent: Boolean = false,
    action: () -> Unit
) {
    IconButton(
        onClick = action,
        enabled = enabled,
        modifier = Modifier.size(47.dp).then(
            if (accent) Modifier.background(
                Brush.linearGradient(listOf(Color(0xFF8AA7FF), Color(0xFF7CE5D9))),
                RoundedCornerShape(17.dp)
            ) else Modifier
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = when {
                !enabled -> palette.muted.copy(alpha = 0.38f)
                accent -> Color(0xFF0B1736)
                else -> palette.text
            }
        )
    }
}

@Composable
private fun NewTabPage(
    palette: GcPalette,
    openUrl: (String) -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier.size(78.dp).background(
                Brush.linearGradient(listOf(Color(0xFFA78AFF), Color(0xFF72E1DA))),
                RoundedCornerShape(26.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Color(0xFF101831), modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(25.dp))
        Text(
            "New beginnings.",
            fontSize = 31.sp, lineHeight = 35.sp,
            fontWeight = FontWeight.Bold, color = palette.text
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "An open canvas for your next curiosity.",
            color = palette.muted, fontSize = 14.sp
        )
        Spacer(Modifier.height(33.dp))
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, palette.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = palette.accent)
                    Spacer(Modifier.width(10.dp))
                    Text("Search the open web", fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, color = palette.text)
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    "Startpage protects your searches while GoreeCloud Search is being stabilized.",
                    color = palette.muted, fontSize = 13.sp, lineHeight = 19.sp
                )
                Spacer(Modifier.height(17.dp))
                Button(
                    onClick = { openUrl(SearchPolicy.HOME_URL) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent, contentColor = palette.accentOn
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Open Startpage", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(17.dp))
                }
            }
        }
        Spacer(Modifier.height(29.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("A LITTLE INSPIRATION", color = palette.muted,
                fontSize = 11.sp, letterSpacing = 1.7.sp)
        }
        Spacer(Modifier.height(13.dp))
        SuggestionRow("Discover something new", "the history of the internet", palette, openUrl)
        SuggestionRow("Explore the universe", "latest space discoveries", palette, openUrl)
        SuggestionRow("Learn one new thing", "science breakthroughs", palette, openUrl)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onHome) {
            Icon(Icons.Default.Home, null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Visit your Home", color = palette.accent)
        }
    }
}

@Composable
private fun SuggestionRow(
    title: String,
    query: String,
    palette: GcPalette,
    openUrl: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .background(palette.surfaceAlt.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .clickable { openUrl(query) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = palette.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, null, tint = palette.muted, modifier = Modifier.size(18.dp))
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
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Privacy at a glance", fontSize = 20.sp, fontWeight = FontWeight.Bold,
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
                name = "Trackers removed",
                icon = Icons.Default.Lock,
                palette = palette
            )
        }
        Spacer(Modifier.height(27.dp))
        QuickDestinations(library, preferences, icons, faviconRevision, palette, openUrl)
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
            .padding(17.dp)
    ) {
        Icon(icon, null, tint = palette.success, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(13.dp))
        Text(count, color = palette.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
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
    Text("Quick destinations", fontSize = 20.sp,
        fontWeight = FontWeight.Bold, color = palette.text)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(DestinationMode.RECENT to "Recent",
            DestinationMode.FAVORITES to "Favorites",
            DestinationMode.MOST to "Most visited").forEach { (option, label) ->
            FilterChip(selected = mode == option, onClick = { mode = option },
                label = { Text(label, fontSize = 11.sp) })
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
    Box {
        DockButton(Icons.Default.MoreVert, "Menu", palette) { expanded = true }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(palette.surface)
        ) {
            DropdownMenuItem(
                text = { Text("Webspaces") },
                leadingIcon = { Icon(Icons.Default.Language, null) },
                onClick = { expanded = false; onWebspaces() }
            )
            DropdownMenuItem(
                text = { Text("Reload page") },
                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                enabled = tab.surface == Surface.WEB,
                onClick = { expanded = false; tab.webView?.reload() }
            )
            DropdownMenuItem(
                text = { Text(if (tab.url in preferences.savedSites)
                    "Remove bookmark" else "Bookmark page") },
                leadingIcon = { Icon(Icons.Default.Bookmark, null) },
                enabled = tab.surface == Surface.WEB,
                onClick = { expanded = false; preferences.toggleBookmark(tab.url) }
            )
            DropdownMenuItem(
                text = { Text("Share page") },
                leadingIcon = { Icon(Icons.Default.Share, null) },
                enabled = tab.surface == Surface.WEB,
                onClick = {
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
            )
            HorizontalDivider(color = palette.outline)
            DropdownMenuItem(
                text = { Text("Privacy & DNS manager") },
                leadingIcon = { Icon(Icons.Default.Shield, null) },
                onClick = { expanded = false; onPrivacy() }
            )
            DropdownMenuItem(
                text = { Text("Home") },
                leadingIcon = { Icon(Icons.Default.Home, null) },
                onClick = { expanded = false; onHome() }
            )
        }
    }
}
