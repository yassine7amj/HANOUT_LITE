package ma.hanout.lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ma.hanout.lite.ui.AboutScreen
import ma.hanout.lite.ui.BrandTopBar
import ma.hanout.lite.ui.CartScreen
import ma.hanout.lite.ui.HanoutTheme
import ma.hanout.lite.ui.HomeScreen
import ma.hanout.lite.ui.LoadError
import ma.hanout.lite.ui.LoadingBox
import ma.hanout.lite.ui.MerchantRoot
import ma.hanout.lite.ui.OrdersScreen
import ma.hanout.lite.ui.StoreViewModel
import ma.hanout.lite.ui.ThemePrefs
import ma.hanout.lite.ui.UiState
import ma.hanout.lite.ui.parseHexColor

/**
 * نقطة الدخول ديال منصة HANWEB HUB.
 * ------------------------------------------------------------------
 * نفس الكود كيولّد جوج تطبيقات حسب [BuildConfig.APP_ROLE]:
 *   • customer : تطبيق الزبون — كيتصفح الحانوت، كيعمّر السلة وكيصيفط الطلب.
 *   • merchant : تطبيق التاجر (مول الحانوت) — كيسيّر الطلبات، الكتالوغ والحانوت.
 *
 * الشعار، الألوان والاتجاه (RTL) مشتركين بين الجوج.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePrefs.load(this)
        setContent {
            // التطبيق عربي بالكامل — كنفرضو الاتجاه من اليمين لليسار.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                if (BuildConfig.APP_ROLE.equals("merchant", ignoreCase = true)) {
                    MerchantRoot()
                } else {
                    CustomerRoot()
                }
            }
        }
    }
}

/* ============================================================
 * نسخة الزبون
 * ============================================================ */

@Composable
private fun CustomerRoot() {
    val viewModel: StoreViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    // لون العلامة ديال الحانوت (إلا كان محدّد فـ Firestore).
    val brandHex = state.shop?.color?.takeIf { it.isNotBlank() }
    val brandColor = remember(brandHex) { brandHex?.let(::parseHexColor) }

    HanoutTheme(brandColor = brandColor) {
        CustomerScaffold(state = state, viewModel = viewModel)
    }
}

/** التبويبات السفلية ديال تطبيق الزبون. */
private enum class CustomerTab(val icon: ImageVector, val label: Int) {
    Home(Icons.Filled.Home, R.string.nav_home),
    Cart(Icons.Filled.ShoppingCart, R.string.nav_cart),
    Orders(Icons.Filled.List, R.string.nav_orders),
    About(Icons.Filled.Info, R.string.nav_about)
}

@Composable
private fun CustomerScaffold(state: UiState, viewModel: StoreViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageId = state.message
    val messageText = messageId?.let { stringResource(it) }

    LaunchedEffect(messageId) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText, duration = SnackbarDuration.Short)
            viewModel.consumeMessage()
        }
    }

    if (state.shop == null) {
        if (state.loading) LoadingBox(Modifier.fillMaxSize())
        else LoadFailure(state.error, viewModel::refresh)
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val shop = state.shop

    Scaffold(
        topBar = {
            BrandTopBar(
                title = shop?.name.orEmpty(),
                subtitle = stringResource(R.string.app_role_customer),
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                CustomerTab.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        label = {
                            Text(
                                text = stringResource(item.label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            if (item == CustomerTab.Cart && state.cartCount > 0) {
                                BadgedBox(badge = { Badge { Text(state.cartCount.toString()) } }) {
                                    Icon(item.icon, contentDescription = null)
                                }
                            } else {
                                Icon(item.icon, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    ) { inner ->
        val contentModifier = Modifier.padding(inner).fillMaxSize()
        when (CustomerTab.entries[tab]) {
            CustomerTab.Home -> HomeScreen(state, viewModel, contentModifier)
            CustomerTab.Cart -> CartScreen(state, viewModel, contentModifier)
            CustomerTab.Orders -> OrdersScreen(state, contentModifier)
            CustomerTab.About -> AboutScreen(state, viewModel, contentModifier)
        }
    }
}

@Composable
private fun LoadFailure(error: LoadError?, onRetry: () -> Unit) {
    val messageId = when (error) {
        LoadError.NotFound -> R.string.shop_not_found
        LoadError.Suspended -> R.string.shop_suspended
        else -> R.string.error_load
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(messageId),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
