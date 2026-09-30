# ChatPilot AI ProGuard / R8 Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Accessibility Services and Broadcast Receivers
-keep public class * extends android.accessibilityservice.AccessibilityService { *; }
-keep public class * extends android.app.Service { *; }

# Kotlinx Serialization & Data Models
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Jetpack Compose runtime
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# OkHttp networking
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase