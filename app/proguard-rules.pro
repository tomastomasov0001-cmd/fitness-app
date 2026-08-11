# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path/to/sdk/tools/proguard/proguard-android.txt

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.harvis.fitnessapp.data.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Keep data classes for serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
