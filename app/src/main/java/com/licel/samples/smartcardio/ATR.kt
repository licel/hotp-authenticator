package com.licel.samples.smartcardio

import java.util.Arrays

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

/**
 * Representation of a smart card answer-to-reset
 */
class ATR(bytes: ByteArray) {
    private val mBytes: ByteArray
    private val mHistOffset = 0
    private val mHistLength = 0

    /**
     * Main constructor
     * @param bytes with raw ATR
     */
    init {
        mBytes = bytes.clone()
    }

    /** @return a copy of the raw ATR
     */
    val bytes: ByteArray
        get() = mBytes.clone()

    /** @return the historical bytes of the ATR
     */
    val historicalBytes: ByteArray
        get() {
            val b = ByteArray(mHistLength)
            System.arraycopy(mBytes, mHistOffset, b, 0, mHistLength)
            return b
        }

    override fun toString(): String {
        return "ATR: " + mBytes.size + " bytes"
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(mBytes)
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj is ATR == false) {
            return false
        }
        return Arrays.equals(mBytes, obj.bytes)
    }

    private fun parse() {}
}