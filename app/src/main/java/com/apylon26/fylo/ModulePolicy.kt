package com.apylon26.fylo

object ModulePolicy {
    const val ACTION = "com.apylon26.fylo.MODULE"
    const val API_KEY = "com.apylon26.fylo.API_VERSION"
    const val API_VERSION = 1
    fun accepts(exported: Boolean, enabled: Boolean, apiVersion: Int, needsPermission: Boolean) =
        exported && enabled && apiVersion == API_VERSION && !needsPermission
}
