package com.licel.samples

// Copyright 2023 LICEL CORPORATION
// All Rights Reserved.
//
// NOTICE:  All information contained herein is, and remains
// the property of LICEL CORPORATION and its suppliers,
// if any.  The intellectual and technical concepts contained
// herein are proprietary to LICEL CORPORATION
// and its suppliers and may be covered by U.S. and Foreign Patents,
// patents in process, and are protected by trade secret or copyright law.
// Dissemination of this information or reproduction of this material
// is strictly forbidden unless prior written permission is obtained
// from LICEL CORPORATION.

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.licel.samples.core.ui.DismissBackground
import com.licel.samples.core.ui.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onFloatingButtonQRScanClick: () -> Unit = {},
    onFloatingButtonSimulateClick: () -> Unit = {},
    isSetupJCardSimR: Boolean = false,
    onDismissDialog: () -> Unit = {},
    onRemoveOtp: (Int) -> Unit = {},
    onBackButtonPress: () -> Unit = {},
) {
    val lazyListState = rememberLazyListState()

    if (!uiState.isQrCodeValid) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(text = "Cannot interpret QR code") },
            confirmButton = {
                TextButton(onClick = onDismissDialog) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (uiState.isQrCodeDuplicated) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(text = "Duplicate QR code") },
            confirmButton = {
                TextButton(onClick = onDismissDialog) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (uiState.isSwError) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(text = "SW: ${"%x".format(uiState.sw)}") },
            confirmButton = {
                TextButton(onClick = onDismissDialog) {
                    Text("Dismiss")
                }
            }
        )
    }

    BackHandler { onBackButtonPress() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authenticator") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    titleContentColor = Color.White,
                    containerColor = Color.Black,
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Your main content goes here
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    top = 20.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                modifier = modifier,
                state = lazyListState
            ) {
                items(uiState.otpParams.size) {
                    val otp = uiState.otpParams[it]
                    OtpItem(
                        modifier = modifier,
                        otp = otp,
                        otpValue = uiState.otpValue[otp.keyId] ?: "",
                        onRemove = onRemoveOtp,
                        counter = if (otp.period == 30)
                            uiState.counter30
                        else
                            uiState.counter60
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), // Adjust the padding as needed
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    FloatingActionButton(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        onClick = onFloatingButtonQRScanClick,
//                        onClick = onFloatingButtonSimulateClick,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpItem(
    modifier: Modifier = Modifier,
    otp: Otp,
    otpValue: String,
    counter: Int,
    onRemove: (Int) -> Unit = {},
) {
    var show by remember(otp) { mutableStateOf(true) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    val dismissState = rememberDismissState(
        confirmValueChange = {
            showRemoveConfirmation = true
            false
        }, positionalThreshold = { 150.dp.toPx() }
    )

    if (showRemoveConfirmation) {
        RemoveOtpConfirmationDialog(
            account = otp.issuer ?: "",
            onCancel = { showRemoveConfirmation = false },
            onConfirm = {
                onRemove(otp.keyId)
                show = false
                showRemoveConfirmation = false
            }
        )
    }

    AnimatedVisibility(
        show, exit = fadeOut(spring())
    ) {
        SwipeToDismiss(
            state = dismissState,
            directions = setOf(DismissDirection.EndToStart),
            modifier = Modifier,
            background = { DismissBackground(dismissState) },
            dismissContent = {
                OtpCard(
                    modifier = modifier,
                    otp = otp,
                    otpValue = otpValue,
                    counter = counter
                )
            }
        )
    }
}

@Composable
fun OtpCard(
    modifier: Modifier = Modifier,
    otp: Otp,
    otpValue: String,
    counter: Int,
) {
    Card(modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(otp.issuer)
                Text(
                    otpValue,
                    style = MaterialTheme.typography.displaySmall
                )
            }
            Spacer(modifier = modifier.weight(1f))
            Text("${counter}s")
        }
    }
}

@Composable
fun RemoveOtpConfirmationDialog(
    account: String,
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = "Remove $account") },
        text = { Text("Remove this account will remove your ability to generate codes, however, it will not turn off 2-factor authentication. This may prevent you from signing into your account.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun PreviewMainScreen() {
    MainScreen(uiState = MainUiState())
}