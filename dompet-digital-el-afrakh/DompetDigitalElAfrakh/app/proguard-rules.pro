# ProGuard rules for Dompet Digital El Afrakh
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-keep class com.elafrakh.dompet.data.local.** { *; }
