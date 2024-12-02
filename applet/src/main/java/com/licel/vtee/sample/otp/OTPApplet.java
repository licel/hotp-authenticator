// LICEL CORPORATION CONFIDENTIAL
//
// Copyright 2023-2024 LICEL CORPORATION
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
//
package com.licel.vtee.sample.otp;

import com.licel.jcardsimr.nativeinterface.NativeSystem;
import javacard.framework.*;
import javacard.security.HMACKey;
import javacard.security.KeyBuilder;
import javacard.security.Signature;

public class OTPApplet extends Applet {
    private final static byte CLA = 0x00;
    private final static byte INS_CALCULATE_HMAC = 0x01;
    private final static byte INS_IMPORT_HMAC_KEY = 0x02;
    private final static byte INS_DELETE_HMAC_KEY = 0x03;
    private final static byte INS_CALCULATE_OTP = 0x04;

    private final Signature sigEngineSHA1;
    private final Signature sigEngineSHA256;
    private final Signature sigEngineSHA384;
    private final Signature sigEngineSHA512;

    private final HMACKey[] hmacKeys;
    private final byte[] transientHmacValue;
    private final byte[] transientData;

    private static final short HMAC_KEYS_SIZE = 256;
    private static final short MAX_ALLOWED_HMAC_SIZE_BYTES = 64;
    private static final short MAX_ALLOWED_DATA_SIZE_BYTES = 512;

    OTPApplet(byte[] bArray, short bOffset, byte bLength) {
        hmacKeys = new HMACKey[HMAC_KEYS_SIZE];
        transientHmacValue = JCSystem.makeTransientByteArray(MAX_ALLOWED_HMAC_SIZE_BYTES, JCSystem.CLEAR_ON_DESELECT);
        transientData = JCSystem.makeTransientByteArray(MAX_ALLOWED_DATA_SIZE_BYTES, JCSystem.CLEAR_ON_DESELECT);
        register();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
        new OTPApplet(bArray, bOffset, bLength);
    }

    @Override
    public void process(APDU apdu) throws ISOException {
        if (!selectingApplet()) {

            byte[] buffer = apdu.getBuffer();

            if (buffer[ISO7816.OFFSET_CLA] != CLA) {
                ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
            }
            switch (buffer[ISO7816.OFFSET_INS]) {
                case INS_IMPORT_HMAC_KEY:
                    importHmacKey(apdu);
                    break;
                case INS_DELETE_HMAC_KEY:
                    deleteHmacKey(apdu);
                    break;
                case INS_CALCULATE_HMAC:
                    calculateHmacValue(apdu);
                    break;
                case INS_CALCULATE_OTP:
                    calculateOtp(apdu);
                    break;
                default:
                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            }
        }
    }

    private void importHmacKey(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        if (buffer[ISO7816.OFFSET_P1] != 0x00 || buffer[ISO7816.OFFSET_P2] != 0x00) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }

        short dataLength = apdu.setIncomingAndReceive();
        byte[] secretBytes = getData(apdu, dataLength);

        HMACKey hmacKey = generateHmacKey(secretBytes, dataLength);
        short index = getNextHmacKeyIndex();
        hmacKeys[index] = hmacKey;

//        printWrappedAndUnwrappedKeyForDebugOnly(index);

        buffer[0] = (byte) index;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void printWrappedAndUnwrappedKeyForDebugOnly(short index) {
        NativeSystem.printShort(index);
        NativeSystem.printShort(hmacKeys[index].getSize());

        for(short hmacKeyIdx = 0; hmacKeyIdx <= index; hmacKeyIdx++){
            short wrappedHMACKeySize = NativeSystem.getWrappedKeySizeBytes(hmacKeys[hmacKeyIdx].getSize());
            byte[] wrappedHMACKeyBytes = JCSystem.makeTransientByteArray(wrappedHMACKeySize, JCSystem.CLEAR_ON_DESELECT);
            hmacKeys[hmacKeyIdx].getKey(wrappedHMACKeyBytes, (short) 0);
            NativeSystem.printArrayAsHexString(wrappedHMACKeyBytes, (short) 0, wrappedHMACKeySize);

            byte[] unwrappedHMACKeyBytes = JCSystem.makeTransientByteArray((short) (hmacKeys[hmacKeyIdx].getSize()/8), JCSystem.CLEAR_ON_DESELECT);
            NativeSystem.symKeyUnwrap(wrappedHMACKeyBytes,unwrappedHMACKeyBytes);
            NativeSystem.printArrayAsHexString(unwrappedHMACKeyBytes, (short) 0, (short) unwrappedHMACKeyBytes.length);

        }
    }

    private void deleteHmacKey(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        byte hmacKeyIndex = buffer[ISO7816.OFFSET_P2];
        HMACKey hmacKey = getHmacKey(hmacKeyIndex);

        hmacKey.clearKey();
        hmacKeys[hmacKeyIndex] = null;
    }

    private void calculateHmacValue(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        byte algorithmType = buffer[ISO7816.OFFSET_P1];
        byte hmacKeyIndex = buffer[ISO7816.OFFSET_P2];

        HMACKey hmacKey = getHmacKey(hmacKeyIndex);

        byte[] counterBytes = getData(apdu, dataLength);
        byte[] hmacValue = hmacValue(hmacKey, counterBytes, dataLength, algorithmType);
        byte length = getByteOutput(algorithmType);

        apdu.setOutgoing();
        apdu.setOutgoingLength(length);
        apdu.sendBytesLong(hmacValue, (short) 0, length);
    }

    private HMACKey getHmacKey(byte index) {
        HMACKey hmacKey = hmacKeys[index];
        if (hmacKey == null) {
            ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
        }
        return hmacKey;
    }

    private byte[] getData(APDU apdu, short readCount) {
        byte[] buffer = apdu.getBuffer();
        byte lc = buffer[ISO7816.OFFSET_LC];
        if (lc > MAX_ALLOWED_DATA_SIZE_BYTES || lc == 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        short offsetCData = apdu.getOffsetCdata();
        short read = readCount;
        while (read < lc) {
            read += apdu.receiveBytes(read);
        }

        Util.arrayCopyNonAtomic(buffer, offsetCData, transientData, (short) 0, readCount);

        return transientData;
    }

    private HMACKey generateHmacKey(final byte[] secretBytes, final short secretLength) {
        short secretLengthBits = (short)(secretLength*8);
        HMACKey hmacKey = (HMACKey) KeyBuilder.buildKey(KeyBuilder.TYPE_HMAC, secretLengthBits, false);
        hmacKey.setKey(secretBytes, (short) 0, (short) secretLength);
        return hmacKey;
    }

    private byte[] hmacValue(final HMACKey hmacKey, final byte[] counterBytes, final short counterLength, final byte algorithm) {
        switch (algorithm) {
            case Signature.ALG_HMAC_SHA1: {
                sigEngineSHA1.init(hmacKey, Signature.MODE_SIGN);
                sigEngineSHA1.sign(counterBytes, (short) 0, (short) counterLength, transientHmacValue, (short) 0);
                break;
            }
            case Signature.ALG_HMAC_SHA_256: {
                sigEngineSHA256.init(hmacKey, Signature.MODE_SIGN);
                sigEngineSHA256.sign(counterBytes, (short) 0, (short) counterLength, transientHmacValue, (short) 0);
                break;
            }
            case Signature.ALG_HMAC_SHA_384: {
                sigEngineSHA384.init(hmacKey, Signature.MODE_SIGN);
                sigEngineSHA384.sign(counterBytes, (short) 0, (short) counterLength, transientHmacValue, (short) 0);
                break;
            }
            case Signature.ALG_HMAC_SHA_512: {
                sigEngineSHA512.init(hmacKey, Signature.MODE_SIGN);
                sigEngineSHA512.sign(counterBytes, (short) 0, (short) counterLength, transientHmacValue, (short) 0);
                break;
            }
            default: {
                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
            }
        }

        return transientHmacValue;
    }

    private short getNextHmacKeyIndex() {
        short counter = 0;
        for (short i = 0; i < hmacKeys.length; i++) {
            if (hmacKeys[i] != null) {
                counter++;
            } else {
                break;
            }
        }
        if (counter >= hmacKeys.length) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
        return counter;
    }

    private byte getByteOutput(byte algorithm) {
        switch (algorithm) {
            case Signature.ALG_HMAC_SHA1:
                return 20;
            case Signature.ALG_HMAC_SHA_256:
                return 32;
            case Signature.ALG_HMAC_SHA_384:
                return 48;
            case Signature.ALG_HMAC_SHA_512:
                return 64;
        }
        ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        return 0;
    }

    private void calculateOtp(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        byte algorithmType = buffer[ISO7816.OFFSET_P1];
        byte hmacKeyIndex = buffer[ISO7816.OFFSET_P2];

        HMACKey hmacKey = getHmacKey(hmacKeyIndex);
        byte lastIndex = (byte) (getByteOutput(algorithmType) - 1);

        byte[] counterBytes = getData(apdu, dataLength);
        byte[] hmacValue = hmacValue(hmacKey, counterBytes, dataLength, algorithmType);
        byte[] truncateHash = truncate(hmacValue, lastIndex);

        byte length = (byte) truncateHash.length;

        apdu.setOutgoing();
        apdu.setOutgoingLength(length);
        apdu.sendBytesLong(truncateHash, (short) 0, length);
    }

    private byte[] truncate(final byte[] hash, final byte length) {
        byte offset = (byte) (hash[length] & 0xf);
        byte[] result = new byte[(short) 4];

        result[0] = (byte) (hash[offset] & 0x7f);
        result[1] = (byte) (hash[offset + 1]);
        result[2] = (byte) (hash[offset + 2]);
        result[3] = (byte) (hash[offset + 3]);

        return result;
    }
}