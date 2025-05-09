package com.example.myratingapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Класс адаптера
class ItemAdapter(
    private var itemList: MutableList<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    // ViewHolder содержит ссылки на элементы UI одного элемента списка
    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)
        val textViewComment: TextView = itemView.findViewById(R.id.textViewComment) // <-- Находим TextView для Комментария
        val textViewDateAdded: TextView = itemView.findViewById(R.id.textViewDateAdded) // <-- Находим TextView для Даты
        val ratingBarItem: RatingBar = itemView.findViewById(R.id.ratingBarItem)
        val imageViewListItem: ImageView = itemView.findViewById(R.id.imageViewListItem)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = itemList[position]
                    onItemClick.invoke(clickedItem)
                }
            }
        }
    }

    // onCreateViewHolder: создает ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_layout, parent, false)
        return ItemViewHolder(itemView)
    }

    // onBindViewHolder: связывает данные из объекта Item с элементами в ViewHolder
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = itemList[position]

        // Логируем, какие данные передаются
        Log.d("ItemAdapter", "Binding item at position $position: ${currentItem.title}")

        holder.textViewTitle.text = currentItem.title
        holder.textViewCategory.text = currentItem.category

        // --- Отображение Комментария ---
        if (!currentItem.description.isNullOrEmpty()) {
            holder.textViewComment.text = currentItem.description
            holder.textViewComment.visibility = View.VISIBLE // Показываем TextView комментария
        } else {
            holder.textViewComment.text = "" // Очищаем текст
            holder.textViewComment.visibility = View.GONE // Скрываем TextView
        }
        // -----------------------------

        // --- Отображение Форматированной Даты Добавления ---
        try {
            val date = Date(currentItem.dateAdded)
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formattedDate = format.format(date)
            holder.textViewDateAdded.text = "Добавлено: $formattedDate"
            holder.textViewDateAdded.visibility = View.VISIBLE // TextView даты всегда видим
        } catch (e: Exception) {
            Log.e("ItemAdapter", "Error formatting date for item ${currentItem.id}: ${currentItem.dateAdded}", e)
            holder.textViewDateAdded.text = "Добавлено: Неизвестная дата"
            holder.textViewDateAdded.visibility = View.VISIBLE
        }
        // --------------------------------------

        holder.ratingBarItem.rating = currentItem.rating

        // --- Логика отображения картинки с помощью Glide ---
        if (!currentItem.imageUri.isNullOrEmpty()) {
            val imageUri = Uri.parse(currentItem.imageUri)
            Glide.with(holder.itemView.context)
                .load(imageUri)
                .apply(RequestOptions()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_dialog_alert)
                    .centerCrop())
                .into(holder.imageViewListItem)
            holder.imageViewListItem.visibility = View.VISIBLE
        } else {
            Glide.with(holder.itemView.context).clear(holder.imageViewListItem)
            holder.imageViewListItem.setImageDrawable(null)
            holder.imageViewListItem.setBackgroundResource(android.R.color.darker_gray)
            holder.imageViewListItem.visibility = View.VISIBLE
        }
        // -------------------------------------------------
    }

    // getItemCount: возвращает количество элементов
    override fun getItemCount() = itemList.size

    // updateData: метод для обновления списка данных в адаптере
    fun updateData(newData: List<Item>) {
        Log.d("ItemAdapter", "Updating data. New count: ${newData.size}")
        this.itemList.clear()
        this.itemList.addAll(newData)
        notifyDataSetChanged()
    }
}
