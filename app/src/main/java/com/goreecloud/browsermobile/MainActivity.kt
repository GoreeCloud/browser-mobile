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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
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

// Native mapping of the stable GC UI v1 Iris semantic palette.
// Keep light/dark colors paired with their explicit on-colors.
private val DarkGc = GcPalette(
    canvas = Color(0xFF10131E), surface = Color(0xFF1D2231),
    surfaceAlt = Color(0xFF292F40), text = Color(0xFFF2F2FA),
    muted = Color(0xFFB3B9CF), accent = Color(0xFFC5BFFF),
    accentOn = Color(0xFF241B66), outline = Color(0xFF747C99),
    success = Color(0xFF9CE3B9)
)
private val LightGc = GcPalette(
    canvas = Color(0xFFECF0F8), surface = Color(0xFFF9FAFD),
    surfaceAlt = Color(0xFFE5EBF6), text = Color(0xFF20243A),
    muted = Color(0xFF5F657B), accent = Color(0xFF5145D9),
    accentOn = Color.White, outline = Color(0xFF8990A7),
    success = Color(0xFF206A46)
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
    val configuration = LocalConfiguration.current
    val compactHeader = configuration.screenWidthDp < 390 ||
        configuration.fontScale >= 1.25f

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
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(39.dp)
                        .background(palette.surfaceAlt, RoundedCornerShape(14.dp))
                        .border(1.dp, palette.outline.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "GoreeCloud Browser Mobile",
                        modifier = Modifier.size(35.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("GoreeCloud", color = palette.text, fontSize = 19.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("BROWSER MOBILE", color = palette.muted,
                        fontSize = 10.sp, letterSpacing = 1.4.sp)
                }
                Row(
                    modifier = Modifier.background(
                        palette.surfaceAlt, RoundedCornerShape(99.dp)
                    ).padding(horizontal = if (compactHeader) 8.dp else 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = if (compactHeader) {
                            if (preferences.blockAds) "Ad and tracker blocking on"
                            else "Ad and tracker blocking off"
                        } else null,
                        tint = if (preferences.blockAds) palette.success else palette.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        when {
                            !compactHeader && preferences.blockAds -> "Blocking on"
                            !compactHeader -> "Blocking off"
                            preferences.blockAds -> "On"
                            else -> "Off"
                        },
                        color = palette.text, fontSize = 11.sp,
                        fontWeight = FontWeight.Medium, maxLines = 1
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
                    .shadow(3.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.outline.copy(alpha = 0.38f),
                        RoundedCornerShape(18.dp)),
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
                    placeholder = {
                        Text("Search or enter address", fontSize = 14.sp,
                            color = palette.muted, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = palette.text),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { openUrl(address) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = palette.text, unfocusedTextColor = palette.text,
                        cursorColor = palette.accent
                    )
                )
                IconButton(onClick = { openUrl(address) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ArrowForward, "Go to address or search",
                        tint = palette.accent)
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
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 9.dp)
                    .shadow(12.dp, RoundedCornerShape(30.dp))
                    .background(palette.surface, RoundedCornerShape(30.dp))
                    .border(1.dp, palette.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(30.dp))
                    .padding(horizontal = 5.dp, vertical = 5.dp),
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
                    SheetHeading("Your tabs", "${tabs.size} of 12 tabs · choose one to continue", palette)
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 7.dp)) {
                        items(tabs.toList(), key = { it.id }) { tab ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(bottom = 9.dp)
                                    .background(
                                        if (tab.id == activeId) palette.surfaceAlt else palette.canvas,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(1.dp,
                                        if (tab.id == activeId) palette.accent.copy(alpha = 0.58f)
                                        else palette.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { activeId = tab.id; panel = null }
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp)
                                        .background(palette.surface, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (tab.surface == Surface.WEB) Icons.Default.Language
                                        else Icons.Default.Home,
                                        null, tint = palette.accent, modifier = Modifier.size(21.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tab.title, color = palette.text, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f, fill = false))
                                        if (tab.id == activeId) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("ACTIVE", color = palette.accent,
                                                fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(webspaces.entries.firstOrNull { it.id == tab.webspaceId }?.name
                                        ?: "Standard", color = palette.accent, fontSize = 12.sp)
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
                    Button(
                        onClick = { openNewTab() }, enabled = tabs.size < 12,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent, contentColor = palette.accentOn
                        )
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("New tab")
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("NEW TAB", color = palette.accent, fontSize = 11.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        Text("Where to next?", fontSize = 32.sp, lineHeight = 38.sp,
            fontWeight = FontWeight.Bold, color = palette.text)
        Spacer(Modifier.height(6.dp))
        Text("A fresh space for your next discovery.",
            color = palette.muted, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(26.dp))
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.38f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(palette.surfaceAlt, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Language, null, tint = palette.accent,
                            modifier = Modifier.size(23.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Explore the open web", fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold, color = palette.text)
                        Text("Powered by Startpage", color = palette.muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text(
                    "Private search while GoreeCloud Search is being stabilized.",
                    color = palette.muted, fontSize = 14.sp, lineHeight = 21.sp
                )
                Spacer(Modifier.height(17.dp))
                Button(
                    onClick = { openUrl(SearchPolicy.HOME_URL) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent, contentColor = palette.accentOn
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                ) {
                    Text("Open Startpage", fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("A LITTLE INSPIRATION", color = palette.muted, fontSize = 11.sp,
            letterSpacing = 1.7.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        SuggestionRow("Discover something new", "the history of the internet", palette, openUrl)
        SuggestionRow("Explore the universe", "latest space discoveries", palette, openUrl)
        SuggestionRow("Learn something today", "science breakthroughs", palette, openUrl)
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onHome, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.Home, null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Visit your Home", color = palette.accent)
        }
        Spacer(Modifier.height(18.dp))
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
            .background(palette.surfaceAlt, RoundedCornerShape(18.dp))
            .clickable { openUrl(query) }
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = palette.text, fontSize = 14.sp,
            modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
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
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        val today = remember {
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        }
        Text(today.uppercase(), color = palette.muted,
            letterSpacing = 1.5.sp, fontSize = 11.sp)
        Spacer(Modifier.height(17.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your places", fontSize = 29.sp, lineHeight = 34.sp,
                fontWeight = FontWeight.Bold, color = palette.text,
                modifier = Modifier.weight(1f))
            Text("ON THIS DEVICE", fontSize = 10.sp, letterSpacing = 1.sp,
                color = palette.muted)
        }
        Spacer(Modifier.height(15.dp))
        QuickDestinations(library, preferences, icons, faviconRevision, palette, openUrl)
        Spacer(Modifier.height(23.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Privacy at a glance", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = palette.text, modifier = Modifier.weight(1f))
            TextButton(onClick = onPrivacy) {
                Text("Manage", color = palette.accent,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
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
                name = "Tracking fields removed",
                icon = Icons.Default.Lock,
                palette = palette
            )
        }
        Spacer(Modifier.height(30.dp))
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
            .border(1.dp, palette.outline.copy(alpha = 0.38f),
                RoundedCornerShape(22.dp))
            .padding(17.dp)
    ) {
        Icon(icon, null, tint = palette.success, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(13.dp))
        Text(count, color = palette.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(name, color = palette.muted, fontSize = 12.sp,
            lineHeight = 16.sp)
    }
}

@Composable
private fun SheetHeading(title: String, subtitle: String, palette: GcPalette) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = palette.text,
            lineHeight = 33.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 14.sp, color = palette.muted, lineHeight = 20.sp)
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector, label: String, palette: GcPalette, action: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = action)
            .heightIn(min = 52.dp).padding(horizontal = 24.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(21.dp), tint = palette.accent)
        Spacer(Modifier.width(17.dp))
        Text(label, fontSize = 15.sp, color = palette.text)
    }
}

@Composable
private fun PrivacySectionHeading(
    icon: ImageVector, title: String, description: String, palette: GcPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp)
                .background(palette.surfaceAlt, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold)
            Text(description, color = palette.muted,
                fontSize = 12.sp, lineHeight = 18.sp)
        }
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
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .heightIn(min = 56.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = palette.muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = enabled, onCheckedChange = null,
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
    var confirmHistoryClear by remember { mutableStateOf(false) }
    var confirmHistoryDisable by remember { mutableStateOf(false) }
    var confirmCookiesClear by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeading("Privacy center", "Visible controls, no hidden promises", palette)
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 9.dp)
                .background(palette.surfaceAlt, RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("${tab.blocked}", color = palette.text, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold)
                Text("Blocked on this tab", color = palette.muted,
                    fontSize = 12.sp, lineHeight = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("$cleanedCount", color = palette.text, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold)
                Text("Tracking fields removed", color = palette.muted,
                    fontSize = 12.sp, lineHeight = 18.sp)
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
                if (enabled) library.updateHistoryEnabled(true)
                else confirmHistoryDisable = true
            }
        )
        SheetAction(Icons.Default.Delete, "Clear recent websites and favicons", palette) {
            confirmHistoryClear = true
        }
        HorizontalDivider(color = palette.outline, modifier = Modifier.padding(horizontal = 20.dp))
        PrivacySectionHeading(
            Icons.Default.Shield, "Filter subscriptions",
            "OISD Small and StevenBlack Unified are the defaults. Other lists are optional. " +
                "Saved rules stay on-device and filter WebView subresources only.",
            palette
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
        PrivacySectionHeading(
            Icons.Default.Lock, "Cookies & site data",
            "Control cookies stored by Android WebView.", palette
        )
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
            confirmCookiesClear = true
        }
        HorizontalDivider(color = palette.outline, modifier = Modifier.padding(horizontal = 20.dp))
        PrivacySectionHeading(
            Icons.Default.Settings, "DNS settings",
            "Android WebView uses system DNS. This version does not provide a " +
                "separate browser DNS-over-HTTPS resolver.",
            palette
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
    if (confirmHistoryDisable) {
        AlertDialog(
            onDismissRequest = { confirmHistoryDisable = false },
            title = { Text("Turn off visited websites?") },
            text = {
                Text("Turning off this feature also clears recent and most-visited " +
                    "website hosts and saved favicons from this device. Bookmarks remain.")
            },
            confirmButton = {
                TextButton(onClick = {
                    library.updateHistoryEnabled(false)
                    library.clear()
                    icons.clear()
                    message.value = "Visited websites disabled; local history and favicons cleared"
                    confirmHistoryDisable = false
                }) { Text("Turn off & clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmHistoryDisable = false }) { Text("Cancel") }
            }
        )
    }
    if (confirmHistoryClear) {
        AlertDialog(
            onDismissRequest = { confirmHistoryClear = false },
            title = { Text("Clear recent websites?") },
            text = {
                Text("This removes the local recent and most-visited list and saved favicons. " +
                    "It will not delete bookmarks.")
            },
            confirmButton = {
                TextButton(onClick = {
                    library.clear()
                    icons.clear()
                    message.value = "Local website history and saved favicons cleared"
                    confirmHistoryClear = false
                }) { Text("Clear history") }
            },
            dismissButton = {
                TextButton(onClick = { confirmHistoryClear = false }) { Text("Cancel") }
            }
        )
    }
    if (confirmCookiesClear) {
        AlertDialog(
            onDismissRequest = { confirmCookiesClear = false },
            title = { Text("Clear cookies and site data?") },
            text = {
                Text("This signs you out of some sites and deletes WebView cookies and web storage.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCookiesClear = false
                    CookieManager.getInstance().removeAllCookies {
                        CookieManager.getInstance().flush()
                        WebStorage.getInstance().deleteAllData()
                        message.value = "Cookies and web storage cleared"
                    }
                }) { Text("Clear site data") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCookiesClear = false }) { Text("Cancel") }
            }
        )
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
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        listOf(DestinationMode.RECENT to "Recent",
            DestinationMode.FAVORITES to "Favorites",
            DestinationMode.MOST to "Most visited").forEach { (option, label) ->
            FilterChip(
                selected = mode == option, onClick = { mode = option },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.surfaceAlt,
                    selectedLabelColor = palette.text,
                    containerColor = palette.canvas,
                    labelColor = palette.muted
                )
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
    // At high text scales or narrow windows, avoid squeezing two site names into one row.
    val oneColumn = LocalConfiguration.current.screenWidthDp < 350 ||
        LocalDensity.current.fontScale >= 1.45f
    sites.chunked(if (oneColumn) 1 else 2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            pair.forEach { site ->
                val favicon = remember(site.host, faviconRevision) { icons.load(site.host) }
                Column(
                    modifier = Modifier.weight(1f)
                        .background(palette.surface, RoundedCornerShape(20.dp))
                        .border(1.dp, palette.outline.copy(alpha = 0.38f),
                            RoundedCornerShape(20.dp))
                        .clickable { openUrl(site.url) }
                        .heightIn(min = 115.dp)
                        .padding(15.dp)
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
                    Text(site.host, color = palette.text, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(if (site.visits == 1) "1 visit"
                         else if (site.visits > 1) site.visits.toString() + " visits"
                         else "Bookmarked",
                        color = palette.muted, fontSize = 12.sp,
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
    var pendingRemove by remember { mutableStateOf<com.goreecloud.browsermobile.core.WebspaceEntry?>(null) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeading("Webspaces", "Organize tabs and route sites", palette)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 12.dp)
                .background(palette.surfaceAlt, RoundedCornerShape(18.dp))
                .padding(15.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Lock, null, tint = palette.accent,
                modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Text("Webspaces organize tabs, but share Android WebView cookies. " +
                "They do not isolate website accounts.",
                color = palette.text, fontSize = 13.sp, lineHeight = 19.sp)
        }
        webspaces.entries.forEach { space ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 5.dp)
                    .background(
                        if (current.webspaceId == space.id) palette.surfaceAlt else palette.surface,
                        RoundedCornerShape(18.dp)
                    )
                    .border(
                        1.dp,
                        if (current.webspaceId == space.id) palette.accent.copy(alpha = 0.65f)
                        else palette.outline.copy(alpha = 0.38f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { open(space.id) }
                    .heightIn(min = 70.dp)
                    .padding(horizontal = 15.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Language, null, tint = palette.accent)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text(space.name, color = palette.text, fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp)
                    if (current.webspaceId == space.id) {
                        Text("CURRENT WEBSPACE", color = palette.accent,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    val count = tabs.count { it.webspaceId == space.id }
                    Text(count.toString() + if (count == 1) " tab" else " tabs",
                        color = palette.muted, fontSize = 12.sp)
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
                    IconButton(onClick = { pendingRemove = space }) {
                        Icon(Icons.Default.Delete,
                            "Remove " + space.name + " Webspace", tint = palette.muted)
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
    pendingRemove?.let { space ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Remove " + space.name + "?") },
            text = {
                Text("This removes the Webspace and its site routing rules. Open tabs " +
                    "in it move back to Standard. Website cookies are not deleted.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(space.id)
                    pendingRemove = null
                }) { Text("Remove Webspace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text("Cancel") }
            }
        )
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
