# Keep data classes and serialization models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class com.htoms.brief.model.** { *; }
-keep class com.htoms.brief.auth.** { *; }
-keep class com.htoms.brief.api.** { *; }
