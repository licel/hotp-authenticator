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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.licel.samples.core.datastore.PreferencesKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinCodeViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    val uiState: StateFlow<PinCodeUiState> = dataStore.data.map {
        val state = if (it[PreferencesKeys.PIN_CODE].isNullOrEmpty()) {
            PinCodeState.CREATE
        } else {
            PinCodeState.VERIFY
        }
        PinCodeUiState(state)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PinCodeUiState())

    val verifyPinCode: StateFlow<String> = dataStore.data.map {
        it[PreferencesKeys.PIN_CODE] ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    fun setPinCode(pinCode: String) {
        viewModelScope.launch {
            dataStore.edit {
                it[PreferencesKeys.PIN_CODE] = pinCode
            }
        }
    }
}