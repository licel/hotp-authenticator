package com.licel.samples.feature.migration.nativation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.licel.samples.feature.migration.ImportMigrationScreen
import com.licel.samples.feature.migration.ImportMigrationViewModel

private const val importMigrationRoute = "import_migration_route"
private const val importMigrationQrParam = "qr"

fun NavController.navigateToImportMigrationScreen(qr: String) =
    navigate("$importMigrationRoute?$importMigrationQrParam=$qr")

fun NavGraphBuilder.importMigrationScreen(
    navController: NavController
) {
    composable(
        route = "$importMigrationRoute?$importMigrationQrParam={$importMigrationQrParam}",
        arguments = listOf(
            navArgument(importMigrationQrParam) { type = NavType.StringType }
        )
    ) {
        val viewModel: ImportMigrationViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val importMigrationQr = it.arguments?.getString(importMigrationQrParam)
        importMigrationQr?.let { viewModel.importMigration(it) }

        ImportMigrationScreen(
            uiState,
            onSubmitButtonClick = { indexes ->
                navController
                    .previousBackStackEntry
                    ?.savedStateHandle?.run {
                        set("qr", importMigrationQr)
                        set("indexes", indexes)
                    }
                navController.popBackStack()
            }
        )
    }
}