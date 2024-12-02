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

import android.net.Uri
import android.util.Base64
import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.InvalidProtocolBufferException
import com.licel.samples.MigrationPayload

object MigrationPayloadProcessor {
    private const val MIGRATION_AUTHORITY = "offline"
    private const val MIGRATION_SCHEME = "otpauth-migration"
    private const val PAYLOAD_QUERY_PARAM = "data"

    fun parse(str: String?): MigrationPayload? {
        if (str == null) return null

        val parse = Uri.parse(str)
        if (MIGRATION_SCHEME != parse.scheme) {
            throw InvalidProtocolBufferException("Wrong scheme for migration URI")
        }
        if (MIGRATION_AUTHORITY != parse.authority) {
            throw InvalidProtocolBufferException("Wrong authority in migration URI")
        }
        val data = parse.getQueryParameter(PAYLOAD_QUERY_PARAM)
            ?: throw InvalidProtocolBufferException("Missing data parameter in migration URI")
        try {
            val parseFrom = MigrationPayload.parseFrom(
                Base64.decode(data, 0),
                ExtensionRegistryLite.newInstance()
            )
//            if (!parseFrom.hasVersion()) {
//                throw InvalidProtocolBufferException("Missing version in migration payload")
//            }
            if (parseFrom.version > 1) {
                throw UnsupportedMigrationVersionException(parseFrom.version)
            }
            if (parseFrom.batchSize > 0
                && parseFrom.batchIndex >= 0
                && parseFrom.batchSize > parseFrom.batchIndex) {
                return parseFrom
            }
            throw InvalidProtocolBufferException("Invalid batch parameters")
        } catch (e: IllegalArgumentException) {
            throw InvalidProtocolBufferException("Invalid base64: " + e.message)
        }
    }
}

class UnsupportedMigrationVersionException(i: Int) : Exception(
    String.format(
        "Unsupported migration payload version: found %d, max supported version %d",
        Integer.valueOf(i),
        1
    )
)