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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ma.hanout.lite.BuildConfig
import ma.hanout.lite.R

/** لوحة ألوان الهوية ديال الحانوت (نفس ألوان منصة HANWEB HUB). */
private val SHOP_COLORS = listOf(
    "#003DA5", "#00843D", "#E01A2B", "#FFC220", "#7A2E9B", "#00696D"
)

/**
 * شاشة الحانوت: معلومات الحانوت، الهوية اللونية، الحساب وإعدادات المظهر.
 */
@Composable
fun MerchantShopScreen(
    state: MerchantState,
    viewModel: MerchantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var name by rememberSaveable(state.slug) { mutableStateOf(state.shop?.name.orEmpty()) }
    var phone by rememberSaveable(state.slug) { mutableStateOf(state.shop?.phone.orEmpty()) }
    var color by rememberSaveable(state.slug) { mutableStateOf(state.shop?.color.orEmpty()) }
    var active by rememberSaveable(state.slug) { mutableStateOf(state.shop?.active ?: true) }

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
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandLogo(size = 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.shop?.name?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.hub_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (state.slug.isBlank()) stringResource(R.string.no_shop_selected)
                            else state.slug,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.hub_name)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.shop_name)) }
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.shop_phone_edit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.shop_color),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                ColorChips(
                    colors = SHOP_COLORS,
                    selected = color,
                    onSelect = { color = it }
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.shop_active),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.shop_active_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveShop(name, phone, color, active) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.shop_account)) {
                KeyValueRow(
                    label = stringResource(R.string.merchant_email),
                    value = state.email.ifBlank { "—" }
                )
                Spacer(Modifier.height(8.dp))
                KeyValueRow(
                    label = stringResource(R.string.shop_signed_as),
                    value = stringResource(R.string.app_role_merchant)
                )
                Spacer(Modifier.height(8.dp))
                KeyValueRow(
                    label = stringResource(R.string.shop_slug),
                    value = state.slug.ifBlank { "—" }
                )
                Spacer(Modifier.height(8.dp))
                KeyValueRow(
                    label = stringResource(R.string.shop_plan),
                    value = stringResource(R.string.shop_plan_free)
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = viewModel::signOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.merchant_sign_out))
                }
            }
        }

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

        item { ThemeSettingsCard() }

        item {
            SectionCard(title = stringResource(R.string.hub_tagline)) {
                Text(
                    text = stringResource(R.string.hub_about),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                KeyValueRow(
                    label = stringResource(R.string.shop_version),
                    value = BuildConfig.VERSION_NAME
                )
                Spacer(Modifier.height(8.dp))
                KeyValueRow(
                    label = stringResource(R.string.shop_backend),
                    value = stringResource(
                        if (BuildConfig.FB_PROJECT_ID.isNotBlank()) R.string.backend_cloud
                        else R.string.backend_demo
                    )
                )
            }
        }
    }
}
