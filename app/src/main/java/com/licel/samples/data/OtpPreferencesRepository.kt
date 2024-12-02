package com.licel.samples.data

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
import com.licel.samples.Otp
import com.licel.samples.OtpPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface OtpPreferencesRepository {
    suspend fun gets(): Flow<OtpPreferences>
    suspend fun add(otp: Otp)
    suspend fun delete(keyId: Int)
}

class OtpPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<OtpPreferences>,
) : OtpPreferencesRepository {

    override suspend fun add(otp: Otp) {
        dataStore.updateData {
            it.toBuilder()
                .addOtp(otp)
                .build()
        }
    }

    override suspend fun delete(keyId: Int) {
        dataStore.updateData {
            val index = it.otpList.indexOfFirst { it.keyId == keyId }

            it.toBuilder()
                .removeOtp(index)
                .build()
        }
    }

    override suspend fun gets() = dataStore.data
}