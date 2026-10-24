package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ThemePalette
import java.text.DecimalFormat

data class CalculationHistoryItem(
    val expression: String,
    val result: String
)

@Composable
fun CalculatorView(
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    var displayValue by remember { mutableStateOf("0") }
    var expressionLine by remember { mutableStateOf("") }
    var storedValue by remember { mutableStateOf<Double?>(null) }
    var currentOperator by remember { mutableStateOf<String?>(null) }
    var isNewEntry by remember { mutableStateOf(true) }
    var history by remember { mutableStateOf<List<CalculationHistoryItem>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }

    val decimalFormat = remember { DecimalFormat("#,##0.########") }

    fun formatNumber(raw: String): String {
        return try {
            val num = raw.toDouble()
            if (raw.endsWith(".")) {
                "${decimalFormat.format(num)}."
            } else if (raw.contains(".") && raw.endsWith("0")) {
                raw
            } else {
                decimalFormat.format(num)
            }
        } catch (_: Exception) {
            raw
        }
    }

    fun onDigit(d: String) {
        if (isNewEntry) {
            displayValue = if (d == "00") "0" else d
            isNewEntry = false
        } else {
            if (displayValue == "0" && d != ".") {
                displayValue = if (d == "00") "0" else d
            } else if (displayValue.length < 14) {
                if (d == "." && displayValue.contains(".")) {
                    // Ignore duplicate decimal point
                } else {
                    displayValue += d
                }
            }
        }
    }

    fun onOperator(op: String) {
        val currentNum = displayValue.toDoubleOrNull() ?: 0.0
        if (storedValue != null && currentOperator != null && !isNewEntry) {
            // Perform pending calculation first
            val prev = storedValue!!
            val calculated = when (currentOperator) {
                "+" -> prev + currentNum
                "-" -> prev - currentNum
                "×" -> prev * currentNum
                "÷" -> if (currentNum != 0.0) prev / currentNum else Double.NaN
                else -> currentNum
            }
            if (calculated.isNaN() || calculated.isInfinite()) {
                displayValue = "エラー"
                storedValue = null
                currentOperator = null
                expressionLine = ""
                isNewEntry = true
                return
            } else {
                storedValue = calculated
                val formatted = decimalFormat.format(calculated)
                displayValue = formatted.replace(",", "")
            }
        } else {
            storedValue = currentNum
        }
        currentOperator = op
        expressionLine = "${formatNumber(storedValue.toString())} $op"
        isNewEntry = true
    }

    fun onEquals() {
        val currentNum = displayValue.toDoubleOrNull() ?: 0.0
        if (storedValue != null && currentOperator != null) {
            val prev = storedValue!!
            val op = currentOperator!!
            val calculated = when (op) {
                "+" -> prev + currentNum
                "-" -> prev - currentNum
                "×" -> prev * currentNum
                "÷" -> if (currentNum != 0.0) prev / currentNum else Double.NaN
                else -> currentNum
            }

            if (calculated.isNaN() || calculated.isInfinite()) {
                displayValue = "エラー"
                expressionLine = "$prev $op $currentNum ="
                storedValue = null
                currentOperator = null
                isNewEntry = true
            } else {
                val fullExpr = "${formatNumber(prev.toString())} $op ${formatNumber(currentNum.toString())}"
                val resultStr = decimalFormat.format(calculated)
                history = listOf(CalculationHistoryItem(fullExpr, resultStr)) + history.take(20)

                expressionLine = "$fullExpr ="
                displayValue = resultStr.replace(",", "")
                storedValue = null
                currentOperator = null
                isNewEntry = true
            }
        }
    }

    fun onClear() {
        displayValue = "0"
        expressionLine = ""
        storedValue = null
        currentOperator = null
        isNewEntry = true
    }

    fun onBackspace() {
        if (!isNewEntry && displayValue.isNotEmpty() && displayValue != "エラー") {
            displayValue = if (displayValue.length > 1) {
                displayValue.dropLast(1)
            } else {
                "0"
            }
        }
    }

    fun onPercent() {
        val num = displayValue.toDoubleOrNull() ?: return
        val percentVal = num / 100.0
        displayValue = decimalFormat.format(percentVal).replace(",", "")
        isNewEntry = true
    }

    fun onTax(rate: Double, isAdd: Boolean) {
        val num = displayValue.toDoubleOrNull() ?: return
        val result = if (isAdd) num * (1.0 + rate) else num / (1.0 + rate)
        val resultStr = decimalFormat.format(result)
        val label = if (isAdd) "税込(10%)" else "税抜(10%)"
        history = listOf(CalculationHistoryItem("${formatNumber(num.toString())} $label", resultStr)) + history.take(20)
        expressionLine = "${formatNumber(num.toString())} $label ="
        displayValue = resultStr.replace(",", "")
        isNewEntry = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Top Toolbar: History button & Mode badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.buttonColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00B0FF),
                        modifier = Modifier.size(6.dp)
                    ) {}
                    Text(
                        text = "スマート電卓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                }
            }

            IconButton(
                onClick = { showHistory = !showHistory },
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (showHistory) palette.accentColor.copy(alpha = 0.25f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "計算履歴",
                    tint = if (showHistory) palette.accentColor else palette.secondaryTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showHistory) {
            // History View
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.itemBackgroundColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "計算履歴 (${history.size}件)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        if (history.isNotEmpty()) {
                            TextButton(
                                onClick = { history = emptyList() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("履歴消去", fontSize = 11.sp, color = Color(0xFFEF5350))
                            }
                        }
                    }

                    HorizontalDivider(color = palette.cardBorderColor, thickness = 0.5.dp)

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "まだ計算履歴はありません",
                                fontSize = 12.sp,
                                color = palette.secondaryTextColor
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(history) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.containerColor,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, palette.cardBorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            displayValue = item.result.replace(",", "")
                                            isNewEntry = true
                                            showHistory = false
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = item.expression,
                                            fontSize = 11.sp,
                                            color = palette.secondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "= ${item.result}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Main Display Screen
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.itemBackgroundColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Formula / intermediate expression
                    Text(
                        text = expressionLine.ifEmpty { " " },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Primary Result / Input value
                    val formattedResult = formatNumber(displayValue)
                    Text(
                        text = formattedResult,
                        fontSize = if (formattedResult.length > 10) 22.sp else 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Keypad Layout (4 columns x 5 rows)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Tax, Clear, Backspace
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcButton("税込", palette.buttonColor, palette.accentColor, Modifier.weight(1f)) { onTax(0.10, true) }
                    CalcButton("税抜", palette.buttonColor, palette.accentColor, Modifier.weight(1f)) { onTax(0.10, false) }
                    CalcButton("C", palette.buttonColor, Color(0xFFFF7043), Modifier.weight(1f)) { onBackspace() }
                    CalcButton("AC", palette.buttonColor, Color(0xFFEF5350), Modifier.weight(1f)) { onClear() }
                }

                // Row 2: 7, 8, 9, ÷
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcButton("7", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("7") }
                    CalcButton("8", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("8") }
                    CalcButton("9", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("9") }
                    CalcButton("÷", palette.buttonColor, Color(0xFF29B6F6), Modifier.weight(1f)) { onOperator("÷") }
                }

                // Row 3: 4, 5, 6, ×
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcButton("4", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("4") }
                    CalcButton("5", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("5") }
                    CalcButton("6", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("6") }
                    CalcButton("×", palette.buttonColor, Color(0xFF29B6F6), Modifier.weight(1f)) { onOperator("×") }
                }

                // Row 4: 1, 2, 3, -
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcButton("1", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("1") }
                    CalcButton("2", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("2") }
                    CalcButton("3", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("3") }
                    CalcButton("-", palette.buttonColor, Color(0xFF29B6F6), Modifier.weight(1f)) { onOperator("-") }
                }

                // Row 5: 0, 00, ., +, =
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcButton("0", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("0") }
                    CalcButton("00", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit("00") }
                    CalcButton(".", palette.containerColor, palette.textColor, Modifier.weight(1f)) { onDigit(".") }
                    CalcButton("+", palette.buttonColor, Color(0xFF29B6F6), Modifier.weight(1f)) { onOperator("+") }
                    CalcButton("=", palette.accentColor, Color.Black, Modifier.weight(1.2f)) { onEquals() }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = modifier
            .fillMaxHeight()
            .testTag("calc_btn_$text")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (text.length > 2) 13.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
