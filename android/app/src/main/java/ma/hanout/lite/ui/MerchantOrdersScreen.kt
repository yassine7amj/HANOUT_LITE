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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ma.hanout.lite.R
import ma.hanout.lite.data.Order

/**
 * شاشة الطلبات: تصفّح، بحث، تبديل الحالة (جديد/تم)، مشاركة وحذف.
 */
@Composable
fun MerchantOrdersScreen(
    state: MerchantState,
    viewModel: MerchantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Order?>(null) }
    val chooserTitle = stringResource(R.string.share_chooser)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.ordersQuery,
                onValueChange = viewModel::setOrdersQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.orders_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.ordersQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setOrdersQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                }
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.statusFilter.isEmpty(),
                        onClick = { viewModel.setStatusFilter("") },
                        label = { Text(stringResource(R.string.orders_filter_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = state.statusFilter == Order.STATUS_NEW,
                        onClick = { viewModel.setStatusFilter(Order.STATUS_NEW) },
                        label = { Text(stringResource(R.string.order_status_new)) }
                    )
                }
                item {
                    FilterChip(
                        selected = state.statusFilter == Order.STATUS_DONE,
                        onClick = { viewModel.setStatusFilter(Order.STATUS_DONE) },
                        label = { Text(stringResource(R.string.order_status_done)) }
                    )
                }
            }
        }

        val orders = state.visibleOrders
        if (orders.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.List,
                    title = stringResource(R.string.orders_empty_filter),
                    hint = stringResource(R.string.dashboard_no_orders)
                )
            }
        } else {
            items(orders, key = { it.id }) { order ->
                MerchantOrderCard(
                    order = order,
                    onToggle = { viewModel.toggleOrderStatus(order) },
                    onShare = { shareText(context, orderSummary(order), chooserTitle) },
                    onDelete = { pendingDelete = order }
                )
            }
        }
    }

    pendingDelete?.let { order ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeOrder(order)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun MerchantOrderCard(
    order: Order,
    onToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = order.buyer.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.order_no_buyer),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = rememberDateTime(order.ts),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MerchantStatusPill(order)
            }

            Spacer(Modifier.height(10.dp))
            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(
                        text = "${item.name} × ${item.qty}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = money(item.price * item.qty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (order.fulfillment.isNotBlank() || order.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                if (order.fulfillment.isNotBlank()) {
                    KeyValueRow(
                        label = stringResource(R.string.order_fulfillment),
                        value = order.fulfillment
                    )
                }
                if (order.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    KeyValueRow(
                        label = stringResource(R.string.order_note),
                        value = order.note
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = money(order.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = if (order.isNew) Icons.Filled.CheckCircle else Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (order.isNew) R.string.order_mark_done else R.string.order_mark_new
                    )
                )
            }
        }
    }
}

/** كيفرّغ الطلب لنص كيتصيفط فالـ share sheet. */
private fun orderSummary(order: Order): String = buildString {
    append("طلب ").append(order.buyer.ifBlank { "بلا اسم" }).append('\n')
    order.items.forEach { append("• ${it.name} × ${it.qty}\n") }
    append("المجموع: ${money(order.total)}\n")
    if (order.fulfillment.isNotBlank()) append("الاستلام: ${order.fulfillment}\n")
    if (order.note.isNotBlank()) append("ملاحظة: ${order.note}\n")
}
