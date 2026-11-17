package ma.hanout.lite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ma.hanout.lite.R
import ma.hanout.lite.data.Product

@Composable
fun HomeScreen(
    state: UiState,
    viewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val shop = state.shop ?: return
    val deals = remember(shop.products) {
        shop.products.filter { it.hasDiscount }
            .sortedByDescending { (it.oldPrice ?: it.price) - it.price }
            .take(10)
    }
    val showDeals = deals.isNotEmpty() && state.query.isBlank() && state.category.isEmpty()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ShopHeader(
                name = shop.name,
                phone = shop.phone,
                productCount = shop.products.size,
                demo = state.demo
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchRow(
                query = state.query,
                onQueryChange = viewModel::setQuery
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            CategoryRow(
                categories = shop.categories.map { it.name },
                selected = state.category,
                onSelect = viewModel::setCategory
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            FilterRow(state = state, viewModel = viewModel)
        }

        if (showDeals) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionTitle(stringResource(R.string.deals_title))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(deals, key = { "deal-" + it.id }) { product ->
                            DealCard(product = product, onAdd = { viewModel.addToCart(product.id) })
                        }
                    }
                }
            }
        }

        val products = state.visibleProducts
        if (products.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyNotice(
                    text = if (state.onlyFavorites) stringResource(R.string.empty_favorites)
                    else stringResource(R.string.empty_products)
                )
            }
        } else {
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    quantity = state.cart[product.id] ?: 0,
                    favorite = product.id in state.favorites,
                    onAdd = { viewModel.addToCart(product.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(product.id) }
                )
            }
        }
    }
}

@Composable
private fun ShopHeader(name: String, phone: String, productCount: Int, demo: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.products_count, productCount),
                    style = MaterialTheme.typography.bodySmall
                )
                if (phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = phone, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (demo) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = stringResource(R.string.demo_notice),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun SearchRow(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        }
    )
}

@Composable
private fun CategoryRow(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = { onSelect("") },
                label = { Text(stringResource(R.string.all_categories)) }
            )
        }
        items(categories, key = { it }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

@Composable
private fun FilterRow(state: UiState, viewModel: StoreViewModel) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = state.onlyAvailable,
            onClick = { viewModel.setOnlyAvailable(!state.onlyAvailable) },
            label = { Text(stringResource(R.string.only_available)) }
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = state.onlyFavorites,
            onClick = { viewModel.setOnlyFavorites(!state.onlyFavorites) },
            label = { Text(stringResource(R.string.only_favorites)) }
        )
        Spacer(Modifier.weight(1f))
        Box {
            TextButton(onClick = { menuOpen = true }) {
                Text(sortLabel(state.sort))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                SortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(sortLabel(mode)) },
                        onClick = {
                            viewModel.setSort(mode)
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.Default -> stringResource(R.string.sort_default)
    SortMode.PriceAsc -> stringResource(R.string.sort_price_asc)
    SortMode.PriceDesc -> stringResource(R.string.sort_price_desc)
    SortMode.Name -> stringResource(R.string.sort_name)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun DealCard(product: Product, onAdd: () -> Unit) {
    Card(modifier = Modifier.width(150.dp)) {
        Column(Modifier.padding(10.dp)) {
            ProductVisual(
                emoji = product.emoji,
                image = product.image,
                modifier = Modifier.fillMaxWidth().height(70.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 40.dp)
            )
            PriceRow(product)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_to_cart))
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    quantity: Int,
    favorite: Boolean,
    onAdd: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Box(Modifier.fillMaxWidth()) {
                ProductVisual(
                    emoji = product.emoji,
                    image = product.image,
                    modifier = Modifier.fillMaxWidth().height(96.dp)
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 44.dp)
            )
            PriceRow(product)
            Spacer(Modifier.height(8.dp))
            if (!product.available) {
                Text(
                    text = stringResource(R.string.out_of_stock),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (quantity == 0) {
                OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_to_cart))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f)) {
                        Text("+")
                    }
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceRow(product: Product) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = money(product.price),
            style = MaterialTheme.typography.titleMedium,
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

@Composable
private fun EmptyNotice(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
