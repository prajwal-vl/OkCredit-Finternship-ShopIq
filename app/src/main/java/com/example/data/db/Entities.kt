package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_profile")
data class StoreEntity(
    @PrimaryKey val id: Int = 1, // Only 1 store profile persists locally
    val storeName: String,
    val ownerMobile: String,
    val setupCompleted: Boolean = true
)

@Entity(tableName = "inventory_items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val barcode: String? = null,
    val price: Double,
    val costPrice: Double,
    val stock: Int = 0,
    val category: String = "General"
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val billNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val paymentMode: String = "Cash", // Cash, UPI, Card
    val customerMobile: String? = null
)

@Entity(tableName = "bill_items")
data class BillItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val billId: Int,
    val itemName: String,
    val price: Double,
    val quantity: Int,
    val total: Double
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mobile: String,
    val outstandingBalance: Double = 0.0
)

@Entity(tableName = "customer_logs")
data class CustomerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "Purchase" or "Repayment"
    val amount: Double,
    val note: String? = null,
    val billId: Int? = null
)

