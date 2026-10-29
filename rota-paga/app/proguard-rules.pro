# Métodos chamados pelo JavaScript não podem ser renomeados.
-keepclassmembers class com.velox.rotapaga.Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
