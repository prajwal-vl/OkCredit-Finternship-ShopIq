package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import com.example.data.db.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.common.util.concurrent.ListenableFuture
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import android.widget.Toast
import android.os.Vibrator
import android.os.VibrationEffect
import kotlinx.coroutines.launch
import com.example.data.api.AISuggestedItem

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun KiranaAppContent(viewModel: KiranaViewModel) {
    val authState by viewModel.authState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = authState) {
            is AuthState.Unauthenticated -> {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginClick = { mobile, otp ->
                        viewModel.loginWithOtp(mobile, otp)
                    }
                )
            }
            is AuthState.AwaitingStoreSetup -> {
                StoreSetupScreen(
                    mobile = state.mobile,
                    onSetupComplete = { name ->
                        viewModel.setupStore(name, state.mobile)
                    }
                )
            }
            is AuthState.Authenticated -> {
                MainLayout(
                    storeName = state.storeName,
                    currentScreen = currentScreen,
                    onScreenChange = { viewModel.setScreen(it) },
                    onLogout = { viewModel.logout() }
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() with fadeOut()
                        }
                    ) { target ->
                        when (target) {
                            Screen.DASHBOARD -> DashboardScreen(
                                storeName = state.storeName,
                                viewModel = viewModel
                            )
                            Screen.NEW_BILL -> NewBillScreen(
                                viewModel = viewModel
                            )
                            Screen.INVENTORY -> InventoryScreen(
                                viewModel = viewModel
                            )
                            Screen.BILL_HISTORY -> BillHistoryScreen(
                                viewModel = viewModel
                            )
                            Screen.ANALYTICS -> AnalyticsScreen(
                                viewModel = viewModel
                            )
                            Screen.UDHAR_BOOK -> UdharBookScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================== AUTH & LOGIN ========================

@Composable
fun LoginScreen(
    viewModel: KiranaViewModel,
    onLoginClick: (String, String) -> Boolean
) {
    var loginModeByClerk by remember { mutableStateOf(true) } // default to Clerk Cloud Authentication
    var showSignUpMode by remember { mutableStateOf(false) }

    // Clerk Login form state
    var clerkIdentifier by remember { mutableStateOf("") }

    // Clerk Signup form state
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPhone by remember { mutableStateOf("") }
    var signUpFirstName by remember { mutableStateOf("") }
    var signUpLastName by remember { mutableStateOf("") }

    // Local OTP screen states
    var mobile by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val isVerifyingClerk by viewModel.isVerifyingClerk.collectAsState()
    val clerkFeedback by viewModel.clerkFeedback.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.5.dp, SaffronLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SaffronLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = "Store icon",
                        tint = Saffron,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ShopIq",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Navy
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Pro",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Saffron
                    )
                }

                Text(
                    text = "Enterprise Ledger & Local POS System",
                    fontSize = 12.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Network Ready Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(SaffronLight.copy(alpha = 0.6f), shape = RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Green, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CLERK AUTH CONNECTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Saffron
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tabs for Clerk vs Local OTP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Cream, shape = RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = {
                            loginModeByClerk = true
                            errorMessage = null
                            successMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (loginModeByClerk) White else Color.Transparent,
                            contentColor = if (loginModeByClerk) Saffron else Muted
                        ),
                        elevation = if (loginModeByClerk) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clerk Login", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            loginModeByClerk = false
                            errorMessage = null
                            successMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!loginModeByClerk) White else Color.Transparent,
                            contentColor = if (!loginModeByClerk) Saffron else Muted
                        ),
                        elevation = if (!loginModeByClerk) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phone OTP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Feedback and Error Panels
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SaffronLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Saffron, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = errorMessage ?: "", color = Navy, fontSize = 12.sp)
                        }
                    }
                }

                if (successMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GreenLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = successMessage ?: "", color = Navy, fontSize = 12.sp)
                        }
                    }
                }

                if (clerkFeedback != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (isVerifyingClerk) {
                                CircularProgressIndicator(color = Saffron, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = clerkFeedback ?: "", color = Navy, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // MODE A: Clerk Authentication Mode
                if (loginModeByClerk) {
                    if (!showSignUpMode) {
                        // Clerk SIGN IN
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Sign in using your Clerk User credentials. Enter your registered primary Email or Mobile Number.",
                                fontSize = 12.sp,
                                color = Muted,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = clerkIdentifier,
                                onValueChange = { clerkIdentifier = it },
                                label = { Text("Clerk Email or Phone") },
                                leadingIcon = { Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = null) },
                                placeholder = { Text("e.g. user@clerk.accounts.dev") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Saffron,
                                    focusedLabelColor = Saffron
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("clerk_identifier_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    errorMessage = null
                                    successMessage = null
                                    viewModel.loginWithClerk(clerkIdentifier) { ok, msg ->
                                        if (ok) {
                                            successMessage = msg
                                        } else {
                                            errorMessage = msg
                                        }
                                    }
                                },
                                enabled = !isVerifyingClerk && clerkIdentifier.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("clerk_login_button")
                            ) {
                                if (isVerifyingClerk) {
                                    CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Sign In Securely via Clerk", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
                                }
                            }

                            TextButton(
                                onClick = {
                                    showSignUpMode = true
                                    errorMessage = null
                                    successMessage = null
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("New to Clerk? Sign up as a new user", color = Saffron, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        // Clerk SIGN UP
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Create a secure account on Clerk.",
                                fontSize = 12.sp,
                                color = Muted,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = signUpFirstName,
                                onValueChange = { signUpFirstName = it },
                                label = { Text("First Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Saffron),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signUpLastName,
                                onValueChange = { signUpLastName = it },
                                label = { Text("Last Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Saffron),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signUpEmail,
                                onValueChange = { signUpEmail = it },
                                label = { Text("Email Address") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Saffron),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signUpPhone,
                                onValueChange = { signUpPhone = it },
                                label = { Text("Phone Number") },
                                placeholder = { Text("+91XXXXXXXXXX") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Saffron),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    errorMessage = null
                                    successMessage = null
                                    viewModel.signUpWithClerk(
                                        firstName = signUpFirstName,
                                        lastName = signUpLastName,
                                        email = signUpEmail,
                                        phone = signUpPhone
                                    ) { ok, msg ->
                                        if (ok) {
                                            successMessage = msg
                                        } else {
                                            errorMessage = msg
                                        }
                                    }
                                },
                                enabled = !isVerifyingClerk && (signUpEmail.isNotBlank() || signUpPhone.isNotBlank()),
                                colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isVerifyingClerk) {
                                    CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Register Clerk Account", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
                                }
                            }

                            TextButton(
                                onClick = {
                                    showSignUpMode = false
                                    errorMessage = null
                                    successMessage = null
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Back to Sign In", color = Saffron, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // MODE B: Traditional Phone Mobile OTP Mode
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Log in using a mock 10-digit phone number and any 6-digit OTP for secure offline prototype testing.",
                            fontSize = 12.sp,
                            color = Muted,
                            lineHeight = 16.sp
                        )

                        // Mobile field
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { if (it.length <= 10) mobile = it },
                            label = { Text("Mobile Number") },
                            prefix = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Saffron,
                                focusedLabelColor = Saffron,
                                focusedTextColor = Navy,
                                unfocusedTextColor = Navy
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mobile_input")
                        )

                        if (showOtpField) {
                            OutlinedTextField(
                                value = otp,
                                onValueChange = { if (it.length <= 6) otp = it },
                                label = { Text("6-Digit OTP") },
                                placeholder = { Text("Enter 123456 as default OTP") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Saffron,
                                    focusedLabelColor = Saffron,
                                    focusedTextColor = Navy,
                                    unfocusedTextColor = Navy
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_input")
                            )

                            Text(
                                text = "Tip: You can use any 6-digit OTP for testing this prototype",
                                fontSize = 11.sp,
                                color = Muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                if (mobile.length != 10) {
                                    errorMessage = "Please enter a valid 10-digit mobile number."
                                } else if (!showOtpField) {
                                    showOtpField = true
                                    errorMessage = null
                                } else {
                                    if (otp.length != 6) {
                                        errorMessage = "OTP must be a 6-digit number."
                                    } else {
                                        val success = onLoginClick(mobile, otp)
                                        if (!success) {
                                            errorMessage = "Invalid login sequence."
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_button")
                        ) {
                            Text(
                                text = if (!showOtpField) "Request OTP" else "Verify & Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreSetupScreen(mobile: String, onSetupComplete: (String) -> Unit) {
    var storeName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SaffronLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddBusiness,
                        contentDescription = "Setup Icon",
                        tint = Saffron,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Setup Your ShopIq Store",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )

                Text(
                    text = "Welcome! Register active mobile: +91 $mobile",
                    fontSize = 13.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = Saffron,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name") },
                    placeholder = { Text("e.g., Krishna ShopIq Store") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Saffron,
                        focusedLabelColor = Saffron,
                        focusedTextColor = Navy,
                        unfocusedTextColor = Navy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_name_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (storeName.trim().isEmpty()) {
                            errorText = "Please enter a valid name for your store."
                        } else {
                            onSetupComplete(storeName.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("setup_store_button")
                ) {
                    Text(
                        text = "Get Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = White
                    )
                }
            }
        }
    }
}

// ======================== APP LAYOUT ========================

@Composable
fun MainLayout(
    storeName: String,
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = Cream,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Green)
                            )
                            Text(
                                text = "Live • Store ID: " + (storeName.hashCode().let { Math.abs(it) % 9000 + 1000 }),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Muted,
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = storeName,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Navy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 240.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Pro",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Saffron
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Initials Avatar clickable to log out
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Navy)
                                .clickable { onLogout() },
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = remember(storeName) {
                                val words = storeName.split(" ").filter { it.isNotEmpty() }
                                if (words.size >= 2) {
                                    (words[0].take(1) + words[1].take(1)).uppercase()
                                } else if (storeName.isNotEmpty()) {
                                    storeName.take(2).uppercase()
                                } else {
                                    "KP"
                                }
                            }
                            Text(
                                text = initials,
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = White,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { onScreenChange(Screen.DASHBOARD) },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Saffron,
                        selectedTextColor = Saffron,
                        indicatorColor = SaffronLight,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.NEW_BILL,
                    onClick = { onScreenChange(Screen.NEW_BILL) },
                    icon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Billing") },
                    label = { Text("New Bill", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Saffron,
                        selectedTextColor = Saffron,
                        indicatorColor = SaffronLight,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.UDHAR_BOOK,
                    onClick = { onScreenChange(Screen.UDHAR_BOOK) },
                    icon = { Icon(imageVector = Icons.Default.Book, contentDescription = "Udhar Book") },
                    label = { Text("Udhar Book", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Saffron,
                        selectedTextColor = Saffron,
                        indicatorColor = SaffronLight,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.INVENTORY,
                    onClick = { onScreenChange(Screen.INVENTORY) },
                    icon = { Icon(imageVector = Icons.Default.Inventory, contentDescription = "Inventory") },
                    label = { Text("Stock Items", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Saffron,
                        selectedTextColor = Saffron,
                        indicatorColor = SaffronLight,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.BILL_HISTORY,
                    onClick = { onScreenChange(Screen.BILL_HISTORY) },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = "History") },
                    label = { Text("Bills", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Saffron,
                        selectedTextColor = Saffron,
                        indicatorColor = SaffronLight,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.ANALYTICS,
                    onClick = { onScreenChange(Screen.ANALYTICS) },
                    icon = { Icon(imageVector = Icons.Default.PieChart, contentDescription = "Analytics") },
                    label = { Text("Stats", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Saffron,
                        selectedTextColor = Saffron,
                        indicatorColor = SaffronLight,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(Cream)
        ) {
            content()
        }
    }
}

// ======================== DASHBOARD SCREEN ========================

@Composable
fun DashboardScreen(storeName: String, viewModel: KiranaViewModel) {
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val monthTotal by viewModel.currentMonthTotal.collectAsState()
    val billsToday by viewModel.billsCreatedCount.collectAsState()
    val inventoryCount by viewModel.inventoryItems.collectAsState()

    // Dynamic greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "$greeting, shopkeeper! 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Text(
                    text = "Here's what is happening at $storeName today.",
                    fontSize = 13.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // AI Powered Search (Gemini)
        item {
            Card(
                onClick = { viewModel.setScreen(Screen.NEW_BILL) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pulse circle on the left
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Saffron)
                        )

                        Text(
                            text = "Ask Gemini: 'How much sugar left?'",
                            fontSize = 13.sp,
                            color = Muted,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SaffronLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✦",
                            color = Saffron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Quick Action Grid Section (Aspect 16/10)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // New Automated Billing (Saffron)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setScreen(Screen.NEW_BILL) },
                    colors = CardDefaults.cardColors(containerColor = Saffron),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "New Bill",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Inventory Tracking (Green)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setScreen(Screen.INVENTORY) },
                    colors = CardDefaults.cardColors(containerColor = Green),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Stock Items",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Ledger Ledger / Udhar (Navy)
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .clickable { viewModel.setScreen(Screen.UDHAR_BOOK) },
                    colors = CardDefaults.cardColors(containerColor = Navy),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Ledger Udhar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Stats Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today Revenue
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SaffronLight),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Saffron,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Today's Sale", fontSize = 11.sp, color = Muted)
                        Text(
                            text = "₹${"%.1f".format(todayRevenue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Saffron
                        )
                    }
                }

                // Month Revenue
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = GreenLight),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val monthName = remember { SimpleDateFormat("MMMM", Locale.getDefault()).format(Date()) }
                        Text(text = "$monthName Sale", fontSize = 11.sp, color = Muted)
                        Text(
                            text = "₹${"%.1f".format(monthTotal)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Green
                        )
                    }
                }
            }
        }

        // Stock Status Section (Custom card matching the exact Design HTML mock)
        item {
            val lowStockItems = remember(inventoryCount) {
                inventoryCount.filter { it.stock < 10 }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stock Status",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                        
                        if (lowStockItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SaffronLight)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${lowStockItems.size} LOW STOCK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Saffron
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GreenLight)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "STOCK HEALTHY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Green
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    if (inventoryCount.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No inventory items found. Register items in Stock Items first.", fontSize = 11.sp, color = Muted)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Show top 3 low stock or regular items
                            val itemsToShow = remember(inventoryCount) {
                                val low = inventoryCount.filter { it.stock < 10 }
                                val healthy = inventoryCount.filter { it.stock >= 10 }
                                (low + healthy).take(3)
                            }
                            
                            itemsToShow.forEach { item ->
                                val emoji = remember(item.name) {
                                    val nameLower = item.name.lowercase()
                                    when {
                                        nameLower.contains("rice") || nameLower.contains("chawal") -> "🍚"
                                        nameLower.contains("oil") || nameLower.contains("tel") -> "🧴"
                                        nameLower.contains("milk") || nameLower.contains("doodh") -> "🥛"
                                        nameLower.contains("salt") || nameLower.contains("namak") -> "🧂"
                                        nameLower.contains("sugar") || nameLower.contains("chini") -> "🍬"
                                        nameLower.contains("soap") || nameLower.contains("sabun") || nameLower.contains("deterg") -> "🧼"
                                        nameLower.contains("chips") || nameLower.contains("biscuit") || nameLower.contains("kurkure") || nameLower.contains("haldi") -> "🍪"
                                        nameLower.contains("tea") || nameLower.contains("chai") -> "☕"
                                        nameLower.contains("spice") || nameLower.contains("masala") -> "🌶️"
                                        else -> "📦"
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Cream, shape = RoundedCornerShape(16.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 20.sp)
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.name, fontWeight = FontWeight.Bold, color = Navy, fontSize = 13.sp)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${item.stock} units left", 
                                                fontSize = 11.sp, 
                                                color = if (item.stock < 10) Saffron else Muted
                                            )
                                            Text("•", fontSize = 11.sp, color = Muted)
                                            Text(text = item.category, fontSize = 11.sp, color = Muted)
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val progress = remember(item.stock) {
                                            (item.stock / 50f).coerceIn(0.1f, 1f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(White)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(progress)
                                                    .height(4.dp)
                                                    .background(if (item.stock < 10) Saffron else Green)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(10.dp))
                                    
                                    Text(
                                        text = "₹${"%.1f".format(item.price)}",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = Navy
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Billing Ledger",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                TextButton(onClick = { viewModel.setScreen(Screen.BILL_HISTORY) }) {
                    Text("View All", fontSize = 12.sp, color = Saffron)
                }
            }
        }

        item {
            val bills by viewModel.allBills.collectAsState()
            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No transactions logged yet", fontSize = 12.sp, color = Muted)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bills.take(3).forEach { bill ->
                        val timeString = remember(bill.timestamp) {
                            SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(bill.timestamp))
                        }
                        var showDetailDialog by remember { mutableStateOf(false) }

                        Card(
                            onClick = { showDetailDialog = true },
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = bill.billNumber, fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp)
                                    Text(text = timeString, fontSize = 11.sp, color = Muted)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (bill.paymentMode == "UPI") GreenLight else SaffronLight)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = bill.paymentMode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (bill.paymentMode == "UPI") Green else Saffron
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "₹${"%.1f".format(bill.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Navy,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        if (showDetailDialog) {
                            ReceiptDetailDialog(billId = bill.id, viewModel = viewModel, onDismiss = { showDetailDialog = false })
                        }
                    }
                }
            }
        }
    }
}

// ======================== NEW BILL SCREEN ========================

@Composable
fun NewBillScreen(viewModel: KiranaViewModel) {
    val cart by viewModel.cartItems.collectAsState()
    val paymentMode by viewModel.paymentMode.collectAsState()
    val customerMobile by viewModel.customerMobile.collectAsState()
    val barcodeInput by viewModel.scannedBarcode.collectAsState()
    val scanFeedback by viewModel.scanFeedback.collectAsState()
    val isAiLoading by viewModel.isAILoading.collectAsState()
    val billSuccessId by viewModel.billSuccessId.collectAsState()
    val itemsAvailable by viewModel.inventoryItems.collectAsState()

    var showVoiceDialog by remember { mutableStateOf(false) }
    var voiceText by remember { mutableStateOf("") }
    var showManualAddCartItem by remember { mutableStateOf(false) }
    var showItemsDropdown by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }

    if (billSuccessId != null) {
        ReceiptDetailDialog(
            billId = billSuccessId!!.toInt(),
            viewModel = viewModel,
            onDismiss = { viewModel.clearBillingState() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scanner simulation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Scan/Enter Barcode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                    
                    Button(
                        onClick = { showCameraScanner = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronLight, contentColor = Saffron),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera Scan", tint = Saffron, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Cam Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { viewModel.setScannedBarcode(it) },
                        placeholder = { Text("Scan / Type Barcode...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("barcode_input"),
                        trailingIcon = {
                            IconButton(onClick = { showItemsDropdown = true }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Choose catalog")
                            }
                        }
                    )

                    Button(
                        onClick = { viewModel.handleBarcodeScanSubmit() },
                        colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Lookup Barcode", tint = White)
                    }
                }

                DropdownMenu(
                    expanded = showItemsDropdown,
                    onDismissRequest = { showItemsDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.8f).background(White)
                ) {
                    if (itemsAvailable.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Catalog empty. Register items in 'Stock Items' first") },
                            onClick = { showItemsDropdown = false }
                        )
                    } else {
                        itemsAvailable.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name} - ₹${item.price} [Stock: ${item.stock}]") },
                                onClick = {
                                    viewModel.addItemToCart(item, 1)
                                    showItemsDropdown = false
                                }
                            )
                        }
                    }
                }

                if (scanFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SaffronLight)
                            .padding(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = scanFeedback ?: "",
                                fontSize = 12.sp,
                                color = Saffron,
                                modifier = Modifier.weight(1f)
                            )
                            if (scanFeedback!!.contains("not found")) {
                                TextButton(onClick = { showManualAddCartItem = true }) {
                                    Text("Quick Add", fontWeight = FontWeight.Bold, color = Saffron, fontSize = 12.sp)
                                }
                            }
                            IconButton(
                                onClick = { viewModel.dismissScanFeedback() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Saffron, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Voice AI Billing Trigger card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GreenLight),
            shape = RoundedCornerShape(16.dp),
            onClick = { showVoiceDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Green),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "AI Smart Order Bilifier", fontWeight = FontWeight.Bold, color = Green, fontSize = 14.sp)
                    Text(text = "Transcribe a customer order list automatically using AI!", color = Muted, fontSize = 11.sp)
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Green)
            }
        }

        // Active Cart List Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Active Invoice Items", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                Spacer(modifier = Modifier.height(12.dp))

                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = Muted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Search product above or use AI Voice billing.", fontSize = 12.sp, color = Muted)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        cart.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Cream, shape = RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(text = item.name, fontWeight = FontWeight.Bold, color = Navy, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "₹${item.unitPrice} each", fontSize = 11.sp, color = Muted)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.updateCartQty(item, item.quantity - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = Saffron, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${item.quantity}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateCartQty(item, item.quantity + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Saffron, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "₹${"%.1f".format(item.total)}",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Navy,
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        Divider(color = Border, modifier = Modifier.padding(vertical = 8.dp))

                        // Total Row
                        val grandTotal = cart.sumOf { it.total }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Grand Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                            Text(
                                text = "₹${"%.1f".format(grandTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 20.sp,
                                color = Saffron
                            )
                        }
                    }
                }
            }
        }

        // Checkout Button
        if (cart.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "Payment details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)

                    OutlinedTextField(
                        value = customerMobile,
                        onValueChange = { viewModel.setCustomerMobile(it) },
                        label = { Text("Customer Phone (Optional)") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Saffron) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Cash", "UPI", "Credit").forEach { mode ->
                            val isSelected = paymentMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Saffron else Cream)
                                    .clickable { viewModel.setPaymentMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) White else Navy,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (paymentMode == "Credit") {
                        val isMobileValid = customerMobile.trim().length >= 10
                        Text(
                            text = if (!isMobileValid) "⚠️ Minimum 10-digit Customer Phone is required for Credit!" else "✅ Balance will be logged under outstanding credit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isMobileValid) Saffron else Green,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val creditError = paymentMode == "Credit" && customerMobile.trim().length < 10
                    val canCheckout = !creditError && cart.isNotEmpty()

                    Button(
                        onClick = { viewModel.checkoutBill() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Saffron,
                            disabledContainerColor = Saffron.copy(alpha = 0.4f)
                        ),
                        enabled = canCheckout,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_button")
                    ) {
                        Text(text = "Save & Print Receipt", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (canCheckout) White else Navy.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // Dynamic quick add dialog when barcode not found
    if (showManualAddCartItem) {
        Dialog(onDismissRequest = { showManualAddCartItem = false }) {
            var tempName by remember { mutableStateOf("") }
            var tempPrice by remember { mutableStateOf("") }
            var tempCost by remember { mutableStateOf("") }
            var tempCategory by remember { mutableStateOf("Snacks") }
            var isDetailsFetching by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "Quick Product Registry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Product Query / Short Name") },
                            placeholder = { Text("e.g., Amul Milk 1L") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Saffron,
                                focusedLabelColor = Saffron,
                                focusedTextColor = Navy,
                                unfocusedTextColor = Navy
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (tempName.isNotEmpty()) {
                                    isDetailsFetching = true
                                    viewModel.fetchAISuggestedItem(tempName) { suggested ->
                                        tempName = suggested.name
                                        tempPrice = suggested.price.toString()
                                        tempCost = suggested.costPrice.toString()
                                        tempCategory = suggested.category
                                        isDetailsFetching = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(GreenLight, shape = RoundedCornerShape(10.dp))
                        ) {
                            if (isDetailsFetching) {
                                CircularProgressIndicator(color = Green, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Query Gemini", tint = Green)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempPrice,
                        onValueChange = { tempPrice = it },
                        label = { Text("Base Price ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    OutlinedTextField(
                        value = tempCost,
                        onValueChange = { tempCost = it },
                        label = { Text("Cost Price ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    Text(text = "Barcode Associated: $barcodeInput", fontSize = 12.sp, color = Muted)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showManualAddCartItem = false }) {
                            Text("Cancel", color = Muted)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val price = tempPrice.toDoubleOrNull() ?: 10.0
                                val cost = tempCost.toDoubleOrNull() ?: (price * 0.8)
                                viewModel.quickAddInventoryAndCart(
                                    name = tempName.takeIf { it.isNotEmpty() } ?: "Custom Item",
                                    price = price,
                                    costPrice = cost,
                                    barcode = barcodeInput,
                                    category = tempCategory
                                )
                                showManualAddCartItem = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Register & Select", color = White)
                        }
                    }
                }
            }
        }
    }

    // AI Voice Free-text Dialog popup
    if (showVoiceDialog) {
        Dialog(onDismissRequest = { showVoiceDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(GreenLight, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Green)
                    }

                    Text(
                        text = "AI Smart Order Bilifier",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )

                    Text(
                        text = "Paste/Type purchase text list (e.g., '2 maggie packet, 5 tata salt, 1 packet milk'). Gemini AI parses matching inventory or creates automated bills instantly!",
                        fontSize = 12.sp,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = voiceText,
                        onValueChange = { voiceText = it },
                        placeholder = { Text("e.g., 2 Pepsi, 5 Parle-G, 1 Milk packet") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy,
                            focusedBorderColor = Green,
                            focusedLabelColor = Green
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TextButton(
                            onClick = {
                                voiceText = "2 Milk, 1 Butter 100g, 3 Pepsi, 5 Parle-G"
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Green, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("💡 Try Demo: '2 Milk, 1 Butter...'", fontSize = 11.sp, color = Green)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showVoiceDialog = false }) {
                            Text("Cancel", color = Muted, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.autobilifyFromVoiceText(voiceText)
                                showVoiceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAiLoading
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(color = White, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bilify with AI")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            viewModel = viewModel,
            onDismiss = { showCameraScanner = false }
        )
    }
}

// ======================== CAMERA BARCODE SCANNER IMPLEMENTATION ========================

@OptIn(ExperimentalPermissionsApi::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraBarcodeScannerDialog(
    viewModel: KiranaViewModel,
    isInventoryMode: Boolean = false,
    onBarcodeScanned: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val coroutineScope = rememberCoroutineScope()

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    var simulatedBarcodeResult by remember { mutableStateOf<String?>(null) }
    var resolvedResultItem by remember { mutableStateOf<ItemEntity?>(null) }
    var isAILookupActive by remember { mutableStateOf(false) }
    var lookupStatusMessage by remember { mutableStateOf("") }
    
    // Cooldown helper for real camera scans
    var lastScannedBarcode by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableStateOf(0L) }

    // Quick add form state if lookup fails and they select manual fallback
    var showManualFormInScanner by remember { mutableStateOf(false) }
    var manualFormName by remember { mutableStateOf("") }
    var manualFormPrice by remember { mutableStateOf("") }
    var manualFormCost by remember { mutableStateOf("") }
    var manualFormCategory by remember { mutableStateOf("Dairy & Fresh") }

    val handleScannedBarcode = { scannedString: String ->
        val trimmed = scannedString.trim()
        if (trimmed.isNotEmpty()) {
            simulatedBarcodeResult = trimmed
            
            // Sound beep or vibrate
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
            } catch (e: Exception) {}

            if (onBarcodeScanned != null) {
                onBarcodeScanned(trimmed)
                onDismiss()
            } else {
                resolvedResultItem = null
                lookupStatusMessage = "Looking up local stock for barcode $trimmed..."
                isAILookupActive = true
    
                coroutineScope.launch {
                    val item = viewModel.getItemByBarcode(trimmed)
                    if (item != null) {
                        resolvedResultItem = item
                        if (isInventoryMode) {
                            lookupStatusMessage = "Success! Found '${item.name}' locally."
                        } else {
                            viewModel.addItemToCart(item, 1)
                            lookupStatusMessage = "Success! Found '${item.name}' locally. Added to cart!"
                        }
                    } else {
                        lookupStatusMessage = "Item not found in stock. Please add it from the Inventory screen first."
                        showManualFormInScanner = false
                    }
                    isAILookupActive = false
                }
            }
            Unit
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp),
            border = BorderStroke(2.dp, SaffronLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SaffronLight, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Saffron, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Live Barcode Scanner", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                            Text(text = "Scan barcodes in under a second!", fontSize = 11.sp, color = Muted)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Scanner", tint = Muted)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Cream)

                // Viewport / Camera screen container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        // Display Live CameraX View
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().apply {
                                        setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx), BarcodeAnalyzer { barcode ->
                                        val currentTime = System.currentTimeMillis()
                                        if (barcode != lastScannedBarcode || (currentTime - lastScannedTime) > 2000L) {
                                            lastScannedBarcode = barcode
                                            lastScannedTime = currentTime
                                            handleScannedBarcode(barcode)
                                        }
                                    })

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (exc: Exception) {
                                        // Error binding camera
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Cool overlay design - Scanning line & scope target
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokePx = 1.5.dp.toPx()
                            val width = size.width
                            val height = size.height

                            // Draw rounded rectangular viewfinder outline
                            val rectW = width * 0.7f
                            val rectH = height * 0.5f
                            val rectX = (width - rectW) / 2f
                            val rectY = (height - rectH) / 2f

                            drawRoundRect(
                                color = Saffron,
                                topLeft = Offset(rectX, rectY),
                                size = Size(rectW, rectH),
                                style = Stroke(width = strokePx),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        }

                        // Scanning Red Laser Line Animation
                        val infiniteTransition = rememberInfiniteTransition(label = "laser")
                        val laserOffset by infiniteTransition.animateFloat(
                            initialValue = 0.25f,
                            targetValue = 0.75f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "laserOffset"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.5f)
                                .align(Alignment.TopCenter)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(2.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Red)
                                    .offset(y = 100.dp * (laserOffset - 0.5f))
                            )
                        }

                        Text(
                            text = "📷 Camera active. Aim at Barcode/QR.",
                            color = White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    } else {
                        // Permission request state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = White.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                            Text(
                                text = "Camera permission is required to point scan product barcodes physically.",
                                color = White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant Camera Permission", color = White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Scan results or Lookup Loader panel
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (simulatedBarcodeResult != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (resolvedResultItem != null) GreenLight else SaffronLight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Scanned/Simulated: $simulatedBarcodeResult",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (resolvedResultItem != null) Green else Saffron
                                    )
                                    if (isAILookupActive) {
                                        CircularProgressIndicator(color = Saffron, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lookupStatusMessage,
                                    fontSize = 11.sp,
                                    color = Navy,
                                    lineHeight = 15.sp
                                )
                                
                                if (resolvedResultItem != null) {
                                    val item = resolvedResultItem!!
                                    val gstPercentage = when (item.category) {
                                        "Dairy & Fresh" -> "5% GST"
                                        "Snacks" -> "12% GST"
                                        "Spices & Masala" -> "5% GST"
                                        "Beverages" -> "18% GST"
                                        "Household" -> "18% GST"
                                        "Hygienes & Soap" -> "18% GST"
                                        "Grains & Pulses" -> "0% (Exempt)"
                                        else -> "12% GST"
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = (if (resolvedResultItem != null) Green else Saffron).copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = item.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Navy)
                                            Text(text = "Category: ${item.category} | Tax Structure: $gstPercentage", fontSize = 11.sp, color = Muted)
                                        }
                                        Text(text = "₹${"%.2f".format(item.price)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Navy)
                                    }
                                }
                            }
                        }
                    } else {
                        // Ready state instructions
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Cream, shape = RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "💡 Tip: Barcode auto lookup immediately searches item and determines prices & GST instantly.",
                                fontSize = 11.sp,
                                color = Navy,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Manual Entry Fallback / Form section
                Spacer(modifier = Modifier.height(6.dp))
                if (showManualFormInScanner) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "📝 Manual Entry Fallback (Smudged / Unknown Barcode)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Saffron)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualFormName,
                                    onValueChange = { manualFormName = it },
                                    label = { Text("Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Saffron, unfocusedBorderColor = Border)
                                )
                                OutlinedTextField(
                                    value = manualFormPrice,
                                    onValueChange = { manualFormPrice = it },
                                    label = { Text("Price", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Saffron, unfocusedBorderColor = Border)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showManualFormInScanner = false }) {
                                    Text("Dismiss", fontSize = 12.sp, color = Muted)
                                }
                                Button(
                                    onClick = {
                                        val barcodeToSave = simulatedBarcodeResult ?: "SEC${(100000..999999).random()}"
                                        val priceVal = manualFormPrice.toDoubleOrNull() ?: 20.0
                                        if (isInventoryMode) {
                                            viewModel.addInventoryItem(
                                                name = manualFormName.takeIf { it.isNotEmpty() } ?: "Custom Item",
                                                price = priceVal,
                                                costPrice = priceVal * 0.8,
                                                barcode = barcodeToSave,
                                                stock = 50,
                                                category = manualFormCategory
                                            )
                                        } else {
                                            viewModel.quickAddInventoryAndCart(
                                                name = manualFormName.takeIf { it.isNotEmpty() } ?: "Custom Item",
                                                price = priceVal,
                                                costPrice = priceVal * 0.8,
                                                barcode = barcodeToSave,
                                                category = manualFormCategory
                                            )
                                        }
                                        simulatedBarcodeResult = barcodeToSave
                                        resolvedResultItem = ItemEntity(
                                            name = manualFormName.takeIf { it.isNotEmpty() } ?: "Custom Item",
                                            barcode = barcodeToSave,
                                            price = priceVal,
                                            costPrice = priceVal * 0.8,
                                            stock = 50,
                                            category = manualFormCategory
                                        )
                                        showManualFormInScanner = false
                                        lookupStatusMessage = if (isInventoryMode) "Success! Manually registered." else "Success! Manually registered & added to billing cart."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Register & Save", fontSize = 11.sp, color = White)
                                }
                            }
                        }
                    }
                }

                // Sandbox Emulator Simulator area
                Text(
                    text = "🖥️ WEB EMULATOR SIMULATOR SANDBOX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Cream, shape = RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    val demoItems = listOf(
                        Triple("8901058002315", "Tata Salt 1kg", "₹28.00 (5% GST)"),
                        Triple("8901262010014", "Amul Butter 100g", "₹56.00 (5% GST)"),
                        Triple("8901058862414", "Maggi Noodles 12P", "₹168.00 (12% GST)"),
                        Triple("8901725181223", "Aashirvaad Atta 5kg", "₹270.00 (0% GST)"),
                        Triple("8901764012229", "Coca-Cola 500ml", "₹40.00 (18% GST)"),
                        Triple("8901396221157", "Dettol Soap 250ml", "₹99.00 (18% GST)"),
                        Triple("8904063200122", "Haldiram Bhujia 150g", "₹45.00 (12% GST)"),
                        Triple("8900000000001", "Scan Unknown Item", "Triggers ShopIq AI!")
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(demoItems) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { handleScannedBarcode(item.first) },
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Border)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Green, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(text = item.second, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
                                            Text(text = "UPC: ${item.first}", fontSize = 9.sp, color = Muted)
                                        }
                                    }
                                    Text(text = item.third, fontSize = 11.sp, color = Saffron, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom footer action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showManualFormInScanner = true
                            manualFormName = ""
                            manualFormPrice = ""
                            manualFormCost = ""
                            manualFormCategory = "Dairy & Fresh"
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Saffron),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Manual Entry", color = Saffron, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Finished scanning", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@androidx.camera.core.ExperimentalGetImage
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrEmpty()) {
                            onBarcodeDetected(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    // Fail silently
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

// ======================== STOCK LEDGER SCREEN ========================

@Composable
fun InventoryScreen(viewModel: KiranaViewModel) {
    val items by viewModel.inventoryItems.collectAsState()
    val isAiLoading by viewModel.isAILoading.collectAsState()
    val searchQuery by viewModel.inventorySearchQuery.collectAsState()

    var showAddItemPopup by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Local Stock Ledger", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)

            Button(
                onClick = { showAddItemPopup = true },
                colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setInventorySearchQuery(it) },
            placeholder = { Text("Search catalog products, categories, barcode...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Saffron) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Saffron,
                focusedLabelColor = Saffron,
                focusedTextColor = Navy,
                unfocusedTextColor = Navy
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_box")
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Muted, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Inventory is Empty", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text(
                        text = "Add custom products manually, or seed a complete list of 8 standard ShopIq staples instantly for testing!",
                        fontSize = 12.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.seedSampleInventory() },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Seed Standard ShopIq staples")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp)
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SaffronLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = item.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Saffron)
                                    }
                                    if (item.barcode != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Barcode: ${item.barcode}", fontSize = 10.sp, color = Muted)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(text = "Sell Price: ₹${"%.1f".format(item.price)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy)
                                    Text(text = "Cost: ₹${"%.1f".format(item.costPrice)}", fontSize = 12.sp, color = Muted)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (item.stock < 10) SaffronLight else GreenLight)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Qty: ${item.stock}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.stock < 10) Saffron else Green
                                    )
                                }

                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    IconButton(
                                        onClick = { itemToEdit = item },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit stock", tint = Muted, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteInventoryItem(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete product", tint = Saffron, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Product Dialog popup
    if (showAddItemPopup) {
        Dialog(onDismissRequest = { showAddItemPopup = false }) {
            var name by remember { mutableStateOf("") }
            var price by remember { mutableStateOf("") }
            var costPrice by remember { mutableStateOf("") }
            var barcode by remember { mutableStateOf("") }
            var stock by remember { mutableStateOf("50") }
            var category by remember { mutableStateOf("Snacks") }
            var isDetailsFetching by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "Add New Product Record", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Product Query / Short Name") },
                            placeholder = { Text("e.g., Surf Excel 1kg") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Saffron,
                                focusedLabelColor = Saffron,
                                focusedTextColor = Navy,
                                unfocusedTextColor = Navy
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (name.isNotEmpty()) {
                                    isDetailsFetching = true
                                    viewModel.fetchAISuggestedItem(name) { suggested ->
                                        name = suggested.name
                                        price = suggested.price.toString()
                                        costPrice = suggested.costPrice.toString()
                                        barcode = suggested.barcode ?: ""
                                        category = suggested.category
                                        isDetailsFetching = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(GreenLight, shape = RoundedCornerShape(10.dp))
                        ) {
                            if (isDetailsFetching) {
                                CircularProgressIndicator(color = Green, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Fetch details", tint = Green)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Store Sell Price ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Acquisition Cost Price ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    var showScannerForForm by remember { mutableStateOf(false) }
                    
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode (Required)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showScannerForForm = true }) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = Saffron)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )
                    
                    if (showScannerForForm) {
                        CameraBarcodeScannerDialog(
                            viewModel = viewModel,
                            isInventoryMode = true,
                            onBarcodeScanned = { scanned ->
                                barcode = scanned
                            },
                            onDismiss = { showScannerForForm = false }
                        )
                    }

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Opening Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    var expandedCategoryDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedCategoryDropdown = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)
                        ) {
                            Text("Category: $category")
                        }
                        DropdownMenu(
                            expanded = expandedCategoryDropdown,
                            onDismissRequest = { expandedCategoryDropdown = false },
                            modifier = Modifier.background(White)
                        ) {
                            listOf("Grains & Pulses", "Beverages", "Snacks", "Hygienes & Soap", "Spices & Masala", "Dairy & Fresh", "Household", "Cosmetics", "Others").forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        expandedCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddItemPopup = false }) {
                            Text("Cancel", color = Muted)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (barcode.isNotEmpty() && name.isNotEmpty()) {
                                    viewModel.addInventoryItem(
                                        name = name,
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                                        barcode = barcode.takeIf { it.isNotEmpty() },
                                        stock = stock.toIntOrNull() ?: 50,
                                        category = category
                                    )
                                    showAddItemPopup = false
                                }
                            },
                            enabled = barcode.isNotEmpty() && name.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Saffron,
                                disabledContainerColor = SaffronLight
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Staples", color = White)
                        }
                    }
                }
            }
        }
    }

    // Edit Product Dialog popup
    if (itemToEdit != null) {
        Dialog(onDismissRequest = { itemToEdit = null }) {
            var name by remember { mutableStateOf(itemToEdit!!.name) }
            var price by remember { mutableStateOf(itemToEdit!!.price.toString()) }
            var costPrice by remember { mutableStateOf(itemToEdit!!.costPrice.toString()) }
            var stock by remember { mutableStateOf(itemToEdit!!.stock.toString()) }

            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "Modify Product Stack", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Selling Price ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Acquisition Cost Price ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Inventory Stock Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { itemToEdit = null }) {
                            Text("Cancel", color = Muted)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                viewModel.updateInventoryItem(
                                    itemToEdit!!.copy(
                                        name = name,
                                        price = price.toDoubleOrNull() ?: itemToEdit!!.price,
                                        costPrice = costPrice.toDoubleOrNull() ?: itemToEdit!!.costPrice,
                                        stock = stock.toIntOrNull() ?: itemToEdit!!.stock
                                    )
                                )
                                itemToEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Apply changes", color = White)
                        }
                    }
                }
            }
        }
    }
}

// ======================== TRANSACTION HISTORY LEDGER ========================

@Composable
fun BillHistoryScreen(viewModel: KiranaViewModel) {
    val bills by viewModel.allBills.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Transaction Billing Ledger", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)

        if (bills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = Muted, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No Invoices Logged Yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text(text = "When you checkout active bills, they will appear in this historical ledger.", fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bills) { bill ->
                    var showReceiptDialog by remember { mutableStateOf(false) }
                    val timeString = remember(bill.timestamp) {
                        SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(bill.timestamp))
                    }

                    Card(
                        onClick = { showReceiptDialog = true },
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = bill.billNumber, fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (bill.paymentMode == "UPI") GreenLight else SaffronLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = bill.paymentMode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (bill.paymentMode == "UPI") Green else Saffron)
                                    }
                                }
                                Text(text = timeString, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
                                if (!bill.customerMobile.isNullOrEmpty()) {
                                    Text(text = "Customer Mobile: +91 ${bill.customerMobile}", fontSize = 10.sp, color = Muted)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹${"%.1f".format(bill.totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    color = Navy
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
                            }
                        }
                    }

                    if (showReceiptDialog) {
                        ReceiptDetailDialog(billId = bill.id, viewModel = viewModel, onDismiss = { showReceiptDialog = false })
                    }
                }
            }
        }
    }
}

// ======================== STATS / NATIVE CANVAS ANALYTICS ========================

@Composable
fun AnalyticsScreen(viewModel: KiranaViewModel) {
    val bills by viewModel.allBills.collectAsState()
    val items by viewModel.inventoryItems.collectAsState()

    val totalRevenue = remember(bills) { bills.sumOf { it.totalAmount } }
    
    // Compute total cost price for stock
    val totalStockValuation = remember(items) { items.sumOf { it.stock * it.costPrice } }
    val totalStockRetailValuation = remember(items) { items.sumOf { it.stock * itemToFloatPrice(it) } }
    val potentialProfitMargin = remember(totalStockValuation, totalStockRetailValuation) {
        if (totalStockRetailValuation > 0) {
            ((totalStockRetailValuation - totalStockValuation) / totalStockRetailValuation * 100).coerceIn(0.0, 100.0)
        } else 0.0
    }

    // Mock category breakdowns
    val categoriesMap = remember(items) {
        val mapped = items.groupBy { it.category }
        val finalRep = mapped.map { (cat, list) ->
            cat to list.size.toFloat()
        }.sortedByDescending { it.second }
        if (finalRep.isEmpty()) {
            listOf("General" to 1f)
        } else finalRep
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "ShopIq Store Analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
        }

        // Summary Statistics cards
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Valuation & Sales Breakdown", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)

                    Divider(color = Border)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Historical Net Sales Revenue", fontSize = 12.sp, color = Muted)
                        Text(text = "₹${"%.1f".format(totalRevenue)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Navy)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Current Wholesale Stock Cost", fontSize = 12.sp, color = Muted)
                        Text(text = "₹${"%.1f".format(totalStockValuation)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Navy)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Estimated Potential Margin", fontSize = 12.sp, color = Muted)
                        Text(text = "${"%.1f".format(potentialProfitMargin)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Green)
                    }
                }
            }
        }

        // Native Donut Chart Drawn Customly via Canvas
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Staples Category Share",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val donutColors = listOf(Saffron, Green, Navy, SaffronMid, GreenMid, Muted)
                        Canvas(modifier = Modifier.size(140.dp)) {
                            var currentAngle = -90f
                            val totalElements = categoriesMap.sumOf { it.second.toDouble() }.toFloat()

                            categoriesMap.forEachIndexed { idx, pair ->
                                val sweep = if (totalElements > 0) (pair.second / totalElements) * 360f else 360f
                                val col = donutColors[idx % donutColors.size]
                                drawArc(
                                    color = col,
                                    startAngle = currentAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    size = Size(size.width, size.height),
                                    style = Stroke(width = 32f, cap = StrokeCap.Round)
                                )
                                currentAngle += sweep
                            }
                        }
                        Text(
                            text = "${items.size}\nStaples",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Legend Grid
                    val donutColors = listOf(Saffron, Green, Navy, SaffronMid, GreenMid, Muted)
                    FlowRowLegend(categoriesMap = categoriesMap, colors = donutColors)
                }
            }
        }

        // Native Bar Chart Trending
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Weekly Revenue Trend",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Let's implement a Canvas drawn 7-day Bar graph
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val values = listOf(0.4f, 0.7f, 0.5f, 0.3f, 0.8f, 0.95f, 0.6f) // mock percentages of target
                        val barWidth = 36f
                        val spacing = (size.width - (values.size * barWidth)) / (values.size + 1)

                        // Base axis line
                        drawLine(
                            color = Border,
                            start = Offset(0f, size.height - 24f),
                            end = Offset(size.width, size.height - 24f),
                            strokeWidth = 3f
                        )

                        for (i in values.indices) {
                            val x = spacing + i * (barWidth + spacing)
                            val barHeight = values[i] * (size.height - 40f)
                            val y = size.height - 24f - barHeight

                            // Rounded Rect bar
                            drawRect(
                                color = Saffron,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        days.forEach { day ->
                            Text(text = day, fontSize = 11.sp, color = Muted, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowRowLegend(categoriesMap: List<Pair<String, Float>>, colors: List<Color>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categoriesMap.take(4).forEachIndexed { index, pair ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors[index % colors.size])
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = pair.first, fontSize = 11.sp, color = Navy, modifier = Modifier.weight(1f))
                Text(text = "${pair.second.toInt()} items", fontSize = 11.sp, color = Muted)
            }
        }
    }
}

// ======================== RECEIPT DIALOG MODAL ========================

@Composable
fun ReceiptDetailDialog(billId: Int, viewModel: KiranaViewModel, onDismiss: () -> Unit) {
    var billDetail by remember { mutableStateOf<BillEntity?>(null) }
    var billItems by remember { mutableStateOf<List<BillItemEntity>>(emptyList()) }
    var storeProfile by remember { mutableStateOf<String>("ShopIq Store") }

    LaunchedEffect(billId) {
        viewModel.getBillDetails(billId) { bill, items ->
            billDetail = bill
            billItems = items
        }
        viewModel.authState.value.let {
            if (it is AuthState.Authenticated) {
                storeProfile = it.storeName
            }
        }
    }

    if (billDetail != null) {
        val bill = billDetail!!
        val timeString = remember(bill.timestamp) {
            SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(bill.timestamp))
        }

        Dialog(onDismissRequest = onDismiss) {
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Receipt Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = storeProfile, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Text(text = "Transaction Billing Invoice", fontSize = 11.sp, color = Muted)
                    }

                    // Metadata table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Cream, shape = RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Invoice ID", fontSize = 11.sp, color = Muted)
                            Text(text = bill.billNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Timestamp", fontSize = 11.sp, color = Muted)
                            Text(text = timeString, fontSize = 11.sp, color = Navy)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Payment Mode", fontSize = 11.sp, color = Muted)
                            Text(text = bill.paymentMode, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (bill.paymentMode == "UPI") Green else Saffron)
                        }
                        if (!bill.customerMobile.isNullOrEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Customer Phone", fontSize = 11.sp, color = Muted)
                                Text(text = "+91 " + bill.customerMobile, fontSize = 11.sp, color = Navy)
                            }
                        }
                    }

                    // Receipt Staples line items
                    Text(text = "Billing Items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        billItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text(text = item.itemName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
                                    Text(text = "${item.quantity} units x ₹${"%.1f".format(item.price)}", fontSize = 10.sp, color = Muted)
                                }
                                Text(
                                    text = "₹${"%.1f".format(item.total)}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy
                                )
                            }
                        }
                    }

                    Divider(color = Border)

                    // Grand total summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Amount Paid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Text(
                            text = "₹${"%.1f".format(bill.totalAmount)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Saffron
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Close Receipt", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helpers
private fun itemToFloatPrice(item: ItemEntity): Double {
    return item.price
}

@Composable
fun KiranaLandingDialog(onDismiss: () -> Unit) {
    var selectedPlanForDemo by remember { mutableStateOf<String?>(null) }
    var expandedProblemIndex by remember { mutableStateOf(-1) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Cream),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Saffron),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SHOPIQ PRO HUB",
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Features, Pricing & Credentials",
                                color = SaffronLight,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = White)
                    }
                }
                
                // Content Body (Scrollable)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // HERO HEADER
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SaffronLight),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Saffron)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "AI-Powered Billing Platform",
                                        color = White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Your ShopIq Store, Smarter Than Ever",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Navy,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 28.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Scan barcodes, generate instant bills, track outstanding credit ledger (udhar), and gain AI-powered sales insights — directly from your phone. No physical notebook required.",
                                    fontSize = 13.sp,
                                    color = Navy.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    
                    // THREE METRICS ROW
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val metrics = listOf(
                                Triple("10x", "Faster Billing", Green),
                                Triple("₹0", "Billing Errors", Saffron),
                                Triple("5 Min", "Set Up Time", Navy)
                            )
                            metrics.forEach { (bold, sub, color) ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = White),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Border)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = bold, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = sub, fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                    
                    // RECEIPTS SIMULATION PREVIEW
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = White),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Border)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Sharma General Store", fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp)
                                        Text(text = "Laxmi Nagar, Delhi • BILL #0247", fontSize = 11.sp, color = Muted)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(GreenLight)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = "Scanned ✓", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "-".repeat(36),
                                    fontFamily = FontFamily.Monospace,
                                    color = Border,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val sampleBillItems = listOf(
                                    Triple("Aashirvaad Aata 5kg", "1 × ₹285.00", "₹285.00"),
                                    Triple("Tata Salt 1kg", "2 × ₹22.00", "₹44.00"),
                                    Triple("Surf Excel 1kg", "1 × ₹349.00", "₹349.00"),
                                    Triple("Amul Butter 500g", "1 × ₹169.50", "₹169.50")
                                )
                                
                                sampleBillItems.forEach { (name, qtyPrice, tot) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy)
                                            Text(text = qtyPrice, fontSize = 11.sp, color = Muted)
                                        }
                                        Text(text = tot, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Navy, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "-".repeat(36),
                                    fontFamily = FontFamily.Monospace,
                                    color = Border,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "GST (5%)", fontSize = 12.sp, color = Muted)
                                    Text(text = "₹42.38", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Muted)
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "BILL TOTAL (4 items)", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Navy)
                                    Text(text = "₹889.88", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Saffron, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                    
                    // THE PROBLEM SECTION (Interactive Accordions!)
                    item {
                        Column {
                            Text(
                                text = "The Problem",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Saffron,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Manual Bookkeeping Costs You Daily",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            val problems = listOf(
                                "Slow Handwritten Billing" to "Writing every bill by hand takes minutes per customer, creating long queues and unhappy shoppers in your shop.",
                                "Calculation Errors" to "Manual math leads to billing mistakes — either you lose your hard-earned margin, or the customer disputes the bill because they distrust you.",
                                "Scattered Credit Records" to "Using multiple physical notebooks for udhar (credit ledger) makes it impossible to see who owes you what, when, or how much.",
                                "No Business Insights" to "Without digital ledger software, you can't tell which products are peak sellers or what your actual gross margin or cashflow looks like.",
                                "No Digital Backups" to "Paper bills get lost, wet, or misread. If you lose your record, you lose your record, which creates instant store-vs-customer disputes.",
                                "Month-End Math Headaches" to "Spending hours after-closing to sum totals and reconcile balances on loose paper sheets when you should be resting."
                            )
                            
                            problems.forEachIndexed { idx, (title, desc) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            expandedProblemIndex = if (expandedProblemIndex == idx) -1 else idx
                                        },
                                    colors = CardDefaults.cardColors(containerColor = White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (expandedProblemIndex == idx) Saffron else Border)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Saffron,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                                            }
                                            Icon(
                                                imageVector = if (expandedProblemIndex == idx) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Muted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        if (expandedProblemIndex == idx) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = desc,
                                                fontSize = 12.sp,
                                                color = Muted,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // FEATURES GRID
                    item {
                        Column {
                            Text(
                                text = "Features",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Green,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Everything You Need, Smarter.",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            val featuresList = listOf(
                                Pair("01 — SCANNING", "Point phone camera at any product. Kirana scans in <1s, automatically fetching the product name, price structure, and GST category metadata instantly."),
                                Pair("02 — AI ANALYTICS", "Weekly trends, top sales graphs, stock status, and AI insights generated automatically to analyze and level-up your general store revenue."),
                                Pair("03 — INSTANT BILLING", "Auto-generated digital bills carrying custom store name and full item breakdown. Send via WhatsApp / SMS directly or save as offline PDF."),
                                Pair("04 — CREDIT UDHAR BOOK", "Replace physical books with a secure database ledger. Set customer profiles, check total credit, and send polite automated payment reminder templates.")
                            )
                            
                            featuresList.forEach { (head, desc) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    colors = CardDefaults.cardColors(containerColor = White),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Border)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = head, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Saffron)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = desc, fontSize = 12.sp, color = Navy, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                    
                    // HOW IT WORKS TIMELINE
                    item {
                        Column {
                            Text(
                                text = "How It Works",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Up & Running in Under 5 Minutes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            val steps = listOf(
                                "01" to "Download & Login" to "Install Kirana store manager and log in with your mobile number in seconds.",
                                "02" to "Set Up Your Store" to "Enter store name and location coordinates. Add staples manually or let AI auto-fill them.",
                                "03" to "Start Scanning & Billing" to "Point your camera to trigger scan, add items, and receive UPI / Cash instantly.",
                                "04" to "Watch AI Insights Grow" to "Watch automatic weekly reports, graphs, and smart AI inventory forecasting tips!"
                            )
                            
                            steps.forEach { step ->
                                val (numTitle, desc) = step
                                val (num, title) = numTitle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Navy),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = num, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                                        Text(text = desc, fontSize = 12.sp, color = Muted, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                    
                    // PRICING CARDS
                    item {
                        Column {
                            Text(
                                text = "Simple Pricing",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Saffron,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Choose Your Store Plan",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            if (selectedPlanForDemo != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = GreenLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Green)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Selected Plan: $selectedPlanForDemo active for this store!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Green
                                        )
                                    }
                                }
                            }
                            
                            // Free Plan
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Border)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(text = "Free Plan", fontWeight = FontWeight.Bold, color = Muted, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "₹0", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Navy)
                                    Text(text = "/ month, forever", fontSize = 11.sp, color = Muted)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "• Up to 50 bills / month\n• Barcode scanning & fallbacks\n• Basic analytics metadata\n• 10 active outstanding credit profiles", fontSize = 12.sp, color = Navy, lineHeight = 18.sp)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { selectedPlanForDemo = "Free Plan" },
                                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Get Started Free")
                                    }
                                }
                            }
                            
                            // Pro Plan
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(2.dp, Saffron)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "PRO PLAN", fontWeight = FontWeight.Black, color = Saffron, fontSize = 13.sp)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SaffronLight)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = "RECOMMENDED", color = Saffron, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "₹299", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Navy)
                                    Text(text = "/ month, billed monthly", fontSize = 11.sp, color = Muted)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "• Unlimited automated bills\n• POINT CAMERA scanning\n• Full AI analytics summaries\n• Unlimited credit ledger accounts\n• Excel spreadsheet + PDF Export\n• Direct WhatsApp payment alerts", fontSize = 12.sp, color = Navy, lineHeight = 18.sp)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { selectedPlanForDemo = "Pro Trial Plan" },
                                        colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Start 30-Day Free Trial", color = White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            // Business Plan
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Border)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(text = "Business Plan", fontWeight = FontWeight.Bold, color = Muted, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "₹699", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Navy)
                                    Text(text = "/ month, per store", fontSize = 11.sp, color = Muted)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "• Everything included in Pro\n• Multi-store management portal\n• Staff / cashier accounts (roles)\n• Automated GST filing reports\n• Live inventory tracking & 24/7 priority support", fontSize = 12.sp, color = Navy, lineHeight = 18.sp)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedButton(
                                        onClick = { selectedPlanForDemo = "Business Plan (Contacted Sales)" },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Navy),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Contact Sales Team")
                                    }
                                }
                            }
                        }
                    }
                    
                    // TESTIMONIALS SECTION
                    item {
                        Column {
                            Text(
                                text = "Trusted By Store Owners",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Green,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Real Stores, Real Results",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            val reviews = listOf(
                                Triple("RS", "Rajesh Sharma", "Pehle bill likhne mein 5 minute lagte the. Ab 30 second. Customers bhi khush hain, main bhi khush hoon.") to "Sharma General Store, Delhi",
                                Triple("MK", "Manoj Kumar", "Udhar notebook ka jhanjhat khatam. Ab WhatsApp pe reminder bhejta hoon, paise time pe milte hain.") to "Manoj Kirana, Lucknow",
                                Triple("SG", "Sunita Gupta", "Monthly report se pata chala ki biscuits sabse zyada bikti hain. Maine stock badha diya — profit 18% badh gaya.") to "Gupta Provisions, Jaipur"
                            )
                            
                            reviews.forEach { (reviewer, location) ->
                                val (initials, name, text) = reviewer
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    colors = CardDefaults.cardColors(containerColor = White),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Border)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "“$text”",
                                            fontSize = 12.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = Navy,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(SaffronLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = initials, color = Saffron, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
                                                Text(text = location, fontSize = 10.sp, color = Muted)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // FOOTER
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your store deserves better than notebook bookkeeping.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = Navy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Available in Android & iOS • Works Offline • Multi-Language Support",
                                fontSize = 10.sp,
                                color = Muted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "© 2026 ShopIq Technologies Pvt. Ltd. Made within India",
                                fontSize = 9.sp,
                                color = Muted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UdharBookScreen(viewModel: KiranaViewModel) {
    val customers by viewModel.allCustomers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val logs by viewModel.customerLogs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showRepaymentDialog by remember { mutableStateOf(false) }

    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.mobile.contains(searchQuery)
    }

    val totalDues = customers.sumOf { it.outstandingBalance }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            // Mobile (Single Column / Pane) layout
            if (selectedCustomer == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Balance card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SaffronLight),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Total Outstanding Credit (Udhar)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Navy
                            )
                            Text(
                                text = "₹${"%,.2f".format(totalDues)}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Navy
                            )
                            Button(
                                onClick = { showAddCustomerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Customer", fontWeight = FontWeight.Bold, color = White)
                            }
                        }
                    }

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name or phone...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Muted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Saffron,
                            focusedLabelColor = Saffron,
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy,
                            unfocusedBorderColor = Cream
                        )
                    )

                    // Customer list
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .padding(8.dp)
                    ) {
                        if (filteredCustomers.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No customers found", color = Muted, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredCustomers) { cust ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectCustomer(cust) },
                                        colors = CardDefaults.cardColors(containerColor = Cream),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Customer initials avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (cust.outstandingBalance > 0) SaffronLight else GreenLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val initials = cust.name.trim().split(" ")
                                                    .filter { it.isNotEmpty() }
                                                    .map { it.take(1) }
                                                    .joinToString("")
                                                    .take(2)
                                                    .uppercase()
                                                Text(
                                                    text = initials,
                                                    color = if (cust.outstandingBalance > 0) Saffron else Green,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = cust.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Navy
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = null,
                                                        tint = Muted,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = cust.mobile,
                                                        fontSize = 11.sp,
                                                        color = Muted
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "₹${"%.0f".format(cust.outstandingBalance)}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = if (cust.outstandingBalance > 0) Saffron else Green
                                                )
                                                if (cust.outstandingBalance > 0) {
                                                    Text(text = "Dues", fontSize = 9.sp, color = Saffron)
                                                } else {
                                                    Text(text = "Clear", fontSize = 9.sp, color = Green)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Show detailed customer info full screen for mobile
                val currentCust = selectedCustomer!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Back button header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.selectCustomer(null) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(White, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to list",
                                tint = Navy
                            )
                        }
                        Text(
                            text = "Customer Ledger",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Navy
                        )
                    }

                    // Detailed ledger view panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Title header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Navy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = currentCust.name.trim().split(" ")
                                        .filter { it.isNotEmpty() }
                                        .map { it.take(1) }
                                        .joinToString("")
                                        .take(2)
                                        .uppercase()
                                    Text(
                                        text = initials,
                                        color = White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentCust.name,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Navy
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = Muted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = currentCust.mobile,
                                            fontSize = 12.sp,
                                            color = Muted
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Outstanding", fontSize = 11.sp, color = Muted)
                                    Text(
                                        text = "₹${"%,.2f".format(currentCust.outstandingBalance)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (currentCust.outstandingBalance > 0) Saffron else Green
                                    )
                                }
                            }

                            HorizontalDivider(thickness = 1.dp, color = Cream)

                            // Action buttons with icons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showRepaymentDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Record Activity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White)
                                }

                                val context = androidx.compose.ui.platform.LocalContext.current
                                Button(
                                    onClick = {
                                        val message = "Dear ${currentCust.name}, your outstanding dues at our shop is Rs. ${"%.2f".format(currentCust.outstandingBalance)}. Please clear the outstanding via UPI or Cash. Thank you! - ShopIq"
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Payment Reminder", message)
                                        clipboard.setPrimaryClip(clip)
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, message)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Send reminder with:")
                                        context.startActivity(shareIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White)
                                }
                            }

                            Text("Ledger Timeline", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)

                            // Logs timeline list
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(logs) { log ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Cream),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val isPurchase = log.type == "Purchase"
                                                    val col = if (isPurchase) Saffron else Green
                                                    val ico = if (isPurchase) Icons.Default.TrendingUp else Icons.Default.CheckCircle
                                                    Icon(
                                                        imageVector = ico,
                                                        contentDescription = null,
                                                        tint = col,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = log.type,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = col
                                                    )
                                                }
                                                Text(
                                                    text = log.note ?: "",
                                                    fontSize = 11.sp,
                                                    color = Navy
                                                )
                                                val sfd = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                                Text(
                                                    text = sfd.format(Date(log.timestamp)),
                                                    fontSize = 9.sp,
                                                    color = Muted
                                                )
                                            }

                                            Text(
                                                text = "${if (log.type == "Purchase") "+" else "-"} ₹${"%.0f".format(log.amount)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (log.type == "Purchase") Saffron else Green
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Expanded/Tablet (Side-by-Side Dual Pane) layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Upper balance card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SaffronLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Outstanding Credit (Udhar)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Navy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${"%,.2f".format(totalDues)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Navy
                            )
                        }

                        Button(
                            onClick = { showAddCustomerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Customer", fontWeight = FontWeight.Bold, color = White)
                        }
                    }
                }

                // Search Bar or customer profile trigger
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or phone...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Muted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Saffron,
                        focusedLabelColor = Saffron,
                        focusedTextColor = Navy,
                        unfocusedTextColor = Navy,
                        unfocusedBorderColor = Cream
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Customer List List pane
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .padding(8.dp)
                    ) {
                        if (filteredCustomers.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No customers found", color = Muted, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredCustomers) { cust ->
                                    val isChosen = selectedCustomer?.id == cust.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectCustomer(cust) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isChosen) SaffronLight.copy(alpha = 0.6f) else Cream
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Customer initials avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (cust.outstandingBalance > 0) SaffronLight else GreenLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val initials = cust.name.trim().split(" ")
                                                    .filter { it.isNotEmpty() }
                                                    .map { it.take(1) }
                                                    .joinToString("")
                                                    .take(2)
                                                    .uppercase()
                                                Text(
                                                    text = initials,
                                                    color = if (cust.outstandingBalance > 0) Saffron else Green,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = cust.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Navy
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = null,
                                                        tint = Muted,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = cust.mobile,
                                                        fontSize = 11.sp,
                                                        color = Muted
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "₹${"%.0f".format(cust.outstandingBalance)}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = if (cust.outstandingBalance > 0) Saffron else Green
                                                )
                                                if (cust.outstandingBalance > 0) {
                                                    Text(text = "Dues", fontSize = 9.sp, color = Saffron)
                                                } else {
                                                    Text(text = "Clear", fontSize = 9.sp, color = Green)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Customer Detail View Panel (Right list detail canopy)
                    Box(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .padding(16.dp)
                    ) {
                        val currentCust = selectedCustomer
                        if (currentCust == null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = Saffron.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Select a customer to view ledger timeline & log repayment",
                                    color = Muted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Detailed Customer View
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Title header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Navy),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initials = currentCust.name.trim().split(" ")
                                            .filter { it.isNotEmpty() }
                                            .map { it.take(1) }
                                            .joinToString("")
                                            .take(2)
                                            .uppercase()
                                        Text(
                                            text = initials,
                                            color = White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentCust.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = Navy
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = Muted,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = currentCust.mobile,
                                                fontSize = 12.sp,
                                                color = Muted
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Outstanding Dues", fontSize = 11.sp, color = Muted)
                                        Text(
                                            text = "₹${"%,.2f".format(currentCust.outstandingBalance)}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (currentCust.outstandingBalance > 0) Saffron else Green
                                        )
                                    }
                                }

                                HorizontalDivider(thickness = 1.dp, color = Cream)

                                // Action row (Rec repayment, Send notification)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showRepaymentDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Record Activity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White)
                                    }

                                    // Share payment reminder
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    Button(
                                        onClick = {
                                            val message = "Dear ${currentCust.name}, your outstanding dues at our shop is Rs. ${"%.2f".format(currentCust.outstandingBalance)}. Please clear the outstanding via UPI or Cash. Thank you! - ShopIq"
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Payment Reminder", message)
                                            clipboard.setPrimaryClip(clip)
                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, message)
                                                type = "text/plain"
                                            }
                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Send reminder with:")
                                            context.startActivity(shareIntent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White)
                                    }
                                }

                                Text("Ledger Timeline", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)

                                // Logs List
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(logs) { log ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Cream),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val isPurchase = log.type == "Purchase"
                                                        val col = if (isPurchase) Saffron else Green
                                                        val ico = if (isPurchase) Icons.Default.TrendingUp else Icons.Default.CheckCircle
                                                        Icon(
                                                            imageVector = ico,
                                                            contentDescription = null,
                                                            tint = col,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = log.type,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = col
                                                        )
                                                    }
                                                    Text(
                                                        text = log.note ?: "",
                                                        fontSize = 11.sp,
                                                        color = Navy
                                                    )
                                                    val sfd = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                                    Text(
                                                        text = sfd.format(Date(log.timestamp)),
                                                        fontSize = 9.sp,
                                                        color = Muted
                                                    )
                                                }

                                                Text(
                                                    text = "${if (log.type == "Purchase") "+" else "-"} ₹${"%.0f".format(log.amount)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (log.type == "Purchase") Saffron else Green
                                                )
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

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        Dialog(onDismissRequest = { showAddCustomerDialog = false }) {
            var name by remember { mutableStateOf("") }
            var mobile by remember { mutableStateOf("") }

            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Add Customer to Ledger", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Saffron) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy,
                            focusedBorderColor = Saffron
                        )
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Saffron) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Navy,
                            unfocusedTextColor = Navy,
                            focusedBorderColor = Saffron
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddCustomerDialog = false }) {
                            Text("Cancel", color = Muted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotEmpty() && mobile.isNotEmpty()) {
                                    viewModel.addCustomer(name, mobile)
                                    showAddCustomerDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add to Book", color = White)
                        }
                    }
                }
            }
        }
    }

    // Repayment Dialog
    if (showRepaymentDialog) {
        val currentCust = selectedCustomer
        if (currentCust != null) {
            Dialog(
                onDismissRequest = { showRepaymentDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                var paymentAmt by remember { mutableStateOf("") }
                var note by remember { mutableStateOf("") }
                var txMode by remember { mutableStateOf("Credit") } // Credit, Debit, Balance

                Card(
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .imePadding()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Record Transaction",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Debit", "Credit", "Balance").forEach { mode ->
                                val isSelected = txMode == mode
                                val containerCol = if (isSelected) Saffron else Cream
                                val textCol = if (isSelected) White else Navy
                                Button(
                                    onClick = { txMode = mode },
                                    colors = ButtonDefaults.buttonColors(containerColor = containerCol),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(mode, color = textCol, fontSize = 12.sp)
                                }
                            }
                        }

                        Text(
                            text = "Customer: ${currentCust.name} (Outstanding: ₹${"%.2f".format(currentCust.outstandingBalance)})",
                            fontSize = 12.sp,
                            color = Muted
                        )

                        OutlinedTextField(
                            value = paymentAmt,
                            onValueChange = { paymentAmt = it },
                            label = { 
                                val lbl = when(txMode) {
                                    "Debit" -> "Added Dues (₹)"
                                    "Credit" -> "Amount Received (₹)"
                                    else -> "New Outstanding Balance (₹)"
                                }
                                Text(lbl) 
                            },
                            leadingIcon = { Icon(imageVector = Icons.Default.CurrencyRupee, contentDescription = null, tint = if(txMode=="Credit") Green else Saffron) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Navy,
                                unfocusedTextColor = Navy,
                                focusedBorderColor = Saffron
                            )
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Additional Note (Optional)") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Muted) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            placeholder = { Text("Details...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Navy,
                                unfocusedTextColor = Navy,
                                focusedBorderColor = Saffron
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showRepaymentDialog = false }) {
                                Text("Cancel", color = Muted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amt = paymentAmt.toDoubleOrNull()
                                    if (amt != null && amt >= 0) {
                                        val isSetBalance = txMode == "Balance"
                                        val logType = if (isSetBalance) "Adjustment" else if (txMode == "Debit") "Purchase" else "Repayment"
                                        viewModel.addCustomerTransaction(currentCust.id, logType, amt, note, isSetBalance)
                                        showRepaymentDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save", color = White)
                            }
                        }
                    }
                }
            }
        }
    }
}

