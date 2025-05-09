package com.example.myratingapp // <-- Убедитесь, что это имя вашего пакета

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class StatisticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics) // Устанавливаем макет activity_statistics.xml

        // Устанавливаем заголовок для Action Bar (опционально)
        supportActionBar?.title = "Статистика"
    }
}