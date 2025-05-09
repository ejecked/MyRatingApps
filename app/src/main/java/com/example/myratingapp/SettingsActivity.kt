package com.example.myratingapp // <-- Убедитесь, что это имя вашего пакета

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings) // Устанавливаем макет activity_settings.xml

        // Устанавливаем заголовок для Action Bar (опционально)
        supportActionBar?.title = "Настройки"
    }
}