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
fun BattleZoneMainApp(viewModel: BattleZoneViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    var isAdminMode by remember { mutableStateOf(false) }
    var activeTournamentIdForDetails by remember { mutableStateOf<Int?>(null) }

    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val userJoins by viewModel.currentUserJoins.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BattleZoneTopBar(
                isAdmin = isAdminMode,
                user = user,
                onToggleAdmin = {
                    isAdminMode = !isAdminMode
                    activeTournamentIdForDetails = null // reset view
                    scope.launch {
                        val roleText = if (isAdminMode) "Admin Controls" else "Gamer Lobby"
                        snackbarHostState.showSnackbar("Switched portal to $roleText")
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
                AdminDashboardScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
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
                                snackbarHostState = snackbarHostState,
                                onEnterAdmin = { isAdminMode = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

// TOP RUNNING BAR
@Composable
fun BattleZoneTopBar(
    isAdmin: Boolean,
    user: UserEntity?,
    onToggleAdmin: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Dual Role Changer Badge
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

                    Dialog(onDismissRequest = { showInGameNamePrompt = false }) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
                        ) {
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
                                            showInGameNamePrompt = false
                                            // Perform join
                                            viewModel.joinTournament(match.id) { response ->
                                                scope.launch {
                                                    if (response == "SUCCESS") {
                                                        snackbarHostState.showSnackbar("Match registration confirmed!")
                                                    } else {
                                                        snackbarHostState.showSnackbar(response)
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                                    ) {
                                        Text("CONFIRM JOIN")
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
        var selectedPaymentMethod by remember { mutableStateOf("GPay UPI") }

        Dialog(onDismissRequest = { showDepositDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF28252C)), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Deposit Money to Wallet",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Simulate UPI / Razorpay secure payment checkout pipeline.",
                        fontSize = 10.sp,
                        color = GreyText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = depositAmountInput,
                        onValueChange = { depositAmountInput = it },
                        label = { Text("Deposit Amount (INR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFF28252C)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("CHOOSE UPI METHOD:", fontSize = 10.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val gateways = listOf("PhonePe UPI", "GPay UPI", "PayTM Checkout", "Credit Card")
                    gateways.forEach { gw ->
                        val isSel = selectedPaymentMethod == gw
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = gw }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = { selectedPaymentMethod = gw },
                                colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                            )
                            Text(gw, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDepositDialog = false }) {
                            Text("CLOSE", color = GreyText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showDepositDialog = false
                                val amtNum = depositAmountInput.toDoubleOrNull() ?: 0.0
                                viewModel.addMoney(amtNum, selectedPaymentMethod) { invoiceId ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Deposited ₹$amtNum! Receipt: $invoiceId")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("PAY NOW")
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
    }
}


// --- 6. USER PROFILE & REFERRALS ---
@Composable
fun ProfileScreen(
    viewModel: BattleZoneViewModel,
    user: UserEntity?,
    snackbarHostState: SnackbarHostState,
    onEnterAdmin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }

    var ignState by remember { mutableStateOf(user?.inGameName ?: "Alpha_Gamer") }
    var ffUidState by remember { mutableStateOf(user?.freeFireUid ?: "FF-837492047") }
    var phoneState by remember { mutableStateOf(user?.phoneNumber ?: "+91 91929 39495") }
    var emailState by remember { mutableStateOf(user?.email ?: "gamer@battlezone.com") }

    var referralCodePrompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("GAMER PROFILE PROFILE CARD", fontSize = 12.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        // Avatar banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, Color(0xFF1F1C25))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
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
                    Text("Email: ${user?.email ?: "gamer@battlezone.com"}", fontSize = 11.sp, color = GreyText)
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
                        value = phoneState,
                        onValueChange = { phoneState = it },
                        label = { Text("Phone Contacts") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = emailState,
                        onValueChange = { emailState = it },
                        label = { Text("Email Contacts") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(ignState, ffUidState, phoneState, emailState) {
                                isEditing = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Profile updated securely.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE CHANGES")
                    }
                } else {
                    ProfileFieldRow(label = "In Game Alias", value = user?.inGameName ?: "Alpha_Gamer")
                    ProfileFieldRow(label = "Free Fire UID", value = user?.freeFireUid ?: "FF-837492047")
                    ProfileFieldRow(label = "Phone details", value = user?.phoneNumber ?: "+91 98765 43210")
                    ProfileFieldRow(label = "Support Mail", value = user?.email ?: "gamer@battlezone.com")
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

@Composable
fun ProfileFieldRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label.uppercase(), fontSize = 8.sp, color = GreyText, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}


// --- 7. ADMIN DASHBOARD & CONTROLS ---
@Composable
fun AdminDashboardScreen(
    viewModel: BattleZoneViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var activeAdminTab by remember { mutableStateOf("METRICS") } // "METRICS", "TOURNAMENTS", "WITHDRAWALS", "TICKETS"

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
                Pair("PROOFS", "Verification Queue")
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
            var depositAdjustment by remember { mutableStateOf("50") }
            var winningAdjustment by remember { mutableStateOf("0") }
            var bonusAdjustment by remember { mutableStateOf("0") }

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
                            text = "Credit / Debit: ${userToAdjust.inGameName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text("Direct wallet control panel. Fill negative numbers to deduct balance.", fontSize = 9.sp, color = GreyText)
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = depositAdjustment,
                            onValueChange = { depositAdjustment = it },
                            label = { Text("Deposit Mod Amount") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = winningAdjustment,
                            onValueChange = { winningAdjustment = it },
                            label = { Text("Winning Mod Amount") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color(0xFF28252C)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = bonusAdjustment,
                            onValueChange = { bonusAdjustment = it },
                            label = { Text("Bonus Mod Amount") },
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
                                    val dVal = depositAdjustment.toDoubleOrNull() ?: 0.0
                                    val wVal = winningAdjustment.toDoubleOrNull() ?: 0.0
                                    val bVal = bonusAdjustment.toDoubleOrNull() ?: 0.0

                                    viewModel.adminModifyUserBalance(userToAdjust.id, dVal, wVal, bVal)
                                    selectUserForWalletAdjust = null
                                    scope.launch {
                                        snackbarHost.showSnackbar("Wrote wallet modifications successfully.")
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
