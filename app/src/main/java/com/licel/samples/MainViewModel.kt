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

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.licel.jcardsim.base.Simulator
import com.licel.jcardsim.utils.ByteUtil
import com.licel.samples.data.OtpPreferencesRepository
import com.licel.samples.feature.migration.MigrationPayloadProcessor
import com.licel.samples.smartcardio.CardSimulator
import com.licel.samples.smartcardio.ResponseAPDU
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base32
import timber.log.Timber
import java.io.File
import java.lang.Long
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.Any
import kotlin.Boolean
import kotlin.Byte
import kotlin.ByteArray
import kotlin.Exception
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.String
import kotlin.apply
import kotlin.let
import kotlin.longArrayOf
import kotlin.system.measureTimeMillis
import kotlin.with

@HiltViewModel
class MainViewModel @Inject constructor(
    private val simulator: CardSimulator,
    private val gmsBarcodeScanner: GmsBarcodeScanner,
    private val otpPreferencesRepository: OtpPreferencesRepository
) : ViewModel() {

    companion object {
        var SW_NO_ERROR = 0x9000
    }

    private val digitsPower = longArrayOf(1000000, 100000000)

    private val ALG_HMAC_SHA1 = 24
    private val ALG_HMAC_SHA_256 = 25
    private val ALG_HMAC_SHA_384 = 26
    private val ALG_HMAC_SHA_512 = 27

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var otpValue: MutableMap<Int, String> = mutableMapOf()

    init {
        loadOtpList()
    }

    private fun loadOtpList() {
        viewModelScope.launch {
            val otpList = otpPreferencesRepository.gets().first().otpList
            _uiState.update { it.copy(otpParams = otpList) }
            calculateOTPAll()
        }
    }

    fun initiateScanner() {
        gmsBarcodeScanner
            .startScan()
            .addOnSuccessListener(::scanQrCode)
            .addOnFailureListener { e -> Timber.d("initiateScanner: " + e.printStackTrace()) }
    }

    private fun scanQrCode(barcode: Barcode) {
        val barcodeString = barcode.rawValue
        if (barcodeString?.startsWith("otpauth-migration") == true) {
            if (_uiState.value.migrationQrCode.isNullOrEmpty()) {
                _uiState.update { it.copy(migrationQrCode = barcodeString) }
            }
        } else if (barcodeString?.startsWith("otpauth") == true) {
            importOtp(barcode)
        }
    }

    fun importMigration(barcode: String, indexes: List<Int>) {
        val payload = MigrationPayloadProcessor.parse(barcode)
        payload?.otpParametersList?.let {
            if (it.isNotEmpty()) {
                it.forEachIndexed { index, it ->
                    if (indexes.contains(index)) {
                        try {
                            val response = importHmacKey(it.secret.toByteArray())
                            if (response.sW == SW_NO_ERROR) {
                                save(buildOtpObject(response.data as Byte, it))
                            } else {
                                _uiState.update { it.copy(isSwError = true, sw = response.sW) }
                            }
                        } catch (e: IllegalArgumentException) {
                            _uiState.update { it.copy(isQrCodeValid = false) }
                        }
                    }
                }
            }
        }
    }

    private fun importOtp(barcode: Barcode) {
        try {
            val uri = Uri.parse(barcode.rawValue)
            val response = importHmacKeyFromUri(uri)
            if (response.sW == SW_NO_ERROR) {
                save(buildOtpObject(response.data[0], uri))
            } else {
                _uiState.update { it.copy(isSwError = true, sw = response.sW) }
            }
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(isQrCodeValid = false) }
        }
    }

    fun importOtp(barcode: String) {
        try {
            val uri = Uri.parse(barcode)
            val response = importHmacKeyFromUri(uri)
            if (response.sW == SW_NO_ERROR) {
                save(buildOtpObject(response.data[0], uri))
            } else {
                _uiState.update { it.copy(isSwError = true, sw = response.sW) }
            }
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(isQrCodeValid = false) }
        }
    }

    private fun isDuplicate(otp: Otp): Boolean {
        _uiState.value.otpParams.firstOrNull {
            it.digits == otp.digits
                    && it.algorithm == otp.algorithm
                    && it.issuer == otp.issuer
                    && it.period == otp.period
                    && it.type == otp.type
        }?.let {
            val otp1 = calculateOTP(it)
            val otp2 = calculateOTP(otp)
            val isDuplicated = otp1 == otp2
            _uiState.update { it.copy(isQrCodeDuplicated = isDuplicated) }
            return isDuplicated
        }
        return false
    }

    private fun save(otp: Otp) {
        if (!isDuplicate(otp)) {
            viewModelScope.launch {
                calculateOTP(otp)
                otpPreferencesRepository.add(otp)
            }.apply {
                val newList = _uiState.value.otpParams.toMutableList()
                newList.add(otp)
                _uiState.update { it.copy(otpParams = newList) }
            }
        }
    }

    private fun buildOtpObject(keyId: Byte, barcodeUri: Uri): Otp {
        with(barcodeUri) {
            val digits = getQueryParameter("digits")?.toInt() ?: 6
            val issuer = path?.removePrefix("/") ?: getQueryParameter("issuer") ?: ""
            if (issuer.isNullOrEmpty() || digits !in 6..8) {
                throw IllegalArgumentException("Cannot interpret QR code")
            }

            return Otp.newBuilder()
                .setType(host)
                .setKeyId(keyId.toInt())
                .setDigits(digits)
                .setPeriod(getQueryParameter("period")?.toInt() ?: 30)
                .setAlgorithm(getAlgorithm(getQueryParameter("algorithm")))
                .setIssuer(issuer)
                .build()
        }
    }

    private fun buildOtpObject(keyId: Byte, otpParameters: MigrationPayload.OtpParameters): Otp {
        with(otpParameters) {
            return Otp.newBuilder()
                .setType(if (typeValue == 1) "HOTP" else "TOTP")
                .setKeyId(keyId.toInt())
                .setDigits(6)
                .setPeriod(30)
                .setAlgorithm(getAlgorithm("SHA1"))
                .setIssuer(issuer)
                .build()
        }
    }

    private fun importHmacKeyFromUri(barcodeUri: Uri?): ResponseAPDU{
        val secret = barcodeUri?.getQueryParameter("secret")
        return importHmacKey(Base32().decode(secret))
    }

    private fun importHmacKey(secret: ByteArray): ResponseAPDU {
        val result: ByteArray =
            simulator.transmitCommand(commandAPDU(0x00, 0x02, 0x00, 0x00, secret))
        ByteUtil.requireSW(result, SW_NO_ERROR)

        return ResponseAPDU(result)
    }

    fun deleteHmacKey(keyId: Int) {

        val result: ByteArray =
            simulator.transmitCommand(commandAPDU(0x00, 0x03, 0x00, keyId.toByte()))
        ByteUtil.requireSW(result, SW_NO_ERROR);

        viewModelScope.launch {
            otpPreferencesRepository.delete(keyId)
        }.apply {
            val otpList = _uiState.value.otpParams.toMutableList()
            otpList.removeAt(otpList.indexOfFirst { it.keyId == keyId })
            _uiState.update { it.copy(otpParams = otpList) }
        }
    }

    fun calculateOTPAll() {
        _uiState.value.otpParams.forEach {
            viewModelScope.launch {
                calculateOTP(it)
            }
            _uiState.update { it.copy(otpValue = otpValue) }
        }
    }

    private fun calculateOTP(otp: Otp) {
        val keyId = otp.keyId
        val digits = otp.digits
        val index = if (digits == 6) 0 else 1
        val power = digitsPower[index]
        val time = (System.currentTimeMillis() / 1000) / otp.period
        val counter =
            ByteBuffer.allocate(Long.SIZE / java.lang.Byte.SIZE)
                .putLong(time).array()
        val calculateCommand = commandAPDU(0x00, 0x04, otp.algorithm.toByte(), otp.keyId.toByte(), counter)
        val result = simulator.transmitCommand(calculateCommand)
        val response = ResponseAPDU(result)

        if( response.sW != SW_NO_ERROR ){
            _uiState.update { it.copy(isSwError = true, sw = response.sW) }
        }
        else {
            val otpValInt = (fromByteArray(response.data) % power)
            otpValue[keyId] = otpValInt.toString().padStart(digits, '0')
        }
    }

    fun updateCounter(): Int {
        if (_uiState.value.otpValue.isNullOrEmpty()) calculateOTPAll()
        val second = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toInt()
        val counter30 = 30 - (second % 30)
        val counter60 = 60 - (second % 60)
        _uiState.update { it.copy(counter30 = counter30, counter60 = counter60) }
        return counter30
    }

    fun resetQrCodeStatus() =
        _uiState.update {
            it.copy(
                isQrCodeValid = true,
                isQrCodeDuplicated = false,
                migrationQrCode = null,
                isFoundApplet = true,
                isSwError = false,
                sw = null
            )
        }

    private fun getAlgorithm(algorithm: String?): Int {
        when (algorithm) {
            "SHA1" -> return ALG_HMAC_SHA1
            "SHA256" -> return ALG_HMAC_SHA_256
            "SHA384" -> return ALG_HMAC_SHA_384
            "SHA512" -> return ALG_HMAC_SHA_512
            else -> ALG_HMAC_SHA1
        }
        return ALG_HMAC_SHA1
    }
}

fun commandAPDU(
    cla: Byte,
    ins: Byte,
    p1: Byte,
    p2: Byte,
    data: ByteArray,
    le: Byte = 0x00,
): ByteArray {
    val commandApdu = ByteArray(6 + data.size)
    commandApdu[0] = cla
    commandApdu[1] = ins
    commandApdu[2] = p1
    commandApdu[3] = p2
    commandApdu[4] = ((data.size) and 0x0FF).toByte() // Lc
    System.arraycopy(data, 0, commandApdu, 5, data.size) // data
    commandApdu[commandApdu.size - 1] = le // Le
    return commandApdu
}

fun commandAPDU(cla: Byte, ins: Byte, p1: Byte, p2: Byte): ByteArray {
    val commandApdu = ByteArray(4)
    commandApdu[0] = cla
    commandApdu[1] = ins
    commandApdu[2] = p1
    commandApdu[3] = p2
    return commandApdu
}

fun getSW(byteArray: ByteArray): Int {
    val size = byteArray.size
    return (byteArray[size - 2].toInt() and 0xFF) shl 8 or
            (byteArray[size - 1].toInt() and 0xFF)
}

fun fromByteArray(bytes: ByteArray): Int {
    return bytes[0].toInt() and 0x7F shl 24 or
            (bytes[1].toInt() and 0xFF shl 16) or
            (bytes[2].toInt() and 0xFF shl 8) or
            (bytes[3].toInt() and 0xFF shl 0)
}