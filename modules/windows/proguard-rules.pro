# Compose Desktop ProGuard Rules
-dontwarn
-dontnote
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepclassmembers enum * { *; }

# Application Entry and Domain Classes
-keep class com.woofish.** { *; }
-keep class com.woofish.desktop.MainKt {
    public static void main(java.lang.String[]);
}

# Skiko & Compose
-dontwarn org.jetbrains.skiko.**
-dontwarn androidx.compose.**

# Audio & MP3 SPI
-keep class javazoom.jl.decoder.** { *; }
-keep class javazoom.spi.mpeg.sampled.file.** { *; }
-keep class com.googlecode.soundlibs.** { *; }
-keep class org.tritonus.share.sampled.file.** { *; }
-keep class * implements javax.sound.sampled.spi.AudioFileReader { *; }
-keep class * implements javax.sound.sampled.spi.FormatConversionProvider { *; }
-keep class * implements javax.sound.sampled.spi.AudioFileWriter { *; }

# Kotlin & Coroutines
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { *; }
