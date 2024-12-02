package com.licel.samples.utils

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

import org.apache.commons.codec.binary.Base32

import java.security.SecureRandom

object SecretGenerator {
    private const val DEFAULT_BITS = 160
    private val random = SecureRandom()

    fun generate(bits: Int = DEFAULT_BITS): ByteArray {
        require(bits > 0) { "Bits must be greater than or equal to 0" }
        val bytes = ByteArray(bits / java.lang.Byte.SIZE)
        random.nextBytes(bytes)
        val encoder = Base32()
        return encoder.encode(bytes)
    }

    fun generateSecretKey(): String? {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        val base32 = Base32()
        return base32.encodeToString(bytes)
    }
}