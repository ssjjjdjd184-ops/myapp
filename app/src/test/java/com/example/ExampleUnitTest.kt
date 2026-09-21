package com.example

import com.example.data.database.entity.CreditAccount
import com.example.data.database.entity.SavingsAccount
import com.example.location.MovementClassifier
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testMovementClassifierSpeedCategories() {
    val classifier = MovementClassifier()
    assertEquals("Stopped", classifier.classify(0.5))
    assertEquals("Walking", classifier.classify(3.5))
    assertEquals("Bike", classifier.classify(18.0))
    assertEquals("Bike", classifier.classify(40.0))
  }

  @Test
  fun testSavingsSimpleInterestCalculation() {
    val now = System.currentTimeMillis()
    val oneYearAgo = now - (365L * 24 * 3600 * 1000)
    val account = SavingsAccount(
      name = "Fixed Deposit",
      currentBalance = 10000.0,
      startingAmount = 10000.0,
      targetAmount = 20000.0,
      interestRate = 10.0, // 10% per year
      interestType = "Simple Interest",
      interestFrequency = "Yearly",
      startDateMillis = oneYearAgo
    )
    val interest = account.calculateInterest(now)
    // 10,000 * 10% = 1000 approximately (within 5% for leap year drift)
    assertTrue("Expected ~1000 interest, got $interest", interest in 950.0..1050.0)
  }

  @Test
  fun testCreditAccountInterestCalculation() {
    val now = System.currentTimeMillis()
    val oneYearAgo = now - (365L * 24 * 3600 * 1000)
    val credit = CreditAccount(
      personName = "Jane Doe",
      contact = "1234567890",
      principalAmount = 5000.0,
      interestRate = 12.0, // 12% per year
      interestType = "Simple Interest",
      interestFrequency = "Yearly",
      startDateMillis = oneYearAgo,
      dueDateMillis = now
    )
    val interest = credit.calculateAccruedInterest(now)
    // 5000 * 12% = 600
    assertTrue("Expected ~600 interest, got $interest", interest in 570.0..630.0)
  }

  @Test
  fun testTripDistanceAndCostCalculations() {
    val trip = com.example.data.database.entity.Trip(
      name = "Daily Commute",
      startTimeMillis = 1000L,
      endTimeMillis = 5000L,
      startOdometer = 12000.0,
      endOdometer = 12050.0,
      totalGpsDistanceMeters = 50000.0,
      fuelCost = 250.0,
      tollCost = 50.0,
      parkingCost = 20.0
    )
    assertEquals(320.0, trip.totalTripExpense, 0.01)
    assertEquals(50.0, trip.odometerDistance, 0.01)
    // 320 / 50 km = 6.4 per km
    assertEquals(6.4, trip.costPerKm, 0.01)
  }

  @Test
  fun testExpenseEntityCreation() {
    val expense = com.example.data.database.entity.Expense(
      dateMillis = System.currentTimeMillis(),
      category = "Fuel",
      description = "Petrol refill",
      amount = 500.0,
      paymentMethod = "UPI"
    )
    assertEquals("Fuel", expense.category)
    assertEquals(500.0, expense.amount, 0.001)
    assertEquals("UPI", expense.paymentMethod)
  }
}
