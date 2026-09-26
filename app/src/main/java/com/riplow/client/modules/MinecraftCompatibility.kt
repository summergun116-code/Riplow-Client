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
            val versionCode =
                if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            MinecraftInstall(true, info.versionName ?: "unknown", versionCode)
        } catch (_: Exception) {
            MinecraftInstall(false)
        }
    }

    fun bridgeState(context: Context): String {
        val install = detect(context)
        if (!install.installed) return "Minecraft not detected"

        val version = BedrockVersionParser.parse(install.versionName)
            ?: return "Bedrock " + install.versionName + " • version could not be parsed"

        val bridge = BedrockBridgeRegistry.inspect(version)
        val profile = bridge.profile ?: BedrockCompatibilityCatalog.inspect(version)

        return when (bridge.state) {
            BedrockBridgeState.NOT_DETECTED -> "Minecraft not detected"
            BedrockBridgeState.VERSION_RECOGNIZED_NO_BRIDGE ->
                "Bedrock " + version +
                    " • " + profile.note +
                    " • game bridge not attached"
            BedrockBridgeState.VERSION_PARSED_NO_ADAPTER ->
                "Bedrock " + version + " • no version adapter"
            BedrockBridgeState.ADAPTER_READY ->
                "Bedrock " + version + " • adapter " + bridge.adapterId + " ready"
            BedrockBridgeState.ATTACHED ->
                "Bedrock " + version + " • bridge attached"
        }
    }
}
