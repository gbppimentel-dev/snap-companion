# Framework lifecycle entries are retained by the Android default rules.
# The local CSP-restricted page has only haptics, map visibility and four whitelisted tool actions.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-keepclassmembers class dev.gbppimentel.snapcompanion.MainActivity$TouchFeedback {
    @android.webkit.JavascriptInterface <methods>;
}
