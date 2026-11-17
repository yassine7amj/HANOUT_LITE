package ma.hanout.lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ma.hanout.lite.R

/**
 * جذر تطبيق التاجر (مول الحانوت) — نسخة HANWEB HUB للتسيير.
 * ------------------------------------------------------------------
 * كنستعملو نفس [HanoutTheme] ونفس الشعار المستعمل فالنسخة الويب.
 */
@Composable
fun MerchantRoot() {
    val viewModel: MerchantViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    val brandHex = state.shop?.color?.takeIf { it.isNotBlank() }
    val brandColor = remember(brandHex) { brandHex?.let(::parseHexColor) }

    HanoutTheme(brandColor = brandColor) {
        if (state.signedIn) {
            MerchantScaffold(state = state, viewModel = viewModel)
        } else {
            MerchantLoginScreen(state = state, viewModel = viewModel)
        }
    }
}

/** تبويبات تطبيق التاجر. */
private enum class MerchantTab(val icon: ImageVector, val label: Int) {
    Dashboard(Icons.Filled.Home, R.string.nav_dashboard),
    Orders(Icons.Filled.List, R.string.nav_manage_orders),
    Catalog(Icons.Filled.ShoppingCart, R.string.nav_catalog),
    Shop(Icons.Filled.Settings, R.string.nav_shop)
}

@Composable
private fun MerchantScaffold(state: MerchantState, viewModel: MerchantViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.consumeMessage()
        }
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            BrandTopBar(
                title = state.shop?.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.hub_name),
                subtitle = if (state.demo) stringResource(R.string.merchant_demo_badge)
                else stringResource(R.string.app_role_merchant),
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
                MerchantTab.entries.forEachIndexed { index, item ->
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
                            if (item == MerchantTab.Orders && state.newOrders > 0) {
                                BadgedBox(badge = { Badge { Text(state.newOrders.toString()) } }) {
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
        when {
            state.loading && state.shop == null -> LoadingBox(contentModifier)

            state.shop == null -> MerchantNoShop(state, viewModel, contentModifier)

            else -> when (MerchantTab.entries[tab]) {
                MerchantTab.Dashboard -> MerchantDashboardScreen(
                    state = state,
                    modifier = contentModifier,
                    onSeeOrders = { tab = MerchantTab.Orders.ordinal }
                )

                MerchantTab.Orders -> MerchantOrdersScreen(state, viewModel, contentModifier)

                MerchantTab.Catalog -> MerchantCatalogScreen(state, viewModel, contentModifier)

                MerchantTab.Shop -> MerchantShopScreen(state, viewModel, contentModifier)
            }
        }
    }
}

/* ============================================================
 * شاشة الدخول
 * ============================================================ */

@Composable
fun MerchantLoginScreen(
    state: MerchantState,
    viewModel: MerchantViewModel,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val loginError = state.error?.takeIf { it.isNotBlank() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandLogo(size = 88.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.hub_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.hub_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            RolePill(stringResource(R.string.app_role_merchant))
            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.merchant_login_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.merchant_login_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.busy,
                        label = { Text(stringResource(R.string.merchant_email)) },
                        leadingIcon = { Icon(Icons.Filled.MailOutline, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.busy,
                        label = { Text(stringResource(R.string.merchant_password)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    if (loginError != null) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = loginError,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.signIn(email, password) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.merchant_sign_in))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::enterDemo,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.merchant_demo))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.merchant_auth_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ============================================================
 * مكوّنات مشتركة
 * ============================================================ */

/** شرابة صغيرة كتورّي نوع النسخة (زبون / تاجر). */
@Composable
fun RolePill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** ملي ما يكونش الحانوت مربوط بالحساب. */
@Composable
private fun MerchantNoShop(
    state: MerchantState,
    viewModel: MerchantViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = state.error?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.dashboard_no_shop),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = viewModel::refresh) {
            Text(stringResource(R.string.retry))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::enterDemo) {
            Text(stringResource(R.string.merchant_demo))
        }
    }
}
