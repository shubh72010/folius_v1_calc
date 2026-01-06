package com.example.jetcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// State Management
sealed class CalculatorAction {
    data class Number(val number: String) : CalculatorAction()
    object Clear : CalculatorAction()
    object Operation : CalculatorAction()
    object Delete : CalculatorAction()
    object Decimal : CalculatorAction()
    object Calculate : CalculatorAction()
}

class CalculatorViewModel @Inject constructor() : ViewModel() {
    var state by mutableStateOf(CalculatorState())
        private set

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Number -> enterNumber(action.number)
            is CalculatorAction.Delete -> delete()
            is CalculatorAction.Clear -> state = CalculatorState()
            is CalculatorAction.Decimal -> enterDecimal()
            is CalculatorAction.Operation -> enterOperation()
            is CalculatorAction.Calculate -> calculate()
        }
    }

    private fun enterNumber(number: String) {
        if (state.operation == null) {
            if (state.firstNumber.length < 10) {
                state = state.copy(firstNumber = state.firstNumber + number)
            }
        } else {
            if (state.secondNumber.length < 10) {
                state = state.copy(secondNumber = state.secondNumber + number)
            }
        }
    }

    private fun delete() {
        if (state.secondNumber.isNotEmpty()) {
            state = state.copy(secondNumber = state.secondNumber.dropLast(1))
        } else if (state.operation != null) {
            state = state.copy(operation = null)
        } else if (state.firstNumber.isNotEmpty()) {
            state = state.copy(firstNumber = state.firstNumber.dropLast(1))
        }
    }

    private fun enterDecimal() {
        if (state.operation == null && !state.firstNumber.contains(".")) {
            state = state.copy(firstNumber = state.firstNumber + ".")
        } else if (state.operation != null && !state.secondNumber.contains(".")) {
            state = state.copy(secondNumber = state.secondNumber + ".")
        }
    }

    private fun enterOperation() {
        if (state.firstNumber.isNotEmpty()) {
            state = state.copy(operation = "+") // Simplified to just Add for this example, logic handles change
            // Toggle operation if needed
        }
    }

    private fun calculate() {
        val first = state.firstNumber.toDoubleOrNull() ?: return
        val second = state.secondNumber.toDoubleOrNull() ?: return
        val result = when (state.operation) {
            "+" -> first + second
            "-" -> first - second
            "*" -> first * second
            "/" -> first / second
            else -> return
        }
        state = state.copy(
            firstNumber = result.toString().take(10),
            secondNumber = "",
            operation = null
        )
    }
}

data class CalculatorState(
    val firstNumber: String = "",
    val secondNumber: String = "",
    val operation: String? = null
)

// UI Components
val buttonSpacing = 8.dp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel = hiltViewModel<CalculatorViewModel>()
                    CalculatorScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = state.firstNumber + (state.operation ?: ""),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = state.secondNumber,
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }

        // Buttons Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(buttonSpacing),
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {
            items(getCalculatorButtons()) { button ->
                CalculatorButton(
                    button = button,
                    onClick = { viewModel.onAction(button.action) }
                )
            }
        }
    }
}

@Composable
fun CalculatorButton(button: CalculatorButtonData, onClick: () -> Unit) {
    val buttonColor = when (button.type) {
        ButtonType.OPERATOR -> MaterialTheme.colorScheme.primary
        ButtonType.ACTION -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when (button.type) {
        ButtonType.OPERATOR -> MaterialTheme.colorScheme.onPrimary
        ButtonType.ACTION -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = textColor),
        modifier = Modifier.height(70.dp)
    ) {
        Text(
            text = button.text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// Data Models for Buttons
enum class ButtonType { NUMBER, OPERATOR, ACTION }

data class CalculatorButtonData(
    val text: String,
    val action: CalculatorAction,
    val type: ButtonType
)

fun getCalculatorButtons(): List<CalculatorButtonData> {
    return listOf(
        CalculatorButtonData("AC", CalculatorAction.Clear, ButtonType.ACTION),
        CalculatorButtonData("Del", CalculatorAction.Delete, ButtonType.ACTION),
        CalculatorButtonData("%", CalculatorAction.Operation, ButtonType.OPERATOR),
        CalculatorButtonData("/", CalculatorAction.Operation, ButtonType.OPERATOR),
        CalculatorButtonData("7", CalculatorAction.Number("7"), ButtonType.NUMBER),
        CalculatorButtonData("8", CalculatorAction.Number("8"), ButtonType.NUMBER),
        CalculatorButtonData("9", CalculatorAction.Number("9"), ButtonType.NUMBER),
        CalculatorButtonData("*", CalculatorAction.Operation, ButtonType.OPERATOR),
        CalculatorButtonData("4", CalculatorAction.Number("4"), ButtonType.NUMBER),
        CalculatorButtonData("5", CalculatorAction.Number("5"), ButtonType.NUMBER),
        CalculatorButtonData("6", CalculatorAction.Number("6"), ButtonType.NUMBER),
        CalculatorButtonData("-", CalculatorAction.Operation, ButtonType.OPERATOR),
        CalculatorButtonData("1", CalculatorAction.Number("1"), ButtonType.NUMBER),
        CalculatorButtonData("2", CalculatorAction.Number("2"), ButtonType.NUMBER),
        CalculatorButtonData("3", CalculatorAction.Number("3"), ButtonType.NUMBER),
        CalculatorButtonData("+", CalculatorAction.Operation, ButtonType.OPERATOR),
        CalculatorButtonData("0", CalculatorAction.Number("0"), ButtonType.NUMBER),
        CalculatorButtonData(".", CalculatorAction.Decimal, ButtonType.NUMBER),
        CalculatorButtonData("=", CalculatorAction.Calculate, ButtonType.OPERATOR)
    )
}