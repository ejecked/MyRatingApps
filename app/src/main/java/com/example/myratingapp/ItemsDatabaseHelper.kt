package com.example.myratingapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// Константы для базы данных и таблицы (оставляем как есть)
private const val DATABASE_NAME = "items.db"
private const val DATABASE_VERSION = 3 // Версия базы данных с категориями

// Константы для таблицы элементов (оставляем как есть)
private const val TABLE_ITEMS = "items"
private const val COLUMN_ID = "id"
private const val COLUMN_TITLE = "title"
private const val COLUMN_DESCRIPTION = "description" // Теперь для комментариев
private const val COLUMN_RATING = "rating"
private const val COLUMN_DATE_ADDED = "date_added"
private const val COLUMN_IMAGE_URI = "image_uri"
private const val COLUMN_CATEGORY = "category"

// Константы для вариантов сортировки (оставляем как есть)
const val SORT_BY_DATE_DESC = "$COLUMN_DATE_ADDED DESC"
const val SORT_BY_DATE_ASC = "$COLUMN_DATE_ADDED ASC"
const val SORT_BY_RATING_DESC = "$COLUMN_RATING DESC"
const val SORT_BY_RATING_ASC = "$COLUMN_RATING ASC"
const val SORT_BY_TITLE_ASC = "$COLUMN_TITLE ASC"
const val SORT_BY_TITLE_DESC = "$COLUMN_TITLE DESC"
// Можно добавить константы для сортировки по категории

// Список категорий по умолчанию (можно вынести в ресурсы или отдельный файл)
val DEFAULT_CATEGORIES = listOf("Фильм", "Книга", "Музыка", "Игра", "Товар", "Прочее")
const val DEFAULT_CATEGORY = "Прочее" // Значение по умолчанию в БД

// <-- НОВАЯ КОНСТАНТА ДЛЯ ОПЦИИ "ВСЕ КАТЕГОРИИ" В ФИЛЬТРЕ
const val ALL_CATEGORIES_FILTER = "Все категории"


class ItemsDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, 4) {

    // ... методы onCreate и onUpgrade остаются как есть ...
    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_ITEMS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_RATING REAL NOT NULL,
                $COLUMN_DATE_ADDED INTEGER NOT NULL,
                $COLUMN_IMAGE_URI TEXT,
                $COLUMN_CATEGORY TEXT NOT NULL DEFAULT '$DEFAULT_CATEGORY'
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
        Log.d("DatabaseHelper", "Database table $TABLE_ITEMS created (Version $DATABASE_VERSION)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val alterTableQueryV2 = "ALTER TABLE $TABLE_ITEMS ADD COLUMN $COLUMN_IMAGE_URI TEXT"
            db?.execSQL(alterTableQueryV2)
            Log.d("DatabaseHelper", "Migrated database from version $oldVersion to 2: Added $COLUMN_IMAGE_URI column")
        }
        if (oldVersion < 3) {
            val alterTableQueryV3 = "ALTER TABLE $TABLE_ITEMS ADD COLUMN $COLUMN_CATEGORY TEXT NOT NULL DEFAULT '$DEFAULT_CATEGORY'"
            db?.execSQL(alterTableQueryV3)
            Log.d("DatabaseHelper", "Migrated database from version $oldVersion to 3: Added $COLUMN_CATEGORY column with default")
        }
        Log.d("DatabaseHelper", "Database table $TABLE_ITEMS upgraded from $oldVersion to $newVersion. Handled specific migrations up to version ${newVersion-1}.")
    }

    // ... методы addItem и updateItem остаются как есть (обновлены в предыдущем шаге для категорий) ...
    fun addItem(item: Item): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, item.title)
            put(COLUMN_DESCRIPTION, item.description)
            put(COLUMN_RATING, item.rating)
            put(COLUMN_DATE_ADDED, item.dateAdded)
            put(COLUMN_IMAGE_URI, item.imageUri)
            put(COLUMN_CATEGORY, item.category) // Сохраняем category
        }
        val id = db.insert(TABLE_ITEMS, null, values)
        db.close()
        Log.d("DatabaseHelper", "Item added with ID: $id, Category: ${item.category}")
        return id
    }

    fun updateItem(item: Item): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, item.title)
            put(COLUMN_DESCRIPTION, item.description)
            put(COLUMN_RATING, item.rating)
            put(COLUMN_IMAGE_URI, item.imageUri)
            put(COLUMN_CATEGORY, item.category) // Обновляем category
        }
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(item.id.toString())
        val count = db.update(TABLE_ITEMS, values, selection, selectionArgs)
        db.close()
        Log.d("DatabaseHelper", "Item with ID ${item.id} updated. Rows affected: $count, Category: ${item.category}")
        return count
    }


    // ... метод deleteItem остается как есть ...
    fun deleteItem(itemId: Long): Int {
        val db = this.writableDatabase
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(itemId.toString())
        val count = db.delete(TABLE_ITEMS, selection, selectionArgs)
        db.close()
        Log.d("DatabaseHelper", "Item with ID $itemId deleted. Rows affected: $count")
        return count
    }


    // === Модифицированный и объединенный метод для получения данных с фильтром, поиском и сортировкой ===

    // Метод для получения элементов с учетом фильтрации по категории, поискового запроса и сортировки
    fun getItems(categoryFilter: String, query: String, orderBy: String): List<Item> {
        val itemList = mutableListOf<Item>()
        val db = this.readableDatabase
        // Выбираем все необходимые колонки
        val selectColumns = arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_RATING, COLUMN_DATE_ADDED, COLUMN_IMAGE_URI, COLUMN_CATEGORY)

        var selection: String? = null // Начальное условие WHERE (может быть null)
        val selectionArgs = mutableListOf<String>() // Аргументы для условия WHERE

        val searchQuery = "%$query%" // Подготавливаем поисковый запрос для LIKE

        // 1. Строим часть условия WHERE для поискового запроса (если запрос не пустой)
        if (query.isNotBlank()) {
            selection = "$COLUMN_TITLE LIKE ? OR $COLUMN_DESCRIPTION LIKE ?" // Ищем в названии ИЛИ описании
            selectionArgs.add(searchQuery)
            selectionArgs.add(searchQuery)
        }

        // 2. Добавляем часть условия WHERE для фильтрации по категории (если выбрана не "Все категории")
        if (categoryFilter != ALL_CATEGORIES_FILTER) {
            val categoryCondition = "$COLUMN_CATEGORY = ?"
            if (selection == null) {
                // Если поискового запроса не было, условие по категории становится первым
                selection = categoryCondition
            } else {
                // Если поисковый запрос был, объединяем условия через AND
                selection = "$selection AND $categoryCondition"
            }
            selectionArgs.add(categoryFilter) // Добавляем выбранную категорию как аргумент
        }

        // Преобразуем список аргументов в массив String (требуется методом db.query)
        val selectionArgsArray = if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray()


        // Выполняем запрос к базе данных
        val cursor: Cursor? = db.query(
            TABLE_ITEMS,         // Таблица
            selectColumns,       // Колонки
            selection,           // Условие WHERE (фильтрация + поиск)
            selectionArgsArray,  // Аргументы для WHERE
            null,                // Группировка (GROUP BY)
            null,                // Условие для группировки (HAVING)
            orderBy              // Параметр сортировки (ORDER BY)
        )

        // Читаем данные из курсора
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                val description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                val rating = it.getFloat(it.getColumnIndexOrThrow(COLUMN_RATING))
                val dateAdded = it.getLong(it.getColumnIndexOrThrow(COLUMN_DATE_ADDED))
                val imageUriString = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_URI))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))

                val item = Item(id, title, description, rating, dateAdded, imageUriString, category)
                itemList.add(item)
            }
        }

        db.close()
        Log.d("DatabaseHelper", "Fetched ${itemList.size} items. Filter: '$categoryFilter', Query: '$query', Order: '$orderBy'")
        return itemList
    }

    // Модифицируем getAllItems, чтобы он просто вызывал getItems с параметрами по умолчанию
    fun getAllItems(orderBy: String = SORT_BY_DATE_DESC): List<Item> {
        // Возвращает все элементы (фильтр "Все категории", пустой поисковый запрос) с указанной сортировкой
        return getItems(ALL_CATEGORIES_FILTER, "", orderBy)
    }

    // Модифицируем searchItems, чтобы он просто вызывал getItems с параметрами по умолчанию для категории
    // Если вам нужен отдельный метод searchItems, он должен вызвать getItems
    fun searchItems(query: String, orderBy: String = SORT_BY_DATE_DESC): List<Item> {
        // Вызывает getItems с фильтром "Все категории", указанным поисковым запросом и сортировкой
        return getItems(ALL_CATEGORIES_FILTER, query, orderBy)
    }


    // ... метод getItemById остается как есть (обновлен в предыдущем шаге для категорий) ...
    fun getItemById(id: Long): Item? {
        val db = this.readableDatabase
        val selectColumns = arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_RATING, COLUMN_DATE_ADDED, COLUMN_IMAGE_URI, COLUMN_CATEGORY)
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(id.toString())

        val cursor: Cursor? = db.query(
            TABLE_ITEMS,
            selectColumns,
            selection,
            selectionArgs,
            null, null, null
        )

        var item: Item? = null
        cursor?.use {
            if (it.moveToFirst()) {
                val itemId = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                val description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                val rating = it.getFloat(it.getColumnIndexOrThrow(COLUMN_RATING))
                val dateAdded = it.getLong(it.getColumnIndexOrThrow(COLUMN_DATE_ADDED))
                val imageUriString = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_URI))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))

                item = Item(itemId, title, description, rating, dateAdded, imageUriString, category)
            }
        }
        cursor?.close()
        return item
    }


    // Важно закрывать базу данных, когда Activity уничтожается

}