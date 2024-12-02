package com.licel.samples.core.ui

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

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricPrompt(
    title: String = "Authentication",
    description: String = "Please verify your credential",
    negativeButton: String = "Cancel",
    onAuthenticated: () -> Unit = {},
    onCancel: () -> Unit = {},
    onAuthenticationError: () -> Unit = {},
    onAuthenticationSoftError: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    val executor = remember { ContextCompat.getMainExecutor(activity) }
    val biometricManager = remember { BiometricManager.from(context) }
    val callback = remember {
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }

            override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errCode, errString)

                if (errCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancel()
                } else {
                    onAuthenticationError()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onAuthenticationSoftError()
            }
        }
    }

    val promptInfo = remember {
        val secureOption = bestSecureOption(biometricManager)

        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setDescription(description)
            .apply {
                if ((secureOption and BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 0) {
                    setNegativeButtonText(negativeButton)
                }
            }.setAllowedAuthenticators(secureOption)
            .build()
    }

    val biometricPrompt = remember { BiometricPrompt(activity, executor, callback) }

    DisposableEffect(biometricPrompt) {
        biometricPrompt.authenticate(promptInfo)

        onDispose {
            biometricPrompt.cancelAuthentication()
        }
    }
}

private fun bestSecureOption(biometricManager: BiometricManager): Int {
    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS,
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> return BiometricManager.Authenticators.BIOMETRIC_STRONG
    }
    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
        BiometricManager.BIOMETRIC_SUCCESS,
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> return BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
        BiometricManager.BIOMETRIC_SUCCESS,
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> return BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
    return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_WEAK
    } else {
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}