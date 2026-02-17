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
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import com.loaderapp.ui.auth.RoleSelectionScreen
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.dispatcher.DispatcherViewModel
import com.loaderapp.ui.dispatcher.DispatcherViewModelFactory
import com.loaderapp.ui.loader.LoaderScreen
import com.loaderapp.ui.loader.LoaderViewModel
import com.loaderapp.ui.loader.LoaderViewModelFactory
import com.loaderapp.ui.order.OrderDetailScreen
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

// Навигационные состояния всего приложения
private sealed class AppScreen {
    object Splash : AppScreen()
    object Auth : AppScreen()
    data class Dispatcher(val user: User) : AppScreen()
    data class Loader(val user: User) : AppScreen()
    data class OrderDetail(
        val order: Order,
        val dispatcher: User?,
        val worker: User?,
        val isDispatcher: Boolean,
        val onTake: ((Order) -> Unit)?,
        val onComplete: ((Order) -> Unit)?,
        val onCancel: ((Order) -> Unit)?
    ) : AppScreen()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as LoaderApplication }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    val scope = rememberCoroutineScope()
    val isDarkTheme by app.userPreferences.isDarkTheme.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        app.userPreferences.currentUserId.collect { userId ->
            if (userId != null) currentUser = app.repository.getUserById(userId)
        }
    }

    val switchRole = {
        scope.launch {
            app.userPreferences.clearCurrentUser()
            currentUser = null
            screen = AppScreen.Auth
        }
    }

    LoaderAppTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    when {
                        // Splash → что угодно: плавное растворение
                        initialState is AppScreen.Splash ->
                            fadeIn(tween(400, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(300))

                        // Переход на детали заказа: слайд снизу вверх
                        targetState is AppScreen.OrderDetail ->
                            (fadeIn(tween(280)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 6 }) togetherWith
                            fadeOut(tween(180))

                        // Назад с деталей: слайд вниз
                        initialState is AppScreen.OrderDetail ->
                            fadeIn(tween(220)) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it / 6 })

                        // Auth → главный экран: слайд слева
                        initialState is AppScreen.Auth ->
                            (fadeIn(tween(350)) + slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 5 }) togetherWith
                            fadeOut(tween(200))

                        else ->
                            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    }
                },
                label = "root_nav"
            ) { s ->
                when (s) {
                    is AppScreen.Splash -> SplashScreen(onFinished = {
                        screen = if (currentUser != null) {
                            when (currentUser!!.role) {
                                UserRole.DISPATCHER -> AppScreen.Dispatcher(currentUser!!)
                                UserRole.LOADER -> AppScreen.Loader(currentUser!!)
                            }
                        } else AppScreen.Auth
                    })

                    is AppScreen.Auth -> RoleSelectionScreen(onUserCreated = { newUser ->
                        scope.launch {
                            val userId = app.repository.createUser(newUser)
                            app.userPreferences.setCurrentUserId(userId)
                            val saved = newUser.copy(id = userId)
                            currentUser = saved
                            screen = when (saved.role) {
                                UserRole.DISPATCHER -> AppScreen.Dispatcher(saved)
                                UserRole.LOADER -> AppScreen.Loader(saved)
                            }
                        }
                    })

                    is AppScreen.Dispatcher -> {
                        val viewModel: DispatcherViewModel = viewModel(
                            factory = DispatcherViewModelFactory(app, app.repository, s.user.id)
                        )
                        DispatcherScreen(
                            viewModel = viewModel,
                            userName = s.user.name,
                            onSwitchRole = { switchRole() },
                            onDarkThemeChanged = { enabled -> scope.launch { app.userPreferences.setDarkTheme(enabled) } },
                            onOrderClick = { order, dispatcher, worker ->
                                screen = AppScreen.OrderDetail(
                                    order = order,
                                    dispatcher = dispatcher,
                                    worker = worker,
                                    isDispatcher = true,
                                    onTake = null,
                                    onComplete = null,
                                    onCancel = { o -> viewModel.cancelOrder(o); screen = AppScreen.Dispatcher(s.user) }
                                )
                            }
                        )
                    }

                    is AppScreen.Loader -> {
                        val viewModel: LoaderViewModel = viewModel(
                            factory = LoaderViewModelFactory(app, app.repository, s.user.id)
                        )
                        LoaderScreen(
                            viewModel = viewModel,
                            userName = s.user.name,
                            onSwitchRole = { switchRole() },
                            onDarkThemeChanged = { enabled -> scope.launch { app.userPreferences.setDarkTheme(enabled) } },
                            onOrderClick = { order, dispatcher, worker ->
                                screen = AppScreen.OrderDetail(
                                    order = order,
                                    dispatcher = dispatcher,
                                    worker = worker,
                                    isDispatcher = false,
                                    onTake = { o ->
                                        viewModel.takeOrder(o)
                                        screen = AppScreen.Loader(s.user)
                                    },
                                    onComplete = { o ->
                                        viewModel.completeOrder(o)
                                        screen = AppScreen.Loader(s.user)
                                    },
                                    onCancel = null
                                )
                            }
                        )
                    }

                    is AppScreen.OrderDetail -> OrderDetailScreen(
                        order = s.order,
                        dispatcher = s.dispatcher,
                        worker = s.worker,
                        isDispatcher = s.isDispatcher,
                        onBack = { screen = if (currentUser?.role == UserRole.DISPATCHER) AppScreen.Dispatcher(currentUser!!) else AppScreen.Loader(currentUser!!) },
                        onTakeOrder = s.onTake,
                        onCompleteOrder = s.onComplete,
                        onCancelOrder = s.onCancel
                    )
                }
            }
        }
    }
}
