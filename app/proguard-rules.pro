# Proguard rules for MovieApp

# Preserve Retrofit and Gson models
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# R8 Full Mode strips generic signatures from non-kept classes.
# Kotlin suspend functions use Continuation<T> as the last parameter,
# where T is the response type inspected by Retrofit via reflection.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep DTO models in features
-keep class com.movieapp.features.**.**DTO { *; }
-keepclassmembers class com.movieapp.features.**.**DTO { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.SerializedName <methods>;
}

# Retain Retrofit interfaces
-keep interface com.movieapp.network.** { *; }
-keep class com.movieapp.network.** { *; }

# Keep Gson itself
-keep class com.google.gson.** { *; }

# Keep OkHttp and Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Strip OkHttp logging in release builds to reduce size and prevent log leakage
-assumenosideeffects class okhttp3.logging.HttpLoggingInterceptor {
    public void log(java.lang.String);
}

# Coroutines & Coil Proguard optimizations
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.Signal*


