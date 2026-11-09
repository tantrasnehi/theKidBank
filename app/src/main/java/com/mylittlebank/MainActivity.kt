package com.mylittlebank

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.withTransaction
import com.mylittlebank.data.*
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

private val Ink = Color(0xFF20334D)
private val Sky = Color(0xFFE8F5FF)
private val Mint = Color(0xFFE6F6EE)
private val Gold = Color(0xFFFFF4D4)
private val Lilac = Color(0xFFF1EBFF)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = BankDatabase.get(applicationContext).bankDao()
        MonthlyInterestScheduler.scheduleCurrentMonth(applicationContext)
        setContent { LittleBankApp(dao) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LittleBankApp(dao: BankDao) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var childName by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("Home") }
    var activities by remember { mutableStateOf<List<ActivityWithRule>>(emptyList()) }
    var available by remember { mutableLongStateOf(0L) }
    var savings by remember { mutableLongStateOf(0L) }
    var rows by remember { mutableStateOf<List<PassbookRow>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var showActivity by remember { mutableStateOf<ActivityWithRule?>(null) }
    var monthlyInterestPercent by remember { mutableIntStateOf(5) }
    var celebrationToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        dao.initialize()
        dao.observeActivities().collect { activities = it }
    }
    LaunchedEffect(Unit) { context.profileDataStore.data.map { it[ProfilePreferenceKeys.childName] ?: "" }.collect { childName = it } }
    LaunchedEffect(Unit) { context.profileDataStore.data.map { it[ProfilePreferenceKeys.monthlySavingsInterestPercent] ?: 5 }.collect { monthlyInterestPercent = it } }
    LaunchedEffect(Unit) { dao.observeBalance("AVAILABLE").collect { available = it } }
    LaunchedEffect(Unit) { dao.observeBalance("SAVINGS").collect { savings = it } }
    LaunchedEffect(Unit) { dao.observePassbook().collect { rows = it } }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF3478C8), secondary = Color(0xFF48A984), background = Color(0xFFF7FAFC),
        surface = Color.White, onSurface = Ink, onPrimary = Color.White
    )) {
        if (childName.isBlank()) {
            WelcomeScreen(nameInput, { nameInput = it }) {
                childName = nameInput.trim()
                scope.launch { context.profileDataStore.edit { it[ProfilePreferenceKeys.childName] = childName } }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val tabletLayout = maxWidth >= 700.dp
            Scaffold(
                topBar = { TopAppBar(title = { Text("My Little Bank", fontWeight = FontWeight.Bold) }, actions = {
                    IconButton(onClick = { selected = "Parent" }) { Icon(Icons.Default.Settings, contentDescription = "Grown-up settings") }
                    Text("🌈", modifier = Modifier.padding(end = 20.dp), fontSize = 24.sp)
                }) },
                bottomBar = if (!tabletLayout) {{
                    NavigationBar(containerColor = Color.White) {
                        listOf("Home", "Earn", "My Bank", "Savings", "Learn").forEach { destination ->
                            val icon = when (destination) {
                                "Home" -> Icons.Default.Home; "Earn" -> Icons.Default.Star
                                "My Bank" -> Icons.Default.AccountBalanceWallet; "Savings" -> Icons.Default.Savings
                                "Learn" -> Icons.Default.School; else -> Icons.Default.Settings
                            }
                            NavigationBarItem(selected == destination, onClick = { selected = destination }, icon = { Icon(icon, destination) }, label = { Text(destination) })
                        }
                    }
                }} else {{}}, snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) }
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (tabletLayout) {
                        NavigationRail(containerColor = Color.White) {
                            listOf("Home", "Earn", "My Bank", "Savings", "Learn").forEach { destination ->
                                val icon = when (destination) {
                                    "Home" -> Icons.Default.Home; "Earn" -> Icons.Default.Star
                                    "My Bank" -> Icons.Default.AccountBalanceWallet; "Savings" -> Icons.Default.Savings
                                    else -> Icons.Default.School
                                }
                                NavigationRailItem(selected == destination, onClick = { selected = destination }, icon = { Icon(icon, destination) }, label = { Text(destination) })
                            }
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (selected) {
                        "Home" -> HomeScreen(childName, available, savings, rows, onGo = { selected = it })
                        "Earn" -> EarnScreen(activities.filter { it.activity.enabled }) { showActivity = it }
                        "My Bank" -> BankScreen(available, savings, rows)
                        "Savings" -> SavingsScreen(savings, available, monthlyInterestPercent, onTransfer = { amount, save ->
                            scope.launch {
                                runCatching { dao.transfer(amount, save, System.currentTimeMillis(), UUID.randomUUID().toString()) }
                                    .onSuccess { message = if (save) "Points moved to savings!" else "Points moved back to your balance." }
                                    .onFailure { message = it.message ?: "That transfer could not be completed." }
                            }
                        })
                        "Learn" -> LearnScreen()
                        else -> ParentScreen(activities, onAddActivity = { title, emoji, unit, points, quantityLimit, pointsLimit ->
                            scope.launch {
                                runCatching { dao.addActivity(title, emoji, unit, points, quantityLimit, pointsLimit) }
                                    .onSuccess { message = "$title was added to activities." }
                                    .onFailure { message = it.message ?: "The activity could not be added." }
                            }
                        }, onToggle = { key, enabled -> scope.launch { dao.setActivityEnabled(key, enabled) } }, onEditActivity = { key, title, emoji, unit, points, quantityLimit, pointsLimit ->
                            scope.launch {
                                runCatching { dao.updateActivityConfiguration(key, title, emoji, unit, points, quantityLimit, pointsLimit) }
                                    .onSuccess { message = "$title was updated." }
                                    .onFailure { message = it.message ?: "The activity could not be updated." }
                            }
                        }, onDeleteActivity = { key ->
                            scope.launch {
                                runCatching { dao.deleteCustomActivity(key) }
                                    .onSuccess { message = "$it was deleted. Past passbook entries are still saved." }
                                    .onFailure { message = it.message ?: "The activity could not be deleted." }
                            }
                        }, onName = { newName ->
                            childName = newName.trim(); scope.launch { context.profileDataStore.edit { it[ProfilePreferenceKeys.childName] = childName } }
                        }, onInterestRate = { newRate ->
                            scope.launch {
                                context.profileDataStore.edit { it[ProfilePreferenceKeys.monthlySavingsInterestPercent] = newRate }
                                message = "Monthly savings interest set to $newRate%."
                            }
                        }, monthlyInterestPercent = monthlyInterestPercent, currentName = childName)
                    }
                    }
                }
            }
            }
        }
        if (showActivity != null) {
            ActivityDialog(showActivity!!, onDismiss = { showActivity = null }, onSubmit = { quantity, note ->
                val activity = showActivity!!
                scope.launch {
                    runCatching { dao.postReward(activity.activity.key, quantity, note, UUID.randomUUID().toString(), System.currentTimeMillis()) }
                        .onSuccess { message = "You earned ${formatPoints(it)} points! Great job!"; celebrationToken++ }
                        .onFailure { message = it.message ?: "Please try again." }
                    showActivity = null
                }
            })
        }
        message?.let { text ->
            AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("Yay!") } }, text = { Text(text) })
        }
        ConfettiCelebration(celebrationToken)
    }
}

@Composable
private fun WelcomeScreen(name: String, onName: (String) -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Sky).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🐷", fontSize = 72.sp)
        Text("Welcome to My Little Bank!", fontSize = 30.sp, color = Ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text("A happy place to earn, save, and learn.", fontSize = 18.sp, color = Ink)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(name, onName, label = { Text("What should we call you?") }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onContinue, enabled = name.trim().isNotEmpty(), modifier = Modifier.heightIn(min = 56.dp)) { Text("Let's get started!", fontSize = 18.sp) }
        Spacer(Modifier.height(12.dp)); Text("Your points are pretend money for learning.", color = Ink.copy(alpha = .75f))
    }
}

@Composable
private fun HomeScreen(name: String, available: Long, savings: Long, rows: List<PassbookRow>, onGo: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Hi, $name! 👋", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BalanceCard("Your points", available, "⭐", Sky, Modifier.weight(1f)) { onGo("My Bank") }
                BalanceCard("In savings", savings, "🐷", Mint, Modifier.weight(1f)) { onGo("Savings") }
            }
        }
        item { Text("What would you like to do?", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Earn points", "✨", Gold, Modifier.weight(1f)) { onGo("Earn") }
                QuickAction("Money School", "📚", Lilac, Modifier.weight(1f)) { onGo("Learn") }
            }
        }
        item { Text("Recent activity", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink) }
        if (rows.isEmpty()) item { Text("Your first adventure is waiting. Try an activity to earn your first points!", color = Ink.copy(alpha = .75f)) }
        items(rows.take(4)) { TransactionLine(it) }
    }
}

@Composable
private fun BalanceCard(title: String, amount: Long, emoji: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.heightIn(min = 138.dp), colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("$emoji  $title", color = Ink, fontWeight = FontWeight.Medium)
            Text("${formatPoints(amount)} pts", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

@Composable
private fun QuickAction(title: String, emoji: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.heightIn(min = 104.dp), colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 27.sp); Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EarnScreen(activities: List<ActivityWithRule>, onActivity: (ActivityWithRule) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Choose an activity", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { Text("Do your best, then tell us what you completed.", color = Ink.copy(alpha = .75f)) }
        items(activities) { item ->
            Card(onClick = { onActivity(item) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 82.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.activity.emoji, fontSize = 30.sp); Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) { Text(item.activity.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink); Text("${formatPoints(item.ratePoints)} points per ${item.activity.unit}", color = Ink.copy(alpha = .75f)) }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Choose ${item.activity.title}")
                }
            }
        }
    }
}

@Composable
private fun ActivityDialog(activity: ActivityWithRule, onDismiss: () -> Unit, onSubmit: (Long, String) -> Unit) {
    var quantityText by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val quantity = quantityText.toLongOrNull() ?: 0L
    val earned = runCatching { Math.multiplyExact(activity.ratePoints, quantity.coerceAtLeast(0)) }.getOrDefault(0L)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("${activity.activity.emoji} ${activity.activity.title}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("How many ${activity.activity.unit}s did you finish?")
            OutlinedTextField(quantityText, { quantityText = it.filter(Char::isDigit) }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Add a note (optional)") }, singleLine = true)
            Text("You'll earn $earned points", fontWeight = FontWeight.Bold, color = Color(0xFF247B56))
        } },
        confirmButton = { Button(onClick = { if (!submitting) { submitting = true; onSubmit(quantity, note) } }, enabled = quantity > 0 && !submitting) { Text(if (submitting) "Saving…" else "Submit & Earn") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not yet") } })
}

@Composable
private fun BankScreen(available: Long, savings: Long, rows: List<PassbookRow>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("My Bank", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { Text("Available: ${formatPoints(available)} points     •     Saved: ${formatPoints(savings)} points", color = Ink) }
        item { Text("Passbook", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink) }
        if (rows.isEmpty()) item { Text("Your activity will show up here.", color = Ink.copy(alpha = .7f)) }
        items(rows) { TransactionLine(it) }
    }
}

@Composable
private fun TransactionLine(row: PassbookRow) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(when (row.kind) { "REWARD" -> "⭐"; "INTEREST" -> "✨"; else -> "🐷" }, fontSize = 24.sp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(row.description, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(row.createdAt)), fontSize = 12.sp, color = Ink.copy(alpha = .65f))
            }
            val sign = if (row.pointsDelta >= 0) "+" else "−"
            Text("$sign${formatPoints(kotlin.math.abs(row.pointsDelta))}", color = if (row.pointsDelta >= 0) Color(0xFF247B56) else Ink, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SavingsScreen(savings: Long, available: Long, monthlyInterestPercent: Int, onTransfer: (Long, Boolean) -> Unit) {
    var amount by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("My Savings 🐷", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { Text("A little saved today can help with a big dream tomorrow.", color = Ink) }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Sky)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("What are savings?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Savings are points you set aside for later. They stay yours, and you can move them back whenever you need.", color = Ink)
                    Text("At the end of each month, the bank adds $monthlyInterestPercent% extra points to your savings. Those extra points are called interest.", color = Ink)
                    Text("For example, 100 points saved at month-end earns $monthlyInterestPercent extra points at this rate.", fontSize = 13.sp, color = Ink.copy(alpha = .75f))
                }
            }
        }
        item { BalanceCard("Saved points", savings, "🌱", Mint, Modifier.fillMaxWidth()) {} }
        item { OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Points to move") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }
        item { Button(onClick = { amount.toLongOrNull()?.let { onTransfer(it, true) } }, enabled = (amount.toLongOrNull() ?: 0) > 0 && available >= (amount.toLongOrNull() ?: Long.MAX_VALUE), modifier = Modifier.heightIn(min = 52.dp)) { Text("Put points into savings") } }
        item { OutlinedButton(onClick = { amount.toLongOrNull()?.let { onTransfer(it, false) } }, enabled = (amount.toLongOrNull() ?: 0) > 0 && savings >= (amount.toLongOrNull() ?: Long.MAX_VALUE), modifier = Modifier.heightIn(min = 52.dp)) { Text("Move points back") } }
        item { Text("Goal tracking is coming soon. For now, you can keep points safe in savings.", color = Ink.copy(alpha = .7f)) }
    }
}

@Composable
private fun LearnScreen() {
    val lessons = listOf("What is money?" to "Money can be earned, saved, and used to make choices.", "Earn and save" to "Saving means keeping some points for a goal later.", "Add it up" to "If you earn 5 points twice, you have 10 points.", "Needs and wants" to "Needs help us stay safe and healthy. Wants are extra things we enjoy.")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Money School 📚", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { Text("Short lessons for curious minds. Learning never changes your bank balance.", color = Ink) }
        items(lessons) { (title, detail) -> Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Lilac)) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(6.dp)); Text(detail, color = Ink) } } }
    }
}

@Composable
private fun ParentScreen(
    activities: List<ActivityWithRule>,
    onAddActivity: (String, String, String, Long, Long?, Long?) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEditActivity: (String, String, String, String, Long, Long?, Long?) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onName: (String) -> Unit,
    onInterestRate: (Int) -> Unit,
    monthlyInterestPercent: Int,
    currentName: String
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    var editKey by remember { mutableStateOf<String?>(null) }
    var deleteKey by remember { mutableStateOf<String?>(null) }
    var addingActivity by remember { mutableStateOf(false) }
    var interestRateText by remember(monthlyInterestPercent) { mutableStateOf(monthlyInterestPercent.toString()) }
    val parsedInterestRate = interestRateText.toIntOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Grown-up corner", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { Text("Choose activities and set their rewards. Changes apply to future activities.", color = Ink) }
        item { OutlinedTextField(name, { name = it }, label = { Text("Child's name") }, trailingIcon = { TextButton(onClick = { if (name.isNotBlank()) onName(name) }) { Text("Save") } }, singleLine = true) }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Mint)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Savings interest", fontWeight = FontWeight.Bold, color = Ink)
                    Text("Choose the monthly rate. Interest is added to savings once at month-end.", color = Ink)
                    OutlinedTextField(
                        interestRateText,
                        { interestRateText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Percent per month (0–100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Button(
                        onClick = { parsedInterestRate?.let(onInterestRate) },
                        enabled = parsedInterestRate != null && parsedInterestRate in 0..100
                    ) { Text("Save interest rate") }
                }
            }
        }
        item { Button(onClick = { addingActivity = true }, modifier = Modifier.heightIn(min = 48.dp)) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Add activity") } }
        items(activities) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(item.activity.enabled, onCheckedChange = { onToggle(item.activity.key, it) })
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text("${item.activity.emoji} ${item.activity.title}", fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("${formatPoints(item.ratePoints)} points per ${item.activity.unit}", fontSize = 13.sp, color = Ink.copy(alpha = .75f))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { editKey = item.activity.key }) { Text("Edit") }
                        if (item.activity.key !in BuiltInActivityKeys) {
                            TextButton(onClick = { deleteKey = item.activity.key }) { Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Delete") }
                        }
                    }
                }
            }
        }
        item { Text("PIN protection is turned off for this first version.", fontSize = 13.sp, color = Ink.copy(alpha = .65f)) }
    }
    if (addingActivity) {
        var title by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("⭐") }
        var unit by remember { mutableStateOf("") }
        var newRate by remember { mutableStateOf("") }
        var quantityCap by remember { mutableStateOf("") }
        var pointsCap by remember { mutableStateOf("") }
        val parsedRate = newRate.toLongOrNull()
        val quantityLimit = quantityCap.toLongOrNull()
        val pointsLimit = pointsCap.toLongOrNull()
        val valid = title.isNotBlank() && title.length <= 40 && unit.isNotBlank() && unit.length <= 24 &&
            (parsedRate ?: 0L) > 0 && (quantityCap.isBlank() || (quantityLimit ?: 0L) > 0) &&
            (pointsCap.isBlank() || (pointsLimit ?: 0L) > 0)
        AlertDialog(onDismissRequest = { addingActivity = false }, title = { Text("Add an activity") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(40) }, label = { Text("Activity name") }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it }, label = { Text("Emoji") }, singleLine = true)
                OutlinedTextField(unit, { unit = it.take(24) }, label = { Text("Count each one as (for example, page)") }, singleLine = true)
                OutlinedTextField(newRate, { newRate = it.filter(Char::isDigit) }, label = { Text("Points per unit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(quantityCap, { quantityCap = it.filter(Char::isDigit) }, label = { Text("Daily quantity limit (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(pointsCap, { pointsCap = it.filter(Char::isDigit) }, label = { Text("Daily points limit (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            } },
            confirmButton = { Button(onClick = {
                onAddActivity(title.trim(), emoji, unit.trim(), parsedRate ?: 0L, quantityLimit, pointsLimit)
                addingActivity = false
            }, enabled = valid) { Text("Add activity") } },
            dismissButton = { TextButton(onClick = { addingActivity = false }) { Text("Cancel") } })
    }
    editKey?.let { key ->
        val activity = activities.firstOrNull { it.activity.key == key }
        var title by remember(key) { mutableStateOf(activity?.activity?.title.orEmpty()) }
        var emoji by remember(key) { mutableStateOf(activity?.activity?.emoji.orEmpty()) }
        var unit by remember(key) { mutableStateOf(activity?.activity?.unit.orEmpty()) }
        var rateText by remember(key) { mutableStateOf(activity?.ratePoints?.toString().orEmpty()) }
        var quantityCap by remember(key) { mutableStateOf(activity?.dailyQuantityLimit?.toString().orEmpty()) }
        var pointsCap by remember(key) { mutableStateOf(activity?.dailyPointsLimit?.toString().orEmpty()) }
        val parsedRate = rateText.toLongOrNull()
        val quantityLimit = quantityCap.toLongOrNull()
        val pointsLimit = pointsCap.toLongOrNull()
        val valid = activity != null && title.isNotBlank() && unit.isNotBlank() && (parsedRate ?: 0L) > 0 &&
            (quantityCap.isBlank() || (quantityLimit ?: 0L) > 0) && (pointsCap.isBlank() || (pointsLimit ?: 0L) > 0)
        AlertDialog(onDismissRequest = { editKey = null }, title = { Text("Edit activity") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(40) }, label = { Text("Activity name") }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it }, label = { Text("Emoji") }, singleLine = true)
                OutlinedTextField(unit, { unit = it.take(24) }, label = { Text("Count each one as (for example, page)") }, singleLine = true)
                OutlinedTextField(rateText, { rateText = it.filter(Char::isDigit) }, label = { Text("Points per unit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(quantityCap, { quantityCap = it.filter(Char::isDigit) }, label = { Text("Daily quantity limit (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(pointsCap, { pointsCap = it.filter(Char::isDigit) }, label = { Text("Daily points limit (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Text("Daily limits reset at local midnight.", fontSize = 12.sp)
            } },
            confirmButton = { Button(onClick = {
                onEditActivity(key, title.trim(), emoji, unit.trim(), parsedRate ?: 0L, quantityLimit, pointsLimit)
                editKey = null
            }, enabled = valid) { Text("Save activity") } },
            dismissButton = { TextButton(onClick = { editKey = null }) { Text("Cancel") } })
    }
    deleteKey?.let { key ->
        val activity = activities.firstOrNull { it.activity.key == key }
        AlertDialog(
            onDismissRequest = { deleteKey = null },
            title = { Text("Delete activity?") },
            text = { Text("${activity?.activity?.title ?: "This activity"} will be removed from the activity list. Existing passbook entries and balances will be kept.") },
            confirmButton = { Button(onClick = { onDeleteActivity(key); deleteKey = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteKey = null }) { Text("Cancel") } }
        )
    }
}

private enum class ConfettiStyle { RAIN, FIREWORKS, POP, SPRAY, DOUBLE_CANNON }

private data class ConfettiPiece(
    val originX: Float,
    val originY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val delay: Float,
    val size: Float,
    val rotation: Float,
    val color: Color,
    val shape: Int
)

private fun createConfettiPieces(style: ConfettiStyle, palette: List<Color>): List<ConfettiPiece> {
    return List(Random.nextInt(240, 321)) { index ->
        val fromLeft = index % 2 == 0
        val originX = if (fromLeft) 0.015f + Random.nextFloat() * 0.06f else 0.925f + Random.nextFloat() * 0.06f
        val originY: Float
        val velocityX: Float
        val velocityY: Float
        when (style) {
            ConfettiStyle.RAIN -> {
                originY = -0.1f - Random.nextFloat() * 0.35f
                velocityX = if (fromLeft) 0.18f + Random.nextFloat() * 0.42f else -0.18f - Random.nextFloat() * 0.42f
                velocityY = 0.9f + Random.nextFloat() * 0.45f
            }
            ConfettiStyle.FIREWORKS -> {
                originY = 0.28f + Random.nextFloat() * 0.35f
                val angle = if (fromLeft) {
                    (-Math.PI / 2.0 + Random.nextDouble() * Math.PI).toFloat()
                } else {
                    (Math.PI / 2.0 + Random.nextDouble() * Math.PI).toFloat()
                }
                val speed = 0.45f + Random.nextFloat() * 0.48f
                velocityX = kotlin.math.cos(angle) * speed
                velocityY = kotlin.math.sin(angle) * speed
            }
            ConfettiStyle.POP -> {
                originY = 0.42f + Random.nextFloat() * 0.18f
                val angle = if (fromLeft) {
                    (-Math.PI / 2.0 + Random.nextDouble() * (Math.PI * 0.82)).toFloat()
                } else {
                    (Math.PI / 2.0 + Random.nextDouble() * (Math.PI * 0.82)).toFloat()
                }
                val speed = 0.48f + Random.nextFloat() * 0.55f
                velocityX = kotlin.math.cos(angle) * speed
                velocityY = kotlin.math.sin(angle) * speed - 0.12f
            }
            ConfettiStyle.SPRAY -> {
                originY = 0.48f + Random.nextFloat() * 0.4f
                velocityX = if (fromLeft) 0.55f + Random.nextFloat() * 0.42f else -0.55f - Random.nextFloat() * 0.42f
                velocityY = -0.3f - Random.nextFloat() * 0.72f
            }
            ConfettiStyle.DOUBLE_CANNON -> {
                originY = 0.56f + Random.nextFloat() * 0.34f
                val spread = Random.nextFloat()
                velocityX = if (fromLeft) 0.25f + spread * 1.35f else -0.25f - spread * 1.35f
                velocityY = -0.15f - Random.nextFloat() * 1.05f
            }
        }
        ConfettiPiece(
            originX = originX,
            originY = originY,
            velocityX = velocityX,
            velocityY = velocityY,
            delay = Random.nextFloat() * 0.2f,
            size = 7f + Random.nextFloat() * 14f,
            rotation = (Random.nextFloat() - 0.5f) * 1440f,
            color = palette.random(),
            shape = Random.nextInt(3)
        )
    }
}

@Composable
private fun ConfettiCelebration(trigger: Int) {
    val progress = remember { Animatable(1f) }
    val colors = listOf(Color(0xFFFFC857), Color(0xFFFF7799), Color(0xFF51B9F2), Color(0xFF53C995), Color(0xFFAA8BF2), Color(0xFFFF8A4C), Color(0xFF54D7D0))
    var style by remember { mutableStateOf(ConfettiStyle.RAIN) }
    var pieces by remember { mutableStateOf(emptyList<ConfettiPiece>()) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            style = ConfettiStyle.values().filter { it != style }.random()
            pieces = createConfettiPieces(style, colors.shuffled().take(Random.nextInt(5, colors.size + 1)))
            progress.snapTo(0f)
            if (ValueAnimator.areAnimatorsEnabled()) {
                progress.animateTo(1f, animationSpec = tween(durationMillis = 3400, easing = LinearEasing))
            } else {
                progress.snapTo(1f)
            }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        if (trigger == 0 || progress.value >= 1f || pieces.isEmpty()) return@Canvas
        pieces.forEachIndexed { index, piece ->
            val fall = ((progress.value - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            val drift = kotlin.math.sin((fall * 8.0 + piece.rotation).toDouble()).toFloat() * 0.012f * size.width
            val x = piece.originX * size.width + piece.velocityX * size.width * fall + drift
            val y = piece.originY * size.height + piece.velocityY * size.height * fall + styleGravity(style) * size.height * fall * fall
            rotate(piece.rotation * fall, pivot = Offset(x, y)) {
                when (piece.shape) {
                    0 -> drawCircle(piece.color, radius = piece.size * 0.5f, center = Offset(x, y))
                    1 -> drawRect(piece.color, topLeft = Offset(x - piece.size * 0.4f, y - piece.size * 0.8f), size = Size(piece.size * 0.8f, piece.size * 1.6f))
                    else -> {
                        val star = Path()
                        for (point in 0 until 10) {
                            val angle = Math.PI * point / 5.0 - Math.PI / 2.0
                            val radius = if (point % 2 == 0) piece.size * 0.65f else piece.size * 0.28f
                            val px = x + kotlin.math.cos(angle).toFloat() * radius
                            val py = y + kotlin.math.sin(angle).toFloat() * radius
                            if (point == 0) star.moveTo(px, py) else star.lineTo(px, py)
                        }
                        star.close()
                        drawPath(star, piece.color)
                    }
                }
            }
        }
    }
}

private fun styleGravity(style: ConfettiStyle): Float = when (style) {
    ConfettiStyle.RAIN -> 0.36f
    ConfettiStyle.FIREWORKS -> 0.85f
    ConfettiStyle.POP -> 0.72f
    ConfettiStyle.SPRAY -> 0.64f
    ConfettiStyle.DOUBLE_CANNON -> 0.7f
}

private fun formatPoints(points: Long): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(points)
