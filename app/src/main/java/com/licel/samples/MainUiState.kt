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

data class MainUiState(
    val otpParams: List<Otp> = emptyList(),
    var otpValue: MutableMap<Int, String> = mutableMapOf(),
    val counter30: Int = 0,
    val counter60: Int = 0,
    val isLoading: Boolean = false,
    val isQrCodeValid: Boolean = true,
    val isFoundApplet: Boolean = true,
    val isSwError: Boolean = false,
    val sw: Int? = null,
    val isQrCodeDuplicated: Boolean = false,
    val migrationQrCode: String? = null
)