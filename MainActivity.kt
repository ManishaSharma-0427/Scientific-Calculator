package com.example.scientificcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scientificcalculator.ui.theme.ScientificCalculatorTheme
import kotlinx.coroutines.launch

// Color Palette
val ColorBackground = Color(0xFF07111F)
val ColorSurface = Color(0xFF172133)
val ColorTextScientific = Color(0xFF00E5D4)
val ColorOperator = Color(0xFF7545E8)
val ColorAC = Color(0xFFFF6666)
val ColorEquals = Color(0xFF00D9D2)
val ColorWhite = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScientificCalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ColorBackground
                ) {
                    ScientificCalculatorApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificCalculatorApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // State
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var settings by remember { mutableStateOf(CalculatorSettings()) }
    
    val engine = remember { CalculatorEngine() }

    val onButtonClick: (String) -> Unit = { input ->
        when (input) {
            "AC" -> { expression = ""; result = "0" }
            "⌫" -> {
                if (expression.isNotEmpty()) expression = expression.dropLast(1)
                if (expression.isEmpty()) result = "0"
            }
            "=" -> {
                if (expression.isNotEmpty()) {
                    val res = engine.evaluate(expression, settings.angleMode == AngleMode.DEG, settings.resultFormat)
                    result = res
                    CalculatorHistory.add(expression, res)
                }
            }
            "DEG", "RAD" -> {
                val newMode = if (settings.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
                settings = settings.copy(angleMode = newMode)
            }
            "sin", "cos", "tan", "log", "ln", "sin⁻¹", "cos⁻¹", "tan⁻¹", "√" -> expression += "$input("
            "x²" -> expression += "^2"
            "xʸ" -> expression += "^"
            "+/−" -> {
                expression = toggleSign(expression)
            }
            "x!" -> expression += "!"
            else -> expression += input
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = ColorSurface,
                drawerContentColor = ColorWhite
            ) {
                HistoryPanel(
                    onItemClick = { item ->
                        expression = item.expression
                        result = item.result
                        scope.launch { drawerState.close() }
                    },
                    onClearHistory = { CalculatorHistory.clear() }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Header(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    settings = settings,
                    onSettingsChange = { settings = it }
                )
            },
            containerColor = ColorBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                DisplayArea(expression, result)
                Spacer(modifier = Modifier.height(16.dp))
                CalculatorGrid(settings.angleMode == AngleMode.DEG, onButtonClick, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun Header(
    onMenuClick: () -> Unit,
    settings: CalculatorSettings,
    onSettingsChange: (CalculatorSettings) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "History", tint = ColorWhite)
        }
        Text(
            text = "Scientific Calculator",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorWhite
        )
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = ColorWhite)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(ColorSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("Angle: ${settings.angleMode}", color = ColorWhite) },
                    onClick = {
                        val nextMode = if (settings.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
                        onSettingsChange(settings.copy(angleMode = nextMode))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Format: ${settings.resultFormat}", color = ColorWhite) },
                    onClick = {
                        val nextFormat = if (settings.resultFormat == ResultFormat.STANDARD) ResultFormat.SCIENTIFIC else ResultFormat.STANDARD
                        onSettingsChange(settings.copy(resultFormat = nextFormat))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Vibration: ${if (settings.vibrationEnabled) "ON" else "OFF"}", color = ColorWhite) },
                    onClick = {
                        onSettingsChange(settings.copy(vibrationEnabled = !settings.vibrationEnabled))
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun DisplayArea(expression: String, result: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(ColorSurface, RoundedCornerShape(24.dp))
            .border(1.dp, ColorWhite.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            // Clear Display Icon
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Clear Display",
                tint = ColorWhite.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { /* Could clear display */ }
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                // Expression (Smaller, Muted)
                Text(
                    text = expression.ifEmpty { " " },
                    fontSize = 20.sp,
                    color = ColorWhite.copy(alpha = 0.6f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
                
                // Result (Larger, White, Adaptive size)
                val resultFontSize = when {
                    result.length > 15 -> 32.sp
                    result.length > 10 -> 40.sp
                    else -> 52.sp
                }
                
                Text(
                    text = result,
                    fontSize = resultFontSize,
                    fontWeight = FontWeight.Bold,
                    color = ColorWhite,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
fun HistoryPanel(onItemClick: (HistoryItem) -> Unit, onClearHistory: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ColorWhite,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (CalculatorHistory.items.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No history yet", color = ColorWhite.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(CalculatorHistory.items) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(item.expression, color = ColorWhite.copy(alpha = 0.6f), fontSize = 16.sp)
                        Text("= ${item.result}", color = ColorWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = ColorWhite.copy(alpha = 0.1f), modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        
        Button(
            onClick = onClearHistory,
            colors = ButtonDefaults.buttonColors(containerColor = ColorAC),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Clear History")
        }
    }
}

@Composable
fun CalculatorGrid(isDegreeMode: Boolean, onButtonClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf(if (isDegreeMode) "DEG" else "RAD", "⌫", "AC"),
        listOf("sin", "cos", "tan", "log"),
        listOf("ln", "√", "x²", "xʸ"),
        listOf("π", "e", "(", ")"),
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "−"),
        listOf("sin⁻¹", "cos⁻¹", "tan⁻¹", "+"),
        listOf("+/−", "0", ".", "=")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { label ->
                    CalcButton(
                        label = label,
                        onClick = { onButtonClick(label) },
                        modifier = Modifier.weight(if (row.size == 3) 1.33f else 1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CalcButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = when (label) {
        "AC" -> ColorAC
        "=" -> ColorEquals
        "÷", "×", "−", "+", "⌫" -> ColorOperator
        else -> ColorSurface
    }

    val contentColor = when (label) {
        "sin", "cos", "tan", "log", "ln", "√", "x²", "xʸ", "π", "e", "(", ")", "sin⁻¹", "cos⁻¹", "tan⁻¹" -> ColorTextScientific
        else -> ColorWhite
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = ColorWhite.copy(alpha = 0.2f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Toggles the sign of the last operand in the expression.
 */
fun toggleSign(expression: String): String {
    if (expression.isEmpty()) return "-"
    
    // Find the last number in the expression using regex
    val regex = Regex("(-?\\d+\\.?\\d*)$")
    val match = regex.find(expression)
    
    return if (match != null) {
        val lastNumber = match.value
        val prefix = expression.substring(0, match.range.first)
        val toggledNumber = if (lastNumber.startsWith("-")) {
            lastNumber.substring(1)
        } else {
            "-$lastNumber"
        }
        prefix + toggledNumber
    } else {
        // If no number at the end, just append a minus or handle it gracefully
        if (expression.endsWith("-")) expression.dropLast(1) else expression + "-"
    }
}
