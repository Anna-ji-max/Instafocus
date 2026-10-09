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
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.loading_overlay, null)
        overlayView?.layoutParams = params

        // Emergency Safety: Double-tapping the overlay force-dismisses it
        var lastTapTime = 0L
        overlayView?.setOnTouchListener { _, event ->
            val currentTime = System.currentTimeMillis()
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                if (currentTime - lastTapTime < 300) {
                    hideOverlay() // Double tap safety override
                }
                lastTapTime = currentTime
            }
            true // Intercept touches on blocked content
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // SAFETY CHECK 1: Instantly strip overlay if not actively inside Instagram
        val currentPackage = event?.packageName?.toString()
        if (currentPackage == null || currentPackage != "com.instagram.android") {
            hideOverlay()
            return
        }

        val root = rootInActiveWindow
        if (root == null) {
            hideOverlay()
            return
        }

        // Targeted Reels and Explore detection
        val isReels = findNodeByTextOrDescription(root, "Reels viewer") ||
                      findNodeByTextOrDescription(root, "Audio by")
        
        val isSearch = findNodeByTextOrDescription(root, "Search and explore")

        // Exclude active chat windows
        val isChat = findNodeByTextOrDescription(root, "Message...") ||
                     findNodeByTextOrDescription(root, "Audio call")

        if ((isReels || isSearch) && !isChat) {
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

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}
