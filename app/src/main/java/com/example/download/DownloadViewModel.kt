package com.example.download

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DownloadRepository

    init {
        val dao = DownloadDatabase.getInstance(application).downloadDao()
        repository = DownloadRepository(dao)
    }

    val downloads: StateFlow<List<DownloadTask>> = repository.downloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
