package com.example.ui.screens.savings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.entity.SavingsAccount
import com.example.ui.components.ProgressRing
import com.example.ui.components.SubMetricItem
import com.example.ui.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accountsList.collectAsStateWithLifecycle()
    val totalSavings by viewModel.totalSavings.collectAsStateWithLifecycle()
    val totalInterest by viewModel.totalInterestEarned.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.selectedAccountTransactions.collectAsStateWithLifecycle()
    val pref = viewModel.prefManager
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<SavingsAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<SavingsAccount?>(null) }

    // Transaction dialog
    var transactionTargetAccount by remember { mutableStateOf<SavingsAccount?>(null) }
    var transactionType by remember { mutableStateOf("Deposit") }
    var transactionAmountText by remember { mutableStateOf("") }
    var transactionNotes by remember { mutableStateOf("") }
    var transactionError by remember { mutableStateOf<String?>(null) }

    // Account form dialog fields
    var accName by remember { mutableStateOf("") }
    var accStartingBalance by remember { mutableStateOf("") }
    var accTarget by remember { mutableStateOf("") }
    var accInterestRate by remember { mutableStateOf("") }
    var accInterestType by remember { mutableStateOf("No Interest") }
    var accFrequency by remember { mutableStateOf("Monthly") }
    var accOverdraft by remember { mutableStateOf(false) }
    var accNotes by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    accName = ""
                    accStartingBalance = ""
                    accTarget = ""
                    accInterestRate = ""
                    accInterestType = "No Interest"
                    accFrequency = "Monthly"
                    accOverdraft = false
                    accNotes = ""
                    accountToEdit = null
                    showAddAccountDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("NEW SAVINGS GOAL") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search savings accounts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            // Total Savings Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL SAVINGS BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = pref.formatMoney(totalSavings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (totalInterest > 0) {
                            Text(
                                text = "Accrued Interest: +${pref.formatMoney(totalInterest)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No savings accounts or goals",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create accounts for emergency funds, goals, or fixed deposits",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        val accrued = acc.calculateInterest(System.currentTimeMillis())
                        val pct = if (acc.targetAmount > 0) ((acc.currentBalance / acc.targetAmount) * 100).toFloat().coerceIn(0f, 100f) else 100f

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectAccount(acc) },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (acc.interestType != "No Interest" && acc.interestRate > 0) {
                                            Text(
                                                text = "${acc.interestRate}% ${acc.interestType} (${acc.interestFrequency})",
                                                fontSize = 11.sp,
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            accountToEdit = acc
                                            accName = acc.name
                                            accStartingBalance = acc.startingAmount.toString()
                                            accTarget = acc.targetAmount.toString()
                                            accInterestRate = acc.interestRate.toString()
                                            accInterestType = acc.interestType
                                            accFrequency = acc.interestFrequency
                                            accOverdraft = acc.allowOverdraft
                                            accNotes = acc.notes
                                            showAddAccountDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { accountToDelete = acc }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Current Balance", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = pref.formatMoney(acc.currentBalance),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (acc.targetAmount > 0) {
                                            Text(
                                                text = "Target: ${pref.formatMoney(acc.targetAmount)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (accrued > 0) {
                                            Text(
                                                text = "Interest: +${pref.formatMoney(accrued)}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    if (acc.targetAmount > 0) {
                                        ProgressRing(
                                            progressPercent = pct,
                                            modifier = Modifier.size(75.dp),
                                            label = "Goal"
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action buttons: Deposit / Withdraw / Transactions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            transactionTargetAccount = acc
                                            transactionType = "Deposit"
                                            transactionAmountText = ""
                                            transactionNotes = ""
                                            transactionError = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Deposit", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            transactionTargetAccount = acc
                                            transactionType = "Withdrawal"
                                            transactionAmountText = ""
                                            transactionNotes = ""
                                            transactionError = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Withdraw", fontSize = 12.sp)
                                    }
                                }

                                // Expanded Transactions for selected account
                                if (selectedAccount?.id == acc.id && transactions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Recent Transactions", style = MaterialTheme.typography.labelSmall)
                                    transactions.take(5).forEach { tx ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = tx.notes.ifBlank { tx.type },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = dateFormat.format(Date(tx.dateMillis)),
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = (if (tx.type == "Deposit") "+" else "-") + pref.formatMoney(tx.amount),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (tx.type == "Deposit") Color(0xFF10B981) else Color(0xFFEF4444)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deleteTransaction(tx) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(14.dp))
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

    // TRANSACTION DIALOG (DEPOSIT / WITHDRAWAL)
    if (transactionTargetAccount != null) {
        val target = transactionTargetAccount!!
        AlertDialog(
            onDismissRequest = { transactionTargetAccount = null },
            title = { Text("$transactionType — ${target.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current Balance: ${pref.formatMoney(target.currentBalance)}")
                    OutlinedTextField(
                        value = transactionAmountText,
                        onValueChange = { transactionAmountText = it },
                        label = { Text("Amount (${pref.currency.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = transactionNotes,
                        onValueChange = { transactionNotes = it },
                        label = { Text("Notes") },
                        placeholder = { Text("Salary, monthly deposit, emergency expense, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (transactionError != null) {
                        Text(
                            text = transactionError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = transactionAmountText.toDoubleOrNull() ?: 0.0
                        viewModel.addTransaction(
                            account = target,
                            amount = amount,
                            type = transactionType,
                            dateMillis = System.currentTimeMillis(),
                            notes = transactionNotes,
                            onError = { err -> transactionError = err }
                        )
                        if (transactionError == null) {
                            transactionTargetAccount = null
                        }
                    }
                ) {
                    Text("Confirm $transactionType")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionTargetAccount = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD / EDIT ACCOUNT DIALOG
    if (showAddAccountDialog) {
        val interestTypes = listOf("No Interest", "Simple Interest", "Compound Interest")
        val frequencies = listOf("Daily", "Monthly", "Quarterly", "Yearly")

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text(if (accountToEdit == null) "New Savings Account / Goal" else "Edit Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text("Account Name *") },
                        placeholder = { Text("Emergency Fund / Fixed Deposit / Bike") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (accountToEdit == null) {
                        OutlinedTextField(
                            value = accStartingBalance,
                            onValueChange = { accStartingBalance = it },
                            label = { Text("Starting Balance (${pref.currency.value})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = accTarget,
                        onValueChange = { accTarget = it },
                        label = { Text("Target Goal Amount (${pref.currency.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Interest Calculation", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        interestTypes.forEach { type ->
                            FilterChip(
                                selected = accInterestType == type,
                                onClick = { accInterestType = type },
                                label = { Text(type, fontSize = 10.sp) }
                            )
                        }
                    }

                    if (accInterestType != "No Interest") {
                        OutlinedTextField(
                            value = accInterestRate,
                            onValueChange = { accInterestRate = it },
                            label = { Text("Annual Interest Rate (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Compounding Frequency", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            frequencies.forEach { freq ->
                                FilterChip(
                                    selected = accFrequency == freq,
                                    onClick = { accFrequency = freq },
                                    label = { Text(freq, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Allow Overdraft", style = MaterialTheme.typography.bodyMedium)
                            Text("Permit balance to drop below zero", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = accOverdraft,
                            onCheckedChange = { accOverdraft = it }
                        )
                    }

                    OutlinedTextField(
                        value = accNotes,
                        onValueChange = { accNotes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val starting = accStartingBalance.toDoubleOrNull() ?: 0.0
                        val target = accTarget.toDoubleOrNull() ?: 0.0
                        val rate = accInterestRate.toDoubleOrNull() ?: 0.0

                        if (accountToEdit == null) {
                            viewModel.addAccount(
                                name = accName.ifBlank { "Savings Account" },
                                startingAmount = starting,
                                targetAmount = target,
                                interestRate = rate,
                                interestType = accInterestType,
                                interestFrequency = accFrequency,
                                startDateMillis = System.currentTimeMillis(),
                                maturityDateMillis = null,
                                allowOverdraft = accOverdraft,
                                notes = accNotes
                            )
                        } else {
                            viewModel.updateAccount(
                                accountToEdit!!.copy(
                                    name = accName.ifBlank { accountToEdit!!.name },
                                    targetAmount = target,
                                    interestRate = rate,
                                    interestType = accInterestType,
                                    interestFrequency = accFrequency,
                                    allowOverdraft = accOverdraft,
                                    notes = accNotes
                                )
                            )
                        }
                        showAddAccountDialog = false
                    }
                ) {
                    Text(if (accountToEdit == null) "Create Account" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE ACCOUNT DIALOG
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete '${accountToDelete!!.name}'? All transaction records for this account will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        accountToDelete?.let { viewModel.deleteAccount(it) }
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
