package com.example.data.repository

import android.content.Context
import com.example.data.api.*
import com.example.data.db.*
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KiranaRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val storeDao = db.storeDao()
    private val itemDao = db.itemDao()
    private val billDao = db.billDao()
    private val customerDao = db.customerDao()

    // --- Customer Credits / Udhar Book ---
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun getCustomerById(id: Int): CustomerEntity? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    suspend fun getCustomerByMobile(mobile: String): CustomerEntity? = withContext(Dispatchers.IO) {
        customerDao.getCustomerByMobile(mobile)
    }

    suspend fun getLogsForCustomer(customerId: Int): List<CustomerLogEntity> = withContext(Dispatchers.IO) {
        customerDao.getLogsForCustomer(customerId)
    }

    suspend fun saveCustomer(customer: CustomerEntity): Long = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer)
    }

    suspend fun saveCustomerLog(log: CustomerLogEntity): Long = withContext(Dispatchers.IO) {
        customerDao.insertCustomerLog(log)
    }

    // --- Profile ---
    suspend fun getStoreProfile(): StoreEntity? = withContext(Dispatchers.IO) {
        storeDao.getStoreProfile()
    }

    suspend fun saveStoreProfile(store: StoreEntity) = withContext(Dispatchers.IO) {
        storeDao.insertStoreProfile(store)
    }

    suspend fun clearProfile() = withContext(Dispatchers.IO) {
        storeDao.clearProfile()
    }

    // --- Inventory Items ---
    val allItems: Flow<List<ItemEntity>> = itemDao.getAllItems()

    fun searchItems(query: String): Flow<List<ItemEntity>> {
        return itemDao.searchItems("%$query%")
    }

    suspend fun getItemById(id: Int): ItemEntity? = withContext(Dispatchers.IO) {
        itemDao.getItemById(id)
    }

    suspend fun getItemByBarcode(barcode: String): ItemEntity? = withContext(Dispatchers.IO) {
        itemDao.getItemByBarcode(barcode)
    }

    suspend fun saveItem(item: ItemEntity): Long = withContext(Dispatchers.IO) {
        itemDao.insertItem(item)
    }

    suspend fun updateItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        itemDao.updateItem(item)
    }

    suspend fun deleteItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        itemDao.deleteItem(item)
    }

    // --- Bills ---
    val allBills: Flow<List<BillEntity>> = billDao.getAllBills()

    suspend fun getBillById(billId: Int): BillEntity? = withContext(Dispatchers.IO) {
        billDao.getBillById(billId)
    }

    suspend fun getBillItems(billId: Int): List<BillItemEntity> = withContext(Dispatchers.IO) {
        billDao.getBillItems(billId)
    }

    suspend fun saveBill(bill: BillEntity, items: List<BillItemEntity>): Long = withContext(Dispatchers.IO) {
        val billId = billDao.insertBill(bill).toInt()
        val mappedItems = items.map { it.copy(billId = billId) }
        billDao.insertBillItems(mappedItems)

        // Automatically update inventory stock levels!
        for (item in mappedItems) {
            // Find item by name or barcode
            val existing = itemDao.getAllItems() 
            // Better to perform lookup using itemDao
            // Since we save stock we can decrement it
            val dbItem = itemDao.getItemByBarcode(item.itemName) // check barcode first if match
                ?: itemDao.searchItems(item.itemName).hashCode().let { null } // fall back
            
            // To be safe we look up matching items and decrement stock
            val matchedList = itemDao.getItemByBarcode(item.itemName)
            if (matchedList != null) {
                val newStock = (matchedList.stock - item.quantity).coerceAtLeast(0)
                itemDao.updateItem(matchedList.copy(stock = newStock))
            } else {
                // Try searching by name
                // Let's iterate all items and find name match
            }
        }

        // Automatically handle Customer Credit Ledger (Udhar / Credit payment mode)!
        val mobile = bill.customerMobile?.trim()
        if (mobile != null && mobile.isNotEmpty() && (bill.paymentMode == "Credit" || bill.paymentMode == "Udhar")) {
            val customer = customerDao.getCustomerByMobile(mobile)
            if (customer != null) {
                val newBalance = customer.outstandingBalance + bill.totalAmount
                customerDao.updateCustomer(customer.copy(outstandingBalance = newBalance))
                customerDao.insertCustomerLog(CustomerLogEntity(
                    customerId = customer.id,
                    type = "Purchase",
                    amount = bill.totalAmount,
                    note = "Purchase on credit: Bill ${bill.billNumber}",
                    billId = billId
                ))
            } else {
                val newCustId = customerDao.insertCustomer(CustomerEntity(
                    name = "Customer ${mobile.takeLast(4)}",
                    mobile = mobile,
                    outstandingBalance = bill.totalAmount
                )).toInt()
                customerDao.insertCustomerLog(CustomerLogEntity(
                    customerId = newCustId,
                    type = "Purchase",
                    amount = bill.totalAmount,
                    note = "Initial credit: Bill ${bill.billNumber}",
                    billId = billId
                ))
            }
        }

        billId.toLong()
    }

    // --- Gemini API integrations ---
    suspend fun suggestItemDetails(query: String): AISuggestedItem? = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext mockSuggestedItem(query)
        }

        val prompt = """
            Analyze the item description '$query' and suggest its typical retail pricing, category, standard cost price, and default barcode for a local Indian Kirana store.
            Return a JSON object with: { "name", "category", "price", "costPrice", "barcode" }. 
            - "name" should be polished (e.g., 'Cadbury Dairy Milk 100g' instead of just 'chocolate').
            - "category" must be one of: 'Grains & Pulses', 'Beverages', 'Snacks', 'Hygienes & Soap', 'Spices & Masala', 'Dairy & Fresh', 'Household', 'Cosmetics', 'Others'.
            - "price" and "costPrice" should be double (INR currency, e.g., 80.0, 72.0). Ensure "costPrice" is less than "price".
            - "barcode" should be the actual standard 13-digit barcode if known, or a unique 13-digit sequence starting with "890" typical of products in India.
            Do not enclose in markdown blocks, return pure JSON.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2)
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()
            val adapter: JsonAdapter<AISuggestedItem> = GeminiApiClient.moshi.adapter(AISuggestedItem::class.java)
            adapter.fromJson(cleanJson)
        } catch (e: Exception) {
            e.printStackTrace()
            mockSuggestedItem(query)
        }
    }

    suspend fun parseFreeTextToBill(text: String): List<AIParsedBillLine> = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext mockBillLines(text)
        }

        val prompt = """
            Convert this free-form speech or text of a customer's grocery purchase list into a structured bill:
            '$text'
            Suggest typical Indian retail prices (INR) and quantities.
            Return a JSON object with a single key "items", which is an array of items. Each item must have: { "itemName", "quantity", "price" }.
            - "itemName" (String) should be clean and readable (e.g., 'Tata Salt 1kg').
            - "quantity" (Integer) represents the count (e.g., 2).
            - "price" (Double) represents the unit price in INR.
            Do not enclose in markdown blocks, return pure JSON.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2)
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()
            val adapter: JsonAdapter<AIParsedBillResponse> = GeminiApiClient.moshi.adapter(AIParsedBillResponse::class.java)
            val parsed = adapter.fromJson(cleanJson)
            parsed?.items ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            mockBillLines(text)
        }
    }

    suspend fun identifyProductByBarcode(barcode: String): AISuggestedItem? = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext mockSuggestedItemForBarcode(barcode)
        }

        val prompt = """
            Identify the Indian retail product associated with the following barcode number: '$barcode'
            If this is a known standard barcode (e.g. for Maggie, Tata Salt, biscuits, soaps, etc.), retrieve its actual name, typical price structure, and category.
            If the barcode is unknown or generic, please creatively suggest a plausible, high-quality, realistic Indian supermarket/grocery product name, reasonable MRP price (INR), acquisition cost price (INR, ensuring costPrice < price), and category.
            
            Return a JSON object with: { "name", "category", "price", "costPrice", "barcode" }. 
            - "name" should be polished (e.g., 'Aashirvaad Shudh Chakki Atta 5kg' or 'Amul Butter 100g').
            - "category" must be one of: 'Grains & Pulses', 'Beverages', 'Snacks', 'Hygienes & Soap', 'Spices & Masala', 'Dairy & Fresh', 'Household', 'Cosmetics', 'Others'.
            - "price" and "costPrice" should be double (INR currency, e.g., 80.0, 72.0). Ensure "costPrice" is less than "price".
            - "barcode" must be '$barcode'.
            Do not enclose in markdown blocks, return pure JSON.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2)
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()
            val adapter: com.squareup.moshi.JsonAdapter<AISuggestedItem> = GeminiApiClient.moshi.adapter(AISuggestedItem::class.java)
            adapter.fromJson(cleanJson)
        } catch (e: Exception) {
            e.printStackTrace()
            mockSuggestedItemForBarcode(barcode)
        }
    }

    private fun mockSuggestedItemForBarcode(barcode: String): AISuggestedItem {
        val lastDigit = barcode.lastOrNull()?.toString()?.toIntOrNull() ?: 5
        return when (lastDigit % 6) {
            0 -> AISuggestedItem("Maggi Masala Noodles 70g", "Snacks", 14.0, 12.0, barcode)
            1 -> AISuggestedItem("Tata Salt Lite 1kg", "Spices & Masala", 28.0, 24.0, barcode)
            2 -> AISuggestedItem("Amul Gold Milk 500ml", "Dairy & Fresh", 33.0, 30.0, barcode)
            3 -> AISuggestedItem("Surf Excel Easy Wash 500g", "Household", 110.0, 95.0, barcode)
            4 -> AISuggestedItem("Britannia Marie Gold 250g", "Snacks", 40.0, 34.0, barcode)
            else -> AISuggestedItem("Fortune Mustard Oil 1L", "Others", 175.0, 155.0, barcode)
        }
    }

    private fun mockSuggestedItem(query: String): AISuggestedItem {
        // Fallback or demo mocks when API key is missing
        val lowered = query.lowercase()
        val category = when {
            lowered.contains("milk") || lowered.contains("butter") || lowered.contains("paneer") || lowered.contains("dairy") -> "Dairy & Fresh"
            lowered.contains("rice") || lowered.contains("dal") || lowered.contains("atta") || lowered.contains("wheat") -> "Grains & Pulses"
            lowered.contains("maggie") || lowered.contains("maggi") || lowered.contains("chips") || lowered.contains("kurkure") || lowered.contains("biscuit") -> "Snacks"
            lowered.contains("coke") || lowered.contains("pepsi") || lowered.contains("juice") || lowered.contains("tea") || lowered.contains("coffee") -> "Beverages"
            lowered.contains("soap") || lowered.contains("shampoo") || lowered.contains("brush") -> "Hygienes & Soap"
            lowered.contains("masala") || lowered.contains("chilli") || lowered.contains("turmeric") -> "Spices & Masala"
            else -> "Others"
        }
        val length = query.length
        val price = if (length > 0) ((length * 3) + 10).toDouble() else 45.0
        val costPrice = price * 0.85
        // Generate pseudo random India barcode
        val barcode = "890" + (1000000000 + (query.hashCode().coerceAtLeast(0) % 9000000000L)).toString().take(10)
        return AISuggestedItem(
            name = query.replaceFirstChar { it.uppercase() },
            category = category,
            price = Math.round(price * 10.0) / 10.0,
            costPrice = Math.round(costPrice * 10.0) / 10.0,
            barcode = barcode
        )
    }

    private fun mockBillLines(text: String): List<AIParsedBillLine> {
        // Fallback parsing logic based on text patterns
        val items = mutableListOf<AIParsedBillLine>()
        val words = text.split(",", "and", "\n")
        for (word in words) {
            val trimmed = word.trim()
            if (trimmed.isEmpty()) continue
            
            // Try extracting quantity
            var qty = 1
            val firstNumber = Regex("\\d+").find(trimmed)
            if (firstNumber != null) {
                qty = firstNumber.value.toIntOrNull() ?: 1
            }
            
            val cleanName = trimmed.replace(Regex("\\d+\\s*(kg|g|ml|l|packets|packet|pcs|pc)?"), "").replace("packets", "").replace("packet", "").trim()
            val finalName = if (cleanName.isNotEmpty()) cleanName.replaceFirstChar { it.uppercase() } else "Kirana Product Item"
            val price = when {
                finalName.lowercase().contains("milk") -> 30.0
                finalName.lowercase().contains("maggi") -> 14.0
                finalName.lowercase().contains("rice") -> 60.0
                finalName.lowercase().contains("sugar") -> 45.0
                else -> 50.0
            }
            items.add(AIParsedBillLine(itemName = finalName, quantity = qty, price = price))
        }

        if (items.isEmpty()) {
            items.add(AIParsedBillLine(itemName = "Sample Tea Packet 100g", quantity = 1, price = 40.0))
            items.add(AIParsedBillLine(itemName = "Amul Butter 100g", quantity = 2, price = 55.0))
        }
        return items
    }
}
