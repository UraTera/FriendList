package com.tera.friendlist.utils

data class ModelPerson(
    var id: Int,
    var photo: String,
    var name: String,
    var date: String,    // Дата рождения
    var died: String,    // умер
    var age: String,     // Возраст
    var dayLeft: String, // До дна рождения
    var note: String,    // Заметка
    var keyDel: Boolean
)
