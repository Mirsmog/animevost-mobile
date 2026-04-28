# Add project specific ProGuard rules here.

# Keep Gson model classes — R8 strips field names which breaks
# @SerializedName-based deserialization (manifests as ClassCastException
# from Gson reflective adapter).
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.animevost.app.core.network.alloha.** { *; }
-keep class com.animevost.app.core.network.AnimeVost*Response { *; }
-keep class com.animevost.app.core.network.dto.** { *; }
