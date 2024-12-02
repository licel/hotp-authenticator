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

import android.net.Uri
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.licel.jcardsim.utils.AIDUtil
import com.licel.samples.applet.OTPApplet
import com.licel.samples.smartcardio.CardSimulator
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import javacard.security.Signature
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class MainViewModelTest {

    companion object {
        private const val ISSUER = "Licel"
        private const val ACCOUNT_NAME = "teerapong@licelus.com"
        private const val TYPE = "totp"
        private const val SECRET = "Q4TU56JLS6OZMYDHMX6AC74OFLFFCK7Z"
        private const val DIGITS = 6
        private const val ALGORITHM = "SHA1"
        private const val PERIOD = 30

        private lateinit var simulator: CardSimulator
        private lateinit var gmsBarcodeScanner: GmsBarcodeScanner
        private lateinit var viewModel: MainViewModel
        private lateinit var mockUri: Uri

        @BeforeClass
        @JvmStatic
        fun setup() {
            val barcodeUrl =
                "otpauth://$TYPE/$ISSUER:$ACCOUNT_NAME?secret=$SECRET&issuer=$ISSUER&digits=$DIGITS&algorithm=$ALGORITHM&period=$PERIOD"

            gmsBarcodeScanner = mockk()
            val mockBarcode = mockk<Barcode>()
            every { mockBarcode.rawValue } returns barcodeUrl
            val mockTaskBarcode = mockk<Task<Barcode>>()
            every { mockTaskBarcode.result } returns mockBarcode

            val slot = slot<OnSuccessListener<Barcode>>()
            every {
                gmsBarcodeScanner.startScan()
                    .addOnSuccessListener(capture(slot))
                    .addOnFailureListener(any())
            } answers {
                slot.captured.onSuccess(mockBarcode)
                mockTaskBarcode
            }

            mockkStatic(Uri::class)
            mockUri = mockk()
            every { mockUri.host } returns TYPE
            every { mockUri.path } returns "$ISSUER:$ACCOUNT_NAME"
            every { mockUri.getQueryParameter("secret") } returns SECRET
            every { mockUri.getQueryParameter("issuer") } returns ISSUER
            every { mockUri.getQueryParameter("digits") } returns "$DIGITS"
            every { mockUri.getQueryParameter("algorithm") } returns ALGORITHM
            every { mockUri.getQueryParameter("period") } returns "$PERIOD"
            every { Uri.parse(barcodeUrl) } returns mockUri
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            unmockkStatic(Uri::class)
        }
    }

    @Before
    fun setUp() {
        val aid = AIDUtil.create("F000000001")
        simulator = CardSimulator()
        simulator.installApplet(aid, OTPApplet::class.java)
        simulator.selectApplet(aid)
        viewModel = MainViewModel(simulator, gmsBarcodeScanner)

        every { mockUri.path } returns "$ISSUER:$ACCOUNT_NAME"
        every { mockUri.getQueryParameter("issuer") } returns ISSUER
    }

    @Test
    fun scanQRCode_algorithmSha1() {
        every { mockUri.getQueryParameter("algorithm") } returns "SHA1"

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.algorithm, Signature.ALG_HMAC_SHA1)
    }

    @Test
    fun scanQRCode_algorithmSha256() {
        every { mockUri.getQueryParameter("algorithm") } returns "SHA256"

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.algorithm, Signature.ALG_HMAC_SHA_256)
    }

    @Test
    fun scanQRCode_algorithmSha384() {
        every { mockUri.getQueryParameter("algorithm") } returns "SHA384"

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.algorithm, Signature.ALG_HMAC_SHA_384)
    }

    @Test
    fun scanQRCode_algorithmSha512() {
        every { mockUri.getQueryParameter("algorithm") } returns "SHA512"

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.algorithm, Signature.ALG_HMAC_SHA_512)
    }

    @Test
    fun scanQRCode_noAlgorithm() {
        every { mockUri.getQueryParameter("algorithm") } returns ""

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.algorithm, Signature.ALG_HMAC_SHA1)
    }

    @Test
    fun scanQRCode_6digits() {
        every { mockUri.getQueryParameter("digits") } returns "6"

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.digits, 6)
        assertNotNull(otp.value)
        assertEquals(otp.value?.count(), 6)
    }

    @Test
    fun scanQRCode_8digits() {
        every { mockUri.getQueryParameter("digits") } returns "8"

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.digits, 8)
        assertNotNull(otp.value)
        assertEquals(otp.value?.count(), 8)
    }

    @Test
    fun scanQRCode_incorrectDigits() {
        every { mockUri.getQueryParameter("digits") } returns null

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.digits, 6)
        assertNotNull(otp.value)
        assertEquals(otp.value?.count(), 6)
    }

    @Test
    fun scanQRCode_noIssuerPrefix() {
        every { mockUri.path } returns ""

        viewModel.initiateScanner()

        assertEquals(viewModel.uiState.value.isQrCodeValid, false)
    }

    @Test
    fun scanQRCode_noIssuerParams() {
        every { mockUri.getQueryParameter("issuer") } returns null

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.issuer, "$ISSUER:$ACCOUNT_NAME")
    }

    @Test
    fun scanQRCode_noIssuerPrefixAndParams() {
        every { mockUri.path } returns ""
        every { mockUri.getQueryParameter("issuer") } returns null

        viewModel.initiateScanner()

        assertEquals(viewModel.uiState.value.isQrCodeValid, false)
    }

    @Test
    fun scanQRCode_resetQrCodeStatus() {
        every { mockUri.path } returns ""

        viewModel.initiateScanner()

        assertEquals(viewModel.uiState.value.isQrCodeValid, false)

        viewModel.resetQrCodeStatus()

        assertEquals(viewModel.uiState.value.isQrCodeValid, true)
    }

    @Test
    fun scanQRCode_withDefaultOptions() {
        every { mockUri.getQueryParameter("issuer") } returns null
        every { mockUri.getQueryParameter("digits") } returns null
        every { mockUri.getQueryParameter("algorithm") } returns null
        every { mockUri.getQueryParameter("period") } returns null

        viewModel.initiateScanner()

        val otp = viewModel.uiState.value.otpList.first()
        assertEquals(otp.algorithm, Signature.ALG_HMAC_SHA1)
        assertEquals(otp.digits, 6)
        assertEquals(otp.period, 30)
        assertEquals(otp.issuer, "$ISSUER:$ACCOUNT_NAME")
    }

    @Test
    fun counter() {
        viewModel.updateCounter()

        val second = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toInt()
        val counter = PERIOD - (second % PERIOD)

        assertEquals(viewModel.uiState.value.counter, counter)
    }

    @Test
    fun removeOtp_success() {
        viewModel.initiateScanner()
        viewModel.deleteHmacKey(0)

        val otpList = viewModel.uiState.value.otpList
        assertEquals(otpList.count(), 0)
    }

    @Test(expected = AssertionError::class)
    fun removeOtp_notFound() = viewModel.deleteHmacKey(0)
}