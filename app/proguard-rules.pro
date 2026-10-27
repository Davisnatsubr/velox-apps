# Métodos chamados pelo site (JavaScript) não podem ser renomeados.
-keepclassmembers class com.veloxplay.app.MainActivity$Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
