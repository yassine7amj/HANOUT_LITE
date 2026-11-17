package ma.hanout.lite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.hanout.lite.R
import ma.hanout.lite.data.Product

@Composable
fun CartScreen(
    state: UiState,
    viewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var buyer by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var delivery by rememberSaveable { mutableStateOf(false) }

    val lines = state.cartLines
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.cart_empty),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.cart_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val fulfillment = if (delivery) stringResource(R.string.fulfillment_delivery)
    else stringResource(R.string.fulfillment_pickup)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cart_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::clearCart) {
                    Text(stringResource(R.string.cart_clear))
                }
            }
        }

        items(lines, key = { it.first.id }) { (product, quantity) ->
            CartLineCard(
                product = product,
                quantity = quantity,
                onDecrease = { viewModel.changeQuantity(product.id, -1) },
                onIncrease = { viewModel.changeQuantity(product.id, 1) }
            )
        }

        item {
            CheckoutFields(
                buyer = buyer,
                onBuyerChange = { buyer = it },
                note = note,
                onNoteChange = { note = it },
                delivery = delivery,
                onDeliveryChange = { delivery = it }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cart_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = money(state.cartTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Button(
                onClick = {
                    when (val result = viewModel.submitOrder(buyer, fulfillment, note)) {
                        is CheckoutResult.Ready -> {
                            openWhatsApp(context, result.phone, result.text)
                            viewModel.postMessage(R.string.checkout_sent)
                            buyer = ""
                            note = ""
                            delivery = false
                        }

                        CheckoutResult.EmptyCart -> viewModel.postMessage(R.string.checkout_empty)
                        CheckoutResult.NoPhone -> viewModel.postMessage(R.string.checkout_no_phone)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.checkout))
            }
        }
    }
}

@Composable
private fun CartLineCard(
    product: Product,
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductVisual(
                emoji = product.emoji,
                image = product.image,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${money(product.price)}  ×  $quantity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = money(product.price * quantity),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDecrease) {
                Text(text = "−", fontSize = 22.sp)
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onIncrease) {
                Text(text = "+", fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun CheckoutFields(
    buyer: String,
    onBuyerChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    delivery: Boolean,
    onDeliveryChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = buyer,
                onValueChange = onBuyerChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.buyer_label)) },
                placeholder = { Text(stringResource(R.string.buyer_hint)) }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.fulfillment_label),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            FilterChip(
                selected = !delivery,
                onClick = { onDeliveryChange(false) },
                label = {
                    Text(
                        text = stringResource(R.string.fulfillment_pickup),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            FilterChip(
                selected = delivery,
                onClick = { onDeliveryChange(true) },
                label = {
                    Text(
                        text = stringResource(R.string.fulfillment_delivery),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text(stringResource(R.string.note_label)) },
                placeholder = { Text(stringResource(R.string.note_hint)) }
            )
        }
    }
}
