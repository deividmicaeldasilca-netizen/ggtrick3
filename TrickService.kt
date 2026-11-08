package com.ggtrick
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import org.json.JSONArray

class TrickService : AccessibilityService() {
  companion object { var inst: TrickService? = null; var keymap = "[]"; var target = "" }
  private var active = false
  private val h = Handler(Looper.getMainLooper())
  private val held = HashSet<String>()

  override fun onServiceConnected() { inst = this }
  override fun onDestroy() { inst = null; super.onDestroy() }
  override fun onInterrupt() {}
  override fun onAccessibilityEvent(e: AccessibilityEvent) {
    if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) active = e.packageName?.toString() == target
  }

  private fun name(code: Int): String = when (code) {
    KeyEvent.KEYCODE_SPACE -> "Espaço"; KeyEvent.KEYCODE_SHIFT_LEFT -> "L-Shift"
    KeyEvent.KEYCODE_ALT_LEFT -> "L-Alt"; KeyEvent.KEYCODE_ESCAPE -> "ESC"
    else -> KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")
  }
  private fun screen(): Pair<Float, Float> {
    val m = resources.displayMetrics
    return Pair(maxOf(m.widthPixels, m.heightPixels).toFloat(), minOf(m.widthPixels, m.heightPixels).toFloat())
  }
  private fun tap(xp: Double, yp: Double, ms: Long = 60) {
    val (w, hh) = screen(); val p = Path(); p.moveTo((xp / 100 * w).toFloat(), (yp / 100 * hh).toFloat())
    dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, ms)).build(), null, null)
  }
  private fun swipe(cx: Double, cy: Double, dx: Float, dy: Float) {
    val (w, hh) = screen(); val p = Path(); val x = (cx / 100 * w).toFloat(); val y = (cy / 100 * hh).toFloat()
    p.moveTo(x, y); p.lineTo(x + dx, y + dy)
    dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 150)).build(), null, null)
  }
  private fun comps(): JSONArray? = try { JSONArray(keymap) } catch (e: Exception) { null }
  private val joyLoop = object : Runnable { override fun run() {
    val dx = (if ("D" in held) 1 else 0) - (if ("A" in held) 1 else 0)
    val dy = (if ("S" in held) 1 else 0) - (if ("W" in held) 1 else 0)
    if (dx != 0 || dy != 0) {
      comps()?.let { a -> for (i in 0 until a.length()) { val c = a.getJSONObject(i)
        if (c.getString("t") == "joy") swipe(c.getDouble("x"), c.getDouble("y"), dx * 120f, dy * 120f) } }
      h.postDelayed(this, 140)
    }
  } }

  override fun onKeyEvent(ev: KeyEvent): Boolean {
    if (!active) return false
    val k = name(ev.keyCode)
    val a = comps() ?: return false
    val hasJoy = (0 until a.length()).any { a.getJSONObject(it).getString("t") == "joy" }
    if (hasJoy && k in listOf("W", "A", "S", "D")) {
      if (ev.action == KeyEvent.ACTION_DOWN) { if (held.isEmpty()) h.post(joyLoop); held.add(k) } else held.remove(k)
      return true
    }
    if (ev.action != KeyEvent.ACTION_DOWN || ev.repeatCount > 0) return false
    var hit = false
    for (i in 0 until a.length()) { val c = a.getJSONObject(i)
      if (c.getString("t") == "key" && c.getString("k") == k) { tap(c.getDouble("x"), c.getDouble("y")); hit = true } }
    return hit
  }
}
