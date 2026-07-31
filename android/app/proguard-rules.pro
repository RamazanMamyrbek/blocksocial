-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-keep class com.blocksocial.detection.BlockSocialAccessibilityService { *; }

-keep class com.blocksocial.core.data.database.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
