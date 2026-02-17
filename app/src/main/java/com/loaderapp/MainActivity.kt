package com.loaderapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import com.loaderapp.ui.auth.RoleSelectionScreen
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.dispatcher.DispatcherViewModel
import com.loaderapp.ui.dispatcher.DispatcherViewModelFactory
import com.loaderapp.ui.loader.LoaderScreen
import com.loaderapp.ui.loader.LoaderViewModel
import com.loaderapp.ui.loader.LoaderViewModelFactory
import com.loaderapp.ui.theme.LoaderAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Обработка результата запроса разрешения
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = remember(context) { 
        context.applicationContext as LoaderApplication 
    }
    
    var currentUser by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()
    
    // Читаем тёмную тему из DataStore
    val isDarkTheme by app.userPreferences.isDarkTheme.collectAsState(initial = false)
    
    val switchRole = {
        scope.launch {
            app.userPreferences.clearCurrentUser()
            currentUser = null
        }
    }
    
    LaunchedEffect(Unit) {
        app.userPreferences.currentUserId.collect { userId ->
            if (userId != null) {
                currentUser = app.repository.getUserById(userId)
            }
        }
    }
    
    LoaderAppTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                currentUser == null -> {
                    RoleSelectionScreen(
                        onUserCreated = { user ->
                            scope.launch {
                                val userId = app.repository.createUser(user)
                                app.userPreferences.setCurrentUserId(userId)
                                currentUser = user.copy(id = userId)
                            }
                        }
                    )
                }
                currentUser?.role == UserRole.DISPATCHER -> {
                    val viewModel: DispatcherViewModel = viewModel(
                        factory = DispatcherViewModelFactory(
                            application = app,
                            repository = app.repository,
                            dispatcherId = currentUser!!.id
                        )
                    )
                    DispatcherScreen(
                        viewModel = viewModel,
                        userName = currentUser!!.name,
                        onSwitchRole = { switchRole() },
                        onDarkThemeChanged = { enabled ->
                            scope.launch { app.userPreferences.setDarkTheme(enabled) }
                        }
                    )
                }
                currentUser?.role == UserRole.LOADER -> {
                    val viewModel: LoaderViewModel = viewModel(
                        factory = LoaderViewModelFactory(
                            application = app,
                            repository = app.repository,
                            loaderId = currentUser!!.id
                        )
                    )
                    LoaderScreen(
                        viewModel = viewModel,
                        userName = currentUser!!.name,
                        onSwitchRole = { switchRole() },
                        onDarkThemeChanged = { enabled ->
                            scope.launch { app.userPreferences.setDarkTheme(enabled) }
                        }
                    )
                }
            }
        }
    }
}
