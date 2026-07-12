package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.api.RetrofitClient
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class EmpireViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EmpireRepository

    val empireState: StateFlow<EmpireState?>
    val logs: StateFlow<List<LogEntry>>

    // Advisor State
    private val _advisorResponse = MutableStateFlow<String?>(null)
    val advisorResponse: StateFlow<String?> = _advisorResponse.asStateFlow()

    private val _isAdvisorLoading = MutableStateFlow(false)
    val isAdvisorLoading: StateFlow<Boolean> = _isAdvisorLoading.asStateFlow()

    // Campaign State
    private val _campaignResult = MutableStateFlow<String?>(null)
    val campaignResult: StateFlow<String?> = _campaignResult.asStateFlow()

    // Event/Dilemma State
    private val _currentDilemma = MutableStateFlow<Dilemma?>(null)
    val currentDilemma: StateFlow<Dilemma?> = _currentDilemma.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EmpireRepository(database.empireDao())

        empireState = repository.empireState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        logs = repository.logs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize game state on first start
        viewModelScope.launch {
            val stateSnapshot = repository.getEmpireStateSnapshot()
            if (stateSnapshot == null) {
                repository.initializeNewEmpire("MAM")
            }
        }
    }

    // Static Game Calculations
    fun getBuildingCost(type: String, count: Int): Map<String, Int> {
        return when (type) {
            "farm" -> mapOf("wood" to 60 + count * 35, "gold" to 40 + count * 20)
            "lumber_mill" -> mapOf("wood" to 80 + count * 30, "stone" to 40 + count * 20)
            "quarry" -> mapOf("wood" to 100 + count * 40, "stone" to 60 + count * 25)
            "gold_mine" -> mapOf("stone" to 120 + count * 45, "gold" to 80 + count * 30)
            "barracks" -> mapOf("wood" to 180 + count * 90, "stone" to 120 + count * 60, "gold" to 100 + count * 50)
            "wall" -> mapOf("stone" to 140 + count * 80, "wood" to 60 + count * 40)
            "temple" -> mapOf("stone" to 200 + count * 120, "gold" to 180 + count * 100)
            else -> emptyMap()
        }
    }

    fun getUnitCost(type: String): Map<String, Int> {
        return when (type) {
            "peasant" -> mapOf("food" to 30, "gold" to 10)
            "knight" -> mapOf("food" to 80, "gold" to 35)
            "archer" -> mapOf("wood" to 50, "gold" to 20)
            "mage" -> mapOf("gold" to 120, "stone" to 40)
            else -> emptyMap()
        }
    }

    fun getResearchCost(type: String): Map<String, Int> {
        return when (type) {
            "crop_rotation" -> mapOf("gold" to 200, "food" to 100)
            "steel_axes" -> mapOf("gold" to 200, "wood" to 100)
            "masonry" -> mapOf("gold" to 250, "stone" to 120)
            "fortification" -> mapOf("gold" to 350, "stone" to 200)
            "chivalry" -> mapOf("gold" to 450, "food" to 250)
            "arcane_wisdom" -> mapOf("gold" to 500, "stone" to 250)
            else -> emptyMap()
        }
    }

    // Active Resource Gathering Actions
    fun gatherResource(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getEmpireStateSnapshot() ?: return@launch
            var bonus = 10
            
            // Apply tech bonuses to manual click
            val updatedState = when (type) {
                "food" -> {
                    if (state.cropRotation) bonus = (bonus * 1.5).toInt()
                    state.copy(food = state.food + bonus)
                }
                "wood" -> {
                    if (state.steelAxes) bonus = (bonus * 1.5).toInt()
                    state.copy(wood = state.wood + bonus)
                }
                "stone" -> {
                    if (state.masonry) bonus = (bonus * 1.5).toInt()
                    state.copy(stone = state.stone + bonus)
                }
                "gold" -> {
                    // Taxing people gains gold but loses some happiness
                    val taxAmount = 15 + (state.population / 10)
                    val happinessDrop = if (Random.nextFloat() < 0.4f) 1 else 0
                    state.copy(
                        gold = state.gold + taxAmount,
                        happiness = (state.happiness - happinessDrop).coerceAtLeast(10)
                    )
                }
                else -> state
            }
            
            repository.saveEmpireState(updatedState)
        }
    }

    // Construction Actions
    fun buildStructure(type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getEmpireStateSnapshot() ?: return@launch
            
            val count = when (type) {
                "farm" -> state.farmCount
                "lumber_mill" -> state.lumberMillCount
                "quarry" -> state.quarryCount
                "gold_mine" -> state.goldMineCount
                "barracks" -> state.barracksCount
                "wall" -> state.castleWallLevel
                "temple" -> state.templeCount
                else -> 0
            }

            val cost = getBuildingCost(type, count)
            
            // Check if can afford
            for ((res, amount) in cost) {
                val balance = when (res) {
                    "wood" -> state.wood
                    "stone" -> state.stone
                    "gold" -> state.gold
                    else -> 0
                }
                if (balance < amount) {
                    withContext(Dispatchers.Main) {
                        onError("Insufficient $res! Need $amount.")
                    }
                    return@launch
                }
            }

            // Deduct and build
            val deductedState = state.copy(
                wood = state.wood - (cost["wood"] ?: 0),
                stone = state.stone - (cost["stone"] ?: 0),
                gold = state.gold - (cost["gold"] ?: 0)
            )

            val finalState = when (type) {
                "farm" -> deductedState.copy(farmCount = state.farmCount + 1)
                "lumber_mill" -> deductedState.copy(lumberMillCount = state.lumberMillCount + 1)
                "quarry" -> deductedState.copy(quarryCount = state.quarryCount + 1)
                "gold_mine" -> deductedState.copy(goldMineCount = state.goldMineCount + 1)
                "barracks" -> deductedState.copy(barracksCount = state.barracksCount + 1)
                "wall" -> deductedState.copy(castleWallLevel = state.castleWallLevel + 1)
                "temple" -> deductedState.copy(templeCount = state.templeCount + 1)
                else -> deductedState
            }

            repository.saveEmpireState(finalState)
            
            val nameDisplay = type.replace("_", " ").replaceFirstChar { it.uppercase() }
            repository.addLog(
                day = state.day,
                category = "Turn",
                message = "Constructed a new $nameDisplay. Production expanded!"
            )
            
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    // Recruitment Actions
    fun recruitUnit(type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getEmpireStateSnapshot() ?: return@launch
            
            // Checks
            if (type != "peasant" && state.barracksCount == 0) {
                withContext(Dispatchers.Main) {
                    onError("Requires Barracks to train military forces!")
                }
                return@launch
            }
            if (type == "mage" && state.templeCount == 0) {
                withContext(Dispatchers.Main) {
                    onError("Requires a Temple to summon Arcane Mages!")
                }
                return@launch
            }

            // Housing check
            val maxPop = 30 + state.farmCount * 15 + state.castleWallLevel * 5
            val currentPop = state.peasantCount + state.knightCount + state.archerCount + state.mageCount
            if (currentPop >= maxPop) {
                withContext(Dispatchers.Main) {
                    onError("Housing capacity reached ($currentPop/$maxPop)! Construct more Farms.")
                }
                return@launch
            }

            val cost = getUnitCost(type)
            for ((res, amount) in cost) {
                val balance = when (res) {
                    "food" -> state.food
                    "wood" -> state.wood
                    "stone" -> state.stone
                    "gold" -> state.gold
                    else -> 0
                }
                if (balance < amount) {
                    withContext(Dispatchers.Main) {
                        onError("Insufficient $res! Need $amount to recruit.")
                    }
                    return@launch
                }
            }

            val deductedState = state.copy(
                food = state.food - (cost["food"] ?: 0),
                wood = state.wood - (cost["wood"] ?: 0),
                stone = state.stone - (cost["stone"] ?: 0),
                gold = state.gold - (cost["gold"] ?: 0),
                population = currentPop + 1
            )

            val finalState = when (type) {
                "peasant" -> deductedState.copy(peasantCount = state.peasantCount + 1)
                "knight" -> deductedState.copy(knightCount = state.knightCount + 1)
                "archer" -> deductedState.copy(archerCount = state.archerCount + 1)
                "mage" -> deductedState.copy(mageCount = state.mageCount + 1)
                else -> deductedState
            }

            repository.saveEmpireState(finalState)
            
            val unitDisplay = type.replaceFirstChar { it.uppercase() }
            repository.addLog(
                day = state.day,
                category = "Turn",
                message = "Trained a new $unitDisplay into the empire ranks."
            )
            
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    // Technology Research Actions
    fun researchTech(type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getEmpireStateSnapshot() ?: return@launch
            
            // Check if already researched
            val alreadyResearched = when (type) {
                "crop_rotation" -> state.cropRotation
                "steel_axes" -> state.steelAxes
                "masonry" -> state.masonry
                "fortification" -> state.fortification
                "chivalry" -> state.chivalry
                "arcane_wisdom" -> state.arcaneWisdom
                else -> false
            }

            if (alreadyResearched) {
                withContext(Dispatchers.Main) {
                    onError("Technology is already researched!")
                }
                return@launch
            }

            val cost = getResearchCost(type)
            for ((res, amount) in cost) {
                val balance = when (res) {
                    "gold" -> state.gold
                    "food" -> state.food
                    "wood" -> state.wood
                    "stone" -> state.stone
                    else -> 0
                }
                if (balance < amount) {
                    withContext(Dispatchers.Main) {
                        onError("Insufficient $res! Need $amount to research.")
                    }
                    return@launch
                }
            }

            val deductedState = state.copy(
                gold = state.gold - (cost["gold"] ?: 0),
                food = state.food - (cost["food"] ?: 0),
                wood = state.wood - (cost["wood"] ?: 0),
                stone = state.stone - (cost["stone"] ?: 0)
            )

            val finalState = when (type) {
                "crop_rotation" -> deductedState.copy(cropRotation = true)
                "steel_axes" -> deductedState.copy(steelAxes = true)
                "masonry" -> deductedState.copy(masonry = true)
                "fortification" -> deductedState.copy(fortification = true)
                "chivalry" -> deductedState.copy(chivalry = true)
                "arcane_wisdom" -> deductedState.copy(arcaneWisdom = true)
                else -> deductedState
            }

            repository.saveEmpireState(finalState)
            
            val techDisplay = type.replace("_", " ").replaceFirstChar { it.uppercase() }
            repository.addLog(
                day = state.day,
                category = "Turn",
                message = "Researched groundbreaking technology: $techDisplay!"
            )
            
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    // End Day / Advance Turn Action
    fun advanceTurn() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getEmpireStateSnapshot() ?: return@launch
            
            // 1. Calculate consumption (Citizens consume Food)
            val currentPop = state.peasantCount + state.knightCount + state.archerCount + state.mageCount
            val foodConsum = currentPop * 1
            
            var remainingFood = state.food - foodConsum
            var happinessChange = 0
            var starvationLog = ""
            var peasantDesertion = 0
            
            if (remainingFood < 0) {
                // Starvation occurs!
                val deficit = -remainingFood
                remainingFood = 0
                happinessChange -= 15
                
                // Desertions/deaths
                peasantDesertion = (state.peasantCount * 0.15).toInt().coerceAtLeast(1)
                starvationLog = " STARVATION! Deficit of $deficit Food occurred! Happiness plummeted. $peasantDesertion Peasants deserted."
            } else {
                // Happy surplus
                happinessChange += if (remainingFood > 200) 2 else 1
            }

            // 2. Resource production
            var goldProd = state.peasantCount * 2 + state.goldMineCount * 20
            
            var foodProdBase = state.peasantCount * 3 + state.farmCount * 15
            if (state.cropRotation) foodProdBase = (foodProdBase * 1.25).toInt()
            
            var woodProdBase = state.peasantCount * 2 + state.lumberMillCount * 15
            if (state.steelAxes) woodProdBase = (woodProdBase * 1.25).toInt()
            
            var stoneProdBase = state.peasantCount * 1 + state.quarryCount * 15
            if (state.masonry) stoneProdBase = (stoneProdBase * 1.25).toInt()
            
            // Temple happiness boost
            happinessChange += state.templeCount * 3

            val nextDay = state.day + 1
            val finalHappiness = (state.happiness + happinessChange).coerceIn(10, 100)
            
            // Auto population growth (chance)
            var popGrowthLog = ""
            var newPeasants = 0
            if (remainingFood > 150 && finalHappiness > 75 && Random.nextFloat() < 0.35f) {
                newPeasants = 1
                popGrowthLog = " A peasant family, attracted by our high happiness and food surplus, immigrated to our empire!"
            }

            val advancedState = state.copy(
                day = nextDay,
                food = remainingFood + foodProdBase,
                gold = state.gold + goldProd,
                wood = state.wood + woodProdBase,
                stone = state.stone + stoneProdBase,
                peasantCount = (state.peasantCount - peasantDesertion + newPeasants).coerceAtLeast(1),
                happiness = finalHappiness,
                population = currentPop - peasantDesertion + newPeasants
            )

            repository.saveEmpireState(advancedState)

            // Log production results
            val techBonusText = if (state.cropRotation || state.steelAxes || state.masonry) " (includes technology bonuses)" else ""
            val prodMsg = "Day ${state.day} Complete. Advanced to Day $nextDay! Gathered +$goldProd Gold, +$foodProdBase Food, +$woodProdBase Wood, +$stoneProdBase Stone$techBonusText. Consumed $foodConsum Food.$starvationLog$popGrowthLog"
            
            repository.addLog(
                day = nextDay,
                category = "Turn",
                message = prodMsg
            )

            // 3. Roll a chance of triggering a Dilemma/Random Event or a Barbarian Raid!
            rollTurnEvent(advancedState)
        }
    }

    private suspend fun rollTurnEvent(state: EmpireState) {
        val dice = Random.nextFloat()
        
        // 45% chance of event triggering on turn advance
        if (dice < 0.45f) {
            val dilemmas = getDilemmaPool(state)
            if (dilemmas.isNotEmpty()) {
                val chosen = dilemmas.random()
                _currentDilemma.value = chosen
            }
        }
    }

    // Handle Dilemma Choice
    fun handleDilemmaChoice(optionIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getEmpireStateSnapshot() ?: return@launch
            val dilemma = _currentDilemma.value ?: return@launch

            val option = if (optionIndex == 1) dilemma.option1 else dilemma.option2
            val outcome = option.effect(state)
            
            repository.saveEmpireState(outcome.updatedState)
            repository.addLog(
                day = state.day,
                category = "Decision",
                message = "Ruler Decision: ${dilemma.title}. Choices made. ${outcome.logMessage}"
            )
            
            _currentDilemma.value = null
        }
    }

    // Campaign/Expedition Simulation
    fun executeCampaign(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getEmpireStateSnapshot() ?: return@launch
            
            // Check army size
            val soldierCount = state.knightCount + state.archerCount + state.mageCount
            if (soldierCount == 0 && type != "settle") {
                _campaignResult.value = "Our Empire has no soldiers to send on campaign! Recruit knights or archers first."
                return@launch
            }

            _campaignResult.value = "Your military is marching out..."
            
            var battleLogs = StringBuilder()
            var successChance = 0.5f
            var success = false
            
            // Compute Military Attack strength
            var knightAtk = 15
            if (state.chivalry) knightAtk = 25
            
            var archerAtk = 10
            
            var mageAtk = 30
            if (state.arcaneWisdom) mageAtk = 50

            val totalStrength = state.knightCount * knightAtk + state.archerCount * archerAtk + state.mageCount * mageAtk
            
            var goldReward = 0
            var woodReward = 0
            var stoneReward = 0
            var foodReward = 0
            var popReward = 0
            
            // Casualties chances
            var casualtiesKnight = 0
            var casualtiesArcher = 0
            var casualtiesMage = 0

            when (type) {
                "settle" -> {
                    // Settle New Lands (Easy)
                    successChance = 0.90f
                    success = Random.nextFloat() < successChance
                    battleLogs.append("Our pioneer battalion sets off to settle the untamed fertile valleys.\n")
                    if (success) {
                        goldReward = 100
                        woodReward = 200
                        foodReward = 250
                        popReward = 3 // immigration boost
                        battleLogs.append("Success! The explorers discovered rich veins and built wooden hamlets. +3 Peasant settlers joined.")
                    } else {
                        battleLogs.append("Failure. Wild beasts and rough terrains forced the pioneer explorers to retreat empty-handed.")
                    }
                }
                "goblin" -> {
                    // Raid Goblin Outpost (Medium)
                    val requiredPower = 50
                    successChance = (totalStrength.toFloat() / requiredPower).coerceIn(0.1f, 0.95f)
                    success = Random.nextFloat() < successChance
                    battleLogs.append("Emperor ${state.rulerName}\'s legions assault a fortified Goblin Outpost.\n")
                    battleLogs.append("Our total army offensive power: $totalStrength vs Goblin Defense: $requiredPower.\n")
                    
                    if (success) {
                        goldReward = 250
                        woodReward = 120
                        stoneReward = 80
                        
                        // Small casualities
                        if (state.knightCount > 0 && Random.nextFloat() < 0.4) casualtiesKnight = 1
                        if (state.archerCount > 0 && Random.nextFloat() < 0.3) casualtiesArcher = 1
                        
                        battleLogs.append("VICTORY! We ran over the goblin lines and pillaged their treasure storage. ")
                    } else {
                        // High casualities
                        if (state.knightCount > 0) casualtiesKnight = (state.knightCount * 0.5).toInt().coerceAtLeast(1)
                        if (state.archerCount > 0) casualtiesArcher = (state.archerCount * 0.4).toInt().coerceAtLeast(1)
                        battleLogs.append("DEFEAT! The goblins ambushed our legions from spider caverns. We retreated with heavy wounds. ")
                    }
                }
                "siege" -> {
                    // Siege Mountain Fortress (Hard)
                    val requiredPower = 150
                    successChance = (totalStrength.toFloat() / requiredPower).coerceIn(0.05f, 0.90f)
                    success = Random.nextFloat() < successChance
                    battleLogs.append("Our grand forces march with siege engines to conquer a rogue Mountain Fortress.\n")
                    battleLogs.append("Our military offensive power: $totalStrength vs Fortress Defense: $requiredPower.\n")
                    
                    if (success) {
                        goldReward = 500
                        stoneReward = 250
                        woodReward = 100
                        
                        // Medium casualties
                        casualtiesKnight = (state.knightCount * 0.25).toInt().coerceAtLeast(1)
                        casualtiesArcher = (state.archerCount * 0.2).toInt()
                        
                        battleLogs.append("GLORIOUS VICTORY! Our banner is raised on the high ramparts! We seized immense stone deposits and spoils of war. ")
                    } else {
                        // High casualties
                        casualtiesKnight = (state.knightCount * 0.6).toInt().coerceAtLeast(1)
                        casualtiesArcher = (state.archerCount * 0.5).toInt().coerceAtLeast(1)
                        if (state.mageCount > 0) casualtiesMage = 1
                        battleLogs.append("SAD DEFEAT! Boiling oil and boulder trebuchets decimated our infantry columns. ")
                    }
                }
                "dragon" -> {
                    // Slay Ancient Dragon (Legendary Boss)
                    val requiredPower = 350
                    successChance = (totalStrength.toFloat() / requiredPower).coerceIn(0.01f, 0.85f)
                    success = Random.nextFloat() < successChance
                    battleLogs.append("The Royal Host marches to the volcanic lair of the Ancient Red Dragon Balerion!\n")
                    battleLogs.append("Empire Army Combat Power: $totalStrength vs Dragon Flame Power: $requiredPower.\n")
                    
                    if (success) {
                        goldReward = 1500
                        stoneReward = 400
                        
                        // Substantial casualties
                        casualtiesKnight = (state.knightCount * 0.5).toInt().coerceAtLeast(1)
                        casualtiesArcher = (state.archerCount * 0.4).toInt().coerceAtLeast(1)
                        casualtiesMage = (state.mageCount * 0.3).toInt().coerceAtLeast(1)
                        
                        battleLogs.append("EPIC TRIUMPH! The dragon falls with a final arrow through its eye! Emperor ${state.rulerName}\'s names will live forever! Gold mountains and ancient stone are recovered. ")
                    } else {
                        // Decimation
                        casualtiesKnight = state.knightCount
                        casualtiesArcher = state.archerCount
                        casualtiesMage = state.mageCount
                        battleLogs.append("EXTINCTION DEFEAT! Volcanic flame breath vaporized our legions. Entire host disintegrated. ")
                    }
                }
            }

            // Apply losses and rewards
            val finalKnight = (state.knightCount - casualtiesKnight).coerceAtLeast(0)
            val finalArcher = (state.archerCount - casualtiesArcher).coerceAtLeast(0)
            val finalMage = (state.mageCount - casualtiesMage).coerceAtLeast(0)
            val finalPeasant = (state.peasantCount + popReward).coerceAtLeast(1)
            
            val lostCount = casualtiesKnight + casualtiesArcher + casualtiesMage
            val lossLog = if (lostCount > 0) {
                "Losses incurred: $casualtiesKnight Knights, $casualtiesArcher Archers, $casualtiesMage Mages."
            } else "No casualties reported."
            
            battleLogs.append("\n$lossLog")

            val resultingHappiness = if (success) {
                (state.happiness + 15).coerceAtMost(100)
            } else {
                (state.happiness - 15).coerceAtLeast(15)
            }

            val updatedState = state.copy(
                gold = state.gold + goldReward,
                wood = state.wood + woodReward,
                stone = state.stone + stoneReward,
                food = state.food + foodReward,
                peasantCount = finalPeasant,
                knightCount = finalKnight,
                archerCount = finalArcher,
                mageCount = finalMage,
                happiness = resultingHappiness,
                population = finalPeasant + finalKnight + finalArcher + finalMage
            )

            repository.saveEmpireState(updatedState)
            
            val campaignLabel = type.replaceFirstChar { it.uppercase() }
            repository.addLog(
                day = state.day,
                category = "Campaign",
                message = "Expedition [$campaignLabel]: " + battleLogs.toString()
            )

            withContext(Dispatchers.Main) {
                _campaignResult.value = battleLogs.toString()
            }
        }
    }

    fun clearCampaignResult() {
        _campaignResult.value = null
    }

    // Gemini Strategic Advisor
    fun askAdvisor(userQuery: String) {
        val state = empireState.value ?: return
        
        viewModelScope.launch {
            _isAdvisorLoading.value = true
            _advisorResponse.value = null
            
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                // Formulate status prompt
                val statusDetails = """
                    Ruler Name: Emperor/Empress ${state.rulerName}
                    Empire Name: ${state.empireName}
                    Day: ${state.day}
                    Gold: ${state.gold}, Food: ${state.food}, Wood: ${state.wood}, Stone: ${state.stone}
                    Population: ${state.population} (Happiness: ${state.happiness}%)
                    Buildings: Farms=${state.farmCount}, Lumber Mills=${state.lumberMillCount}, Quarries=${state.quarryCount}, Gold Mines=${state.goldMineCount}, Barracks=${state.barracksCount}, Wall Level=${state.castleWallLevel}, Temples=${state.templeCount}
                    Military: Peasants=${state.peasantCount}, Knights=${state.knightCount}, Archers=${state.archerCount}, Mages=${state.mageCount}
                    Researched Technologies: Crop Rotation=${state.cropRotation}, Steel Axes=${state.steelAxes}, Masonry=${state.masonry}, Fortification=${state.fortification}, Chivalry=${state.chivalry}, Arcane Wisdom=${state.arcaneWisdom}
                """.trimIndent()
                
                val promptText = "The ruler asks: '$userQuery'\n\nPlease analyze our empire state and reply in detail as the wise Advisor."

                val systemInstructionText = """
                    You are the loyal, slightly humorous, and highly strategic Royal Grand Advisor to Emperor/Empress ${state.rulerName} in the simulator game "MAM's Empire".
                    Your response must always be in-character, using medieval royal phrasing ("Sire", "Your Majesty", "Our domain", "My liege").
                    Based on the current empire stats provided, give concrete strategic advice on:
                    1. What resources are deficient and what to gather.
                    2. Which building is most crucial to build next.
                    3. Which technology to research next.
                    4. Whether our military is strong enough to attempt a goblin raid, mountain siege, or slay the volcanic dragon.
                    Maintain a friendly, strategic, and deeply engaging persona. Do not mention that you are an AI model. Keep your advice around 3-4 paragraphs.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = "Current Empire Status:\n$statusDetails\n\n$promptText")))
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
                    generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 800)
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "Sire, I must have dozed off... Please ask again! My scrolls are ready."

                _advisorResponse.value = reply
                
                repository.addLog(
                    day = state.day,
                    category = "Advisor",
                    message = "Imperial Council: Strategic consultation with Advisor completed."
                )
                
            } catch (e: Exception) {
                _advisorResponse.value = "Sire, the magical scrying pool is cloudy... Perhaps our connections are severed. (Error: ${e.message})"
            } finally {
                _isAdvisorLoading.value = false
            }
        }
    }

    fun resetGame(rulerName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initializeNewEmpire(rulerName)
            _advisorResponse.value = null
            _campaignResult.value = null
            _currentDilemma.value = null
        }
    }
}

// Dilemma Structure
data class Dilemma(
    val title: String,
    val description: String,
    val option1: DilemmaOption,
    val option2: DilemmaOption
)

data class DilemmaOption(
    val text: String,
    val effect: (EmpireState) -> DilemmaOutcome
)

data class DilemmaOutcome(
    val updatedState: EmpireState,
    val logMessage: String
)

// Dynamic Dilemma Pools depending on state
private fun getDilemmaPool(state: EmpireState): List<Dilemma> {
    return listOf(
        Dilemma(
            title = "Nomadic Tribe",
            description = "A tribe of displaced nomads stands at our wooden gates, pleading for shelter and jobs inside the empire. They offer labor but consume our food.",
            option1 = DilemmaOption("Admit Them (+10 Pop, -150 Food)") { s ->
                DilemmaOutcome(
                    updatedState = s.copy(
                        peasantCount = s.peasantCount + 10,
                        food = (s.food - 150).coerceAtLeast(0),
                        population = s.population + 10,
                        happiness = (s.happiness + 5).coerceAtMost(100)
                    ),
                    logMessage = "We admitted the tribe! Our fields are bustling, but grain reserves depleted."
                )
            },
            option2 = DilemmaOption("Turn Them Away (+10 Happiness, -20 Gold)") { s ->
                DilemmaOutcome(
                    updatedState = s.copy(
                        happiness = (s.happiness - 10).coerceAtLeast(10),
                        gold = (s.gold + 50).coerceAtLeast(0) // pillaged or tax some
                    ),
                    logMessage = "We closed the portcullis. Nomads cursed our name, but security remains intact."
                )
            }
        ),
        Dilemma(
            title = "Merchant Guild Trade Pact",
            description = "The Wealthy Merchant Guild proposes a royal monopoly in exchange for massive upfront taxes. However, citizens will suffer high prices, reducing happiness.",
            option1 = DilemmaOption("Accept Deal (+400 Gold, -15 Happiness)") { s ->
                DilemmaOutcome(
                    updatedState = s.copy(
                        gold = s.gold + 400,
                        happiness = (s.happiness - 15).coerceAtLeast(10)
                    ),
                    logMessage = "Signed the monopoly. Vaults are heavy with gold, but peasants grumble in market alleys."
                )
            },
            option2 = DilemmaOption("Decline Pact (+10 Happiness)") { s ->
                DilemmaOutcome(
                    updatedState = s.copy(
                        happiness = (s.happiness + 10).coerceAtMost(100)
                    ),
                    logMessage = "We kept markets free. Citizens celebrate the Emperor\'s benevolence!"
                )
            }
        ),
        Dilemma(
            title = "Mystical Meteor Fall",
            description = "A burning meteor crashed into the nearby forest, leaving an iridescent, glowing crater. Scholars ask for funds to analyze the heavenly stone.",
            option1 = DilemmaOption("Fund Analysis (-150 Gold, Unlock +1 Mage)") { s ->
                DilemmaOutcome(
                    updatedState = s.copy(
                        gold = (s.gold - 150).coerceAtLeast(0),
                        mageCount = s.mageCount + 1,
                        population = s.population + 1
                    ),
                    logMessage = "Scholars found mystical properties. A wizard apprentice has joined the court!"
                )
            },
            option2 = DilemmaOption("Sell Crater Ore (+200 Stone)") { s ->
                DilemmaOutcome(
                    updatedState = s.copy(
                        stone = s.stone + 200
                    ),
                    logMessage = "We crushed and smelted the space debris. +200 Stone collected."
                )
            }
        ),
        Dilemma(
            title = "Rogue Bandits Attack",
            description = "Scurvy forest bandits have pillaged outlying hamlets! They demand gold, or we must fight them.",
            option1 = DilemmaOption("Pay Ransom (-100 Gold)") { s ->
                DilemmaOutcome(
                    updatedState = s.copy(
                        gold = (s.gold - 100).coerceAtLeast(0),
                        happiness = (s.happiness - 5).coerceAtLeast(10)
                    ),
                    logMessage = "Paid off the thieves. Hamlets are safe, but our pride is wounded."
                )
            },
            option2 = DilemmaOption("Mobilize Defenses (Requires military, risk of damage)") { s ->
                val combatPower = s.knightCount * 10 + s.archerCount * 5
                val success = combatPower >= 20 || Random.nextFloat() < 0.5f
                if (success) {
                    DilemmaOutcome(
                        updatedState = s.copy(
                            gold = s.gold + 50, // loot bandits
                            happiness = (s.happiness + 10).coerceAtMost(100)
                        ),
                        logMessage = "Our archers ambushed the outlaws in the woods! We captured their plunder."
                    )
                } else {
                    DilemmaOutcome(
                        updatedState = s.copy(
                            wood = (s.wood - 100).coerceAtLeast(0),
                            happiness = (s.happiness - 15).coerceAtLeast(10)
                        ),
                        logMessage = "Our defense failed! The bandits burned our lumber sheds before fleeing."
                    )
                }
            }
        )
    )
}
