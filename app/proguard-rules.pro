# Firebase Firestore — mantener clases del modelo para deserialización
-keep class com.kosking.app.modelo.** { *; }

# Firebase Auth y Firestore
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Coil
-keep class coil.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }

# Kotlin serialization / reflection usada por Firestore
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
