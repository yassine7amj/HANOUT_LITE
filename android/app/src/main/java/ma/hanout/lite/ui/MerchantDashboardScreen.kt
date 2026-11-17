package ma.hanout.lite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ma.hanout.lite.BuildConfig
import ma.hanout.lite.R
import ma.hanout.lite.data.Order

/**
 * لوحة تحكم التاجر: نظرة سريعة على المبيعات، الطلبات الجديدة والمنتوجات،
 * مع آخر الطلبات وإجراءات سريعة (مشاركة الحانوت، حلّ النسخة الويب).
 */
@Composable
fun MerchantDashboardScreen(
    state: MerchantState,
    modifier: Modifier = Modifier,
    onSeeOrders: () -> Unit = {}
) {
    val context = LocalContext.current
    val webUrl = remember(state.slug) {
        val base = BuildConfig.WEB_BASE.trimEnd('/')
        if (state.slug.isBlank()) "$base/" else "$base/?shop=${state.slug}"
    }
    val shareMessage = stringResource(R.string.share_text, state.shop?.name.orEmpty()) +
        "\n" + webUrl
    val chooserTitle = stringResource(R.string.share_chooser)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { WelcomeCard(state) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        icon = Icons.Filled.Notifications,
                        value = state.newOrders.toString(),
                        label = stringResource(R.string.stat_new_orders),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.List,
                        value = state.orders.size.toString(),
                        label = stringResource(R.string.stat_all_orders),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        icon = Icons.Filled.Star,
                        value = money(state.revenue),
                        label = stringResource(R.string.stat_revenue),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.ShoppingCart,
                        value = "${state.availableProducts}/${state.products.size}",
                        label = stringResource(R.string.stat_products),
                        container = MaterialTheme.colorScheme.surfaceContainerHighest,
                        onContainer = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item { TodayCard(state) }

        item {
            SectionCard(title = stringResource(R.string.dashboard_quick)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { shareText(context, shareMessage, chooserTitle) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.dashboard_share_shop), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { openUrl(context, webUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.dashboard_open_web), maxLines = 1)
                    }
                }
            }
        }

        item { LatestOrders(state, onSeeOrders) }
    }
}

@Composable
private fun WelcomeCard(state: MerchantState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_welcome),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.shop?.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.hub_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = if (state.demo) stringResource(R.string.merchant_demo_badge)
                    else stringResource(R.string.app_role_merchant),
                    container = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                StatusPill(
                    text = stringResource(R.string.shop_plan) + ": " +
                        stringResource(R.string.shop_plan_free),
                    container = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayCard(state: MerchantState) {
    SectionCard(title = stringResource(R.string.dashboard_today)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.todayOrders.size.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.stat_all_orders),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = money(state.todayRevenue),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.stat_revenue),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LatestOrders(state: MerchantState, onSeeOrders: () -> Unit) {
    val latest = remember(state.orders) { state.orders.sortedByDescending { it.ts }.take(4) }

    SectionCard(
        title = stringResource(R.string.dashboard_latest),
        trailing = {
            if (latest.isNotEmpty()) {
                TextButton(onClick = onSeeOrders) {
                    Text(stringResource(R.string.dashboard_see_all))
                }
            }
        }
    ) {
        if (latest.isEmpty()) {
            Text(
                text = stringResource(R.string.dashboard_no_orders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
            return@SectionCard
        }
        latest.forEachIndexed { index, order ->
            if (index > 0) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
            }
            MiniOrderRow(order)
        }
    }
}

@Composable
private fun MiniOrderRow(order: Order) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = order.buyer.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.order_no_buyer),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = rememberDateTime(order.ts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = money(order.total),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        MerchantStatusPill(order)
    }
}

/** شرابة حالة الطلب — مشتركة بين لوحة التحكم وشاشة الطلبات. */
@Composable
internal fun MerchantStatusPill(order: Order) {
    val done = !order.isNew
    StatusPill(
        text = stringResource(if (done) R.string.order_status_done else R.string.order_status_new),
        container = if (done) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (done) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onTertiaryContainer
    )
}
