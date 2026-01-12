package com.example.homebrowse

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var urlEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        urlEdit = findViewById(R.id.url_edit)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        urlEdit.setText(prefs.getString("home_url", ""))

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val u = urlEdit.text.toString().trim()
            prefs.edit().putString("home_url", u).apply()
            finish()
        }
    }
}