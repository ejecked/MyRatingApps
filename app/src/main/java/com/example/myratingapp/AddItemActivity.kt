package com.example.myratingapp

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.* // <-- Импортируем все из widget, включая Spinner и ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.myratingapp.DEFAULT_CATEGORIES // <-- Импортируем список категорий из ItemsDatabaseHelper


class AddItemActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var ratingBarInput: RatingBar
    private lateinit var buttonSaveItem: Button
    private lateinit var buttonDeleteItem: Button
    private lateinit var imageViewItem: ImageView
    private lateinit var spinnerCategory: Spinner // <-- Объявляем переменную для Spinner'а

    private var imageUri: Uri? = null

    private lateinit var dbHelper: ItemsDatabaseHelper

    private var itemId: Long = -1L // Переменная для хранения ID элемента. -1L = НОВЫЙ элемент.


    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    // Константы для ключей Intent extras
    private val EXTRA_ITEM_ID = "ITEM_ID"
    private val EXTRA_ITEM_TITLE = "ITEM_TITLE"
    private val EXTRA_ITEM_DESCRIPTION = "ITEM_DESCRIPTION"
    private val EXTRA_ITEM_RATING = "ITEM_RATING"
    private val EXTRA_ITEM_IMAGE_URI = "ITEM_IMAGE_URI"
    private val EXTRA_ITEM_CATEGORY = "ITEM_CATEGORY" // <-- НОВАЯ КОНСТАНТА для Категории

    // Константа для кода результата удаления
    private val RESULT_DELETE = Activity.RESULT_FIRST_USER + 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        dbHelper = ItemsDatabaseHelper(this)

        // Получаем ссылки на элементы UI
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextDescription = findViewById(R.id.editTextDescription)
        ratingBarInput = findViewById(R.id.ratingBarInput)
        buttonSaveItem = findViewById(R.id.buttonSaveItem)
        buttonDeleteItem = findViewById(R.id.buttonDeleteItem)
        imageViewItem = findViewById(R.id.imageViewItem)
        spinnerCategory = findViewById(R.id.spinnerCategory) // <-- Находим Spinner

        // --- Настраиваем Spinner категорий ---
        // Создаем адаптер для Spinner'а, используя список категорий из ItemsDatabaseHelper
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, // Стандартный макет элемента выпадающего списка
            DEFAULT_CATEGORIES // Наш список категорий
        )
        // Устанавливаем адаптер для Spinner'а
        spinnerCategory.adapter = categoryAdapter
        // ------------------------------------

        // --- Регистрация ActivityResultLauncher для выбора картинки ---
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null && data.data != null) {
                    imageUri = data.data
                    Glide.with(this)
                        .load(imageUri)
                        .apply(RequestOptions().placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_dialog_alert))
                        .into(imageViewItem)
                    Log.d("AddItemActivity", "Image selected with URI: $imageUri")
                } else {
                    Toast.makeText(this, "Выбор картинки отменен", Toast.LENGTH_SHORT).show()
                    Log.d("AddItemActivity", "Image selection canceled or data is null")
                }
            } else {
                Toast.makeText(this, "Выбор картинки не удался", Toast.LENGTH_SHORT).show()
                Log.d("AddItemActivity", "Image selection failed with result code: ${result.resultCode}")
            }
        }
        // -------------------------------------------------------------


        // --- Проверяем Intent на наличие данных элемента (режим редактирования) ---
        val intent = intent
        itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)

        if (itemId != -1L) { // Если ID существует, это режим редактирования
            val title = intent.getStringExtra(EXTRA_ITEM_TITLE)
            val description = intent.getStringExtra(EXTRA_ITEM_DESCRIPTION)
            val rating = intent.getFloatExtra(EXTRA_ITEM_RATING, 0.0f)
            val imageUriString = intent.getStringExtra(EXTRA_ITEM_IMAGE_URI)
            val category = intent.getStringExtra(EXTRA_ITEM_CATEGORY) // <-- Получаем категорию из Intent

            editTextTitle.setText(title)
            editTextDescription.setText(description)
            ratingBarInput.rating = rating

            // --- Устанавливаем выбранную категорию в Spinner ---
            if (category != null) {
                // Находим позицию категории в нашем списке категорий
                val categoryPosition = DEFAULT_CATEGORIES.indexOf(category)
                if (categoryPosition >= 0) {
                    // Устанавливаем Spinner на найденную позицию
                    spinnerCategory.setSelection(categoryPosition)
                } else {
                    // Если категория не найдена в списке (например, добавлена в старой версии),
                    // можно установить дефолтное значение или оставить как есть.
                    // По умолчанию останется выбранным первый элемент или предыдущее значение.
                    // Можем установить на категорию "Прочее", если она есть
                    val defaultPosition = DEFAULT_CATEGORIES.indexOf(DEFAULT_CATEGORY)
                    if (defaultPosition >= 0) {
                        spinnerCategory.setSelection(defaultPosition)
                    }
                    Log.w("AddItemActivity", "Category '$category' not found in default list.")
                }
            } else {
                // Если категория null (например, из старых записей версии < 3), устанавливаем дефолт
                val defaultPosition = DEFAULT_CATEGORIES.indexOf(DEFAULT_CATEGORY)
                if (defaultPosition >= 0) {
                    spinnerCategory.setSelection(defaultPosition)
                }
            }
            // ----------------------------------------------------


            if (!imageUriString.isNullOrEmpty()) {
                imageUri = Uri.parse(imageUriString)
                Glide.with(this)
                    .load(imageUri)
                    .apply(RequestOptions().placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_dialog_alert))
                    .into(imageViewItem)
                Log.d("AddItemActivity", "Loaded existing Image URI: $imageUri")
            } else {
                imageViewItem.setImageDrawable(null)
                imageViewItem.setBackgroundResource(android.R.color.darker_gray)
                Log.d("AddItemActivity", "No existing Image URI found in Intent.")
            }


            setTitle("Редактировать элемент")
            buttonDeleteItem.visibility = View.VISIBLE

        } else { // Если ID -1L, это новый элемент
            buttonDeleteItem.visibility = View.GONE
            setTitle("Добавить новый элемент")
            imageViewItem.setImageDrawable(null)
            imageViewItem.setBackgroundResource(android.R.color.darker_gray)
            // Для нового элемента, Spinner по умолчанию будет показывать первый элемент списка
        }
        // ----------------------------------------------------------------------

        // --- Настраиваем обработчик клика по ImageView для выбора картинки ---
        imageViewItem.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK)
            galleryIntent.type = "image/*"
            pickImageLauncher.launch(galleryIntent)
        }
        // -------------------------------------------------------------------


        // Настраиваем нажатие на кнопку "Сохранить"
        buttonSaveItem.setOnClickListener {
            saveItem()
        }

        // Настраиваем нажатие на кнопку "Удалить"
        buttonDeleteItem.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    // --- Метод для загрузки данных существующего элемента из базы данных (не используется в этой версии, данные берутся из Intent) ---
    // Если вы решили использовать загрузку из БД, активируйте этот метод и вызовите его в onCreate
    /*
    private fun loadItemData() {
        // ... код загрузки из БД с помощью dbHelper.getItemById() ...
        // При загрузке из БД нужно также установить Spinner:
        // val categoryPosition = DEFAULT_CATEGORIES.indexOf(item.category)
        // if (categoryPosition >= 0) spinnerCategory.setSelection(categoryPosition)
    }
    */


    private fun saveItem() {
        val title = editTextTitle.text.toString().trim()
        val description = editTextDescription.text.toString().trim()
        val rating = ratingBarInput.rating
        val selectedCategory = spinnerCategory.selectedItem.toString() // <-- Получаем выбранную категорию из Spinner'а

        if (title.isEmpty()) {
            editTextTitle.error = "Введите название"
            editTextTitle.requestFocus()
            return
        }

        // Создаем объект Item. Включаем category и imageUri?.toString()
        val itemToSave = Item(itemId, title, description, rating, imageUri = imageUri?.toString(), category = selectedCategory) // <-- Включаем category

        val resultId: Long

        if (itemId == -1L) {
            resultId = dbHelper.addItem(itemToSave)
            if (resultId != -1L) itemId = resultId
            Log.d("AddItemActivity", "Added item, new ID: $itemId, Category: ${itemToSave.category}, URI: ${itemToSave.imageUri}")
        } else {
            resultId = dbHelper.updateItem(itemToSave).toLong()
            Log.d("AddItemActivity", "Updated item with ID: $itemId, Rows affected: $resultId, Category: ${itemToSave.category}, URI: ${itemToSave.imageUri}")
        }

        if (resultId != -1L || (itemId != -1L && resultId >= 0L)) {
            Toast.makeText(this, "Сохранение успешно!", Toast.LENGTH_SHORT).show()

            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_ITEM_ID, itemId)
            resultIntent.putExtra(EXTRA_ITEM_IMAGE_URI, itemToSave.imageUri)
            resultIntent.putExtra(EXTRA_ITEM_CATEGORY, itemToSave.category) // <-- Передаем категорию обратно

            setResult(Activity.RESULT_OK, resultIntent)

            finish()

        } else {
            Toast.makeText(this, "Ошибка при сохранении!", Toast.LENGTH_SHORT).show()
        }
    }

    // ... Методы showDeleteConfirmationDialog() и deleteItem() остаются без изменений ...
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Подтверждение удаления")
            .setMessage("Вы уверены, что хотите удалить этот элемент?")
            .setPositiveButton("Удалить") { dialog: DialogInterface, which: Int ->
                deleteItem()
            }
            .setNegativeButton("Отмена") { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteItem() {
        if (itemId != -1L) {
            val deletedRows = dbHelper.deleteItem(itemId)

            if (deletedRows > 0) {
                Toast.makeText(this, "Элемент удален!", Toast.LENGTH_SHORT).show()

                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_ITEM_ID, itemId)
                // Можно также передать URI удаленного изображения, если нужно удалить сам файл (более сложно)
                setResult(RESULT_DELETE, resultIntent)

                finish()

            } else {
                Toast.makeText(this, "Ошибка при удалении!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("AddItemActivity", "Attempted to delete an item with ID -1L")
            Toast.makeText(this, "Нельзя удалить несохраненный элемент.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }


    // Важно закрывать базу данных, когда Activity уничтожается
    override fun onDestroy() {
        super.onDestroy()
        if (::dbHelper.isInitialized) {
            dbHelper.close()
        }
    }

    // === Примечание о загрузке данных из Intent vs из БД ===
    // В текущей реализации данные для редактирования берутся из Intent в onCreate.
    // Это проще, но менее надежно, чем загрузка из БД.
    // Если вы хотите загружать из БД, вам понадобится метод getItemById в ItemsDatabaseHelper
    // и вызов метода loadItemData() (реализованного для загрузки из БД) в onCreate.
}