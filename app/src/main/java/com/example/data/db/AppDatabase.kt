package com.example.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM store_profile LIMIT 1")
    suspend fun getStoreProfile(): StoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreProfile(store: StoreEntity)

    @Query("DELETE FROM store_profile")
    suspend fun clearProfile()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Int): ItemEntity?

    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): ItemEntity?

    @Query("SELECT * FROM inventory_items WHERE name LIKE :query OR barcode LIKE :query OR category LIKE :query")
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getBillById(billId: Int): BillEntity?

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getBillItems(billId: Int): List<BillItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Query("SELECT * FROM customers WHERE mobile = :mobile LIMIT 1")
    suspend fun getCustomerByMobile(mobile: String): CustomerEntity?

    @Query("SELECT * FROM customer_logs WHERE customerId = :customerId ORDER BY timestamp DESC")
    suspend fun getLogsForCustomer(customerId: Int): List<CustomerLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerLog(log: CustomerLogEntity): Long
}

@Database(entities = [StoreEntity::class, ItemEntity::class, BillEntity::class, BillItemEntity::class, CustomerEntity::class, CustomerLogEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun itemDao(): ItemDao
    abstract fun billDao(): BillDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kirana_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
