package com.winhub.hernandez.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

// ─────────────────────────────────────────────────────────────────────────────
// Container glossary — a plain-language "what is all this?" compendium for the
// container editor. Newcomer-facing: short, friendly definitions of the terms a
// first-time user meets on the setup screen (wrappers, DXVK/VKD3D, Mali/Adreno,
// BCn, colours, glibc/bionic, …). Searchable. Self-contained so the shortcut
// editor (or a future setup-wizard tutorial) can reuse it verbatim.
// ─────────────────────────────────────────────────────────────────────────────

internal data class GlossaryEntry(val term: String, val definition: String)
internal data class GlossarySection(val title: String, val entries: List<GlossaryEntry>)

internal val CONTAINER_GLOSSARY: List<GlossarySection> = listOf(
    GlossarySection(I18n.s(R.string.ui_graphics_translation_directx_vulkan), listOf(
        GlossaryEntry("DXVK",
            "Translates a game's DirectX 9/10/11 graphics into Vulkan — the language your phone's GPU actually speaks. It's the default and usually the fastest option."),
        GlossaryEntry("VKD3D",
            "The same idea as DXVK, but for DirectX 12 games. It needs a newer Vulkan setup, so not every wrapper or GPU can use it."),
        GlossaryEntry("WineD3D",
            "An older, slower translator that turns DirectX into OpenGL instead of Vulkan. Keep it as a fallback for games that misbehave on DXVK."),
        GlossaryEntry("VEGAS",
            I18n.s(R.string.ui_an_alternative_dxvk_style_translator_tuned_for_certain_d)),
        GlossaryEntry("DirectX (D3D9–12)",
            "The Windows graphics API games are built on — D3D9/10/11 are older, D3D12 is the newest. The wrappers above translate it into Vulkan for your phone."),
        GlossaryEntry("OpenGL",
            I18n.s(R.string.ui_an_older_cross_platform_graphics_standard_opengl_games_r)),
        GlossaryEntry("Zink",
            I18n.s(R.string.ui_translates_opengl_into_vulkan_so_opengl_based_games_and)),
    )),
    GlossarySection(I18n.s(R.string.ui_the_windows_layer), listOf(
        GlossaryEntry("Wine",
            "Runs Windows programs on Android without Windows itself — it translates a game's Windows system calls into Android/Linux ones on the fly."),
        GlossaryEntry("Proton",
            I18n.s(R.string.ui_valve_s_gaming_focused_version_of_wine_with_extra_patche)),
        GlossaryEntry("Container",
            "A self-contained virtual Windows environment for a game — its own C: drive, settings and installed components. Think of it as one sandbox per game or per configuration."),
        GlossaryEntry(I18n.s(R.string.ui_wrapper_graphics_driver),
            I18n.s(R.string.ui_the_vulkan_driver_layer_a_container_uses_to_talk_to_your)),
    )),
    GlossarySection(I18n.s(R.string.ui_running_x86_games_on_arm), listOf(
        GlossaryEntry("FEXCore",
            "Translates a game's x86/x64 (Intel/AMD) instructions into ARM so they run on your phone's processor. WinHub's primary translator (arm64ec-focused)."),
        GlossaryEntry("box64",
            "The classic x86/x64-to-ARM instruction translator — an alternative engine to FEXCore, picked per container."),
        GlossaryEntry("arm64ec",
            I18n.s(R.string.ui_a_build_format_that_lets_native_arm_code_and_translated)),
        GlossaryEntry(I18n.s(R.string.ui_fexcore_preset),
            "Trades emulation accuracy for speed by tuning how faithfully FEXCore copies the PC's memory ordering (TSO). Stability/Compatibility stay fully accurate (safest); Performance relaxes it for more FPS (can break some games); the in-between and Denuvo presets are middle grounds. Start on the default."),
    )),
    GlossarySection(I18n.s(R.string.ui_gpus_how_they_draw), listOf(
        GlossaryEntry("Vulkan",
            I18n.s(R.string.ui_the_modern_low_level_graphics_standard_your_phone_s_gpu)),
        GlossaryEntry(I18n.s(R.string.ui_mali_vs_adreno),
            "The two main phone-GPU families. Adreno (Snapdragon) has the best support here. Mali (many MediaTek/Exynos phones) works, but needs extra help with compressed textures — that's what the BCn / wrapper settings are for."),
        GlossaryEntry("Turnip",
            I18n.s(R.string.ui_the_open_source_vulkan_driver_for_adreno_gpus_that_winhu)),
        GlossaryEntry(I18n.s(R.string.ui_bcn_emulation_2),
            "PC games store textures in a compressed format (BCn / DXT / S3TC) that Adreno reads natively but Mali cannot. BCn emulation converts them on the fly so they display correctly. If it's off or incomplete on a Mali device, you get black textures in some areas."),
        GlossaryEntry("ASTC / ETC2",
            I18n.s(R.string.ui_compressed_texture_formats_that_mali_gpus_can_read_nativ)),
        GlossaryEntry("Adrenotools",
            I18n.s(R.string.ui_a_loader_that_lets_winhub_use_a_custom_gpu_driver_like_t)),
        GlossaryEntry(I18n.s(R.string.ui_vulkan_version_2),
            "The Vulkan level the driver reports to the DirectX translator (DXVK/VKD3D). A higher number can unlock features some games ask for, but it's automatically capped to what your driver really supports — so picking one above your driver does nothing."),
        GlossaryEntry(I18n.s(R.string.ui_gpu_spoofing_gpu_name),
            "Reports a different graphics card to games than your real one. Some games unlock better graphics options, or simply run, when they recognise a specific GPU. Leave it on 'Device' to report your real GPU."),
        GlossaryEntry(I18n.s(R.string.ui_max_device_memory_vram_cap),
            "Limits how much video memory the driver tells games it has. Useful when a game tries to use more than your phone can spare — a fix for out-of-memory crashes or stutter. Leave on Default unless a game misbehaves."),
    )),
    GlossarySection(I18n.s(R.string.ui_turnip_driver_tuning), listOf(
        GlossaryEntry(I18n.s(R.string.ui_gmem_tiled_rendering),
            "How the GPU draws each frame. GMEM (tiled) rendering splits the screen into small tiles and draws them in ultra-fast on-chip memory — the efficient, mobile-native way. Auto turns it on only for Adreno 710/720/722 (which need it to avoid glitches); Force On applies it to any GPU; Force Off never does. Its opposite is 'sysmem'."),
        GlossaryEntry("sysmem",
            "Forces the GPU to draw straight to main memory instead of the fast on-chip tile memory — the opposite of GMEM. The most compatible option (and the default), but uses more memory bandwidth. Use it if forcing GMEM makes a game glitch."),
        GlossaryEntry(I18n.s(R.string.graphics_driver_sync_frame),
            "Makes the GPU fully finish and show each frame before starting the next. Turn it on to fix tearing, flicker, or glitchy on-screen frames; leave it off for best performance, since it can cost some FPS by stopping the CPU and GPU from working ahead of each other."),
        GlossaryEntry("Concurrent binning (forcecb / nocb)",
            "Binning is the step where the GPU sorts what belongs in each on-screen tile. 'forcecb' forces it to run alongside rendering (can help performance); 'nocb' turns it off (can fix glitches on some games). Expert Turnip options — only touch them if a specific game needs it."),
        GlossaryEntry("deck_emu",
            "Makes the GPU report itself to the game as a Steam Deck, which can unlock better graphics settings or code paths in some games. Only works on WinHub's own (Banners-Turnip) drivers — ignored on stock Turnip."),
        GlossaryEntry("KHR_present_wait",
            "A Vulkan feature that lets a game wait until a frame has actually appeared on screen. Some GPU drivers implement it incorrectly, causing stalls or hangs — 'Disable KHR_present_wait' is the compatibility fallback that stops the driver using it."),
        GlossaryEntry("BCn emulation / Transcode to ASTC",
            "On Mali and other non-Adreno GPUs, a game's compressed BCn/DXT textures have to be emulated. The BCn 'Type' picks the decoder (a GPU compute layer or a software one), the cache reuses decoded textures to cut stutter, and 'Transcode to ASTC' repacks them into ASTC — a format mobile GPUs read in hardware — to save memory. All of this has no effect on Adreno, which reads BCn natively."),
    )),
    GlossarySection(I18n.s(R.string.ui_picture_settings), listOf(
        GlossaryEntry(I18n.s(R.string.ui_colors_rgba_vs_bgra),
            "The order of the red and blue colour channels. BGRA (the default) is correct for almost everything — only switch to RGBA if a game's colours look swapped (blue where red should be)."),
        GlossaryEntry("Render scale / supersampling",
            I18n.s(R.string.ui_renders_the_game_at_a_higher_resolution_and_shrinks_it_f)),
        GlossaryEntry(I18n.s(R.string.ui_frame_generation),
            "Inserts extra in-between frames to make motion look smoother. Interpolation always adds about one frame of input delay, and can produce occasional artifacts, so it's optional. Two engines, both running inside WinHub itself so the extra frames reach the screen directly: Win-FG Native (built in, needs nothing) and LSFG Native (runs the Lossless Scaling shaders, and needs your own imported Lossless.dll). Needs the Vulkan renderer."),
        GlossaryEntry(I18n.s(R.string.ui_fps_limiter),
            "Caps how many frames the game renders per second. A cap cuts heat and battery use and can smooth out an unstable frame rate. Turning it on just loads the limiter — you set the actual cap live from the in-game FPS menu."),
        GlossaryEntry(I18n.s(R.string.ui_native_rendering_2),
            "Sends the game's image straight to the screen instead of through WinHub's compositor — lower latency and a little faster, but it disables colour swapping (RGBA) and the present-mode control. Turn it off if a game shows wrong colours or won't display."),
        GlossaryEntry(I18n.s(R.string.ui_frame_gen_fps_numbers),
            "With frame generation on, two apps' FPS readings won't match — they count at different points. A counter that reads the game's raw output shows a clean 2x/3x/4x (impressive, but it counts frames your screen never actually displays). WinHub's HUD counts frames as they reach the display pipeline, so it shows the real gain — not a perfectly clean multiple, and always capped by your screen's refresh rate. Neither number is 'frames on glass'; your panel's refresh rate is the true ceiling."),
        GlossaryEntry("DXVK HUD / overlay",
            I18n.s(R.string.ui_an_on_screen_readout_fps_frame_times_gpu_load_driver_inf)),
        GlossaryEntry(I18n.s(R.string.ui_present_mode),
            "How finished frames are handed to your screen — a trade-off between smoothness, input lag and battery. FIFO is the safe default; Mailbox is best with frame generation; Immediate chases the lowest lag but can tear."),
        GlossaryEntry(I18n.s(R.string.ui_fifo_present_mode),
            "The default 'vsync on': every frame waits its turn and is shown on the next screen refresh. Smooth, tear-free and the most battery-friendly — but it makes the game wait for the display, which can hold frame generation back."),
        GlossaryEntry(I18n.s(R.string.ui_mailbox_present_mode),
            "'Fast vsync': the screen always takes the newest finished frame and never makes the game wait. Tear-free like FIFO but with lower lag — and the right choice for frame generation, because its extra generated frames actually get through. Uses a little more battery."),
        GlossaryEntry(I18n.s(R.string.ui_immediate_present_mode),
            "'Vsync off': frames go to the screen the instant they're ready. Lowest input lag, but you can see tearing (parts of two frames at once). Usually not worth it on a high-refresh phone screen."),
        GlossaryEntry(I18n.s(R.string.ui_shader_stutter_cache),
            I18n.s(R.string.ui_the_first_time_you_play_the_game_compiles_its_shaders_wh)),
    )),
    GlossarySection(I18n.s(R.string.ui_under_the_hood), listOf(
        GlossaryEntry(I18n.s(R.string.ui_glibc_vs_bionic),
            "Two different C libraries. Android uses 'bionic'; desktop Linux uses 'glibc'. WinHub's Wine and drivers are built for bionic — a glibc build simply won't load, no matter how good it looks."),
        GlossaryEntry(I18n.s(R.string.ui_environment_variables),
            "Advanced per-container switches (like DXVK_FRAME_RATE=60 or BCN_TRANSCODE_TO_ASTC=1). Most people never need them, but they let power users fine-tune a game."),
        GlossaryEntry("ESYNC / FSYNC",
            I18n.s(R.string.ui_wine_speed_ups_that_let_a_game_s_threads_coordinate_more)),
        GlossaryEntry(I18n.s(R.string.ui_dll_overrides),
            "Tell Wine whether to use a game's own copy of a system file (native) or Wine's built-in version. A common fix for games that ship their own DLLs."),
        GlossaryEntry("Components",
            "Extra Windows runtimes some games need (Visual C++ redistributables, .NET, DirectX, etc.). Install them from a container's Components list if a game complains one is missing."),
    )),
    GlossarySection(I18n.s(R.string.ui_controls_audio), listOf(
        GlossaryEntry(I18n.s(R.string.ui_xinput_vs_dinput),
            "The two ways Windows games read controllers. XInput is the modern Xbox-style standard; DInput is the older one. Match whichever a game expects."),
        GlossaryEntry(I18n.s(R.string.ui_exclusive_input_2),
            I18n.s(R.string.ui_hands_your_controller_to_one_game_exclusively_so_android)),
        GlossaryEntry(I18n.s(R.string.midi_sound_font),
            I18n.s(R.string.ui_a_sound_bank_used_to_play_a_game_s_midi_music_only_matte)),
        GlossaryEntry(I18n.s(R.string.ui_gyro_motion_aim),
            "Uses the phone's motion sensor to aim by tilting. Mode sets whether tilt SPEED or tilt ANGLE aims; the activator is the button that switches it on; the target is what it moves (right stick, left stick or mouse); sensitivity sets how far a tilt turns."),
        GlossaryEntry("Audio driver (ALSA / PulseAudio)",
            "How a game's sound reaches Android. ALSA is the simple, low-overhead default; PulseAudio is a fuller sound server some games or voice features need. Switch to PulseAudio if a game has no sound or crackles."),
    )),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerGlossarySheet(onDismiss: () -> Unit, initialQuery: String = "") {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cs = MaterialTheme.colorScheme
    // A per-field help icon opens the sheet pre-filtered to that term (initialQuery); the general
    // "What is all this?" button opens it unfiltered (initialQuery == "").
    var query by remember { mutableStateOf(initialQuery) }

    // Flatten + filter. A section is shown only if it has any matching entries.
    val q = query.trim()
    val sections = remember(q) {
        if (q.isEmpty()) CONTAINER_GLOSSARY
        else CONTAINER_GLOSSARY.mapNotNull { section ->
            val hits = section.entries.filter {
                it.term.contains(q, ignoreCase = true) || it.definition.contains(q, ignoreCase = true)
            }
            if (hits.isEmpty()) null else section.copy(entries = hits)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "What is all this?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                I18n.s(R.string.ui_plain_language_explanations_of_the_setup_options_tap_a_t),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(I18n.s(R.string.ui_search_terms)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.padding(top = 8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEach { section ->
                    item(key = "sec:${section.title}") {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(section.entries, key = { "${section.title}:${it.term}" }) { entry ->
                        Surface(
                            color = cs.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(entry.term, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    entry.definition,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
                if (sections.isEmpty()) {
                    item {
                        Text(
                            "No terms match \"$q\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                item { Spacer(Modifier.padding(bottom = 16.dp)) }
            }
        }
    }
}
