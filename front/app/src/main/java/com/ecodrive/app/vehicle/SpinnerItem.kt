package com.ecodrive.app.vehicle

interface SpinnerItem<E> {
    val code: E?
    val label: String
    val description: String
}