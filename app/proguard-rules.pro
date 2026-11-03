# Room - mantieni entità e DAO
-keep class com.example.woundmanagement.data.db.entity.** { *; }
-keep class com.example.woundmanagement.data.db.dao.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# SQLCipher per Android (net.zetetic.database): le regole arrivano con la libreria.

# Security Crypto / EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose - necessario per reflection interna
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Rimuove TUTTE le chiamate android.util.Log nella build release (R8): nessun residuo
# nei log di produzione su un'app che tratta dati sanitari.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Tink (dentro security-crypto) cita annotazioni che servono solo in compilazione:
# regole generate da R8 in missing_rules.txt (build di rilascio del 3/11/2026).
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
# Tink legge i suoi messaggi protobuf-lite per riflessione: i campi devono restare.
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { <fields>; }
