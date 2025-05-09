package com.example.myratingapp // <-- Убедитесь, что это имя вашего пакета

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class CategoryManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_management) // Устанавливаем макет activity_category_management.xml

        // Устанавливаем заголовок для Action Bar (опционально)
        supportActionBar?.title = "Управление категориями"
    }
}