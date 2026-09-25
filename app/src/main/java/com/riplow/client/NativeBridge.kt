package com.riplow.client
object NativeBridge {
    init { System.loadLibrary("riplow_core") }
    external fun version(): String
    external fun nativeModuleSummary(): String
    external fun nativeDiagnostics(): String
    external fun nativeToggleModule(id: String): Boolean
}