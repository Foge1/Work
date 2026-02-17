package com.loaderapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.loaderapp.ui.splash.SplashScreen
import com.loaderapp.ui.theme.LoaderAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent { MainScreen() }
    }
}

private enum class AppState { SPLASH, CONTENT }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as LoaderApplication }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var appState by remember { mutableStateOf(AppState.SPLASH) }
    val scope = rememberCoroutineScope()
    val isDarkTheme by app.userPreferences.isDarkTheme.collectAsState(initial = false)

    val switchRole = {
        scope.launch {
            app.userPreferences.clearCurrentUser()
            currentUser = null
        }
    }

    LaunchedEffect(Unit) {
        app.userPreferences.currentUserId.collect { userId ->
            if (userId != null) currentUser = app.repository.getUserById(userId)
        }
    }

    LoaderAppTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = appState,
                transitionSpec = {
                    if (targetState == AppState.CONTENT) {
                        fadeIn(tween(500)) togetherWith fadeOut(tween(300))
                    } else {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    }
                },
                label = "app_state"
            ) { state ->
                when (state) {
                    AppState.SPLASH -> {
                        SplashScreen(onFinished = { appState = AppState.CONTENT })
                    }
                    AppState.CONTENT -> {
                        AnimatedContent(
                            targetState = currentUser,
                            transitionSpec = {
                                fadeIn(tween(350)) + slideInHorizontally(tween(350), initialOffsetX = { it / 8 }) togetherWith
                                fadeOut(tween(250)) + slideOutHorizontally(tween(250), targetOffsetX = { -it / 8 })
                            },
                            label = "screen_transition"
                        ) { user ->
                            when {
                                user == null -> {
                                    RoleSelectionScreen(onUserCreated = { newUser ->
                                        scope.launch {
                                            val userId = app.repository.createUser(newUser)
                                            app.userPreferences.setCurrentUserId(userId)
                                            currentUser = newUser.copy(id = userId)
                                        }
                                    })
                                }
                                user.role == UserRole.DISPATCHER -> {
                                    val viewModel: DispatcherViewModel = viewModel(
                                        factory = DispatcherViewModelFactory(app, app.repository, user.id)
                                    )
                                    DispatcherScreen(
                                        viewModel = viewModel,
                                        userName = user.name,
                                        onSwitchRole = { switchRole() },
                                        onDarkThemeChanged = { enabled -> scope.launch { app.userPreferences.setDarkTheme(enabled) } }
                                    )
                                }
                                user.role == UserRole.LOADER -> {
                                    val viewModel: LoaderViewModel = viewModel(
                                        factory = LoaderViewModelFactory(app, app.repository, user.id)
                                    )
                                    LoaderScreen(
                                        viewModel = viewModel,
                                        userName = user.name,
                                        onSwitchRole = { switchRole() },
                                        onDarkThemeChanged = { enabled -> scope.launch { app.userPreferences.setDarkTheme(enabled) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
