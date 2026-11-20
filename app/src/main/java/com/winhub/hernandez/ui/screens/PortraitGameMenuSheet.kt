package com.winhub.hernandez.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.withFrameNanos
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/** One line saying what each game-menu row does (bannercode shows these under every row). */
private fun menuHint(label: String): String? = when (label) {
    "Play" -> I18n.s(R.string.ui_starts_the_game_with_its_saved_launch_choice)
    "Emulator" -> I18n.s(R.string.ui_choose_the_app_this_console_s_games_start_in)
    "Get covers" -> I18n.s(R.string.ui_fetch_the_covers_and_backgrounds_that_are_missing)
    "Remove game" -> I18n.s(R.string.ui_takes_it_off_the_library_the_rom_file_stays_on_the_devic)
    "Consoles" -> I18n.s(R.string.ui_pick_the_consoles_the_library_shows)
    "Show removed games" -> I18n.s(R.string.ui_brings_back_the_games_you_removed)
    "ROM folder" -> I18n.s(R.string.ui_where_your_roms_are)
    "Rescan library" -> I18n.s(R.string.ui_looks_for_new_and_missing_rom_files)
    "Launch method" -> I18n.s(R.string.ui_choose_how_this_game_starts_and_whether_to_remember_it)
    "Settings" -> I18n.s(R.string.ui_display_graphics_controls_and_more_for_this_game_only)
    "Game Doctor" -> I18n.s(R.string.ui_finds_why_it_won_t_run_and_offers_one_tap_fixes)
    "Game details" -> I18n.s(R.string.ui_name_steam_link_genres_year_description)
    "Search covers" -> I18n.s(R.string.ui_find_cover_art_on_steamgriddb_and_pick_one)
    "Clone to container" -> I18n.s(R.string.ui_copy_this_shortcut_into_another_container)
    "Copy to Drive C" -> I18n.s(R.string.ui_copy_the_game_folder_onto_the_container_s_c_drive_and_re)
    "Change executable" -> "Point the shortcut at a different .exe in the game's folder."
    "Add to favourites" -> I18n.s(R.string.ui_pin_this_game_to_the_favourites_shelf)
    "Remove from favourites" -> I18n.s(R.string.ui_take_this_game_off_the_favourites_shelf)
    "Add to Home Screen" -> I18n.s(R.string.ui_pin_a_launcher_icon_to_android_s_home_screen)
    "Properties" -> I18n.s(R.string.ui_container_times_played_and_total_playtime)
    "Delete" -> I18n.s(R.string.ui_asks_first_only_the_shortcut_goes_the_game_files_stay)
    else -> null
}

/**
 * Bannercode's DeckGameMenuSheet (bottom-anchored on a phone): cover, title and a line of facts on
 * top, then the game menu as grouped cards of rows. Groups: Play / everything else / destructive.
 */
@Composable
internal fun PortraitGameMenuSheet(
    title: String,
    subtitle: String,
    cover: Bitmap?,
    items: List<DeckConsoleItem>,
    onDismiss: () -> Unit,
) {
    val firstRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstRequester.requestFocus() }
    }
    val groups = remember(items) {
        val play = items.filter { it.label == "Play" }
        val danger = items.filter { it.danger }
        val rest = items.filter { it.label != "Play" && !it.danger }
        listOf(play, rest, danger).filter { it.isNotEmpty() }
    }
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler(onBack = onDismiss)
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss, indication = null, interactionSource = remember { MutableInteractionSource() }),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(shape)
                    .background(Deck.background)
                    .border(1.dp, Deck.line, shape)
                    // Swallow taps so they don't reach the dismiss scrim behind the sheet.
                    .clickable(onClick = {}, indication = null, interactionSource = remember { MutableInteractionSource() })
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(I18n.s(R.string.ui_game_menu), color = Deck.onBackground, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    val closeSource = remember { MutableInteractionSource() }
                    val closeFocused by closeSource.collectIsFocusedAsState()
                    val closeShape = RoundedCornerShape(14.dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(closeShape)
                            .border(if (closeFocused) 2.dp else 0.dp, if (closeFocused) Deck.primary else Color.Transparent, closeShape)
                            .clickable(interactionSource = closeSource, indication = null, onClick = onDismiss),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = I18n.s(R.string.wrapper_manager_close), tint = Deck.onBackground)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 14.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 54.dp, height = 76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Deck.surfaceVariant),
                    ) {
                        if (cover != null) {
                            val img = remember(cover) { cover.asImageBitmap() }
                            Image(img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = Deck.primary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Deck.onBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, color = Deck.onSurfaceVariant, fontSize = 14.sp, maxLines = 1)
                    }
                }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    groups.forEachIndexed { gi, group ->
                        val cardShape = RoundedCornerShape(20.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(Deck.surface)
                                .border(1.dp, Deck.line, cardShape),
                        ) {
                            group.forEachIndexed { i, item ->
                                if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 1.dp).background(Deck.line))
                                MenuRow(
                                    item = item,
                                    modifier = if (gi == 0 && i == 0) Modifier.focusRequester(firstRequester) else Modifier,
                                    onClick = { onDismiss(); item.onClick() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: DeckConsoleItem, onClick: () -> Unit, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val tint = if (item.danger) Deck.attention else Deck.onBackground
    val hint = menuHint(item.label)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(if (focused) Deck.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(item.icon, contentDescription = null, tint = if (item.danger) Deck.attention else Deck.primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.label, color = tint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (hint != null) Text(hint, color = Deck.onSurfaceVariant, fontSize = 14.sp, lineHeight = 18.sp)
        }
    }
}
