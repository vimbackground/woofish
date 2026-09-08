# Keep Compose-related rules
-keepattributes *Annotation*
-dontwarn androidx.compose.**

# Keep data models if any
-keep class com.woofish.WoodenFishUiState { *; }
