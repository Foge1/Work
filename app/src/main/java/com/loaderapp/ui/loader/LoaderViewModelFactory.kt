package com.loaderapp.ui.loader

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.loaderapp.data.repository.AppRepository

class LoaderViewModelFactory(
    private val application: Application,
    private val repository: AppRepository,
    private val loaderId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoaderViewModel::class.java)) {
            return LoaderViewModel(application, repository, loaderId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
