package com.instafocus

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(64, 64, 64, 64)
        
        val tvInfo = TextView(this)
        tvInfo.text = "1. Enable Accessibility Service\n2. Allow Display Over Other Apps"
        tvInfo.textSize = 18f
        
        val btnAccessibility = Button(this)
        btnAccessibility.text = "Open Accessibility Settings"
        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        
        layout.addView(tvInfo)
        layout.addView(btnAccessibility)
        setContentView(layout)
    }
}
