package com.winhub.hernandez.ui.screens

import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/**
 * Plain-English reference for Wine's debug channels, used by the Log Manager's "Browse all" dialog.
 *
 * Wine ships 521 of these and they are, with few exceptions, named after the DLL or subsystem whose
 * output they switch on. Hand-writing 521 accurate descriptions is not something anyone can do
 * honestly, so this works in two tiers:
 *
 *  1. [DESCRIPTIONS] — channels worth knowing, described specifically. These are the ones a user is
 *     plausibly here to find: the tracing channels, the graphics stack, audio, input, networking.
 *  2. [categoryOf] — a family for every remaining channel, so the tail still gets a true statement
 *     ("Direct3D and graphics — output from Wine's `d3dxof` component") instead of an invented one.
 *
 * The rule for tier 1: if the specific behaviour isn't known, leave it out and let the category
 * answer. A vague-but-true line is more use than a confident-sounding guess about which component
 * logs what.
 */
object WineChannelInfo {

    const val CAT_TRACING = I18n.s(R.string.ui_errors_and_tracing)
    const val CAT_GRAPHICS = I18n.s(R.string.ui_direct3d_and_graphics)
    const val CAT_AUDIO = "Sound"
    const val CAT_VIDEO = I18n.s(R.string.ui_video_and_media_playback)
    const val CAT_UI = I18n.s(R.string.ui_windows_menus_and_controls)
    const val CAT_INPUT = I18n.s(R.string.ui_input_and_devices)
    const val CAT_NET = I18n.s(R.string.ui_networking_and_the_internet)
    const val CAT_FILES = I18n.s(R.string.ui_files_and_storage)
    const val CAT_RUNTIME = I18n.s(R.string.ui_processes_threads_and_memory)
    const val CAT_REGISTRY = I18n.s(R.string.ui_registry_and_settings)
    const val CAT_SECURITY = I18n.s(R.string.ui_security_and_encryption)
    const val CAT_COM = I18n.s(R.string.ui_com_scripting_and_automation)
    const val CAT_TEXT = I18n.s(R.string.ui_fonts_and_text)
    const val CAT_PRINT = "Printing"
    const val CAT_INSTALL = I18n.s(R.string.ui_installers_and_packaging)
    const val CAT_HOST = I18n.s(R.string.ui_the_android_side)
    const val CAT_OTHER = I18n.s(R.string.ui_everything_else)

    /** Order the browse dialog groups them in — most useful first, not alphabetical. */
    val CATEGORY_ORDER = listOf(
        CAT_TRACING, CAT_GRAPHICS, CAT_AUDIO, CAT_VIDEO, CAT_INPUT, CAT_UI, CAT_RUNTIME,
        CAT_FILES, CAT_NET, CAT_REGISTRY, CAT_SECURITY, CAT_COM, CAT_TEXT, CAT_PRINT,
        CAT_INSTALL, CAT_HOST, CAT_OTHER,
    )

    /** Short note on what each category is for, shown once above its group. */
    fun categoryBlurb(category: String): String = when (category) {
        CAT_TRACING -> I18n.s(R.string.ui_start_here_these_decide_how_much_detail_everything_else)
        CAT_GRAPHICS -> I18n.s(R.string.ui_the_graphics_stack_where_most_game_problems_actually_liv)
        CAT_AUDIO -> I18n.s(R.string.ui_sound_output_mixing_and_the_audio_apis_games_use)
        CAT_VIDEO -> I18n.s(R.string.ui_cutscenes_codecs_and_media_playback)
        CAT_INPUT -> I18n.s(R.string.ui_controllers_mouse_keyboard_and_other_hardware)
        CAT_UI -> I18n.s(R.string.ui_windows_dialogs_and_the_standard_windows_controls)
        CAT_RUNTIME -> I18n.s(R.string.ui_how_the_program_runs_processes_threads_memory_exceptions)
        CAT_FILES -> I18n.s(R.string.ui_reading_and_writing_files_drives_and_the_filesystem)
        CAT_NET -> I18n.s(R.string.ui_anything_that_talks_over_a_network)
        CAT_REGISTRY -> I18n.s(R.string.ui_the_windows_registry_and_configuration)
        CAT_SECURITY -> I18n.s(R.string.ui_certificates_encryption_credentials_and_permissions)
        CAT_COM -> "Windows' component/automation plumbing, used heavily by launchers."
        CAT_TEXT -> I18n.s(R.string.ui_font_loading_and_text_rendering)
        CAT_PRINT -> I18n.s(R.string.ui_printers_and_print_jobs)
        CAT_INSTALL -> I18n.s(R.string.ui_game_and_dependency_installers)
        CAT_HOST -> I18n.s(R.string.ui_where_wine_meets_android_the_display_driver_and_sound_ba)
        else -> I18n.s(R.string.ui_components_without_an_obvious_home_mostly_niche_windows)
    }

    /**
     * The channels worth describing specifically. Everything absent from this map falls through to
     * the category sentence, which is the point — see the class note.
     */
    private val DESCRIPTIONS: Map<String, String> = mapOf(
        // ── Tracing. The ones that change the character of the whole log. ──────────────────
        "err" to I18n.s(R.string.ui_real_errors_on_by_default_and_the_first_thing_to_read),
        "warn" to I18n.s(R.string.ui_warnings_something_unexpected_but_not_fatal_on_by_def),
        "fixme" to I18n.s(R.string.ui_wine_admitting_a_feature_is_missing_or_faked_very_oft),
        "seh" to "Windows exceptions as they are thrown. Invaluable for a crash, but games throw " +
            "these constantly, so it makes logs huge and slows the game down noticeably.",
        "relay" to "Logs every call in and out of a DLL. Extremely detailed and extremely slow — " +
            "useful for a specific hang, unusable as a general setting.",
        "snoop" to I18n.s(R.string.ui_like_relay_but_for_calls_wine_did_not_build_itself_sa),
        "exception" to I18n.s(R.string.ui_exception_handling_as_it_unwinds_pairs_with_seh),
        "unwind" to I18n.s(R.string.ui_stack_unwinding_during_exception_handling),
        "debugstr" to "Text the program itself prints with OutputDebugString — often the " +
            "developer's own messages.",
        "debug_buffer" to I18n.s(R.string.ui_contents_of_buffers_passed_around_dumped_as_hex),

        // ── Graphics ──────────────────────────────────────────────────────────────────────
        "d3d" to I18n.s(R.string.ui_direct3d_core_the_general_starting_point_for_a_graphi),
        "d3d8" to I18n.s(R.string.ui_direct3d_8_older_games),
        "d3d9" to I18n.s(R.string.ui_direct3d_9_the_most_common_api_for_games_of_that_era),
        "d3d10" to I18n.s(R.string.ui_direct3d_10),
        "d3d10core" to I18n.s(R.string.ui_direct3d_10_core_layer),
        "d3d11" to I18n.s(R.string.ui_direct3d_11_what_most_modern_games_in_winhub_use),
        "d3d12" to I18n.s(R.string.ui_direct3d_12_translated_by_vkd3d),
        "d3d_shader" to I18n.s(R.string.ui_shader_compilation_try_this_for_missing_or_corrupted),
        "d3d_decl" to I18n.s(R.string.ui_vertex_declarations_how_geometry_is_described_to_the),
        "d3dcompiler" to I18n.s(R.string.ui_the_shader_compiler_itself),
        "d3dx" to I18n.s(R.string.ui_the_d3dx_helper_library_games_use_for_textures_maths),
        "dxgi" to "DXGI — swapchains, display modes and presentation. Good for black-screen, " +
            "fullscreen and refresh-rate problems.",
        "ddraw" to I18n.s(R.string.ui_directdraw_2d_and_pre_direct3d_8_titles),
        "vulkan" to I18n.s(R.string.ui_vulkan_calls_this_is_what_dxvk_and_vkd3d_ultimately_p),
        "opengl" to I18n.s(R.string.ui_opengl_calls),
        "wgl" to I18n.s(R.string.ui_the_glue_between_opengl_and_windows_windows),
        "gl_compat" to I18n.s(R.string.ui_opengl_compatibility_workarounds),
        "gdi" to I18n.s(R.string.ui_gdi_classic_2d_windows_drawing_menus_and_launchers_us),
        "gdiplus" to I18n.s(R.string.ui_gdi_the_newer_2d_drawing_library),
        "d2d" to I18n.s(R.string.ui_direct2d),
        "dcomp" to I18n.s(R.string.ui_directcomposition_desktop_composition_of_windows),
        "dwmapi" to I18n.s(R.string.ui_desktop_window_manager_compositing_and_window_effects),
        "bitblt" to I18n.s(R.string.ui_bitmap_block_transfers_the_workhorse_of_2d_drawing),
        "bitmap" to I18n.s(R.string.ui_bitmap_handling),
        "palette" to I18n.s(R.string.ui_colour_palettes_used_by_older_games),
        "dxcore" to I18n.s(R.string.ui_dxcore_how_the_app_enumerates_graphics_adapters),
        "dxva2" to I18n.s(R.string.ui_hardware_video_acceleration),
        "asmshader" to I18n.s(R.string.ui_assembly_shader_parsing_for_older_shader_models),

        // ── Audio ─────────────────────────────────────────────────────────────────────────
        "dsound" to I18n.s(R.string.ui_directsound_the_usual_audio_api_for_older_games),
        "dsound3d" to I18n.s(R.string.ui_directsound_3d_positional_audio),
        "xaudio2" to I18n.s(R.string.ui_xaudio2_the_usual_audio_api_for_modern_games),
        "mmdevapi" to I18n.s(R.string.ui_the_modern_windows_audio_device_api_wasapi),
        "coreaudio" to I18n.s(R.string.ui_core_audio_routing),
        "winmm" to I18n.s(R.string.ui_multimedia_legacy_sound_midi_and_timers),
        "midi" to I18n.s(R.string.ui_midi_playback),
        "wavemap" to I18n.s(R.string.ui_wave_audio_mapping_between_formats),
        "sound" to I18n.s(R.string.ui_general_sound_output),
        "alsa" to I18n.s(R.string.ui_the_alsa_backend_one_way_sound_leaves_wine_on_the_and),
        "pulse" to I18n.s(R.string.ui_the_pulseaudio_backend),
        "oss" to I18n.s(R.string.ui_the_oss_audio_backend),
        "dmusic" to I18n.s(R.string.ui_directmusic),
        "dmsynth" to I18n.s(R.string.ui_the_directmusic_software_synthesiser),
        "msacm" to I18n.s(R.string.ui_audio_compression_and_decompression),

        // ── Video / media ─────────────────────────────────────────────────────────────────
        "quartz" to I18n.s(R.string.ui_directshow_cutscene_and_video_playback_try_this_when),
        "mfplat" to I18n.s(R.string.ui_media_foundation_the_modern_video_pipeline_also_commo),
        "evr" to I18n.s(R.string.ui_the_enhanced_video_renderer),
        "devenum" to I18n.s(R.string.ui_enumeration_of_installed_codecs_and_capture_devices),
        "avifile" to I18n.s(R.string.ui_avi_file_handling),
        "mciavi" to I18n.s(R.string.ui_avi_playback_through_the_mci_interface),
        "wmvcore" to I18n.s(R.string.ui_windows_media_video),
        "msvideo" to I18n.s(R.string.ui_video_compression_and_decompression),
        "mmio" to "Multimedia file I/O.",

        // ── Input ─────────────────────────────────────────────────────────────────────────
        "dinput" to I18n.s(R.string.ui_directinput_controllers_and_joysticks_in_older_games),
        "xinput" to I18n.s(R.string.ui_xinput_the_modern_gamepad_api_try_this_when_a_control),
        "rawinput" to I18n.s(R.string.ui_raw_input_mouse_and_keyboard_straight_from_the_device),
        "hid" to I18n.s(R.string.ui_usb_hid_devices_which_is_what_most_controllers_are),
        "joycpl" to I18n.s(R.string.ui_the_joystick_control_panel),
        "keyboard" to I18n.s(R.string.ui_keyboard_handling),
        "cursor" to I18n.s(R.string.ui_mouse_cursor_worth_trying_for_an_invisible_or_stuck_c),
        "input" to I18n.s(R.string.ui_general_input_dispatch),
        "usb" to I18n.s(R.string.ui_usb_devices),
        "wintab32" to I18n.s(R.string.ui_graphics_tablet_input),

        // ── Windows and controls ──────────────────────────────────────────────────────────
        "win" to I18n.s(R.string.ui_window_creation_and_management_broad_and_useful_for_w),
        "message" to I18n.s(R.string.ui_the_windows_message_loop_every_event_the_program_rece),
        "msg" to I18n.s(R.string.ui_individual_window_messages),
        "nonclient" to I18n.s(R.string.ui_title_bars_borders_and_window_frames),
        "dialog" to I18n.s(R.string.ui_dialog_boxes),
        "menu" to I18n.s(R.string.ui_menus),
        "user" to I18n.s(R.string.ui_user32_the_core_windowing_library),
        "clipboard" to I18n.s(R.string.ui_copy_and_paste),
        "dragdrop" to I18n.s(R.string.ui_drag_and_drop),
        "hook" to I18n.s(R.string.ui_windows_hooks_which_intercept_events_overlays_and_ant),
        "theme_scroll" to I18n.s(R.string.ui_themed_scrollbar_drawing),
        "uxtheme" to I18n.s(R.string.ui_visual_themes_and_control_styling),
        "commctrl" to I18n.s(R.string.ui_the_standard_windows_controls_as_a_group),
        "commdlg" to "The standard file/print/colour dialogs.",
        "edit" to I18n.s(R.string.ui_text_edit_boxes),
        "listbox" to I18n.s(R.string.ui_list_boxes),
        "listview" to I18n.s(R.string.ui_list_views),
        "combo" to I18n.s(R.string.ui_combo_boxes_drop_downs),
        "button" to I18n.s(R.string.ui_buttons),
        "toolbar" to I18n.s(R.string.ui_toolbars),
        "tooltips" to I18n.s(R.string.ui_tooltips),
        "treeview" to I18n.s(R.string.ui_tree_views),
        "statusbar" to I18n.s(R.string.ui_status_bars),
        "systray" to "The system tray / notification area.",
        "shell" to I18n.s(R.string.ui_the_windows_shell_folders_icons_and_file_associations),
        "explorerframe" to I18n.s(R.string.ui_explorer_window_framing),

        // ── Runtime ───────────────────────────────────────────────────────────────────────
        "process" to I18n.s(R.string.ui_process_creation_and_exit_good_for_a_game_that_closes),
        "thread" to I18n.s(R.string.ui_thread_creation_and_scheduling),
        "threadpool" to I18n.s(R.string.ui_the_thread_pool),
        "module" to I18n.s(R.string.ui_dll_loading_which_libraries_the_game_pulls_in_very_us),
        "loaddll" to I18n.s(R.string.ui_each_dll_as_it_loads_one_line_apiece_cheap_and_often),
        "unloaddll" to I18n.s(R.string.ui_dlls_being_unloaded),
        "heap" to I18n.s(R.string.ui_memory_allocation_verbose_but_the_place_to_look_for_m),
        "globalmem" to I18n.s(R.string.ui_global_memory_allocation),
        "sync" to I18n.s(R.string.ui_locks_mutexes_and_synchronisation_the_channel_for_a_h),
        "ntdll" to I18n.s(R.string.ui_the_lowest_level_windows_library_broad_and_noisy),
        "kernelbase" to I18n.s(R.string.ui_core_kernel_api_calls),
        "ntoskrnl" to I18n.s(R.string.ui_kernel_side_calls_used_by_drivers_and_anti_cheat),
        "syslevel" to I18n.s(R.string.ui_legacy_16_bit_locking),
        "environ" to I18n.s(R.string.ui_environment_variables_as_the_process_sees_them),
        "exec" to I18n.s(R.string.ui_program_execution),
        "toolhelp" to I18n.s(R.string.ui_process_and_module_enumeration_used_by_tools_and_anti),
        "vcruntime" to I18n.s(R.string.ui_the_visual_c_runtime),
        "msvcrt" to I18n.s(R.string.ui_the_microsoft_c_runtime_the_standard_library_most_gam),
        "msvcp" to I18n.s(R.string.ui_the_microsoft_c_standard_library),
        "concrt" to I18n.s(R.string.ui_microsoft_s_concurrency_runtime),
        "vcomp" to I18n.s(R.string.ui_openmp_parallel_processing),
        "wow" to "32-bit-on-64-bit translation.",
        "int" to I18n.s(R.string.ui_dos_era_interrupt_handling),
        "int21" to I18n.s(R.string.ui_dos_interrupt_21h_for_very_old_software),
        "vxd" to I18n.s(R.string.ui_windows_9x_virtual_device_drivers),

        // ── Files, registry ───────────────────────────────────────────────────────────────
        "file" to I18n.s(R.string.ui_file_opens_reads_and_writes_broad_and_the_channel_for),
        "reg" to I18n.s(R.string.ui_registry_reads_and_writes_good_for_a_game_that_won_t),
        "storage" to I18n.s(R.string.ui_structured_storage_files),
        "volume" to I18n.s(R.string.ui_drives_and_volumes),
        "mountmgr" to I18n.s(R.string.ui_drive_letter_assignment_relevant_to_how_winhub_maps_y),
        "cabinet" to I18n.s(R.string.ui_cab_archive_extraction_used_by_installers),
        "profile" to I18n.s(R.string.ui_ini_file_reads_and_writes_older_games_keep_settings_t),
        "path" to I18n.s(R.string.ui_path_handling_and_conversion_between_windows_and_linu),
        "dosmem" to I18n.s(R.string.ui_dos_memory_emulation),

        // ── Networking ────────────────────────────────────────────────────────────────────
        "winsock" to I18n.s(R.string.ui_sockets_the_base_of_nearly_all_networking_start_here),
        "wininet" to "The classic HTTP/FTP library, used by launchers and updaters.",
        "winhttp" to I18n.s(R.string.ui_the_newer_http_library),
        "http" to I18n.s(R.string.ui_http_handling),
        "urlmon" to I18n.s(R.string.ui_url_handling_and_downloads),
        "dnsapi" to I18n.s(R.string.ui_dns_lookups),
        "iphlpapi" to I18n.s(R.string.ui_network_adapter_and_ip_configuration_queries),
        "netapi32" to I18n.s(R.string.ui_windows_networking_and_domain_queries),
        "dplay" to I18n.s(R.string.ui_directplay_multiplayer_in_older_games),
        "dpnet" to I18n.s(R.string.ui_directplay_8_networking),
        "rpc" to I18n.s(R.string.ui_remote_procedure_calls_used_between_windows_component),
        "mswsock" to I18n.s(R.string.ui_winsock_extensions),

        // ── Security ──────────────────────────────────────────────────────────────────────
        "crypt" to I18n.s(R.string.ui_cryptography),
        "crypto" to I18n.s(R.string.ui_low_level_cryptographic_operations),
        "bcrypt" to I18n.s(R.string.ui_the_modern_cryptography_api),
        "ncrypt" to I18n.s(R.string.ui_key_storage_and_management),
        "schannel" to "TLS/SSL — secure connections. Try this when a launcher can't log in.",
        "secur32" to I18n.s(R.string.ui_authentication),
        "wintrust" to I18n.s(R.string.ui_signature_verification_including_anti_tamper_checks),
        "cred" to I18n.s(R.string.ui_stored_credentials),
        "security" to I18n.s(R.string.ui_permissions_and_access_checks),
        "advapi" to I18n.s(R.string.ui_advanced_windows_apis_services_registry_and_security),

        // ── COM / scripting ───────────────────────────────────────────────────────────────
        "ole" to "COM/OLE — Windows' component system. Launchers lean on it heavily.",
        "combase" to I18n.s(R.string.ui_the_com_base_library),
        "olemalloc" to I18n.s(R.string.ui_com_memory_allocation),
        "variant" to I18n.s(R.string.ui_com_variant_type_conversion),
        "msi" to I18n.s(R.string.ui_windows_installer_the_channel_for_a_failing_msi_insta),
        "msxml" to I18n.s(R.string.ui_xml_parsing),
        "xmllite" to I18n.s(R.string.ui_lightweight_xml_parsing),
        "vbscript" to I18n.s(R.string.ui_vbscript),
        "jscript" to "JScript/JavaScript.",
        "mshtml" to I18n.s(R.string.ui_the_embedded_web_browser_control_used_by_some_launche),
        "actctx" to "Activation contexts and side-by-side assemblies — a common source of " +
            "\"the application failed to initialize\" errors.",
        "sxs" to I18n.s(R.string.ui_side_by_side_assembly_resolution_the_same_family_as_a),
        "fusion" to ".NET assembly loading.",

        // ── Text and printing ─────────────────────────────────────────────────────────────
        "font" to I18n.s(R.string.ui_font_loading_and_matching_the_channel_for_missing_or),
        "dwrite" to I18n.s(R.string.ui_directwrite_modern_text_rendering),
        "fontcache" to I18n.s(R.string.ui_the_font_cache_service),
        "text" to I18n.s(R.string.ui_text_drawing),
        "string" to I18n.s(R.string.ui_string_handling),
        "nls" to I18n.s(R.string.ui_locale_and_codepage_handling_relevant_to_non_english),
        "locale" to I18n.s(R.string.ui_regional_settings),
        "imm" to I18n.s(R.string.ui_input_method_editors_for_cjk_text_entry),
        "print" to I18n.s(R.string.ui_printing),
        "winspool" to I18n.s(R.string.ui_the_print_spooler),

        // ── Host side ─────────────────────────────────────────────────────────────────────
        "x11drv" to "The X11 display driver — how Wine's windows reach WinHub's screen. " +
            "The channel for windowing, fullscreen and mouse-capture problems.",
        "waylanddrv" to I18n.s(R.string.ui_the_wayland_display_driver_used_by_the_experimental_w),
        "xrandr" to I18n.s(R.string.ui_display_modes_and_refresh_rates_under_x11),
        "xrender" to I18n.s(R.string.ui_x11_accelerated_drawing),
        "xim" to I18n.s(R.string.ui_x11_input_methods),
        "xdnd" to I18n.s(R.string.ui_x11_drag_and_drop),
        "macdrv" to I18n.s(R.string.ui_the_macos_display_driver_not_used_on_android),
        "winebrowser" to I18n.s(R.string.ui_opening_links_in_a_browser),
    )

    /** The specific description when there is one, otherwise an honest category-based line. */
    fun describe(channel: String): String {
        DESCRIPTIONS[channel]?.let { return it }
        val cat = categoryOf(channel)
        val tail = when (cat) {
            CAT_OTHER -> "Debug output from Wine's `$channel` component."
            else -> "${cat.replaceFirstChar { it.uppercase() }} — debug output from Wine's " +
                "`$channel` component."
        }
        return tail
    }

    /** True when [describe] has something specific to say, rather than a category fallback. */
    fun hasDetail(channel: String): Boolean = DESCRIPTIONS.containsKey(channel)

    /**
     * Family for any channel. Prefix and suffix matching, because Wine's naming is consistent
     * enough for it: everything starting "d3d" is graphics, everything starting "crypt" is
     * security, and so on. Order matters — the first match wins.
     */
    fun categoryOf(channel: String): String {
        val c = channel.lowercase()

        if (c in setOf("err", "warn", "fixme", "seh", "relay", "snoop", "trace", "exception",
                "unwind", "debugstr", "debug_buffer", "fixup", "stress", "message", "msg")) return CAT_TRACING

        if (c in setOf("x11drv", "waylanddrv", "macdrv", "xrandr", "xrender", "xim", "xdnd",
                "xvidmode", "alsa", "pulse", "oss", "winebrowser", "winemapi", "wineusb")) return CAT_HOST

        if (c.startsWith("d3d") || c.startsWith("dxg") || c.startsWith("ddraw") ||
            c.startsWith("gdi") || c.startsWith("opengl") || c.startsWith("wgl") ||
            c in setOf("vulkan", "d2d", "dcomp", "dwmapi", "bitblt", "bitmap", "palette", "dxcore",
                "dxva2", "asmshader", "gl_compat", "glu", "graphics", "icm", "image", "region",
                "clipping", "dc", "dciman", "enhmetafile", "metafile", "dxtrans", "dxdiag",
                "wincodecs", "wing", "icon", "imagelist", "cursor", "display", "psdrv", "olepicture",
                "d3drm", "dx8vb", "uianimation", "manipulation")) return CAT_GRAPHICS

        if (c.startsWith("dsound") || c.startsWith("dm") || c.startsWith("xaudio") ||
            c.startsWith("midi") || c.startsWith("mci") && c.contains("wave") ||
            c in setOf("winmm", "mmdevapi", "coreaudio", "wavemap", "sound", "msacm", "mmaux",
                "mmsys", "mmtime", "adpcm", "g711", "gsm", "speech", "sapi", "msttsengine",
                "mp3dmod", "wmadec", "avrt", "mciwave", "mcicda", "mcimidi", "audio")) return CAT_AUDIO

        if (c.startsWith("wmv") || c.startsWith("mpeg") || c.startsWith("msvid") ||
            c in setOf("quartz", "mfplat", "evr", "devenum", "avifile", "avicap", "mciavi",
                "mciqtz", "msvideo", "mmio", "media", "mediacontrol", "capture", "iccvid",
                "ir50_32", "msrle32", "msvidc32", "msmpeg2vdec", "msauddecmft", "dmo", "msdmo",
                "dsdmo", "twain", "sti", "wia", "bytecodewriter", "packager")) return CAT_VIDEO

        if (c.startsWith("dinput") || c.startsWith("xinput") || c.startsWith("wintab") ||
            c in setOf("rawinput", "hid", "joycpl", "keyboard", "input", "usb", "usbd", "winusb",
                "bluetooth", "bluetoothapis", "hotkey", "ndis", "plugplay", "setupapi", "ir",
                "scsiport", "tape", "aspi", "capi", "ctapi32", "smbios", "perception",
                "geolocator", "sensapi", "winscard", "hostname")) return CAT_INPUT

        if (c.startsWith("list") || c.startsWith("combo") || c.startsWith("tool") ||
            c.startsWith("prop") || c.startsWith("tree") ||
            c in setOf("win", "nonclient", "dialog", "menu", "menubuilder", "user", "clipboard",
                "dragdrop", "hook", "theme_scroll", "uxtheme", "commctrl", "commdlg", "edit",
                "button", "statusbar", "systray", "shell", "explorerframe", "shdocvw", "shlctrl",
                "shcore", "browseui", "appbar", "animate", "comboex", "header", "monthcal",
                "pager", "progress", "rebar", "scroll", "static", "syslink", "tab", "trackbar",
                "updown", "ipaddress", "datetime", "taskdialog", "uiribbon", "uiautomation",
                "oleacc", "nstc", "cards", "mdi", "richedit", "richedit_lists", "class", "ui",
                "ninput", "gamebar", "gameux", "gamingtcui", "mmc", "recyclebin", "pidl",
                "selector", "enumeration", "twinapi", "wpc", "htmlhelp", "hlink")) return CAT_UI

        if (c.startsWith("msvc") || c.startsWith("dbghelp") ||
            c in setOf("process", "thread", "threadpool", "module", "loaddll", "unloaddll", "heap",
                "globalmem", "virtual", "sync", "ntdll", "kernelbase", "ntoskrnl", "server",
                "syslevel", "environ", "exec", "toolhelp", "vcruntime", "concrt", "vcomp", "wow",
                "int", "int21", "int31", "vxd", "dosmem", "atom", "handle", "global", "local",
                "context", "thunk", "atlthunk", "atl", "dll", "resource", "ver", "system",
                "dbgeng", "diasymreader", "faultrep", "wer", "rstrtmgr", "apphelp", "fltlib",
                "fltmgr", "driver", "vdmdbg", "event", "eventlog", "wevtapi", "tdh", "pdh",
                "loadperf", "powermgnt", "powrprof", "clusapi", "schedsvc", "mstask", "taskschd",
                "task", "service", "wmi", "wmiutils", "wbemprox", "wbemdisp", "mgmtapi",
                "query", "data", "model", "wldp", "hvsi", "tbs", "amsi")) return CAT_RUNTIME

        if (c in setOf("file", "storage", "volume", "mountmgr", "cabinet", "profile", "path",
                "davclnt", "wofutil", "virtdisk", "sfc", "msisip", "mspatcha", "msopc",
                "wimgapi", "wnet", "mpr", "lanman", "itss", "infosoft")) return CAT_FILES

        if (c.startsWith("dp") || c.startsWith("dhcp") || c.startsWith("snmp") ||
            c.startsWith("wsnmp") || c.startsWith("ras") || c.startsWith("wlan") ||
            c in setOf("winsock", "wininet", "winhttp", "http", "urlmon", "dnsapi", "iphlpapi",
                "netapi32", "rpc", "mswsock", "netbios", "netcfgx", "netio", "netprofm", "nsi",
                "url", "jsproxy", "inetcomm", "inetcpl", "inetmib1", "wldap32", "ldap", "webservices",
                "wsdapi", "qwave", "traffic", "qmgr", "hnetcfg", "fwpuclnt", "mprapi", "rtutils",
                "tdi", "tapi", "wpcap", "wtsapi", "winsta", "winstation", "mapi", "cdosys",
                "wuapi", "connect", "sensapi2", "wsock")) return CAT_NET

        if (c in setOf("reg", "advapi", "policy")) return CAT_REGISTRY

        if (c.startsWith("crypt") || c.startsWith("acl") || c.startsWith("cred") ||
            c in setOf("bcrypt", "ncrypt", "schannel", "secur32", "wintrust", "security", "authz",
                "kerberos", "ntlm", "ksecdd", "sspicli", "msasn1", "dssenh", "pstores", "msdrm",
                "slc", "pidgen", "mssign", "ntdsapi", "activeds", "adsldp", "dsquery", "dsdmo2",
                "dsuiext", "objsel", "msident", "scrobj")) return CAT_SECURITY

        if (c.startsWith("ole") || c.startsWith("msxml") || c.startsWith("msdas") ||
            c in setOf("ole", "combase", "olemalloc", "variant", "xmllite", "vbscript", "jscript",
                "mshtml", "actctx", "sxs", "fusion", "actxprxy", "comsvcs", "msctf", "msctfmonitor",
                "msimtf", "msscript", "scrrun", "odbc", "oledb", "msado15", "xolehlp", "dhtmled",
                "ieframe", "inkobj", "inseng", "propsys", "wintypes", "winstring", "dcom",
                "typelib", "msdasql")) return CAT_COM

        if (c.startsWith("font") || c.startsWith("atm") ||
            c in setOf("dwrite", "text", "string", "nls", "locale", "imm", "t2embed", "fontsub",
                "nativefont", "mlang", "bidi", "msftedit", "atmlib")) return CAT_TEXT

        if (c.startsWith("print") || c.startsWith("spool") || c.startsWith("localsp") ||
            c in setOf("winspool", "compstui", "prntvpt", "ntprint", "winprint", "localui",
                "printui", "spoolss")) return CAT_PRINT

        if (c.startsWith("msi") || c.startsWith("appwiz") || c.startsWith("adv") ||
            c in setOf("appx", "advpack", "difxapi", "updspapi", "setupapi2", "cabinet2",
                "msidb", "msisys")) return CAT_INSTALL

        return CAT_OTHER
    }
}
