package com.licel.samples.feature.migration

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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ImportMigrationViewModel: ViewModel() {

    private val _uiState = MutableStateFlow(ImportMigrationUiState())
    val uiState: StateFlow<ImportMigrationUiState> = _uiState.asStateFlow()

    fun importMigration(barcode: String) {
        MigrationPayloadProcessor.parse(barcode)?.let { payload ->
            _uiState.update {
                it.copy(issuers = payload.otpParametersList.map { it.issuer })
            }
        }
    }
}