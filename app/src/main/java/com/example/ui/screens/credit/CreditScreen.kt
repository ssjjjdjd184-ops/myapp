package com.example.ui.screens.credit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.data.database.entity.CreditAccount
import com.example.ui.components.SubMetricItem
import com.example.ui.viewmodel.CreditItemUiModel
import com.example.ui.viewmodel.CreditViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreditScreen(
    viewModel: CreditViewModel,
    modifier: Modifier = Modifier
) {
    val creditList by viewModel.creditList.collectAsStateWithLifecycle()
    val totalLent by viewModel.totalLent.collectAsStateWithLifecycle()
    val totalCollected by viewModel.totalCollected.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val payments by viewModel.selectedAccountPayments.collectAsStateWithLifecycle()
    val pref = viewModel.prefManager
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    var showAddCreditDialog by remember { mutableStateOf(false) }
    var creditToEdit by remember { mutableStateOf<CreditAccount?>(null) }
    var creditToDelete by remember { mutableStateOf<CreditAccount?>(null) }

    // Add Payment dialog
    var paymentTargetAccount by remember { mutableStateOf<CreditAccount?>(null) }
    var paymentAmountText by remember { mutableStateOf("") }
    var paymentNotes by remember { mutableStateOf("") }
    var paymentError by remember { mutableStateOf<String?>(null) }

    // Form fields
    var personName by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var principalText by remember { mutableStateOf("") }
    var interestRateText by remember { mutableStateOf("") }
    var interestType by remember { mutableStateOf("No Interest") }
    var interestFrequency by remember { mutableStateOf("Monthly") }
    var dueDaysOffset by remember { mutableIntStateOf(30) }
    var notes by remember { mutableStateOf("") }

    val outstanding = creditList.sumOf { it.remainingBalance }
    val overdue = creditList.filter { it.status == "OVERDUE" }.sumOf { it.remainingBalance }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    personName = ""
                    contact = ""
                    principalText = ""
                    interestRateText = ""
                    interestType = "No Interest"
                    interestFrequency = "Monthly"
                    dueDaysOffset = 30
                    notes = ""
                    creditToEdit = null
                    showAddCreditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("LEND MONEY") },
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
                placeholder = { Text("Search person name, phone, notes...") },
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

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "ACTIVE", "OVERDUE", "PAID").forEach { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { viewModel.setStatusFilter(filter) },
                        label = { Text(filter, fontSize = 12.sp) }
                    )
                }
            }

            // Overview Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL OUTSTANDING BALANCE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = pref.formatMoney(outstanding),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (overdue > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.Handshake,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Lent: ${pref.formatMoney(totalLent)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Collected: ${pref.formatMoney(totalCollected)}", fontSize = 12.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                        if (overdue > 0) {
                            Text("Overdue: ${pref.formatMoney(overdue)}", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (creditList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No credit accounts found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Track money lent to friends, clients, or family with interest & due dates",
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
                    items(creditList, key = { it.account.id }) { item ->
                        val acc = item.account
                        val statusColor = when (item.status) {
                            "PAID" -> Color(0xFF10B981)
                            "OVERDUE" -> Color(0xFFEF4444)
                            else -> Color(0xFF3B82F6)
                        }

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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = acc.personName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = statusColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = item.status,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (acc.contact.isNotBlank()) {
                                            Text(acc.contact, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            creditToEdit = acc
                                            personName = acc.personName
                                            contact = acc.contact
                                            principalText = acc.principalAmount.toString()
                                            interestRateText = acc.interestRate.toString()
                                            interestType = acc.interestType
                                            interestFrequency = acc.interestFrequency
                                            notes = acc.notes
                                            showAddCreditDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { creditToDelete = acc }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Remaining Balance", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = pref.formatMoney(item.remainingBalance),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.status == "OVERDUE") Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Due Date", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = dateFormat.format(Date(acc.dueDateMillis)),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (item.status == "OVERDUE") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SubMetricItem(label = "Principal", value = pref.formatMoney(acc.principalAmount))
                                    SubMetricItem(label = "Interest Due", value = pref.formatMoney(item.accruedInterest))
                                    SubMetricItem(label = "Total Paid", value = pref.formatMoney(item.totalPaid))
                                }

                                if (acc.interestType != "No Interest" && acc.interestRate > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Interest: ${acc.interestRate}% ${acc.interestType} (${acc.interestFrequency})",
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Record payment button
                                Button(
                                    onClick = {
                                        paymentTargetAccount = acc
                                        paymentAmountText = if (item.remainingBalance > 0) item.remainingBalance.toString() else ""
                                        paymentNotes = ""
                                        paymentError = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Record Payment", fontSize = 13.sp)
                                }

                                // Expanded Payment History
                                if (selectedAccount?.id == acc.id && payments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Payment History", style = MaterialTheme.typography.labelSmall)
                                    payments.forEach { pay ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = pay.notes.ifBlank { "Payment received" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = dateFormat.format(Date(pay.dateMillis)),
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "+" + pref.formatMoney(pay.amount),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deletePayment(pay) },
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

    // RECORD PAYMENT DIALOG
    if (paymentTargetAccount != null) {
        val target = paymentTargetAccount!!
        AlertDialog(
            onDismissRequest = { paymentTargetAccount = null },
            title = { Text("Record Payment from ${target.personName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Payment Amount (${pref.currency.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentNotes,
                        onValueChange = { paymentNotes = it },
                        label = { Text("Notes (e.g. Cash / UPI transfer)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (paymentError != null) {
                        Text(paymentError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountText.toDoubleOrNull() ?: 0.0
                        viewModel.addPayment(
                            creditId = target.id,
                            amount = amount,
                            dateMillis = System.currentTimeMillis(),
                            notes = paymentNotes,
                            onError = { err -> paymentError = err }
                        )
                        if (paymentError == null) {
                            paymentTargetAccount = null
                        }
                    }
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentTargetAccount = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD / EDIT CREDIT DIALOG
    if (showAddCreditDialog) {
        val interestTypes = listOf("No Interest", "Simple Interest", "Compound Interest")
        val frequencies = listOf("Monthly", "Quarterly", "Yearly")
        val dueOptions = listOf(Pair("30 Days", 30), Pair("60 Days", 60), Pair("90 Days", 90), Pair("180 Days", 180), Pair("1 Year", 365))

        AlertDialog(
            onDismissRequest = { showAddCreditDialog = false },
            title = { Text(if (creditToEdit == null) "Lend Money / Add Credit" else "Edit Credit Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Person Name *") },
                        placeholder = { Text("e.g. John Doe / Sharma Ji") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact / Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = principalText,
                        onValueChange = { principalText = it },
                        label = { Text("Principal Amount (${pref.currency.value}) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Due Date Period", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dueOptions.forEach { (label, days) ->
                            FilterChip(
                                selected = dueDaysOffset == days,
                                onClick = { dueDaysOffset = days },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Interest Type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        interestTypes.forEach { type ->
                            FilterChip(
                                selected = interestType == type,
                                onClick = { interestType = type },
                                label = { Text(type, fontSize = 10.sp) }
                            )
                        }
                    }

                    if (interestType != "No Interest") {
                        OutlinedTextField(
                            value = interestRateText,
                            onValueChange = { interestRateText = it },
                            label = { Text("Annual Interest Rate (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Interest Frequency", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            frequencies.forEach { freq ->
                                FilterChip(
                                    selected = interestFrequency == freq,
                                    onClick = { interestFrequency = freq },
                                    label = { Text(freq, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val principal = principalText.toDoubleOrNull() ?: 0.0
                        val rate = interestRateText.toDoubleOrNull() ?: 0.0
                        val dueMillis = System.currentTimeMillis() + (dueDaysOffset * 24L * 3600L * 1000L)

                        if (principal > 0) {
                            if (creditToEdit == null) {
                                viewModel.addCreditAccount(
                                    personName = personName.ifBlank { "Client" },
                                    contact = contact,
                                    principalAmount = principal,
                                    interestRate = rate,
                                    interestType = interestType,
                                    interestFrequency = interestFrequency,
                                    startDateMillis = System.currentTimeMillis(),
                                    dueDateMillis = dueMillis,
                                    notes = notes
                                )
                            } else {
                                viewModel.updateCreditAccount(
                                    creditToEdit!!.copy(
                                        personName = personName.ifBlank { creditToEdit!!.personName },
                                        contact = contact,
                                        principalAmount = principal,
                                        interestRate = rate,
                                        interestType = interestType,
                                        interestFrequency = interestFrequency,
                                        dueDateMillis = dueMillis,
                                        notes = notes
                                    )
                                )
                            }
                            showAddCreditDialog = false
                        }
                    }
                ) {
                    Text(if (creditToEdit == null) "Save Credit" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCreditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (creditToDelete != null) {
        AlertDialog(
            onDismissRequest = { creditToDelete = null },
            title = { Text("Delete Credit Account") },
            text = { Text("Are you sure you want to delete the record for '${creditToDelete!!.personName}'? All payment history will also be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        creditToDelete?.let { viewModel.deleteCreditAccount(it) }
                        creditToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { creditToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
