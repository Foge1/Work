package com.loaderapp.ui.dispatcher

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.loaderapp.data.repository.AppRepository

class DispatcherViewModelFactory(
    private val application: Application,
    private val repository: AppRepository,
    private val dispatcherId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DispatcherViewModel::class.java)) {
            return DispatcherViewModel(application, repository, dispatcherId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
