package com.example.data.backup

import com.example.data.database.AppDatabase
import com.example.data.database.entity.*
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(private val database: AppDatabase) {

    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        // Trips
        val tripsArray = JSONArray()
        val trips = database.tripDao().getAllTripsSnapshot()
        for (trip in trips) {
            val tObj = JSONObject().apply {
                put("id", trip.id)
                put("name", trip.name)
                put("startTimeMillis", trip.startTimeMillis)
                put("endTimeMillis", trip.endTimeMillis ?: JSONObject.NULL)
                put("startOdometer", trip.startOdometer)
                put("endOdometer", trip.endOdometer)
                put("totalGpsDistanceMeters", trip.totalGpsDistanceMeters)
                put("bikeDistanceMeters", trip.bikeDistanceMeters)
                put("walkingDistanceMeters", trip.walkingDistanceMeters)
                put("durationSeconds", trip.durationSeconds)
                put("avgSpeedKmh", trip.avgSpeedKmh)
                put("maxSpeedKmh", trip.maxSpeedKmh)
                put("fuelCost", trip.fuelCost)
                put("tollCost", trip.tollCost)
                put("parkingCost", trip.parkingCost)
                put("otherCost", trip.otherCost)
                put("notes", trip.notes)
                put("startLatitude", trip.startLatitude ?: JSONObject.NULL)
                put("startLongitude", trip.startLongitude ?: JSONObject.NULL)
                put("endLatitude", trip.endLatitude ?: JSONObject.NULL)
                put("endLongitude", trip.endLongitude ?: JSONObject.NULL)
                put("status", trip.status)
            }
            tripsArray.put(tObj)
        }
        root.put("trips", tripsArray)

        // Route points
        val routePointsArray = JSONArray()
        val points = database.tripDao().getAllRoutePointsSnapshot()
        for (pt in points) {
            val pObj = JSONObject().apply {
                put("id", pt.id)
                put("tripId", pt.tripId)
                put("latitude", pt.latitude)
                put("longitude", pt.longitude)
                put("timestamp", pt.timestamp)
                put("speedKmh", pt.speedKmh.toDouble())
                put("accuracy", pt.accuracy.toDouble())
                put("movementType", pt.movementType)
            }
            routePointsArray.put(pObj)
        }
        root.put("routePoints", routePointsArray)

        // Trip expenses
        val tripExpensesArray = JSONArray()
        val tExpenses = database.tripDao().getAllTripExpensesSnapshot()
        for (te in tExpenses) {
            val teObj = JSONObject().apply {
                put("id", te.id)
                put("tripId", te.tripId)
                put("title", te.title)
                put("category", te.category)
                put("amount", te.amount)
                put("notes", te.notes)
                put("dateMillis", te.dateMillis)
            }
            tripExpensesArray.put(teObj)
        }
        root.put("tripExpenses", tripExpensesArray)

        // General Expenses
        val expensesArray = JSONArray()
        val expenses = database.expenseDao().getAllExpensesSnapshot()
        for (exp in expenses) {
            val eObj = JSONObject().apply {
                put("id", exp.id)
                put("dateMillis", exp.dateMillis)
                put("category", exp.category)
                put("description", exp.description)
                put("amount", exp.amount)
                put("paymentMethod", exp.paymentMethod)
                put("notes", exp.notes)
            }
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        // Savings Accounts
        val savingsArray = JSONArray()
        val savings = database.savingsDao().getAllAccountsSnapshot()
        for (sa in savings) {
            val sObj = JSONObject().apply {
                put("id", sa.id)
                put("name", sa.name)
                put("startingAmount", sa.startingAmount)
                put("currentBalance", sa.currentBalance)
                put("targetAmount", sa.targetAmount)
                put("interestRate", sa.interestRate)
                put("interestType", sa.interestType)
                put("interestFrequency", sa.interestFrequency)
                put("startDateMillis", sa.startDateMillis)
                put("maturityDateMillis", sa.maturityDateMillis ?: JSONObject.NULL)
                put("allowOverdraft", sa.allowOverdraft)
                put("notes", sa.notes)
            }
            savingsArray.put(sObj)
        }
        root.put("savingsAccounts", savingsArray)

        // Savings Transactions
        val sTxArray = JSONArray()
        val sTransactions = database.savingsDao().getAllTransactionsSnapshot()
        for (st in sTransactions) {
            val stObj = JSONObject().apply {
                put("id", st.id)
                put("accountId", st.accountId)
                put("dateMillis", st.dateMillis)
                put("amount", st.amount)
                put("type", st.type)
                put("notes", st.notes)
            }
            sTxArray.put(stObj)
        }
        root.put("savingsTransactions", sTxArray)

        // Credit Accounts
        val creditArray = JSONArray()
        val credits = database.creditDao().getAllCreditAccountsSnapshot()
        for (ca in credits) {
            val cObj = JSONObject().apply {
                put("id", ca.id)
                put("personName", ca.personName)
                put("contact", ca.contact)
                put("principalAmount", ca.principalAmount)
                put("interestRate", ca.interestRate)
                put("interestType", ca.interestType)
                put("interestFrequency", ca.interestFrequency)
                put("startDateMillis", ca.startDateMillis)
                put("dueDateMillis", ca.dueDateMillis)
                put("notes", ca.notes)
            }
            creditArray.put(cObj)
        }
        root.put("creditAccounts", creditArray)

        // Credit Payments
        val paymentsArray = JSONArray()
        val payments = database.creditDao().getAllPaymentsSnapshot()
        for (cp in payments) {
            val pObj = JSONObject().apply {
                put("id", cp.id)
                put("creditId", cp.creditId)
                put("dateMillis", cp.dateMillis)
                put("amount", cp.amount)
                put("notes", cp.notes)
            }
            paymentsArray.put(pObj)
        }
        root.put("creditPayments", paymentsArray)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<String> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("version")) {
                return Result.failure(IllegalArgumentException("Invalid backup format: missing version header"))
            }

            // Clear old data safely before restoring
            database.tripDao().clearAllTrips()
            database.expenseDao().clearAllExpenses()
            database.savingsDao().clearAllSavings()
            database.creditDao().clearAllCredit()

            // Restore Trips
            if (root.has("trips")) {
                val tripsArr = root.getJSONArray("trips")
                for (i in 0 until tripsArr.length()) {
                    val t = tripsArr.getJSONObject(i)
                    database.tripDao().insertTrip(
                        Trip(
                            id = t.optLong("id", 0L),
                            name = t.optString("name", "Restored Trip"),
                            startTimeMillis = t.optLong("startTimeMillis", System.currentTimeMillis()),
                            endTimeMillis = if (t.isNull("endTimeMillis")) null else t.optLong("endTimeMillis"),
                            startOdometer = t.optDouble("startOdometer", 0.0),
                            endOdometer = t.optDouble("endOdometer", 0.0),
                            totalGpsDistanceMeters = t.optDouble("totalGpsDistanceMeters", 0.0),
                            bikeDistanceMeters = t.optDouble("bikeDistanceMeters", 0.0),
                            walkingDistanceMeters = t.optDouble("walkingDistanceMeters", 0.0),
                            durationSeconds = t.optLong("durationSeconds", 0L),
                            avgSpeedKmh = t.optDouble("avgSpeedKmh", 0.0),
                            maxSpeedKmh = t.optDouble("maxSpeedKmh", 0.0),
                            fuelCost = t.optDouble("fuelCost", 0.0),
                            tollCost = t.optDouble("tollCost", 0.0),
                            parkingCost = t.optDouble("parkingCost", 0.0),
                            otherCost = t.optDouble("otherCost", 0.0),
                            notes = t.optString("notes", ""),
                            startLatitude = if (t.isNull("startLatitude")) null else t.optDouble("startLatitude"),
                            startLongitude = if (t.isNull("startLongitude")) null else t.optDouble("startLongitude"),
                            endLatitude = if (t.isNull("endLatitude")) null else t.optDouble("endLatitude"),
                            endLongitude = if (t.isNull("endLongitude")) null else t.optDouble("endLongitude"),
                            status = t.optString("status", "COMPLETED")
                        )
                    )
                }
            }

            // Route points
            if (root.has("routePoints")) {
                val ptArr = root.getJSONArray("routePoints")
                val pts = mutableListOf<TripRoutePoint>()
                for (i in 0 until ptArr.length()) {
                    val p = ptArr.getJSONObject(i)
                    pts.add(
                        TripRoutePoint(
                            id = p.optLong("id", 0L),
                            tripId = p.optLong("tripId"),
                            latitude = p.optDouble("latitude"),
                            longitude = p.optDouble("longitude"),
                            timestamp = p.optLong("timestamp"),
                            speedKmh = p.optDouble("speedKmh").toFloat(),
                            accuracy = p.optDouble("accuracy").toFloat(),
                            movementType = p.optString("movementType", "Stopped")
                        )
                    )
                }
                database.tripDao().insertRoutePoints(pts)
            }

            // Trip expenses
            if (root.has("tripExpenses")) {
                val teArr = root.getJSONArray("tripExpenses")
                for (i in 0 until teArr.length()) {
                    val te = teArr.getJSONObject(i)
                    database.tripDao().insertTripExpense(
                        TripExpense(
                            id = te.optLong("id", 0L),
                            tripId = te.optLong("tripId"),
                            title = te.optString("title", "Expense"),
                            category = te.optString("category", "Other"),
                            amount = te.optDouble("amount", 0.0),
                            notes = te.optString("notes", ""),
                            dateMillis = te.optLong("dateMillis", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Expenses
            if (root.has("expenses")) {
                val expArr = root.getJSONArray("expenses")
                val exps = mutableListOf<Expense>()
                for (i in 0 until expArr.length()) {
                    val e = expArr.getJSONObject(i)
                    exps.add(
                        Expense(
                            id = e.optLong("id", 0L),
                            dateMillis = e.optLong("dateMillis", System.currentTimeMillis()),
                            category = e.optString("category", "Other"),
                            description = e.optString("description", ""),
                            amount = e.optDouble("amount", 0.0),
                            paymentMethod = e.optString("paymentMethod", "Cash"),
                            notes = e.optString("notes", "")
                        )
                    )
                }
                database.expenseDao().insertExpenses(exps)
            }

            // Savings Accounts
            if (root.has("savingsAccounts")) {
                val saArr = root.getJSONArray("savingsAccounts")
                val sas = mutableListOf<SavingsAccount>()
                for (i in 0 until saArr.length()) {
                    val s = saArr.getJSONObject(i)
                    sas.add(
                        SavingsAccount(
                            id = s.optLong("id", 0L),
                            name = s.optString("name", "Account"),
                            startingAmount = s.optDouble("startingAmount", 0.0),
                            currentBalance = s.optDouble("currentBalance", 0.0),
                            targetAmount = s.optDouble("targetAmount", 0.0),
                            interestRate = s.optDouble("interestRate", 0.0),
                            interestType = s.optString("interestType", "No Interest"),
                            interestFrequency = s.optString("interestFrequency", "Monthly"),
                            startDateMillis = s.optLong("startDateMillis", System.currentTimeMillis()),
                            maturityDateMillis = if (s.isNull("maturityDateMillis")) null else s.optLong("maturityDateMillis"),
                            allowOverdraft = s.optBoolean("allowOverdraft", false),
                            notes = s.optString("notes", "")
                        )
                    )
                }
                database.savingsDao().insertAccounts(sas)
            }

            // Savings transactions
            if (root.has("savingsTransactions")) {
                val stArr = root.getJSONArray("savingsTransactions")
                val sts = mutableListOf<SavingsTransaction>()
                for (i in 0 until stArr.length()) {
                    val st = stArr.getJSONObject(i)
                    sts.add(
                        SavingsTransaction(
                            id = st.optLong("id", 0L),
                            accountId = st.optLong("accountId"),
                            dateMillis = st.optLong("dateMillis", System.currentTimeMillis()),
                            amount = st.optDouble("amount", 0.0),
                            type = st.optString("type", "Deposit"),
                            notes = st.optString("notes", "")
                        )
                    )
                }
                database.savingsDao().insertTransactions(sts)
            }

            // Credit Accounts
            if (root.has("creditAccounts")) {
                val caArr = root.getJSONArray("creditAccounts")
                val cas = mutableListOf<CreditAccount>()
                for (i in 0 until caArr.length()) {
                    val c = caArr.getJSONObject(i)
                    cas.add(
                        CreditAccount(
                            id = c.optLong("id", 0L),
                            personName = c.optString("personName", "Person"),
                            contact = c.optString("contact", ""),
                            principalAmount = c.optDouble("principalAmount", 0.0),
                            interestRate = c.optDouble("interestRate", 0.0),
                            interestType = c.optString("interestType", "No Interest"),
                            interestFrequency = c.optString("interestFrequency", "Monthly"),
                            startDateMillis = c.optLong("startDateMillis", System.currentTimeMillis()),
                            dueDateMillis = c.optLong("dueDateMillis", System.currentTimeMillis()),
                            notes = c.optString("notes", "")
                        )
                    )
                }
                database.creditDao().insertCreditAccounts(cas)
            }

            // Credit payments
            if (root.has("creditPayments")) {
                val cpArr = root.getJSONArray("creditPayments")
                val cps = mutableListOf<CreditPayment>()
                for (i in 0 until cpArr.length()) {
                    val p = cpArr.getJSONObject(i)
                    cps.add(
                        CreditPayment(
                            id = p.optLong("id", 0L),
                            creditId = p.optLong("creditId"),
                            dateMillis = p.optLong("dateMillis", System.currentTimeMillis()),
                            amount = p.optDouble("amount", 0.0),
                            notes = p.optString("notes", "")
                        )
                    )
                }
                database.creditDao().insertPayments(cps)
            }

            Result.success("Backup restored successfully!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllData() {
        database.tripDao().clearAllTrips()
        database.expenseDao().clearAllExpenses()
        database.savingsDao().clearAllSavings()
        database.creditDao().clearAllCredit()
    }
}
