package com.licel.samples.feature.pincode

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
//

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.licel.samples.core.ui.BackButton
import com.licel.samples.core.ui.BiometricPrompt
import com.licel.samples.feature.pincode.ui.NumPad

@Composable
fun PinCodeScreen(
    uiState: PinCodeUiState,
    modifier: Modifier = Modifier,
    pinCode: String = "",
    onCreatePinCode: (String) -> Unit = {},
    onVerify: (Boolean) -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    var state by remember(uiState) { mutableStateOf(uiState.pinCodeState) }
    var showBiometricPrompt by remember(uiState) { mutableStateOf(state == PinCodeState.VERIFY) }
    var showBiometricButton by remember(uiState) { mutableStateOf(state == PinCodeState.VERIFY) }
    var verifyPinCode by remember(uiState) { mutableStateOf(pinCode) }
    var currentPinCode by remember { mutableStateOf("") }

    val onClick: (String) -> Unit = {
        if (currentPinCode.length < 6) {
            currentPinCode += it
        }
        if (currentPinCode.length == 6) {
            when (state) {
                PinCodeState.CREATE -> {
                    verifyPinCode = currentPinCode
                    state = PinCodeState.CONFIRM
                }

                PinCodeState.CONFIRM -> {
                    val isValid = verifyPinCode == currentPinCode
                    if (isValid) {
                        onCreatePinCode.invoke(currentPinCode)
                    }
                    onVerify.invoke(isValid)
                }

                PinCodeState.VERIFY -> {
                    onVerify.invoke(verifyPinCode == currentPinCode)
                }
            }
            currentPinCode = ""
        }
    }
    val onBiometricClick: () -> Unit = {
        showBiometricPrompt = true
    }
    val onDelClick: () -> Unit = {
        currentPinCode = currentPinCode.dropLast(1)
    }

    if (showBiometricPrompt) {
        BiometricPrompt(
            negativeButton = "Use PIN",
            onAuthenticated = { onVerify.invoke(true) },
            onCancel = { showBiometricPrompt = false },
            onAuthenticationError = { showBiometricButton = false }
        )
    }
    if (!showBiometricButton) {
        BackButton(onClick = onBackClick)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
    ) {
        Text(
            text = when (state) {
                PinCodeState.CREATE -> "Create pincode"
                PinCodeState.CONFIRM -> "Verify pincode"
                PinCodeState.VERIFY -> "Enter pincode"
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black,
            modifier = modifier
                .padding(horizontal = 48.dp)
                .padding(top = 80.dp),
        )
        Text(
            text = "",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = modifier
                .padding(horizontal = 48.dp)
                .padding(top = 20.dp),
        )
        Dots(
            pinCode = currentPinCode,
            modifier = modifier.padding(vertical = 50.dp)
        )
        NumPad(
            onClick = onClick,
            showBiometric = showBiometricButton,
            onBiometricClick = onBiometricClick,
            onDelClick = onDelClick,
        )
    }
}

@Composable
fun Dots(
    pinCode: String,
    modifier: Modifier = Modifier,
) {
    Row(horizontalArrangement = Arrangement.Center, modifier = modifier) {
        repeat(6) { index ->
            Dot(isSelected = pinCode.length <= index)
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun Dot(isSelected: Boolean = false) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.LightGray else Color.DarkGray)
    )
}

@Preview
@Composable
fun PreviewPinCodeScreen() {
    PinCodeScreen(PinCodeUiState())
}