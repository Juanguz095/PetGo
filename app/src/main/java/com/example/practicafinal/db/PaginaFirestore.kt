package com.example.practicafinal.db

import com.google.firebase.firestore.DocumentSnapshot

data class PaginaFirestore<T>(
    val items: List<T>,
    val cursor: DocumentSnapshot?,
    val hayMas: Boolean
)
