object AppConfiguration {
    const val appId = "dev.sunls24.sbv"
    const val applicationId = "dev.sunls24.sbv"
    const val compileSdk = 37
    const val minSdk = 24
    const val targetSdk = 36

    private const val versionMajor = 1
    private const val versionMinor = 0
    private const val versionPatch = 0

    const val versionName = "$versionMajor.$versionMinor.$versionPatch"
    const val versionCode = versionMajor * 1_000_000 + versionMinor * 1_000 + versionPatch
}
