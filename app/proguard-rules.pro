# StayFlow R8 + ProGuard Obfuscation & Optimization Rules
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-repackageclasses 'sf'

# Strip source file names & debug logs in release APK
-renamesourcefileattribute SF
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Preserve WebView JavaScript Bridge (CRITICAL for SessionGuardScript)
-keepattributes JavascriptInterface,Annotation,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.example.browser.BebasGuardJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve Android entry points & manifest components
-keep public class com.example.MainActivity
-keep public class com.example.service.SessionKeepAliveService
-keep public class com.example.browser.KeepAliveWebView

# Preserve encrypted vault & AI data models
-keepclassmembers class com.example.BuildConfig {
    public static final java.lang.String VAULT_*;
}

# Preserve Kotlin coroutines & AndroidX DataStore / WebKit
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# OkHttp / Okio optional platform classes
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn java.lang.invoke.**
