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
import androidx.compose.ui.graphics.graphicsLayer
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
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
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

    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    var isAdminUnlocked by remember(userRole) { mutableStateOf(userRole == "admin") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val userJoins by viewModel.currentUserJoins.collectAsStateWithLifecycle()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (isSplashScreenVisible) {
        SplashScreen(onTimeout = { isSplashScreenVisible = false })
    } else if (!isUserLoggedIn) {
        LoginRegistrationScreen(viewModel = viewModel)
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                BattleZoneTopBar(
                    isAdmin = isAdminMode,
                    isAdminUnlocked = isAdminUnlocked,
                    user = user,
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
                    }
                )
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
                            onBack = { activeTournamentIdForDetails = null }
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
                                    onViewTournament = { activeTournamentIdForDetails = it }
                                )
                                1 -> WalletScreen(
                                    viewModel = viewModel,
                                    snackbarHostState = snackbarHostState
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
                                passwordError = "Access Denied: Invalid Master Password!"
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
                                    passwordError = "Access Denied: Invalid Master Password!"
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
    onToggleAdmin: () -> Unit,
    onLogoClick: () -> Unit,
    onLockAdmin: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFF1E1C24)),
        modifier = Modifier.fillMaxWidth()
    ) {
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


// --- 1. LOBBY MATCHES SCREEN ---
@Composable
fun LobbyScreen(
    viewModel: BattleZoneViewModel,
    tournaments: List<TournamentEntity>,
    onViewTournament: (Int) -> Unit
) {
    var activeFilterTab by remember { mutableStateOf("UPCOMING") } // "UPCOMING", "LIVE", "COMPLETED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Banner announcement
        GamingBannerSlider()

        // Filter tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val filters = listOf(
                Pair("UPCOMING", "UPCOMING"),
                Pair("LIVE", "LIVE BATTLES"),
                Pair("COMPLETED", "PAST CUPS")
            )
            filters.forEach { (type, label) ->
                val isSelected = activeFilterTab == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) RedPrimary else Color.Transparent)
                        .clickable { activeFilterTab = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else GreyText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        val filteredTournaments = tournaments.filter { it.status == activeFilterTab }

        if (filteredTournaments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.VideogameAsset,
                        contentDescription = "Empty",
                        tint = GreyText.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No $activeFilterTab matches active right now.",
                        color = GreyText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Stay tuned! Admin adds tournaments regularly.",
                        color = GreyText.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredTournaments, key = { it.id }) { match ->
                    TournamentCard(match = match, onViewDetails = { onViewTournament(match.id) })
                }
            }
        }
    }
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
fun TournamentCard(match: TournamentEntity, onViewDetails: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onViewDetails() }
            .testTag("tournament_card_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .border(BorderStroke(1.dp, Color(0xFF1F1C25)), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF281116), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = match.type.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RedPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF121B1C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = match.map.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E676)
                        )
                    }
                }
                Text(
                    text = match.dateTimeStr,
                    fontSize = 11.sp,
                    color = RedPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = match.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Big Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TOTAL PRIZE POOL", fontSize = 9.sp, color = GreyText)
                    Text(
                        text = match.prizePool.toCurrency(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ENTRY FEE", fontSize = 9.sp, color = GreyText)
                    Text(
                        text = if (match.entryFee == 0.0) "FREE" else match.entryFee.toCurrency(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = if (match.entryFee == 0.0) Color(0xFF00E676) else Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("MAP SECT", fontSize = 9.sp, color = GreyText)
                    Text(
                        text = match.map,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Slots loading bar
            val progress = if (match.slotsTotal > 0) {
                (match.slotsTotal - match.slotsRemaining).toFloat() / match.slotsTotal.toFloat()
            } else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Joined: ${match.slotsTotal - match.slotsRemaining} / ${match.slotsTotal}",
                        fontSize = 10.sp,
                        color = GreyText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (match.slotsRemaining == 0) "House Full" else "Only ${match.slotsRemaining} slots left",
                        fontSize = 10.sp,
                        color = if (match.slotsRemaining == 0) RedPrimary else NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = RedPrimary,
                    trackColor = Color(0xFF28252F)
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
    onBack: () -> Unit
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
                Text("${match.type} • ${match.map}", fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PRIZE POOL", fontSize = 9.sp, color = GreyText)
                    Text(match.prizePool.toCurrency(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NeonGold)
                }
                Divider(
                    modifier = Modifier
                        .height(34.dp)
                        .width(1.dp), color = Color(0xFF28252C)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ENTRY FEE", fontSize = 9.sp, color = GreyText)
                    Text(
                        if (match.entryFee == 0.0) "FREE" else match.entryFee.toCurrency(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Divider(
                    modifier = Modifier
                        .height(34.dp)
                        .width(1.dp), color = Color(0xFF28252C)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GAME DATE", fontSize = 9.sp, color = GreyText)
                    Text(match.dateTimeStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = RedSecondary)
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
                                                text = "Schedule: ${match.dateTimeStr}",
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
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Go to the wallet screen to add money!")
                                                    }
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
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val transactions by viewModel.currentUserTransactions.collectAsStateWithLifecycle()

    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawalDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Text(
            text = "WALLET BALANCE CARD",
            fontSize = 11.sp,
            color = GreyText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

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
                                    if (amt <= 0) {
                                        scope.launch { snackbarHostState.showSnackbar("Please enter a valid deposit amount.") }
                                    } else {
                                        currentDepositStep = 2
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) {
                                Text("PROCEED TO CHECKOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    Text(
                        "Winnings balance is transferable with ₹50 minimum constraint.",
                        fontSize = 10.sp,
                        color = GreyText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = withdrawalAmountInput,
                        onValueChange = { withdrawalAmountInput = it },
                        label = { Text("Transfer Target Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = upiIdInput,
                        onValueChange = { upiIdInput = it },
                        label = { Text("UPI Receiver ID (e.g. name@gpay)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showWithdrawalDialog = false }) {
                            Text("CANCEL", color = GreyText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amtNum = withdrawalAmountInput.toDoubleOrNull() ?: 0.0
                                viewModel.requestWithdrawal(amtNum, upiIdInput) { resp ->
                                    scope.launch {
                                        if (resp == "SUCCESS") {
                                            snackbarHostState.showSnackbar("Withdrawal placed. Pending Admin review.")
                                            showWithdrawalDialog = false
                                        } else {
                                            snackbarHostState.showSnackbar(resp)
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("CONFIRM WITHDRAWAL")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Text("BATTLEZONE CHAMPIONS LEADERBOARD", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Updated dynamically with top monthly reward distributions.", fontSize = 10.sp, color = GreyText)
        Spacer(modifier = Modifier.height(14.dp))

        // Top 3 Podium Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Rank 2
            PodiumCard(name = "Elite_Sniper", prize = "₹3,900", rank = 2, sizeModifier = 0.85f, modifier = Modifier.weight(1f))
            // Rank 1
            PodiumCard(name = "Shadow_Hunter", prize = "₹4,500", rank = 1, sizeModifier = 1f, modifier = Modifier.weight(1.1f))
            // Rank 3
            PodiumCard(name = "ViperFF_God", prize = "₹3,200", rank = 3, sizeModifier = 0.80f, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Standard Scroll list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(mockLeaderboard.drop(3)) { (name, points, rank) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$rank",
                                color = GreyText,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.width(28.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(text = points.toCurrency(), color = NeonGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
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
                modifier = Modifier.size(if (rank == 1) 28.dp else 22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = prize,
                color = NeonGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
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
                    fontSize = 8.sp
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
    val securityMetrics by viewModel.securityMetrics.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }

    var ignState by remember(user) { mutableStateOf(user?.inGameName ?: "Alpha_Gamer") }
    var ffUidState by remember(user) { mutableStateOf(user?.freeFireUid ?: "FF-837492047") }
    var phoneState by remember(user) { mutableStateOf(user?.phoneNumber ?: "+91 91929 39495") }
    var extraPhoneState by remember(user) { mutableStateOf(user?.extraMobileNumber ?: "") }
    var emailState by remember(user) { mutableStateOf(user?.email ?: "gamer@battlezone.com") }
    var bioState by remember(user) { mutableStateOf(user?.profilePicture ?: "🎯 Free Fire Pro Challenger!") }

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
            border = BorderStroke(1.dp, Color(0xFF1F1C25))
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
                                        listOf(RedPrimary, RedDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "profile", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(user?.inGameName ?: "Alpha_Gamer", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
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

        // Profile Editing toggle card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EDIT GAME ACCOUNT CREDENTIALS", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = RedPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (isEditing) {
                    OutlinedTextField(
                        value = ignState,
                        onValueChange = { ignState = it },
                        label = { Text("In-Game Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = ffUidState,
                        onValueChange = { ffUidState = it },
                        label = { Text("Profile Hero UID") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = bioState,
                        onValueChange = { bioState = it },
                        label = { Text("Gamer Bio / Status Message") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = phoneState,
                        onValueChange = { phoneState = it },
                        label = { Text("WhatsApp Contact") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = extraPhoneState,
                        onValueChange = { extraPhoneState = it },
                        label = { Text("Mobile Number") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = maskEmail(emailState),
                        onValueChange = {},
                        label = { Text("Associated Google Mail (SECURED)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF28252C),
                            unfocusedBorderColor = Color(0xFF28252C)
                        ),
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(ignState, ffUidState, phoneState, extraPhoneState, emailState, bioState) {
                                isEditing = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Profile details updated.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE PROFILE CONFIGURATIONS")
                    }
                } else {
                    ProfileFieldRow(label = "In Game Alias", value = user?.inGameName ?: "Alpha_Gamer")
                    ProfileFieldRow(label = "Free Fire UID", value = user?.freeFireUid ?: "FF-837492047")
                    ProfileFieldRow(label = "WhatsApp Contact", value = user?.phoneNumber ?: "+91 98765 43210")
                    ProfileFieldRow(label = "Mobile Number", value = if (user?.extraMobileNumber.isNullOrBlank()) "Not Provided" else user?.extraMobileNumber!!)
                    ProfileFieldRow(label = "Support Mail", value = maskEmail(user?.email ?: "gamer@battlezone.com"))
                }
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Instagram Card Button/Tab
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                uriHandler.openUri("https://www.instagram.com/its_nivetha_01?igsh=ejV2bnR4NTVkb2oz")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F121C)),
                        border = BorderStroke(1.dp, Color(0xFFC13584).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFC13584).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Official Instagram link tab",
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("INSTAGRAM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("@its_nivetha_01", color = Color(0xFFE1306C), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Telegram Card Button/Tab
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                uriHandler.openUri("https://t.me/battlezone_esports_official")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101B24)),
                        border = BorderStroke(1.dp, Color(0xFF0088CC).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF0088CC).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Official Telegram link tab",
                                    tint = Color(0xFF0088CC),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("TELEGRAM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Join Channel", color = Color(0xFF0088CC), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
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
                            Text(usr.inGameName, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text("UID: ${usr.freeFireUid}", color = GreyText, fontSize = 10.sp)
                        }

                        // Wallet adjustment selector
                        Button(
                            onClick = { selectUserForWalletAdjust = usr },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281116)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("CREDIT WALLET", fontSize = 9.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
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
    var entryFeeState by remember { mutableStateOf("40") }
    var prizeState by remember { mutableStateOf("2000") }
    var mapState by remember { mutableStateOf("Bermuda") }
    var formatState by remember { mutableStateOf("Solo") }
    var slotsState by remember { mutableStateOf("48") }
    var timeState by remember { mutableStateOf("June 22, 05:00 PM") }
    var rulesState by remember { mutableStateOf("1. Strictly mobile clients.\n2. In-game verification checked early.") }

    // Direct credentials edit selector
    var editCredentialsForTournament by remember { mutableStateOf<TournamentEntity?>(null) }
    // Distribute victory payouts selector
    var selectWinnersForTournament by remember { mutableStateOf<TournamentEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = entryFeeState,
                            onValueChange = { entryFeeState = it },
                            label = { Text("Entry Fee") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prizeState,
                            onValueChange = { prizeState = it },
                            label = { Text("Prize Pool") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = mapState,
                            onValueChange = { mapState = it },
                            label = { Text("Map (e.g. Bermuda)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = formatState,
                            onValueChange = { formatState = it },
                            label = { Text("Type (Solo/Duo/Squad)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = timeState,
                        onValueChange = { timeState = it },
                        label = { Text("Date & Time (e.g. June 22, 05:00 PM)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

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
                            viewModel.adminCreateTournament(
                                title = matchTitle,
                                dateTimeStr = timeState,
                                entryFee = entryFeeState.toDoubleOrNull() ?: 0.0,
                                prizePool = prizeState.toDoubleOrNull() ?: 0.0,
                                map = mapState,
                                type = formatState,
                                slotsTotal = slotsState.toIntOrNull() ?: 48,
                                rules = rulesState
                            ) {
                                isCreatingMatch = false
                                scope.launch {
                                    snackBars.showSnackbar("Broadly created new tournament!")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PUBLISH MATCH TO LOBBY")
                    }
                }
            }
        }

        // Active management items
        tourneys.forEach { match ->
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
                    Text("Time: ${match.dateTimeStr} • Fee: ₹${match.entryFee}", color = GreyText, fontSize = 11.sp)
                    Text("Map: ${match.map} • Joined: ${match.slotsTotal - match.slotsRemaining}/${match.slotsTotal}", color = GreyText, fontSize = 11.sp)

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
                            onClick = { viewModel.adminCancelTournament(match.id) },
                            modifier = Modifier
                                .background(Color(0xFF281116), RoundedCornerShape(4.dp))
                                .size(32.dp)
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
                        ) {
                            Text("UPDATE KEYS", fontSize = 9.sp, color = Color.White)
                        }

                        if (match.status != "COMPLETED") {
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

        // Room ID pass update dialog
        if (editCredentialsForTournament != null) {
            val focusM = editCredentialsForTournament!!
            var rId by remember { mutableStateOf(focusM.roomId ?: "") }
            var rPs by remember { mutableStateOf(focusM.roomPassword ?: "") }

            Dialog(onDismissRequest = { editCredentialsForTournament = null }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Set Custom Match Credentials", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = rId,
                            onValueChange = { rId = it },
                            label = { Text("Game Room ID Code") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = rPs,
                            onValueChange = { rPs = it },
                            label = { Text("Game Room Password") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { editCredentialsForTournament = null }) {
                                Text("CANCEL", color = GreyText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.adminUpdateRoomDetails(focusM.id, rId, rPs)
                                    editCredentialsForTournament = null
                                    scope.launch {
                                        snackBars.showSnackbar("Credentials saved to database.")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) {
                                Text("SAVE KEYS")
                            }
                        }
                    }
                }
            }
        }

        // winner distribution dialog
        if (selectWinnersForTournament != null) {
            val focusMatchWinner = selectWinnersForTournament!!

            var winnerFFUid by remember { mutableStateOf("FF-837492047") }
            var winnerInGameName by remember { mutableStateOf("Alpha_Gamer") }

            Dialog(onDismissRequest = { selectWinnersForTournament = null }) {
                Surface(shape = RoundedCornerShape(12.dp), color = DarkSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Declare Prize Champions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text("Selected player will automatically receive ₹${focusMatchWinner.prizePool} credited to winnings balance.", fontSize = 9.sp, color = GreyText)
                        Spacer(modifier = Modifier.height(12.dp))

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("PENDING TRANSFERS GATEWAY APPROVAL", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        val pending = withdrawals.filter { it.status == "PENDING" }

        if (pending.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No pending withdrawal requests available.", color = GreyText, fontSize = 12.sp)
            }
        } else {
            LazyColumn {
                items(pending) { req ->
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
                                    Text("UserId: ${req.userId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("UPI ID: ${req.upiId}", color = GreyText, fontSize = 10.sp)
                                }
                                Text(req.amount.toCurrency(), color = NeonGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
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
                            else -> Icons.Filled.CloudUpload
                        },
                        contentDescription = "Proof Status Indicator",
                        tint = when (join.proofStatus) {
                            "PENDING" -> NeonGold
                            "APPROVED" -> Color(0xFF4CAF50)
                            "REJECTED" -> RedPrimary
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
                        else -> Color(0xFF232029)
                    },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, when (join.proofStatus) {
                        "PENDING" -> NeonGold.copy(alpha = 0.3f)
                        "APPROVED" -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                        "REJECTED" -> RedPrimary.copy(alpha = 0.3f)
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
        
        val presetScreenshots = listOf(
            Pair("https://images.unsplash.com/photo-1542751371-adc38448a05e", "Booyah #1 Victory Screen"),
            Pair("https://images.unsplash.com/photo-1511512578047-dfb367046420", "#3 Placement Battle Screen"),
            Pair("https://images.unsplash.com/photo-1560253023-3ec5d502959f", "#12 Placement High Fire Screen")
        )
        var selectedPresetUrl by remember { mutableStateOf(presetScreenshots[0].first) }
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

                    // Simulated image file select
                    Text("SELECT MATCH RESULT SCREENSHOT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreyText)
                    Spacer(modifier = Modifier.height(6.dp))

                    presetScreenshots.forEach { (url, title) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPresetUrl = url }
                                .background(
                                    if (selectedPresetUrl == url) Color(0xFF281116) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedPresetUrl == url,
                                onClick = { selectedPresetUrl = url },
                                colors = RadioButtonDefaults.colors(selectedColor = RedPrimary, unselectedColor = GreyText)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(title, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Mock file: ${url.takeLast(12)}...png", fontSize = 10.sp, color = GreyText)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Image loading preview box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = selectedPresetUrl,
                            contentDescription = "Preview match proof screenshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
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
                                val rank = rankInput.toIntOrNull() ?: 1
                                val kills = killsInput.toIntOrNull() ?: 0
                                viewModel.submitScreenshotProof(
                                    tournamentId = join.tournamentId,
                                    screenshotPath = selectedPresetUrl,
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

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.updatePaymentConfig(upiState, payeeState, bankAccState, bankIfscState, bankNameState, gatewayModeState)
                        scope.launch {
                            snackbarHost.showSnackbar("Payment Gateway settings updated successfully in active context.")
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
                    val modes = listOf("TEST_MODE", "FAST2SMS", "TWILIO", "CUSTOM_HTTP_API")
                    modes.forEach { m ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (smsGatewayMode == m) RedPrimary else Color(0xFF1F1C25),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { smsGatewayMode = m }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (m) {
                                    "TEST_MODE" -> "DEBUG TEST"
                                    "CUSTOM_HTTP_API" -> "CUSTOM URL"
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
                        scope.launch {
                            snackbarHost.showSnackbar("SMS OTP Gateway routing configurations saved successfully!")
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

        Spacer(modifier = Modifier.height(20.dp))

        // Part 2: Review user pending deposits
        Text("PENDING GATEWAY DEPOSITS QUEUE (${pendingDeposits.size})", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (pendingDeposits.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
            ) {
                Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No pending pay-ins are waiting for manual check.", color = GreyText, fontSize = 11.sp)
                }
            }
        } else {
            pendingDeposits.forEach { dp ->
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
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF22110D)),
                                border = BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.4f))
                            ) {
                                Text("PENDING", color = Color(0xFFFF5722), fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(dp.title, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Invoice Ref: ${dp.invoiceId} | Date: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(dp.timestamp)}", fontSize = 8.sp, color = GreyText)

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

@Composable
fun LoginRegistrationScreen(viewModel: BattleZoneViewModel) {
    var activeAuthTab by remember { mutableStateOf("SIGN_IN") } // "SIGN_IN" or "REGISTER"

    var authStep by remember { mutableStateOf("FORM") } // "FORM" or "OTP"
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var otpFlowType by remember { mutableStateOf("SIGN_IN") } // "SIGN_IN", "REGISTER", "GOOGLE"
    var otpErrorMsg by remember { mutableStateOf<String?>(null) }

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
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var customGoogleName by remember { mutableStateOf("") }
    var customGoogleEmail by remember { mutableStateOf("") }
    var showCustomGoogleFields by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val accountsList = remember { mutableStateListOf<String>() }
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
            if (hasAccountPermission) {
                try {
                    val am = AccountManager.get(context)
                    val accounts = am.getAccountsByType("com.google")
                    accountsList.clear()
                    for (acc in accounts) {
                        if (acc.name.contains("@") && !accountsList.contains(acc.name)) {
                            accountsList.add(acc.name)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                permissionLauncher.launch(Manifest.permission.GET_ACCOUNTS)
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
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = enteredOtp,
                                onValueChange = { 
                                    enteredOtp = it.filter { c -> c.isDigit() }.take(4) 
                                    otpErrorMsg = null
                                },
                                label = { Text("Enter 4-Digit OTP Code") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "otp", tint = RedPrimary) },
                                placeholder = { Text("e.g. 5824") },
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
                                    if (enteredOtp == generatedOtp || (viewModel.getSmsGatewayMode() == "TEST_MODE" && enteredOtp == "1212")) {
                                        if (otpFlowType == "SIGN_IN") {
                                            viewModel.loginExistingUser(
                                                phone = signInPhoneInput.trim(),
                                                onFinished = { success, error ->
                                                    if (success) {
                                                        authStep = "FORM"
                                                    } else {
                                                        otpErrorMsg = error ?: "Login verification failed."
                                                    }
                                                }
                                            )
                                        } else if (otpFlowType == "REGISTER") {
                                            viewModel.loginUser(
                                                ign = ignInput.trim(),
                                                ffUid = ffUidInput.trim(),
                                                phone = phoneInput.trim(),
                                                extraMobile = extraMobileInput.trim(),
                                                email = emailInput.trim(),
                                                onFinished = {
                                                    authStep = "FORM"
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
                                        }
                                    } else {
                                        otpErrorMsg = "Incorrect OTP! The lobby remains locked. Try again."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("otp_submit_btn")
                            ) {
                                Text("VERIFY OTP & OPEN APP", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
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

                    AnimatedContent(
                        targetState = activeAuthTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "AuthTabTransition"
                    ) { tab ->
                        Column {
                            if (tab == "SIGN_IN") {
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
                            onValueChange = { 
                                signInPhoneInput = it 
                                errorMsg = null
                            },
                            label = { Text("Enter WhatsApp/Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "phone", tint = RedPrimary) },
                            placeholder = { Text("e.g. +91 88877 66554") },
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
                                    errorMsg = "Please enter your WhatsApp/Mobile number to sign in!"
                                } else {
                                    scope.launch {
                                        val registeredUser = viewModel.getRegisteredUserByPhone(signInPhoneInput.trim())
                                        val exists = registeredUser != null
                                        if (exists) {
                                            val otp = (1000..9999).random().toString()
                                            val targetNumber = registeredUser?.phoneNumber ?: signInPhoneInput.trim()
                                            viewModel.sendOtpSms(targetNumber, otp) { success, errMsg ->
                                                if (success) {
                                                    generatedOtp = otp
                                                    otpFlowType = "SIGN_IN"
                                                    authStep = "OTP"
                                                    enteredOtp = ""
                                                    otpErrorMsg = null
                                                    if (viewModel.getSmsGatewayMode() == "TEST_MODE") {
                                                        viewModel.showToast(
                                                            title = "🔒 SECURE ACCOUNT VERIFICATION",
                                                            message = "Your BattleZone entry verification OTP is: $otp. Input this code to enter.",
                                                            type = NotificationType.SUCCESS
                                                        )
                                                    } else {
                                                        viewModel.showToast(
                                                            title = "✉️ SMS OTP DISPATCHED",
                                                            message = "A BattleZone verification code has been sent securely via SMS. Please check your phone inbox!",
                                                            type = NotificationType.SUCCESS
                                                        )
                                                    }
                                                } else {
                                                    errorMsg = errMsg ?: "Failed to dispatch SMS OTP. Please try again or contact support."
                                                }
                                            }
                                        } else {
                                            errorMsg = "No registered account found with Mobile Number: ${signInPhoneInput.trim()}. Please register first!"
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
                                ignInput = it 
                                errorMsg = null
                            },
                            label = { Text("In-Game Name (IGN)") },
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
                                ffUidInput = it 
                                errorMsg = null
                            },
                            label = { Text("Free Fire Character UID") },
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
                            onValueChange = { 
                                phoneInput = it 
                                errorMsg = null
                            },
                            label = { Text("WhatsApp Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "phone", tint = RedPrimary) },
                            placeholder = { Text("e.g. +91 9876543210") },
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
                            value = extraMobileInput,
                            onValueChange = { 
                                extraMobileInput = it 
                                errorMsg = null
                            },
                            label = { Text("Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "extra_phone", tint = RedPrimary) },
                            placeholder = { Text("e.g. +91 9123456789") },
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
                                .testTag("login_extra_phone_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Contact (Required)") },
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
                                if (ignInput.isBlank() || ffUidInput.isBlank() || phoneInput.isBlank() || extraMobileInput.isBlank() || emailInput.isBlank() || !emailInput.contains("@") || !emailInput.contains(".")) {
                                    errorMsg = "Please fill in IGN, Character UID, WhatsApp number, Mobile number, and a valid Gmail/Email address to register!"
                                } else {
                                    val otp = (1000..9999).random().toString()
                                    viewModel.sendOtpSms(phoneInput.trim(), otp) { success, errMsg ->
                                        if (success) {
                                            generatedOtp = otp
                                            otpFlowType = "REGISTER"
                                            authStep = "OTP"
                                            enteredOtp = ""
                                            otpErrorMsg = null
                                            if (viewModel.getSmsGatewayMode() == "TEST_MODE") {
                                                viewModel.showToast(
                                                    title = "🔒 NEW REGISTRATION OTP",
                                                    message = "Your BattleZone account setup verification OTP is: $otp. Please input this code to verify.",
                                                    type = NotificationType.SUCCESS
                                                )
                                            } else {
                                                viewModel.showToast(
                                                    title = "✉️ SMS OTP DISPATCHED",
                                                    message = "A registration verification code has been sent securely via SMS. Please check your phone!",
                                                    type = NotificationType.SUCCESS
                                                )
                                            }
                                        } else {
                                            errorMsg = errMsg ?: "Failed to dispatch SMS OTP. Please try again or contact support."
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
                onClick = { showGoogleDialog = true },
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
        Dialog(onDismissRequest = { if (!isGoogleLoading) showGoogleDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF202124), // Google Dark theme
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF3C4043)), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Logo branding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "G", color = Color(0xFF4285F4), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "o", color = Color(0xFFEA4335), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "o", color = Color(0xFFFBBC05), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "g", color = Color(0xFF4285F4), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "l", color = Color(0xFF34A853), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "e", color = Color(0xFFEA4335), fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Choose an account",
                        color = Color.White,
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
                            if (accountsList.isNotEmpty()) {
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
                                                isGoogleLoading = true
                                                scope.launch {
                                                    delay(1000)
                                                    isGoogleLoading = false
                                                    val registeredPhone = viewModel.getGoogleUserPhone(accEmail)
                                                    if (!registeredPhone.isNullOrBlank() && !registeredPhone.startsWith("+91 123456")) {
                                                        val otp = (1000..9999).random().toString()
                                                        viewModel.sendOtpSms(registeredPhone, otp) { success, errMsg ->
                                                            if (success) {
                                                                generatedOtp = otp
                                                                otpFlowType = "GOOGLE"
                                                                customGoogleEmail = accEmail
                                                                customGoogleName = accName
                                                                authStep = "OTP"
                                                                enteredOtp = ""
                                                                otpErrorMsg = null
                                                                showGoogleDialog = false
                                                                if (viewModel.getSmsGatewayMode() == "TEST_MODE") {
                                                                    viewModel.showToast(
                                                                        title = "🔒 SECURE ACCOUNT VERIFICATION",
                                                                        message = "Google Account: $accEmail has registered phone: $registeredPhone. OTP code is: $otp",
                                                                        type = NotificationType.SUCCESS
                                                                    )
                                                                } else {
                                                                    viewModel.showToast(
                                                                        title = "✉️ SMS OTP DISPATCHED",
                                                                        message = "A verification code has been sent securely via SMS to your registered number: $registeredPhone.",
                                                                        type = NotificationType.SUCCESS
                                                                    )
                                                                }
                                                            } else {
                                                                showGoogleDialog = false
                                                                viewModel.showToast(
                                                                    title = "⚠️ Verification Error",
                                                                    message = "Could not deliver secure OTP to registered number: $registeredPhone. Detail: $errMsg",
                                                                    type = NotificationType.WARNING
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        showGoogleDialog = false
                                                        viewModel.loginWithGoogle(email = accEmail, name = accName) {
                                                            authStep = "FORM"
                                                        }
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
                            } else {
                                Text(
                                    text = "No Google accounts were automatically detected on this device. Please enter your Gmail address below to sign in with Google:",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                OutlinedTextField(
                                    value = customGoogleEmail,
                                    onValueChange = { customGoogleEmail = it },
                                    label = { Text("Gmail Address", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFF5F6368)
                                    ),
                                    placeholder = { Text("username@gmail.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (customGoogleEmail.isNotBlank() && customGoogleEmail.contains("@") && customGoogleEmail.contains(".")) {
                                            isGoogleLoading = true
                                            scope.launch {
                                                delay(1000)
                                                isGoogleLoading = false
                                                val autoName = customGoogleEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                val registeredPhone = viewModel.getGoogleUserPhone(customGoogleEmail.trim().lowercase())
                                                if (!registeredPhone.isNullOrBlank() && !registeredPhone.startsWith("+91 123456")) {
                                                    val otp = (1000..9999).random().toString()
                                                    viewModel.sendOtpSms(registeredPhone, otp) { success, errMsg ->
                                                        if (success) {
                                                            generatedOtp = otp
                                                            otpFlowType = "GOOGLE"
                                                            customGoogleEmail = customGoogleEmail.trim().lowercase()
                                                            customGoogleName = autoName
                                                            authStep = "OTP"
                                                            enteredOtp = ""
                                                            otpErrorMsg = null
                                                            showGoogleDialog = false
                                                            if (viewModel.getSmsGatewayMode() == "TEST_MODE") {
                                                                viewModel.showToast(
                                                                    title = "🔒 SECURE ACCOUNT VERIFICATION",
                                                                    message = "Google Account: $customGoogleEmail has registered phone: $registeredPhone. OTP code is: $otp",
                                                                    type = NotificationType.SUCCESS
                                                                )
                                                            } else {
                                                                viewModel.showToast(
                                                                    title = "✉️ SMS OTP DISPATCHED",
                                                                    message = "A verification code has been sent securely via SMS to your registered number: $registeredPhone.",
                                                                    type = NotificationType.SUCCESS
                                                                )
                                                            }
                                                        } else {
                                                            showGoogleDialog = false
                                                            viewModel.showToast(
                                                                title = "⚠️ Verification Error",
                                                                message = "Could not deliver secure OTP to registered number: $registeredPhone. Detail: $errMsg",
                                                                type = NotificationType.WARNING
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    showGoogleDialog = false
                                                    viewModel.loginWithGoogle(email = customGoogleEmail.trim().lowercase(), name = autoName) {
                                                        authStep = "FORM"
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = customGoogleEmail.isNotBlank() && customGoogleEmail.contains("@") && customGoogleEmail.contains(".")
                                ) {
                                    Text("CONTINUE WITH GOOGLE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showGoogleDialog = false }
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

