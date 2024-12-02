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

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.licel.samples.OtpPreferences
import java.io.InputStream
import java.io.OutputStream
object OtpPreferencesSerializer : Serializer<OtpPreferences> {
    override val defaultValue: OtpPreferences = OtpPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): OtpPreferences {
        try {
            return OtpPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }
    override suspend fun writeTo(t: OtpPreferences, output: OutputStream) = t.writeTo(output)
}
