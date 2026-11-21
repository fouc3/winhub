package com.winhub.hernandez.contents

import com.winhub.hernandez.R
import com.winhub.hernandez.util.I18n

/**
 * Layer 1 of the Smart Wrapper Manager (issue #132) — the maintained dictionary that turns a raw,
 * auto-detected env-var NAME (found by "strings"-scanning a wrapper's binaries; see
 * [WrapperManager.scanEnvKeys]) into a proper settings control (toggle / slider / dropdown / text)
 * with a human label, hint, default and value domain.
 *
 * This is OURS to grow: one [SettingDef] per common Winlator-family setting → every imported wrapper
 * that references that env var automatically gets a polished control, with ZERO cooperation from the
 * wrapper author. A detected key NOT in the table falls back to a generic [Type.TEXT] field whose
 * label is the raw env key (still usable by power users) via [defFor].
 *
 * The dialog stores each control's value in `graphicsDriverConfig` under the RAW ENV KEY, and XSDA
 * emits `KEY=value` generically at launch — so adding a row here is the ONLY step needed to support
 * a new setting end-to-end (no dialog or emission code changes).
 */
object WrapperSettingsDictionary {

    enum class Type { TOGGLE, SLIDER, DROPDOWN, TEXT }

    data class SettingDef(
        val key: String,
        val type: Type,
        val label: String,
        val hint: String = "",
        val default: String = "",
        val choices: List<String> = emptyList(),
        val min: Float = 0f,
        val max: Float = 0f,
        val step: Float = 0f,
    )

    // Seeded with the known Winlator-family vocabulary. Keys that a curated control already exposes
    // (see WrapperManager.HANDLED_ENV_KEYS) are still listed for completeness, but the "Detected
    // settings" section filters them out so nothing is shown twice.
    private val DEFS: Map<String, SettingDef> = listOf(
        // --- WRAPPER_* (integrated ICD) ---
        SettingDef("WRAPPER_VK_VERSION", Type.TEXT, I18n.s(R.string.ui_vulkan_version_2), "e.g. 1.4", default = "1.4"),
        SettingDef("WRAPPER_EMULATE_BCN", Type.DROPDOWN, I18n.s(R.string.ui_bcn_emulation_2),
            "0 off · 1 basic · 2 full · 3 auto", default = "3", choices = listOf("0", "1", "2", "3")),
        SettingDef("WRAPPER_BCN_ASTC", Type.TOGGLE, I18n.s(R.string.graphics_driver_bcn_emulation_astc)),
        SettingDef("WRAPPER_EXTENSION_BLACKLIST", Type.TEXT, I18n.s(R.string.ui_blacklisted_vulkan_extensions),
            "comma-separated"),
        SettingDef("WRAPPER_DRIVER_ID", Type.TEXT, I18n.s(R.string.ui_driver_id_override)),
        SettingDef("WRAPPER_SAFE_CREATE_DEVICE", Type.TOGGLE, I18n.s(R.string.ui_safe_create_device)),
        SettingDef("WRAPPER_DEVICE_NAME", Type.TEXT, I18n.s(R.string.ui_spoof_gpu_name)),
        SettingDef("WRAPPER_DEVICE_ID", Type.TEXT, I18n.s(R.string.ui_spoof_device_id)),
        SettingDef("WRAPPER_VENDOR_ID", Type.TEXT, I18n.s(R.string.ui_spoof_vendor_id)),
        SettingDef("WRAPPER_VMEM_MAX_SIZE", Type.TEXT, I18n.s(R.string.ui_max_device_memory_mb), "number, 0 = unset"),
        SettingDef("WRAPPER_DISABLE_PRESENT_WAIT", Type.TOGGLE, I18n.s(R.string.ui_disable_present_wait_2)),
        SettingDef("WRAPPER_MAX_IMAGE_COUNT", Type.TEXT, I18n.s(R.string.ui_max_swapchain_images), "number"),
        SettingDef("WRAPPER_ONE_BY_ONE", Type.TOGGLE, I18n.s(R.string.ui_submit_commands_one_by_one),
            "Compatibility/stability; can be slower"),
        SettingDef("WRAPPER_REDUCE_DEPTH_FORMAT", Type.TOGGLE, I18n.s(R.string.ui_reduce_depth_buffer_format),
            I18n.s(R.string.ui_compat_for_gpus_that_struggle_with_some_depth_formats)),
        SettingDef("WRAPPER_DISABLE_PLACED", Type.TOGGLE, I18n.s(R.string.ui_disable_placed_memory), I18n.s(R.string.ui_compatibility_toggle)),
        // --- MESA_VK_* / GALLIUM_* ---
        SettingDef("MESA_VK_VERSION_OVERRIDE", Type.TEXT, I18n.s(R.string.ui_force_vulkan_version_mesa),
            I18n.s(R.string.ui_e_g_1_3_overrides_the_reported_vulkan_version)),
        SettingDef("MESA_VK_WSI_PRESENT_MODE", Type.DROPDOWN, I18n.s(R.string.ui_wsi_present_mode),
            default = "mailbox", choices = listOf("mailbox", "fifo", "immediate")),
        SettingDef("GALLIUM_DRIVER", Type.TEXT, I18n.s(R.string.ui_gallium_driver), "e.g. zink"),
        // --- BCn layer (leegao standalone) ---
        SettingDef("ENABLE_BCN_COMPUTE", Type.TOGGLE, I18n.s(R.string.ui_enable_bcn_compute)),
        SettingDef("BCN_COMPUTE_AUTO", Type.TOGGLE, I18n.s(R.string.ui_bcn_compute_auto_detect)),
        SettingDef("BCN_TRANSCODE_TO_ETC2", Type.TOGGLE, I18n.s(R.string.ui_transcode_bcn_etc2)),
        SettingDef("BCN_TRANSCODE_TO_ASTC", Type.TOGGLE, I18n.s(R.string.ui_transcode_bcn_astc)),
        SettingDef("BCN_COMPUTE_IMAGE_VIEW", Type.TOGGLE, I18n.s(R.string.bcn_layer_image_view)),
        SettingDef("BCN_LAYER_LOG_LEVEL", Type.TEXT, I18n.s(R.string.ui_bcn_log_level), "e.g. info,error"),
        SettingDef("BCN_MAX_STAGING_CACHE_MB", Type.TEXT, I18n.s(R.string.ui_bcn_staging_cache_mb), "number"),
        SettingDef("BCN_QUEUE_THROTTLE_LIMIT", Type.TEXT, I18n.s(R.string.ui_bcn_queue_throttle_limit), "number"),
        SettingDef("BCN_ASTC_USE_LARGE_STEPS", Type.TOGGLE, I18n.s(R.string.ui_astc_use_large_steps),
            I18n.s(R.string.ui_fcharan_bcn_fork_faster_astc_transcode_lower_quality)),
        // --- DXVK/Mali compat + DX12 ---
        SettingDef("ENABLE_DXVK_MALI_COMPAT_LAYER", Type.TOGGLE, I18n.s(R.string.ui_enable_dxvk_mali_compat_layer)),
        SettingDef("COMPAT_EMULATE_SPARSE_BINDING", Type.TOGGLE, I18n.s(R.string.ui_emulate_sparse_binding)),
        SettingDef("COMPAT_FORCE_MASKING", Type.TOGGLE, I18n.s(R.string.ui_force_masking)),
        SettingDef("COMPAT_EMULATE_PUSH_DESCRIPTORS", Type.TOGGLE, I18n.s(R.string.ui_emulate_push_descriptors_dx12),
            I18n.s(R.string.ui_compat_layer_leave_on_for_dx12_titles_that_use_push_desc)),
        SettingDef("COMPAT_EMULATE_NULL_DESCRIPTORS", Type.TOGGLE, I18n.s(R.string.ui_emulate_null_descriptors_dx12),
            I18n.s(R.string.ui_compat_layer_dx12_null_descriptor_support)),
        SettingDef("COMPAT_SPARSE_COMMIT_BUDGET", Type.TEXT, I18n.s(R.string.ui_sparse_commit_budget_mb),
            "Compat layer — memory budget for D3D12 tiled/sparse resources"),
        // --- Added from a strings-scan of the WinlatorMali / WinNative / leegao / Fcharan fork binaries
        //     (#132 dictionary hardening). Debug/diag/profile keys these binaries also expose are left to
        //     WrapperManager.isDebugEnvKey (hidden from settings), not curated here.
        // BCn transcode / emulation (leegao + Fcharan BCn layer)
        SettingDef("DISABLE_BCN", Type.TOGGLE, I18n.s(R.string.ui_disable_bcn_transcode),
            "Skip the BCn→ASTC/ETC path entirely"),
        SettingDef("FORCE_BCN_EMULATION", Type.TOGGLE, I18n.s(R.string.ui_force_bcn_emulation),
            I18n.s(R.string.ui_emulate_bcn_even_when_the_gpu_claims_native_support)),
        SettingDef("BCN_TRANSCODE_TO_ETC1", Type.TOGGLE, I18n.s(R.string.ui_transcode_bcn_etc1)),
        SettingDef("BCN_ASTC_TRY_2P", Type.TOGGLE, I18n.s(R.string.ui_astc_try_2_partition_blocks),
            I18n.s(R.string.ui_higher_quality_slower_transcode)),
        SettingDef("BCN_ASTC_ONLY_2P", Type.TOGGLE, I18n.s(R.string.ui_astc_2_partition_only)),
        SettingDef("ENABLE_COMPUTE_TRACKING", Type.TOGGLE, I18n.s(R.string.ui_track_compute_resources)),
        // Wrapper-side BCn acceleration (base ICD)
        SettingDef("WRAPPER_BCN_GPU", Type.TOGGLE, I18n.s(R.string.ui_gpu_accelerated_bcn_decode)),
        SettingDef("WRAPPER_BCN_GPU_CAP_MB", Type.TEXT, I18n.s(R.string.ui_bcn_gpu_memory_cap_mb), "number, 0 = unset"),
        SettingDef("WRAPPER_USE_BCN_CACHE", Type.TOGGLE, I18n.s(R.string.ui_cache_transcoded_bcn_textures)),
        SettingDef("WRAPPER_NO_BCN_THREAD", Type.TOGGLE, I18n.s(R.string.ui_disable_bcn_worker_thread),
            I18n.s(R.string.ui_transcode_inline_instead_of_on_a_background_thread)),
        SettingDef("WRAPPER_DMAHEAP_CACHED", Type.TOGGLE, I18n.s(R.string.ui_cached_dma_heap_allocations)),
        // Shader-compat family (base ICD) — workarounds for SPIR-V/driver quirks
        SettingDef("DISABLE_CLIP_DISTANCE", Type.TOGGLE, I18n.s(R.string.ui_disable_gl_clipdistance)),
        SettingDef("FORCE_CLIP_DISTANCE", Type.TOGGLE, I18n.s(R.string.ui_force_gl_clipdistance)),
        SettingDef("WRAPPER_NO_REMOVE_CLIP_DISTANCE", Type.TOGGLE, I18n.s(R.string.ui_keep_clip_distance_in_shaders)),
        SettingDef("DISABLE_OPTIMIZATION_BARRIERS", Type.TOGGLE, I18n.s(R.string.ui_disable_optimization_barriers)),
        SettingDef("FORCE_OPTIMIZATION_BARRIERS", Type.TOGGLE, I18n.s(R.string.ui_force_optimization_barriers)),
        SettingDef("DISABLE_SPEC_COMPOSITE_CONSTANTS", Type.TOGGLE, I18n.s(R.string.ui_disable_spec_composite_constants)),
        SettingDef("FORCE_SPEC_COMPOSITE_CONSTANTS", Type.TOGGLE, I18n.s(R.string.ui_force_spec_composite_constants)),
        SettingDef("WRAPPER_NO_PATCH_OPCONSTCOMP", Type.TOGGLE, I18n.s(R.string.ui_don_t_patch_opconstantcomposite)),
        SettingDef("DISABLE_EXTERNAL_FD", Type.TOGGLE, I18n.s(R.string.ui_disable_external_fd_memory)),
        // Paths / misc (base ICD)
        SettingDef("WRAPPER_CACHE_PATH", Type.TEXT, I18n.s(R.string.ui_wrapper_cache_path), "absolute path"),
        SettingDef("WRAPPER_RESOURCE_TYPE", Type.TEXT, I18n.s(R.string.ui_resource_type_override)),
        // --- Confirmed by getenv() disassembly across the WinlatorMali/WinNative/GameNative/leegao/base
        //     binaries (#132). Types are what the binary actually does with the value (atoi→INT/TOGGLE,
        //     pointer→TEXT). System/driver/debug getenv reads (HOME, PREFIX, NIR_SKIP, HWCPIPE_*, …) are
        //     filtered by WrapperManager.isDriverInternalEnvKey / isDebugEnvKey, NOT curated here.
        SettingDef("WRAPPER_BLIT", Type.TOGGLE, I18n.s(R.string.ui_force_swapchain_blit),
            I18n.s(R.string.ui_route_presents_through_a_blit_compatibility_for_some_swa)),
        SettingDef("WRAPPER_DEVICE_FAULT", Type.TOGGLE, I18n.s(R.string.ui_device_fault_reporting),
            "Enable VK_EXT_device_fault diagnostics on GPU hangs"),
        SettingDef("WRAPPER_EMULATE_PUSH_DESCRIPTOR", Type.TOGGLE, I18n.s(R.string.ui_emulate_push_descriptors),
            I18n.s(R.string.ui_icd_side_push_descriptor_emulation)),
        SettingDef("WRAPPER_ASTC_BLOCK", Type.TEXT, I18n.s(R.string.ui_astc_block_size), "number, 0 = default"),
        SettingDef("WRAPPER_LAYER_PATH", Type.TEXT, I18n.s(R.string.ui_vulkan_layer_path_advanced),
            "absolute path — usually leave blank"),
        SettingDef("USE_IMAGE_VIEW", Type.TOGGLE, I18n.s(R.string.ui_bcn_use_storage_image_view),
            "leegao BCn — storage-image transcode path"),
    ).associateBy { it.key }

    /** Dictionary hit for [key], else a generic TEXT field whose label is the raw env key. */
    fun defFor(key: String): SettingDef = DEFS[key] ?: SettingDef(key, Type.TEXT, key)
}
