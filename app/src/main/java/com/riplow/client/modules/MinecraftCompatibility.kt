package com.riplow.client.modules

import android.content.Context
import android.content.pm.PackageInfo

data class MinecraftInstall(
    val installed: Boolean,
    val versionName: String = "not detected",
    val versionCode: Long = -1L
)

object MinecraftCompatibility {
    private const val PACKAGE = "com.mojang.minecraftpe"

    fun detect(context: Context): MinecraftInstall {
        return try {
            val info: PackageInfo = context.packageManager.getPackageInfo(PACKAGE, 0)
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            MinecraftInstall(true, info.versionName ?: "unknown", versionCode)
        } catch (_: Exception) {
            MinecraftInstall(false)
        }
    }

    fun bridgeState(context: Context): String {
        val install = detect(context)
        return if (!install.installed) "Minecraft not detected"
        else "Bedrock ${install.versionName} • game bridge pending"
    }
}
