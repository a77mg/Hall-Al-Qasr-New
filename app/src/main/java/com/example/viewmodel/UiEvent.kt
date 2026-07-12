package com.example.viewmodel

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
}
