package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.EmpireState
import com.example.data.LogEntry
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpireScreen(
    viewModel: EmpireViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.empireState.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val advisorResponse by viewModel.advisorResponse.collectAsStateWithLifecycle()
    val isAdvisorLoading by viewModel.isAdvisorLoading.collectAsStateWithLifecycle()
    val campaignResult by viewModel.campaignResult.collectAsStateWithLifecycle()
    val currentDilemma by viewModel.currentDilemma.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var renameRulerDialogShow by remember { mutableStateOf(false) }
    var rulerInput by remember { mutableStateOf("") }

    val tabs = listOf("Realm", "Structures", "Expeditions", "Advisor", "Chronicles")

    // Error and success notification helpers
    val showNotification: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.empire_crest),
                            contentDescription = "Imperial Crest",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = state?.empireName ?: "MAM's Empire",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            state?.let {
                                Text(
                                    text = "Ruler: Emperor ${it.rulerName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            state?.let {
                                rulerInput = it.rulerName
                                renameRulerDialogShow = true
                            }
                        },
                        modifier = Modifier.testTag("rename_ruler_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename Ruler",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.resetGame("MAM")
                            showNotification("The empire has been reset to its foundations.")
                        },
                        modifier = Modifier.testTag("reset_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Empire",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val icon = when (index) {
                        0 -> Icons.Default.Home
                        1 -> Icons.Default.Business
                        2 -> Icons.Default.Flag
                        3 -> Icons.Default.Psychology
                        else -> Icons.Default.History
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(icon, contentDescription = title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.background,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            state?.let { empire ->
                Column(modifier = Modifier.fillMaxSize()) {
                    // Empire Status Header Strip
                    EmpireStatusHeader(empire = empire)

                    Divider(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Active Screen based on Selected Tab
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> RealmTab(viewModel = viewModel, empire = empire)
                            1 -> InfrastructureTab(viewModel = viewModel, empire = empire, onError = showNotification)
                            2 -> CampaignsAndTechTab(viewModel = viewModel, empire = empire, onError = showNotification)
                            3 -> AdvisorTab(viewModel = viewModel, isAdvisorLoading = isAdvisorLoading, advisorResponse = advisorResponse)
                            4 -> ChroniclesTab(logs = logs)
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // --- Event Dialogs ---

        // Dilemma Dialog
        currentDilemma?.let { dilemma ->
            Dialog(onDismissRequest = { /* Force action, no escape */ }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("dilemma_card"),
                    colors = CardDefaults.cardColors(containerColor = VelvetSurface),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Royal Dilemma",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = dilemma.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = dilemma.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.handleDilemmaChoice(1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("dilemma_option_1"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                dilemma.option1.text,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.handleDilemmaChoice(2) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("dilemma_option_2"),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                dilemma.option2.text,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Campaign Result Dialog
        campaignResult?.let { result ->
            Dialog(onDismissRequest = { viewModel.clearCampaignResult() }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("campaign_result_card"),
                    colors = CardDefaults.cardColors(containerColor = VelvetSurface),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val isVictory = result.contains("VICTORY") || result.contains("TRIUMPH") || result.contains("Success")
                        val headerIcon = if (isVictory) Icons.Default.MilitaryTech else Icons.Default.Warning
                        val headerColor = if (isVictory) ForestGreen else CoralRed

                        Icon(
                            imageVector = headerIcon,
                            contentDescription = "Campaign Outcome",
                            tint = headerColor,
                            modifier = Modifier.size(56.dp)
                        )

                        Text(
                            text = if (isVictory) "Campaign Victory!" else "Campaign Report",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = headerColor
                        )

                        Text(
                            text = result,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.clearCampaignResult() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("campaign_result_dismiss"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Acknowledge", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Rename Ruler Dialog
        if (renameRulerDialogShow) {
            AlertDialog(
                onDismissRequest = { renameRulerDialogShow = false },
                title = { Text("Rename Imperial Ruler", color = MaterialTheme.colorScheme.primary) },
                text = {
                    OutlinedTextField(
                        value = rulerInput,
                        onValueChange = { rulerInput = it },
                        label = { Text("Ruler Name") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("ruler_name_textfield")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (rulerInput.isNotBlank()) {
                                viewModel.resetGame(rulerInput.trim())
                                showNotification("A new sovereign reigns! Empire reset completed.")
                            }
                            renameRulerDialogShow = false
                        },
                        modifier = Modifier.testTag("ruler_name_confirm")
                    ) {
                        Text("Crown Sovereign", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameRulerDialogShow = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = VelvetSurface
            )
        }
    }
}

// Header summary strip for important resources
@Composable
fun EmpireStatusHeader(empire: EmpireState) {
    val maxPop = 30 + empire.farmCount * 15 + empire.castleWallLevel * 5
    val currentPop = empire.peasantCount + empire.knightCount + empire.archerCount + empire.mageCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day Counter
        Card(
            colors = CardDefaults.cardColors(containerColor = VelvetSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Day", tint = RoyalGold, modifier = Modifier.size(16.dp))
                Text("Day ${empire.day}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalGold)
            }
        }

        // Citizens / Housing limit
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.People, contentDescription = "Population", tint = WarmIvory, modifier = Modifier.size(16.dp))
            Text("Citizens: ", fontSize = 12.sp, color = WarmIvory)
            Text("$currentPop/$maxPop", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalGold)
        }

        // Happiness indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val moodIcon = when {
                empire.happiness >= 80 -> Icons.Default.SentimentSatisfiedAlt
                empire.happiness >= 40 -> Icons.Default.SentimentNeutral
                else -> Icons.Default.SentimentVeryDissatisfied
            }
            val moodColor = when {
                empire.happiness >= 80 -> ForestGreen
                empire.happiness >= 40 -> RoyalGold
                else -> CoralRed
            }
            Icon(moodIcon, contentDescription = "Happiness", tint = moodColor, modifier = Modifier.size(18.dp))
            Text("Happiness: ", fontSize = 12.sp, color = WarmIvory)
            Text("${empire.happiness}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moodColor)
        }
    }
}

// Tab 1: Overview & active gatherer panel
@Composable
fun RealmTab(viewModel: EmpireViewModel, empire: EmpireState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ruler's greeting banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VelvetSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = "Crown",
                    tint = RoyalGold,
                    modifier = Modifier.size(40.dp)
                )
                Column {
                    Text(
                        "Welcome, Sovereign ${empire.rulerName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RoyalGold
                    )
                    Text(
                        "Rule your empire wisely. Balance production, grow your military, and expand your domain.",
                        fontSize = 12.sp,
                        color = WarmIvory.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Resource clickers grid
        Text(
            "Imperial Resources & Active Labors",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = RoyalGold,
            modifier = Modifier.align(Alignment.Start)
        )

        val foodIncome = (empire.peasantCount * 3 + empire.farmCount * 15).let { if (empire.cropRotation) (it * 1.25).toInt() else it } - (empire.peasantCount + empire.knightCount + empire.archerCount + empire.mageCount)
        val woodIncome = (empire.peasantCount * 2 + empire.lumberMillCount * 15).let { if (empire.steelAxes) (it * 1.25).toInt() else it }
        val stoneIncome = (empire.peasantCount * 1 + empire.quarryCount * 15).let { if (empire.masonry) (it * 1.25).toInt() else it }
        val goldIncome = empire.peasantCount * 2 + empire.goldMineCount * 20

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ResourceGatherCard(
                        name = "Gold Coin",
                        value = empire.gold,
                        income = goldIncome,
                        icon = Icons.Default.MonetizationOn,
                        iconColor = RoyalGold,
                        actionLabel = "Tax Citizens",
                        warningLabel = "Loses happiness",
                        onClick = { viewModel.gatherResource("gold") },
                        tag = "gather_gold"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ResourceGatherCard(
                        name = "Granary Food",
                        value = empire.food,
                        income = foodIncome,
                        icon = Icons.Default.Restaurant,
                        iconColor = ForestGreen,
                        actionLabel = "Harvest Crops",
                        onClick = { viewModel.gatherResource("food") },
                        tag = "gather_food"
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ResourceGatherCard(
                        name = "Shed Wood",
                        value = empire.wood,
                        income = woodIncome,
                        icon = Icons.Default.Forest,
                        iconColor = OrangeYellow,
                        actionLabel = "Chop Timber",
                        onClick = { viewModel.gatherResource("wood") },
                        tag = "gather_wood"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ResourceGatherCard(
                        name = "Quarried Stone",
                        value = empire.stone,
                        income = stoneIncome,
                        icon = Icons.Default.Category,
                        iconColor = IronGrey,
                        actionLabel = "Quarry Rock",
                        onClick = { viewModel.gatherResource("stone") },
                        tag = "gather_stone"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Grand End Turn button
        Button(
            onClick = { viewModel.advanceTurn() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("end_day_button")
                .border(2.dp, RoyalGold, RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonBurgundy),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Advance Day",
                    tint = RoyalGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "ADVANCE EMPIRE (END DAY)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = RoyalGold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
fun ResourceGatherCard(
    name: String,
    value: Int,
    income: Int,
    icon: ImageVector,
    iconColor: Color,
    actionLabel: String,
    warningLabel: String? = null,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VelvetSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, WarmIvory.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = name, tint = iconColor, modifier = Modifier.size(20.dp))
                Column {
                    Text(name, fontSize = 11.sp, color = WarmIvory.copy(alpha = 0.6f))
                    Text(
                        value.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WarmIvory
                    )
                }
            }

            // Daily income indicator
            val incomeText = if (income >= 0) "+$income/day" else "$income/day"
            val incomeColor = if (income >= 0) ForestGreen else CoralRed
            Text(
                incomeText,
                fontSize = 11.sp,
                color = incomeColor,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(tag),
                colors = ButtonDefaults.buttonColors(containerColor = VelvetSurface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, iconColor.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iconColor)
                    if (warningLabel != null) {
                        Text(warningLabel, fontSize = 8.sp, color = CoralRed.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

val OrangeYellow = Color(0xFFFBBF24)

// Tab 2: Infrastructure (Buildings & Military Recruitment)
@Composable
fun InfrastructureTab(
    viewModel: EmpireViewModel,
    empire: EmpireState,
    onError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Buildings section ---
        Text(
            "Construct Imperial Architecture",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = RoyalGold
        )

        val buildingsList = listOf(
            Triple("farm", "Wheat Farm", "Produces +15 Food/day. Expands citizen housing limit by +15."),
            Triple("lumber_mill", "Lumber Mill", "Produces +15 Wood/day for construction."),
            Triple("quarry", "Stone Quarry", "Produces +15 Stone/day for fortified defenses."),
            Triple("gold_mine", "Gold Mine", "Produces +20 Gold/day from raw underground ores."),
            Triple("barracks", "War Barracks", "Allows training of military units (Knights, Archers, Mages)."),
            Triple("wall", "Castle Fortress Wall", "Adds +15 Castle Defense to repel events. Max Pop +5."),
            Triple("temple", "Divine Temple", "Produces +3 Happiness/day. Crucial for Summoning Mages.")
        )

        buildingsList.forEach { (type, name, description) ->
            val count = when (type) {
                "farm" -> empire.farmCount
                "lumber_mill" -> empire.lumberMillCount
                "quarry" -> empire.quarryCount
                "gold_mine" -> empire.goldMineCount
                "barracks" -> empire.barracksCount
                "wall" -> empire.castleWallLevel
                "temple" -> empire.templeCount
                else -> 0
            }

            val cost = viewModel.getBuildingCost(type, count)

            BuildingRowItem(
                name = name,
                count = count,
                description = description,
                cost = cost,
                onBuild = {
                    viewModel.buildStructure(
                        type = type,
                        onSuccess = { onError("Successfully built $name!") },
                        onError = onError
                    )
                },
                tag = "build_$type"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Military section ---
        Text(
            "Empire Garrison & Labor Forces",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = RoyalGold
        )

        if (empire.barracksCount == 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = VelvetSurface),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f))
            ) {
                Text(
                    "⚠ Construct a Barracks to unlock military recruitment!",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = CoralRed,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        val militaryList = listOf(
            Triple("peasant", "Peasant Laborer", "Basic gatherer. Produces Food/Wood/Stone/Gold each turn."),
            Triple("knight", "Heavy Knight", "Heavy melee defender. Core spearhead of expeditions."),
            Triple("archer", "Royal Archer", "Support damage dealer. Counters flying and agile targets."),
            Triple("mage", "Arcane Mage", "Summoned spelling magic. Deals elemental destructive damage.")
        )

        militaryList.forEach { (type, name, desc) ->
            val count = when (type) {
                "peasant" -> empire.peasantCount
                "knight" -> empire.knightCount
                "archer" -> empire.archerCount
                "mage" -> empire.mageCount
                else -> 0
            }

            val cost = viewModel.getUnitCost(type)

            UnitRowItem(
                name = name,
                count = count,
                description = desc,
                cost = cost,
                onRecruit = {
                    viewModel.recruitUnit(
                        type = type,
                        onSuccess = { onError("Successfully trained $name!") },
                        onError = onError
                    )
                },
                tag = "recruit_$type"
            )
        }
    }
}

@Composable
fun BuildingRowItem(
    name: String,
    count: Int,
    description: String,
    cost: Map<String, Int>,
    onBuild: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VelvetSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, WarmIvory.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmIvory)
                    Badge(containerColor = RoyalGold, contentColor = RoyalNavy) {
                        Text("Qty: $count", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                Text(description, fontSize = 11.sp, color = WarmIvory.copy(alpha = 0.6f), lineHeight = 14.sp)
                
                Spacer(modifier = Modifier.height(6.dp))

                // Cost tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cost.forEach { (res, amount) ->
                        val chipColor = when (res) {
                            "wood" -> OrangeYellow
                            "stone" -> IronGrey
                            "gold" -> RoyalGold
                            else -> WarmIvory
                        }
                        Text(
                            "$res: $amount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipColor,
                            modifier = Modifier
                                .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onBuild,
                modifier = Modifier.testTag(tag),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
            ) {
                Text("Build", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalNavy)
            }
        }
    }
}

@Composable
fun UnitRowItem(
    name: String,
    count: Int,
    description: String,
    cost: Map<String, Int>,
    onRecruit: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VelvetSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, RoyalBlue.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmIvory)
                    Badge(containerColor = RoyalBlue, contentColor = WarmIvory) {
                        Text("Count: $count", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                Text(description, fontSize = 11.sp, color = WarmIvory.copy(alpha = 0.6f), lineHeight = 14.sp)

                Spacer(modifier = Modifier.height(6.dp))

                // Cost tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cost.forEach { (res, amount) ->
                        val chipColor = when (res) {
                            "food" -> ForestGreen
                            "wood" -> OrangeYellow
                            "stone" -> IronGrey
                            "gold" -> RoyalGold
                            else -> WarmIvory
                        }
                        Text(
                            "$res: $amount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipColor,
                            modifier = Modifier
                                .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onRecruit,
                modifier = Modifier.testTag(tag),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("Train", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmIvory)
            }
        }
    }
}

// Tab 3: Campaigns (Expeditions) and Tech Upgrades
@Composable
fun CampaignsAndTechTab(
    viewModel: EmpireViewModel,
    empire: EmpireState,
    onError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Tech Tree (Research) ---
        Text(
            "Research Imperial Technologies",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = RoyalGold
        )

        val techs = listOf(
            Triple("crop_rotation", "Crop Rotation", "Improves standard Food production by +25%."),
            Triple("steel_axes", "Steel Axes", "Improves standard Wood production by +25%."),
            Triple("masonry", "Masonry Architecture", "Improves standard Stone production by +25%."),
            Triple("fortification", "Castle Fortification", "Enhances wall armor and defense mechanisms."),
            Triple("chivalry", "Knight Chivalry Code", "Boosts heavy Knight damage from 15 to 25."),
            Triple("arcane_wisdom", "Arcane Wisdom Core", "Boosts magic Mage damage from 30 to 50.")
        )

        techs.forEach { (type, name, description) ->
            val researched = when (type) {
                "crop_rotation" -> empire.cropRotation
                "steel_axes" -> empire.steelAxes
                "masonry" -> empire.masonry
                "fortification" -> empire.fortification
                "chivalry" -> empire.chivalry
                "arcane_wisdom" -> empire.arcaneWisdom
                else -> false
            }

            val cost = viewModel.getResearchCost(type)

            TechRowItem(
                name = name,
                researched = researched,
                description = description,
                cost = cost,
                onResearch = {
                    viewModel.researchTech(
                        type = type,
                        onSuccess = { onError("Researched $name!") },
                        onError = onError
                    )
                },
                tag = "research_$type"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Campaigns (Expeditions) ---
        Text(
            "Military Expeditions",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = RoyalGold
        )

        val campaignStrength = empire.knightCount * (if (empire.chivalry) 25 else 15) + empire.archerCount * 10 + empire.mageCount * (if (empire.arcaneWisdom) 50 else 30)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Combat Power:", fontSize = 12.sp, color = WarmIvory)
            Badge(containerColor = CrimsonBurgundy, contentColor = RoyalGold) {
                Text("$campaignStrength Combat Rating", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
            }
        }

        val expeditions = listOf(
            Triple("settle", "Settle Fertility Valleys (Easy)", "Establishes food settlements. Est. Success: 90%. Reward: Wood, Food, gold, +3 Peasants."),
            Triple("goblin", "Raid Goblin Outposts (Medium)", "Pillage goblin treasures. Est. Success: Medium. Reward: Wood, Stone, Gold. Risk of soldier casualty."),
            Triple("siege", "Siege Mountain Fortress (Hard)", "Conquer rogue fortress walls. Est. Success: Low. Reward: Gold, Stone, Wood. Casualties likely."),
            Triple("dragon", "Slay Volcanic Dragon Balerion (Legendary)", "The boss battle! Slay the dragon. Est. Success: Very Low. Reward: Gold heap. Extreme casualties risk.")
        )

        expeditions.forEach { (type, name, description) ->
            CampaignRowItem(
                name = name,
                description = description,
                onLaunch = { viewModel.executeCampaign(type) },
                tag = "campaign_$type"
            )
        }
    }
}

@Composable
fun TechRowItem(
    name: String,
    researched: Boolean,
    description: String,
    cost: Map<String, Int>,
    onResearch: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VelvetSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (researched) ForestGreen.copy(alpha = 0.5f) else WarmIvory.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmIvory)
                    if (researched) {
                        Text(
                            "RESEARCHED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            modifier = Modifier
                                .border(1.dp, ForestGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(description, fontSize = 11.sp, color = WarmIvory.copy(alpha = 0.6f), lineHeight = 14.sp)

                if (!researched) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cost.forEach { (res, amount) ->
                            val chipColor = when (res) {
                                "food" -> ForestGreen
                                "wood" -> OrangeYellow
                                "stone" -> IronGrey
                                "gold" -> RoyalGold
                                else -> WarmIvory
                            }
                            Text(
                                "$res: $amount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipColor,
                                modifier = Modifier
                                    .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (!researched) {
                Button(
                    onClick = onResearch,
                    modifier = Modifier.testTag(tag),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Research", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmIvory)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Researched",
                    tint = ForestGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CampaignRowItem(
    name: String,
    description: String,
    onLaunch: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VelvetSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CrimsonBurgundy.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmIvory)
                Text(description, fontSize = 11.sp, color = WarmIvory.copy(alpha = 0.6f), lineHeight = 14.sp)
            }

            Button(
                onClick = onLaunch,
                modifier = Modifier.testTag(tag),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonBurgundy)
            ) {
                Text("Launch", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalGold)
            }
        }
    }
}

// Tab 4: Advisor Chat (Strategic Gemini advice)
@Composable
fun AdvisorTab(
    viewModel: EmpireViewModel,
    isAdvisorLoading: Boolean,
    advisorResponse: String?
) {
    var queryInput by remember { mutableStateOf("") }

    val prepopulatedQuestions = listOf(
        "What should we build next, Advisor?",
        "Is our military strong enough to assault Goblins?",
        "How do we raise citizen happiness?",
        "Advise me on our current strategic path."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Royal Strategic Council",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = RoyalGold
        )

        // Scrollable output area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(VelvetSurface, RoundedCornerShape(12.dp))
                .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (isAdvisorLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = RoyalGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Alfred the advisor is consulting ancient archives...", fontSize = 12.sp, color = RoyalGold)
                }
            } else if (advisorResponse != null) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Advisor Alfred:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RoyalGold,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        advisorResponse,
                        fontSize = 13.sp,
                        color = WarmIvory,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.testTag("advisor_response_text")
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "Advisor Scroll",
                        tint = RoyalGold.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Ruler of the Realm, what counsel do you seek of me?",
                        fontSize = 13.sp,
                        color = WarmIvory.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }

        // Suggestions chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Suggested inquiries:", fontSize = 10.sp, color = WarmIvory.copy(alpha = 0.5f))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                prepopulatedQuestions.forEach { question ->
                    Text(
                        text = question,
                        fontSize = 10.sp,
                        color = RoyalGold,
                        modifier = Modifier
                            .background(RoyalGold.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                            .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                            .clickable { viewModel.askAdvisor(question) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Search Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = queryInput,
                onValueChange = { queryInput = it },
                placeholder = { Text("Consult on a decree...", fontSize = 12.sp, color = WarmIvory.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("advisor_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = WarmIvory,
                    unfocusedTextColor = WarmIvory
                ),
                maxLines = 2,
                textStyle = TextStyle(fontSize = 13.sp)
            )

            IconButton(
                onClick = {
                    if (queryInput.isNotBlank()) {
                        viewModel.askAdvisor(queryInput.trim())
                        queryInput = ""
                    }
                },
                modifier = Modifier
                    .background(RoyalGold, RoundedCornerShape(50.dp))
                    .testTag("advisor_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = RoyalNavy)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

// Tab 5: Chronicles / Game History Logs
@Composable
fun ChroniclesTab(logs: List<LogEntry>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Imperial Chronicles & History",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = RoyalGold
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Your rule has just begun. Annals are empty.", fontSize = 12.sp, color = WarmIvory.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("logs_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    ChronicleLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun ChronicleLogItem(log: LogEntry) {
    val categoryColor = when (log.category) {
        "Turn" -> RoyalGold
        "Decision" -> RoyalBlue
        "Campaign" -> CrimsonBurgundy
        "Advisor" -> ForestGreen
        else -> WarmIvory
    }

    val categoryIcon = when (log.category) {
        "Turn" -> Icons.Default.HourglassEmpty
        "Decision" -> Icons.Default.Gavel
        "Campaign" -> Icons.Default.Flag
        "Advisor" -> Icons.Default.Psychology
        else -> Icons.Default.Message
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VelvetSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = log.category,
                tint = categoryColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Day ${log.day}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                    Text(
                        text = log.category.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    fontSize = 12.sp,
                    color = WarmIvory,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
