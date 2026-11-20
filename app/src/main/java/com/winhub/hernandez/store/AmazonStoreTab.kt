package com.winhub.hernandez.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.winhub.hernandez.ui.components.Button
import androidx.compose.material3.MaterialTheme
import com.winhub.hernandez.ui.components.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winhub.hernandez.ui.deck.XmbGroupItem
import com.winhub.hernandez.ui.deck.rememberXmbFocusGroup
import com.winhub.hernandez.ui.deck.xmbCardFill
import com.winhub.hernandez.ui.deck.xmbCardLine
import com.winhub.hernandez.ui.deck.xmbGlassCard
import com.winhub.hernandez.ui.deck.xmbInner
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/**
 * The Amazon **Store** tab.
 *
 * Amazon Games has no public catalog: Prime Gaming's monthly claims and the store itself are only
 * reachable signed-in on gaming.amazon.com, and the distribution API the app holds a token for
 * serves the library, not a storefront. So this tab is honest about it — a Prime Gaming landing
 * card whose buttons open the real pages in the in-app WebView (the Amazon sign-in already runs
 * through a WebView, so the cookie jar usually carries the session), then rails cut from the
 * user's own entitlements: Ready to install · Updates available · Installed. Search filters the
 * same set. Every card opens the app's own Amazon detail page.
 */
@Composable
fun AmazonStoreTab(
    wide: Boolean,
    state: AmazonLibraryState,
    onOpen: (AmazonGame) -> Unit,
    onOpenWeb: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }

    val items = remember(state.games, state.installedIds, state.posterTick) {
        state.games.map { g -> AmazonLibraryRepo.toCatalogItem(ctx, g) to g }
    }
    val byId = remember(items) { items.associate { it.first.id to it.second } }
    val installed = remember(items, state.installedIds) { items.map { it.first }.filter { it.id in state.installedIds } }
    val notInstalled = remember(items, state.installedIds) { items.map { it.first }.filter { it.id !in state.installedIds } }
    val updatable = remember(items, state.updatableIds) { items.map { it.first }.filter { it.id in state.updatableIds } }
    val hero = remember(notInstalled, installed) {
        (notInstalled + installed).firstOrNull { !it.imageUrl.isNullOrBlank() }
    }

    fun actionFor(item: CatalogItem): CatalogAction = CatalogAction.Open(item.id in state.installedIds)
    fun open(item: CatalogItem) { byId[item.id]?.let(onOpen) }

    val results = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.length < 2) emptyList() else items.map { it.first }.filter { it.title.lowercase().contains(q) }
    }

    val header = rememberXmbHeader()
    CompositionLocalProvider(LocalXmbHeader provides header) {
        Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            header.Header {
                CatalogSearchField(
                    query = query,
                    placeholder = I18n.s(R.string.ui_search_your_amazon_games),
                    onQueryChange = { query = it },
                    onClear = { query = ""; focusManager.clearFocus() },
                    onSearch = { focusManager.clearFocus() },
                )
            }
            if (query.trim().length >= 2) {
                CatalogResults(
                    results = results,
                    searching = false,
                    query = query.trim(),
                    wide = wide,
                    storeLabel = "Amazon",
                    emptyBody = I18n.s(R.string.ui_nothing_in_your_amazon_library_matched_that_new_prime_ga),
                    actionFor = ::actionFor,
                    onOpen = ::open,
                    onAction = ::open,
                    modifier = Modifier.weight(1f),
                )
                return@Column
            }

            val prime: @Composable () -> Unit = { PrimeGamingCard(onOpenWeb, Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) }
            val heroCard: @Composable () -> Unit = {
                hero?.let { h ->
                    CatalogHeroCard(
                        item = h,
                        action = actionFor(h),
                        storeLabel = "Amazon",
                        onOpen = { open(h) },
                        onAction = { open(h) },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        eyebrow = if (h.id in state.installedIds) "FROM YOUR LIBRARY" else I18n.s(R.string.ui_ready_to_install),
                    )
                }
            }

            if (wide) {
                Row(modifier = Modifier.weight(1f).fillMaxSize()) {
                    LazyColumn(
                        state = rememberTrackedListState(),
                        modifier = Modifier.weight(0.42f).fillMaxHeight().focusGroup(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        item(key = "prime") { prime() }
                        item(key = "hero") { heroCard() }
                        if (notInstalled.isNotEmpty()) {
                            item(key = "ni_hdr") { StoreSectionHeader("Ready to install", "${notInstalled.size}") }
                            items(notInstalled, key = { "ni_${it.id}" }) { g ->
                                CatalogResultRow(g, actionFor(g), "Amazon", { open(g) }, { open(g) },
                                    Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                            }
                        }
                    }
                    LazyColumn(
                        state = rememberTrackedListState(),
                        modifier = Modifier.weight(0.58f).fillMaxHeight().focusGroup(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        catalogRail(I18n.s(R.string.ui_updates_available), I18n.s(R.string.ui_newer_build_on_amazon), updatable, "Amazon", ::actionFor, ::open, ::open)
                        catalogRail("Installed", I18n.s(R.string.ui_play_now), installed, "Amazon", ::actionFor, ::open, ::open)
                        if (state.games.isEmpty()) item(key = "empty") { EmptyLibraryNotice(state) }
                    }
                }
            } else {
                LazyColumn(
                    state = rememberTrackedListState(),
                    modifier = Modifier.weight(1f).fillMaxSize().focusGroup(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item(key = "prime") { prime() }
                    item(key = "hero") { heroCard() }
                    catalogRail(I18n.s(R.string.ui_ready_to_install_2), I18n.s(R.string.ui_owned_not_installed), notInstalled, "Amazon", ::actionFor, ::open, ::open)
                    catalogRail(I18n.s(R.string.ui_updates_available), I18n.s(R.string.ui_newer_build_on_amazon), updatable, "Amazon", ::actionFor, ::open, ::open)
                    catalogRail("Installed", I18n.s(R.string.ui_play_now), installed, "Amazon", ::actionFor, ::open, ::open)
                    if (state.games.isEmpty()) item(key = "empty") { EmptyLibraryNotice(state) }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryNotice(state: AmazonLibraryState) {
    if (state.isSyncing) {
        StoreNotice(title = I18n.s(R.string.ui_syncing_your_library), body = state.statusText.ifBlank { "Fetching your Amazon games…" })
    } else {
        StoreNotice(
            title = I18n.s(R.string.ui_no_amazon_games_yet),
            body = I18n.s(R.string.ui_claim_games_on_prime_gaming_and_sync_they_show_up_her),
            actionLabel = I18n.s(R.string.ui_sync_now),
            onAction = { state.refresh(true) },
        )
    }
}

/** The Prime Gaming landing card: what it is, and the two web actions that actually claim games. */
@Composable
private fun PrimeGamingCard(onOpenWeb: (url: String, title: String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    // XMB: the whole card is one stop of the D-pad; A enters its two buttons, B comes back to the card.
    val group = rememberXmbFocusGroup()
    XmbGroupItem(group, shape, modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .xmbGlassCard(shape, xmbCardFill(MaterialTheme.colorScheme.surfaceContainer))
                .border(1.dp, xmbCardLine(MaterialTheme.colorScheme.outline), shape)
                .padding(16.dp),
        ) {
            Text(
                text = I18n.s(R.string.ui_prime_gaming),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = I18n.s(R.string.ui_free_games_every_month),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = I18n.s(R.string.ui_amazon_has_no_browsable_catalog_for_third_party_apps_cla) +
                    "in the in-app browser — they land in your library here after a sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onOpenWeb("https://gaming.amazon.com/home", I18n.s(R.string.ui_prime_gaming_2)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).steamFocusRing(RoundedCornerShape(8.dp)).xmbInner(group, first = true),
                ) { Text(I18n.s(R.string.ui_claim_free_games), maxLines = 1) }
                OutlinedButton(
                    onClick = { onOpenWeb("https://gaming.amazon.com/home?filter=games", "Amazon Games") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).steamFocusRing(RoundedCornerShape(8.dp)).xmbInner(group),
                ) { Text(I18n.s(R.string.browse), maxLines = 1) }
            }
        }
    }
}
