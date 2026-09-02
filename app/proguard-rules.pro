# ML Kit 通过反射加载模型，保留其 API
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_textrecognition.** { *; }
-dontwarn com.google.mlkit.**
