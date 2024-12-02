package com.licel.samples.feature.pincode.navigation

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

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.licel.samples.feature.pincode.PinCodeScreen
import com.licel.samples.feature.pincode.PinCodeViewModel

const val pinCodeRoute = "pin_code"
const val verifyPinCodeSuccess = "verify_pin_code_success"

fun NavGraphBuilder.pinCodeScreen(
    navController: NavHostController,
    onSuccess: () -> Unit = {},
) {
    composable(pinCodeRoute) {
        val viewModel: PinCodeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val verifyPinCode by viewModel.verifyPinCode.collectAsStateWithLifecycle()

        val context = LocalContext.current
        val showToast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

        PinCodeScreen(
            uiState,
            pinCode = verifyPinCode,
            onCreatePinCode = viewModel::setPinCode,
            onVerify = { success ->
                if (success) {
                    showToast("Success!")
                    navController
                        .previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(verifyPinCodeSuccess, true)
                    onSuccess.invoke()
                } else {
                    showToast("Please try again!")
                }
            },
            onBackClick = navController::popBackStack
        )
    }
}