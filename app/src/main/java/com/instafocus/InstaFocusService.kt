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

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.loading_overlay, null)
        overlayView?.layoutParams = params
        overlayView?.setOnTouchListener { _, _ -> true }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName?.toString() != "com.instagram.android") {
            hideOverlay()
            return
        }

        val root = rootInActiveWindow ?: return

        // Check for specific Reels player signatures
        val isReelsTab = findNodeByTextOrDescription(root, "Reels viewer") ||
                         findNodeByTextOrDescription(root, "Audio by") ||
                         findNodeByTextOrDescription(root, "Original audio")

        // Check for Search grid signature
        val isSearchGrid = findNodeByTextOrDescription(root, "Search and explore")

        // Check if user is actively inside an open Chat thread
        val isInsideChatThread = findNodeByTextOrDescription(root, "Message...") ||
                                 findNodeByTextOrDescription(root, "Audio call") ||
                                 findNodeByTextOrDescription(root, "Video call")

        if ((isReelsTab || isSearchGrid) && !isInsideChatThread) {
            showOverlay()
        } else {
            hideOverlay()
        }
    }

    private fun showOverlay() {
        if (!isOverlayShowing && overlayView != null) {
            try {
                windowManager?.addView(overlayView, overlayView?.layoutParams)
                isOverlayShowing = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hideOverlay() {
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
                isOverlayShowing = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun findNodeByTextOrDescription(node: AccessibilityNodeInfo?, keyword: String): Boolean {
        if (node == null) return false
        if (node.text?.contains(keyword, true) == true || 
            node.contentDescription?.contains(keyword, true) == true) {
            return true
        }
        for (i in 0 until node.childCount) {
            if (findNodeByTextOrDescription(node.getChild(i), keyword)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        hideOverlay()
    }
}
