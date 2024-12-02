package com.licel.samples

// from LICEL CORPORATION.

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

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.licel.samples.feature.migration.nativation.importMigrationScreen
import com.licel.samples.feature.migration.nativation.navigateToImportMigrationScreen
import com.licel.samples.feature.pincode.navigation.pinCodeRoute
import com.licel.samples.feature.pincode.navigation.pinCodeScreen
import com.licel.samples.utils.SecretGenerator
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

const val mainRoute = "main"

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = pinCodeRoute,
    ) {
        pinCodeScreen(
            navController,
            onSuccess = navController::navigateToMainScreen
        )
        mainScreen(navController)
        importMigrationScreen(navController)
    }
}

fun NavController.navigateToMainScreen() = navigate(mainRoute)

fun NavGraphBuilder.mainScreen(
    navController: NavHostController
) {
    composable(route = mainRoute) {
        val qr = it.savedStateHandle.get<String>("qr")
        val indexes = it.savedStateHandle.get<List<Int>>("indexes")

        val context = LocalContext.current
        val viewModel: MainViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            while (true) {
                val counter = viewModel.updateCounter()
                if (counter == 30) {
                    viewModel.calculateOTPAll()
                }
                delay(1.seconds)
            }
        }

        LaunchedEffect(uiState.isFoundApplet) {
            if (!uiState.isFoundApplet) {
                (context as? Activity)?.finishAndRemoveTask()
            }
        }

        LaunchedEffect(uiState.migrationQrCode) {
            uiState.migrationQrCode?.let {
                navController.navigateToImportMigrationScreen(it)
                viewModel.resetQrCodeStatus()
            }
        }

        LaunchedEffect(qr, indexes) {
            if (qr?.isNotEmpty() == true && indexes?.isNotEmpty() == true) {
                viewModel.importMigration(qr, indexes)
            }
        }

        MainScreen(
            uiState = uiState,
            onFloatingButtonQRScanClick = viewModel::initiateScanner,
            onFloatingButtonSimulateClick = {
                viewModel.importOtp(
                    "otpauth://totp/Licel:vtee@licelus.com?secret=${SecretGenerator.generateSecretKey()}&issuer=Licel&algorithm=SHA1&period=30"
                )
            },
            onDismissDialog = viewModel::resetQrCodeStatus,
            onRemoveOtp = viewModel::deleteHmacKey,
            onBackButtonPress = {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                }.let { ContextCompat.startActivity(context, it, null) }
            }
        )
    }
}