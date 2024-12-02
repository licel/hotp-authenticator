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

import com.licel.jcardsim.bouncycastle.util.Arrays
import com.licel.jcardsim.bouncycastle.util.encoders.Hex
import com.licel.jcardsim.utils.AIDUtil
import com.licel.samples.applet.OTPApplet
import com.licel.samples.smartcardio.CardSimulator
import com.licel.samples.smartcardio.ResponseAPDU
import com.licel.samples.utils.SecretGenerator
import javacard.framework.AID
import javacard.framework.ISO7816
import javacard.security.Signature
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.nio.ByteBuffer
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset


class OTPAppletUnitTest {

    private val hmacSha1Test = arrayOf(
        // RFC - 2202 HMAC-SHA1 test case 1
        arrayOf(
            "0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B",
            "4869205468657265",
            "B617318655057264E28BC0B6FB378C8EF146BE00"
        ),
        // RFC - 2202 HMAC-SHA1 test case 2
        arrayOf(
            "4A656665",
            "7768617420646F2079612077616E7420666F72206E6F7468696E673F",
            "EFFCDF6AE5EB2FA2D27416D5F184DF9C259A7C79"
        ),
        // RFC - 2202 HMAC-SHA1 test case 3
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD",
            "125D7342B9AC11CD91A39AF48AA17B4F63F175D3"
        ),
        // RFC - 2202 HMAC-SHA1 test case 4
        arrayOf(
            "0102030405060708090A0B0C0D0E0F10111213141516171819",
            "CDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCD",
            "4C9007F4026250C6BC8414F9BF50C86C2D7235DA"
        ),
        // RFC - 2202 HMAC-SHA1 test case 5
        arrayOf(
            "0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C",
            "546573742057697468205472756E636174696F6E",
            "4C1A03424B55E07FE7F27BE1D58BB9324A9A5A04"
        ),
        // RFC - 2202 HMAC-SHA1 test case 6
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "54657374205573696E67204C6172676572205468616E20426C6F636B2D53697A65204B6579202D2048617368204B6579204669727374",
            "AA4AE5E15272D00E95705637CE8A3B55ED402112"
        ),
        // RFC - 2202 HMAC-SHA1 test case 7
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "54657374205573696E67204C6172676572205468616E20426C6F636B2D53697A65204B657920616E64204C6172676572205468616E204F6E6520426C6F636B2D53697A652044617461",
            "E8E99D0F45237D786D6BBAA7965C7808BBFF1A91"
        )
    )
    private val hmacSha256Test = arrayOf(
        // RFC - 4231 HMAC-SHA-256 test case 1
        arrayOf(
            "0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B",
            "4869205468657265",
            "B0344C61D8DB38535CA8AFCEAF0BF12B881DC200C9833DA726E9376C2E32CFF7"
        ),
        // RFC - 4231 HMAC-SHA-256 test case 2
        arrayOf(
            "4A656665",
            "7768617420646F2079612077616E7420666F72206E6F7468696E673F",
            "5BDCC146BF60754E6A042426089575C75A003F089D2739839DEC58B964EC3843"
        ),
        // RFC - 4231 HMAC-SHA-256 test case 3
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD",
            "773EA91E36800E46854DB8EBD09181A72959098B3EF8C122D9635514CED565FE"
        ),
        // RFC - 4231 HMAC-SHA-256 test case 4
        arrayOf(
            "0102030405060708090A0B0C0D0E0F10111213141516171819",
            "CDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCD",
            "82558A389A443C0EA4CC819899F2083A85F0FAA3E578F8077A2E3FF46729665B"
        ),
        // RFC - 4231 HMAC-SHA-256 test case 6
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "54657374205573696E67204C6172676572205468616E20426C6F636B2D53697A65204B6579202D2048617368204B6579204669727374",
            "60E431591EE0B67F0D8A26AACBF5B77F8E0BC6213728C5140546040F0EE37F54"
        ),
        // RFC - 4231 HMAC-SHA-256 test case 7
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "5468697320697320612074657374207573696E672061206C6172676572207468616E20626C6F636B2D73697A65206B657920616E642061206C6172676572207468616E20626C6F636B2D73697A6520646174612E20546865206B6579206E6565647320746F20626520686173686564206265666F7265206265696E6720757365642062792074686520484D414320616C676F726974686D2E",
            "9B09FFA71B942FCB27635FBCD5B0E944BFDC63644F0713938A7F51535C3A35E2"
        ),
    )
    private val hmacSha384Test  = arrayOf(
        // RFC - 4231 HMAC-SHA-384 test case 1
        arrayOf(
            "0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B",
            "4869205468657265",
            "AFD03944D84895626B0825F4AB46907F15F9DADBE4101EC682AA034C7CEBC59CFAEA9EA9076EDE7F4AF152E8B2FA9CB6"
        ),
        // RFC - 4231 HMAC-SHA-384 test case 2
        arrayOf(
            "4A656665",
            "7768617420646F2079612077616E7420666F72206E6F7468696E673F",
            "AF45D2E376484031617F78D2B58A6B1B9C7EF464F5A01B47E42EC3736322445E8E2240CA5E69E2C78B3239ECFAB21649"
        ),
        // RFC - 4231 HMAC-SHA-384 test case 3
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD",
            "88062608D3E6AD8A0AA2ACE014C8A86F0AA635D947AC9FEBE83EF4E55966144B2A5AB39DC13814B94E3AB6E101A34F27"
        ),
        // RFC - 4231 HMAC-SHA-384 test case 4
        arrayOf(
            "0102030405060708090a0b0c0d0e0f10111213141516171819",
            "CDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCD",
            "3E8A69B7783C25851933AB6290AF6CA77A9981480850009CC5577C6E1F573B4E6801DD23C4A7D679CCF8A386C674CFFB"
        ),
        // RFC - 4231 HMAC-SHA-384 test case 6
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "54657374205573696E67204C6172676572205468616E20426C6F636B2D53697A65204B6579202D2048617368204B6579204669727374",
            "4ECE084485813E9088D2C63A041BC5B44F9EF1012A2B588F3CD11F05033AC4C60C2EF6AB4030FE8296248DF163F44952"
        ),
        // RFC - 4231 HMAC-SHA-384 test case 7
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "5468697320697320612074657374207573696E672061206C6172676572207468616E20626C6F636B2D73697A65206B657920616E642061206C6172676572207468616E20626C6F636B2D73697A6520646174612E20546865206B6579206E6565647320746F20626520686173686564206265666F7265206265696E6720757365642062792074686520484D414320616C676F726974686D2E",
            "6617178E941F020D351E2F254E8FD32C602420FEB0B8FB9ADCCEBB82461E99C5A678CC31E799176D3860E6110C46523E"
        ),
    )
    private val hmacSha512Test = arrayOf(
        // RFC - 4231 HMAC-SHA-512 test case 1
        arrayOf(
            "0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B",
            "4869205468657265",
            "87AA7CDEA5EF619D4FF0B4241A1D6CB02379F4E2CE4EC2787AD0B30545E17CDEDAA833B7D6B8A702038B274EAEA3F4E4BE9D914EEB61F1702E696C203A126854"
        ),
        // RFC - 4231 HMAC-SHA-512 test case 2
        arrayOf(
            "4A656665",
            "7768617420646F2079612077616E7420666F72206E6F7468696E673F",
            "164B7A7BFCF819E2E395FBE73B56E0A387BD64222E831FD610270CD7EA2505549758BF75C05A994A6D034F65F8F0E6FDCAEAB1A34D4A6B4B636E070A38BCE737"
        ),
        // RFC - 4231 HMAC-SHA-512 test case 3
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD",
            "FA73B0089D56A284EFB0F0756C890BE9B1B5DBDD8EE81A3655F83E33B2279D39BF3E848279A722C806B485A47E67C807B946A337BEE8942674278859E13292FB"
        ),
        // RFC - 4231 HMAC-SHA-512 test case 4
        arrayOf(
            "0102030405060708090a0b0c0d0e0f10111213141516171819",
            "CDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCDCD",
            "B0BA465637458C6990E5A8C5F61D4AF7E576D97FF94B872DE76F8050361EE3DBA91CA5C11AA25EB4D679275CC5788063A5F19741120C4F2DE2ADEBEB10A298DD"
        ),
        // RFC - 4231 HMAC-SHA-512 test case 6
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "54657374205573696E67204C6172676572205468616E20426C6F636B2D53697A65204B6579202D2048617368204B6579204669727374",
            "80B24263C7C1A3EBB71493C1DD7BE8B49B46D1F41B4AEEC1121B013783F8F3526B56D037E05F2598BD0FD2215D6A1E5295E64F73F63F0AEC8B915A985D786598"
        ),
        // RFC - 4231 HMAC-SHA-512 test case 7
        arrayOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "5468697320697320612074657374207573696E672061206C6172676572207468616E20626C6F636B2D73697A65206B657920616E642061206C6172676572207468616E20626C6F636B2D73697A6520646174612E20546865206B6579206E6565647320746F20626520686173686564206265666F7265206265696E6720757365642062792074686520484D414320616C676F726974686D2E",
            "E37B6A775DC87DBAA4DFA9F96E5E3FFDDEBD71F8867289865DF5A32D20CDC944B6022CAC3C4982B10D5EEB55C3E4DE15134676FB6DE0446065C97440FA8C6A58"
        ),
    )
    private val otpSha1Test = arrayOf(
        arrayOf(
            "12345678901234567890",
            "59",
            "94287082"
        ),
        arrayOf(
            "12345678901234567890",
            "1111111109",
            "07081804"
        ),
        arrayOf(
            "12345678901234567890",
            "1111111111",
            "14050471"
        ),
        arrayOf(
            "12345678901234567890",
            "1234567890",
            "89005924"
        ),
        arrayOf(
            "12345678901234567890",
            "2000000000",
            "69279037"
        ),
        arrayOf(
            "12345678901234567890",
            "20000000000",
            "65353130"
        ),
    )
    private val otpSha256Test = arrayOf(
        arrayOf(
            "12345678901234567890123456789012",
            "59",
            "46119246"
        ),
        arrayOf(
            "12345678901234567890123456789012",
            "1111111109",
            "68084774"
        ),
        arrayOf(
            "12345678901234567890123456789012",
            "1111111111",
            "67062674"
        ),
        arrayOf(
            "12345678901234567890123456789012",
            "1234567890",
            "91819424"
        ),
        arrayOf(
            "12345678901234567890123456789012",
            "2000000000",
            "90698825"
        ),
        arrayOf(
            "12345678901234567890123456789012",
            "20000000000",
            "77737706"
        ),
    )
    private val otpSha512Test = arrayOf(
        arrayOf(
            "1234567890123456789012345678901234567890123456789012345678901234",
            "59",
            "90693936"
        ),
        arrayOf(
            "1234567890123456789012345678901234567890123456789012345678901234",
            "1111111109",
            "25091201"
        ),
        arrayOf(
            "1234567890123456789012345678901234567890123456789012345678901234",
            "1111111111",
            "99943326"
        ),
        arrayOf(
            "1234567890123456789012345678901234567890123456789012345678901234",
            "1234567890",
            "93441116"
        ),
        arrayOf(
            "1234567890123456789012345678901234567890123456789012345678901234",
            "2000000000",
            "38618901"
        ),
        arrayOf(
            "1234567890123456789012345678901234567890123456789012345678901234",
            "20000000000",
            "47863826"
        ),
    )

    companion object {
        private const val INS_CALCULATE_HMAC: Byte = 0x01
        private const val INS_IMPORT_HMAC_KEY: Byte = 0x02
        private const val INS_DELETE_HMAC_KEY: Byte = 0x03
        private const val INS_CALCULATE_OTP: Byte = 0x04

        private lateinit var aid: AID
        private lateinit var simulator: CardSimulator
        private lateinit var importHmacCommand: ByteArray

        @BeforeClass
        @JvmStatic
        fun setup() {
            aid = AIDUtil.create("F000000001")
        }
    }

    @Before
    fun setUp() {
        simulator = CardSimulator()
        simulator.installApplet(aid, OTPApplet::class.java)
        simulator.selectApplet(aid)

        val secret = SecretGenerator.generate()
        importHmacCommand = commandAPDU(0x00, INS_IMPORT_HMAC_KEY, 0x00, 0x00, secret, 0x01)
    }

    @Test
    fun importHmacKeys() {
        var result = simulator.transmitCommand(importHmacCommand)

        var responseApdu = ResponseAPDU(result)
        assertEquals(0x9000, responseApdu.sW)
        assertEquals(0, responseApdu.data[0].toInt())

        result = simulator.transmitCommand(importHmacCommand)

        responseApdu = ResponseAPDU(result)
        assertEquals(0x9000, responseApdu.sW)
        assertEquals(1, responseApdu.data[0].toInt())
    }

    @Test
    fun importHmacKeys_incorrectP1() {
        val secret = SecretGenerator.generate()
        val importHmacCommand = commandAPDU(0x00, INS_IMPORT_HMAC_KEY, 0x01, 0x00, secret, 0x01)

        val result = simulator.transmitCommand(importHmacCommand)

        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_INCORRECT_P1P2, responseApdu.sW.toShort())
    }

    @Test
    fun importHmacKeys_incorrectP2() {
        val secret = SecretGenerator.generate()
        val importHmacCommand = commandAPDU(0x00, INS_IMPORT_HMAC_KEY, 0x00, 0x01, secret, 0x01)

        val result = simulator.transmitCommand(importHmacCommand)

        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_INCORRECT_P1P2, responseApdu.sW.toShort())
    }

    // negative byte
    @Test
    fun importHmacKeys_insufficientMemorySpace() {
        var result: ByteArray = byteArrayOf()

        for (i in 0..256) {
            val secret = SecretGenerator.generate()
            val importHmacCommand = commandAPDU(0x00, INS_IMPORT_HMAC_KEY, 0x00, 0x00, secret, 0x01)
            result = simulator.transmitCommand(importHmacCommand)
        }

        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_FILE_FULL, responseApdu.sW.toShort())
    }

    @Test
    fun deleteHmacKeys() {
        simulator.transmitCommand(importHmacCommand)
        simulator.transmitCommand(importHmacCommand)

        val keyId: Byte = 1
        val deleteHmacCommand = commandAPDU(0x00, INS_DELETE_HMAC_KEY, 0x00, keyId)

        val result = simulator.transmitCommand(deleteHmacCommand)

        val responseApdu = ResponseAPDU(result)
        assertEquals(0x9000, responseApdu.sW)
    }

    @Test
    fun deleteHmacKey_notFound() {
        val keyId: Byte = 1
        val deleteHmacCommand = commandAPDU(0x00, INS_DELETE_HMAC_KEY, 0x00, keyId)

        val result = simulator.transmitCommand(deleteHmacCommand)

        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_RECORD_NOT_FOUND, responseApdu.sW.toShort())
    }

    @Test
    fun calculateHmacValue_notFound() {
        simulator.transmitCommand(importHmacCommand)

        val algorithm = Signature.ALG_HMAC_SHA1
        val keyId: Byte = 1
        val count = byteArrayOf(0)
        val calculateCommand = commandAPDU(0x00, INS_CALCULATE_HMAC, algorithm, keyId, count, 0x00)
        val result = simulator.transmitCommand(calculateCommand)

        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_RECORD_NOT_FOUND, responseApdu.sW.toShort())
    }

    @Test
    fun calculateHmacValue_sha1() {
        for (i in 0 until hmacSha1Test.count()) {
            testHmac(Signature.ALG_HMAC_SHA1, i, hmacSha1Test[i])
        }
    }

    @Test
    fun calculateHmacValue_sha256() {
        for (i in 0 until hmacSha256Test.count()) {
            testHmac(Signature.ALG_HMAC_SHA_256, i, hmacSha256Test[i])
        }
    }

    @Test
    fun calculateHmacValue_sha384() {
        for (i in 0 until hmacSha384Test.count()) {
            testHmac(Signature.ALG_HMAC_SHA_384, i, hmacSha384Test[i])
        }
    }

    @Test
    fun calculateHmacValue_sha512() {
        for (i in 0 until hmacSha512Test.count()) {
            testHmac(Signature.ALG_HMAC_SHA_512, i, hmacSha512Test[i])
        }
    }

    @Test
    fun calculateHmacValue_incorrectAlgorithm() {
        val hmacKeyBytes: ByteArray = Hex.decode(hmacSha1Test[0][0])
        val importCommand = commandAPDU(0x00, INS_IMPORT_HMAC_KEY, 0x00, 0x00, hmacKeyBytes, 0x01)
        simulator.transmitCommand(importCommand)

        val keyId: Byte = 0
        val counter = Hex.decode(hmacSha1Test[0][1])
        val calculateCommand = commandAPDU(0x00, INS_CALCULATE_HMAC, Signature.ALG_HMAC_MD5, keyId, counter, 0x00)
        val result = simulator.transmitCommand(calculateCommand)

        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_INCORRECT_P1P2, responseApdu.sW.toShort())
    }

    @Test
    fun calculateOTP_sha1() {
        for (i in 0 until otpSha1Test.count()) {
            testOTP(Signature.ALG_HMAC_SHA1, otpSha1Test[i])
        }
    }

    @Test
    fun calculateOTP_sha256() {
        for (i in 0 until otpSha256Test.count()) {
            testOTP(Signature.ALG_HMAC_SHA_256, otpSha256Test[i])
        }
    }

    @Test
    fun calculateOTP_sha512() {
        for (i in 0 until otpSha512Test.count()) {
            testOTP(Signature.ALG_HMAC_SHA_512, otpSha512Test[i])
        }
    }

    @Test
    fun claNoSupportedOrInvalid() {
        val unknownCommand = commandAPDU(0x01, 0x0F, 0x00, 0x00)

        val result = simulator.transmitCommand(unknownCommand)

        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_CLA_NOT_SUPPORTED, responseApdu.sW.toShort())
    }

    @Test
    fun instructionNoSupportedOrInvalid() {
        val unknownCommand = commandAPDU(0x00, 0x0F, 0x00, 0x00)

        val result = simulator.transmitCommand(unknownCommand)

        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_INS_NOT_SUPPORTED, responseApdu.sW.toShort())
    }

    private fun testHmac(algorithm: Byte, index: Int, hmacTest: Array<String>) {
        val hmacKeyBytes: ByteArray = Hex.decode(hmacTest[0])
        val importCommand = commandAPDU(0x00, INS_IMPORT_HMAC_KEY, 0x00, 0x00, hmacKeyBytes, 0x01)
        simulator.transmitCommand(importCommand)

        val keyId = index.toByte()
        val counter = Hex.decode(hmacTest[1])
        val calculateCommand = commandAPDU(0x00, INS_CALCULATE_HMAC, algorithm, keyId, counter, 0x00)
        val result = simulator.transmitCommand(calculateCommand)

        val responseApdu = ResponseAPDU(result)
        val expectedOutput = Hex.decode(hmacTest[2])
        assertEquals(0x9000, responseApdu.sW)
        assert(Arrays.areEqual(expectedOutput, responseApdu.data))
    }

    private fun testOTP(algorithm: Byte, otpSha1Test: Array<String>) {
        val hmacKeyBytes: ByteArray = otpSha1Test[0].toByteArray()
        val importCommand = commandAPDU(0x00, INS_IMPORT_HMAC_KEY, 0x00, 0x00, hmacKeyBytes, 0x01)
        simulator.transmitCommand(importCommand)

        val X = 30 // Time Step
        val keyId: Byte = 0
        val epochSecond = otpSha1Test[1].toLong()
        val time = Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC).millis() / 1000 / X
        val counter = ByteBuffer.allocate(java.lang.Long.SIZE / java.lang.Byte.SIZE)
            .putLong(time).array()
        val calculateCommand = commandAPDU(0x00, INS_CALCULATE_OTP, algorithm, keyId, counter, 0x00)
        val result = simulator.transmitCommand(calculateCommand)

        val responseApdu = ResponseAPDU(result)
        val output = fromByteArray(responseApdu.data).toString()
        val expectedOutput = otpSha1Test[2]
        assertEquals(0x9000, responseApdu.sW)
        assert(output.contains(expectedOutput))
    }

    @After
    fun tearDown() {
        simulator.deleteApplet(aid)
    }
}