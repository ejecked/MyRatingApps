package com.example.myratingapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu // Импортируем Menu
import android.view.MenuItem // Импортируем MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView // Импортируем SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
// Убедитесь, что импортировали константы сортировки из ItemsDatabaseHelper
import com.example.myratingapp.SORT_BY_DATE_DESC
import com.example.myratingapp.SORT_BY_DATE_ASC
import com.example.myratingapp.SORT_BY_RATING_DESC
import com.example.myratingapp.SORT_BY_RATING_ASC
import com.example.myratingapp.SORT_BY_TITLE_ASC
import com.example.myratingapp.SORT_BY_TITLE_DESC


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var itemList: MutableList<Item>

    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var dbHelper: ItemsDatabaseHelper

    // Переменные для хранения текущего состояния поиска и сортировки
    private var currentSearchQuery: String = ""
    private var currentSortOrder: String = SORT_BY_DATE_DESC // Сортировка по умолчанию


    // Константы для ключей Intent extras
    private val EXTRA_ITEM_ID = "ITEM_ID"
    private val EXTRA_ITEM_TITLE = "ITEM_TITLE"
    private val EXTRA_ITEM_DESCRIPTION = "ITEM_DESCRIPTION"
    private val EXTRA_ITEM_RATING = "ITEM_RATING"
    private val RESULT_DELETE = Activity.RESULT_FIRST_USER + 1


    private val addItemActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.resultCode
        // val data: Intent? = result.data // Пока не извлекаем данные здесь, т.к. просто перезагружаем

        when (resultCode) {
            Activity.RESULT_OK -> { // Результат успешный (сохранение)
                // Перезагружаем данные из БД с учетом текущего поиска и сортировки
                refreshItemList()
                Log.d("MainActivity", "Received RESULT_OK, refreshing list.")
            }
            RESULT_DELETE -> { // Наш собственный код для удаления
                // Перезагружаем список после удаления с учетом текущего поиска и сортировки
                refreshItemList()
                Log.d("MainActivity", "Received RESULT_DELETE, refreshing list.")
            }
            Activity.RESULT_CANCELED -> { // Пользователь отменил
                Log.d("MainActivity", "Activity result was RESULT_CANCELED")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewItems)
        recyclerView.layoutManager = LinearLayoutManager(this)

        dbHelper = ItemsDatabaseHelper(this)

        // Инициализируем список как пустой MutableList (будет заполнен при первой загрузке)
        itemList = mutableListOf()

        // Создаем адаптер сразу при первом запуске.
        // Передаем ему изначально пустой список и колбэк нажатия.
        itemAdapter = ItemAdapter(itemList) { clickedItem ->
            Log.d("MainActivity", "Clicked item: ${clickedItem.title}")

            val editIntent = Intent(this, AddItemActivity::class.java)
            editIntent.putExtra(EXTRA_ITEM_ID, clickedItem.id)
            editIntent.putExtra(EXTRA_ITEM_TITLE, clickedItem.title)
            editIntent.putExtra(EXTRA_ITEM_DESCRIPTION, clickedItem.description)
            editIntent.putExtra(EXTRA_ITEM_RATING, clickedItem.rating)

            addItemActivityResultLauncher.launch(editIntent)
        }
        recyclerView.adapter = itemAdapter // Устанавливаем адаптер

        fabAddItem = findViewById(R.id.fabAddItem)
        fabAddItem.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            intent.putExtra(EXTRA_ITEM_ID, -1L) // Указываем, что это новый элемент
            addItemActivityResultLauncher.launch(intent)
        }

        // 1. Загружаем данные при старте с учетом текущих (по умолчанию) параметров поиска и сортировки
        refreshItemList()
    }

    // === Методы для работы с Меню в Action Bar ===

    // Метод вызывается для создания меню в Action Bar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // "Надуваем" (inflating) наше меню из файла ресурса main_menu.xml
        menuInflater.inflate(R.menu.main_menu, menu)

        // --- Настраиваем SearchView ---
        val searchItem: MenuItem? = menu.findItem(R.id.action_search) // Находим пункт поиска по ID
        val searchView: SearchView? = searchItem?.actionView as? SearchView // Получаем сам SearchView

        searchView?.apply {
            queryHint = "Введите для поиска..." // Устанавливаем текст-подсказку
            // Настраиваем слушатель для обработки ввода текста
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                // Вызывается при отправке запроса (например, нажатие Enter)
                override fun onQueryTextSubmit(query: String?): Boolean {
                    currentSearchQuery = query.orEmpty() // Обновляем переменную запроса
                    refreshItemList() // Обновляем список с новым запросом и текущей сортировкой
                    searchView.clearFocus() // Убираем фокус с поля ввода после отправки
                    return true // Сообщаем, что мы обработали событие
                }

                // Вызывается при каждом изменении текста в поле ввода
                override fun onQueryTextChange(newText: String?): Boolean {
                    currentSearchQuery = newText.orEmpty() // Обновляем переменную запроса
                    refreshItemList() // Обновляем список с новым запросом и текущей сортировкой
                    return true // Сообщаем, что мы обработали событие
                }
            })
        }
        // -----------------------------------

        return true // Сообщаем системе, что меню было успешно создано и отображено
    }

    // Метод вызывается при выборе пункта меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Проверяем ID выбранного пункта меню
        return when (item.itemId) {
            // --- Обработка выбора сортировки ---
            R.id.sort_date_desc -> {
                currentSortOrder = SORT_BY_DATE_DESC
                refreshItemList() // Обновляем список с новым порядком сортировки
                true // Сообщаем, что мы обработали событие
            }
            R.id.sort_date_asc -> {
                currentSortOrder = SORT_BY_DATE_ASC
                refreshItemList()
                true
            }
            R.id.sort_rating_desc -> {
                currentSortOrder = SORT_BY_RATING_DESC
                refreshItemList()
                true
            }
            R.id.sort_rating_asc -> {
                currentSortOrder = SORT_BY_RATING_ASC
                refreshItemList()
                true
            }
            R.id.sort_title_asc -> {
                currentSortOrder = SORT_BY_TITLE_ASC
                refreshItemList()
                true
            }
            R.id.sort_title_desc -> {
                currentSortOrder = SORT_BY_TITLE_DESC
                refreshItemList()
                true
            }
            // R.id.action_search не обрабатываем здесь, его обрабатывает слушатель SearchView
            // R.id.action_sort не обрабатываем здесь, это пункт с подменю

            else -> super.onOptionsItemSelected(item) // Для всех остальных пунктов вызываем родительский метод
        }
    }

    // === Метод для загрузки данных из базы данных с учетом поиска/сортировки и обновления UI ===
    private fun refreshItemList() {
        // Решаем, какой метод помощника БД вызвать: searchItems или getAllItems
        val itemsFromDb = if (currentSearchQuery.isBlank()) {
            // Если строка поиска пустая, загружаем все элементы с текущей сортировкой
            dbHelper.getAllItems(currentSortOrder)
        } else {
            // Если строка поиска не пустая, выполняем поиск с текущей строкой и сортировкой
            dbHelper.searchItems(currentSearchQuery, currentSortOrder)
        }

        // Обновляем список данных в адаптере
        itemAdapter.updateData(itemsFromDb)

        // notifyDataSetChanged() вызывается внутри updateData
        Log.d("MainActivity", "Item list refreshed. Query: '$currentSearchQuery', Order: '$currentSortOrder', Count: ${itemsFromDb.size}")
    }


    // Важно закрывать базу данных, когда Activity уничтожается
    override fun onDestroy() {
        super.onDestroy()
        if (::dbHelper.isInitialized) {
            dbHelper.close()
        }
    }
}