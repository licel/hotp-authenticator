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

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class CapUtils {
    companion object {

        private val capList = arrayOf(
            "cap/java/lang/javacard/lang.cap",
            "cap/com/licel/jcardsimr/nativeinterface/javacard/nativeinterface.cap",
            "cap/com/licel/jcardsimr/io/javacard/io.cap",
            "cap/javacard/framework/javacard/framework.cap",
            "cap/javacard/security/javacard/security.cap",
            "cap/javacardx/crypto/javacard/crypto.cap",
            "cap/com/licel/jcardsimr/javacard/jcardsimr.cap",
        )
        private const val otpCapFile = "cap/com/licel/jcardsimr/tests/otp/javacard/otp.cap"

        fun getEEPRomPath(context: Context): String = File(context.filesDir, "eeprom").absolutePath

        @Throws(IOException::class)
        fun getJCardSimRCapFiles(context: Context): List<ByteArray> {
            val capArrayList: MutableList<ByteArray> = ArrayList()
            capList.forEach { capArrayList.add(readAsset(context, it)!!) }
            return capArrayList
        }
        fun getOtpCapFile(context: Context): ByteArray = readAsset(context, otpCapFile)!!

        @Throws(IOException::class)
        fun readAsset(context: Context, filename: String?): ByteArray? {
            val `in` = context.resources.assets.open(filename!!)
            return `in`.use { `in` ->
                readAllBytes(`in`)
            }
        }
        @Throws(IOException::class)
        fun readAllBytes(`in`: InputStream?): ByteArray? {
            `in`?.let {
                val out = ByteArrayOutputStream()
                copyAllBytes(it, out)
                return out.toByteArray()
            }
            return null
        }
        @Throws(IOException::class)
        fun copyAllBytes(`in`: InputStream, out: OutputStream): Int {
            var byteCount = 0
            val buffer = ByteArray(4096)
            while (true) {
                val read = `in`.read(buffer)
                if (read == -1) {
                    break
                }
                out.write(buffer, 0, read)
                byteCount += read
            }
            return byteCount
        }
    }
}