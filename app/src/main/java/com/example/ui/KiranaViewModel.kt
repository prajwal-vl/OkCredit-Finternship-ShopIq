package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AIParsedBillLine
import com.example.data.db.*
import com.example.data.repository.KiranaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class AwaitingStoreSetup(val mobile: String) : AuthState
    data class Authenticated(val storeName: String, val ownerMobile: String) : AuthState
}

enum class Screen {
    DASHBOARD,
    NEW_BILL,
    INVENTORY,
    BILL_HISTORY,
    ANALYTICS,
    UDHAR_BOOK
}

data class CartItem(
    val id: Int = 0, // matches ItemEntity id if linked
    val name: String,
    val barcode: String?,
    val unitPrice: Double,
    val quantity: Int,
    val total: Double = unitPrice * quantity
)

class KiranaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = KiranaRepository(application)

    // --- Auth & Profile State ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isVerifyingClerk = MutableStateFlow(false)
    val isVerifyingClerk: StateFlow<Boolean> = _isVerifyingClerk.asStateFlow()

    private val _clerkFeedback = MutableStateFlow<String?>(null)
    val clerkFeedback: StateFlow<String?> = _clerkFeedback.asStateFlow()

    // --- Navigation State ---
    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- Search & Inventory State ---
    private val _inventorySearchQuery = MutableStateFlow("")
    val inventorySearchQuery: StateFlow<String> = _inventorySearchQuery.asStateFlow()

    fun setInventorySearchQuery(query: String) {
        _inventorySearchQuery.value = query
    }

    val inventoryItems: StateFlow<List<ItemEntity>> = _inventorySearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allItems
            } else {
                repository.searchItems(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Billing State ---
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _paymentMode = MutableStateFlow("Cash") // Cash, UPI, Card
    val paymentMode: StateFlow<String> = _paymentMode.asStateFlow()

    private val _customerMobile = MutableStateFlow("")
    val customerMobile: StateFlow<String> = _customerMobile.asStateFlow()

    private val _billSuccessId = MutableStateFlow<Long?>(null)
    val billSuccessId: StateFlow<Long?> = _billSuccessId.asStateFlow()

    // --- Scanning / Add Temp Item ---
    private val _scannedBarcode = MutableStateFlow("")
    val scannedBarcode: StateFlow<String> = _scannedBarcode.asStateFlow()

    private val _scanFeedback = MutableStateFlow<String?>(null)
    val scanFeedback: StateFlow<String?> = _scanFeedback.asStateFlow()

    // --- Free text parser state ---
    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    // --- Database Bills ---
    val allBills: StateFlow<List<BillEntity>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Customer Ledger / Udhar Book ---
    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _customerLogs = MutableStateFlow<List<CustomerLogEntity>>(emptyList())
    val customerLogs: StateFlow<List<CustomerLogEntity>> = _customerLogs.asStateFlow()

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
        if (customer != null) {
            fetchCustomerLogs(customer.id)
        } else {
            _customerLogs.value = emptyList()
        }
    }

    fun fetchCustomerLogs(customerId: Int) {
        viewModelScope.launch {
            _customerLogs.value = repository.getLogsForCustomer(customerId)
        }
    }

    fun addCustomer(name: String, mobile: String) {
        viewModelScope.launch {
            repository.saveCustomer(CustomerEntity(name = name, mobile = mobile))
        }
    }

    fun addCustomerTransaction(customerId: Int, type: String, amount: Double, note: String, isSetBalance: Boolean = false) {
        viewModelScope.launch {
            val customer = repository.getCustomerById(customerId)
            if (customer != null) {
                val newBalance = if (isSetBalance) {
                    amount
                } else if (type == "Credit" || type == "Repayment") {
                    customer.outstandingBalance - amount
                } else {
                    customer.outstandingBalance + amount
                }
                
                val logAmount = if (isSetBalance) kotlin.math.abs(newBalance - customer.outstandingBalance) else amount
                val logType = if (isSetBalance) "Adjustment" else type
                
                repository.updateCustomer(customer.copy(outstandingBalance = newBalance))
                repository.saveCustomerLog(CustomerLogEntity(
                    customerId = customerId,
                    type = logType,
                    amount = logAmount,
                    note = note.takeIf { it.isNotEmpty() } ?: "$type recorded"
                ))
                // Refresh list & current logs
                _customerLogs.value = repository.getLogsForCustomer(customerId)
                _selectedCustomer.value = repository.getCustomerById(customerId)
            }
        }
    }

    fun recordRepayment(customerId: Int, amount: Double, note: String) {
        viewModelScope.launch {
            val customer = repository.getCustomerById(customerId)
            if (customer != null) {
                val newBalance = customer.outstandingBalance - amount
                repository.updateCustomer(customer.copy(outstandingBalance = newBalance))
                repository.saveCustomerLog(CustomerLogEntity(
                    customerId = customerId,
                    type = "Repayment",
                    amount = amount,
                    note = note.takeIf { it.isNotEmpty() } ?: "Payment received"
                ))
                // Refresh list & current logs
                _customerLogs.value = repository.getLogsForCustomer(customerId)
                _selectedCustomer.value = repository.getCustomerById(customerId)
            }
        }
    }

    // --- Statistics ---
    val todayRevenue: StateFlow<Double> = allBills.map { bills ->
        val todayStart = getStartOfDay().timeInMillis
        bills.filter { it.timestamp >= todayStart }.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthTotal: StateFlow<Double> = allBills.map { bills ->
        val monthStart = getStartOfMonth().timeInMillis
        bills.filter { it.timestamp >= monthStart }.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val billsCreatedCount: StateFlow<Int> = allBills.map { bills ->
        val todayStart = getStartOfDay().timeInMillis
        bills.filter { it.timestamp >= todayStart }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        checkStoreSetup()
    }

    private fun checkStoreSetup() {
        viewModelScope.launch {
            val store = repository.getStoreProfile()
            if (store != null && store.setupCompleted) {
                _authState.value = AuthState.Authenticated(store.storeName, store.ownerMobile)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    // --- Authentication Actions ---
    fun loginWithOtp(mobile: String, otp: String): Boolean {
        // Simple mock OTP validation: Allow any 6 digit otp (e.g., "123456" or any input for robust testing)
        if (mobile.length == 10) {
            viewModelScope.launch {
                val existingProfile = repository.getStoreProfile()
                if (existingProfile != null) {
                    _authState.value = AuthState.Authenticated(existingProfile.storeName, existingProfile.ownerMobile)
                } else {
                    _authState.value = AuthState.AwaitingStoreSetup(mobile)
                }
            }
            return true
        }
        return false
    }

    fun loginWithClerk(identifier: String, onResult: (Boolean, String) -> Unit) {
        if (identifier.isBlank()) {
            onResult(false, "Please enter an email or phone identifier.")
            return
        }
        _isVerifyingClerk.value = true
        _clerkFeedback.value = "Connecting to Clerk auth node..."
        viewModelScope.launch {
            try {
                _clerkFeedback.value = "Retrieving app user database..."
                val authHeader = com.example.data.api.ClerkApiClient.getAuthHeader()
                val users = com.example.data.api.ClerkApiClient.service.listUsers(authHeader)
                
                // Let's search if any user matches the identifier (email or phone)
                val cleanId = identifier.trim().lowercase()
                val matchedUser = users.find { user ->
                    val hasEmail = user.email_addresses?.any { it.email_address.lowercase() == cleanId } == true
                    val hasPhone = user.phone_numbers?.any { it.phone_number.lowercase() == cleanId || it.phone_number.replace("+", "").lowercase() == cleanId } == true
                    hasEmail || hasPhone
                }

                if (matchedUser != null) {
                    val displayName = listOfNotNull(matchedUser.first_name, matchedUser.last_name)
                        .joinToString(" ")
                        .ifBlank { "Clerk User ${matchedUser.id.takeLast(6)}" }

                    // Sync locally with local room DB
                    val profile = StoreEntity(
                        storeName = displayName,
                        ownerMobile = identifier,
                        setupCompleted = true
                    )
                    repository.saveStoreProfile(profile)

                    _authState.value = AuthState.Authenticated(displayName, identifier)
                    _clerkFeedback.value = "Welcome back, $displayName!"
                    onResult(true, "Authentication successful via Clerk!")
                } else {
                    _clerkFeedback.value = "No existing account found with '$identifier'. Connect as a new user below."
                    onResult(false, "Identifier not registered on this Clerk application.")
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Authentication failed."
                _clerkFeedback.value = "Clerk Gateway Connection Error: $errorMsg"
                onResult(false, "Clerk Connection issue: $errorMsg")
            } finally {
                _isVerifyingClerk.value = false
            }
        }
    }

    fun signUpWithClerk(firstName: String, lastName: String, email: String, phone: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank() && phone.isBlank()) {
            onResult(false, "At least an email or phone number is required.")
            return
        }
        _isVerifyingClerk.value = true
        _clerkFeedback.value = "Registering user in Clerk database..."
        viewModelScope.launch {
            try {
                val authHeader = com.example.data.api.ClerkApiClient.getAuthHeader()
                val req = com.example.data.api.ClerkCreateUserRequest(
                    first_name = firstName.takeIf { it.isNotBlank() },
                    last_name = lastName.takeIf { it.isNotBlank() },
                    email_address = if (email.isNotBlank()) listOf(email) else null,
                    phone_number = if (phone.isNotBlank()) listOf(phone) else null,
                    skip_password_requirement = true
                )
                val createdUser = com.example.data.api.ClerkApiClient.service.createUser(authHeader, req)
                val displayName = listOfNotNull(createdUser.first_name, createdUser.last_name)
                    .joinToString(" ")
                    .ifBlank { "Clerk User ${createdUser.id.takeLast(6)}" }

                val identifier = email.takeIf { it.isNotBlank() } ?: phone
                val profile = StoreEntity(
                    storeName = displayName,
                    ownerMobile = identifier,
                    setupCompleted = true
                )
                repository.saveStoreProfile(profile)

                _authState.value = AuthState.Authenticated(displayName, identifier)
                _clerkFeedback.value = "Successfully registered! Welcome, $displayName"
                onResult(true, "Verification complete! Account saved securely.")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Registration failed."
                _clerkFeedback.value = "Registration error: $errorMsg"
                onResult(false, "Clerk Registration issue: $errorMsg")
            } finally {
                _isVerifyingClerk.value = false
            }
        }
    }

    fun setupStore(storeName: String, ownerMobile: String) {
        viewModelScope.launch {
            val store = StoreEntity(storeName = storeName, ownerMobile = ownerMobile)
            repository.saveStoreProfile(store)
            _authState.value = AuthState.Authenticated(storeName, ownerMobile)
            _currentScreen.value = Screen.DASHBOARD
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearProfile()
            _authState.value = AuthState.Unauthenticated
            _currentScreen.value = Screen.DASHBOARD
        }
    }

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.NEW_BILL) {
            clearBillingState()
        }
    }

    // --- Cart Actions ---
    fun addItemToCart(item: ItemEntity, qty: Int = 1) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.name == item.name }
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(
                quantity = existing.quantity + qty,
                total = existing.unitPrice * (existing.quantity + qty)
            )
        } else {
            current.add(CartItem(
                id = item.id,
                name = item.name,
                barcode = item.barcode,
                unitPrice = item.price,
                quantity = qty
            ))
        }
        _cartItems.value = current
    }

    fun updateCartQty(cartItem: CartItem, newQty: Int) {
        if (newQty <= 0) {
            removeFromCart(cartItem)
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.name == cartItem.name }
        if (index != -1) {
            current[index] = cartItem.copy(
                quantity = newQty,
                total = cartItem.unitPrice * newQty
            )
            _cartItems.value = current
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        _cartItems.value = _cartItems.value.filterNot { it.name == cartItem.name }
    }

    fun clearBillingState() {
        _cartItems.value = emptyList()
        _customerMobile.value = ""
        _paymentMode.value = "Cash"
        _billSuccessId.value = null
        _scannedBarcode.value = ""
        _scanFeedback.value = null
    }

    fun setPaymentMode(mode: String) {
        _paymentMode.value = mode
    }

    fun setCustomerMobile(mobile: String) {
        _customerMobile.value = mobile
    }

    fun setScannedBarcode(barcode: String) {
        _scannedBarcode.value = barcode
    }

    fun handleBarcodeScanSubmit() {
        val barcode = _scannedBarcode.value.trim()
        if (barcode.isEmpty()) return

        viewModelScope.launch {
            val item = repository.getItemByBarcode(barcode)
            if (item != null) {
                addItemToCart(item, 1)
                _scanFeedback.value = "Added 1x ${item.name} to Cart!"
                _scannedBarcode.value = ""
            } else {
                _isAILoading.value = true
                _scanFeedback.value = "Not found locally. Querying ShopIq AI to identify barcode $barcode..."
                try {
                    val aiSuggested = repository.identifyProductByBarcode(barcode)
                    if (aiSuggested != null) {
                        val newEntity = ItemEntity(
                            name = aiSuggested.name,
                            barcode = barcode,
                            price = aiSuggested.price,
                            costPrice = aiSuggested.costPrice,
                            stock = 50,
                            category = aiSuggested.category
                        )
                        val insertId = repository.saveItem(newEntity)
                        val savedEntity = newEntity.copy(id = insertId.toInt())
                        addItemToCart(savedEntity, 1)
                        _scanFeedback.value = "ShopIq AI Scan successfully identified ${aiSuggested.name}! Registered & added to cart."
                        _scannedBarcode.value = ""
                    } else {
                        _scanFeedback.value = "AI did not return details. Enter details below to Quick-Add."
                    }
                } catch (e: Exception) {
                    _scanFeedback.value = "AI lookup failed. Enter details below to Quick-Add."
                } finally {
                    _isAILoading.value = false
                }
            }
        }
    }

    fun dismissScanFeedback() {
        _scanFeedback.value = null
    }

    suspend fun getItemByBarcode(barcode: String): ItemEntity? {
        return repository.getItemByBarcode(barcode)
    }

    suspend fun identifyProductByBarcode(barcode: String): com.example.data.api.AISuggestedItem? {
        return repository.identifyProductByBarcode(barcode)
    }

    suspend fun saveItem(item: ItemEntity): Long {
        return repository.saveItem(item)
    }

    // --- Quick Add from Scanner screen ---
    fun quickAddInventoryAndCart(name: String, price: Double, costPrice: Double, barcode: String, category: String) {
        viewModelScope.launch {
            val newEntity = ItemEntity(
                name = name,
                barcode = if (barcode.isNotEmpty()) barcode else null,
                price = price,
                costPrice = costPrice,
                stock = 50, // default opening stock
                category = category
            )
            val insertId = repository.saveItem(newEntity)
            val savedEntity = newEntity.copy(id = insertId.toInt())
            addItemToCart(savedEntity, 1)
            _scanFeedback.value = "Saved & added to cart!"
            _scannedBarcode.value = ""
        }
    }

    // --- Save Bill ---
    fun checkoutBill() {
        val cart = _cartItems.value
        if (cart.isEmpty()) return

        viewModelScope.launch {
            val total = cart.sumOf { it.total }
            val billNumber = "KB-" + (System.currentTimeMillis() % 100000).toString().padStart(5, '0')
            val bill = BillEntity(
                billNumber = billNumber,
                totalAmount = total,
                paymentMode = _paymentMode.value,
                customerMobile = _customerMobile.value.takeIf { it.isNotEmpty() }
            )

            val billItems = cart.map {
                BillItemEntity(
                    billId = 0, // Auto-assigned by repo
                    itemName = it.name,
                    price = it.unitPrice,
                    quantity = it.quantity,
                    total = it.total
                )
            }

            val savedId = repository.saveBill(bill, billItems)
            _billSuccessId.value = savedId
            _cartItems.value = emptyList() // Clear cart
        }
    }

    // --- Inventory Management ---
    fun addInventoryItem(name: String, price: Double, costPrice: Double, barcode: String?, stock: Int, category: String) {
        viewModelScope.launch {
            val item = ItemEntity(
                name = name,
                price = price,
                costPrice = costPrice,
                barcode = barcode?.takeIf { it.isNotEmpty() },
                stock = stock,
                category = category
            )
            repository.saveItem(item)
        }
    }

    fun updateInventoryItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteInventoryItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun seedSampleInventory() {
        viewModelScope.launch {
            val sampleItems = listOf(
                ItemEntity(name = "Tata Salt 1kg", barcode = "8901058002315", price = 28.0, costPrice = 24.0, stock = 100, category = "Spices & Masala"),
                ItemEntity(name = "Amul Butter 100g", barcode = "8901262010014", price = 56.0, costPrice = 48.0, stock = 40, category = "Dairy & Fresh"),
                ItemEntity(name = "Maggi Noodles 12-Pack", barcode = "8901058862414", price = 168.0, costPrice = 145.0, stock = 30, category = "Snacks"),
                ItemEntity(name = "Aashirvaad Atta 5kg", barcode = "8901725181223", price = 270.0, costPrice = 240.0, stock = 25, category = "Grains & Pulses"),
                ItemEntity(name = "Coca-Cola 500ml", barcode = "8901764012229", price = 40.0, costPrice = 33.0, stock = 60, category = "Beverages"),
                ItemEntity(name = "Dettol Liquid Soap 250ml", barcode = "8901396221157", price = 99.0, costPrice = 85.0, stock = 20, category = "Hygienes & Soap"),
                ItemEntity(name = "Parle-G Gold Biscuit 100g", barcode = "8901725112111", price = 10.0, costPrice = 8.5, stock = 150, category = "Snacks"),
                ItemEntity(name = "Haldiram Bhujia 150g", barcode = "8904063200122", price = 45.0, costPrice = 38.0, stock = 50, category = "Snacks")
            )
            for (item in sampleItems) {
                repository.saveItem(item)
            }
        }
    }

    // --- Smart AI Auto-complete Helper ---
    fun fetchAISuggestedItem(query: String, onComplete: (ItemEntity) -> Unit) {
        viewModelScope.launch {
            _isAILoading.value = true
            val suggestion = repository.suggestItemDetails(query)
            _isAILoading.value = false
            if (suggestion != null) {
                onComplete(
                    ItemEntity(
                        name = suggestion.name,
                        price = suggestion.price,
                        costPrice = suggestion.costPrice,
                        barcode = suggestion.barcode,
                        category = suggestion.category,
                        stock = 50
                    )
                )
            }
        }
    }

    // --- Smart AI Voice/Text Bilify ---
    fun autobilifyFromVoiceText(voiceText: String) {
        if (voiceText.trim().isEmpty()) return
        viewModelScope.launch {
            _isAILoading.value = true
            val parsedLines = repository.parseFreeTextToBill(voiceText)
            _isAILoading.value = false
            
            val current = _cartItems.value.toMutableList()
            for (line in parsedLines) {
                // Check if item name exists in local inventory
                val matchedList = repository.getItemByBarcode(line.itemName) 
                    ?: repository.getItemByBarcode(line.itemName.lowercase())
                
                if (matchedList != null) {
                    current.add(CartItem(
                        id = matchedList.id,
                        name = matchedList.name,
                        barcode = matchedList.barcode,
                        unitPrice = matchedList.price,
                        quantity = line.quantity
                    ))
                } else {
                    // Create an ephemeral cart item directly
                    current.add(CartItem(
                        name = line.itemName,
                        barcode = null,
                        unitPrice = line.price,
                        quantity = line.quantity
                    ))
                }
            }
            _cartItems.value = current
        }
    }

    fun getBillDetails(billId: Int, onComplete: (BillEntity, List<BillItemEntity>) -> Unit) {
        viewModelScope.launch {
            val bill = repository.getBillById(billId)
            if (bill != null) {
                val items = repository.getBillItems(billId)
                onComplete(bill, items)
            }
        }
    }

    // --- Date Helpers ---
    private fun getStartOfDay(): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    private fun getStartOfMonth(): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
}
