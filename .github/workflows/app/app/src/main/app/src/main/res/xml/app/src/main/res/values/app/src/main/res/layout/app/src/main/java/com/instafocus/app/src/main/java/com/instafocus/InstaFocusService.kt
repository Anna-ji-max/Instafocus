package com.instafocus

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InstaFocusService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        
        overlayView = LayoutInflater.from(this).inflate(R.layout.loading_overlay, null)
        overlayView?.setOnTouchListener { _, _ -> true } // Absorb all touches
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != "com.instagram.android") {
            hideOverlay()
            return
        }

        val root = rootInActiveWindow ?: return
        
        // Logic to detect restricted areas
        val isReels = findNodeByTextOrDescription(root, "Reels")
        val isSearch = findNodeByTextOrDescription(root, "Search")
        val isHighlights = findNodeByTextOrDescription(root, "Highlights")
        val isChat = findNodeByTextOrDescription(root, "Message") || findNodeByTextOrDescription(root, "Chat")
        val isCamera = findNodeByTextOrDescription(root, "Camera")
        
        // Allow chat and camera, block Reels, Search, and profile Highlights
        if ((isReels || isSearch || isHighlights) && !isChat && !isCamera) {
            showOverlay()
        } else {
            // Further optimization: To block vertical scrolling in feeds but allow horizontal
            // This requires intercepting gesture events, handled by the overlay consuming touches
            hideOverlay()
        }
    }

    private fun showOverlay() {
        if (!isOverlayShowing) {
            try {
                windowManager?.addView(overlayView, overlayView?.layoutParams)
                isOverlayShowing = true
            } catch (e: Exception) {}
        }
    }

    private fun hideOverlay() {
        if (isOverlayShowing) {
            try {
                windowManager?.removeView(overlayView)
                isOverlayShowing = false
            } catch (e: Exception) {}
        }
    }

    private fun findNodeByTextOrDescription(node: AccessibilityNodeInfo, keyword: String): Boolean {
        if (node.text?.contains(keyword, true) == true || 
            node.contentDescription?.contains(keyword, true) == true) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && findNodeByTextOrDescription(child, keyword)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        hideOverlay()
    }
}
