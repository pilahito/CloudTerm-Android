package com.pilahito.cloudterm.android.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class PhoneHardware(
    val manufacturer: String,
    val model: String,
    val abi: String,
    val ramMb: Long,
    val sdk: Int,
    val recommended: String,
    val localModels: List<AiModel>,
)

object HardwareProbe {
    fun read(context: Context): PhoneHardware {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val ramMb = mem.totalMem / (1024 * 1024)
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        val recommended = when {
            ramMb < 3000 -> "1B Q4 (Gemma 2B / Qwen 1.5B)"
            ramMb < 6000 -> "1B–3B Q4"
            else -> "3B Q4 si no hay otras apps pesadas"
        }
        val local = ModelCatalog.all.filter { model ->
            when {
                ramMb < 3000 -> model.id in setOf("gemma-edge", "llama-chat")
                ramMb < 6000 -> model.id != "qwen-coder" || ramMb >= 5000
                else -> true
            }
        }
        return PhoneHardware(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            abi = abi,
            ramMb = ramMb,
            sdk = Build.VERSION.SDK_INT,
            recommended = recommended,
            localModels = if (local.isEmpty()) ModelCatalog.all.take(2) else local,
        )
    }
}
