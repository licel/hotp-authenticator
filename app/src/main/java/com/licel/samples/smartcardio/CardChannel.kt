package com.licel.samples.smartcardio

import javacard.framework.CardException

import java.nio.ByteBuffer

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

abstract class CardChannel
/**
 * Main constructor
 */
protected constructor() {
    /** @return the card this channel is for
     */
    abstract val card: Card?

    /** @return the channel number of this channel
     */
    abstract val channelNumber: Int

    /**
     * Transmit an APDU through the channel
     * @param command to transmit
     * @return response to command
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun transmit(command: CommandAPDU?): ResponseAPDU?

    /**
     * Transmit a raw APDU through the channel
     * @param command buffer with command
     * @param response buffer for response
     * @return length of response
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun transmit(command: ByteBuffer?, response: ByteBuffer?): Int

    /**
     * Close the channel
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun close()
}