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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ma.hanout.lite.R
import ma.hanout.lite.data.Category
import ma.hanout.lite.data.Product

/**
 * شاشة الكتالوغ: إضافة/تعديل/حذف المنتوجات، تبديل التوفر، والحفظ فالسحابة.
 */
@Composable
fun MerchantCatalogScreen(
    state: MerchantState,
    viewModel: MerchantViewModel,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<Product?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.catalogQuery,
                onValueChange = viewModel::setCatalogQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.catalog_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.catalogQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setCatalogQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                }
            )
        }

        item {
            Button(
                onClick = viewModel::startNewProduct,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.catalog_add))
            }
        }

        if (state.dirty) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.catalog_unsaved),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = viewModel::saveCatalog, enabled = !state.busy) {
                            Text(stringResource(R.string.catalog_save_cloud))
                        }
                    }
                }
            }
        }

        val products = state.visibleProducts
        if (products.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.ShoppingCart,
                    title = stringResource(R.string.catalog_empty),
                    hint = stringResource(R.string.catalog_empty_hint)
                )
            }
        } else {
            items(products, key = { it.id }) { product ->
                CatalogRow(
                    product = product,
                    onToggleAvailable = { viewModel.toggleAvailable(product.id) },
                    onEdit = { viewModel.editProduct(product) },
                    onDelete = { pendingDelete = product }
                )
            }
        }
    }

    state.draft?.let { draft ->
        ProductDialog(
            draft = draft,
            isNew = state.draftIsNew,
            categories = state.categories,
            onDismiss = viewModel::closeDraft,
            onConfirm = viewModel::upsertProduct
        )
    }

    pendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("${stringResource(R.string.confirm_delete)}\n${product.name}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeProduct(product.id)
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
private fun CatalogRow(
    product: Product,
    onToggleAvailable: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductVisual(
                emoji = product.emoji,
                image = product.image,
                modifier = Modifier.size(54.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.category.isNotBlank()) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = money(product.price),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.hasDiscount) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = money(product.oldPrice ?: 0.0),
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = product.available, onCheckedChange = { onToggleAvailable() })
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (product.available) stringResource(R.string.product_available)
                    else stringResource(R.string.out_of_stock),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.edit))
            }
            TextButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun ProductDialog(
    draft: Product,
    isNew: Boolean,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var name by remember(draft.id) { mutableStateOf(draft.name) }
    var price by remember(draft.id) {
        mutableStateOf(if (draft.price > 0.0) draft.price.toString() else "")
    }
    var oldPrice by remember(draft.id) {
        mutableStateOf(draft.oldPrice?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var category by remember(draft.id) { mutableStateOf(draft.category) }
    var emoji by remember(draft.id) { mutableStateOf(draft.emoji) }
    var available by remember(draft.id) { mutableStateOf(draft.available) }
    var invalid by remember(draft.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isNew) R.string.catalog_new_title else R.string.catalog_edit_title
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.product_name)) }
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.product_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = oldPrice,
                    onValueChange = { oldPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.product_old_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.product_category)) }
                )
                if (categories.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.name }) { item ->
                            FilterChip(
                                selected = category == item.name,
                                onClick = { category = item.name },
                                label = { Text(item.name, maxLines = 1) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.product_emoji)) }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.product_available),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = available, onCheckedChange = { available = it })
                }
                if (invalid) {
                    Text(
                        text = stringResource(R.string.product_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = price.replace(',', '.').trim().toDoubleOrNull()
                if (name.isBlank() || parsed == null || parsed <= 0.0) {
                    invalid = true
                } else {
                    onConfirm(
                        draft.copy(
                            name = name.trim(),
                            price = parsed,
                            oldPrice = oldPrice.replace(',', '.').trim().toDoubleOrNull()
                                ?.takeIf { it > 0.0 },
                            category = category.trim(),
                            emoji = emoji.trim().ifBlank { "🛍️" },
                            available = available
                        )
                    )
                }
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
