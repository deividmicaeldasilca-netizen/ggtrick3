package com.ggtrick
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast

class MainActivity : Activity() {
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    val w = WebView(this)
    w.settings.javaScriptEnabled = true
    w.settings.domStorageEnabled = true
    w.addJavascriptInterface(Bridge(), "Android")
    w.loadUrl("file:///android_asset/index.html")
    setContentView(w)
  }
  inner class Bridge {
    @JavascriptInterface fun setKeymap(json: String) { TrickService.keymap = json }
    @JavascriptInterface fun serviceOn(): Boolean = TrickService.inst != null
    @JavascriptInterface fun openAccessibility() {
      startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    @JavascriptInterface fun launch(pkg: String) {
      TrickService.target = pkg
      val i = packageManager.getLaunchIntentForPackage(pkg)
      if (i == null) runOnUiThread { Toast.makeText(this@MainActivity, "App não instalado: $pkg", Toast.LENGTH_LONG).show() }
      else startActivity(i)
    }
  }
}
