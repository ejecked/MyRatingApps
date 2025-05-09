package com.example.myratingapp

data class Item(
    var id: Long = 0,
    var title: String,
    var description: String?,
    var rating: Float,
    var dateAdded: Long = System.currentTimeMillis(),
    var imageUri: String? = null,
    var category: String = "Прочее"

)