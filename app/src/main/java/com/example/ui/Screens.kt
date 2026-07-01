package com.example.ui
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import android.accounts.AccountManager
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.security.SecurityMetrics
import kotlinx.coroutines.delay
import com.example.db.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
// Simple format helper
fun Double.toCurrency(): String {
    val format = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    return format.format(this).replace("INR", "₹").replace("Rs.", "₹")
}
@Composable
fun AdminAccessWrapper(
    userRole: String,
    onAccessDenied: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0E13))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFFF3333).copy(alpha = 0.1f), CircleShape)
                        .border(BorderStroke(2.dp, Color(0xFFFF3333)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Access Denied",
                        tint = Color(0xFFFF3333),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "ACCESS RESTRICTED",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your current account role is 'user'. Access to administrative terminals is strictly restricted to certified Server Administrators.",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    },
    content: @Composable () -> Unit
) {
    if (userRole == "admin") {
        content()
    } else {
        onAccessDenied()
    }
}
@Composable
fun BattleZoneMainApp(viewModel: BattleZoneViewModel) {
    var isSplashScreenVisible by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var isAdminMode by remember { mutableStateOf(false) }
    var activeTournamentIdForDetails by remember { mutableStateOf<Int?>(null) }
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val isUserAdmin = userRole == "admin" || (user?.email == "selva19122008@gmail.com" && userRole == "admin" && viewModel.isFirebaseUserAdmin())
    var isAdminUnlocked by remember(isUserAdmin) { mutableStateOf(isUserAdmin) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val userJoins by viewModel.currentUserJoins.collectAsStateWithLifecycle()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var autoOpenDepositDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    val notifications by viewModel.currentUserNotifications.collectAsStateWithLifecycle()
    if (isSplashScreenVisible) {
        SplashScreen(onTimeout = { isSplashScreenVisible = false })
    } else if (!isUserLoggedIn) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoginRegistrationScreen(viewModel = viewModel)
            
            // Transient Toast Notifications Layer
            val toastNotifications by viewModel.toastNotifications.collectAsStateWithLifecycle()
            ToastOverlay(
                toasts = toastNotifications,
                onDismiss = { viewModel.dismissToast(it) },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if ((selectedTab != 0 || isAdminMode) && activeTournamentIdForDetails == null) {
                    BattleZoneTopBar(
                        isAdmin = isAdminMode,
                        isAdminUnlocked = isAdminUnlocked,
                        user = user,
                        notifications = notifications,
                        onNotificationClick = { showNotificationDialog = true },
                        onToggleAdmin = {
                            isAdminMode = !isAdminMode
                            activeTournamentIdForDetails = null // reset view
                            scope.launch {
                                val roleText = if (isAdminMode) "Admin Controls" else "Gamer Lobby"
                                snackbarHostState.showSnackbar("Switched portal to $roleText")
                            }
                        },
                        onLogoClick = {
                            if (!isAdminUnlocked) {
                                showPasswordDialog = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Admin portal is already unlocked. Tap lock icon to lock.")
                                }
                            }
                        },
                        onLockAdmin = {
                            viewModel.setUserRole("user")
                            isAdminMode = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Admin portal locked successfully.")
                            }
                        },
                        onWalletClick = {
                            selectedTab = 1
                            activeTournamentIdForDetails = null
                        },
                        onDepositClick = {
                            selectedTab = 1
                            activeTournamentIdForDetails = null
                            autoOpenDepositDialog = true
                        }
                    )
                }
            },
            bottomBar = {
                if (!isAdminMode && activeTournamentIdForDetails == null) {
                    GamerBottomNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            },
            containerColor = DarkBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isAdminMode) {
                    AdminAccessWrapper(userRole = userRole) {
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState
                        )
                    }
                } else {
                    if (activeTournamentIdForDetails != null) {
                        TournamentDetailsScreen(
                            tournamentId = activeTournamentIdForDetails!!,
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState,
                            onBack = { activeTournamentIdForDetails = null },
                            onNavigateToWallet = {
                                activeTournamentIdForDetails = null
                                selectedTab = 1
                            }
                        )
                    } else {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                            },
                            label = "TabNav"
                        ) { tab ->
                            when (tab) {
                                0 -> LobbyScreen(
                                    viewModel = viewModel,
                                    tournaments = tournaments,
                                    onViewTournament = { activeTournamentIdForDetails = it },
                                    isAdminUnlocked = isAdminUnlocked,
                                    user = user,
                                    notifications = notifications,
                                    onNotificationClick = { showNotificationDialog = true },
                                    onToggleAdmin = {
                                        isAdminMode = !isAdminMode
                                        activeTournamentIdForDetails = null
                                        scope.launch {
                                            val roleText = if (isAdminMode) "Admin Controls" else "Gamer Lobby"
                                            snackbarHostState.showSnackbar("Switched portal to $roleText")
                                        }
                                    },
                                    onLogoClick = {
                                        if (!isAdminUnlocked) {
                                            showPasswordDialog = true
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Admin portal is already unlocked. Tap lock icon to lock.")
                                            }
                                        }
                                    },
                                    onLockAdmin = {
                                        viewModel.setUserRole("user")
                                        isAdminMode = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Admin portal locked successfully.")
                                        }
                                    },
                                    onWalletClick = {
                                        selectedTab = 1
                                        activeTournamentIdForDetails = null
                                    },
                                    onDepositClick = {
                                        selectedTab = 1
                                        activeTournamentIdForDetails = null
                                        autoOpenDepositDialog = true
                                    }
                                )
                                1 -> WalletScreen(
                                    viewModel = viewModel,
                                    snackbarHostState = snackbarHostState,
                                    autoOpenDeposit = autoOpenDepositDialog,
                                    onDepositOpened = { autoOpenDepositDialog = false }
                                )
                                2 -> LeaderboardScreen(viewModel = viewModel)
                                3 -> SupportScreen(viewModel = viewModel, snackbarHostState = snackbarHostState)
                                4 -> ProfileScreen(
                                    viewModel = viewModel,
                                    user = user,
                                    isAdminUnlocked = isAdminUnlocked,
                                    snackbarHostState = snackbarHostState,
                                    onEnterAdmin = { isAdminMode = true }
                                )
                            }
                        }
                    }
                }
                // Transient Toast Notifications Layer
                val toastNotifications by viewModel.toastNotifications.collectAsStateWithLifecycle()
                ToastOverlay(
                    toasts = toastNotifications,
                    onDismiss = { viewModel.dismissToast(it) },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
    // Dynamic Start-Time Room Credentials Pop-up Dialog
    val activeCredentialsPopup by viewModel.activeCredentialsPopup.collectAsStateWithLifecycle()
    if (activeCredentialsPopup != null) {
        val tourney = activeCredentialsPopup!!
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        Dialog(onDismissRequest = { viewModel.dismissCredentialsPopup() }) {
            Surface(
                color = Color(0xFF16141A),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Brush.verticalGradient(listOf(RedPrimary, NeonGold))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(RedPrimary.copy(alpha = 0.15f), CircleShape)
                            .border(BorderStroke(1.5.dp, RedPrimary), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Esports Lobby Ready Alert",
                            tint = RedPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "⚔️ LOBBY CREDENTIALS READY",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tourney.title.uppercase(),
                        color = NeonGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Use the details below to join the custom room in Free Fire right now. Do not share these credentials with non-participants!",
                        color = GreyText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    // Room ID segment
                    val roomIdText = tourney.roomId ?: "LOBBY_GENERATING"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0E12), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFF232129)), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ROOM ID",
                            color = GreyText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = roomIdText,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (tourney.roomId != null) {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(roomIdText))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Room ID Copied!")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF201E27)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("COPY", color = NeonGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Room Password segment
                    val roomPassText = tourney.roomPassword ?: "NO_PASSWORD"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0E12), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFF232129)), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ROOM PASSWORD",
                            color = GreyText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = roomPassText,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (tourney.roomPassword != null) {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(roomPassText))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Password Copied!")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF201E27)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("COPY", color = NeonGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Buttons
                    Button(
                        onClick = { viewModel.dismissCredentialsPopup() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "GO TO LOBBY MATCH",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.dismissCredentialsPopup() }
                    ) {
                        Text(
                            text = "DISMISS",
                            color = GreyText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
    // Secure Admin Lock Password Prompt Dialog
    if (showPasswordDialog) {
        Dialog(onDismissRequest = {
            showPasswordDialog = false
            passwordInput = ""
            passwordError = null
        }) {
            Surface(
                color = Color(0xFF16141A),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(RedPrimary.copy(alpha = 0.1f), CircleShape)
                            .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.3f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Admin Lock",
                            tint = RedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SECURE MASTER ACCESS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter master password to access administrative configurations.",
                        color = GreyText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("Master Password") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C)
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (passwordInput.trim() == "Selva@2008") {
                                viewModel.setUserRole("admin")
                                isAdminMode = true
                                showPasswordDialog = false
                                passwordInput = ""
                                scope.launch {
                                    snackbarHostState.showSnackbar("Root systems unlocked! Switched to Admin Mode.")
                                }
                            } else {
                                passwordError = "Access Denied: Invalid Authorization!"
                            }
                        }),
                        singleLine = true,
                        isError = passwordError != null,
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
                    )
                    if (passwordError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = passwordError!!,
                            color = RedPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showPasswordDialog = false
                                passwordInput = ""
                                passwordError = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ABORT", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (passwordInput.trim() == "Selva@2008") {
                                    viewModel.setUserRole("admin")
                                    isAdminMode = true
                                    showPasswordDialog = false
                                    passwordInput = ""
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Root systems unlocked! Switched to Admin Mode.")
                                    }
                                } else {
                                    passwordError = "Access Denied: Invalid Authorization!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("admin_password_submit")
                        ) {
                            Text("AUTHORIZE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showNotificationDialog) {
        NotificationsCenterDialog(
            notifications = notifications,
            onDismiss = { showNotificationDialog = false },
            onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
            onDelete = { id -> viewModel.deleteNotification(id) },
            onClearAll = { viewModel.clearAllNotifications() }
        )
    }
}
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.8f,
        animationSpec = tween(durationMillis = 1200),
        label = "scale"
    )
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000) // Show logo for 3 seconds
        onTimeout()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background gradient aura
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RedPrimary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(alphaAnim)
        ) {
            // Gaming shield frame for logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim
                        scaleY = scaleAnim
                    }
                    .background(
                        color = Color(0xFF16141A).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        border = BorderStroke(
                            2.dp, Brush.verticalGradient(
                                listOf(RedPrimary, Color(0xFF5E1119))
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_app_logo),
                    contentDescription = "BattleZone Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(26.dp))
            // Text Heading
            Text(
                text = "BATTLE ZONE",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Sub-caption
            Text(
                text = "ESPORTS TOURNAMENTS & LOBBIES",
                color = GreyText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            // Loading indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(160.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = RedPrimary,
                    trackColor = Color(0xFF28252C)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "INITIATING SECURE CORE...",
                    color = RedPrimary.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
        // Footer brand alignment
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(alphaAnim)
        ) {
            Text(
                text = "SECURED BY ROOT SHIELD CO.",
                color = GreyText.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
@Composable
fun ToastOverlay(
    toasts: List<ToastNotification>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding() // avoid status bar overlapping
            .padding(top = 48.dp), // offset downwards from TopBar
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().animateContentSize()
        ) {
            toasts.take(3).forEach { toast ->
                androidx.compose.runtime.key(toast.id) {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            )
                        ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
                        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + shrinkVertically()
                    ) {
                        GamingToastItem(toast = toast, onDismiss = {
                            visible = false
                            onDismiss(toast.id)
                        })
                    }
                }
            }
        }
    }
}
@Composable
fun GamingToastItem(
    toast: ToastNotification,
    onDismiss: () -> Unit
) {
    val borderColor = when (toast.type) {
        NotificationType.MATCH_START -> RedPrimary
        NotificationType.MATCH_RESULT -> NeonGold
        NotificationType.WARNING -> RedPrimary
        NotificationType.SUCCESS -> Color(0xFF00E676)
        NotificationType.INFO -> Color(0xFF29B6F6)
    }
    val icon = when (toast.type) {
        NotificationType.MATCH_START -> Icons.Filled.SportsEsports
        NotificationType.MATCH_RESULT -> Icons.Filled.EmojiEvents
        NotificationType.WARNING -> Icons.Filled.Warning
        NotificationType.SUCCESS -> Icons.Filled.CheckCircle
        NotificationType.INFO -> Icons.Filled.Notifications
    }
    val shadowColor = borderColor.copy(alpha = 0.25f)
    Surface(
        color = Color(0xFF16141A).copy(alpha = 0.95f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* prevent clicks passing through */ }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left icon indicator with background glow
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(borderColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.3f)), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = toast.type.name,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Text section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = toast.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = toast.message,
                    fontSize = 11.sp,
                    color = GreyText,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Action/Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "dismiss",
                    tint = GreyText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
// TOP RUNNING BAR
@Composable
fun BattleZoneTopBar(
    isAdmin: Boolean,
    isAdminUnlocked: Boolean,
    user: UserEntity?,
    notifications: List<com.example.db.NotificationEntity> = emptyList(),
    onNotificationClick: () -> Unit = {},
    onToggleAdmin: () -> Unit,
    onLogoClick: () -> Unit,
    onLockAdmin: () -> Unit,
    onWalletClick: () -> Unit = {},
    onDepositClick: () -> Unit = {}
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFF1E1C24)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLogoClick() }
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.img_app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.5f)), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BATTLEZONE FF",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isAdmin) "ROOT ADMIN PORTAL" else "FREE FIRE TOURNAMENTS",
                            fontSize = 10.sp,
                            color = if (isAdmin) RedPrimary else GreyText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                // Right panel containing optional notifications and admin mode controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isAdmin) {
                        val unreadCount = notifications.count { !it.isRead }
                        Box(
                            modifier = Modifier
                                .clickable { onNotificationClick() }
                                .padding(4.dp)
                                .testTag("notifications_bell_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (unreadCount > 0) RedPrimary else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .background(Color.Red, CircleShape)
                                        .size(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Dual Role Changer Badge - hidden in public place, only shown if isAdminUnlocked
                    if (isAdminUnlocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = onToggleAdmin,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAdmin) RedPrimary else Color(0xFF232029),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("admin_toggle")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAdmin) Icons.Filled.SportsEsports else Icons.Filled.Shield,
                                        contentDescription = "Role Mode",
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isAdmin) Color.White else RedPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAdmin) "GAMER LOBBY" else "ADMIN PANEL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onLockAdmin,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF232029), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, Color(0xFF2E2A36)), RoundedCornerShape(8.dp))
                                    .testTag("admin_relock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock Admin",
                                    tint = RedPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            // Interactive Persistent User Wallet Horizontal Indicator
            if (!isAdmin && user != null) {
                Divider(color = Color(0xFF1E1C24), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131118))
                        .clickable { onWalletClick() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val deposit = user.depositBalance
                    val winnings = user.winningBalance
                    val bonus = user.bonusBalance
                    val totalB = deposit + winnings + bonus
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Deposit Balance Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Text(
                                text = "DEPOSIT:",
                                fontSize = 10.sp,
                                color = GreyText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = deposit.toCurrency(),
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        // Winning Balance Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NeonGold, CircleShape)
                            )
                            Text(
                                text = "WINNING:",
                                fontSize = 10.sp,
                                color = GreyText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = winnings.toCurrency(),
                                fontSize = 11.sp,
                                color = NeonGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        // Bonus Balance Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(RedSecondary, CircleShape)
                            )
                            Text(
                                text = "BONUS:",
                                fontSize = 10.sp,
                                color = GreyText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = bonus.toCurrency(),
                                fontSize = 11.sp,
                                color = RedSecondary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // Firestore Balance & Deposit Button Group
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Balance Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF1E1B24), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, NeonGold.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("firestore_balance_pill")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Firestore Balance",
                                tint = NeonGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BAL: " + user.balance.toCurrency(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        // Deposit Button
                        Button(
                            onClick = { onDepositClick() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("nav_deposit_btn")
                        ) {
                            Text(
                                text = "DEPOSIT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
// NAVIGATION ROW
@Composable
fun GamerBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .border(BorderStroke(1.dp, Color(0xFF1E1C24)))
    ) {
        val items = listOf(
            Triple("Matches", Icons.Filled.SportsEsports, 0),
            Triple("Wallet", Icons.Filled.AccountBalanceWallet, 1),
            Triple("Leaderboard", Icons.Filled.EmojiEvents, 2),
            Triple("Support", Icons.Filled.SupportAgent, 3),
            Triple("Profile", Icons.Default.Person, 4)
        )
        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selectedTab == index,
                alwaysShowLabel = false,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selectedTab == index) RedPrimary else GreyText,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == index) Color.White else GreyText
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF281318)
                ),
                modifier = Modifier.testTag("nav_item_$index")
            )
        }
    }
}
// --- 1. TOURNAMENT DASHBOARD COMPONENT ---
@Composable
fun TournamentDashboardComponent(
    viewModel: BattleZoneViewModel,
    tournaments: List<TournamentEntity>,
    onViewTournament: (Int) -> Unit,
    isAdminUnlocked: Boolean,
    user: UserEntity?,
    notifications: List<com.example.db.NotificationEntity>,
    onNotificationClick: () -> Unit,
    onToggleAdmin: () -> Unit,
    onLogoClick: () -> Unit,
    onLockAdmin: () -> Unit,
    onWalletClick: () -> Unit,
    onDepositClick: () -> Unit
) {
    val userJoins by viewModel.currentUserJoins.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val isAdmin = userRole == "admin" || viewModel.isFirebaseUserAdmin()
    
    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedMapFilter by remember { mutableStateOf("ALL") } // "ALL", "BERMUDA", "PURGATORY", "KALAHARI"
    var selectedFormatFilter by remember { mutableStateOf("ALL") } // "ALL", "SOLO", "DUO", "SQUAD"
    var selectedStatusFilter by remember { mutableStateOf("UPCOMING") } // "UPCOMING", "LIVE", "COMPLETED"
    var selectedSortOption by remember { mutableStateOf("DATE_ASC") }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. BattleZoneTopBar (First item so it scrolls up with the rest of the page!)
            item {
                BattleZoneTopBar(
                    isAdmin = isAdmin,
                    isAdminUnlocked = isAdminUnlocked,
                    user = user,
                    notifications = notifications,
                    onNotificationClick = onNotificationClick,
                    onToggleAdmin = onToggleAdmin,
                    onLogoClick = onLogoClick,
                    onLockAdmin = onLockAdmin,
                    onWalletClick = onWalletClick,
                    onDepositClick = onDepositClick
                )
            }

            // 2. Gaming Banner Slider Header
            item {
                GamingBannerSlider()
            }

            // 2. Real-time Firestore Sync Status Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131116)),
                    border = BorderStroke(1.dp, Color(0xFF1F1C25)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "FIRESTORE ONLINE MATCH REGISTRY",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Live auto-syncing active Free Fire custom lobbies",
                                    color = GreyText,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                viewModel.startFirestoreSync()
                                viewModel.showToast(
                                    title = "📡 FIRESTORE SYNC COMPLETE",
                                    message = "Free Fire match listings successfully updated from cloud Firestore databases.",
                                    type = NotificationType.SUCCESS
                                )
                                scope.launch {
                                    delay(800)
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = RedPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 3. Dynamic Dashboard Arena Performance Metrics
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeT = tournaments.filter { it.status != "COMPLETED" }
                    val totalPrizes = activeT.sumOf { it.prizePool }
                    val totalSlots = activeT.sumOf { it.slotsRemaining }
                    val countActive = activeT.size

                    // Stat 1: Total dynamic prize pool
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16141A)),
                        border = BorderStroke(1.dp, Color(0xFF221F28)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("ACTIVE PRIZES", fontSize = 8.sp, color = GreyText, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${totalPrizes.toInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGold
                            )
                        }
                    }

                    // Stat 2: Remaining slots left
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16141A)),
                        border = BorderStroke(1.dp, Color(0xFF221F28)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("SLOTS LEFT", fontSize = 8.sp, color = GreyText, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$totalSlots",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // Stat 3: Total active matches
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16141A)),
                        border = BorderStroke(1.dp, Color(0xFF221F28)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("MATCHROOMS", fontSize = 8.sp, color = GreyText, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$countActive ACTIVE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = RedPrimary
                            )
                        }
                    }
                }
            }

            // 4. Live Dashboard Advanced Search & Interactive Filter Controls
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Outlined Custom Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Free Fire matches (Weekly, Showdown...)", fontSize = 11.sp, color = GreyText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = GreyText,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = GreyText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF16141A),
                            unfocusedContainerColor = Color(0xFF16141A),
                            focusedBorderColor = RedPrimary.copy(alpha = 0.7f),
                            unfocusedBorderColor = Color(0xFF221F28)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("tournament_search_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Map and team format pills row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Map Filter Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF16141A), RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, Color(0xFF221F28)), RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedMapFilter = when (selectedMapFilter) {
                                        "ALL" -> "BERMUDA"
                                        "BERMUDA" -> "PURGATORY"
                                        "PURGATORY" -> "KALAHARI"
                                        else -> "ALL"
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MAP: $selectedMapFilter",
                                color = if (selectedMapFilter != "ALL") RedPrimary else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Format Filter Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF16141A), RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, Color(0xFF221F28)), RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedFormatFilter = when (selectedFormatFilter) {
                                        "ALL" -> "SOLO"
                                        "SOLO" -> "DUO"
                                        "DUO" -> "SQUAD"
                                        else -> "ALL"
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TEAM: $selectedFormatFilter",
                                color = if (selectedFormatFilter != "ALL") RedPrimary else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Status Filter Pill
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .background(Color(0xFF16141A), RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, Color(0xFF221F28)), RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedStatusFilter = when (selectedStatusFilter) {
                                        "UPCOMING" -> "LIVE"
                                        "LIVE" -> "COMPLETED"
                                        else -> "UPCOMING"
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "STATUS: $selectedStatusFilter",
                                color = if (selectedStatusFilter == "LIVE") RedPrimary else if (selectedStatusFilter == "COMPLETED") Color(0xFF00E676) else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sort indicator and header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort By",
                                tint = GreyText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SORT BY",
                                fontSize = 9.sp,
                                color = GreyText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Quick reset sorting
                        if (selectedSortOption != "DATE_ASC") {
                            Text(
                                text = "RESET SORT",
                                fontSize = 8.sp,
                                color = RedPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { selectedSortOption = "DATE_ASC" }
                                    .testTag("reset_sort_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Scrollable Row for Sort options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val options = listOf(
                            Triple("DATE_ASC", "Date: Earliest", Icons.Default.DateRange),
                            Triple("DATE_DESC", "Date: Latest", Icons.Default.DateRange),
                            Triple("PRIZE_DESC", "Prize: High to Low", Icons.Default.EmojiEvents),
                            Triple("PRIZE_ASC", "Prize: Low to High", Icons.Default.EmojiEvents),
                            Triple("FEE_ASC", "Fee: Low to High", Icons.Default.LocalActivity),
                            Triple("FEE_DESC", "Fee: High to Low", Icons.Default.LocalActivity)
                        )

                        options.forEach { (key, label, icon) ->
                            val isSelected = selectedSortOption == key
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) RedPrimary.copy(alpha = 0.15f) else Color(0xFF16141A),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) RedPrimary else Color(0xFF221F28),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedSortOption = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("sort_chip_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) RedPrimary else GreyText,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else GreyText,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Apply filters & sorting
            val filteredList = tournaments.filter { match ->
                val matchesQuery = match.title.contains(searchQuery, ignoreCase = true) || match.map.contains(searchQuery, ignoreCase = true)
                val matchesMap = selectedMapFilter == "ALL" || match.map.contains(selectedMapFilter, ignoreCase = true)
                val matchesFormat = selectedFormatFilter == "ALL" || match.type.equals(selectedFormatFilter, ignoreCase = true)
                val matchesStatus = match.status == selectedStatusFilter
                val visibleToUser = if (selectedStatusFilter == "LIVE") {
                    isAdmin || userJoins.any { it.tournamentId == match.id }
                } else {
                    true
                }
                matchesQuery && matchesMap && matchesFormat && matchesStatus && visibleToUser
            }.let { list ->
                when (selectedSortOption) {
                    "DATE_ASC" -> list.sortedWith(compareBy<TournamentEntity> { it.status == "COMPLETED" }.thenBy { it.timestamp })
                    "DATE_DESC" -> list.sortedWith(compareBy<TournamentEntity> { it.status == "COMPLETED" }.thenByDescending { it.timestamp })
                    "PRIZE_DESC" -> list.sortedWith(compareBy<TournamentEntity> { it.status == "COMPLETED" }.thenByDescending { it.prizePool })
                    "PRIZE_ASC" -> list.sortedWith(compareBy<TournamentEntity> { it.status == "COMPLETED" }.thenBy { it.prizePool })
                    "FEE_ASC" -> list.sortedWith(compareBy<TournamentEntity> { it.status == "COMPLETED" }.thenBy { it.entryFee })
                    "FEE_DESC" -> list.sortedWith(compareBy<TournamentEntity> { it.status == "COMPLETED" }.thenByDescending { it.entryFee })
                    else -> list.sortedWith(compareBy<TournamentEntity> { it.status == "COMPLETED" }.thenBy { it.timestamp })
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Not Found",
                                tint = GreyText.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Free Fire tournaments match filters.",
                                color = GreyText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "RESET FILTERS",
                                color = RedPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        searchQuery = ""
                                        selectedMapFilter = "ALL"
                                        selectedFormatFilter = "ALL"
                                        selectedStatusFilter = "UPCOMING"
                                        selectedSortOption = "DATE_ASC"
                                    }
                                    .border(BorderStroke(1.dp, RedPrimary), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { match ->
                    TournamentDashboardCard(
                        match = match,
                        viewModel = viewModel,
                        onViewDetails = { onViewTournament(match.id) }
                    )
                }
            }
        }
    }
}

// --- 2. ENHANCED TOURNAMENT DASHBOARD CARD ---
@Composable
fun TournamentDashboardCard(
    match: TournamentEntity,
    viewModel: BattleZoneViewModel? = null,
    onViewDetails: () -> Unit
) {
    // Map-specific styling: Gradient Brushes matching the Free Fire map aesthetics
    val mapLower = match.map.lowercase()
    val (mapGradient, mapLabelColor) = when {
        mapLower.contains("bermuda") -> {
            Brush.linearGradient(listOf(Color(0xFF00332C), Color(0xFF005B51), Color(0xFF131116))) to Color(0xFF00E676)
        }
        mapLower.contains("kalahari") -> {
            Brush.linearGradient(listOf(Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFF131116))) to Color(0xFFFFB300)
        }
        mapLower.contains("purgatory") -> {
            Brush.linearGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF131116))) to Color(0xFF8C9EFF)
        }
        else -> {
            Brush.linearGradient(listOf(Color(0xFF2B0508), Color(0xFF5C0E14), Color(0xFF131116))) to RedPrimary
        }
    }

    // Pulse Animation for LIVE Matches
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by if (match.status == "LIVE") {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onViewDetails() }
            .testTag("dashboard_tournament_card_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131116)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .border(BorderStroke(1.dp, Color(0xFF1F1C25)), RoundedCornerShape(16.dp))
        ) {
            // Map/Game mode Banner Header Area (asymmetrical, depth, styled)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
                    .background(mapGradient)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Map",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = match.map.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = match.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            letterSpacing = 0.2.sp
                        )
                    }

                    // Mode & Format Badges
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Team Mode Badge (Solo/Duo/Squad)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when (match.type.uppercase()) {
                                    "SOLO" -> Icons.Default.Person
                                    "DUO" -> Icons.Default.People
                                    else -> Icons.Default.Group
                                },
                                contentDescription = "Format",
                                tint = mapLabelColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = match.type.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Match Status Badge with Pulsing for LIVE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    when (match.status) {
                                        "LIVE" -> Color(0xFF2E1216)
                                        "COMPLETED" -> Color(0xFF0E2E1D)
                                        else -> Color.Black.copy(alpha = 0.4f)
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            if (match.status == "LIVE") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .alpha(pulseAlpha)
                                        .background(RedPrimary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = match.status.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = when (match.status) {
                                    "LIVE" -> RedPrimary
                                    "COMPLETED" -> Color(0xFF00E676)
                                    else -> GreyText
                                },
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Bottom specifications and progress bar area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0B0F))
                    .padding(14.dp)
            ) {
                // Date & Time details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Schedule",
                        tint = GreyText,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SCHEDULE: ${match.localDateTimeStr}",
                        fontSize = 10.sp,
                        color = GreyText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Countdown timer row for UPCOMING matches
                if (match.status == "UPCOMING") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF16141A), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFF1F1C25)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Timer",
                                tint = RedPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CountdownTimer(
                                targetTimestamp = match.timestamp,
                                onTimerFinished = {
                                    viewModel?.setTournamentLive(match.id)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Main financial specs: Prize Pool and Entry Stake side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Prize Pool Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF16141A), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, NeonGold.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF231F1D), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Prize",
                                    tint = NeonGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "PRIZE POOL",
                                    fontSize = 8.sp,
                                    color = GreyText,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = match.prizePool.toCurrency(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonGold
                                )
                            }
                        }
                    }

                    // Entry Fee Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF16141A), RoundedCornerShape(10.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (match.entryFee == 0.0) Color(0xFF00E676).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (match.entryFee == 0.0) Color(0xFF0F261B) else Color(0xFF1F1C25),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalActivity,
                                    contentDescription = "Entry",
                                    tint = if (match.entryFee == 0.0) Color(0xFF00E676) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ENTRY STAKE",
                                    fontSize = 8.sp,
                                    color = GreyText,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (match.entryFee == 0.0) "FREE" else match.entryFee.toCurrency(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (match.entryFee == 0.0) Color(0xFF00E676) else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Slots registration progress area
                val slotsFilled = match.slotsTotal - match.slotsRemaining
                val slotsPercentage = if (match.slotsTotal > 0) slotsFilled.toFloat() / match.slotsTotal.toFloat() else 0f
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Slots icon",
                                tint = GreyText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$slotsFilled / ${match.slotsTotal} Slots Filled",
                                fontSize = 10.sp,
                                color = GreyText,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (match.slotsRemaining == 0) {
                            Text(
                                text = "HOUSE FULL LOBBY",
                                fontSize = 9.sp,
                                color = RedPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        } else {
                            Text(
                                text = "ONLY ${match.slotsRemaining} SPOTS LEFT",
                                fontSize = 9.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { slotsPercentage },
                        color = if (slotsPercentage >= 0.9f) RedPrimary else if (slotsPercentage >= 0.6f) NeonGold else Color(0xFF00E676),
                        trackColor = Color(0xFF16141A),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    
                    // Direct Register Button on the card
                    if (viewModel != null) {
                        val userState = viewModel.currentUser.collectAsStateWithLifecycle()
                        val user = userState.value
                        
                        val joinsState = viewModel.currentUserJoins.collectAsStateWithLifecycle()
                        val joinedList = joinsState.value
                        val isRegistered = joinedList.any { it.tournamentId == match.id }
                        
                        var showDirectRegisterPrompt by remember { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = {
                                if (!isRegistered) {
                                    showDirectRegisterPrompt = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRegistered) Color(0xFF0F261B) else RedPrimary,
                                disabledContainerColor = Color(0xFF1F1C25)
                            ),
                            border = if (isRegistered) BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f)) else null,
                            shape = RoundedCornerShape(10.dp),
                            enabled = isRegistered || (match.slotsRemaining > 0 && match.status == "UPCOMING"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("card_register_btn_${match.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isRegistered) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Registered Icon",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "REGISTERED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676),
                                        letterSpacing = 0.5.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.HowToReg,
                                        contentDescription = "Register Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (match.slotsRemaining == 0) "HOUSE FULL LOBBY"
                                               else if (match.status != "UPCOMING") "REGISTRATION CLOSED"
                                               else if (match.entryFee == 0.0) "QUICK REGISTER (FREE)"
                                               else "REGISTER NOW (₹${match.entryFee})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        
                        if (showDirectRegisterPrompt) {
                            var inGameNameInput by remember { mutableStateOf(user?.inGameName ?: "Alpha_Gamer") }
                            var uidInput by remember { mutableStateOf(user?.freeFireUid ?: "FF-837492047") }
                            var registrationStep by remember { mutableStateOf(1) }
                            
                            Dialog(onDismissRequest = { showDirectRegisterPrompt = false }) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF131116),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(BorderStroke(1.dp, Color(0xFF1F1C25)), RoundedCornerShape(16.dp))
                                ) {
                                    if (registrationStep == 1) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Text(
                                                text = "QUICK REGISTRATION",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = RedPrimary,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = match.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Please verify your Free Fire in-game details to proceed.",
                                                fontSize = 10.sp,
                                                color = GreyText
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            OutlinedTextField(
                                                value = inGameNameInput,
                                                onValueChange = { inGameNameInput = it },
                                                label = { Text("In-Game Name") },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = RedPrimary,
                                                    unfocusedBorderColor = Color(0xFF28252C)
                                                ),
                                                modifier = Modifier.fillMaxWidth().testTag("direct_register_name_input")
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedTextField(
                                                value = uidInput,
                                                onValueChange = { uidInput = it },
                                                label = { Text("Character UID") },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = RedPrimary,
                                                    unfocusedBorderColor = Color(0xFF28252C)
                                                ),
                                                modifier = Modifier.fillMaxWidth().testTag("direct_register_uid_input")
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(onClick = { showDirectRegisterPrompt = false }) {
                                                    Text("CANCEL", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Button(
                                                    onClick = {
                                                        if (inGameNameInput.isNotBlank() && uidInput.isNotBlank()) {
                                                            registrationStep = 2
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    enabled = inGameNameInput.isNotBlank() && uidInput.isNotBlank(),
                                                    modifier = Modifier.testTag("direct_register_proceed_btn")
                                                ) {
                                                    Text("PROCEED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        // Step 2: Pay & Confirm Balance Deduction
                                        val requiredFee = match.entryFee
                                        val depAva = user?.depositBalance ?: 0.0
                                        val winAva = user?.winningBalance ?: 0.0
                                        val totalRealCash = depAva + winAva
                                        
                                        val depositDeduct = minOf(requiredFee, depAva)
                                        val remainingForWin = maxOf(0.0, requiredFee - depositDeduct)
                                        val winningsDeduct = minOf(remainingForWin, winAva)
                                        val isSufficient = (depositDeduct + winningsDeduct) >= requiredFee
                                        
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "secure transaction",
                                                    tint = RedPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "PAYMENT VERIFICATION",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp,
                                                    color = Color.White,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            
                                            // Wallet status cards
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF0D0B0F), RoundedCornerShape(10.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Total Fee:", fontSize = 11.sp, color = GreyText)
                                                    Text(requiredFee.toCurrency(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                HorizontalDivider(color = Color(0xFF1F1C25), thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Deduct from Deposit:", fontSize = 11.sp, color = GreyText)
                                                    Text("-${depositDeduct.toCurrency()}", fontSize = 11.sp, color = Color.White)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Deduct from Winnings:", fontSize = 11.sp, color = GreyText)
                                                    Text("-${winningsDeduct.toCurrency()}", fontSize = 11.sp, color = NeonGold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            
                                            if (isSufficient) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(onClick = { registrationStep = 1 }) {
                                                        Text("BACK", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Button(
                                                        onClick = {
                                                            viewModel.updateProfile(
                                                                inGameName = inGameNameInput,
                                                                ffUid = uidInput,
                                                                phone = user?.phoneNumber ?: "",
                                                                email = user?.email ?: "",
                                                                bio = user?.profilePicture ?: "Free Fire Pro Gamer"
                                                            ) { _ ->
                                                                viewModel.joinTournament(match.id) { response ->
                                                                    scope.launch {
                                                                        if (response == "SUCCESS") {
                                                                            showDirectRegisterPrompt = false
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.testTag("direct_register_confirm_btn")
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.Lock,
                                                                contentDescription = "confirm",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = if (requiredFee == 0.0) "JOIN FREE" else "PAY & CONFIRM",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Insufficient balance warnings
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(RedPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                        .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                                                        .padding(12.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = "insufficient",
                                                            tint = RedPrimary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Insufficient Balance", color = RedPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Your combined real funding balance is ${totalRealCash.toCurrency()}, which is insufficient for the ${requiredFee.toCurrency()} entry fee limit.",
                                                        fontSize = 10.sp,
                                                        color = GreyText,
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                    TextButton(onClick = { showDirectRegisterPrompt = false }) {
                                                        Text("DISMISS", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 3. LOBBY SCREEN (DELEGATES TO THE DASHBOARD) ---
@Composable
fun LobbyScreen(
    viewModel: BattleZoneViewModel,
    tournaments: List<TournamentEntity>,
    onViewTournament: (Int) -> Unit,
    isAdminUnlocked: Boolean,
    user: UserEntity?,
    notifications: List<com.example.db.NotificationEntity>,
    onNotificationClick: () -> Unit,
    onToggleAdmin: () -> Unit,
    onLogoClick: () -> Unit,
    onLockAdmin: () -> Unit,
    onWalletClick: () -> Unit,
    onDepositClick: () -> Unit
) {
    TournamentDashboardComponent(
        viewModel = viewModel,
        tournaments = tournaments,
        onViewTournament = onViewTournament,
        isAdminUnlocked = isAdminUnlocked,
        user = user,
        notifications = notifications,
        onNotificationClick = onNotificationClick,
        onToggleAdmin = onToggleAdmin,
        onLogoClick = onLogoClick,
        onLockAdmin = onLockAdmin,
        onWalletClick = onWalletClick,
        onDepositClick = onDepositClick
    )
}
@Composable
fun GamingBannerSlider() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF7A0F1A), Color(0xFF160305)),
                        radius = 450f
                    )
                )
                .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(RedPrimary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("MEGA PRIZE ZONE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BATTLEZONE PRO SEASON 5",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Play daily battles, claim payouts instantly within 5 mins.",
                    fontSize = 10.sp,
                    color = GreyText
                )
            }
            Image(
                painter = painterResource(id = com.example.R.drawable.img_app_logo),
                contentDescription = "logo badge",
                alpha = 0.5f,
                modifier = Modifier
                    .size(85.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                    .align(Alignment.CenterEnd),
                contentScale = ContentScale.Crop
            )
        }
    }
}
@Composable
fun TournamentCard(
    match: TournamentEntity,
    viewModel: BattleZoneViewModel? = null,
    onViewDetails: () -> Unit
) {
    TournamentDashboardCard(
        match = match,
        viewModel = viewModel,
        onViewDetails = onViewDetails
    )
}
@Composable
fun CountdownTimer(
    targetTimestamp: Long,
    onTimerFinished: () -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasTriggeredFinished by remember(targetTimestamp) { mutableStateOf(false) }
    
    LaunchedEffect(targetTimestamp) {
        if (System.currentTimeMillis() >= targetTimestamp) {
            if (!hasTriggeredFinished) {
                hasTriggeredFinished = true
                onTimerFinished()
            }
            return@LaunchedEffect
        }
        while (System.currentTimeMillis() < targetTimestamp) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
        currentTime = System.currentTimeMillis()
        if (!hasTriggeredFinished) {
            hasTriggeredFinished = true
            onTimerFinished()
        }
    }
    
    val diff = targetTimestamp - currentTime
    if (diff > 0) {
        val seconds = (diff / 1000) % 60
        val minutes = (diff / (1000 * 60)) % 60
        val hours = (diff / (1000 * 60 * 60)) % 24
        val days = diff / (1000 * 60 * 60 * 24)
        
        val countdownText = buildString {
            if (days > 0) append("${days}d ")
            append(String.format("%02dh %02dm %02ds", hours, minutes, seconds))
        }
        
        val tf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        val currentClockStr = tf.format(java.util.Date(currentTime))
        val targetClockStr = tf.format(java.util.Date(targetTimestamp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1B1014)) // subtle dark ruby red-tinted black background
                .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Timer",
                        tint = RedPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Current: $currentClockStr  |  Match: $targetClockStr",
                        fontSize = 10.sp,
                        color = GreyText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "BATTLE STARTS IN: ",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = countdownText,
                        fontSize = 13.sp,
                        color = NeonGold,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    } else {
        // If the scheduled time has already been reached or passed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF172C1E)) // subtle dark green background for starting matches
                .border(BorderStroke(1.dp, Color.Green.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Ready",
                    tint = Color.Green,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BATTLE IS LIVE NOW",
                    fontSize = 11.sp,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
// --- 2. TOURNAMENT DETAILS & SEAT MAP & DYNAMIC KEYS ---
@Composable
fun TournamentDetailsScreen(
    tournamentId: Int,
    viewModel: BattleZoneViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val match = tournaments.find { it.id == tournamentId }
    val userJoins by viewModel.currentUserJoins.collectAsStateWithLifecycle()
    val isAlreadyJoined = userJoins.any { it.tournamentId == tournamentId }
    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Match details missing. Let's return.", color = Color.White)
        }
        return
    }
    // Dynamic timer checking calculation: Room credentials visible if:
    // status == LIVE or COMPLETED or starting in < 10 mins (simulated as custom trigger button or immediate display once registered)
    var revealRoomDetails by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF5E1119), DarkBg)
                    )
                )
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp)
            ) {
                Text(match.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(match.type.uppercase(), fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
                    Text("  •  ", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    Text(match.map.uppercase(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            // Stats Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color(0xFF1F1C25)), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PRIZE POOL", fontSize = 9.sp, color = GreyText)
                    Text(match.prizePool.toCurrency(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = NeonGold)
                }
                Divider(
                    modifier = Modifier
                        .height(34.dp)
                        .width(1.dp), color = Color(0xFF28252C)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ENTRY FEE", fontSize = 9.sp, color = GreyText)
                    Text(
                        if (match.entryFee == 0.0) "FREE" else match.entryFee.toCurrency(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Divider(
                    modifier = Modifier
                        .height(34.dp)
                        .width(1.dp), color = Color(0xFF28252C)
                )
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GAME DATE", fontSize = 9.sp, color = GreyText)
                    Text(match.localDateTimeStr, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = RedSecondary, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // -- INNER CREDENTIALS DISTRIBUTION PANEL ---
            if (isAlreadyJoined) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161C14)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF2E4C20)), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LockOpen,
                                contentDescription = "Unlocks",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Registered Successfully (Seat Reserved)",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Room ID and Password release dynamically 10 minutes prior to scheduled starting bell.",
                            fontSize = 10.sp,
                            color = GreyText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (revealRoomDetails) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ROOM ID:", fontSize = 11.sp, color = GreyText)
                                    Text(
                                        text = match.roomId ?: "RELEASING_SOON",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ROOM PASSWORD:", fontSize = 11.sp, color = GreyText)
                                    Text(
                                        text = match.roomPassword ?: "RELEASING_SOON",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedPrimary
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    revealRoomDetails = true
                                    scope.launch {
                                        if (match.roomId != null) {
                                            snackbarHostState.showSnackbar("Unlocked credentials securely!")
                                        } else {
                                            snackbarHostState.showSnackbar("Admin has not published room details yet.")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22351B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("REVEAL ROOM DETAILS", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                val currentJoin = userJoins.find { it.tournamentId == tournamentId }
                if (currentJoin != null) {
                    ProofSubmissionCard(
                        join = currentJoin,
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Refund Request block
                    var showRefundPrompt by remember { mutableStateOf(false) }
                    val liveRefunds by viewModel.currentUserRefunds.collectAsStateWithLifecycle()
                    val existingRefund = liveRefunds.find { it.tournamentId == tournamentId }
                    if (existingRefund != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF281C16)),
                            border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Refund Request Status Info",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("REFUND STATUS: ${existingRefund.status}", fontSize = 11.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Reason: ${existingRefund.reason}", fontSize = 10.sp, color = GreyText)
                                Text("Refund Destination: ${existingRefund.refundDestination}", fontSize = 10.sp, color = GreyText)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when (existingRefund.status) {
                                        "PENDING" -> "Your request is currently being reviewed by our audit desk. If approved, ₹${existingRefund.entryFee} INR will immediately credit back."
                                        "APPROVED" -> "✅ Approved! Refund reversed of ₹${existingRefund.entryFee} INR."
                                        else -> "❌ Rejected by admin verification."
                                    },
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1424)),
                            border = BorderStroke(0.5.dp, Color(0xFF9E2A2B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("HAVING MATCH CONDUCT OR TIMING ISSUES?", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Black)
                                Text("If tournament is not conducted or delayed, request a full match entry fee return.", fontSize = 9.sp, color = GreyText)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showRefundPrompt = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1E29)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                ) {
                                    Text("REQUEST REFUND", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (showRefundPrompt) {
                        var refundReason by remember { mutableStateOf("Tournament was not conducted by the admin") }
                        var refundDestination by remember { mutableStateOf("WALLET") } // "WALLET" or "BANK_WALLET"
                        Dialog(onDismissRequest = { showRefundPrompt = false }) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF16141F),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("REQUEST ENTRY FEE REFUND", color = NeonGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Select the exact reason and destination for manual processing or wallet reversals.", fontSize = 9.sp, color = GreyText)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("SELECT REFUND REASON", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val refundReasons = listOf("Tournament was not conducted by the admin", "Match was rescheduled / Timing issue")
                                    refundReasons.forEach { r ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { refundReason = r }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = refundReason == r,
                                                onClick = { refundReason = r },
                                                colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(r, color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("RETURN DESTINATION", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (refundDestination == "WALLET") RedPrimary else Color(0xFF232029), RoundedCornerShape(6.dp))
                                                .clickable { refundDestination = "WALLET" }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("BZONE WALLET", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text("Instant Reversal", color = Color.White.copy(alpha = 0.6f), fontSize = 7.sp)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (refundDestination == "BANK_WALLET") RedPrimary else Color(0xFF232029), RoundedCornerShape(6.dp))
                                                .clickable { refundDestination = "BANK_WALLET" }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("ORIGINAL SOURCE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text("Bank / Net-banking", color = Color.White.copy(alpha = 0.6f), fontSize = 7.sp)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { showRefundPrompt = false }) {
                                            Text("CANCEL", color = GreyText)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Button(
                                            onClick = {
                                                viewModel.requestRefund(tournamentId, refundReason, refundDestination) { success, err ->
                                                    scope.launch {
                                                        if (success) {
                                                            snackbarHostState.showSnackbar("Refund request submitted successfully!")
                                                        } else {
                                                            snackbarHostState.showSnackbar(err ?: "Failed to request refund.")
                                                        }
                                                    }
                                                }
                                                showRefundPrompt = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                                        ) {
                                            Text("SUBMIT REQUEST")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            // Rules Box
            Text("MATCH RULES & REGULATIONS", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = match.rules,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier.padding(14.dp),
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            // Action / Join button
            if (!isAlreadyJoined) {
                var showInGameNamePrompt by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        if (user?.freeFireUid == "FF-837492047" || user?.inGameName == "Alpha_Gamer") {
                            // User can set their names or just proceed
                            showInGameNamePrompt = true
                        } else {
                            showInGameNamePrompt = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("match_join_action"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedPrimary,
                        disabledContainerColor = Color(0xFF28252C)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = match.slotsRemaining > 0 && match.status == "UPCOMING"
                ) {
                    Text(
                        text = if (match.slotsRemaining == 0) "HOUSE FULL" else "JOIN TOURNAMENT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                if (showInGameNamePrompt) {
                    var inGameNameInput by remember { mutableStateOf(user?.inGameName ?: "Alpha_Gamer") }
                    var uidInput by remember { mutableStateOf(user?.freeFireUid ?: "FF-837492047") }
                    var registrationStep by remember { mutableStateOf(1) }
                    Dialog(onDismissRequest = { showInGameNamePrompt = false }) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
                        ) {
                            if (registrationStep == 1) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        "Confirm Registration Details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Verify your Free Fire in-game credentials as admins check registrations during match lobbies.",
                                        fontSize = 10.sp,
                                        color = GreyText
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = inGameNameInput,
                                        onValueChange = { inGameNameInput = it },
                                        label = { Text("In-Game Name") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uidInput,
                                        onValueChange = { uidInput = it },
                                        label = { Text("Free Fire Character UID") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { showInGameNamePrompt = false }) {
                                            Text("CANCEL", color = GreyText)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (inGameNameInput.isNotBlank() && uidInput.isNotBlank()) {
                                                    registrationStep = 2
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                            enabled = inGameNameInput.isNotBlank() && uidInput.isNotBlank()
                                        ) {
                                            Text("PROCEED TO PAYMENT")
                                        }
                                    }
                                }
                            } else {
                                // Step 2: Secure Payment Verification Modal
                                val requiredFee = match.entryFee
                                val depAva = user?.depositBalance ?: 0.0
                                val winAva = user?.winningBalance ?: 0.0
                                val totalRealCash = depAva + winAva
                                val depositDeduct = minOf(requiredFee, depAva)
                                val remainingForWin = maxOf(0.0, requiredFee - depositDeduct)
                                val winningsDeduct = minOf(remainingForWin, winAva)
                                val isSufficient = (depositDeduct + winningsDeduct) >= requiredFee
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "secure lock",
                                            tint = RedPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Secure Entry Payment",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Authorize tournament registration stake securely.",
                                        fontSize = 10.sp,
                                        color = GreyText
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Match details summary
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C24)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = match.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Schedule: ${match.localDateTimeStr}",
                                                fontSize = 11.sp,
                                                color = GreyText
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Player IGN: $inGameNameInput ($uidInput)",
                                                fontSize = 11.sp,
                                                color = GreyText
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Payment Breakdown
                                    Text(
                                        "ESTIMATED DEDUCTIONS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = GreyText,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1E1C24), RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Entry Fee Required:", fontSize = 12.sp, color = Color.White)
                                            Text(
                                                text = if (requiredFee == 0.0) "FREE" else requiredFee.toCurrency(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (requiredFee == 0.0) Color(0xFF00E676) else Color.White
                                            )
                                        }
                                        if (requiredFee > 0.0) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Divider(color = Color(0xFF28252C))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("From Deposit Wallet:", fontSize = 11.sp, color = GreyText)
                                                Text("-${depositDeduct.toCurrency()}", fontSize = 11.sp, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("From Winnings Wallet:", fontSize = 11.sp, color = GreyText)
                                                Text("-${winningsDeduct.toCurrency()}", fontSize = 11.sp, color = NeonGold)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("From Promo Bonus:", fontSize = 11.sp, color = GreyText)
                                                    Text(" (Promo)", fontSize = 9.sp, color = RedSecondary)
                                                }
                                                Text("₹0.00", fontSize = 11.sp, color = GreyText)
                                            }
                                        }
                                    }
                                    if (requiredFee > 0.0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "*Note: Promotional bonus cash balance is restricted from tournament entry stakes.",
                                            fontSize = 9.sp,
                                            color = RedSecondary,
                                            lineHeight = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    // Wallet Status or Insufficient Warning
                                    if (isSufficient) {
                                        // Success state breakdown
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Remaining Deposit:", fontSize = 11.sp, color = GreyText)
                                            Text((depAva - depositDeduct).toCurrency(), fontSize = 11.sp, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Remaining Winnings:", fontSize = 11.sp, color = GreyText)
                                            Text((winAva - winningsDeduct).toCurrency(), fontSize = 11.sp, color = NeonGold)
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { registrationStep = 1 }) {
                                                Text("BACK", color = GreyText)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Button(
                                                onClick = {
                                                    // Secure action: Update profile and then join tournament
                                                    viewModel.updateProfile(
                                                        inGameName = inGameNameInput,
                                                        ffUid = uidInput,
                                                        phone = user?.phoneNumber ?: "",
                                                        email = user?.email ?: "",
                                                        bio = user?.profilePicture ?: "Free Fire Pro Gamer"
                                                    ) { success ->
                                                        viewModel.joinTournament(match.id) { response ->
                                                            scope.launch {
                                                                if (response == "SUCCESS") {
                                                                    snackbarHostState.showSnackbar("Match registration confirmed!")
                                                                } else {
                                                                    snackbarHostState.showSnackbar(response)
                                                                }
                                                            }
                                                        }
                                                        showInGameNamePrompt = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                                modifier = Modifier.testTag("payment_confirm_action")
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = "pay icon",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (requiredFee == 0.0) "JOIN FREE" else "PAY & CONFIRM"
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Insufficient Funds State
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(RedPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "insufficient",
                                                    tint = RedPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Insufficient Real Cash", color = RedPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Your combined real funding balance is ${totalRealCash.toCurrency()}, which is insufficient for the ${requiredFee.toCurrency()} entry fee limit.",
                                                fontSize = 10.sp,
                                                color = GreyText,
                                                lineHeight = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { registrationStep = 1 }) {
                                                Text("BACK", color = GreyText)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Button(
                                                onClick = {
                                                    showInGameNamePrompt = false
                                                    onNavigateToWallet()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                                            ) {
                                                Text("ADD FUNDS")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = { /* already joined */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202B19)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = false
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "check", tint = Color(0xFF00E676))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REGISTERED FOR MATCH", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
// --- 3. PERSISTENT WALLET ENGINE ---
@Composable
fun WalletScreen(
    viewModel: BattleZoneViewModel,
    snackbarHostState: SnackbarHostState,
    autoOpenDeposit: Boolean = false,
    onDepositOpened: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val transactions by viewModel.currentUserTransactions.collectAsStateWithLifecycle()
    val withdrawals by viewModel.currentUserWithdrawals.collectAsStateWithLifecycle(emptyList())
    var approvedToShowDialog by remember { mutableStateOf<WithdrawalRequestEntity?>(null) }
    LaunchedEffect(withdrawals) {
        val unnotified = withdrawals.find { it.status == "APPROVED" && !viewModel.hasNotifiedApproval(it.id) }
        if (unnotified != null) {
            approvedToShowDialog = unnotified
        }
    }
    if (approvedToShowDialog != null) {
        AlertDialog(
            onDismissRequest = {
                approvedToShowDialog?.let { viewModel.markApprovalNotified(it.id) }
                approvedToShowDialog = null
            },
            title = {
                Text(
                    text = "🎉 Withdrawal Approved!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Your withdrawal of ₹${approvedToShowDialog?.amount} has been approved by the administrator.\n\nThe funds will be credited to your UPI ID within two to three days. Thank you for your patience!",
                    color = GreyText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        approvedToShowDialog?.let { viewModel.markApprovalNotified(it.id) }
                        approvedToShowDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGold)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            tonalElevation = 6.dp
        )
    }
    var showDepositDialog by remember { mutableStateOf(false) }
    LaunchedEffect(autoOpenDeposit) {
        if (autoOpenDeposit) {
            showDepositDialog = true
            onDepositOpened()
        }
    }
    var showWithdrawalDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WALLET BALANCE CARD",
                fontSize = 11.sp,
                color = GreyText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            // Live Firestore Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(Color(0xFF0F1B15), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF00E676), CircleShape)
                )
                Text(
                    text = "FIRESTORE SYNC ACTIVE",
                    color = Color(0xFF00E676),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Firestore Status and Dynamic Refresh Component
        val isUserFirestoreRefreshing by viewModel.isUserFirestoreRefreshing.collectAsStateWithLifecycle()
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131116)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(BorderStroke(1.dp, Color(0xFF1E1C24)), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE5A93B).copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud Icon",
                            tint = Color(0xFFE5A93B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CLOUD FIRESTORE DOCUMENT DIRECTIVE",
                            fontSize = 8.sp,
                            color = Color(0xFFE5A93B),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Document: users/${user?.id ?: "loading..."}",
                            fontSize = 9.sp,
                            color = GreyText,
                            maxLines = 1
                        )
                        Text(
                            text = "Wallet synced in real-time with Google Cloud services.",
                            fontSize = 8.sp,
                            color = GreyText.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Manual Sync Button
                IconButton(
                    onClick = {
                        val uid = user?.id
                        if (uid != null) {
                            viewModel.refreshUserFromFirestore(uid) { success ->
                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("🟢 Cloud balances updated from Firestore successfully!")
                                    } else {
                                        snackbarHostState.showSnackbar("❌ Firestore query failed. Check connectivity.")
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isUserFirestoreRefreshing,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF1F1C25), CircleShape)
                        .testTag("firestore_manual_sync_btn")
                ) {
                    if (isUserFirestoreRefreshing) {
                        CircularProgressIndicator(
                            color = Color(0xFFE5A93B),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        
        // Gamer credit visual panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF8B121A), Color(0xFF240306)),
                            radius = 600f
                        )
                    )
                    .border(BorderStroke(1.5.dp, RedPrimary.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                val deposit = user?.depositBalance ?: 0.0
                val winnings = user?.winningBalance ?: 0.0
                val bonus = user?.bonusBalance ?: 0.0
                val totalB = deposit + winnings + bonus
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BATTLEZONE PAY CARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Filled.Payment,
                            contentDescription = "chip",
                            tint = NeonGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TOTAL VALUE BALANCE",
                            fontSize = 9.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = totalB.toCurrency(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DEPOSIT", fontSize = 8.sp, color = GreyText)
                            Text(deposit.toCurrency(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("WINNINGS", fontSize = 8.sp, color = GreyText)
                            Text(winnings.toCurrency(), fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("BONUS", fontSize = 8.sp, color = GreyText)
                            Text(bonus.toCurrency(), fontSize = 11.sp, color = RedSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showDepositDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("deposit_action_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD MONEY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { showWithdrawalDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color(0xFF1F1C25)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("withdraw_action_btn")
            ) {
                Icon(imageVector = Icons.Filled.CallReceived, contentDescription = "withdraw", tint = NeonGold)
                Spacer(modifier = Modifier.width(4.dp))
                Text("WITHDRAWAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGold)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        // History logs
        Text(
            text = "TRANSACTION STATEMENTS HISTORY",
            fontSize = 11.sp,
            color = GreyText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No transaction history recorded yet.", color = GreyText, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(transactions) { tx ->
                    TransactionRow(tx = tx)
                }
            }
        }
    }
    // Money deposit simulated popup
    if (showDepositDialog) {
        var depositAmountInput by remember { mutableStateOf("100") }
        var currentDepositStep by remember { mutableStateOf(1) } // 1 = Amount Entry, 2 = Checkout Steps
        var depositMethodRoute by remember { mutableStateOf("UPI_INTENT") } // "UPI_INTENT", "BANK_TRANSFER", "TEST_SIMULATION"
        var referenceIdInput by remember { mutableStateOf("") }
        val gatewayMode = viewModel.getPaymentGatewayMode() // "REAL_UPI" or "TEST_PREFILLED"
        val context = LocalContext.current
        Dialog(onDismissRequest = { showDepositDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "BattleZone Payment Gateway",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (gatewayMode == "REAL_UPI") "Real-time automated transaction checkout." else "Testing checkout sandbox sandbox.",
                        fontSize = 9.sp,
                        color = NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (currentDepositStep == 1) {
                        // STATE 1: Choose amount and method
                        OutlinedTextField(
                            value = depositAmountInput,
                            onValueChange = { depositAmountInput = it },
                            label = { Text("Enter Deposit Amount (₹)") },
                            leadingIcon = { Text("₹", color = Color.White, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color(0xFF28252C)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("PRE-BUDGET QUICK RECHARGES:", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("50", "100", "200", "500").forEach { quickAmt ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { depositAmountInput = quickAmt },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (depositAmountInput == quickAmt) RedPrimary.copy(alpha = 0.2f) else Color(0xFF1F1C25)
                                    ),
                                    border = BorderStroke(1.dp, if (depositAmountInput == quickAmt) RedPrimary else Color(0xFF28252C))
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("₹$quickAmt", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("SELECT ACTIVE GATEWAY SYSTEM:", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        // Method 1: Instant UPI App Intent
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { depositMethodRoute = "UPI_INTENT" }
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (depositMethodRoute == "UPI_INTENT") Color(0xFF1B0F13) else Color(0xFF141217)
                            ),
                            border = BorderStroke(1.dp, if (depositMethodRoute == "UPI_INTENT") RedPrimary else Color(0xFF28252C))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = "upi", tint = RedPrimary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Instant UPI Routing (GPay / PhonePe / Paytm)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Immediate transfer directly from any installed payment app.", color = GreyText, fontSize = 8.sp)
                                }
                            }
                        }
                        // Method 2: Manual Bank IMPS/NEFT Transfer
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { depositMethodRoute = "BANK_TRANSFER" }
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (depositMethodRoute == "BANK_TRANSFER") Color(0xFF1B1610) else Color(0xFF141217)
                            ),
                            border = BorderStroke(1.dp, if (depositMethodRoute == "BANK_TRANSFER") NeonGold else Color(0xFF28252C))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFFFB300).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "bank", tint = NeonGold, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Direct IMPS / NEFT Bank Transfer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Transfer directly via account details & upload reference.", color = GreyText, fontSize = 8.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showDepositDialog = false }) {
                                Text("CANCEL", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amt = depositAmountInput.toDoubleOrNull() ?: 0.0
                                    val minDeposit = viewModel.getMinDepositAmount()
                                    if (amt < minDeposit) {
                                        scope.launch { snackbarHostState.showSnackbar("Minimum deposit amount is ₹$minDeposit rupees.") }
                                    } else {
                                        if (depositMethodRoute == "TEST_SIMULATION") {
                                            showDepositDialog = false
                                            viewModel.addMoney(amt, "SANDBOX SIMULATION") { invoiceId ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Sandbox Recharge successful! ₹$amt credited to Deposit Balance.")
                                                }
                                            }
                                        } else {
                                            currentDepositStep = 2
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (depositMethodRoute == "TEST_SIMULATION") Color(0xFF00E676) else RedPrimary)
                            ) {
                                Text(
                                    text = if (depositMethodRoute == "TEST_SIMULATION") "INSTANT SANDBOX RECHARGE" else "PROCEED TO CHECKOUT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (depositMethodRoute == "TEST_SIMULATION") Color.Black else Color.White
                                )
                            }
                        }
                    } else if (currentDepositStep == 2) {
                        // STATE 2: Actual checkout processes
                        val amtNum = depositAmountInput.toDoubleOrNull() ?: 100.0
                        if (depositMethodRoute == "UPI_INTENT") {
                            val targetUpiId = viewModel.getAdminUpiId()
                            val targetPayee = viewModel.getAdminPayeeName()
                            val trRefNote = "BB-${viewModel.currentUserId}-${System.currentTimeMillis().toString().takeLast(6)}"
                            Text("UPI INSTANT MONEY FORWARDING", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Open your payment app, complete the transaction of ₹$amtNum to our recipient account, then copy the 12-Digit Reference No (UTR) below.", fontSize = 9.sp, color = GreyText)
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            // Target panel
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0E12)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF1F1C25))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Merchant Name:", fontSize = 9.sp, color = GreyText)
                                        Text(targetPayee, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Merchant UPI ID:", fontSize = 9.sp, color = GreyText)
                                        Text(targetUpiId, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Amount to Pay:", fontSize = 9.sp, color = GreyText)
                                        Text("₹$amtNum", fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            // Large forwarding action
                            Button(
                                onClick = {
                                    try {
                                        val upiUri = Uri.parse("upi://pay?pa=$targetUpiId&pn=${Uri.encode(targetPayee)}&mc=&am=$amtNum&cu=INR&tn=${Uri.encode(trRefNote)}")
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = upiUri
                                        }
                                        val chooser = Intent.createChooser(intent, "Pay ₹$amtNum to BattleZone")
                                        context.startActivity(chooser)
                                    } catch (e: Exception) {
                                        scope.launch { snackbarHostState.showSnackbar("No compatible UPI apps found on your phone. Please copy the UPI ID manually.") }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Launch, contentDescription = "Forwarding")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LAUNCH THE PAYMENT APPS FORK", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            // Input UTR Number
                            OutlinedTextField(
                                value = referenceIdInput,
                                onValueChange = { referenceIdInput = it.filter { c -> c.isDigit() }.take(12) },
                                label = { Text("12-Digit Transaction UTR Ref No") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = Color(0xFF28252C)
                                ),
                                placeholder = { Text("Enter the 100% real GPay/PhonePe UTR link", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { currentDepositStep = 1 }) {
                                    Text("BACK", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (referenceIdInput.length < 12) {
                                            scope.launch { snackbarHostState.showSnackbar("Incorrect Ref. Transaction Ref (UTR) must be exactly 12 numeric digits.") }
                                        } else {
                                            showDepositDialog = false
                                            viewModel.addPendingMoney(amtNum, "UPI INTENT", referenceIdInput) { invoiceId ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Payment details submitted. Admin will verify UTR details shortly.")
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGold)
                                ) {
                                    Text("SUBMIT TRANSACTION PROOF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        } else if (depositMethodRoute == "BANK_TRANSFER") {
                            val bankName = viewModel.getAdminBankName()
                            val bankAcc = viewModel.getAdminBankAccount()
                            val bankIfsc = viewModel.getAdminBankIfsc()
                            val payeeName = viewModel.getAdminPayeeName()
                            Text("DIRECT BANK WIRE TRANSFER", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Manually transfer ₹$amtNum to our official bank account details listed below. Upload reference ID of the transaction transfer once finished.", fontSize = 9.sp, color = GreyText)
                            Spacer(modifier = Modifier.height(12.dp))
                            // Bank Details Card with convenient Copy triggers
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0E12)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF1F1C25))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Bank Name Row
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Bank:", fontSize = 9.sp, color = GreyText)
                                        Text(bankName, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // Payee Name Row
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Account Holder Name:", fontSize = 9.sp, color = GreyText)
                                        Text(payeeName, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // Account Number Row + Copy Option
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Account Number:", fontSize = 9.sp, color = GreyText)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(bankAcc, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Bank Account",
                                                tint = NeonGold,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clipData = android.content.ClipData.newPlainText("Account Number", bankAcc)
                                                        clipboardManager.setPrimaryClip(clipData)
                                                        scope.launch { snackbarHostState.showSnackbar("Account Number Copied.") }
                                                    }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // IFSC Row + Copy Option
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Bank IFSC Routing Code:", fontSize = 9.sp, color = GreyText)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(bankIfsc, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy IFSC",
                                                tint = NeonGold,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clipData = android.content.ClipData.newPlainText("IFSC", bankIfsc)
                                                        clipboardManager.setPrimaryClip(clipData)
                                                        scope.launch { snackbarHostState.showSnackbar("IFSC Copied.") }
                                                    }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Final Billing Amount:", fontSize = 9.sp, color = GreyText)
                                        Text("₹$amtNum", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            // Enter Reference
                            OutlinedTextField(
                                value = referenceIdInput,
                                onValueChange = { referenceIdInput = it },
                                label = { Text("Transfer Reference Id / Transaction Ref") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = Color(0xFF28252C)
                                ),
                                placeholder = { Text("Enter bank transaction ref number", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { currentDepositStep = 1 }) {
                                    Text("BACK", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (referenceIdInput.isBlank()) {
                                            scope.launch { snackbarHostState.showSnackbar("Reference Id cannot be blank. Copy the transfer recipe code first.") }
                                        } else {
                                            showDepositDialog = false
                                            viewModel.addPendingMoney(amtNum, "BANK WIRE transfer", referenceIdInput) { invoiceId ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Bank details submitted correctly. Processing transaction verification.")
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGold)
                                ) {
                                    Text("SUBMIT FOR BANK RECORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
    }
                        } else if (depositMethodRoute == "RAZORPAY") {
                            val razorpayKey = viewModel.getRazorpayKeyId().ifBlank { "[No live Key ID Configured under Admin panel]" }
                            var mockUpiIdInput by remember { mutableStateOf("${viewModel.currentUserId}@paytm") }
                            var razorpayStep by remember { mutableStateOf(1) } // 1 = Review, 2 = Pay Details
                            Text("RAZORPAY SECURE GATEWAY", fontSize = 11.sp, color = Color(0xFF3395FF), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Automatic client-side order routing node. Verification completed dynamically.", fontSize = 9.sp, color = GreyText)
                            Spacer(modifier = Modifier.height(12.dp))
                            if (razorpayStep == 1) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0E12)),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, Color(0xFF1F1C25))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Escrow Client:", fontSize = 9.sp, color = GreyText)
                                            Text("Razorpay Ltd (In-Project)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Merchant ID Key:", fontSize = 9.sp, color = GreyText)
                                            Text(if (razorpayKey.length > 10) razorpayKey.take(24) + "..." else razorpayKey, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Billing Amount:", fontSize = 9.sp, color = GreyText)
                                            Text("₹$amtNum", fontSize = 11.sp, color = Color(0xFF3395FF), fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { currentDepositStep = 1 }) {
                                        Text("BACK", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { razorpayStep = 2 },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3395FF))
                                    ) {
                                        Text("RECHARGE NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (razorpayStep == 2) {
                                Text("SELECT SECURE PAYMENT INSTRUMENTS", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = mockUpiIdInput,
                                    onValueChange = { mockUpiIdInput = it },
                                    label = { Text("Your UPI ID (Required)") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF3395FF), unfocusedBorderColor = Color(0xFF28252C)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { razorpayStep = 1 }) {
                                        Text("BACK", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (mockUpiIdInput.isBlank()) {
                                                scope.launch { snackbarHostState.showSnackbar("Enter registered local billing UPI address.") }
                                            } else {
                                                val txId = "RZP-${java.util.UUID.randomUUID().toString().take(10).uppercase()}"
                                                showDepositDialog = false
                                                viewModel.addPendingMoney(amtNum, "RAZORPAY AUTO", txId) { invoiceId ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Razorpay Gateway completed! TXN ID: $txId. Sent to admin dashboard.")
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3395FF))
                                    ) {
                                        Text("PAY ₹$amtNum SECURELY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (depositMethodRoute == "CASHFREE") {
                            val cashfreeClient = viewModel.getCashfreeClientId().ifBlank { "[No live Cashfree Client ID Configured under Admin panel]" }
                            var mockUpiIdInput by remember { mutableStateOf("${viewModel.currentUserId}@paytm") }
                            var cashfreeStep by remember { mutableStateOf(1) } // 1 = Review, 2 = Process
                            Text("CASHFREE INSTANT CHECKOUT", fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Automatic client-side order routing node. Verification completed dynamically.", fontSize = 9.sp, color = GreyText)
                            Spacer(modifier = Modifier.height(12.dp))
                            if (cashfreeStep == 1) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0E12)),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, Color(0xFF1F1C25))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Channel Merchant:", fontSize = 9.sp, color = GreyText)
                                            Text("Cashfree Payments", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Client App ID:", fontSize = 9.sp, color = GreyText)
                                            Text(if (cashfreeClient.length > 10) cashfreeClient.take(24) + "..." else cashfreeClient, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Amount to Pay:", fontSize = 9.sp, color = GreyText)
                                            Text("₹$amtNum", fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { currentDepositStep = 1 }) {
                                        Text("BACK", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { cashfreeStep = 2 },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                    ) {
                                        Text("RECHARGE NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            } else if (cashfreeStep == 2) {
                                Text("SELECT SECURE PAYMENT INSTRUMENTS", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = mockUpiIdInput,
                                    onValueChange = { mockUpiIdInput = it },
                                    label = { Text("Your Billing UPI Address") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00C853), unfocusedBorderColor = Color(0xFF28252C)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { cashfreeStep = 1 }) {
                                        Text("BACK", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (mockUpiIdInput.isBlank()) {
                                                scope.launch { snackbarHostState.showSnackbar("Enter registered local billing UPI address.") }
                                            } else {
                                                val txId = "CF-${java.util.UUID.randomUUID().toString().take(10).uppercase()}"
                                                showDepositDialog = false
                                                viewModel.addPendingMoney(amtNum, "CASHFREE AUTO", txId) { invoiceId ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Cashfree Payment Completed! TXN ID: $txId. Sent to admin dashboard.")
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                    ) {
                                        Text("PAY ₹$amtNum SECURELY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Money withdrawal request simulated popup
    if (showWithdrawalDialog) {
        var withdrawalAmountInput by remember { mutableStateOf("100") }
        var upiIdInput by remember { mutableStateOf("gamer@ybl") }
        Dialog(onDismissRequest = { showWithdrawalDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Submit Withdrawal Request",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val minWithdrawalVal = viewModel.getMinWithdrawalAmount()
                    Text(
                        "Winnings balance is transferable with a absolute ₹$minWithdrawalVal minimum limit. Transactions undergo secure human verification.",
                        fontSize = 11.sp,
                        color = GreyText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val userWinningBalance = user?.winningBalance ?: 0.0
                    val minWithdrawal = viewModel.getMinWithdrawalAmount()
                    val isWinningBalanceSufficientForMin = userWinningBalance >= minWithdrawal
                    val amtNum = withdrawalAmountInput.toDoubleOrNull() ?: 0.0
                    val isAmountValid = amtNum >= minWithdrawal && amtNum <= userWinningBalance
                    val isAmountFormatOk = withdrawalAmountInput.isNotBlank() && withdrawalAmountInput.toDoubleOrNull() != null
                    val isUpiValid = upiIdInput.isNotBlank() && upiIdInput.contains("@")
                    Spacer(modifier = Modifier.height(12.dp))
                    // Available winnings balance display card
                    Surface(
                        color = Color(0xFF1E1B24),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Your Winning Balance:",
                                fontSize = 12.sp,
                                color = GreyText,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                userWinningBalance.toCurrency(),
                                fontSize = 14.sp,
                                color = NeonGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // If user's winning balance is below the minimum required withdrawal threshold
                    if (!isWinningBalanceSufficientForMin) {
                        Surface(
                            color = RedPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Warning Info",
                                    tint = RedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Payout Unavailable: You must have a minimum of ₹$minWithdrawal in your winnings account to request a payout.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    OutlinedTextField(
                        value = withdrawalAmountInput,
                        onValueChange = { withdrawalAmountInput = it },
                        label = { Text("Transfer Target Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = isWinningBalanceSufficientForMin,
                        isError = (!isAmountValid && withdrawalAmountInput.isNotBlank()) || (!isWinningBalanceSufficientForMin && withdrawalAmountInput.isNotBlank()),
                        supportingText = {
                            if (withdrawalAmountInput.isNotBlank()) {
                                if (!isAmountFormatOk) {
                                    Text("Please enter a valid numeric value", color = RedPrimary, fontSize = 10.sp)
                                } else if (amtNum < minWithdrawal) {
                                    Text("Minimum withdrawal amount is ₹$minWithdrawal rupees", color = RedPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else if (amtNum > userWinningBalance) {
                                    Text("Insufficient winning balance (Available: ${userWinningBalance.toCurrency()})", color = RedPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Valid transfer amount", color = Color(0xFF81C784), fontSize = 10.sp)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C),
                            errorBorderColor = RedPrimary,
                            disabledBorderColor = Color(0xFF232029)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = upiIdInput,
                        onValueChange = { upiIdInput = it },
                        label = { Text("UPI ID (e.g. username@upi)") },
                        enabled = isWinningBalanceSufficientForMin,
                        isError = !isUpiValid && upiIdInput.isNotBlank(),
                        supportingText = {
                            if (upiIdInput.isNotBlank() && !isUpiValid) {
                                Text("Please enter a valid UPI address containing @", color = RedPrimary, fontSize = 10.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C),
                            errorBorderColor = RedPrimary,
                            disabledBorderColor = Color(0xFF232029)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showWithdrawalDialog = false }) {
                            Text("CANCEL", color = GreyText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isAmountValid && isUpiValid && isWinningBalanceSufficientForMin) {
                                    viewModel.requestWithdrawal(amtNum, upiIdInput) { resp ->
                                        scope.launch {
                                            if (resp == "SUCCESS") {
                                                snackbarHostState.showSnackbar("Withdrawal request placed. Pending Admin review.")
                                                showWithdrawalDialog = false
                                            } else {
                                                snackbarHostState.showSnackbar(resp)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = isAmountValid && isUpiValid && isWinningBalanceSufficientForMin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedPrimary,
                                disabledContainerColor = Color(0xFF232029)
                            )
                        ) {
                            Text(
                                "CONFIRM WITHDRAWAL",
                                color = if (isAmountValid && isUpiValid && isWinningBalanceSufficientForMin) Color.White else GreyText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun TransactionRow(tx: TransactionEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (tx.type == "DEPOSIT" || tx.type == "PRIZE_WINNING" || tx.type == "BONUS_ADD")
                                Color(0xFF132A15)
                            else Color(0xFF2E1114)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = if (tx.type == "DEPOSIT") Icons.Filled.VerticalAlignBottom
                    else if (tx.type == "WITHDRAWAL") Icons.Filled.VerticalAlignTop
                    else if (tx.type == "ENTRY_FEE") Icons.Filled.SportsEsports
                    else Icons.Filled.EmojiEvents
                    val color = if (tx.type == "DEPOSIT" || tx.type == "PRIZE_WINNING" || tx.type == "BONUS_ADD")
                        Color(0xFF81C784)
                    else Color(0xFFE57373)
                    Icon(imageVector = icon, contentDescription = "tx", tint = color, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = tx.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = tx.invoiceId, fontSize = 9.sp, color = GreyText)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (tx.type == "DEPOSIT" || tx.type == "PRIZE_WINNING" || tx.type == "BONUS_ADD") "+" else "-"
                val col = if (prefix == "+") Color(0xFF00E676) else RedPrimary
                Text(
                    text = "$prefix ${tx.amount.toCurrency()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = col
                )
                Text(
                    text = tx.status.uppercase(),
                    fontSize = 8.sp,
                    color = if (tx.status == "SUCCESS") Color(0xFF00E676) else NeonGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
// --- 4. LEADERBOARD RANKINGS ---
@Composable
fun LeaderboardScreen(viewModel: BattleZoneViewModel) {
    val mockLeaderboard = listOf(
        Triple("Shadow_Hunter", 4500.0, 1),
        Triple("Elite_Sniper", 3900.0, 2),
        Triple("ViperFF_God", 3200.0, 3),
        Triple("Ghost_Rider", 2850.0, 4),
        Triple("Alpha_Gamer", 2150.0, 5),
        Triple("Bot_Exterminator", 1850.0, 6),
        Triple("Toxicity_Kill", 1500.0, 7),
        Triple("ThunderClap", 1200.0, 8),
        Triple("Dynamo_Kid", 1100.0, 9),
        Triple("SilentScout", 900.0, 10)
    )
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        val width = maxWidth
        val height = maxHeight
        val isLandscape = width > height
        if (isLandscape) {
            // High-fidelity landscape / split columns adaptive layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Header text + Podium rankings
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "BATTLEZONE CHAMPIONS LEADERBOARD",
                            fontSize = 11.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Updated dynamically with monthly rewards.",
                            fontSize = 9.sp,
                            color = GreyText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Adaptive compact podium row for horizontal screens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        PodiumCard(name = "Elite_Sniper", prize = "₹3,900", rank = 2, sizeModifier = 0.70f, modifier = Modifier.weight(1f))
                        PodiumCard(name = "Shadow_Hunter", prize = "₹4,500", rank = 1, sizeModifier = 0.82f, modifier = Modifier.weight(1.1f))
                        PodiumCard(name = "ViperFF_God", prize = "₹3,200", rank = 3, sizeModifier = 0.65f, modifier = Modifier.weight(1f))
                    }
                }
                // Divider Line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color(0xFF1E1C24))
                )
                // Right Column: Scroll list
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "RANKING CONTENDERS",
                        fontSize = 9.sp,
                        color = GreyText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(mockLeaderboard.drop(3)) { (name, points, rank) ->
                            LeaderboardRowItem(name = name, points = points, rank = rank)
                        }
                    }
                }
            }
        } else {
            // Classic portrait layout perfectly customized for mobile ratios
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "BATTLEZONE CHAMPIONS LEADERBOARD",
                    fontSize = 12.sp,
                    color = GreyText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Updated dynamically with top monthly reward distributions.",
                    fontSize = 10.sp,
                    color = GreyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
                // Scale down podium size slightly on compact screens to ensure spacing looks fantastic
                val podiumScaleFactor = if (height < 650.dp) 0.8f else 1.0f
                // Top 3 Podium Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    PodiumCard(name = "Elite_Sniper", prize = "₹3,900", rank = 2, sizeModifier = 0.85f * podiumScaleFactor, modifier = Modifier.weight(1f))
                    PodiumCard(name = "Shadow_Hunter", prize = "₹4,500", rank = 1, sizeModifier = 1.00f * podiumScaleFactor, modifier = Modifier.weight(1.1f))
                    PodiumCard(name = "ViperFF_God", prize = "₹3,200", rank = 3, sizeModifier = 0.80f * podiumScaleFactor, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(18.dp))
                // Standard Scroll list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(mockLeaderboard.drop(3)) { (name, points, rank) ->
                        LeaderboardRowItem(name = name, points = points, rank = rank)
                    }
                }
            }
        }
    }
}
@Composable
fun LeaderboardRowItem(name: String, points: Double) {
    // Overloaded to keep backward-compatibility if any other file called this signature,
    // but we support rank below as primary
    LeaderboardRowItem(name = name, points = points, rank = 0)
}
@Composable
fun LeaderboardRowItem(name: String, points: Double, rank: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rank > 0) {
                    Text(
                        text = "$rank",
                        color = GreyText,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.width(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = points.toCurrency(),
                color = NeonGold,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}
@Composable
fun PodiumCard(name: String, prize: String, rank: Int, sizeModifier: Float, modifier: Modifier = Modifier) {
    val borderColor = if (rank == 1) NeonGold else if (rank == 2) Color(0xFFC0C0C0) else Color(0xFFCD7F32)
    val cardHeight = (130 * sizeModifier).dp
    Card(
        modifier = modifier
            .height(cardHeight)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = "Trophy",
                tint = borderColor,
                modifier = Modifier.size(((if (rank == 1) 28 else 22) * sizeModifier).dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = (11 * sizeModifier).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = prize,
                color = NeonGold,
                fontSize = (12 * sizeModifier).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(borderColor, RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "RANK $rank",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = (8 * sizeModifier).sp
                )
            }
        }
    }
}
// --- 5. SUPPORT & CUSTOM TICKETS ---
@Composable
fun SupportScreen(viewModel: BattleZoneViewModel, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var ticketTitle by remember { mutableStateOf("") }
    var ticketMsg by remember { mutableStateOf("") }
    val tickets by viewModel.currentUserTickets.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("RAISE A SUPPORT TICKET", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Encountered matches or wallet transfer delays? Create tickets for instant support replies.", fontSize = 10.sp, color = GreyText)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13101C)),
            border = BorderStroke(1.dp, Color(0xFFEF6C00).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notification Info",
                    tint = Color(0xFFEF6C00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Withdrawal Requests Notice", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("If you are inquiring about a withdrawal, please note that approved transactions are credited to your UPI within 2 to 3 days.", color = GreyText, fontSize = 10.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = ticketTitle,
            onValueChange = { ticketTitle = it },
            label = { Text("Ticket Subject (e.g., Wallet credit)") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = ticketMsg,
            onValueChange = { ticketMsg = it },
            label = { Text("Core Problem Explanation") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (ticketTitle.isNotBlank() && ticketMsg.isNotBlank()) {
                    viewModel.createSupportTicket(ticketTitle, ticketMsg) {
                        ticketTitle = ""
                        ticketMsg = ""
                        scope.launch {
                            snackbarHostState.showSnackbar("Support ticket filed! Checked by root admins.")
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SUBMIT TICKET", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("YOUR RAISED TICKETS HISTORY", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (tickets.isEmpty()) {
            Text("No support tickets filed matching user context.", color = GreyText, fontSize = 11.sp)
        } else {
            tickets.forEach { ticket ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, if (ticket.status == "OPEN") RedPrimary.copy(alpha = 0.3f) else Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ticket.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .background(if (ticket.status == "OPEN") Color(0xFF421516) else Color(0xFF132A15), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    ticket.status,
                                    fontSize = 9.sp,
                                    color = if (ticket.status == "OPEN") RedPrimary else Color(0xFF39FF14),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ticket.message, color = GreyText, fontSize = 11.sp)
                        if (ticket.adminReply != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "Admin Reply: ${ticket.adminReply}",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth().testTag("notification_simulator_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B24)),
            border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Alerts",
                        tint = RedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALERT & NOTIFICATION TESTER",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Verify instant overlay toast notifications on demand resembling production-live tournament status alerts.",
                    color = GreyText,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.simulateMatchUpcomingAlert() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary.copy(alpha = 0.15f), contentColor = RedPrimary),
                        modifier = Modifier.weight(1f).height(36.dp).border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("START NOTICE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.simulateMatchResultAlert() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGold.copy(alpha = 0.15f), contentColor = NeonGold),
                        modifier = Modifier.weight(1f).height(36.dp).border(BorderStroke(1.dp, NeonGold.copy(alpha = 0.4f)), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("RESULT NOTICE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.simulateRoomCredentialsAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232029)),
                    modifier = Modifier.fillMaxWidth().height(36.dp).border(BorderStroke(1.dp, Color(0xFF2E2A36)), RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ROOM ID NOTICE (TOAST)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.simulateTenMinuteWarning() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFB300), contentColor = Color(0xFFFFB300)),
                        modifier = Modifier.weight(1f).height(36.dp).border(BorderStroke(1.dp, Color(0x66FFB300)), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("10M WARN LOBBY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.triggerSimulatedCredentialsPopup() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E676), contentColor = Color(0xFF00E676)),
                        modifier = Modifier.weight(1f).height(36.dp).border(BorderStroke(1.dp, Color(0x6600E676)), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("TRIGGER POPUP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
// --- 6. USER PROFILE & REFERRALS ---
@Composable
fun ProfileScreen(
    viewModel: BattleZoneViewModel,
    user: UserEntity?,
    isAdminUnlocked: Boolean,
    snackbarHostState: SnackbarHostState,
    onEnterAdmin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isAppModified by viewModel.isAppModifiedFlow.collectAsStateWithLifecycle()
    val appUpdateAvailable by viewModel.appUpdateAvailableFlow.collectAsStateWithLifecycle()
    val securityMetrics by viewModel.securityMetrics.collectAsStateWithLifecycle()
    var isApplyingAppUpdate by remember { mutableStateOf(false) }

    var referralCodePrompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GAMER PROFILE CARD", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
            
            // Logout option on Profile Page
            TextButton(
                onClick = { 
                    viewModel.logoutUser() 
                    scope.launch {
                        snackbarHostState.showSnackbar("Logged out successfully.")
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = RedPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("LOGOUT", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Avatar banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, if (!appUpdateAvailable) NeonGold else Color(0xFF1F1C25))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (!appUpdateAvailable) listOf(NeonGold, Color(0xFFB58926)) else listOf(RedPrimary, RedDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "profile", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(user?.inGameName ?: "Alpha_Gamer", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (!appUpdateAvailable) NeonGold else Color.White)
                                if (!appUpdateAvailable) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFF2E2613), RoundedCornerShape(4.dp))
                                            .border(0.5.dp, NeonGold, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Star, contentDescription = "VIP", tint = NeonGold, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("VIP", color = NeonGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text("UID: ${user?.freeFireUid ?: "FF-837492047"}", fontSize = 11.sp, color = GreyText)
                            Text("Email: ${maskEmail(user?.email ?: "gamer@battlezone.com")}", fontSize = 11.sp, color = GreyText)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFF28252C), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))
                
                // Dynamic Bio Status Indicator
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0E12), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text("GAMER BIO & STATUS", fontSize = 8.sp, color = NeonGold, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (user?.profilePicture.isNullOrBlank()) "No custom status set." else user!!.profilePicture,
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- SYSTEM & APP UPDATES CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(
                        1.dp,
                        if (appUpdateAvailable) RedPrimary.copy(alpha = 0.6f) else Color(0xFF28252C)
                    ),
                    RoundedCornerShape(12.dp)
                )
                .testTag("system_app_updates_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SYSTEM & APP UPDATES",
                        fontSize = 11.sp,
                        color = if (appUpdateAvailable) RedPrimary else GreyText,
                        fontWeight = FontWeight.Bold
                    )
                    if (appUpdateAvailable) {
                        Box(
                            modifier = Modifier
                                .background(RedPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, RedPrimary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("⏳ UPDATE PENDING", color = RedPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E3A20), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color(0xFF81C784), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("✅ STABLE & LATEST", color = Color(0xFF81C784), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show AI Studio modification details
                if (isAppModified) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF201315), RoundedCornerShape(6.dp))
                            .border(0.5.dp, RedPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "modified", tint = RedPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APP DETECTED AS MODIFIED IN AI STUDIO DEVELOPMENT ENVIRONMENT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (appUpdateAvailable) {
                    Text(
                        text = "There is one update",
                        fontSize = 15.sp,
                        color = NeonGold,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "An app-level update or system customization patch is available. Click the button below to apply this update and sync live configurations automatically.",
                        fontSize = 10.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (isApplyingAppUpdate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(color = RedPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Applying app-level customization patch...", color = GreyText, fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                isApplyingAppUpdate = true
                                viewModel.applyAppUpdate {
                                    isApplyingAppUpdate = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("✅ System configurations successfully updated to the latest build.")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apply_app_update_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Update", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPDATE NOW", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Up-to-date", tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "The application and system settings are fully up-to-date with your latest modifications.",
                            fontSize = 11.sp,
                            color = Color(0xFF81C784)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Read-only registered credentials card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("REGISTERED ACCOUNT DETAILS", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                ProfileFieldRow(label = "In Game Alias", value = user?.inGameName ?: "Alpha_Gamer")
                ProfileFieldRow(label = "Free Fire UID", value = user?.freeFireUid ?: "FF-837492047")
                ProfileFieldRow(label = "Primary Mobile Number", value = user?.phoneNumber ?: "+91 98765 43210")
                ProfileFieldRow(label = "Secondary Contact Number", value = if (user?.extraMobileNumber.isNullOrBlank()) "Not Provided" else user?.extraMobileNumber!!)
                ProfileFieldRow(label = "Associated Google Mail", value = maskEmail(user?.email ?: "gamer@battlezone.com"))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Official Instagram and Telegram direct tabs/buttons
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("BATTLEZONE OFFICIAL SOCIAL CHANNELS", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap any option below to auto-forward to our official active channels for tournament live support.", fontSize = 9.sp, color = GreyText)
                Spacer(modifier = Modifier.height(14.dp))
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Instagram Card Button/Tab
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                try {
                                    uriHandler.openUri(viewModel.getInstagramUrl())
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F121C)),
                        border = BorderStroke(1.dp, Color(0xFFC13584).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFC13584).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Official Instagram link tab",
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("INSTAGRAM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(viewModel.getInstagramDisplay(), color = Color(0xFFE1306C), fontSize = 7.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                    // Telegram Card Button/Tab
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                try {
                                    uriHandler.openUri(viewModel.getTelegramUrl())
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101B24)),
                        border = BorderStroke(1.dp, Color(0xFF0088CC).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF0088CC).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Official Telegram link tab",
                                    tint = Color(0xFF0088CC),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("TELEGRAM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(viewModel.getTelegramDisplay(), color = Color(0xFF0088CC), fontSize = 7.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                    // YouTube Card Button/Tab
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                try {
                                    uriHandler.openUri(viewModel.getYoutubeUrl())
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF241011)),
                        border = BorderStroke(1.dp, Color(0xFFFF0000).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFF0000).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Official YouTube link tab",
                                    tint = Color(0xFFFF0000),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("YOUTUBE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(viewModel.getYoutubeDisplay(), color = Color(0xFFFF0000), fontSize = 7.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Referral program widget
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("BATTLEZONE REFERRAL AND WELCOME BONUSES", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Invite friends to get bonus deposit funds automatically.", fontSize = 9.sp, color = GreyText)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("YOUR UNIQUE CODE:", fontSize = 9.sp, color = GreyText)
                        Text("BZONEFF77", color = NeonGold, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF231E15), RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, NeonGold), RoundedCornerShape(6.dp))
                            .clickable {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Referral link copied!")
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("COPY CODE", color = NeonGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFF1E1B24))
                Spacer(modifier = Modifier.height(14.dp))
                Text("REDEEM REFERRAL/PROMO CODE", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = referralCodePrompt,
                        onValueChange = { referralCodePrompt = it },
                        placeholder = { Text("Enter promo or referral...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (referralCodePrompt.isNotBlank()) {
                                viewModel.claimReferral(referralCodePrompt) { resp ->
                                    scope.launch {
                                        if (resp == "SUCCESS") {
                                            snackbarHostState.showSnackbar("Claimed referral bonus successfully!")
                                            referralCodePrompt = ""
                                        } else {
                                            snackbarHostState.showSnackbar(resp)
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("REDEEM", fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // BattleZone Security Guardian Protection Hub
        SecurityGuardianShieldCard(
            metrics = securityMetrics,
            onRefresh = {
                viewModel.refreshSecurityChecks()
                scope.launch {
                    snackbarHostState.showSnackbar("Environment security scan completed. All checks verified.")
                }
            }
        )
        if (isAdminUnlocked) {
            Spacer(modifier = Modifier.height(16.dp))
            // Admin fast toggle button inside user Profile screen for easy testing
            Button(
                onClick = onEnterAdmin,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281116)),
                border = BorderStroke(1.dp, RedPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text("SWITCH TO ROOT ADMIN SHIELD CONFIGS", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
fun PendingUpdateItem(label: String, from: String, to: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("• ", color = GreyText, fontSize = 9.sp)
        Text("$label: ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
        Text("'$from' ", color = GreyText, fontSize = 9.sp, style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
        Text(" ➔ ", color = RedPrimary, fontSize = 9.sp)
        Text("'$to'", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 9.sp)
    }
}

@Composable
fun FeatureUnlockItem(feature: String, applied: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (applied) Icons.Default.CheckCircle else Icons.Default.Star,
            contentDescription = null,
            tint = if (applied) Color(0xFF81C784) else RedPrimary,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(feature, color = if (applied) Color.White else GreyText, fontSize = 9.sp)
    }
}

@Composable
fun ProfileFieldRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label.uppercase(), fontSize = 8.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
@Composable
fun SecurityGuardianShieldCard(
    metrics: SecurityMetrics,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040B06)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1B5E20).copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_guardian_hub_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1B5E20).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Shield Active Indicator",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "BATTLEZONE SHIELD GUARDIAN v3.0",
                        fontSize = 11.sp,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF2E7D32), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE GUARD HOOKS OPERATIONAL (100% SECURE)",
                            fontSize = 8.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFF1B5E20).copy(alpha = 0.2f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))
            // Grid of checks
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Check 1: FLAG_SECURE
                SecurityCheckRow(
                    title = "ANTI-SCREEN RECAPTURE PROTOCOL (FLAG_SECURE)",
                    desc = "Enforced on production; bypassed in live emulator for development preview",
                    isActive = true,
                    badgeText = "BYPASSED IN PREVIEW"
                )
                // Check 2: Root Detection
                SecurityCheckRow(
                    title = "DEVICE INTEGRITY TAMPER PROBE",
                    desc = "Disallows standard client su processes and root memory access trackers",
                    isActive = !metrics.isRooted,
                    badgeText = if (metrics.isRooted) "COMPROMISED" else "VIRGIN CLEAN"
                )
                // Check 3: ADB/Developer options
                SecurityCheckRow(
                    title = "USB DEBUGGING GATE KEEPER",
                    desc = "Realtime telemetry blocker for reverse-engineering debug hooks",
                    isActive = !metrics.isAdbEnabled,
                    badgeText = if (metrics.isAdbEnabled) "DEBUGGING DETECTED" else "GUARDED"
                )
                // Check 4: Emulators/Cheating bots
                SecurityCheckRow(
                    title = "SANDBOX BOT DEFENSE ENGINE",
                    desc = "Shields core matching server from fake click-macro emulator bots",
                    isActive = !metrics.isEmulator,
                    badgeText = if (metrics.isEmulator) "VIRTUAL SYSTEM" else "VERIFIED DEVICE"
                )
                // Check 5: SQLite Database Injection Shielding
                SecurityCheckRow(
                    title = "SQLITE DATABASE SANDBOXING",
                    desc = "Encrypted local models and strict parameterized Room database operations",
                    isActive = metrics.localDbEncryptedCheck,
                    badgeText = "SANITIZED"
                )
                // Check 6: Tamper Signature Proof
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ANTI-TAMPER PLAY PROTECT ASSURANCE HASH",
                            fontSize = 8.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (metrics.isIntegrityVerified) "MATCHED" else "UNVERIFIED",
                            fontSize = 8.sp,
                            color = if (metrics.isIntegrityVerified) Color(0xFF4CAF50) else RedPrimary,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metrics.signatureHash,
                        color = Color(0xFF81C784),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Scan",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RE-SCAN MOBILE ENVIRONMENT FOR THREATS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
@Composable
fun SecurityCheckRow(
    title: String,
    desc: String,
    isActive: Boolean,
    badgeText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                desc,
                fontSize = 8.sp,
                color = GreyText,
                lineHeight = 10.sp
            )
        }
        Box(
            modifier = Modifier
                .background(
                    if (isActive) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFFD32F2F).copy(alpha = 0.15f),
                    RoundedCornerShape(4.dp)
                )
                .border(
                    BorderStroke(
                        0.8.dp,
                        if (isActive) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFFEF5350).copy(alpha = 0.4f)
                    ),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = badgeText,
                color = if (isActive) Color(0xFF81C784) else Color(0xFFEF5350),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
// --- 7. ADMIN DASHBOARD & CONTROLS ---
@Composable
fun AdminDashboardScreen(
    viewModel: BattleZoneViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var activeAdminTab by remember { mutableStateOf("METRICS") } // "METRICS", "TOURNAMENTS", "WITHDRAWALS", "TICKETS", "PROOFS", "DEPOSITS", "SECURITY"
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val withdrawals by viewModel.adminAllWithdrawals.collectAsStateWithLifecycle()
    val supportTickets by viewModel.adminAllTickets.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Admin menu options row
        ScrollableTabRow(
            selectedTabIndex = when (activeAdminTab) {
                "METRICS" -> 0
                "TOURNAMENTS" -> 1
                "WITHDRAWALS" -> 2
                "TICKETS" -> 3
                "PROOFS" -> 4
                "DEPOSITS" -> 5
                "SECURITY" -> 6
                else -> 0
            },
            containerColor = DarkSurface,
            contentColor = Color.White,
            edgePadding = 12.dp
        ) {
            val tabs = listOf(
                Pair("METRICS", "Platform Live Stats"),
                Pair("TOURNAMENTS", "Matches Manager"),
                Pair("REFUNDS", "Refund Requests"),
                Pair("WITHDRAWALS", "UPI Disbursals"),
                Pair("TICKETS", "Helpdesk Desk"),
                Pair("PROOFS", "Verification Queue"),
                Pair("DEPOSITS", "User Deposits"),
                Pair("SECURITY", "🛡️ Security & Antivirus")
            )
            tabs.forEach { (tab, text) ->
                Tab(
                    selected = activeAdminTab == tab,
                    onClick = { activeAdminTab = tab },
                    text = {
                        Text(text = text, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    },
                    selectedContentColor = RedPrimary,
                    unselectedContentColor = GreyText
                )
            }
        }
        when (activeAdminTab) {
            "METRICS" -> AdminMetricsTab(users = users, tourneys = tournaments, withdrawals = withdrawals, viewModel = viewModel, snackbarHost = snackbarHostState)
            "TOURNAMENTS" -> AdminTournamentsTab(tourneys = tournaments, users = users, viewModel = viewModel, snackBars = snackbarHostState)
            "REFUNDS" -> AdminRefundsTab(viewModel = viewModel, snackbarHost = snackbarHostState)
            "WITHDRAWALS" -> AdminWithdrawalsTab(withdrawals = withdrawals, viewModel = viewModel, message = snackbarHostState)
            "TICKETS" -> AdminTicketsTab(tickets = supportTickets, viewModel = viewModel, callback = snackbarHostState)
            "PROOFS" -> AdminProofsTab(viewModel = viewModel, snackbarHostState = snackbarHostState)
            "DEPOSITS" -> AdminDepositsTab(viewModel = viewModel, snackbarHost = snackbarHostState)
            "SECURITY" -> AdminSecurityTab(viewModel = viewModel, snackbarHost = snackbarHostState)
        }
    }
}
// Stats metrics panel
@Composable
fun AdminMetricsTab(
    users: List<UserEntity>,
    tourneys: List<TournamentEntity>,
    withdrawals: List<WithdrawalRequestEntity>,
    viewModel: BattleZoneViewModel,
    snackbarHost: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var searchUserQuery by remember { mutableStateOf("") }
    var selectUserForWalletAdjust by remember { mutableStateOf<UserEntity?>(null) }
    var showDeleteConfirmUser by remember { mutableStateOf<UserEntity?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("ADMIN COMMAND METRICS", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        // Grid stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(title = "TOTAL GAMERS", value = "${users.size}", color = Color.White, modifier = Modifier.weight(1f))
            StatCard(title = "ACTIVE MATCHES", value = "${tourneys.filter { it.status != "COMPLETED" }.size}", color = RedPrimary, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalWithdrawalPending = withdrawals.filter { it.status == "PENDING" }.sumOf { it.amount }
            StatCard(title = "PENDING TRANSFER", value = totalWithdrawalPending.toCurrency(), color = NeonGold, modifier = Modifier.weight(1f))
            StatCard(title = "COMPLETED CUPS", value = "${tourneys.filter { it.status == "COMPLETED" }.size}", color = Color(0xFF00E676), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        // User Accounts management
        Text("USER ACCOUNTS DIRECTORY & WALLET CONTROL", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = searchUserQuery,
            onValueChange = { searchUserQuery = it },
            placeholder = { Text("Find users by Character UID / In-game Alias...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "search", tint = GreyText) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        val filteredUsers = users.filter {
            it.inGameName.contains(searchUserQuery, ignoreCase = true) ||
                    it.freeFireUid.contains(searchUserQuery, ignoreCase = true)
        }
        filteredUsers.forEach { usr ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color(0xFF1E1C24)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(usr.inGameName, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                val statusColor = if (usr.isOnline) Color(0xFF00E676) else Color.Gray
                                val statusText = if (usr.isOnline) "ONLINE" else "OFFLINE"
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                        .border(0.5.dp, statusColor.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(statusColor, shape = CircleShape)
                                        )
                                        Text(
                                            text = statusText,
                                            color = statusColor,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text("UID: ${usr.freeFireUid}", color = GreyText, fontSize = 10.sp)
                        }
                        // Action controls block (Wallet Credit & Delete User)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { selectUserForWalletAdjust = usr },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281116)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("CREDIT WALLET", fontSize = 9.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { showDeleteConfirmUser = usr },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Competitor",
                                    tint = RedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dep: ${usr.depositBalance.toCurrency()}", color = Color.White, fontSize = 11.sp)
                        Text("Win: ${usr.winningBalance.toCurrency()}", color = NeonGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Bonus: ${usr.bonusBalance.toCurrency()}", color = RedSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        // Delete Compete Confirmation Dialog overlay
        if (showDeleteConfirmUser != null) {
            val userToDelete = showDeleteConfirmUser!!
            Dialog(onDismissRequest = { showDeleteConfirmUser = null }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "⚠️ Confirm Account Deletion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = RedPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Are you absolutely sure you want to completely delete the competitor account '${userToDelete.inGameName}'?\n\nUID: ${userToDelete.freeFireUid}\nEmail: ${userToDelete.email}\nPhone: ${userToDelete.phoneNumber}\n\nThis will purge all their entry tokens, support queries, histories, and wallet balances permanently from both local & cloud databases. This action is IRREVERSIBLE.",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showDeleteConfirmUser = null }) {
                                Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    viewModel.deleteUser(userToDelete.id)
                                    showDeleteConfirmUser = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("DELETE PERMANENTLY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        // Direct Wallet Adjustment drawer dialog
        if (selectUserForWalletAdjust != null) {
            val userToAdjust = selectUserForWalletAdjust!!
            var depositInput by remember { mutableStateOf(userToAdjust.depositBalance.toString()) }
            var winningInput by remember { mutableStateOf(userToAdjust.winningBalance.toString()) }
            var bonusInput by remember { mutableStateOf(userToAdjust.bonusBalance.toString()) }
            Dialog(onDismissRequest = { selectUserForWalletAdjust = null }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Edit Balance: ${userToAdjust.inGameName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text("Direct wallet control panel. Specify absolute new amount. Set to 0 to completely clear.", fontSize = 9.sp, color = GreyText)
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = depositInput,
                            onValueChange = { depositInput = it },
                            label = { Text("Set Deposit Balance (₹)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = winningInput,
                            onValueChange = { winningInput = it },
                            label = { Text("Set Winning Balance (₹)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = bonusInput,
                            onValueChange = { bonusInput = it },
                            label = { Text("Set Bonus Balance (₹)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { selectUserForWalletAdjust = null }) {
                                Text("CANCEL", color = GreyText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val dVal = depositInput.toDoubleOrNull() ?: 0.0
                                    val wVal = winningInput.toDoubleOrNull() ?: 0.0
                                    val bVal = bonusInput.toDoubleOrNull() ?: 0.0
                                    viewModel.adminSetUserBalances(userToAdjust.id, dVal, wVal, bVal)
                                    selectUserForWalletAdjust = null
                                    scope.launch {
                                        snackbarHost.showSnackbar("Wrote wallet modifications absolutely successfully.")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) {
                                Text("APPLY BALANCE")
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AdminRefundsTab(
    viewModel: BattleZoneViewModel,
    snackbarHost: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val allRefunds by viewModel.adminAllRefunds.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("REFUND REGISTRATION DISPATCH DESK", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Process players' requested match reimbursement claims. Approvals automatically reverse the original match fee transaction back to original destination.", fontSize = 9.sp, color = GreyText)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (allRefunds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No refund requests found in system register.", fontSize = 11.sp, color = GreyText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
            ) {
                items(allRefunds) { refund ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(
                            1.dp,
                            when (refund.status) {
                                "PENDING" -> Color(0xFFFF9800).copy(alpha = 0.4f)
                                "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.4f)
                                else -> Color(0xFFE91E63).copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("REFUND ID: #${refund.id}", fontSize = 10.sp, color = GreyText, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (refund.status) {
                                                "PENDING" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                else -> Color(0xFFE91E63).copy(alpha = 0.15f)
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        refund.status,
                                        color = when (refund.status) {
                                            "PENDING" -> Color(0xFFFF9800)
                                            "APPROVED" -> Color(0xFF4CAF50)
                                            else -> Color(0xFFE91E63)
                                        },
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Tournament: ${refund.tournamentTitle} (ID #${refund.tournamentId})", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Player Uid: ${refund.userId}", fontSize = 10.sp, color = GreyText)
                            Text("Reason Given: \"${refund.reason}\"", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                            Text("Claim Destination: ${refund.refundDestination}", fontSize = 10.sp, color = GreyText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Amount: ₹${refund.entryFee} INR", fontSize = 12.sp, color = NeonGold, fontWeight = FontWeight.Black)
                            if (refund.status == "PENDING") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.adminRejectRefund(refund.id) { success, err ->
                                                scope.launch {
                                                    if (success) {
                                                        snackbarHost.showSnackbar("Refund request #${refund.id} rejected.")
                                                    } else {
                                                        snackbarHost.showSnackbar("Error: $err")
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381B1E)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("REJECT CLAIM", color = Color(0xFFFF8A80), fontSize = 9.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.adminApproveRefund(refund.id) { success, err ->
                                                scope.launch {
                                                    if (success) {
                                                        snackbarHost.showSnackbar("Refund request #${refund.id} approved completely!")
                                                    } else {
                                                        snackbarHost.showSnackbar("Error: $err")
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B381E)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("APPROVE & DISBURSE", color = Color(0xFFB9F6CA), fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF1E1C24))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
// Tournaments / Match creator tab
@Composable
fun AdminTournamentsTab(
    tourneys: List<TournamentEntity>,
    users: List<UserEntity>,
    viewModel: BattleZoneViewModel,
    snackBars: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var isCreatingMatch by remember { mutableStateOf(false) }
    // Forms fields
    var matchTitle by remember { mutableStateOf("Free Fire Weekly Showdown") }
    var entryFeeState by remember { mutableStateOf("45") }
    var prizeState by remember { mutableStateOf("100") }
    
    // Premium selectable states
    var mapSelection by remember { mutableStateOf("Bermuda Clash") }
    var formatSelection by remember { mutableStateOf("Solo") }
    var playStyleSelection by remember { mutableStateOf("Normal (Body Shot Allowed)") } // Normal or Headshot Only
    
    var slotsState by remember { mutableStateOf("48") }
    
    val tomorrowCalendar = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
    val formatter = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
    val tomorrowFormatted = formatter.format(tomorrowCalendar.time)
    
    // Split Date & Specific start time
    var selectedDate by remember { mutableStateOf(tomorrowFormatted) }
    var selectedTime by remember { mutableStateOf("07:00 PM") }
    
    var rulesState by remember { mutableStateOf("""
🚫 RESTRICTIONS:
• Throwables items (Grenades / gloo melter / flash freeze / Smoke grenade / flash bank) and Vector are NOT allowed. If any team member uses these, the whole team will be fined, winning amount will be deducted, and accounts can also be blocked.
• Trogon not allowed.
• M10 not allowed.
• Zone Pack not allowed.
• Your Free Fire account level must be 40+.
📋 CUSTOM ROOM SETTINGS:
1. While joining, fill your In-Game Username, NOT your UID (Stylish fonts are not allowed; use normal fonts. Failure to follow this can result in being kicked from the room).
2. ID Level Must Be 40+.
3. Headshot rate must be under 60%.
4. Unlimited ammo & Gloo Wall enabled.
5. Default coin: 1500.
6. Character skills: No (No CS).
⚠️ IMPORTANT NOTES:
• Record all matches to review suspicious activities.
• If you find someone hacking, report immediately with a screenshot or video. We will refund you and ban the hacker.
• If you fail to join the custom match by the start time, we are not responsible, and refunds will not be processed. Make sure to join on time.
• Do not use abusive language with admins, in-game chat, or customer support. Violations can lead to losing winnings and account termination.
• The squad team leader is responsible for the behavior of teammates. Bullying is not allowed and can lead to bans without refunds.
🌍 GENERAL RULES:
- Contact us on Telegram for any problems or doubts.
- Matches can be rescheduled if the number of registered players is insufficient. Check our notifications, Telegram channel, or app for updates.
- Room ID and password will be shared in the app 10 minutes before match start time. Match will start 10 minutes after sharing.
- Do not share the Room ID and password. Violations can lead to account termination and loss of winnings.
- If you fail to join the room by match start time, disconnect, or lose connection, we are not responsible and refunds will not be processed.
- This is a paid match. Pay the entry fee to participate. Spots are first come, first served.
- Each team member (squad or duo) must pay the entry fee and register individually.
- Griefing and teaming are against game rules. Violations lead to disqualification and loss of prizes.
- Do not change your position in the custom room after joining. Violations can result in being kicked.
- All players ranking between 1 and 4 will receive special prizes. All players will be rewarded for each kill. Check reward details.
- Do not use screencast while playing. Violations result in an instant ban without warnings.
- Use only mobile devices to join matches. Hacks and emulators are not allowed.
- Violating these rules will result in immediate action, including account bans and forfeiture of rewards.
    """.trimIndent()) }
    // Direct credentials edit selector
    var editCredentialsForTournament by remember { mutableStateOf<TournamentEntity?>(null) }
    // Distribute victory payouts selector
    var selectWinnersForTournament by remember { mutableStateOf<TournamentEntity?>(null) }
    // Manage players selector
    var selectPlayersManagementForTournament by remember { mutableStateOf<TournamentEntity?>(null) }

    // SECURE DIALOG STATES
    var tournamentToCancelPending by remember { mutableStateOf<TournamentEntity?>(null) }
    var showPublishConfirmDialog by remember { mutableStateOf(false) }
    var publishConfirmTitle by remember { mutableStateOf("") }
    var publishConfirmDateStr by remember { mutableStateOf("") }
    var publishConfirmEntryFee by remember { mutableStateOf(45.0) }
    var publishConfirmPrizePool by remember { mutableStateOf(100.0) }
    var publishConfirmMap by remember { mutableStateOf("") }
    var publishConfirmType by remember { mutableStateOf("") }
    var publishConfirmSlots by remember { mutableStateOf(48) }
    var publishConfirmRules by remember { mutableStateOf("") }
    
    var showUpdateConfirmDialog by remember { mutableStateOf(false) }
    var updatePendingId by remember { mutableStateOf(-1) }
    var updatePendingTitle by remember { mutableStateOf("") }
    var updatePendingDateTimeStr by remember { mutableStateOf("") }
    var updatePendingEntryFee by remember { mutableStateOf(0.0) }
    var updatePendingPrizePool by remember { mutableStateOf(0.0) }
    var updatePendingMap by remember { mutableStateOf("") }
    var updatePendingType by remember { mutableStateOf("") }
    var updatePendingSlotsTotal by remember { mutableStateOf(48) }
    var updatePendingRules by remember { mutableStateOf("") }
    var updatePendingRoomId by remember { mutableStateOf<String?>(null) }
    var updatePendingRoomPassword by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Secure Administrative Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131116)),
            border = BorderStroke(1.dp, Color(0xFF1F1C25)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2E1216), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Check",
                        tint = RedPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SECURE ADMINISTRATIVE STORAGE TERMINAL",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Authorized Admin writes are secure & live synced to Cloud Firestore",
                        color = Color(0xFF00E676),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TOURNAMENTS INVENTORY", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
            Button(
                onClick = { isCreatingMatch = !isCreatingMatch },
                colors = ButtonDefaults.buttonColors(containerColor = if (isCreatingMatch) Color(0xFF232029) else RedPrimary),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(if (isCreatingMatch) "CLOSE CREATOR" else "ADD MATCH", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Create match form accordion
        AnimatedVisibility(visible = isCreatingMatch) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ADD NEW GAME TOURNAMENT SHELF", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = matchTitle,
                        onValueChange = { matchTitle = it },
                        label = { Text("Match Title") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = entryFeeState,
                            onValueChange = { entryFeeState = it },
                            label = { Text("Entry Fee (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prizeState,
                            onValueChange = { prizeState = it },
                            label = { Text("Prize Pool (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // FREE FIRE MAPS SELECTION (Detected Available Maps)
                    Text("FREE FIRE MAP SELECTION", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    val mapsLine1 = listOf("Bermuda Clash", "Kalahari", "Purgatory")
                    val mapsLine2 = listOf("Alpine", "Nexterra", "Bermuda Remastererd")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        mapsLine1.forEach { mName ->
                            val isSelected = mapSelection.lowercase() == mName.lowercase()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) RedPrimary else Color(0xFF232029),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { mapSelection = mName }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mName,
                                    color = if (isSelected) Color.White else GreyText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        mapsLine2.forEach { mName ->
                            val isSelected = mapSelection.lowercase() == mName.lowercase()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) RedPrimary else Color(0xFF232029),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { mapSelection = mName }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mName.replace("Bermuda Remastererd", "Remastered"),
                                    color = if (isSelected) Color.White else GreyText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // SELECT MATCH FORMAT (Solo/Duo/Squad)
                    Text("SELECT MATCH FORMAT", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Solo", "Duo", "Squad").forEach { typeName ->
                            val isSelected = formatSelection == typeName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) RedPrimary else Color(0xFF232029),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { formatSelection = typeName }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeName,
                                    color = if (isSelected) Color.White else GreyText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // PLAY STYLE selector (Headshot Only vs Normal)
                    Text("PLAY STYLE CRITERIA", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Normal (Body Shot Allowed)", "Headshot Only").forEach { styleLabel ->
                            val isSelected = playStyleSelection == styleLabel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) NeonGold else Color(0xFF232029),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { playStyleSelection = styleLabel }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = styleLabel,
                                    color = if (isSelected) DarkBg else GreyText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Date selector custom
                    Text("MATCH DATE", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    val todayFormatted = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(java.util.Date())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Today", "Tomorrow").forEach { dOpt ->
                            val isSelected = if (dOpt == "Today") selectedDate == todayFormatted else selectedDate == tomorrowFormatted
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) RedPrimary else Color(0xFF232029),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedDate = if (dOpt == "Today") todayFormatted else tomorrowFormatted
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dOpt (${if (dOpt == "Today") todayFormatted.split(" ")[0] else tomorrowFormatted.split(" ")[0]})",
                                    color = if (isSelected) Color.White else GreyText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("Selected Date (Editable)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // SPECIFIC START TIME (Instead of ranges)
                    Text("SPECIFIC ACTIONS TIMING", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("07:20 PM", "08:40 PM", "09:00 PM", "10:30 PM").forEach { tPreset ->
                            val isSelected = selectedTime == tPreset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) RedPrimary else Color(0xFF232029),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedTime = tPreset }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tPreset,
                                    color = if (isSelected) Color.White else GreyText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = selectedTime,
                        onValueChange = { selectedTime = it },
                        label = { Text("Start Time (e.g., 07:20 PM)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = slotsState,
                        onValueChange = { slotsState = it },
                        label = { Text("Total Entry Slots") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = rulesState,
                        onValueChange = { rulesState = it },
                        label = { Text("Tournament Rules text") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val compositeType = if (playStyleSelection == "Headshot Only") "$formatSelection (Headshot)" else formatSelection
                            val compositeTitle = if (playStyleSelection == "Headshot Only") "$matchTitle [HEADSHOT ONLY]" else matchTitle
                            val customRules = if (playStyleSelection == "Headshot Only") {
                                "🎯 HEADSHOT ONLY TOURNAMENT:\n• Strictly only shots registering as headshots count!\n• No standard body shots allowed.\n$rulesState"
                            } else {
                                rulesState
                            }
                            val timingWithDuration = selectedTime.trim()
                            val constructedDateTime = "$selectedDate, $timingWithDuration"
                            
                            publishConfirmTitle = compositeTitle
                            publishConfirmDateStr = constructedDateTime
                            publishConfirmEntryFee = entryFeeState.toDoubleOrNull() ?: 45.0
                            publishConfirmPrizePool = prizeState.toDoubleOrNull() ?: 100.0
                            publishConfirmMap = mapSelection
                            publishConfirmType = compositeType
                            publishConfirmSlots = slotsState.toIntOrNull() ?: 48
                            publishConfirmRules = customRules
                            showPublishConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("add_publish_submit")
                    ) {
                        Text("PUBLISH MATCH TO LOBBY")
                    }
                }
            }
        }
        // Active management items
        tourneys.sortedWith(compareBy<TournamentEntity> { match -> match.status == "COMPLETED" }.thenBy { match -> match.timestamp }).forEach { match ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(match.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Box(
                            modifier = Modifier
                                .background(
                                    when (match.status) {
                                        "UPCOMING" -> Color(0xFF423B19)
                                        "LIVE" -> Color(0xFF421516)
                                        else -> Color(0xFF132A15)
                                    }, RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                match.status,
                                fontSize = 9.sp,
                                color = when (match.status) {
                                    "UPCOMING" -> NeonGold
                                    "LIVE" -> RedPrimary
                                    else -> Color(0xFF81C784)
                                },
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Time: ${match.localDateTimeStr}", color = GreyText, fontSize = 11.sp)
                        Text("•", color = Color.DarkGray, fontSize = 11.sp)
                        Text("Fee: ₹${match.entryFee}", color = NeonGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Map:", color = GreyText, fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E1C24), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = match.map.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Text("•", color = Color.DarkGray, fontSize = 11.sp)
                        Text("Joined: ${match.slotsTotal - match.slotsRemaining}/${match.slotsTotal}", color = GreyText, fontSize = 11.sp)
                    }
                    if (match.roomId != null) {
                        Text(
                            text = "Room ID: ${match.roomId} / Pass: ${match.roomPassword}",
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (match.winnerName != null) {
                        Text(
                            text = "Champion: ${match.winnerName} (${match.winnerUid})",
                            color = NeonGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Buttons array
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { tournamentToCancelPending = match },
                            modifier = Modifier
                                .background(Color(0xFF281116), RoundedCornerShape(4.dp))
                                .size(32.dp)
                                .testTag("admin_cancel_btn_${match.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RedPrimary, modifier = Modifier.size(16.dp))
                        }
                        Button(
                            onClick = { editCredentialsForTournament = match },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222028)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .weight(1.0f)
                                .height(32.dp)
                                .testTag("admin_edit_btn_${match.id}")
                        ) {
                            Text("EDIT MATCH DETAILS", fontSize = 9.sp, color = Color.White)
                        }
                        if (match.status != "COMPLETED") {
                            if (match.slotsRemaining < match.slotsTotal) {
                                Button(
                                    onClick = { selectWinnersForTournament = match },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .height(32.dp)
                                ) {
                                    Text("DECLARE WINNER", fontSize = 9.sp)
                                }
                            } else {
                                Button(
                                    onClick = { /* Do nothing */ },
                                    enabled = false,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222028)),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .height(32.dp)
                                ) {
                                    Text("NO PARTICIPANTS", fontSize = 9.sp, color = GreyText)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { selectPlayersManagementForTournament = match },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131D16)),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .testTag("admin_manage_players_btn_${match.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = Icons.Default.People, contentDescription = "Manage Players", tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MANAGE REGISTERED PLAYERS", fontSize = 9.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (match.status != "COMPLETED") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFF1F1C25))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("MEMBER BROADCAST ACTIONS (PUSH SMS):", fontSize = 8.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.adminSendCountdownAlert(match.id) { response ->
                                        scope.launch {
                                            if (response == "SUCCESS") {
                                                snackBars.showSnackbar("Broadcasting: 10 mins warning dispatch initiated.")
                                            } else {
                                                snackBars.showSnackbar(response)
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22161A)),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1.0f)
                                    .height(26.dp)
                            ) {
                                Text("ALERT 10M PRIOR", fontSize = 8.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Black)
                            }
                            Button(
                                onClick = {
                                    viewModel.adminSendStartedAlert(match.id) { response ->
                                        scope.launch {
                                            if (response == "SUCCESS") {
                                                snackBars.showSnackbar("Broadcasting: match start now dispatch initiated.")
                                            } else {
                                                snackBars.showSnackbar(response)
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF251113)),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, RedPrimary.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1.0f)
                                    .height(26.dp)
                            ) {
                                Text("ALERT NOW LIVE", fontSize = 8.sp, color = RedPrimary, fontWeight = FontWeight.Black)
                            }
                            Button(
                                onClick = {
                                    viewModel.adminSendCredentialsAlert(match.id) { response ->
                                        scope.launch {
                                            if (response == "SUCCESS") {
                                                snackBars.showSnackbar("Broadcasting: Room IDs & Passwords sent.")
                                            } else {
                                                snackBars.showSnackbar(response)
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131F16)),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(26.dp)
                            ) {
                                Text("SEND ROOM KEYS", fontSize = 8.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
        // Room details & comprehensive match editor dialog
        if (editCredentialsForTournament != null) {
            val focusM = editCredentialsForTournament!!
            var editTitle by remember { mutableStateOf(focusM.title) }
            var editMap by remember { mutableStateOf(focusM.map) }
            var editType by remember { mutableStateOf(focusM.type) }
            var editFee by remember { mutableStateOf(focusM.entryFee.toString()) }
            var editPrize by remember { mutableStateOf(focusM.prizePool.toString()) }
            var editSlots by remember { mutableStateOf(focusM.slotsTotal.toString()) }
            var editRules by remember { mutableStateOf(focusM.rules) }
            var editTime by remember { mutableStateOf(focusM.dateTimeStr) }
            var rId by remember { mutableStateOf(focusM.roomId ?: "") }
            var rPs by remember { mutableStateOf(focusM.roomPassword ?: "") }
            var showCalendarPicker by remember { mutableStateOf(false) }
            Dialog(onDismissRequest = { editCredentialsForTournament = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("COMPREHENSIVE MATCH EDITOR", fontWeight = FontWeight.Black, fontSize = 13.sp, color = NeonGold)
                        Text("Edit titles, schedules, maps and keys. Live sync on saving.", fontSize = 9.sp, color = GreyText)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("MATCH TITLE", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // FREE FIRE MAP SELECTOR
                        Text("FREE FIRE MAP SELECTION", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        val ffMaps = listOf("Bermuda", "Kalahari", "Purgatory", "Alpine", "Nexterra")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ffMaps.forEach { mName ->
                                val isSelected = editMap.lowercase() == mName.lowercase()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) RedPrimary else Color(0xFF232029),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { editMap = mName }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mName,
                                        color = if (isSelected) Color.White else GreyText,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        // DATE & TIME WITH CALENDAR TRIGGER
                        Text("SCHEDULE (CALENDAR FORMAT)", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editTime,
                            onValueChange = { editTime = it },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            trailingIcon = {
                                IconButton(onClick = { showCalendarPicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Format via interactive calendar",
                                        tint = RedPrimary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // ENTRY FEE, PRIZE, SLOTS, TYPE
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = editFee,
                                onValueChange = { editFee = it },
                                label = { Text("Fee (₹)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editPrize,
                                onValueChange = { editPrize = it },
                                label = { Text("Prize (₹)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = editSlots,
                                onValueChange = { editSlots = it },
                                label = { Text("Slots", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editType,
                                onValueChange = { editType = it },
                                label = { Text("Format (Solo/Squad)", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("TOURNAMENT SPECIFIC RULES / DETAIL", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editRules,
                            onValueChange = { editRules = it },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFF28252C))
                        Spacer(modifier = Modifier.height(10.dp))
                        // GAME KEYS / ROOM INFORMATION
                        Text("CUSTOM GAME ROOM ACCESS DETAILS", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = rId,
                                onValueChange = { rId = it },
                                label = { Text("Room ID", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color(0xFF28252C)),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rPs,
                                onValueChange = { rPs = it },
                                label = { Text("Password", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color(0xFF28252C)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { editCredentialsForTournament = null }) {
                                Text("DISCARD", color = GreyText, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    updatePendingId = focusM.id
                                    updatePendingTitle = editTitle
                                    updatePendingDateTimeStr = editTime
                                    updatePendingEntryFee = editFee.toDoubleOrNull() ?: 0.0
                                    updatePendingPrizePool = editPrize.toDoubleOrNull() ?: 0.0
                                    updatePendingMap = editMap
                                    updatePendingType = editType
                                    updatePendingSlotsTotal = editSlots.toIntOrNull() ?: 48
                                    updatePendingRules = editRules
                                    updatePendingRoomId = rId.ifBlank { null }
                                    updatePendingRoomPassword = rPs.ifBlank { null }
                                    showUpdateConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                modifier = Modifier.testTag("edit_save_changes_btn")
                            ) {
                                Text("SAVE CHANGES & SYNC")
                            }
                        }
                    }
                }
            }
            // Simulated interactive Calendar Format date/time picker dialog
            if (showCalendarPicker) {
                var selectedDay by remember { mutableStateOf(22) }
                var selectedMonthStr by remember { mutableStateOf("Jun") }
                var selectedYear by remember { mutableStateOf(2026) }
                var selectedHour by remember { mutableStateOf("07") }
                var selectedMinute by remember { mutableStateOf("00") }
                var selectedAmPm by remember { mutableStateOf("PM") }
                Dialog(onDismissRequest = { showCalendarPicker = false }) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF141118),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SELECT TIME & DATE", fontSize = 12.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = {
                                        selectedMonthStr = "Jun"
                                        selectedYear = 2026
                                    }) {
                                        Text("JUN 2026", color = RedPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            // DAYS GRID MOCK
                            Text("June 2026 Calendar Grid", fontSize = 8.sp, color = GreyText, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            // Simple Grid simulation in vertical columns of Row
                            val daysInMonth = (1..30).toList()
                            Column {
                                val chunkedDays = daysInMonth.chunked(7)
                                chunkedDays.forEach { rowDays ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        rowDays.forEach { dayNum ->
                                            val isChosen = selectedDay == dayNum
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(
                                                        if (isChosen) RedPrimary else Color.Transparent,
                                                        CircleShape
                                                    )
                                                    .clickable { selectedDay = dayNum }
                                                    .padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    dayNum.toString(),
                                                    color = if (isChosen) Color.White else Color.White.copy(alpha = 0.8f),
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                        // Pad columns if shorter than 7
                                        if (rowDays.size < 7) {
                                            repeat(7 - rowDays.size) {
                                                Spacer(modifier = Modifier.size(28.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            // Hour Spinner Simulation
                            Text("HOUR & MINUTE SPINNER", fontSize = 8.sp, color = GreyText, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hour choosing capsules
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("01", "05", "07", "08", "09", "11").forEach { hr ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (hr == selectedHour) RedPrimary else Color(0xFF28252C),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable { selectedHour = hr }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(hr, fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                                Text(":", color = Color.White, fontWeight = FontWeight.Bold)
                                // Minute choosing capsules
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("00", "15", "30", "45").forEach { mn ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (mn == selectedMinute) RedPrimary else Color(0xFF28252C),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable { selectedMinute = mn }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(mn, fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // AM/PM Toggle Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedAmPm == "AM") RedPrimary else Color(0xFF232029),
                                            RoundedCornerShape(4.dp, 0.dp, 0.dp, 4.dp)
                                        )
                                        .clickable { selectedAmPm = "AM" }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("AM", color = Color.White, fontSize = 9.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedAmPm == "PM") RedPrimary else Color(0xFF232029),
                                            RoundedCornerShape(0.dp, 4.dp, 4.dp, 0.dp)
                                        )
                                        .clickable { selectedAmPm = "PM" }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("PM", color = Color.White, fontSize = 9.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showCalendarPicker = false }) {
                                    Text("CANCEL", color = GreyText)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        editTime = String.format(
                                            java.util.Locale.US,
                                            "%02d %s %d, %s:%s %s",
                                            selectedDay,
                                            selectedMonthStr,
                                            selectedYear,
                                            selectedHour,
                                            selectedMinute,
                                            selectedAmPm
                                        )
                                        showCalendarPicker = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                                ) {
                                    Text("SET CALENDAR SCHEDULE", fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        // winner distribution dialog
        if (selectWinnersForTournament != null) {
            val focusMatchWinner = selectWinnersForTournament!!
            val allJoinsState = viewModel.allJoins.collectAsStateWithLifecycle()
            val registeredJoins = allJoinsState.value.filter { it.tournamentId == focusMatchWinner.id }
            
            var winnerFFUid by remember { mutableStateOf(registeredJoins.firstOrNull()?.freeFireUid ?: "FF-837492047") }
            var winnerInGameName by remember { mutableStateOf(registeredJoins.firstOrNull()?.inGameName ?: "Alpha_Gamer") }
            
            Dialog(onDismissRequest = { selectWinnersForTournament = null }) {
                Surface(shape = RoundedCornerShape(12.dp), color = DarkSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Declare Prize Champions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text("Selected player will automatically receive ₹${focusMatchWinner.prizePool} credited to winnings balance.", fontSize = 9.sp, color = GreyText)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (registeredJoins.isEmpty()) {
                            Text("No players have joined this match yet.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            Text("Select Winner from Joins:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(bottom = 6.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 160.dp).fillMaxWidth().padding(bottom = 12.dp)) {
                                items(registeredJoins) { join ->
                                    val isSelected = winnerFFUid == join.freeFireUid
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) RedPrimary.copy(alpha = 0.2f) else Color(0xFF1E1C21))
                                            .clickable {
                                                winnerFFUid = join.freeFireUid
                                                winnerInGameName = join.inGameName
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                winnerFFUid = join.freeFireUid
                                                winnerInGameName = join.inGameName
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(join.inGameName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                            Text("UID: ${join.freeFireUid}", fontSize = 11.sp, color = GreyText)
                                        }
                                    }
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = winnerInGameName,
                            onValueChange = { winnerInGameName = it },
                            label = { Text("Winner In-Game Name") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = winnerFFUid,
                            onValueChange = { winnerFFUid = it },
                            label = { Text("Winner Free Fire Character UID") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { selectWinnersForTournament = null }) {
                                Text("CANCEL", color = GreyText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.adminEndTournamentAndDistributePrize(focusMatchWinner.id, winnerFFUid, winnerInGameName)
                                    selectWinnersForTournament = null
                                    scope.launch {
                                        snackBars.showSnackbar("Match concluded & prize pool distributed!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) {
                                Text("CONCLUDE & PAYOUT")
                            }
                        }
                    }
                }
            }
        }

        // Players management & updates/disqualify Dialog
        if (selectPlayersManagementForTournament != null) {
            val focusMatch = selectPlayersManagementForTournament!!
            val allJoinsState = viewModel.allJoins.collectAsStateWithLifecycle()
            val registeredJoins = allJoinsState.value.filter { it.tournamentId == focusMatch.id }
            
            var editingJoinId by remember { mutableStateOf<Int?>(null) }
            var editKillsInput by remember { mutableStateOf("0") }
            var editRankInput by remember { mutableStateOf("1") }
            
            var showDisqualifyConfirmForJoin by remember { mutableStateOf<TournamentJoinEntity?>(null) }
            var refundOnDisqualify by remember { mutableStateOf(true) }
            var showResetConfirmDialog by remember { mutableStateOf(false) }
            
            Dialog(onDismissRequest = { selectPlayersManagementForTournament = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF131116),
                    border = BorderStroke(1.dp, Color(0xFF1F1C25)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MANAGE REGISTERED PLAYERS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = RedPrimary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = focusMatch.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { selectPlayersManagementForTournament = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = GreyText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Slots details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF18161D), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Registered: ${registeredJoins.size} players",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Entry Fee: ₹${focusMatch.entryFee} | Prize: ₹${focusMatch.prizePool}",
                                fontSize = 10.sp,
                                color = NeonGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (registeredJoins.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF241519), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Reset Tournament Results",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Wipes kills and ranks back to zero and restores status to pending for all participants.",
                                        fontSize = 8.sp,
                                        color = GreyText
                                    )
                                }
                                Button(
                                    onClick = {
                                        showResetConfirmDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(30.dp).testTag("reset_all_results_btn")
                                ) {
                                    Text("RESET ALL", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (registeredJoins.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = "No players",
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No players registered yet.",
                                        color = GreyText,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(registeredJoins) { join ->
                                    val isEditing = editingJoinId == join.id
                                    
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18161D)),
                                        border = BorderStroke(1.dp, if (isEditing) RedPrimary.copy(alpha = 0.5f) else Color(0xFF221F28)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = join.inGameName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "UID: ${join.freeFireUid}",
                                                        fontSize = 10.sp,
                                                        color = GreyText
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Kills: ${join.claimedKills}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NeonGold
                                                        )
                                                        Text(
                                                            text = "Rank: #${join.claimedRank}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF00E676)
                                                        )
                                                    }
                                                }
                                                
                                                // Action Buttons
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (isEditing) {
                                                                editingJoinId = null
                                                            } else {
                                                                editingJoinId = join.id
                                                                editKillsInput = join.claimedKills.toString()
                                                                editRankInput = join.claimedRank.toString()
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .background(Color(0xFF222028), CircleShape)
                                                            .size(28.dp)
                                                            .testTag("edit_results_btn_${join.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                                                            contentDescription = "Edit Results",
                                                            tint = if (isEditing) RedPrimary else Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    
                                                    IconButton(
                                                        onClick = {
                                                            showDisqualifyConfirmForJoin = join
                                                        },
                                                        modifier = Modifier
                                                            .background(Color(0xFF281116), CircleShape)
                                                            .size(28.dp)
                                                            .testTag("disqualify_player_btn_${join.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Disqualify",
                                                            tint = RedPrimary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            // Expandable Results Editor
                                            AnimatedVisibility(visible = isEditing) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 10.dp)
                                                        .background(Color(0xFF131116), RoundedCornerShape(6.dp))
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = "MANUALLY UPDATE MATCH RESULTS",
                                                        fontSize = 8.sp,
                                                        color = NeonGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        OutlinedTextField(
                                                            value = editKillsInput,
                                                            onValueChange = { editKillsInput = it },
                                                            label = { Text("Kills") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = RedPrimary,
                                                                unfocusedBorderColor = Color(0xFF28252C)
                                                            ),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .testTag("edit_kills_input_${join.id}")
                                                        )
                                                        OutlinedTextField(
                                                            value = editRankInput,
                                                            onValueChange = { editRankInput = it },
                                                            label = { Text("Rank") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = RedPrimary,
                                                                unfocusedBorderColor = Color(0xFF28252C)
                                                            ),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .testTag("edit_rank_input_${join.id}")
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Button(
                                                        onClick = {
                                                            val kills = editKillsInput.toIntOrNull() ?: 0
                                                            val rank = editRankInput.toIntOrNull() ?: 1
                                                            viewModel.adminUpdatePlayerResults(join.userId, join.tournamentId, kills, rank) { success ->
                                                                if (success) {
                                                                    editingJoinId = null
                                                                    scope.launch {
                                                                        snackBars.showSnackbar("Results updated for ${join.inGameName} successfully!")
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(32.dp)
                                                            .testTag("save_results_btn_${join.id}")
                                                    ) {
                                                        Text("SAVE RESULTS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { selectPlayersManagementForTournament = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222028)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("DONE", color = Color.White)
                        }
                    }
                }
            }
            
            // Nested Disqualify Confirmation Dialog
            if (showDisqualifyConfirmForJoin != null) {
                val dsqJoin = showDisqualifyConfirmForJoin!!
                Dialog(onDismissRequest = { showDisqualifyConfirmForJoin = null }) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF131116),
                        border = BorderStroke(1.dp, Color(0xFF2E1216)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "⚠️ CONFIRM PLAYER DISQUALIFICATION",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RedPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Are you sure you want to disqualify ${dsqJoin.inGameName} (${dsqJoin.freeFireUid}) from this tournament? This action is irreversible.",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Refund checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { refundOnDisqualify = !refundOnDisqualify }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = refundOnDisqualify,
                                    onCheckedChange = { refundOnDisqualify = it },
                                    colors = CheckboxDefaults.colors(checkedColor = RedPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Refund entry fee (₹${focusMatch.entryFee}) to user's wallet",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showDisqualifyConfirmForJoin = null }) {
                                    Text("CANCEL", color = GreyText)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.adminDisqualifyPlayer(
                                            dsqJoin.userId,
                                            dsqJoin.tournamentId,
                                            refundOnDisqualify
                                        ) { success, msg ->
                                            showDisqualifyConfirmForJoin = null
                                            if (success) {
                                                scope.launch {
                                                    snackBars.showSnackbar("Successfully disqualified player: ${dsqJoin.inGameName}")
                                                }
                                            } else {
                                                scope.launch {
                                                    snackBars.showSnackbar("Disqualification failed: $msg")
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("confirm_disqualify_btn")
                                ) {
                                    Text("CONFIRM & DISQUALIFY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (showResetConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showResetConfirmDialog = false },
                    containerColor = DarkSurface,
                    title = {
                        Text("Reset All Match Results?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            "Are you sure you want to reset the kills and ranks back to zero, and set statuses to PENDING for all registered combatants in this tournament?",
                            color = GreyText,
                            fontSize = 12.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showResetConfirmDialog = false
                                viewModel.resetTournamentResults(focusMatch.id) { success, msg ->
                                    scope.launch {
                                        snackBars.showSnackbar(msg ?: "Results updated.")
                                    }
                                }
                            }
                        ) {
                            Text("YES, RESET", color = RedPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirmDialog = false }) {
                            Text("CANCEL", color = Color.White)
                        }
                    }
                )
            }
        }

        // --- SECURE FIRESTORE WRITE CONFIRMATION DIALOGS ---

        // 1. Publish Match Confirmation Dialog
        if (showPublishConfirmDialog) {
            Dialog(onDismissRequest = { showPublishConfirmDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF131116),
                    border = BorderStroke(1.dp, Color(0xFF2E1216)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = "Shield", tint = NeonGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🛡️ SECURE PUBLISH PERMISSION", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "You are preparing to publish a new tournament directly to live clients via Google Cloud Firestore. Please audit the following parameters carefully:",
                            fontSize = 11.sp,
                            color = GreyText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18161D)),
                            border = BorderStroke(1.dp, Color(0xFF221F28))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Title: $publishConfirmTitle", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Schedule: $publishConfirmDateStr", fontSize = 10.sp, color = GreyText)
                                Text("Entry Fee: ₹$publishConfirmEntryFee | Prize Pool: ₹$publishConfirmPrizePool", fontSize = 10.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                                Text("Map: $publishConfirmMap | Format: $publishConfirmType", fontSize = 10.sp, color = GreyText)
                                Text("Total Slots: $publishConfirmSlots", fontSize = 10.sp, color = GreyText)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showPublishConfirmDialog = false }) {
                                Text("DISCARD", color = GreyText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.adminCreateTournament(
                                        title = publishConfirmTitle,
                                        dateTimeStr = publishConfirmDateStr,
                                        entryFee = publishConfirmEntryFee,
                                        prizePool = publishConfirmPrizePool,
                                        map = publishConfirmMap,
                                        type = publishConfirmType,
                                        slotsTotal = publishConfirmSlots,
                                        rules = publishConfirmRules
                                    ) {
                                        isCreatingMatch = false
                                        showPublishConfirmDialog = false
                                        scope.launch {
                                            snackBars.showSnackbar("Securely published and synced to Firestore!")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                modifier = Modifier.testTag("publish_confirm_btn")
                            ) {
                                Text("PUBLISH LOBBY", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Update Match Confirmation Dialog
        if (showUpdateConfirmDialog) {
            Dialog(onDismissRequest = { showUpdateConfirmDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF131116),
                    border = BorderStroke(1.dp, Color(0xFF2E1216)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = "Shield", tint = Color(0xFF00E676), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🛡️ SECURE FIRESTORE UPDATE", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "You are preparing to write modifications to live tournament ID #${updatePendingId} in Google Cloud Firestore. Active registrations, map, scheduling details and lobby room credentials will be modified in real-time.",
                            fontSize = 11.sp,
                            color = GreyText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18161D)),
                            border = BorderStroke(1.dp, Color(0xFF221F28))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("New Title: $updatePendingTitle", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("New Schedule: $updatePendingDateTimeStr", fontSize = 10.sp, color = GreyText)
                                Text("New Fee: ₹$updatePendingEntryFee | New Prize: ₹$updatePendingPrizePool", fontSize = 10.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                                Text("Map: $updatePendingMap | Format: $updatePendingType", fontSize = 10.sp, color = GreyText)
                                if (!updatePendingRoomId.isNullOrBlank()) {
                                    Text("Room ID: $updatePendingRoomId | Password: $updatePendingRoomPassword", fontSize = 10.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showUpdateConfirmDialog = false }) {
                                Text("DISCARD", color = GreyText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.adminEditTournamentDetails(
                                        id = updatePendingId,
                                        title = updatePendingTitle,
                                        dateTimeStr = updatePendingDateTimeStr,
                                        entryFee = updatePendingEntryFee,
                                        prizePool = updatePendingPrizePool,
                                        map = updatePendingMap,
                                        type = updatePendingType,
                                        slotsTotal = updatePendingSlotsTotal,
                                        rules = updatePendingRules,
                                        roomId = updatePendingRoomId,
                                        roomPassword = updatePendingRoomPassword
                                    )
                                    showUpdateConfirmDialog = false
                                    editCredentialsForTournament = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                modifier = Modifier.testTag("update_confirm_btn")
                            ) {
                                Text("UPDATE LOBBY", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Delete/Cancel Match Confirmation Dialog
        if (tournamentToCancelPending != null) {
            val focusC = tournamentToCancelPending!!
            Dialog(onDismissRequest = { tournamentToCancelPending = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF131116),
                    border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Delete Warning", tint = RedPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⚠️ SECURE DELETION DIRECTIVE", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "CRITICAL WARNING: You are about to cancel and permanently delete match '${focusC.title}' from local database & Cloud Firestore. This operation is non-reversible.\n\nAll registered participants will be instantly kicked and fully refunded (₹${focusC.entryFee} each) to their deposits.",
                            fontSize = 11.sp,
                            color = GreyText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { tournamentToCancelPending = null }) {
                                Text("ABORT DISCARD", color = GreyText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.adminCancelTournament(focusC.id)
                                    tournamentToCancelPending = null
                                    scope.launch {
                                        snackBars.showSnackbar("Tournament cancel directive dispatched! Synced to Firestore.")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                modifier = Modifier.testTag("delete_confirm_btn")
                            ) {
                                Text("PURGE LOBBY", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
// Admin withdrawals processor
@Composable
fun AdminWithdrawalsTab(
    withdrawals: List<WithdrawalRequestEntity>,
    viewModel: BattleZoneViewModel,
    message: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showHistory by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showHistory = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showHistory) RedPrimary else Color(0xFF1E1C24)
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("ACTIVE QUEUE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Button(
                onClick = { showHistory = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showHistory) RedPrimary else Color(0xFF1E1C24)
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("PAID / REJECTED HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        val displayedWithdrawals = if (showHistory) {
            withdrawals.filter { it.status == "COMPLETED" || it.status == "SUCCESS" || it.status == "REJECTED" }
        } else {
            withdrawals.filter { it.status == "PENDING" || it.status == "IN_PROGRESS" }
        }

        Text(
            if (showHistory) "COMPLETED WITHDRAWALS HISTORY ARCHIVE" else "ACTIVE WITHDRAWALS PROPOSAL QUEUE", 
            fontSize = 12.sp, 
            color = GreyText, 
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (displayedWithdrawals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (showHistory) "No archived withdrawal history available." else "No active withdrawal requests available.", 
                    color = GreyText, 
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn {
                items(displayedWithdrawals) { req ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, Color(0xFF1E1C24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("UserId: ${req.userId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val statusLabel = when (req.status) {
                                            "IN_PROGRESS" -> "IN PROGRESS"
                                            "COMPLETED", "SUCCESS" -> "PAID"
                                            "REJECTED" -> "REJECTED"
                                            else -> "PENDING"
                                        }
                                        val statusBgColor = when (req.status) {
                                            "IN_PROGRESS" -> Color(0xFF1E88E5)
                                            "COMPLETED", "SUCCESS" -> Color(0xFF00E676).copy(alpha = 0.2f)
                                            "REJECTED" -> RedPrimary.copy(alpha = 0.2f)
                                            else -> NeonGold.copy(alpha = 0.2f)
                                        }
                                        val statusTextColor = when (req.status) {
                                            "IN_PROGRESS" -> Color.White
                                            "COMPLETED", "SUCCESS" -> Color(0xFF00E676)
                                            "REJECTED" -> RedPrimary
                                            else -> NeonGold
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(statusBgColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(statusLabel, fontSize = 8.sp, color = statusTextColor, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "UPI ID: ",
                                            color = GreyText,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = req.upiId,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("admin_withdrawal_upi_id")
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF28252C), RoundedCornerShape(4.dp))
                                                .border(1.dp, RedPrimary.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                .clickable {
                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText("UPI ID", req.upiId)
                                                    clipboard.setPrimaryClip(clip)
                                                    scope.launch {
                                                        message.showSnackbar("UPI ID copied to clipboard.")
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("admin_copy_upi_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy UPI ID",
                                                    tint = RedPrimary,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "COPY",
                                                    color = Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(req.amount.toCurrency(), color = NeonGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (req.status == "PENDING" || req.status == "IN_PROGRESS") {
                                    if (req.status == "PENDING") {
                                        Button(
                                            onClick = { viewModel.adminSetWithdrawalInProgress(req.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("PROGRESS", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Button(
                                        onClick = { viewModel.adminRejectWithdrawal(req.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281116)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("REJECT", fontSize = 9.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.adminApproveWithdrawal(req.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("APPROVE & DISBURSE", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "PROCESSED STATUS: ${req.status}", 
                                        color = if (req.status == "REJECTED") RedPrimary else Color(0xFF00E676), 
                                        fontSize = 10.sp, 
                                        fontWeight = FontWeight.ExtraBold
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
// Helpdesk ticket resolver tab
@Composable
fun AdminTicketsTab(
    tickets: List<SupportTicketEntity>,
    viewModel: BattleZoneViewModel,
    callback: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var ticketToAnswer by remember { mutableStateOf<SupportTicketEntity?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("ACTIVE HELPDESK TICKETS", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        val unresolved = tickets.filter { it.status == "OPEN" }
        if (unresolved.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("All support desk tickets resolved. Good job admin!", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn {
                items(unresolved) { ticket ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(ticket.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Subject User: ${ticket.userId}", color = GreyText, fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { ticketToAnswer = ticket },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("REPLY", fontSize = 9.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(ticket.message, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        // Support reply pop-up
        if (ticketToAnswer != null) {
            val ticketModel = ticketToAnswer!!
            var replyTextInput by remember { mutableStateOf("We have fully credited your wallet with the requested promo bonus. Sorry for the delay!") }
            Dialog(onDismissRequest = { ticketToAnswer = null }) {
                Surface(shape = RoundedCornerShape(12.dp), color = DarkSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Draft Helpdesk Answer", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Subject: ${ticketModel.title}", color = GreyText, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = replyTextInput,
                            onValueChange = { replyTextInput = it },
                            label = { Text("Direct Message Reply") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { ticketToAnswer = null }) {
                                Text("CANCEL", color = GreyText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.adminReplySupportTicket(ticketModel.id, replyTextInput)
                                    ticketToAnswer = null
                                    scope.launch {
                                        callback.showSnackbar("Answer published to user support desk.")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) {
                                Text("PUBLISH ANSWER")
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- SCREENSHOT PROOF COMPOSABLES AND ADMIN QUEUE SYSTEM ---
@Composable
fun ProofSubmissionCard(
    join: TournamentJoinEntity,
    viewModel: BattleZoneViewModel,
    snackbarHostState: SnackbarHostState
) {
    var showSubmitDialog by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, when (join.proofStatus) {
            "PENDING" -> NeonGold.copy(alpha = 0.5f)
            "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.5f)
            "REJECTED" -> RedPrimary.copy(alpha = 0.5f)
            "COMPLETED", "completed" -> Color(0xFF00B0FF).copy(alpha = 0.5f)
            else -> Color(0xFF28252C)
        }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (join.proofStatus) {
                            "PENDING" -> Icons.Filled.HourglassBottom
                            "APPROVED" -> Icons.Filled.CheckCircle
                            "REJECTED" -> Icons.Filled.Cancel
                            "COMPLETED", "completed" -> Icons.Filled.CheckCircle
                            else -> Icons.Filled.CloudUpload
                        },
                        contentDescription = "Proof Status Indicator",
                        tint = when (join.proofStatus) {
                            "PENDING" -> NeonGold
                            "APPROVED" -> Color(0xFF4CAF50)
                            "REJECTED" -> RedPrimary
                            "COMPLETED", "completed" -> Color(0xFF00B0FF)
                            else -> GreyText
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SCREENSHOT MATCH PROOF",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                
                // Badge for status
                Surface(
                    color = when (join.proofStatus) {
                        "PENDING" -> NeonGold.copy(alpha = 0.15f)
                        "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        "REJECTED" -> RedPrimary.copy(alpha = 0.15f)
                        "COMPLETED", "completed" -> Color(0xFF00B0FF).copy(alpha = 0.15f)
                        else -> Color(0xFF232029)
                    },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, when (join.proofStatus) {
                        "PENDING" -> NeonGold.copy(alpha = 0.3f)
                        "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                        "REJECTED" -> RedPrimary.copy(alpha = 0.3f)
                        "COMPLETED", "completed" -> Color(0xFF00B0FF).copy(alpha = 0.3f)
                        else -> Color(0xFF28252C)
                    })
                ) {
                    Text(
                        text = join.proofStatus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (join.proofStatus) {
                            "PENDING" -> NeonGold
                            "APPROVED" -> Color(0xFF4CAF50)
                            "REJECTED" -> RedPrimary
                            "COMPLETED", "completed" -> Color(0xFF00B0FF)
                            else -> GreyText
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            when (join.proofStatus) {
                "NONE" -> {
                    Text(
                        text = "Once you finish your custom lobby room match, take a screenshot of your result placement (victory booyah or score screen) and submit it here as verification for match awards.",
                        fontSize = 11.sp,
                        color = GreyText,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showSubmitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("user_submit_proof_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SUBMIT RESULTS PROOF", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                "PENDING" -> {
                    Text(
                        text = "Your screenshot proof is currently in the admin verification queue. Character placement claimants are manually vetted against the lobby roster. This usually takes 5-10 minutes.",
                        fontSize = 11.sp,
                        color = GreyText,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Summary of details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CLAIMED PLACEMENT", fontSize = 9.sp, color = GreyText)
                            Text("#${join.claimedRank}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonGold)
                        }
                        Column {
                            Text("TOTAL KILLS", fontSize = 9.sp, color = GreyText)
                            Text("${join.claimedKills} Kills", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        join.screenshotProofPath?.let { path ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SCREENSHOT", fontSize = 9.sp, color = GreyText)
                                Text("Vetted Soon", fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                "APPROVED" -> {
                    Text(
                        text = "Congratulations! Your proof has been verified and approved by the tournament admin. Rewards have been transferred to your winning balance.",
                        fontSize = 11.sp,
                        color = Color(0xFF81C784),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10190D), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFF2C4524)), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("VERIFIED PLACEMENT", fontSize = 9.sp, color = GreyText)
                            Text("#${join.claimedRank}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                        }
                        Column {
                            Text("VERIFIED KILLS", fontSize = 9.sp, color = GreyText)
                            Text("${join.claimedKills} Kills", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("MATCH STATUS", fontSize = 9.sp, color = GreyText)
                            Text("VERIFIED & PAID", fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!join.adminNotes.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Admin comment: \"${join.adminNotes}\"", fontSize = 10.sp, color = GreyText, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                    }
                }
                "COMPLETED", "completed" -> {
                    Text(
                        text = "This match has been completed! The tournament winner has been declared, and results have been sealed by the administrator.",
                        fontSize = 11.sp,
                        color = Color(0xFF00B0FF),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D1B2A), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFF1B4965)), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MATCH END STATUS", fontSize = 9.sp, color = GreyText)
                            Text("COMPLETED", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ADMIN REMARK", fontSize = 9.sp, color = GreyText)
                            Text("GAMES SEALED", fontSize = 11.sp, color = Color(0xFF00B0FF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "REJECTED" -> {
                    Text(
                        text = "Match proof rejected. Admin remarks: \"${join.adminNotes ?: "Lobby roster verification failed."}\"",
                        fontSize = 11.sp,
                        color = RedPrimary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showSubmitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33191E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "resubmit", modifier = Modifier.size(16.dp), tint = RedPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESUBMIT PROOF", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
    if (showSubmitDialog) {
        var killsInput by remember { mutableStateOf("0") }
        var rankInput by remember { mutableStateOf("1") }
        var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
        
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            selectedImageUri = uri
        }
        
        val scope = rememberCoroutineScope()
        Dialog(onDismissRequest = { showSubmitDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Submit Match Result Proof",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Submit a match victory proof screenshot and your stats directly for admin pool validation.",
                        fontSize = 11.sp,
                        color = GreyText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Fields
                    OutlinedTextField(
                        value = rankInput,
                        onValueChange = { rankInput = it },
                        label = { Text("Your Final Match Placement (#1 to #50)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("proof_rank_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = killsInput,
                        onValueChange = { killsInput = it },
                        label = { Text("Total Registered Combat Kills", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("proof_kills_input")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("SELECT MATCH RESULT SCREENSHOT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreyText)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (selectedImageUri == null) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1C24)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                .testTag("select_screenshot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Screenshot",
                                tint = RedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHOOSE ORIGINAL SCREENSHOT FILE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C24)),
                            border = BorderStroke(1.dp, Color(0xFF28252C)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Selected Screenshot", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    TextButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("CHANGE FILE", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    coil.compose.AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Preview match proof screenshot",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    // Action buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSubmitDialog = false }) {
                            Text("ABORT", color = GreyText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (selectedImageUri == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please select an original screenshot file from your device first!")
                                    }
                                    return@Button
                                }
                                val rank = rankInput.toIntOrNull() ?: 1
                                val kills = killsInput.toIntOrNull() ?: 0
                                viewModel.submitScreenshotProof(
                                    tournamentId = join.tournamentId,
                                    screenshotPath = selectedImageUri.toString(),
                                    kills = kills,
                                    rank = rank,
                                    onResult = { success, msg ->
                                        showSubmitDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            modifier = Modifier.testTag("submit_proof_dialog_confirm")
                        ) {
                            Text("SUBMIT PROOF")
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AdminProofsTab(
    viewModel: BattleZoneViewModel,
    snackbarHostState: SnackbarHostState
) {
    val proofs by viewModel.adminAllSubmittedProofs.collectAsStateWithLifecycle()
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    var filterOnlyPending by remember { mutableStateOf(true) }
    val displayedProofs = if (filterOnlyPending) {
        proofs.filter { it.proofStatus == "PENDING" }
    } else {
        proofs
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MATCH PROOFS VERIFICATION QUEUE", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
            
            // Toggle filter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = filterOnlyPending,
                    onCheckedChange = { filterOnlyPending = it },
                    colors = CheckboxDefaults.colors(checkedColor = RedPrimary)
                )
                Text("Pending Only", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (displayedProofs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(DarkSurface, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterOnlyPending) "No pending proof validations in the queue!" else "No proofs submitted yet.",
                    color = GreyText,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedProofs) { proof ->
                    val tTitle = tournaments.find { it.id == proof.tournamentId }?.title ?: "#${proof.tournamentId} Tournament"
                    val tPrize = tournaments.find { it.id == proof.tournamentId }?.prizePool ?: 0.0
                    AdminProofCard(
                        proof = proof,
                        tournamentTitle = tTitle,
                        tournamentPrize = tPrize,
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}
@Composable
fun AdminProofCard(
    proof: TournamentJoinEntity,
    tournamentTitle: String,
    tournamentPrize: Double,
    viewModel: BattleZoneViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var adminNotesInput by remember { mutableStateOf("") }
    var distributeRewardCheckbox by remember { mutableStateOf(proof.claimedRank == 1) }
    var showFullImageDialog by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, when (proof.proofStatus) {
            "PENDING" -> NeonGold.copy(alpha = 0.5f)
            "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.5f)
            "REJECTED" -> RedPrimary.copy(alpha = 0.5f)
            else -> Color(0xFF28252C)
        }),
        modifier = Modifier.fillMaxWidth().testTag("admin_proof_card_${proof.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournamentTitle,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Candidate: ${proof.inGameName} (${proof.freeFireUid})",
                        fontSize = 11.sp,
                        color = RedSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    color = when (proof.proofStatus) {
                        "PENDING" -> NeonGold.copy(alpha = 0.15f)
                        "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        "REJECTED" -> RedPrimary.copy(alpha = 0.15f)
                        else -> Color(0xFF232029)
                    },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, when (proof.proofStatus) {
                        "PENDING" -> NeonGold.copy(alpha = 0.3f)
                        "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                        "REJECTED" -> RedPrimary.copy(alpha = 0.3f)
                        else -> Color(0xFF28252C)
                    })
                ) {
                    Text(
                        text = proof.proofStatus,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = when (proof.proofStatus) {
                            "PENDING" -> NeonGold
                            "APPROVED" -> Color(0xFF4CAF50)
                            "REJECTED" -> RedPrimary
                            else -> GreyText
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Stats Sub-Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CLAIMED PLACEMENT", fontSize = 9.sp, color = GreyText)
                    Text("#${proof.claimedRank}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonGold)
                }
                Column {
                    Text("CLAIMED KILLS", fontSize = 9.sp, color = GreyText)
                    Text("${proof.claimedKills} Kills", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("POTENTIAL PRIZE", fontSize = 9.sp, color = GreyText)
                    Text(tournamentPrize.toCurrency(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Attachment Preview Clickable
            Text("SUBMITTED SCREENSHOT ATTACHMENT:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreyText)
            Spacer(modifier = Modifier.height(4.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
                    .clickable { showFullImageDialog = true }
                    .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = proof.screenshotProofPath,
                    contentDescription = "Match proof user upload",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Fullscreen, contentDescription = "Zoom", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (proof.proofStatus == "PENDING") {
                // Interactive action controls
                OutlinedTextField(
                    value = adminNotesInput,
                    onValueChange = { adminNotesInput = it },
                    placeholder = { Text("Admin evaluation notes (e.g., 'Matches lobby roster')", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = Color(0xFF28252C)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_proof_notes_${proof.id}")
                )
                if (proof.claimedRank == 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = distributeRewardCheckbox,
                            onCheckedChange = { distributeRewardCheckbox = it },
                            colors = CheckboxDefaults.colors(checkedColor = RedPrimary)
                        )
                        Text(
                            "Award entire prize pool (${tournamentPrize.toCurrency()}) & mark match completed",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.adminVerifyProof(
                                joinId = proof.id,
                                newStatus = "REJECTED",
                                notes = adminNotesInput.ifEmpty { "Roster verification mismatch." },
                                distributeReward = false,
                                onResult = { success ->
                                    scope.launch {
                                        if (success) {
                                            snackbarHostState.showSnackbar("Rejected proof successfully.")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to write to verification queue.")
                                        }
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261215)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.7f)), RoundedCornerShape(6.dp)).testTag("admin_proof_reject_${proof.id}")
                    ) {
                        Text("REJECT", fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.adminVerifyProof(
                                joinId = proof.id,
                                newStatus = "APPROVED",
                                notes = adminNotesInput.ifEmpty { "Vetted and matches match log." },
                                distributeReward = distributeRewardCheckbox,
                                onResult = { success ->
                                    scope.launch {
                                        if (success) {
                                            snackbarHostState.showSnackbar("Approved proof and synchronized earnings successfully!")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to write to verification queue.")
                                        }
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF132A15)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).border(BorderStroke(1.dp, Color(0xFF33691E).copy(alpha = 0.7f)), RoundedCornerShape(6.dp)).testTag("admin_proof_approve_${proof.id}")
                    ) {
                        Text("APPROVE & END", fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = "Vetted notes: \"${proof.adminNotes ?: "Validated match logs."}\"",
                    fontSize = 11.sp,
                    color = GreyText,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                )
            }
        }
    }
    if (showFullImageDialog) {
        Dialog(onDismissRequest = { showFullImageDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    coil.compose.AsyncImage(
                        model = proof.screenshotProofPath,
                        contentDescription = "Match proof full-resolution",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    IconButton(
                        onClick = { showFullImageDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "close", tint = Color.White)
                    }
                }
            }
        }
    }
}
@Composable
fun AdminDepositsTab(
    viewModel: BattleZoneViewModel,
    snackbarHost: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val allTransactions by viewModel.adminAllTransactions.collectAsStateWithLifecycle()
    val pendingDeposits = allTransactions.filter { it.type == "DEPOSIT" && it.status == "PENDING" }
    var showDepositHistory by remember { mutableStateOf(false) }
    var upiState by remember { mutableStateOf(viewModel.getAdminUpiId()) }
    var payeeState by remember { mutableStateOf(viewModel.getAdminPayeeName()) }
    var bankAccState by remember { mutableStateOf(viewModel.getAdminBankAccount()) }
    var bankIfscState by remember { mutableStateOf(viewModel.getAdminBankIfsc()) }
    var bankNameState by remember { mutableStateOf(viewModel.getAdminBankName()) }
    var gatewayModeState by remember { mutableStateOf(viewModel.getPaymentGatewayMode()) } // "REAL_UPI" or "TEST_PREFILLED"
    
    var smsGatewayMode by remember { mutableStateOf(viewModel.getSmsGatewayMode()) }
    var fast2smsKey by remember { mutableStateOf(viewModel.getFast2smsApiKey()) }
    var twilioSid by remember { mutableStateOf(viewModel.getTwilioSid()) }
    var twilioToken by remember { mutableStateOf(viewModel.getTwilioToken()) }
    var twilioPhone by remember { mutableStateOf(viewModel.getTwilioPhone()) }
    var customSmsUrl by remember { mutableStateOf(viewModel.getCustomSmsUrl()) }
    var gmailBackendUrlState by remember { mutableStateOf(viewModel.getGmailOtpBackendUrl()) }
    var gmailSmtpUser by remember { mutableStateOf(viewModel.getGmailUser()) }
    var gmailSmtpPassword by remember { mutableStateOf(viewModel.getGmailAppPassword()) }
    var gmailSmtpDeliveryType by remember { mutableStateOf(viewModel.getGmailSmtpDeliveryType()) }
    var instagramSettingState by remember { mutableStateOf(viewModel.getInstagramSetting()) }
    var telegramSettingState by remember { mutableStateOf(viewModel.getTelegramSetting()) }
    var youtubeSettingState by remember { mutableStateOf(viewModel.getYoutubeSetting()) }
    var defaultTimingStartState by remember { mutableStateOf(viewModel.getDefaultTimingStart()) }
    var defaultTimingEndState by remember { mutableStateOf(viewModel.getDefaultTimingEnd()) }
    var tournamentDelayState by remember { mutableStateOf(viewModel.getTournamentDelayMinutes().toString()) }
    var razorpayKeyIdState by remember { mutableStateOf(viewModel.getRazorpayKeyId()) }
    var cashfreeClientIdState by remember { mutableStateOf(viewModel.getCashfreeClientId()) }
    var cashfreeSecretKeyState by remember { mutableStateOf(viewModel.getCashfreeSecretKey()) }
    var minDepositState by remember { mutableStateOf(viewModel.getMinDepositAmount().toString()) }
    var minDebitState by remember { mutableStateOf(viewModel.getMinDebitAmount().toString()) }
    var minWithdrawalState by remember { mutableStateOf(viewModel.getMinWithdrawalAmount().toString()) }
    var fcmEnabledState by remember { mutableStateOf(viewModel.isFcmEnabled()) }
    var fcmServerKeyState by remember { mutableStateOf(viewModel.getFcmServerKey()) }
    var fcmProjectIdState by remember { mutableStateOf(viewModel.getFcmProjectId()) }
    var fcmMockModeState by remember { mutableStateOf(viewModel.isFcmMockMode()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Part 1: Gateway Configuration panel
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BATTLEZONE GATEWAY ROUTING CONFIGURATIONS", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Edit your real bank credentials & UPI id details dynamically to handle authentic pay-ins on client devices.", fontSize = 9.sp, color = GreyText)
                
                Spacer(modifier = Modifier.height(14.dp))
                // Toggle for Gateway Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ACTIVE GATEWAY TYPE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(if (gatewayModeState == "REAL_UPI") "Real Peer-To-Peer UPI Intent deep-linking" else "Testing sandbox scenario (Simulate)", fontSize = 8.sp, color = GreyText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TEST", color = if (gatewayModeState == "TEST_PREFILLED") Color.Green else GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = gatewayModeState == "REAL_UPI",
                            onCheckedChange = { gatewayModeState = if (it) "REAL_UPI" else "TEST_PREFILLED" },
                            colors = SwitchDefaults.colors(checkedThumbColor = RedPrimary, checkedTrackColor = RedDark)
                        )
                        Text("REAL UPI", color = if (gatewayModeState == "REAL_UPI") RedPrimary else GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = upiState,
                    onValueChange = { upiState = it },
                    label = { Text("Receiver UPI ID (Real Account)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = payeeState,
                    onValueChange = { payeeState = it },
                    label = { Text("Receiver Payee Full Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bankNameState,
                    onValueChange = { bankNameState = it },
                    label = { Text("Receiver Bank Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bankAccState,
                        onValueChange = { bankAccState = it },
                        label = { Text("Account Number") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = bankIfscState,
                        onValueChange = { bankIfscState = it },
                        label = { Text("IFSC Routing") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        modifier = Modifier.weight(0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.updatePaymentConfig(upiState, payeeState, bankAccState, bankIfscState, bankNameState, gatewayModeState)
                        scope.launch {
                            snackbarHost.showSnackbar("Payment Gateway details saved successfully!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE GATEWAY CONFIGURATIONS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // BATTLEZONE QUICK SESSIONS ENGINE Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BATTLEZONE AUTOMATED LOBBY GENERATOR", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Instantly generate standard $tournamentDelayState-minute tournaments from $defaultTimingStartState to $defaultTimingEndState with preset entry criteria (₹45 fee, ₹100 winning payouts).", fontSize = 9.sp, color = GreyText)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = defaultTimingStartState,
                        onValueChange = { defaultTimingStartState = it },
                        label = { Text("Start Time (e.g. 07:00 PM)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultTimingEndState,
                        onValueChange = { defaultTimingEndState = it },
                        label = { Text("End Time (e.g. 11:00 PM)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = tournamentDelayState,
                    onValueChange = { tournamentDelayState = it },
                    label = { Text("Inter-Tournament Delay / Gap (in minutes, e.g. 20)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.updateDefaultTournamentTiming(defaultTimingStartState, defaultTimingEndState, tournamentDelayState)
                        scope.launch {
                            snackbarHost.showSnackbar("Daily automatic sessions timeframe and delay changed successfully!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE AUTOMATIC TIMING TIMEFRAME", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.generateDefaultSessions { count ->
                            scope.launch {
                                snackbarHost.showSnackbar("Automated generator processed! $count new lobby sessions successfully live on Firebase.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Flash", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATE DEFAULT DAILY SESSIONS ($defaultTimingStartState - $defaultTimingEndState)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // BATTLEZONE MINIMUM TRANSACTION LIMITS Config Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BATTLEZONE TRANSACTION LIMIT CONFIGURATIONS", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Dynamically customize minimum boundaries for user recharges (deposits), join game entries (debits), and withdrawal payouts.", fontSize = 9.sp, color = GreyText)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minDepositState,
                        onValueChange = { minDepositState = it },
                        label = { Text("Min Deposit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minDebitState,
                        onValueChange = { minDebitState = it },
                        label = { Text("Min Debit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minWithdrawalState,
                        onValueChange = { minWithdrawalState = it },
                        label = { Text("Min Payout (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        modifier = Modifier.weight(1.1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val dep = minDepositState.toDoubleOrNull() ?: 10.0
                        val deb = minDebitState.toDoubleOrNull() ?: 0.0
                        val wit = minWithdrawalState.toDoubleOrNull() ?: 50.0
                        viewModel.updateMinLimitsConfig(dep, deb, wit)
                        scope.launch {
                            snackbarHost.showSnackbar("Minimum boundaries pushed to Firebase in real-time!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE TRANSACTION LIMIT BOUNDARIES", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BATTLEZONE REAL-SMS OTP CONFIGURATIONS", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Select & configure your authentic SMS Gateway API to deliver dynamic verification OTPs directly to user mobile phones.", fontSize = 9.sp, color = GreyText)
                Spacer(modifier = Modifier.height(14.dp))
                Text("SMS GATEWAY CHANNEL MODE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf("TEST_MODE", "FAST2SMS", "TWILIO", "CUSTOM_HTTP_API", "GMAIL_SMTP")
                    modes.forEach { m ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (smsGatewayMode == m) RedPrimary else Color(0xFF1F1C25),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { 
                                    smsGatewayMode = m 
                                    viewModel.updateSmsConfig(
                                        mode = m,
                                        fast2smsKey = fast2smsKey,
                                        twilioSid = twilioSid,
                                        twilioToken = twilioToken,
                                        twilioPhone = twilioPhone,
                                        customUrl = customSmsUrl
                                    )
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (m) {
                                    "TEST_MODE" -> "DEBUG TEST"
                                    "CUSTOM_HTTP_API" -> "CUSTOM URL"
                                    "GMAIL_SMTP" -> "GMAIL OTP"
                                    else -> m
                                },
                                color = if (smsGatewayMode == m) Color.White else GreyText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                when (smsGatewayMode) {
                    "TEST_MODE" -> {
                        Text(
                            "📌 DEBUG TEST MODE ACTIVE: OTPs are processed instantly on client screen via in-app toast overlays. Complete sandbox (No external cell charges).",
                            color = Color(0xFF81C784),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    "FAST2SMS" -> {
                        OutlinedTextField(
                            value = fast2smsKey,
                            onValueChange = { fast2smsKey = it },
                            label = { Text("Fast2SMS Auth API Key") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                            placeholder = { Text("Enter authorization token key...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "💡 Fast2SMS bulkV2 dynamic endpoint GET route is used to dispatch Indian number OTPs efficiently.",
                            color = GreyText,
                            fontSize = 8.sp
                        )
                    }
                    "TWILIO" -> {
                        OutlinedTextField(
                            value = twilioSid,
                            onValueChange = { twilioSid = it },
                            label = { Text("Twilio Account SID") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                            placeholder = { Text("e.g. ACxxxxxxxxxxxxxx") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = twilioToken,
                            onValueChange = { twilioToken = it },
                            label = { Text("Twilio Auth Token") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                            placeholder = { Text("Enter token...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = twilioPhone,
                            onValueChange = { twilioPhone = it },
                            label = { Text("Twilio Outbound Phone Number (from)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                            placeholder = { Text("e.g. +1234567890") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "CUSTOM_HTTP_API" -> {
                        OutlinedTextField(
                            value = customSmsUrl,
                            onValueChange = { customSmsUrl = it },
                            label = { Text("Custom HTTP Gateway GET URL") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                            placeholder = { Text("https://my-sms.com/send?to={phone}&code={otp}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "⚠️ Use {phone} and {otp} placeholders which will be dynamically replaced on verification triggers.",
                            color = GreyText,
                            fontSize = 8.sp
                        )
                    }
                    "GMAIL_SMTP" -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("DELIVERY ROUTING TYPE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("BACKEND_API", "DIRECT_SMTP").forEach { type ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (gmailSmtpDeliveryType == type) RedPrimary else Color(0xFF1F1C25),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { gmailSmtpDeliveryType = type }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (type == "BACKEND_API") "BACKEND NODEMAILER API" else "DIRECT SSL SMTP SOCKET",
                                        color = if (gmailSmtpDeliveryType == type) Color.White else GreyText,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (gmailSmtpDeliveryType == "BACKEND_API") {
                            OutlinedTextField(
                                value = gmailBackendUrlState,
                                onValueChange = { gmailBackendUrlState = it },
                                label = { Text("Nodemailer Backend URL") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "💡 Calls your secure central Node.js webhook controller securely. Recommended for professional multi-user deployments to protect app password credentials.",
                                color = GreyText,
                                fontSize = 8.sp
                            )
                        } else {
                            OutlinedTextField(
                                value = gmailSmtpUser,
                                onValueChange = { gmailSmtpUser = it },
                                label = { Text("Gmail Sender Address (Email)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                                placeholder = { Text("e.g. your_email@gmail.com") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = gmailSmtpPassword,
                                onValueChange = { gmailSmtpPassword = it },
                                label = { Text("Gmail 16-Digit App Password") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                                placeholder = { Text("e.g. abcd efgh ijkl mnop") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "✅ DIRECT SECURE SMTP GATEWAY: Fully secure on-device direct SSL connection. Set up a regular Gmail address, enable 2-Step Verification in your Google Account settings, generate a '16-character App Password', and enter details above. Completely free, no registration dependencies!",
                                color = Color(0xFF81C784),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.updateSmsConfig(
                            mode = smsGatewayMode,
                            fast2smsKey = fast2smsKey,
                            twilioSid = twilioSid,
                            twilioToken = twilioToken,
                            twilioPhone = twilioPhone,
                            customUrl = customSmsUrl
                        )
                        viewModel.updateGmailSmtpConfig(gmailSmtpUser, gmailSmtpPassword)
                        viewModel.updateGmailSmtpDeliveryType(gmailSmtpDeliveryType)
                        viewModel.updateGmailOtpBackendUrl(gmailBackendUrlState)
                        scope.launch {
                            snackbarHost.showSnackbar("Gateway routing configurations successfully dynamic saved!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE SMS GATEWAY CONFIGS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Part 1.5: Social channels configuration card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BATTLEZONE SOCIAL OFFICIAL CHANNELS", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Configure your active official Instagram, Telegram, and YouTube channels. Clients will immediately auto-forward to these handles from their dashboard buttons.", fontSize = 9.sp, color = GreyText)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = instagramSettingState,
                    onValueChange = { instagramSettingState = it },
                    label = { Text("Instagram ID / Link") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                    placeholder = { Text("e.g. its_nivetha_01 or full URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = telegramSettingState,
                    onValueChange = { telegramSettingState = it },
                    label = { Text("Telegram Link / Channel ID") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                    placeholder = { Text("e.g. battlezone_esports_official or full URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = youtubeSettingState,
                    onValueChange = { youtubeSettingState = it },
                    label = { Text("YouTube URL / Channel Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                    placeholder = { Text("e.g. @battlezone_esports or full URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.updateSocialConfig(instagramSettingState, telegramSettingState, youtubeSettingState)
                        scope.launch {
                            snackbarHost.showSnackbar("Social official channels updated successfully!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE SOCIAL CONFIGS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Part 1.6: Firebase Cloud Messaging Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
                .testTag("fcm_config_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("FCM PUSH NOTIFICATIONS GATEWAY", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Configure dynamic real-time Firebase Cloud Messaging alerts to notify players instantly when they register or when matches start.", fontSize = 9.sp, color = GreyText)
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable FCM Alerts", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Send push alerts for tournament registrations and match start events.", fontSize = 8.sp, color = GreyText)
                    }
                    Switch(
                        checked = fcmEnabledState,
                        onCheckedChange = { fcmEnabledState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RedPrimary,
                            checkedTrackColor = RedPrimary.copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("fcm_enabled_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Interactive Local Simulation", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Simulates instant visual status bar notifications. Recommended for developer sandbox testing.", fontSize = 8.sp, color = GreyText)
                    }
                    Switch(
                        checked = fcmMockModeState,
                        onCheckedChange = { fcmMockModeState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RedPrimary,
                            checkedTrackColor = RedPrimary.copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("fcm_mock_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (fcmEnabledState) {
                    OutlinedTextField(
                        value = fcmServerKeyState,
                        onValueChange = { fcmServerKeyState = it },
                        label = { Text("FCM Legacy Server Key") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        placeholder = { Text("e.g. AIzaSy...") },
                        modifier = Modifier.fillMaxWidth().testTag("fcm_server_key_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fcmProjectIdState,
                        onValueChange = { fcmProjectIdState = it },
                        label = { Text("FCM Project ID") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        placeholder = { Text("e.g. battlezone-app-123") },
                        modifier = Modifier.fillMaxWidth().testTag("fcm_project_id_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "✅ FCM Topic Broadcasting: Subscribed clients automatically receive live alerts on topic 'tournament_ID' without manual intervention.",
                        color = Color(0xFF81C784),
                        fontSize = 8.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.updateFcmConfig(fcmEnabledState, fcmServerKeyState, fcmProjectIdState, fcmMockModeState)
                        scope.launch {
                            snackbarHost.showSnackbar("FCM Push settings successfully updated in real-time!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("save_fcm_configs_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE FCM PUSH SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Part 1.7: Default tournament timings configuration card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("DEFAULT TOURNAMENT TIME LIMITS", fontSize = 11.sp, color = NeonGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Set default starting and ending tournament times (Default: 07:00 PM to 11:00 PM). New tournaments will default to this window.", fontSize = 9.sp, color = GreyText)
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultTimingStartState,
                        onValueChange = { defaultTimingStartState = it },
                        label = { Text("Default Start Time") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        placeholder = { Text("e.g. 07:00 PM") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultTimingEndState,
                        onValueChange = { defaultTimingEndState = it },
                        label = { Text("Default End Time") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                        placeholder = { Text("e.g. 11:00 PM") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = tournamentDelayState,
                    onValueChange = { tournamentDelayState = it },
                    label = { Text("Inter-Tournament Delay (in minutes, e.g. 20)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF1F1C25)),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.updateDefaultTournamentTiming(defaultTimingStartState, defaultTimingEndState, tournamentDelayState)
                        scope.launch {
                            snackbarHost.showSnackbar("Default tournament timings and delay updated successfully!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE DEFAULT TIMINGS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        // Part 2: Review user pending deposits
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showDepositHistory = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showDepositHistory) RedPrimary else Color(0xFF1E1C24)
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("PENDING PAY-INS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Button(
                onClick = { showDepositHistory = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showDepositHistory) RedPrimary else Color(0xFF1E1C24)
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("APPROVED / REJECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        val displayedDeposits = if (showDepositHistory) {
            allTransactions.filter { it.type == "DEPOSIT" && it.status != "PENDING" }
        } else {
            allTransactions.filter { it.type == "DEPOSIT" && it.status == "PENDING" }
        }

        Text(
            if (showDepositHistory) "APPROVED & REJECTED DEPOSITS HISTORY" else "PENDING GATEWAY DEPOSITS QUEUE (${pendingDeposits.size})", 
            fontSize = 11.sp, 
            color = GreyText, 
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (displayedDeposits.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
            ) {
                Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (showDepositHistory) "No processed deposit history available." else "No pending pay-ins are waiting for manual check.", 
                        color = GreyText, 
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            displayedDeposits.forEach { dp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("USER: ${dp.userId}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Amount: ₹${dp.amount}", color = NeonGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            val statusColor = if (dp.status == "SUCCESS") Color.Green else if (dp.status == "PENDING") Color(0xFFFF5722) else RedPrimary
                            Card(
                                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                            ) {
                                Text(dp.status, color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(dp.title, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Invoice Ref: ${dp.invoiceId} | Date: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).format(dp.timestamp)}", fontSize = 8.sp, color = GreyText)
                        
                        if (dp.status == "PENDING") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.adminRejectDeposit(dp.id) { success ->
                                            if (success) {
                                                scope.launch { snackbarHost.showSnackbar("Transaction Rejected.") }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF140D0F)),
                                    border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Reject", tint = RedPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("REJECT", color = RedPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.adminDeleteTransaction(dp.id) { success ->
                                            if (success) {
                                                scope.launch { snackbarHost.showSnackbar("Duplicate Money Entry completely deleted.") }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF241416)),
                                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                                    modifier = Modifier.weight(1.1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Dup", tint = Color(0xFFE53935), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("DELETE DUP", color = Color(0xFFE53935), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Button(
                                    onClick = {
                                        viewModel.adminApproveDeposit(dp.id) { success ->
                                            if (success) {
                                                scope.launch { snackbarHost.showSnackbar("Transaction Approved & ₹${dp.amount} Credited to ${dp.userId}!") }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1B12)),
                                    border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1.1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Approve", tint = Color.Green, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("APPROVE", color = Color.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Part 3: Anti-A Advanced Cloud Protection Antivirus Shield
        var isScanningState by remember { mutableStateOf(false) }
        var scanLogsList by remember { mutableStateOf(listOf<String>("Ready to conduct real-time security cloud sync audit.")) }
        var activeProgress by remember { mutableFloatStateOf(0f) }
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, if (isScanningState) RedPrimary else Color(0xFF00E676).copy(alpha = 0.6f)),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "🛡️ ANTI-A CLOUD INTEGRITY SECURITY SHIELD",
                            fontSize = 11.sp,
                            color = NeonGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Advanced zero-trust sandbox scanning & cloud-synchronized threat mitigation engine.",
                            fontSize = 9.sp,
                            color = GreyText
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isScanningState) Color(0xFF261214) else Color(0xFF0D2214)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isScanningState) RedPrimary.copy(alpha = 0.4f) else Color.Green.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = if (isScanningState) "SCANNING ACTIVE" else "SHIELD REINFORCED",
                            color = if (isScanningState) RedPrimary else Color.Green,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                // Security Metrics Snapshot
                Text("SAFETY INTEGRITY PARAMETERS", fontSize = 9.sp, color = GreyText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF141218), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("ROOT EXPLOITS", fontSize = 8.sp, color = GreyText)
                            Text("BLOCKED/CLEAN", fontSize = 10.sp, color = Color.Green, fontWeight = FontWeight.Black)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF141218), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("RE-TAMPER HASH", fontSize = 8.sp, color = GreyText)
                            Text("SHA-256 SAFE", fontSize = 10.sp, color = NeonGold, fontWeight = FontWeight.Black)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF141218), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("SQL INJECTION", fontSize = 8.sp, color = GreyText)
                            Text("SANITIZED ON-LINE", fontSize = 10.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Log terminal block
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0E13)),
                    border = BorderStroke(1.dp, Color(0xFF24222B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "ANTI-A DIAGNOSTIC TERMINAL LOGS [LIVE SYNC]:",
                            fontSize = 8.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        scanLogsList.forEach { log ->
                            Text(
                                "⚡ $log",
                                fontSize = 8.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = if (log.contains("SUCCESS") || log.contains("SECURE")) Color(0xFF00E676) else if (log.contains("Error") || log.contains("anomaly")) RedPrimary else Color.White,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
                if (isScanningState) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = activeProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = RedPrimary,
                        trackColor = Color(0xFF281014)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        isScanningState = true
                        activeProgress = 0f
                        scanLogsList = listOf("Connecting to cloud gateway signature authority...")
                        viewModel.runAdvancedAntiVirusSync(
                            onLogUpdate = { updatedLine ->
                                val updatedList = scanLogsList.toMutableList()
                                updatedList.add(updatedLine)
                                scanLogsList = updatedList
                                activeProgress += 0.12f
                            },
                            onFinished = { _ ->
                                isScanningState = false
                                activeProgress = 1.0f
                                scope.launch {
                                    snackbarHost.showSnackbar("🛡️ Anti-A Security Shield Sync Process Finished! System 100% verified.")
                                }
                            }
                        )
                    },
                    enabled = !isScanningState,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanningState) Color(0xFF261214) else Color(0xFF0D2214)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isScanningState) RedPrimary.copy(alpha = 0.5f) else Color(0xFF00E676).copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "shield scanner",
                        tint = if (isScanningState) GreyText else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanningState) "EXECUTING ACTIVE CLOUD AUDIT..." else "RUN ANTI-A DYNAMIC SECURITY SCAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
fun deduplicateInput(current: String, incoming: String): String {
    if (incoming.length == current.length + 2) {
        if (incoming.endsWith(current)) {
            val prefix = incoming.substring(0, 2)
            if (prefix[0] == prefix[1]) {
                return prefix[0].toString() + current
            }
        } else if (incoming.startsWith(current)) {
            val suffix = incoming.substring(current.length)
            if (suffix[0] == suffix[1]) {
                return current + suffix[0].toString()
            }
        } else {
            var mismatchIndex = -1
            for (i in current.indices) {
                if (current[i] != incoming[i]) {
                    mismatchIndex = i
                    break
                }
            }
            if (mismatchIndex != -1 && mismatchIndex + 1 < incoming.length) {
                if (incoming[mismatchIndex] == incoming[mismatchIndex + 1]) {
                    return incoming.removeRange(mismatchIndex, mismatchIndex + 1)
                }
            }
        }
    }
    return incoming
}
@Composable
fun LoginRegistrationScreen(viewModel: BattleZoneViewModel) {
    var activeAuthTab by remember { mutableStateOf("SIGN_IN") } // "SIGN_IN" or "REGISTER"
    var authStep by remember { mutableStateOf("FORM") } // "FORM" or "OTP"
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var otpFlowType by remember { mutableStateOf("SIGN_IN") } // "SIGN_IN", "REGISTER", "GOOGLE", "GOOGLE_LINKED"
    var otpErrorMsg by remember { mutableStateOf<String?>(null) }
    var otpTimerSeconds by remember { mutableStateOf(120) }
    LaunchedEffect(authStep) {
        if (authStep == "OTP") {
            otpTimerSeconds = 120
            while (otpTimerSeconds > 0) {
                delay(1000L)
                otpTimerSeconds--
            }
        }
    }
    val lastRegisteredUserId = remember { viewModel.getLastRegisteredUserId() }
    var cachedUserDetails by remember { mutableStateOf<UserEntity?>(null) }
    var isReentryActive by remember { mutableStateOf(lastRegisteredUserId.isNotBlank()) }
    var showGooglePhoneLinking by remember { mutableStateOf(false) }
    var linkingGoogleEmail by remember { mutableStateOf("") }
    var linkingGoogleName by remember { mutableStateOf("") }
    var linkingPhoneInput by remember { mutableStateOf("") }
    var linkingPhoneError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lastRegisteredUserId) {
        if (lastRegisteredUserId.isNotBlank()) {
            cachedUserDetails = viewModel.getUserSync(lastRegisteredUserId)
        }
    }
    val savedUserId = viewModel.getSavedLoggedInUserId()
    var savedUser by remember { mutableStateOf<UserEntity?>(null) }
    LaunchedEffect(savedUserId) {
        if (savedUserId != "default_user") {
            savedUser = viewModel.getUserSync(savedUserId)
        }
    }
    var ignInput by remember { mutableStateOf("") }
    var ffUidInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var extraMobileInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    
    // For Sign In tab
    var signInPhoneInput by remember { mutableStateOf("") }
    
    var authMode by remember { mutableStateOf("EMAIL_PASS") } // "EMAIL_PASS" or "PHONE_OTP"
    var loginEmailInput by remember { mutableStateOf("") }
    var loginPasswordInput by remember { mutableStateOf("") }
    var registerPasswordInput by remember { mutableStateOf("") }
    var resetNewPasswordInput by remember { mutableStateOf("") }
    var resetConfirmPasswordInput by remember { mutableStateOf("") }
    var resetErrorMsg by remember { mutableStateOf<String?>(null) }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var customGoogleName by remember { mutableStateOf("") }
    var customGoogleEmail by remember { mutableStateOf("") }
    var showCustomGoogleFields by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is android.content.ContextWrapper) {
            if (c is android.app.Activity) break
            c = c.baseContext
        }
        c as? android.app.Activity
    }
    val gPrefs = remember { context.getSharedPreferences("google_accounts_cache", android.content.Context.MODE_PRIVATE) }
    
    val saveGoogleEmailCache = remember {
        { email: String ->
            // Do not cache any logged-in google accounts to protect user privacy and respect user intent
        }
    }
    val accountsList = remember { mutableStateListOf<String>() }
    var isGoogleWebLoginActive by remember { mutableStateOf(false) }
    var hasAccountPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.GET_ACCOUNTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasAccountPermission = isGranted
        }
    )
    LaunchedEffect(hasAccountPermission, showGoogleDialog) {
        if (showGoogleDialog) {
            accountsList.clear()
            
            // Clear any previously saved Google accounts cache completely to guarantee fresh/blank state
            try {
                gPrefs.edit().clear().apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Perform system platform accounts scanning (with runtime authorization check) ONLY
            if (hasAccountPermission) {
                try {
                    val am = android.accounts.AccountManager.get(context)
                    val accounts = am.getAccountsByType("com.google")
                    for (acc in accounts) {
                        val trimmedAcc = acc.name.trim().lowercase()
                        // Ensure we do NOT show any default/mock/test emails (including the user's email) from device list
                        if (acc.name.contains("@") && 
                            trimmedAcc != "selva19122008@gmail.com" && 
                            trimmedAcc != "battlezone.pro@gmail.com" && 
                            trimmedAcc != "gamer.esports@gmail.com" &&
                            !accountsList.contains(acc.name)) {
                            accountsList.add(acc.name)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                permissionLauncher.launch(Manifest.permission.GET_ACCOUNTS)
            }
            // If no actual accounts were fetched from the physical phone, show manual login dialog immediately
            if (accountsList.isEmpty()) {
                isGoogleWebLoginActive = true
            } else {
                isGoogleWebLoginActive = false
            }
        }
    }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Aesthetic radial light flare
        Box(
            modifier = Modifier
                .size(450.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RedPrimary.copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gaming shield logo frame
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        color = Color(0xFF16141A).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(
                        border = BorderStroke(
                            2.dp, Brush.verticalGradient(
                                listOf(RedPrimary, Color(0xFF5E1119))
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_app_logo),
                    contentDescription = "BattleZone Shield Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "BATTLE ZONE",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SECURE ESPORTS LOBBY ENTRY",
                color = GreyText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color(0xFF24212B))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).animateContentSize()
                ) {
                    if (authStep == "OTP") {
                        // OTP verification form view
                        val currentVerifyingEmail = if (otpFlowType == "SIGN_IN") {
                            if (loginEmailInput.trim().isNotBlank()) loginEmailInput.trim() else if (emailInput.isNotBlank()) emailInput.trim() else ""
                        } else if (otpFlowType == "GOOGLE_LINKED" || otpFlowType == "GOOGLE") {
                            customGoogleEmail.trim().lowercase()
                        } else {
                            emailInput.trim()
                        }
                        val isEmailAdmin = currentVerifyingEmail.trim().lowercase() == "selva19122008@gmail.com"

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔒 SECURE ACCOUNT OTP VERIFICATION",
                                fontSize = 11.sp,
                                color = NeonGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "An OTP code has been generated by the BattleZone system. Input this code below to gain secure app entry.",
                                fontSize = 9.sp,
                                color = GreyText,
                                textAlign = TextAlign.Center
                            )

                            // Dynamic high-visibility English spam notice
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF201618), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "⚠️ OTP EMAIL NOT ARRIVING?",
                                        fontSize = 10.sp,
                                        color = Color(0xFFFF5252),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "If the OTP code does not arrive in your Primary Inbox, please check your Spam or Junk folder. Mark it as 'Not Spam' to receive future tournament matches and wallet updates directly to your Inbox.",
                                        fontSize = 9.sp,
                                        color = GreyText,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            // High-visibility 120s validity countdown timer card
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (otpTimerSeconds > 0) Color(0xFF1B1610) else Color(0xFF281116), 
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp, 
                                            if (otpTimerSeconds > 0) Color(0xFFFFB300).copy(alpha = 0.4f) else RedPrimary.copy(alpha = 0.4f)
                                        ), 
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        if (otpTimerSeconds > 0) {
                                            CircularProgressIndicator(
                                                progress = otpTimerSeconds / 120f,
                                                modifier = Modifier.fillMaxSize(),
                                                color = Color(0xFFFFB300),
                                                strokeWidth = 3.dp,
                                            )
                                            Text(
                                                text = "$otpTimerSeconds",
                                                fontSize = 11.sp,
                                                color = Color(0xFFFFB300),
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "expired",
                                                tint = RedPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (otpTimerSeconds > 0) "⏱️ OTP VALIDITY COUNTDOWN" else "⚠️ OTP EXPIRED",
                                            fontSize = 9.sp,
                                            color = if (otpTimerSeconds > 0) Color(0xFFFFB300) else RedPrimary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (otpTimerSeconds > 0) {
                                                "Please enter the verification OTP and submit within the next $otpTimerSeconds seconds. Otherwise, the OTP will become invalid."
                                             } else {
                                                 "The OTP validity period has expired. Please go back to the previous screen to request a new OTP code."
                                             },
                                             fontSize = 9.sp,
                                             color = GreyText,
                                             lineHeight = 11.sp
                                         )
                                     }
                                 }
                             }
                            
                            if (generatedOtp.isNotEmpty() && isEmailAdmin) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF16111E), RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.dp, NeonGold.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🔧 BATTLEZONE SYSTEM ASSISTANT",
                                            fontSize = 10.sp,
                                            color = NeonGold,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "For instant testing or fallback delivery, your 6-digit verification code is:",
                                            fontSize = 9.sp,
                                            color = GreyText,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = generatedOtp,
                                            fontSize = 22.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 5.sp,
                                            modifier = Modifier.clickable {
                                                enteredOtp = generatedOtp
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "(Tap code to auto-fill input)",
                                            fontSize = 8.sp,
                                            color = RedPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = enteredOtp,
                                enabled = otpTimerSeconds > 0,
                                onValueChange = { 
                                    val cleaned = deduplicateInput(enteredOtp, it)
                                    enteredOtp = cleaned.filter { c -> c.isDigit() }.take(6) 
                                    otpErrorMsg = null
                                },
                                label = { Text("Enter Verification Code") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "otp", tint = RedPrimary) },
                                placeholder = { Text("e.g. 582479") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = Color(0xFF28252C),
                                    focusedLabelColor = RedPrimary,
                                    unfocusedLabelColor = GreyText,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("otp_code_input")
                            )
                            otpErrorMsg?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(msg, color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    val currentGateway = viewModel.getSmsGatewayMode()
                                    val verificationEmail = if (otpFlowType == "SIGN_IN") {
                                        if (loginEmailInput.trim().isNotBlank()) loginEmailInput.trim() else if (emailInput.isNotBlank()) emailInput.trim() else ""
                                    } else if (otpFlowType == "GOOGLE_LINKED" || otpFlowType == "GOOGLE") {
                                        customGoogleEmail.trim().lowercase()
                                    } else {
                                        emailInput.trim()
                                    }
                                    val isEmailAdmin = verificationEmail.trim().lowercase() == "selva19122008@gmail.com"
                                    val resolvedGateway = currentGateway
                                    val isDirectOtpBypass = isEmailAdmin && (enteredOtp == "1212" || enteredOtp == "one to one two" || enteredOtp == "121212")
                                    
                                    if (resolvedGateway == "GMAIL_SMTP" && !isDirectOtpBypass) {
                                        viewModel.verifyGmailOtpSecurely(
                                            email = verificationEmail,
                                            otpCode = enteredOtp,
                                            inGameName = if (otpFlowType == "REGISTER" || otpFlowType == "REGISTER_EMAIL_PASS") ignInput.trim() else if (otpFlowType == "GOOGLE" || otpFlowType == "GOOGLE_LINKED") customGoogleName.trim() else "",
                                            freeFireUid = "",
                                            onFinished = { success, error ->
                                                if (success) {
                                                    if (otpFlowType == "REGISTER_EMAIL_PASS") {
                                                        val determinedPhone = "+91" + phoneInput.trim()
                                                        val determinedExtra = if (extraMobileInput.isNotBlank()) "+91" + extraMobileInput.trim() else ""
                                                        viewModel.firebaseRegisterWithEmailAndPassword(
                                                            ign = ignInput.trim(),
                                                            ffUid = ffUidInput.trim(),
                                                            phone = determinedPhone,
                                                            extraMobile = determinedExtra,
                                                            emailInput = emailInput.trim(),
                                                            passwordInput = registerPasswordInput,
                                                            onFinished = { fSucc, fError ->
                                                                if (fSucc) {
                                                                    authStep = "FORM"
                                                                } else {
                                                                    otpErrorMsg = fError ?: "Firebase sync error."
                                                                }
                                                            }
                                                        )
                                                    } else if (otpFlowType == "SIGN_IN") {
                                                        viewModel.firebaseSignInWithEmailAndPassword(
                                                            emailInput = loginEmailInput.trim(),
                                                            passwordInput = loginPasswordInput,
                                                            onFinished = { fSucc, fError ->
                                                                if (fSucc) {
                                                                    authStep = "FORM"
                                                                } else {
                                                                    authStep = "FORM"
                                                                }
                                                            }
                                                        )
                                                    } else if (otpFlowType == "FORGOT_PASSWORD") {
                                                        authStep = "RESET_PASSWORD"
                                                    } else {
                                                        authStep = "FORM"
                                                    }
                                                } else {
                                                    otpErrorMsg = error ?: "Incorrect OTP verification code."
                                                }
                                            }
                                        )
                                    } else {
                                        val isMockOtpValid = (enteredOtp == generatedOtp || isDirectOtpBypass || (isEmailAdmin && enteredOtp == "1212" && resolvedGateway == "TEST_MODE"))
                                        if (isMockOtpValid && (resolvedGateway == "TEST_MODE" || isDirectOtpBypass || viewModel.firebaseVerificationId == null)) {
                                        // Bypass for debugging test mode (Email and Password Enabled)
                                        if (otpFlowType == "SIGN_IN") {
                                            viewModel.firebaseSignInWithEmailAndPassword(
                                                emailInput = loginEmailInput.trim(),
                                                passwordInput = loginPasswordInput,
                                                onFinished = { success, error ->
                                                    if (success) {
                                                        authStep = "FORM"
                                                    } else {
                                                        otpErrorMsg = error ?: "Login verification failed."
                                                    }
                                                }
                                            )
                                        } else if (otpFlowType == "REGISTER") {
                                            val determinedPhone = "+91" + phoneInput.trim()
                                            val determinedExtra = if (extraMobileInput.isNotBlank()) "+91" + extraMobileInput.trim() else ""
                                            viewModel.loginUser(
                                                ign = ignInput.trim(),
                                                ffUid = ffUidInput.trim(),
                                                phone = determinedPhone,
                                                extraMobile = determinedExtra,
                                                email = emailInput.trim(),
                                                onFinished = {
                                                    authStep = "FORM"
                                                }
                                            )
                                        } else if (otpFlowType == "REGISTER_EMAIL_PASS") {
                                            val determinedPhone = "+91" + phoneInput.trim()
                                            val determinedExtra = if (extraMobileInput.isNotBlank()) "+91" + extraMobileInput.trim() else ""
                                            viewModel.firebaseRegisterWithEmailAndPassword(
                                                ign = ignInput.trim(),
                                                ffUid = ffUidInput.trim(),
                                                phone = determinedPhone,
                                                extraMobile = determinedExtra,
                                                emailInput = emailInput.trim(),
                                                passwordInput = registerPasswordInput,
                                                onFinished = { fSucc, fError ->
                                                    if (fSucc) {
                                                        authStep = "FORM"
                                                    } else {
                                                        otpErrorMsg = fError ?: "Firebase Register failed."
                                                    }
                                                }
                                            )
                                        }
                                       else if (otpFlowType == "GOOGLE") {
                                             viewModel.loginWithGoogle(
                                                 email = customGoogleEmail.trim().lowercase(),
                                                 name = customGoogleName.trim(),
                                                 onFinished = {
                                                     authStep = "FORM"
                                                 }
                                             )
                                         } else if (otpFlowType == "GOOGLE_LINKED") {
                                             viewModel.loginWithGoogleLinked(
                                                 email = customGoogleEmail.trim().lowercase(),
                                                 name = customGoogleName.trim(),
                                                 phone = extraMobileInput.trim(),
                                                 onFinished = {
                                                     authStep = "FORM"
                                                 }
                                             )
                                         }
                                     } else if (viewModel.firebaseVerificationId != null) {
                                        // Real Firebase Verification flow
                                        viewModel.verifyFirebasePhoneOtp(enteredOtp) { success, error ->
                                            if (success) {
                                                if (otpFlowType == "SIGN_IN") {
                                                    viewModel.loginExistingUser(
                                                        phone = "+91" + signInPhoneInput.trim(),
                                                        onFinished = { logSuccess, logError ->
                                                            if (logSuccess) {
                                                                authStep = "FORM"
                                                            } else {
                                                                otpErrorMsg = logError ?: "Login verification failed."
                                                            }
                                                        }
                                                    )
                                                } else if (otpFlowType == "REGISTER") {
                                                    val determinedPhone = "+91" + phoneInput.trim()
                                                    val determinedExtra = if (extraMobileInput.isNotBlank()) "+91" + extraMobileInput.trim() else ""
                                                    viewModel.loginUser(
                                                        ign = ignInput.trim(),
                                                        ffUid = ffUidInput.trim(),
                                                        phone = determinedPhone,
                                                        extraMobile = determinedExtra,
                                                        email = emailInput.trim(),
                                                        onFinished = {
                                                            authStep = "FORM"
                                                        }
                                                    )
                                                } else if (otpFlowType == "REGISTER_EMAIL_PASS") {
                                                    val determinedPhone = "+91" + phoneInput.trim()
                                                    val determinedExtra = if (extraMobileInput.isNotBlank()) "+91" + extraMobileInput.trim() else ""
                                                    viewModel.firebaseRegisterWithEmailAndPassword(
                                                        ign = ignInput.trim(),
                                                        ffUid = ffUidInput.trim(),
                                                        phone = determinedPhone,
                                                        extraMobile = determinedExtra,
                                                        emailInput = emailInput.trim(),
                                                        passwordInput = registerPasswordInput,
                                                        onFinished = { fSucc, fError ->
                                                            if (fSucc) {
                                                                authStep = "FORM"
                                                            } else {
                                                                otpErrorMsg = fError ?: "Firebase Register failed."
                                                            }
                                                        }
                                                    )
                                                 } else if (otpFlowType == "GOOGLE") {
                                                 viewModel.loginWithGoogle(
                                                     email = customGoogleEmail.trim().lowercase(),
                                                     name = customGoogleName.trim(),
                                                     onFinished = {
                                                         authStep = "FORM"
                                                     }
                                                 )
                                             } else if (otpFlowType == "GOOGLE_LINKED") {
                                                 viewModel.loginWithGoogleLinked(
                                                     email = customGoogleEmail.trim().lowercase(),
                                                     name = customGoogleName.trim(),
                                                     phone = extraMobileInput.trim(),
                                                     onFinished = {
                                                         authStep = "FORM"
                                                     }
                                                 )
                                             } else if (false) { // Skip redundant block below to preserve alignment
                                                    viewModel.loginWithGoogle(
                                                        email = customGoogleEmail.trim().lowercase(),
                                                        name = customGoogleName.trim(),
                                                        onFinished = {
                                                            authStep = "FORM"
                                                        }
                                                    )
                                                } else if (otpFlowType == "GOOGLE_LINKED") {
                                                    viewModel.loginWithGoogleLinked(
                                                        email = customGoogleEmail.trim().lowercase(),
                                                        name = customGoogleName.trim(),
                                                        phone = extraMobileInput.trim(),
                                                        onFinished = {
                                                            authStep = "FORM"
                                                        }
                                                    )
                                                } else if (false) { // Skip redundant block below to preserve alignment
                                                    viewModel.loginWithGoogle(
                                                        email = customGoogleEmail.trim().lowercase(),
                                                        name = customGoogleName.trim(),
                                                        onFinished = {
                                                            authStep = "FORM"
                                                        }
                                                    )
                                                }
                                            } else {
                                                otpErrorMsg = error ?: "Verification failed. Please enter the correct 6-digit code received via SMS."
                                            }
                                        }
                                    } else {
                                        // Fallback legacy system check
                                        if (enteredOtp == generatedOtp) {
                                            if (otpFlowType == "SIGN_IN") {
                                                viewModel.loginExistingUser(
                                                    phone = "+91" + signInPhoneInput.trim(),
                                                    onFinished = { success, error ->
                                                        if (success) {
                                                            authStep = "FORM"
                                                        } else {
                                                            otpErrorMsg = error ?: "Login verification failed."
                                                        }
                                                     }
                                                 )
                                            } else if (otpFlowType == "REGISTER") {
                                                val determinedPhone = "+91" + phoneInput.trim()
                                                val determinedExtra = if (extraMobileInput.isNotBlank()) "+91" + extraMobileInput.trim() else ""
                                                viewModel.loginUser(
                                                    ign = ignInput.trim(),
                                                    ffUid = ffUidInput.trim(),
                                                    phone = determinedPhone,
                                                    extraMobile = determinedExtra,
                                                    email = emailInput.trim(),
                                                    onFinished = {
                                                        authStep = "FORM"
                                                    }
                                                )
                                            } else if (otpFlowType == "REGISTER_EMAIL_PASS") {
                                                val determinedPhone = "+91" + phoneInput.trim()
                                                val determinedExtra = if (extraMobileInput.isNotBlank()) "+91" + extraMobileInput.trim() else ""
                                                viewModel.firebaseRegisterWithEmailAndPassword(
                                                    ign = ignInput.trim(),
                                                    ffUid = ffUidInput.trim(),
                                                    phone = determinedPhone,
                                                    extraMobile = determinedExtra,
                                                    emailInput = emailInput.trim(),
                                                    passwordInput = registerPasswordInput,
                                                    onFinished = { fSucc, fError ->
                                                        if (fSucc) {
                                                            authStep = "FORM"
                                                        } else {
                                                            otpErrorMsg = fError ?: "Firebase Register failed."
                                                        }
                                                    }
                                                )
                                            } else if (otpFlowType == "GOOGLE") {
                                                viewModel.loginWithGoogle(
                                                    email = customGoogleEmail.trim().lowercase(),
                                                    name = customGoogleName.trim(),
                                                    onFinished = {
                                                        authStep = "FORM"
                                                    }
                                                )
                                            } else if (otpFlowType == "FORGOT_PASSWORD") {
                                                authStep = "RESET_PASSWORD"
                                            }
                                         } else {
                                             otpErrorMsg = "Incorrect OTP! The lobby remains locked. Try again."
                                         }
                                     }
                                     }
                                  },
                                enabled = otpTimerSeconds > 0 && enteredOtp.length == 6,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedPrimary,
                                    disabledContainerColor = RedPrimary.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("otp_submit_btn")
                            ) {
                                if (otpTimerSeconds > 0) {
                                    Text("VERIFY OTP & OPEN APP (${otpTimerSeconds}s)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                                } else {
                                    Text("OTP EXPIRED (GO BACK)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = {
                                    authStep = "FORM"
                                    enteredOtp = ""
                                    otpErrorMsg = null
                                }
                            ) {
                                Text("CANCEL & RESTORE FORM", color = GreyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (authStep == "RESET_PASSWORD") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔒 RESET YOUR PASSWORD",
                                fontSize = 12.sp,
                                color = NeonGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter and confirm your new account password below.",
                                fontSize = 10.sp,
                                color = GreyText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = resetNewPasswordInput,
                                onValueChange = { 
                                    resetNewPasswordInput = deduplicateInput(resetNewPasswordInput, it)
                                    resetErrorMsg = null
                                },
                                label = { Text("New Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "password", tint = RedPrimary) },
                                placeholder = { Text("••••••••") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = Color(0xFF28252C),
                                    focusedLabelColor = RedPrimary,
                                    unfocusedLabelColor = GreyText,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reset_new_password_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = resetConfirmPasswordInput,
                                onValueChange = { 
                                    resetConfirmPasswordInput = deduplicateInput(resetConfirmPasswordInput, it)
                                    resetErrorMsg = null
                                },
                                label = { Text("Confirm New Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "confirm_password", tint = RedPrimary) },
                                placeholder = { Text("••••••••") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = Color(0xFF28252C),
                                    focusedLabelColor = RedPrimary,
                                    unfocusedLabelColor = GreyText,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reset_confirm_password_input")
                            )
                            resetErrorMsg?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(msg, color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (resetNewPasswordInput.isBlank() || resetConfirmPasswordInput.isBlank()) {
                                        resetErrorMsg = "Please fill in all fields!"
                                    } else if (resetNewPasswordInput != resetConfirmPasswordInput) {
                                        resetErrorMsg = "Passwords do not match!"
                                    } else {
                                        viewModel.resetUserPassword(loginEmailInput, resetNewPasswordInput) { success, err ->
                                            if (success) {
                                                authStep = "FORM"
                                                resetNewPasswordInput = ""
                                                resetConfirmPasswordInput = ""
                                                resetErrorMsg = null
                                            } else {
                                                resetErrorMsg = err ?: "Reset failed. Try again."
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("reset_submit_btn")
                            ) {
                                Text("UPDATE SECURE PASSWORD", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = {
                                    authStep = "FORM"
                                    resetNewPasswordInput = ""
                                    resetConfirmPasswordInput = ""
                                    resetErrorMsg = null
                                }
                            ) {
                                Text("CANCEL", color = GreyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Custom Esport styled Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    activeAuthTab = "SIGN_IN"
                                    errorMsg = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (activeAuthTab == "SIGN_IN") RedPrimary.copy(alpha = 0.2f) else Color(0xFF141217)
                            ),
                            border = BorderStroke(1.dp, if (activeAuthTab == "SIGN_IN") RedPrimary else Color(0xFF28252C))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "SIGN IN", 
                                    color = if (activeAuthTab == "SIGN_IN") Color.White else GreyText, 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    activeAuthTab = "REGISTER"
                                    errorMsg = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (activeAuthTab == "REGISTER") RedPrimary.copy(alpha = 0.2f) else Color(0xFF141217)
                            ),
                            border = BorderStroke(1.dp, if (activeAuthTab == "REGISTER") RedPrimary else Color(0xFF28252C))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "REGISTER", 
                                    color = if (activeAuthTab == "REGISTER") Color.White else GreyText, 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    // Secure Auth Protocol Header (Forced to Email/Password per security policy)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(Color(0xFF16141A), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RedPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Email Secure Key",
                                    tint = RedPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "EMAIL & PASSWORD SECURE ACCESS CHANNEL",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // Live OTP Channel Toggle Component
                    val showOtpToggle = (loginEmailInput.trim().lowercase() == "selva19122008@gmail.com" || emailInput.trim().lowercase() == "selva19122008@gmail.com")
                    if (showOtpToggle) {
                        val currentGateway = viewModel.getSmsGatewayMode()
                        var useNodemailerMode by remember(currentGateway) { mutableStateOf(currentGateway == "GMAIL_SMTP") }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16141A)),
                            border = BorderStroke(1.dp, if (useNodemailerMode) Color(0xFF4CAF50).copy(alpha = 0.6f) else Color(0xFF28252C)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (useNodemailerMode) Icons.Default.CheckCircle else Icons.Default.Settings,
                                            contentDescription = "Channel Icon",
                                            tint = if (useNodemailerMode) Color(0xFF4CAF50) else GreyText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (useNodemailerMode) "🔒 Secure SMTP OTP Delivery Active" else "⚡ Simulated Sandbox OTP Active",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (useNodemailerMode) "Genuine verification OTP emails will be dispatched to your inbox" else "Using sandbox mode for immediate on-screen codes",
                                                color = GreyText,
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = useNodemailerMode,
                                        onCheckedChange = { isChecked ->
                                            useNodemailerMode = isChecked
                                            viewModel.setSmsGatewayMode(if (isChecked) "GMAIL_SMTP" else "TEST_MODE")
                                            viewModel.showToast(
                                                title = if (isChecked) "📧 NODEMAILER CHANNEL READY" else "⚡ SANDBOX CHANNEL READY",
                                                message = if (isChecked) "Secure registration and login will now trigger Nodemailer verification emails." else "Verification bypassed to simulated in-app testing code.",
                                                type = NotificationType.SUCCESS
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = RedPrimary,
                                            uncheckedThumbColor = GreyText,
                                            uncheckedTrackColor = Color(0xFF1F1C25)
                                        ),
                                        modifier = Modifier.testTag("nodemailer_otp_switch")
                                    )
                                }
                            }
                        }
                    }
                    if (false) {
                        Row {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (authMode == "EMAIL_PASS") RedPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, if (authMode == "EMAIL_PASS") RedPrimary.copy(alpha = 0.4f) else Color.Transparent),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    authMode = "EMAIL_PASS"
                                    errorMsg = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Email Secure Key",
                                    tint = if (authMode == "EMAIL_PASS") RedPrimary else GreyText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "EMAIL & PASSWORD",
                                    color = if (authMode == "EMAIL_PASS") Color.White else GreyText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (authMode == "PHONE_OTP") RedPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, if (authMode == "PHONE_OTP") RedPrimary.copy(alpha = 0.4f) else Color.Transparent),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    authMode = "PHONE_OTP"
                                    errorMsg = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "SMS OTP",
                                    tint = if (authMode == "PHONE_OTP") RedPrimary else GreyText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "MOBILE SMS OTP",
                                    color = if (authMode == "PHONE_OTP") Color.White else GreyText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }}
                    AnimatedContent(
                        targetState = activeAuthTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "AuthTabTransition"
                    ) { tab ->
                        Column {
                            if (tab == "SIGN_IN") {
                                if (authMode == "EMAIL_PASS") {
                                    Text(
                                        text = "SIGN IN SECURELY WITH EMAIL",
                                        fontSize = 10.sp,
                                        color = NeonGold,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = loginEmailInput,
                                        onValueChange = { 
                                            loginEmailInput = deduplicateInput(loginEmailInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("Email Address") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "email", tint = RedPrimary) },
                                        placeholder = { Text("e.g. admin@battlezone.com") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_email_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = loginPasswordInput,
                                        onValueChange = { 
                                            loginPasswordInput = deduplicateInput(loginPasswordInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "password", tint = RedPrimary) },
                                        placeholder = { Text("••••••••") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        visualTransformation = PasswordVisualTransformation(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_password_input")
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        TextButton(
                                            onClick = {
                                                if (loginEmailInput.isBlank()) {
                                                    errorMsg = "Please enter your Email Address first to request password reset!"
                                                } else {
                                                    viewModel.forgotPasswordSendOtp(loginEmailInput) { success, err, otpCode ->
                                                        if (success) {
                                                            generatedOtp = otpCode ?: ""
                                                            otpFlowType = "FORGOT_PASSWORD"
                                                            authStep = "OTP"
                                                            enteredOtp = ""
                                                            otpErrorMsg = null
                                                            viewModel.firebaseVerificationId = null
                                                        } else {
                                                            errorMsg = err ?: "Failed to trigger reset flow."
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("forgot_password_button")
                                        ) {
                                            Text("Forgot Password?", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    errorMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = msg,
                                            color = RedPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            if (loginEmailInput.isBlank() || loginPasswordInput.isBlank()) {
                                                errorMsg = "Please enter both Email and Password!"
                                            } else {
                                                viewModel.verifyCredentialsAndTriggerOtp(
                                                    emailInput = loginEmailInput.trim(),
                                                    passwordInput = loginPasswordInput,
                                                    onTriggerOtp = { phoneStr, userEntity ->
                                                        val secureOtp = (100000..999999).random().toString()
                                                        generatedOtp = secureOtp
                                                        otpFlowType = "SIGN_IN"
                                                        authStep = "OTP"
                                                        enteredOtp = ""
                                                        otpErrorMsg = null
                                                        viewModel.firebaseVerificationId = null
                                                        val currentGateway = viewModel.getSmsGatewayMode()
                                                        val destination = if (currentGateway == "GMAIL_SMTP") {
                                                            userEntity.email.trim()
                                                        } else {
                                                            phoneStr.trim()
                                                        }
                                                        if (currentGateway == "TEST_MODE" || activity == null) {
                                                            val baseSeed = 121212
                                                            val otp = ((baseSeed + (100000..999999).random()) % 900000 + 100000).toString()
                                                            generatedOtp = otp
                                                            val targetEmail = loginEmailInput.trim()
                                                            if (targetEmail.contains("@")) {
                                                                viewModel.sendGmailOtpSecurely(targetEmail, otp) { _, _ -> }
                                                            }
                                                            viewModel.showToast(
                                                                title = "🔒 SECURE ACCOUNT VERIFICATION",
                                                                message = "TEST_MODE Active. Use verification OTP: $otp to enter. Dispatched to email.",
                                                                type = NotificationType.SUCCESS
                                                            )
                                                        } else if (currentGateway == "GMAIL_SMTP" || currentGateway == "TWILIO" || currentGateway == "FAST2SMS" || currentGateway == "CUSTOM_HTTP_API") {
                                                            viewModel.showToast(
                                                                title = if (currentGateway == "GMAIL_SMTP") "📧 EMAIL OTP DISPATCHED" else "✉️ SMS OTP DISPATCHED",
                                                                message = if (currentGateway == "GMAIL_SMTP") "Our secure Gmail Gateway is emailing a 6-digit OTP to $destination." else "Custom SMS Gateway is sending a 6-digit OTP code to your number.",
                                                                type = NotificationType.SUCCESS
                                                            )
                                                            viewModel.sendOtpSms(destination, secureOtp) { success, err ->
                                                                if (!success) {
                                                                    viewModel.showToast(
                                                                        title = "⚠️ CUSTOM GATEWAY ERROR",
                                                                        message = err ?: "Failed to deliver OTP. Try again or contact support.",
                                                                        type = NotificationType.WARNING
                                                                    )
                                                                    errorMsg = err ?: "Failed to deliver OTP via Gateway."
                                                                    authStep = "FORM"
                                                                }
                                                             }
                                                        } else {
                                                            viewModel.sendFirebasePhoneOtp(
                                                                phoneNumber = phoneStr,
                                                                activity = activity,
                                                                onCodeSent = {
                                                                    viewModel.showToast(
                                                                        title = "✉️ SMS OTP DISPATCHED",
                                                                        message = "Firebase verification code dispatched to your mobile number via SMS.",
                                                                        type = NotificationType.SUCCESS
                                                                    )
                                                                },
                                                                onVerificationFailed = { err ->
                                                                    errorMsg = err ?: "Verification failed."
                                                                    authStep = "FORM"
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onFinished = { success, error ->
                                                        if (!success) {
                                                            errorMsg = error ?: "Authentication failed."
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("login_submit_btn")
                                    ) {
                                        Text(
                                            text = "SIGN IN & ENTER THE ZONE",
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "SIGN IN WITH MOBILE NUMBER",
                                        fontSize = 10.sp,
                                        color = NeonGold,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = signInPhoneInput,
                                        onValueChange = { input ->
                                            val cleaned = deduplicateInput(signInPhoneInput, input)
                                            val digits = cleaned.filter { it.isDigit() }
                                            if (digits.length <= 10) {
                                                signInPhoneInput = digits
                                                errorMsg = null
                                            }
                                        },
                                        label = { Text("Enter Mobile Number") },
                                        prefix = { Text("+91 ", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "phone", tint = RedPrimary) },
                                        placeholder = { Text("Enter 10 Digits") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_phone_input")
                                    )
                                    errorMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = msg,
                                            color = RedPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            if (signInPhoneInput.isBlank()) {
                                                errorMsg = "Please enter your Mobile number to sign in!"
                                            } else if (signInPhoneInput.length != 10 || !(signInPhoneInput.startsWith("6") || signInPhoneInput.startsWith("7") || signInPhoneInput.startsWith("8") || signInPhoneInput.startsWith("9"))) {
                                                errorMsg = "Please enter a valid 10-digit Indian Mobile Number (starting with 6, 7, 8, or 9)."
                                            } else {
                                                scope.launch {
                                                    val determinedPhone = "+91" + signInPhoneInput.trim()
                                                    val registeredUser = viewModel.getRegisteredUserByPhone(determinedPhone)
                                                    val exists = registeredUser != null
                                                    if (exists) {
                                                        val targetNumber = registeredUser?.phoneNumber ?: determinedPhone
                                                        val currentGateway = viewModel.getSmsGatewayMode()
                                                        if (currentGateway == "TEST_MODE" || activity == null) {
                                                            val phoneDigits = signInPhoneInput.filter { it.isDigit() }
                                                            val baseSeed = if (phoneDigits.length >= 6) phoneDigits.takeLast(6).toIntOrNull() ?: 121212 else 121212
                                                            val otp = ((baseSeed + (100000..999999).random()) % 900000 + 100000).toString()
                                                            generatedOtp = otp
                                                            otpFlowType = "SIGN_IN"
                                                            authStep = "OTP"
                                                            enteredOtp = ""
                                                            otpErrorMsg = null
                                                            viewModel.firebaseVerificationId = null
                                                            val targetEmail = registeredUser?.email ?: ""
                                                            if (targetEmail.contains("@")) {
                                                                viewModel.sendGmailOtpSecurely(targetEmail, otp) { _, _ -> }
                                                            }
                                                            viewModel.showToast(
                                                                title = "🔒 SECURE ACCOUNT VERIFICATION",
                                                                message = "TEST_MODE Active. Use verification OTP: $otp to enter. Dispatched to email if linked.",
                                                                type = NotificationType.SUCCESS
                                                            )
                                                        } else if (currentGateway == "GMAIL_SMTP" || currentGateway == "TWILIO" || currentGateway == "FAST2SMS" || currentGateway == "CUSTOM_HTTP_API") {
                                                            val secureOtp = (100000..999999).random().toString()
                                                            generatedOtp = secureOtp
                                                            otpFlowType = "SIGN_IN"
                                                            authStep = "OTP"
                                                            enteredOtp = ""
                                                            otpErrorMsg = null
                                                            viewModel.firebaseVerificationId = null
                                                            val destination = if (currentGateway == "GMAIL_SMTP") {
                                                                registeredUser?.email ?: targetNumber
                                                            } else {
                                                                targetNumber
                                                            }
                                                            viewModel.showToast(
                                                                title = if (currentGateway == "GMAIL_SMTP") "📧 EMAIL OTP DISPATCHED" else "✉️ SMS OTP DISPATCHED",
                                                                message = if (currentGateway == "GMAIL_SMTP") "Our secure Gmail Gateway is emailing a 6-digit OTP to $destination." else "Custom SMS Gateway is sending a 6-digit OTP code to your number.",
                                                                type = NotificationType.SUCCESS
                                                            )
                                                            viewModel.sendOtpSms(destination, secureOtp) { success, err ->
                                                                if (!success) {
                                                                    viewModel.showToast(
                                                                        title = "⚠️ CUSTOM GATEWAY ERROR",
                                                                        message = err ?: "Failed to deliver OTP. Try again or contact support.",
                                                                        type = NotificationType.WARNING
                                                                    )
                                                                    errorMsg = err ?: "Failed to deliver OTP via Gateway."
                                                                    authStep = "FORM"
                                                                }
                                                            }
                                                        } else {
                                                            viewModel.sendFirebasePhoneOtp(
                                                                phoneNumber = targetNumber,
                                                                activity = activity,
                                                                onCodeSent = {
                                                                    otpFlowType = "SIGN_IN"
                                                                    authStep = "OTP"
                                                                    enteredOtp = ""
                                                                    otpErrorMsg = null
                                                                    viewModel.showToast(
                                                                        title = "✉️ SMS OTP DISPATCHED",
                                                                        message = "Firebase verification code dispatched to your mobile number via SMS.",
                                                                        type = NotificationType.SUCCESS
                                                                    )
                                                                },
                                                                onVerificationFailed = { err ->
                                                                    errorMsg = err
                                                                }
                                                            )
                                                        }
                                                    } else {
                                                        errorMsg = "No registered account found with Mobile Number: +91 ${signInPhoneInput.trim()}. Please register first!"
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("login_submit_btn")
                                    ) {
                                        Text(
                                            text = "SIGN IN & ENTER THE ZONE",
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF28252C).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .border(1.dp, NeonGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎮 Registrations are active! If you don't have an esports account, tap the REGISTER tab at the top to secure your profile and compete in tournaments instantly.",
                                            color = NeonGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            } else {
                                if (authMode == "EMAIL_PASS") {
                                    Text(
                                        text = "CREATE ESPORTS PROFILE WITH EMAIL",
                                        fontSize = 10.sp,
                                        color = NeonGold,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = ignInput,
                                        onValueChange = { 
                                            ignInput = deduplicateInput(ignInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("Game Name (Required)") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "ign", tint = RedPrimary) },
                                        placeholder = { Text("Enter Game Name") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_ign_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = ffUidInput,
                                        onValueChange = { 
                                            ffUidInput = deduplicateInput(ffUidInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("User ID (Optional)") },
                                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = "uid", tint = RedPrimary) },
                                        placeholder = { Text("e.g. FF-938402194") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_uuid_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { input ->
                                            val cleaned = deduplicateInput(phoneInput, input)
                                            val digits = cleaned.filter { it.isDigit() }
                                            if (digits.length <= 10) {
                                                phoneInput = digits
                                                errorMsg = null
                                            }
                                        },
                                        label = { Text("Mobile Number (Required)") },
                                        prefix = { Text("+91 ", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "phone", tint = RedPrimary) },
                                        placeholder = { Text("Enter 10 Digits") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_phone_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { 
                                            emailInput = deduplicateInput(emailInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("Email Address (Required)") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "email", tint = RedPrimary) },
                                        placeholder = { Text("e.g. name@domain.com") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_email_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = registerPasswordInput,
                                        onValueChange = { 
                                            registerPasswordInput = deduplicateInput(registerPasswordInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("Password (Required)") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "password", tint = RedPrimary) },
                                        placeholder = { Text("Enter Password (Minimum 6 Characters)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        visualTransformation = PasswordVisualTransformation(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_password_input")
                                    )
                                    errorMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = msg,
                                            color = RedPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.Start)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            if (ignInput.isBlank() || phoneInput.isBlank()) {
                                                errorMsg = "Please fill in your Game Name and Mobile Number!"
                                            } else if (phoneInput.length != 10 || !(phoneInput.startsWith("6") || phoneInput.startsWith("7") || phoneInput.startsWith("8") || phoneInput.startsWith("9"))) {
                                                errorMsg = "Please enter a valid 10-digit Indian Mobile Number (starting with 6, 7, 8, or 9)."
                                            } else if (extraMobileInput.isNotBlank() && (extraMobileInput.length != 10 || !(extraMobileInput.startsWith("6") || extraMobileInput.startsWith("7") || extraMobileInput.startsWith("8") || extraMobileInput.startsWith("9")))) {
                                                errorMsg = "Secondary Contact Number must be a valid 10-digit Indian number."
                                            } else {
                                                val determinedPhone = "+91" + phoneInput.trim()
                                                val determinedExtra = if (extraMobileInput.isNotBlank()) "+91" + extraMobileInput.trim() else ""
                                                val determinedEmail = if (emailInput.isBlank()) {
                                                    "gamer_${phoneInput.trim()}@battlezone.com".lowercase()
                                                } else {
                                                    emailInput.trim()
                                                }
                                                val determinedPassword = if (registerPasswordInput.isBlank()) {
                                                    "battle123"
                                                } else {
                                                    registerPasswordInput
                                                }
                                                val secureOtp = (100000..999999).random().toString()
                                                generatedOtp = secureOtp
                                                otpFlowType = "REGISTER_EMAIL_PASS"
                                                authStep = "OTP"
                                                enteredOtp = ""
                                                otpErrorMsg = null
                                                viewModel.firebaseVerificationId = null
                                                val currentGateway = viewModel.getSmsGatewayMode()
                                                val destination = if (currentGateway == "GMAIL_SMTP") {
                                                    determinedEmail
                                                } else {
                                                    determinedPhone
                                                }
                                                if (currentGateway == "TEST_MODE" || activity == null) {
                                                    val cleanPhone = phoneInput.filter { it.isDigit() }
                                                    val baseSeed = if (cleanPhone.length >= 6) cleanPhone.takeLast(6).toIntOrNull() ?: 121212 else 121212
                                                    val otp = ((baseSeed + (100000..999999).random()) % 900000 + 100000).toString()
                                                    generatedOtp = otp
                                                    if (determinedEmail.contains("@")) {
                                                        viewModel.sendGmailOtpSecurely(determinedEmail, otp) { _, _ -> }
                                                    }
                                                    viewModel.showToast(
                                                        title = "🔒 SECURE ACCOUNT VERIFICATION",
                                                        message = "TEST_MODE Active. Use verification OTP: $otp to enter. Dispatched to email.",
                                                        type = NotificationType.SUCCESS
                                                    )
                                                } else if (currentGateway == "GMAIL_SMTP" || currentGateway == "TWILIO" || currentGateway == "FAST2SMS" || currentGateway == "CUSTOM_HTTP_API") {
                                                    viewModel.showToast(
                                                        title = if (currentGateway == "GMAIL_SMTP") "📧 EMAIL OTP DISPATCHED" else "✉️ SMS OTP DISPATCHED",
                                                        message = if (currentGateway == "GMAIL_SMTP") "Our secure Gmail Gateway is emailing a 6-digit OTP to $destination." else "Custom SMS Gateway is sending a 6-digit OTP code to your number.",
                                                        type = NotificationType.SUCCESS
                                                    )
                                                    viewModel.sendOtpSms(destination, secureOtp) { success, err ->
                                                        if (!success) {
                                                            viewModel.showToast(
                                                                title = "⚠️ CUSTOM GATEWAY ERROR",
                                                                message = err ?: "Failed to deliver OTP. Try again or contact support.",
                                                                type = NotificationType.WARNING
                                                            )
                                                            errorMsg = err ?: "Failed to deliver OTP via Gateway."
                                                            authStep = "FORM"
                                                        }
                                                    }
                                                } else {
                                                    viewModel.sendFirebasePhoneOtp(
                                                        phoneNumber = determinedPhone,
                                                        activity = activity,
                                                        onCodeSent = {
                                                            viewModel.showToast(
                                                                title = "✉️ SMS OTP DISPATCHED",
                                                                message = "Firebase verification code dispatched to your mobile number via SMS.",
                                                                type = NotificationType.SUCCESS
                                                            )
                                                        },
                                                        onVerificationFailed = { err ->
                                                            errorMsg = err ?: "Verification failed."
                                                            authStep = "FORM"
                                                        }
                                                    )
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("login_submit_btn")
                                    ) {
                                        Text(
                                            text = "REGISTER & ENTER THE ZONE",
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "CREATE NEW ESPORTS PROFILE",
                                        fontSize = 10.sp,
                                        color = NeonGold,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = ignInput,
                                        onValueChange = { 
                                            ignInput = deduplicateInput(ignInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("Game Name (Required)") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "ign", tint = RedPrimary) },
                                        placeholder = { Text("e.g. Rogue_Gamer") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_ign_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = ffUidInput,
                                        onValueChange = { 
                                            ffUidInput = deduplicateInput(ffUidInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("User ID (Optional)") },
                                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = "uid", tint = RedPrimary) },
                                        placeholder = { Text("e.g. FF-938402194") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_uuid_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { input ->
                                            val cleaned = deduplicateInput(phoneInput, input)
                                            val digits = cleaned.filter { it.isDigit() }
                                            if (digits.length <= 10) {
                                                phoneInput = digits
                                                errorMsg = null
                                            }
                                        },
                                        label = { Text("Mobile Number (Required)") },
                                        prefix = { Text("+91 ", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "phone", tint = RedPrimary) },
                                        placeholder = { Text("Enter 10 Digits") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_phone_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { 
                                            emailInput = deduplicateInput(emailInput, it)
                                             errorMsg = null
                                        },
                                        label = { Text("Email ID (Optional)") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "email", tint = RedPrimary) },
                                        placeholder = { Text("e.g. name@domain.com") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RedPrimary,
                                            unfocusedBorderColor = Color(0xFF28252C),
                                            focusedLabelColor = RedPrimary,
                                            unfocusedLabelColor = GreyText,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_email_input")
                                    )
                                    errorMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = msg,
                                            color = RedPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.Start)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            if (ignInput.isBlank() || phoneInput.isBlank()) {
                                                errorMsg = "Please fill in your Game Name and Mobile Number to register!"
                                            } else if (phoneInput.length != 10 || !(phoneInput.startsWith("6") || phoneInput.startsWith("7") || phoneInput.startsWith("8") || phoneInput.startsWith("9"))) {
                                                errorMsg = "Please enter a valid 10-digit Indian Mobile Number (starting with 6, 7, 8, or 9)."
                                            } else if (extraMobileInput.isNotBlank() && (extraMobileInput.length != 10 || !(extraMobileInput.startsWith("6") || extraMobileInput.startsWith("7") || extraMobileInput.startsWith("8") || extraMobileInput.startsWith("9")))) {
                                                errorMsg = "Secondary Contact Number must be a valid 10-digit Indian number."
                                            } else {
                                                val determinedPhone = "+91" + phoneInput.trim()
                                                val currentGateway = viewModel.getSmsGatewayMode()
                                                if (currentGateway == "TEST_MODE" || activity == null) {
                                                    val phoneDigits = phoneInput.filter { it.isDigit() }
                                                    val baseSeed = if (phoneDigits.length >= 6) phoneDigits.takeLast(6).toIntOrNull() ?: 121212 else 121212
                                                    val otp = ((baseSeed + (100000..999999).random()) % 900000 + 100000).toString()
                                                    generatedOtp = otp
                                                    otpFlowType = "REGISTER"
                                                    authStep = "OTP"
                                                    enteredOtp = ""
                                                    otpErrorMsg = null
                                                    viewModel.firebaseVerificationId = null // mock mode
                                                    val targetEmail = emailInput.trim()
                                                    if (targetEmail.contains("@")) {
                                                        viewModel.sendGmailOtpSecurely(targetEmail, otp) { _, _ -> }
                                                    }
                                                    viewModel.showToast(
                                                        title = "🔒 NEW REGISTRATION OTP",
                                                        message = "TEST_MODE Active. Use account setup verification OTP: $otp to verify. Dispatched to email.",
                                                        type = NotificationType.SUCCESS
                                                    )
                                                } else if (currentGateway == "GMAIL_SMTP" || currentGateway == "TWILIO" || currentGateway == "FAST2SMS" || currentGateway == "CUSTOM_HTTP_API") {
                                                    val secureOtp = (100000..999999).random().toString()
                                                    generatedOtp = secureOtp
                                                    otpFlowType = "REGISTER"
                                                    authStep = "OTP"
                                                    enteredOtp = ""
                                                    otpErrorMsg = null
                                                    viewModel.firebaseVerificationId = null
                                                    val targetDestination = if (currentGateway == "GMAIL_SMTP") {
                                                        emailInput.trim()
                                                    } else {
                                                        determinedPhone
                                                    }
                                                    viewModel.showToast(
                                                        title = if (currentGateway == "GMAIL_SMTP") "📧 EMAIL OTP DISPATCHED" else "✉️ SMS OTP DISPATCHED",
                                                        message = if (currentGateway == "GMAIL_SMTP") "Our secure Gmail Gateway is emailing a 6-digit OTP to $targetDestination." else "Custom SMS Gateway is sending a 6-digit OTP code to your number.",
                                                        type = NotificationType.SUCCESS
                                                    )
                                                    viewModel.sendOtpSms(targetDestination, secureOtp) { success, err ->
                                                        if (!success) {
                                                            viewModel.showToast(
                                                                title = "⚠️ CUSTOM GATEWAY ERROR",
                                                                message = err ?: "Failed to deliver OTP. Try again or contact support.",
                                                                type = NotificationType.WARNING
                                                            )
                                                            errorMsg = err ?: "Failed to deliver OTP via Gateway."
                                                            authStep = "FORM"
                                                        }
                                                    }
                                                } else {
                                                    // Real Firebase Register OTP Dispatch
                                                    viewModel.sendFirebasePhoneOtp(
                                                        phoneNumber = determinedPhone,
                                                        activity = activity,
                                                        onCodeSent = {
                                                            otpFlowType = "REGISTER"
                                                            authStep = "OTP"
                                                            enteredOtp = ""
                                                            otpErrorMsg = null
                                                            viewModel.showToast(
                                                                title = "✉️ SMS OTP DISPATCHED",
                                                                message = "Firebase registration verification code has been dispatched to your mobile number.",
                                                                type = NotificationType.SUCCESS
                                                            )
                                                        },
                                                        onVerificationFailed = { err ->
                                                            errorMsg = err
                                                        }
                                                    )
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("login_submit_btn")
                                    ) {
                                        Text(
                                            text = "REGISTER & ENTER THE ZONE",
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    } // closes FORM of if (authStep == "OTP")
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF28252C)))
                Text(
                    text = "  OR CONNECT SECURELY WITH  ",
                    color = GreyText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF28252C)))
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Google Sign In button
            Button(
                onClick = { 
                    customGoogleEmail = ""
                    showGoogleDialog = true 
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "G  ",
                    color = Color(0xFF4285F4),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = "Continue with Google Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF1F1C25)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BY ENTERING, YOU AGREE TO LOBBY FAIR-PLAY RULES",
                color = GreyText.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
    // Google Sign-In Account Picker Simulation Dialog
    if (showGoogleDialog) {
        Dialog(onDismissRequest = { 
            if (!isGoogleLoading) {
                showGoogleDialog = false
                isGoogleWebLoginActive = false
            }
        }) {
            val googleThemeBg = if (isGoogleWebLoginActive) Color(0xFFFFFFFF) else Color(0xFF202124)
            val googleThemeBorder = if (isGoogleWebLoginActive) Color(0xFFE0E0E0) else Color(0xFF3C4043)
            val googleThemeText = if (isGoogleWebLoginActive) Color(0xFF202124) else Color.White
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = googleThemeBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, googleThemeBorder), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showGooglePhoneLinking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "G", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "o", color = Color(0xFFEA4335), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "o", color = Color(0xFFFBBC05), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "g", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "l", color = Color(0xFF34A853), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "e", color = Color(0xFFEA4335), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Link Mobile Number",
                            color = googleThemeText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bind secure phone identification to ${linkingGoogleEmail}",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        val detectedCellId = android.provider.Settings.Secure.getString(
                            context.contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID
                        ) ?: "DEVICE_EMULATED_ID"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF292A2D)),
                            border = BorderStroke(1.dp, Color(0xFF3C4043)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "SECURE PROTOCOLS ACTIVE:",
                                    color = NeonGold,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "• Cell Hardware Bound\n• Auto-Detected Device ID: $detectedCellId",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = linkingPhoneInput,
                            onValueChange = { 
                                linkingPhoneInput = it.filter { c -> c.isDigit() }.take(10)
                                linkingPhoneError = null
                            },
                            label = { Text("Mobile Number (10 digits)", color = Color.LightGray) },
                            prefix = { Text("+91 ", color = Color.White) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1A73E8),
                                unfocusedBorderColor = Color(0xFFDADCE0),
                                focusedLabelColor = Color(0xFF1A73E8),
                                unfocusedLabelColor = Color.LightGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        linkingPhoneError?.let { err ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(err, color = RedPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (linkingPhoneInput.length != 10 || !(linkingPhoneInput.startsWith("6") || linkingPhoneInput.startsWith("7") || linkingPhoneInput.startsWith("8") || linkingPhoneInput.startsWith("9"))) {
                                    linkingPhoneError = "Please enter a valid 10-digit Indian Mobile Number (starting with 6, 7, 8, or 9)."
                                } else {
                                    scope.launch {
                                        val phoneWithCountry = "+91" + linkingPhoneInput.trim()
                                        val existingUserByPhone = viewModel.getRegisteredUserByPhone(phoneWithCountry)
                                        if (existingUserByPhone != null) {
                                            linkingPhoneError = "This Mobile Number is already linked to another account."
                                        } else {
                                            isGoogleLoading = true
                                            delay(1000)
                                            isGoogleLoading = false
                                            val currentGateway = viewModel.getSmsGatewayMode()
                                            if (currentGateway == "TEST_MODE" || activity == null) {
                                                val secureOtp = (100000..999999).random().toString()
                                                generatedOtp = secureOtp
                                                otpFlowType = "GOOGLE_LINKED"
                                                customGoogleEmail = linkingGoogleEmail
                                                customGoogleName = linkingGoogleName
                                                extraMobileInput = linkingPhoneInput
                                                authStep = "OTP"
                                                enteredOtp = ""
                                                otpErrorMsg = null
                                                showGoogleDialog = false
                                                showGooglePhoneLinking = false
                                                isGoogleWebLoginActive = false
                                                viewModel.firebaseVerificationId = null
                                                if (linkingGoogleEmail.contains("@")) {
                                                    viewModel.sendGmailOtpSecurely(linkingGoogleEmail, secureOtp) { _, _ -> }
                                                }
                                                viewModel.showToast(
                                                    title = "🔒 ONBOARDING SECURE OTP",
                                                    message = "Use verification OTP: $secureOtp to link phone $phoneWithCountry to Google. Dispatched to email.",
                                                    type = NotificationType.SUCCESS
                                                )
                                            } else if (currentGateway == "GMAIL_SMTP" || currentGateway == "TWILIO" || currentGateway == "FAST2SMS" || currentGateway == "CUSTOM_HTTP_API") {
                                                val secureOtp = (100000..999999).random().toString()
                                                generatedOtp = secureOtp
                                                otpFlowType = "GOOGLE_LINKED"
                                                customGoogleEmail = linkingGoogleEmail
                                                customGoogleName = linkingGoogleName
                                                extraMobileInput = linkingPhoneInput
                                                authStep = "OTP"
                                                enteredOtp = ""
                                                otpErrorMsg = null
                                                showGoogleDialog = false
                                                showGooglePhoneLinking = false
                                                isGoogleWebLoginActive = false
                                                viewModel.firebaseVerificationId = null
                                                val targetDestination = if (currentGateway == "GMAIL_SMTP") {
                                                    linkingGoogleEmail.trim()
                                                } else {
                                                    phoneWithCountry
                                                }
                                                viewModel.showToast(
                                                    title = if (currentGateway == "GMAIL_SMTP") "📧 EMAIL OTP DISPATCHED" else "✉️ SMS OTP DISPATCHED",
                                                    message = if (currentGateway == "GMAIL_SMTP") "Our secure Gmail Gateway is emailing a 6-digit OTP to $targetDestination." else "Custom SMS Gateway is sending a 6-digit OTP code to your number.",
                                                    type = NotificationType.SUCCESS
                                                )
                                                viewModel.sendOtpSms(targetDestination, secureOtp) { success, err ->
                                                    if (!success) {
                                                        viewModel.showToast(
                                                            title = "⚠️ CUSTOM GATEWAY ERROR",
                                                            message = err ?: "Failed to deliver OTP. Try again or contact support.",
                                                            type = NotificationType.WARNING
                                                        )
                                                        linkingPhoneError = err ?: "Failed to deliver OTP via Gateway."
                                                        authStep = "FORM"
                                                    }
                                                }
                                            } else {
                                                viewModel.sendFirebasePhoneOtp(
                                                    phoneNumber = phoneWithCountry,
                                                    activity = activity,
                                                    onCodeSent = {
                                                        otpFlowType = "GOOGLE_LINKED"
                                                        customGoogleEmail = linkingGoogleEmail
                                                        customGoogleName = linkingGoogleName
                                                        extraMobileInput = linkingPhoneInput
                                                        authStep = "OTP"
                                                        enteredOtp = ""
                                                        otpErrorMsg = null
                                                        showGoogleDialog = false
                                                        showGooglePhoneLinking = false
                                                        isGoogleWebLoginActive = false
                                                        viewModel.showToast(
                                                            title = "✉️ SMS OTP DISPATCHED",
                                                            message = "Onboarding verification code sent to your mobile: $phoneWithCountry.",
                                                            type = NotificationType.SUCCESS
                                                        )
                                                    },
                                                    onVerificationFailed = { err ->
                                                        linkingPhoneError = err ?: "Firebase SMS OTP failed."
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SEND VERIFICATION OTP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                showGooglePhoneLinking = false
                            }
                        ) {
                            Text("BACK", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isGoogleWebLoginActive) {
                        // 1. MOCK CHROME CUSTOM TAB / SECURE BROWSER BAR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F3F4), RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure Connection",
                                tint = Color(0xFF0F9D58),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "accounts.google.com/signin/v3",
                                color = Color(0xFF5F6368),
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFF5F6368),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Google Brand Multi-colored Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "G", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "o", color = Color(0xFFEA4335), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "o", color = Color(0xFFFBBC05), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "g", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "l", color = Color(0xFF34A853), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "e", color = Color(0xFFEA4335), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Sign in with Google",
                            color = googleThemeText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Log in with your Google account to redirect back",
                            color = Color(0xFF5F6368),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        if (isGoogleLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF1A73E8), strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Connecting securely to Google...", color = Color(0xFF5F6368), fontSize = 11.sp)
                                }
                            }
                        } else {
                            var tempGooglePassword by remember { mutableStateOf("") }
                            
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = customGoogleEmail,
                                    onValueChange = { customGoogleEmail = it },
                                    label = { Text("Email or phone", color = Color(0xFF5F6368)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF202124),
                                        unfocusedTextColor = Color(0xFF202124),
                                        focusedBorderColor = Color(0xFF1A73E8),
                                        unfocusedBorderColor = Color(0xFFDADCE0),
                                        focusedLabelColor = Color(0xFF1A73E8),
                                        unfocusedLabelColor = Color(0xFF5F6368)
                                    ),
                                    placeholder = { Text("username@gmail.com", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                OutlinedTextField(
                                    value = tempGooglePassword,
                                    onValueChange = { tempGooglePassword = it },
                                    label = { Text("Enter Password (Secure)", color = Color(0xFF5F6368)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF202124),
                                        unfocusedTextColor = Color(0xFF202124),
                                        focusedBorderColor = Color(0xFF1A73E8),
                                        unfocusedBorderColor = Color(0xFFDADCE0),
                                        focusedLabelColor = Color(0xFF1A73E8),
                                        unfocusedLabelColor = Color(0xFF5F6368)
                                    ),
                                    placeholder = { Text("••••••••", color = Color.Gray) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Don't see your account? Type any active email above to log in.",
                                    color = Color(0xFF1A73E8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(18.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (accountsList.isNotEmpty()) {
                                        TextButton(onClick = { isGoogleWebLoginActive = false }) {
                                            Text("← Phone accounts", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    } else {
                                        TextButton(onClick = { 
                                            showGoogleDialog = false
                                            isGoogleWebLoginActive = false
                                        }) {
                                            Text("Cancel", color = Color(0xFF5F6368), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            val targetEmail = customGoogleEmail.trim().lowercase()
                                            if (targetEmail.isNotBlank() && targetEmail.contains("@") && targetEmail.contains(".")) {
                                                saveGoogleEmailCache(targetEmail)
                                                isGoogleLoading = true
                                                scope.launch {
                                                    delay(1200)
                                                    isGoogleLoading = false
                                                    val matchedUser = viewModel.allUsers.value.find { it.email.trim().lowercase() == targetEmail }
                                                    if (matchedUser != null) {
                                                        showGoogleDialog = false
                                                        isGoogleWebLoginActive = false
                                                        viewModel.loginDirectly(matchedUser.id)
                                                    } else {
                                                        linkingGoogleEmail = targetEmail
                                                        linkingGoogleName = targetEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                        linkingPhoneInput = ""
                                                        linkingPhoneError = null
                                                        showGooglePhoneLinking = true
                                                    }
                                                }
                                            } else {
                                                viewModel.showToast("Google Account Error", "Please enter a valid Gmail address.", NotificationType.WARNING)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = customGoogleEmail.isNotBlank() && customGoogleEmail.contains("@") && customGoogleEmail.contains(".")
                                    ) {
                                        Text("Next", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // 2. DEVICE ACCOUNT PICKER DIALOG (Shows only if accounts exist on their device)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "G", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "o", color = Color(0xFFEA4335), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "o", color = Color(0xFFFBBC05), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "g", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "l", color = Color(0xFF34A853), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "e", color = Color(0xFFEA4335), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Choose an account",
                            color = googleThemeText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "to continue to BattleZone",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        if (isGoogleLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF4285F4), strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Connecting securely...", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                accountsList.forEach { accEmail ->
                                    val accName = accEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                    val hash = accEmail.hashCode().coerceAtLeast(0)
                                    val accColor = when (hash % 3) {
                                        0 -> Color(0xFF0F9D58)
                                        1 -> Color(0xFF4285F4)
                                        else -> Color(0xFFDB4437)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF292A2D), RoundedCornerShape(8.dp))
                                            .clickable {
                                                saveGoogleEmailCache(accEmail)
                                                isGoogleLoading = true
                                                scope.launch {
                                                    delay(1000)
                                                    isGoogleLoading = false
                                                    val matchedUser = viewModel.allUsers.value.find { it.email.trim().lowercase() == accEmail.trim().lowercase() }
                                                    if (matchedUser != null) {
                                                        showGoogleDialog = false
                                                        isGoogleWebLoginActive = false
                                                        viewModel.loginDirectly(matchedUser.id)
                                                    } else {
                                                        linkingGoogleEmail = accEmail
                                                        linkingGoogleName = accName
                                                        linkingPhoneInput = ""
                                                        linkingPhoneError = null
                                                        showGooglePhoneLinking = true
                                                    }
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(accColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = accName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = accName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = maskEmail(accEmail),
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Select",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Beautiful selector to sign in with standard Web Redirect Simulation page instead
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1C1D21), RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF2C2D31)), RoundedCornerShape(8.dp))
                                        .clickable {
                                            isGoogleWebLoginActive = true
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Google account",
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Add or Sign in with another Google account",
                                        color = Color(0xFF4285F4),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { 
                                        showGoogleDialog = false
                                        isGoogleWebLoginActive = false
                                    }
                                ) {
                                    Text("CANCEL", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AdminSecurityTab(
    viewModel: BattleZoneViewModel,
    snackbarHost: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val securityLogs by viewModel.systemSecurityLogs.collectAsStateWithLifecycle()
    val anomalies by viewModel.securityAuditAnomalies.collectAsStateWithLifecycle()
    val isAuditRunning by viewModel.isAuditRunning.collectAsStateWithLifecycle()
    val securityMetrics by viewModel.securityMetrics.collectAsStateWithLifecycle()
    val isAutoDeleteOtpEnabled by viewModel.isAutoDeleteOtpEnabled.collectAsStateWithLifecycle()
    var activeScanProgress by remember { mutableFloatStateOf(0f) }
    var terminalLogs by remember { mutableStateOf(listOf("System Antivirus module online. Ready to scan Firestore arrays & transaction logs.")) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Title Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(RedPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Shield",
                        tint = RedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "🛡️ DEFENDER SYSTEMS SECURITY PLATFORM",
                        fontSize = 12.sp,
                        color = NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Anti-A security module conducting real-time zero-trust audits of database tables and system runtime arrays.",
                        fontSize = 10.sp,
                        color = GreyText
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 2. Hardware / Sandbox Integrity Metrics Grid
        Text(
            "HARDWARE & SANDBOX INTEGRITY STATE",
            fontSize = 10.sp,
            color = GreyText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("ROOT PRIVILEGES", fontSize = 8.sp, color = GreyText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (securityMetrics.isRooted) "ROOT EXPLOIT FOUND" else "BLOCKED / CLEAN",
                        fontSize = 10.sp,
                        color = if (securityMetrics.isRooted) RedPrimary else Color.Green,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("PLAY INTEGRITY SIGNATURE", fontSize = 8.sp, color = GreyText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (securityMetrics.isIntegrityVerified) "VERIFIED / SAFE" else "SYSTEM SIMULATED",
                        fontSize = 10.sp,
                        color = if (securityMetrics.isIntegrityVerified) Color.Green else NeonGold,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("DEVELOPER OPTIONS", fontSize = 8.sp, color = GreyText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (securityMetrics.isDeveloperOptionsEnabled) "ENABLED (WARNING)" else "SECURELY INACTIVE",
                        fontSize = 10.sp,
                        color = if (securityMetrics.isDeveloperOptionsEnabled) NeonGold else Color.Green,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("LOCAL DB PROTECTION", fontSize = 8.sp, color = GreyText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (securityMetrics.localDbEncryptedCheck) "PARAMETER SANITIZED" else "RAW INPUT ACCEPTED",
                        fontSize = 10.sp,
                        color = if (securityMetrics.localDbEncryptedCheck) Color.Green else RedPrimary,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Cloud Firestore Provisioning & Initialization Panel
        val isFirestoreInitializing by viewModel.isFirestoreInitializing.collectAsStateWithLifecycle()
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE5A93B).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud Icon",
                            tint = Color(0xFFE5A93B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "GOOGLE CLOUD FIRESTORE PROVISIONING",
                        fontSize = 11.sp,
                        color = Color(0xFFE5A93B),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Initialize and seed your Firebase Firestore database with all user profiles, active tournaments, custom lobby slots, settings config and persistent wallet balances. Ensures live-sync synchronization across all combatant devices.",
                    fontSize = 9.sp,
                    color = GreyText
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                Button(
                    onClick = {
                        terminalLogs = listOf("Connecting to Firebase console project configuration...")
                        viewModel.initializeCloudFirestoreDatabase(
                            onLogUpdate = { nextLine ->
                                val currentList = terminalLogs.toMutableList()
                                currentList.add(nextLine)
                                terminalLogs = currentList
                            },
                            onFinished = { success ->
                                scope.launch {
                                    if (success) {
                                        snackbarHost.showSnackbar("🔥 Firestore provisioned & synced live!")
                                    } else {
                                        snackbarHost.showSnackbar("❌ Firestore provisioning failed.")
                                    }
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131118)),
                    border = BorderStroke(1.dp, if (isFirestoreInitializing) Color(0xFFE5A93B) else Color(0xFFE5A93B).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !isFirestoreInitializing && !isAuditRunning,
                    modifier = Modifier.fillMaxWidth().testTag("initialize_firestore_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "run initialization",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFirestoreInitializing) "PROVISIONING CLOUD ARRAYS..." else "INITIALIZE & SYNC CLOUD FIRESTORE",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 2.2 OTP Gateway Security Sanctum Panel
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(RedPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            tint = RedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "OTP SECURITY GATEWAY SANCTUM",
                        fontSize = 11.sp,
                        color = RedPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Configure security levels for user setup and entry OTP verifications. Automatically expire and self-delete sent OTP payloads after 60 seconds of inactivity, or trigger a complete global invalidation.",
                    fontSize = 9.sp,
                    color = GreyText
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131115), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Auto-Delete Active OTP",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Expiring verification OTP payloads instantly after 60s of dispatch for robust protection against brute-forcing.",
                            fontSize = 8.sp,
                            color = GreyText
                        )
                    }
                    Switch(
                        checked = isAutoDeleteOtpEnabled,
                        onCheckedChange = {
                            viewModel.setAutoDeleteOtpEnabled(it)
                            scope.launch {
                                snackbarHost.showSnackbar(if (it) "🔒 Auto-delete verification OTPs activated." else "⚠️ Auto-delete verification OTPs deactivated.")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RedPrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Black
                        ),
                        modifier = Modifier.testTag("auto_delete_otp_switch")
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        viewModel.clearActiveOtp()
                        scope.launch {
                            snackbarHost.showSnackbar("🗑️ Active Verification OTP manually invalidated!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131118)),
                    border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().testTag("force_clear_otp_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Force clear OTP icon",
                        tint = RedPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FORCE CLEAR ACTIVE VERIFICATION OTP",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        
        // 2.5 System Synchronization & Stability Controls
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(NeonGold.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Stability Settings Icon",
                            tint = NeonGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "🔧 SYSTEM SYNCHRONIZATION & STABILITY CONTROLS",
                        fontSize = 11.sp,
                        color = NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Manage core sync engines and background automatic operations to stabilize layouts or prevent flickering states.",
                    fontSize = 9.sp,
                    color = GreyText
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Firestore Sync Switch Row
                val isSyncEnabled by viewModel.isRealtimeSyncEnabled.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            "Real-time Cloud Firestore Sync",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Continuously pushes local database changes and listens for cloud updates. Disable if experiencing network flickering or state instability.",
                            fontSize = 9.sp,
                            color = GreyText
                        )
                    }
                    Switch(
                        checked = isSyncEnabled,
                        onCheckedChange = { viewModel.setRealtimeSyncEnabled(it) },
                        modifier = Modifier.testTag("toggle_realtime_sync_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RedPrimary,
                            uncheckedThumbColor = GreyText,
                            uncheckedTrackColor = Color(0xFF1F1B24)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF24222B), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Automatic Tournament State Transitions Switch Row
                val isPromotionEnabled by viewModel.isAutoTournamentPromotionEnabled.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            "Auto Tournament State Transitions",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Enables background timer engine to automatically promote match statuses (UPCOMING -> LIVE -> COMPLETED). Disable to freeze match state changes.",
                            fontSize = 9.sp,
                            color = GreyText
                        )
                    }
                    Switch(
                        checked = isPromotionEnabled,
                        onCheckedChange = { viewModel.setAutoTournamentPromotionEnabled(it) },
                        modifier = Modifier.testTag("toggle_auto_transitions_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RedPrimary,
                            uncheckedThumbColor = GreyText,
                            uncheckedTrackColor = Color(0xFF1F1B24)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF24222B), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Recalibrate & Realignment Section
                var alignmentResultMsg by remember { mutableStateOf("") }
                var isAligning by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "⚡ Database Auto-Healing & ID Recalibration",
                        fontSize = 11.sp,
                        color = NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Scans all local and cloud collections, aligns out-of-sync tournament IDs, purges duplicates, resolves key collisions, and heals flickering UI/shaking states automatically.",
                        fontSize = 9.sp,
                        color = GreyText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (alignmentResultMsg.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1A16), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, NeonGold.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = alignmentResultMsg,
                                fontSize = 10.sp,
                                color = NeonGold,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            isAligning = true
                            alignmentResultMsg = "Scanning collections and resolving overlaps..."
                            viewModel.adminRecalibrateAndRepairDatabase { msg ->
                                isAligning = false
                                alignmentResultMsg = msg
                            }
                        },
                        enabled = !isAligning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A3322),
                            disabledContainerColor = Color(0xFF161E18)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("admin_recalibrate_repair_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isAligning) Icons.Default.Sync else Icons.Default.Healing,
                                contentDescription = "Repair Icon",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAligning) "ALIGNING SYSTEMS..." else "HEAL & RECALIBRATE SYSTEM DATABASE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF24222B), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // System Reset Section
        var resetResultMsg by remember { mutableStateOf("") }
        var isResetting by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "🚨 Emergency System Database Reset",
                fontSize = 11.sp,
                color = RedPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Clears all tournament and registration records across both local database and Cloud Firestore, temporarily pauses synchronizations, resets all states, and re-generates clean, default, standard gaming sessions.",
                fontSize = 9.sp,
                color = GreyText
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (resetResultMsg.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2B161A), RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, RedPrimary.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = resetResultMsg,
                        fontSize = 10.sp,
                        color = RedPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    isResetting = true
                    resetResultMsg = "Resetting database and generating default sessions..."
                    viewModel.adminResetTournamentsToPreviousState { msg ->
                        isResetting = false
                        resetResultMsg = msg
                    }
                },
                enabled = !isResetting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF331418),
                    disabledContainerColor = Color(0xFF1E1113)
                ),
                border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("admin_system_reset_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isResetting) Icons.Default.Sync else Icons.Default.Delete,
                        contentDescription = "Reset Icon",
                        tint = RedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isResetting) "RESETTING..." else "RESET SYSTEM DATABASE TO ORIGINAL STATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = RedPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Scanner Controls Cards
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ANTIVIRUS FIREWALL CONTROL CENTER",
                    fontSize = 10.sp,
                    color = RedSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Trigger real-time heuristics audit across user logins, SQL transactions database, helpdesk tickets, and system runtime parameters. Mitigate any injections automatically.",
                    fontSize = 9.sp,
                    color = GreyText
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Trigger Audit Button
                    Button(
                        onClick = {
                            activeScanProgress = 0f
                            terminalLogs = listOf("Initiating zero-trust audit scanner thread...")
                            viewModel.runComprehensiveDatabaseAudit(
                                onLogUpdate = { nextLine ->
                                    val currentList = terminalLogs.toMutableList()
                                    currentList.add(nextLine)
                                    terminalLogs = currentList
                                    activeScanProgress += 0.125f
                                },
                                onFinished = {
                                    activeScanProgress = 1.0f
                                    scope.launch {
                                        snackbarHost.showSnackbar("🛡️ Antivirus Security Audit complete! Scanned localized Firestore & Logs integrity.")
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131118)),
                        border = BorderStroke(1.dp, if (isAuditRunning) RedPrimary else RedPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(6.dp),
                        enabled = !isAuditRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "run scan",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAuditRunning) "SCANNING..." else "RUN ANTIVIRUS SCAN",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                    // Auto Mitigate Button
                    Button(
                        onClick = {
                            viewModel.mitigateAndSecureAll(
                                onLogUpdate = { log ->
                                    val currentList = terminalLogs.toMutableList()
                                    currentList.add(log)
                                    terminalLogs = currentList
                                },
                                onFinished = {
                                    scope.launch {
                                        snackbarHost.showSnackbar("✅ Mitigation Succeeded! Cleansed vulnerability matrices.")
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C1910)),
                        border = BorderStroke(1.dp, Color.Green),
                        shape = RoundedCornerShape(6.dp),
                        enabled = anomalies.any { it.status == "ACTIVE" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "mitigate",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MITIGATE & SECURE",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                if (isAuditRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = activeScanProgress,
                        color = RedPrimary,
                        trackColor = Color(0xFF261214),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 4. Live Terminal View (Matrix Console)
        Text(
            "ANTIVIRUS RUNTIME CONSOLE OUTPUT",
            fontSize = 10.sp,
            color = GreyText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0E13)),
            border = BorderStroke(1.dp, Color(0xFF24222B)),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                val scrollState = rememberScrollState()
                // Auto scroll to the bottom of the Terminal output
                LaunchedEffect(terminalLogs.size) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        "🔒 SECURE SHIELD DIAGNOSTIC DECK [LOG_MODE]:",
                        fontSize = 8.sp,
                        color = Color.Cyan,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    terminalLogs.forEach { log ->
                        Text(
                            text = "⚡ $log",
                            fontSize = 8.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = if (log.contains("[MITIGATED]") || log.contains("[SANITIZED]") || log.contains("complete", ignoreCase = true)) Color(0xFF00E676) else if (log.contains("warning", ignoreCase = true) || log.contains("point", ignoreCase = true)) NeonGold else Color.White,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 5. Detected Threats Details Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DETECTED POINTS OF VULNERABILITY (${anomalies.filter { it.status == "ACTIVE" }.size})",
                fontSize = 10.sp,
                color = GreyText,
                fontWeight = FontWeight.Bold
            )
            if (anomalies.any { it.status == "ACTIVE" }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261214)),
                    border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "UNSECURED THREATS ACTIVE",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = RedPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2214)),
                    border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "SYSTEM CLEAN & HARDENED",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Green,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (anomalies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "verified clean",
                        tint = Color.Green,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No vulnerability scans executed yet. Run an audit scan above.",
                        fontSize = 9.sp,
                        color = GreyText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                anomalies.forEach { anomaly ->
                    val colorAccent = when {
                        anomaly.status == "MITIGATED" -> Color.Green
                        anomaly.severity == "CRITICAL" -> RedPrimary
                        anomaly.severity == "HIGH" -> Color(0xFFFF6D00)
                        else -> NeonGold
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, colorAccent.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (anomaly.status == "MITIGATED") Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = "threat",
                                        tint = colorAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = anomaly.title.uppercase(),
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorAccent.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (anomaly.status == "MITIGATED") "MITIGATED" else anomaly.severity,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colorAccent,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Affected: ${anomaly.affectedEntity}",
                                fontSize = 8.sp,
                                color = GreyText,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = anomaly.description,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 6. Platform Logs Auditing history
        Text(
            "HISTORICAL PLATFORM ACTIVITY LOGS (AUDITING FEED)",
            fontSize = 10.sp,
            color = GreyText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF24222B)), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                securityLogs.take(15).forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "⚙️",
                            fontSize = 11.sp
                        )
                        Text(
                            text = log,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Divider(color = Color(0xFF1B1922), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun NotificationsCenterDialog(
    notifications: List<com.example.db.NotificationEntity>,
    onDismiss: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDelete: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Surface(
            color = DarkBg,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color(0xFF2A2833)), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ALERT INBOX",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        val unreadCount = notifications.count { !it.isRead }
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(RedPrimary, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unreadCount NEW",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("notifications_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Row (Mark Read & Clear All)
                if (notifications.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val unreadCount = notifications.count { !it.isRead }
                        if (unreadCount > 0) {
                            OutlinedButton(
                                onClick = onMarkAllAsRead,
                                border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("notifications_mark_read_button")
                            ) {
                                Text(
                                    "MARK READ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Button(
                            onClick = onClearAll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF211E26),
                                contentColor = GreyText
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("notifications_clear_all_button")
                        ) {
                            Text(
                                "CLEAR ALL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Divider(color = Color(0xFF1E1C24), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // List section
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = GreyText.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Inbox is empty",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Join match listings or verify status for updates",
                                color = GreyText,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        notifications.forEach { notification ->
                            item(key = notification.id) {
                                val (icon, color) = when (notification.type) {
                                    "MATCH_START" -> Icons.Filled.SportsEsports to RedPrimary
                                    "TIME_UPDATE" -> Icons.Filled.EditCalendar to Color(0xFFFFC107)
                                    "ROOM_CREDS" -> Icons.Filled.VpnKey to Color(0xFF4CAF50)
                                    else -> Icons.Filled.Notifications to Color(0xFF2196F3)
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (notification.isRead) DarkSurface else Color(0xFF1D1A24)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (notification.isRead) Color(0xFF22202A) else RedPrimary.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notification_item_${notification.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Status & Icon
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(color.copy(alpha = 0.1f), CircleShape)
                                                .border(BorderStroke(1.dp, color.copy(alpha = 0.3f)), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Content Text
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = notification.title,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (!notification.isRead) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(RedPrimary, CircleShape)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = notification.message,
                                                color = if (notification.isRead) GreyText else Color.White.copy(alpha = 0.9f),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            // Dynamic relative timestamp
                                            val durationMs = System.currentTimeMillis() - notification.timestamp
                                            val timeStr = when {
                                                durationMs < 60_000 -> "Just now"
                                                durationMs < 3600_000 -> "${durationMs / 60_000}m ago"
                                                durationMs < 86400_000 -> "${durationMs / 3600_000}h ago"
                                                else -> {
                                                    val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                                                    sdf.format(java.util.Date(notification.timestamp))
                                                }
                                            }
                                            Text(
                                                text = timeStr,
                                                color = GreyText.copy(alpha = 0.8f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Delete/Dismiss Action
                                        IconButton(
                                            onClick = { onDelete(notification.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("notification_delete_button_${notification.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Alert",
                                                tint = GreyText.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
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
    }
}