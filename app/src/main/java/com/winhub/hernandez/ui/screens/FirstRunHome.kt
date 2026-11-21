package com.winhub.hernandez.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.winhub.hernandez.ui.deck.DeckStore
import com.winhub.hernandez.ui.deck.XmbStoreMark
import com.winhub.hernandez.ui.deck.isSignedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

// The first-run wizard's console steps: Home (look, Home style, game settings, store style), Sounds and Stores. They write their
// choices to the same preferences Settings uses (see FirstRunViewModel), so they hold from the first launch.

/** Which page the Home step's preview device shows: the group that holds focus decides. */
internal enum class PreviewView { HOME, SETTINGS, STORES }

// ── Step: Home ───────────────────────────────────────────────────────────────

@Composable
internal fun HomeStep(vm: FirstRunViewModel, portrait: Boolean, modifier: Modifier) {
    val ctx = LocalContext.current
    val look by vm.look.collectAsState()
    val home by vm.home.collectAsState()
    val gset by vm.gameSettings.collectAsState()
    val store by vm.store.collectAsState()
    val glass = look == GlassLook.GLASS
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier.verticalScroll(rememberScrollState())) {
        StepHeading(
            HOME,
            if (portrait) "Set up your stores" else I18n.s(R.string.ui_set_up_your_home),
            "Choose how ${if (portrait) "your stores" else "Home, your stores and a game's settings"} look. All of it can be changed later in Settings, Appearance.",
        )
        if (!portrait) {
            WizOptionGroup(I18n.s(R.string.ui_interface_look), I18n.s(R.string.ui_glass_puts_every_page_on_see_through_glass_and_a_game_s), PreviewView.SETTINGS, vm) {
                WizOption("Glass", I18n.s(R.string.ui_see_through_pages_on_the_game_s_art), glass, Modifier.weight(1f), badge = I18n.s(R.string.ui_recommended_2)) { vm.setLook(ctx, GlassLook.GLASS) }
                WizOption("Material", I18n.s(R.string.ui_the_plain_deck_look), !glass, Modifier.weight(1f)) { vm.setLook(ctx, GlassLook.MATERIAL) }
            }
            WizOptionGroup(I18n.s(R.string.ui_home_style), I18n.s(R.string.ui_where_your_games_stores_and_social_live), PreviewView.HOME, vm) {
                WizOption(
                    "Steam", I18n.s(R.string.ui_one_row_of_covers_at_the_bottom_one_shelf_per_store_stor),
                    home == DeckHomeStyle.STEAM, Modifier.weight(1f), badge = I18n.s(R.string.ui_recommended_2),
                ) { vm.setHome(ctx, DeckHomeStyle.STEAM) }
                WizOption("Full XMB", "Stores and Social as cards above your games", home == DeckHomeStyle.FULL_XMB, Modifier.weight(1f)) {
                    vm.setHome(ctx, DeckHomeStyle.FULL_XMB)
                }
            }
            WizOptionGroup(I18n.s(R.string.ui_game_settings), I18n.s(R.string.ui_what_a_game_s_settings_opens_from_home), PreviewView.SETTINGS, vm) {
                WizOption(
                    "Classic", I18n.s(R.string.ui_a_settings_page_on_glass_over_the_game_s_art_small_tabs),
                    gset == GameSettingsStyle.CLASSIC, Modifier.weight(1f), badge = I18n.s(R.string.ui_recommended_2),
                ) { vm.setGameSettings(ctx, GameSettingsStyle.CLASSIC) }
                WizOption("XMB", "The settings as XMB columns beside the game", gset == GameSettingsStyle.XMB, Modifier.weight(1f)) {
                    vm.setGameSettings(ctx, GameSettingsStyle.XMB)
                }
            }
        }
        // The Glass look puts the stores on XMB pages whatever this says, so Deck stays out of reach while it is on.
        val xmb = glass || store == com.winhub.hernandez.ui.deck.StoreStyle.XMB
        WizOptionGroup(
            "Store style", if (glass) "The Glass look always uses XMB store pages" else I18n.s(R.string.ui_how_each_store_opens), PreviewView.STORES, vm,
        ) {
            WizOption("XMB", I18n.s(R.string.ui_each_store_as_its_own_full_screen_xmb_page), xmb, Modifier.weight(1f), badge = I18n.s(R.string.ui_recommended_2)) {
                vm.setStore(ctx, com.winhub.hernandez.ui.deck.StoreStyle.XMB)
            }
            WizOption("Deck", I18n.s(R.string.ui_stores_stay_inside_the_deck_pages), !xmb, Modifier.weight(1f), enabled = !glass) {
                vm.setStore(ctx, com.winhub.hernandez.ui.deck.StoreStyle.DECK)
            }
        }
        Text(
            if (portrait) I18n.s(R.string.ui_portrait_home_is_always_the_cover_grid_change_it_any_tim)
            else I18n.s(R.string.ui_home_keeps_its_own_style_whatever_the_look_change_any_of),
            color = DeckPalette.onSurfaceVariant, fontSize = 13.sp,
        )
    }
}

@Composable
private fun WizOptionGroup(title: String, hint: String, view: PreviewView, vm: FirstRunViewModel, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.onFocusChanged { if (it.hasFocus) vm.view.value = view },
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title.uppercase(), color = DeckPalette.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(hint, color = DeckPalette.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** One of a group's two choices: a compact [WizChoice] with the same select look. A disabled one is dimmed and skipped by focus. */
@Composable
private fun WizOption(
    title: String,
    body: String,
    selected: Boolean,
    modifier: Modifier,
    badge: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .heightIn(min = 68.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .deckRing(interaction, shape)
            .clip(shape)
            .background(if (selected) DeckPalette.primary.copy(alpha = 0.14f) else DeckPalette.surface)
            .border(1.dp, if (selected) DeckPalette.primary else DeckPalette.line, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = DeckPalette.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (badge != null) DeckBadge(badge, DeckPalette.primary)
            }
            Text(body, color = DeckPalette.onSurfaceVariant, fontSize = 12.sp)
        }
        Tick(selected)
    }
}

// ── Home step preview ────────────────────────────────────────────────────────

private val MOCK_ART = listOf(Color(0xFF1B1445), Color(0xFF7A2F6E), Color(0xFFF08A4B))
private val MOCK_NAVY = Color(0xFF060B16)

/** The preview device of the Home step: Home, a game's settings or the stores, as the options now chosen draw them. */
@Composable
internal fun HomePreview(vm: FirstRunViewModel, modifier: Modifier) {
    val look by vm.look.collectAsState()
    val home by vm.home.collectAsState()
    val gset by vm.gameSettings.collectAsState()
    val store by vm.store.collectAsState()
    val view by vm.view.collectAsState()
    val glass = look == GlassLook.GLASS
    val xmbStores = glass || store == com.winhub.hernandez.ui.deck.StoreStyle.XMB
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val height by animateDpAsState(minOf(230.dp, maxHeight - 56.dp), tween(320), label = I18n.s(R.string.ui_homepreviewheight))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .height(height)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF050607))
                    .border(1.dp, DeckPalette.line2, RoundedCornerShape(22.dp))
                    .padding(7.dp),
            ) {
                BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(DeckPalette.background)) {
                    when (view) {
                        PreviewView.HOME -> MockHome(home == DeckHomeStyle.STEAM)
                        PreviewView.SETTINGS -> MockSettings(glass, gset == GameSettingsStyle.CLASSIC)
                        PreviewView.STORES -> MockStores(xmbStores, glass)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(PreviewView.HOME to "Home", PreviewView.SETTINGS to I18n.s(R.string.ui_game_settings), PreviewView.STORES to "Stores").forEach { (v, label) ->
                    val on = v == view
                    Text(
                        label,
                        color = if (on) DeckPalette.primary else DeckPalette.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (on) DeckPalette.primary.copy(alpha = 0.14f) else DeckPalette.surface)
                            .border(1.dp, if (on) DeckPalette.primary else DeckPalette.line, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                "${if (glass) "Glass" else "Material"} · ${if (home == DeckHomeStyle.STEAM) "Steam" else "Full XMB"} Home · " +
                    "${if (gset == GameSettingsStyle.CLASSIC) "Classic" else "XMB"} settings · ${if (xmbStores) "XMB" else "Deck"} stores",
                color = DeckPalette.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
            )
        }
    }
}

/** A block of the mock screen: x / w are shares of its width and y / h of its height. */
@Composable
private fun BoxWithConstraintsScope.Mk(
    x: Float, y: Float, w: Float, h: Float, fill: Brush, radius: Dp = 3.dp, outline: Color? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(radius)
    Box(
        Modifier
            .offset(maxWidth * x, maxHeight * y)
            .size(maxWidth * w, maxHeight * h)
            .clip(shape)
            .background(fill)
            .then(if (outline != null) Modifier.border(1.5.dp, outline, shape) else Modifier),
        content = content,
    )
}

@Composable
private fun BoxWithConstraintsScope.MkText(x: Float, y: Float, text: String, size: Float, color: Color = Color.White, weight: FontWeight = FontWeight.Bold) {
    Text(text, color = color, fontSize = size.sp, fontWeight = weight, maxLines = 1, modifier = Modifier.offset(maxWidth * x, maxHeight * y))
}

private fun solid(c: Color) = Brush.linearGradient(listOf(c, c))

@Composable
private fun BoxWithConstraintsScope.MockArt(dim: Float) {
    Mk(0f, 0f, 1f, 1f, Brush.verticalGradient(MOCK_ART))
    if (dim > 0f) Mk(0f, 0f, 1f, 1f, solid(Color.Black.copy(alpha = dim)))
}

@Composable
private fun BoxWithConstraintsScope.MockHome(steam: Boolean) {
    MockArt(0f)
    Mk(0f, 0f, 1f, 1f, Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.5f), 0.2f to Color.Transparent, 0.6f to Color.Transparent, 1f to MOCK_NAVY.copy(alpha = 0.9f)))
    if (steam) {
        // The shelf strip, the row of covers (the first one wide), the title and the four tabs.
        MkText(0.36f, 0.04f, I18n.s(R.string.ui_all_recent_favourites_steam_epic_gog), 5f, Color.White.copy(alpha = 0.8f))
        Mk(0.05f, 0.5f, 0.35f, 0.3f, Brush.linearGradient(PREVIEW_COVERS[0].let { listOf(Color(it.first), Color(it.second)) }), 4.dp, Color.White)
        for (i in 1..4) {
            val c = PREVIEW_COVERS[i]
            Mk(0.41f + (i - 1) * 0.142f, 0.5f, 0.134f, 0.3f, Brush.linearGradient(listOf(Color(c.first), Color(c.second))), 4.dp)
        }
        MkText(0.065f, 0.815f, "Starfall Odyssey", 7f)
        MkText(0.065f, 0.885f, "▶ STEAM · INSTALLED", 4.5f, Color.White.copy(alpha = 0.9f), FontWeight.SemiBold)
        listOf("STORES", "DOWNLOADS", "FRIENDS", "SOCIAL").forEachIndexed { i, t -> MkText(0.3f + i * 0.13f, 0.93f, t, 4.5f) }
    } else {
        // Store and Social cards above the games row, the focused game drawn larger.
        listOf("S", "E", "G", "A", "X", "+").forEachIndexed { i, t ->
            Mk(0.11f + i * 0.13f, 0.12f, 0.1f, 0.18f, Brush.verticalGradient(XMB_PLAIN_CARD), 5.dp) {
                Text(t, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
            }
        }
        for (i in 0..4) {
            val c = PREVIEW_COVERS[i]
            val on = i == 2
            Mk(0.08f + i * 0.17f, if (on) 0.42f else 0.48f, if (on) 0.17f else 0.14f, if (on) 0.34f else 0.28f, Brush.linearGradient(listOf(Color(c.first), Color(c.second))), 4.dp, if (on) Color.White else null)
        }
        MkText(0.36f, 0.8f, "Starfall Odyssey", 7f)
    }
}

@Composable
private fun BoxWithConstraintsScope.MockSettings(glass: Boolean, classic: Boolean) {
    if (glass) MockArt(0.55f) else Mk(0f, 0f, 1f, 1f, solid(DeckPalette.background))
    val card = if (glass) solid(Color.White.copy(alpha = 0.10f)) else solid(DeckPalette.surface)
    val row = if (glass) solid(Color.White.copy(alpha = 0.10f)) else solid(DeckPalette.surfaceVariant)
    if (classic) {
        Mk(0.06f, 0.08f, 0.88f, 0.84f, card, 8.dp, if (glass) null else DeckPalette.line)
        listOf(I18n.s(R.string.ui_graphics), I18n.s(R.string.ui_audio), I18n.s(R.string.ui_input), "Advanced").forEachIndexed { i, t ->
            MkText(0.09f, 0.14f + i * 0.13f, t, 6f, if (i == 0) DeckPalette.primary else DeckPalette.onSurfaceVariant)
        }
        listOf("Resolution", I18n.s(R.string.ui_frame_limit), I18n.s(R.string.ui_upscaler), "Renderer").forEachIndexed { i, t ->
            Mk(0.26f, 0.13f + i * 0.19f, 0.64f, 0.14f, row, 5.dp) { Text(t, color = Color.White, fontSize = 6.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)) }
        }
    } else {
        Mk(0.08f, 0.14f, 0.16f, 0.3f, Brush.linearGradient(listOf(Color(PREVIEW_COVERS[0].first), Color(PREVIEW_COVERS[0].second))), 6.dp, Color.White)
        listOf(0.30f to listOf(I18n.s(R.string.ui_graphics), I18n.s(R.string.ui_audio), I18n.s(R.string.ui_input)), 0.52f to listOf("Resolution", I18n.s(R.string.ui_frame_limit)), 0.74f to listOf("60 FPS", "Off")).forEachIndexed { c, (x, items) ->
            items.forEachIndexed { i, t ->
                Mk(x, 0.16f + i * 0.2f, 0.19f, 0.14f, if (c == 0 && i == 0) solid(DeckPalette.primary.copy(alpha = 0.3f)) else row, 5.dp, if (c == 0 && i == 0) Color.White else null) {
                    Text(t, color = Color.White, fontSize = 6.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.MockStores(xmb: Boolean, glass: Boolean) {
    if (xmb) {
        MockArt(0.55f)
        Mk(0.04f, 0.1f, 0.07f, 0.8f, solid(Color.White.copy(alpha = 0.10f)), 6.dp)
        listOf("LIBRARY", "STORE", "FREE", "ACCOUNT").forEachIndexed { i, t -> MkText(0.15f + i * 0.14f, 0.08f, t, 5f, Color.White.copy(alpha = if (i == 0) 1f else 0.7f)) }
        for (r in 0..1) for (i in 0..4) {
            val c = PREVIEW_COVERS[(i + r * 3) % PREVIEW_COVERS.size]
            Mk(0.15f + i * 0.16f, 0.2f + r * 0.4f, 0.14f, 0.34f, Brush.linearGradient(listOf(Color(c.first), Color(c.second))), 4.dp, if (r == 0 && i == 1) Color.White else null)
        }
    } else {
        Mk(0f, 0f, 1f, 1f, solid(DeckPalette.background))
        Mk(0f, 0f, 1f, 0.11f, solid(DeckPalette.surface))
        listOf("Home", "Stores", "Storage", "Settings").forEachIndexed { i, t -> MkText(0.05f + i * 0.13f, 0.025f, t, 5.5f, if (i == 1) DeckPalette.primary else DeckPalette.onSurfaceVariant) }
        listOf("Steam", "Epic", "GOG", "Amazon").forEachIndexed { i, t ->
            Mk(0.04f, 0.18f + i * 0.16f, 0.17f, 0.12f, solid(if (i == 0) DeckPalette.primary.copy(alpha = 0.18f) else DeckPalette.surface), 5.dp) {
                Text(t, color = if (i == 0) DeckPalette.primary else DeckPalette.onSurfaceVariant, fontSize = 6.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
            }
        }
        for (r in 0..1) for (i in 0..3) {
            val c = PREVIEW_COVERS[(i + r * 2) % PREVIEW_COVERS.size]
            Mk(0.26f + i * 0.18f, 0.18f + r * 0.4f, 0.16f, 0.34f, Brush.linearGradient(listOf(Color(c.first), Color(c.second))), 4.dp, if (r == 0 && i == 0) DeckPalette.primary else null)
        }
    }
}

// ── Step: Sounds ─────────────────────────────────────────────────────────────

@Composable
internal fun SoundsStep(vm: FirstRunViewModel, modifier: Modifier) {
    val ctx = LocalContext.current
    val snd by vm.sounds.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier.verticalScroll(rememberScrollState())) {
        StepHeading(SOUNDS, I18n.s(R.string.ui_pick_your_sounds), I18n.s(R.string.ui_the_d_pad_and_buttons_make_a_sound_as_you_move_through_d))
        WizChoice(
            title = I18n.s(R.string.ui_steamos), body = I18n.s(R.string.ui_app_own_soft_set_plays_at_the_media_volume), selected = snd == DeckSound.STEAMOS,
            onClick = { vm.setSounds(ctx, DeckSound.STEAMOS) }, badge = I18n.s(R.string.ui_recommended_2),
        ) { SoundGlyph("♪") }
        WizChoice(
            title = I18n.s(R.string.ui_phone), body = I18n.s(R.string.ui_system_click_sounds_follows_your_touch_sounds_switch), selected = snd == DeckSound.PHONE,
            onClick = { vm.setSounds(ctx, DeckSound.PHONE) },
        ) { SoundGlyph("◉") }
        WizChoice(
            title = I18n.s(R.string.ui_off), body = I18n.s(R.string.ui_no_navigation_sounds), selected = snd == DeckSound.OFF,
            onClick = { vm.setSounds(ctx, DeckSound.OFF) },
        ) { SoundGlyph("∅") }
        Text(I18n.s(R.string.ui_change_it_any_time_in_settings_navigation_sounds), color = DeckPalette.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun SoundGlyph(symbol: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DeckPalette.surfaceVariant)
            .border(1.dp, DeckPalette.line2, RoundedCornerShape(12.dp)),
    ) { Text(symbol, color = DeckPalette.primary, fontSize = 22.sp) }
}

@Composable
internal fun SoundTry(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        DeckGroup(I18n.s(R.string.ui_try_it)) {
            DeckRow(I18n.s(R.string.ui_move), I18n.s(R.string.ui_stepping_along_a_list)) { DeckButton("Play", primary = false) { DeckSound.play(DeckSound.MOVE) } }
            DeckRow("Select", I18n.s(R.string.ui_opening_something)) { DeckButton("Play", primary = false) { DeckSound.play(DeckSound.SELECT) } }
            DeckRow("Back", I18n.s(R.string.ui_going_up_a_level)) { DeckButton("Play", primary = false) { DeckSound.play(DeckSound.BACK) } }
            DeckRow(I18n.s(R.string.ui_tab), I18n.s(R.string.ui_changing_page)) { DeckButton("Play", primary = false) { DeckSound.play(DeckSound.TAB) } }
        }
    }
}

// ── Step: Stores ─────────────────────────────────────────────────────────────

@Composable
internal fun StoresStep(modifier: Modifier) {
    val ctx = LocalContext.current
    // A store's sign-in is its own screen: the list is read again each time the wizard comes back to the front.
    var resumes by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumes++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val signedIn by produceState(emptyMap<DeckStore, Boolean>(), resumes) {
        value = withContext(Dispatchers.IO) { DeckStore.entries.associateWith { isSignedIn(ctx, it) } }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.verticalScroll(rememberScrollState())) {
        StepHeading(
            STORES, I18n.s(R.string.ui_sign_in_to_your_stores),
            I18n.s(R.string.ui_sign_in_to_the_stores_you_have_and_every_game_you_own_sh),
        )
        DeckStore.entries.forEach { store ->
            StoreLoginRow(store, signedIn[store] == true) { launchDeckLogin(ctx, store.screen) }
        }
        Text(
            I18n.s(R.string.ui_each_store_opens_its_own_sign_in_come_back_here_when_you),
            color = DeckPalette.onSurfaceVariant, fontSize = 13.sp,
        )
    }
}

@Composable
private fun StoreLoginRow(store: DeckStore, signedIn: Boolean, onSignIn: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .deckRing(interaction, shape)
            .clip(shape)
            .background(if (signedIn) DeckPalette.good.copy(alpha = 0.08f) else DeckPalette.surface)
            .border(1.dp, if (signedIn) DeckPalette.good.copy(alpha = 0.45f) else DeckPalette.line, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = !signedIn, onClick = onSignIn)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF111111)),
        ) { XmbStoreMark(store.mark(), Modifier, 28.dp) }
        Column(Modifier.weight(1f)) {
            Text(store.label, color = DeckPalette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                if (signedIn) "Signed in" else I18n.s(R.string.ui_not_signed_in),
                color = if (signedIn) DeckPalette.good else DeckPalette.onSurfaceVariant, fontSize = 13.sp,
            )
        }
        if (signedIn) {
            Icon(Icons.Filled.Check, contentDescription = I18n.s(R.string.ui_signed_in_2), tint = DeckPalette.good, modifier = Modifier.size(22.dp))
        } else {
            Text(
                I18n.s(R.string.ui_sign_in_2), color = DeckPalette.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(DeckPalette.primary).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
internal fun StorePerks(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        DeckGroup(I18n.s(R.string.ui_what_you_get)) {
            DeckRow(I18n.s(R.string.ui_your_libraries_on_home), I18n.s(R.string.ui_one_shelf_per_store_installed_games_first)) {}
            DeckRow(I18n.s(R.string.ui_install_in_one_press), I18n.s(R.string.ui_a_game_opens_straight_on_its_own_page_b_comes_back_to_ho)) {}
            DeckRow(I18n.s(R.string.ui_saves_and_downloads), I18n.s(R.string.ui_cloud_saves_and_the_download_queue_follow_your_accounts)) {}
        }
    }
}
