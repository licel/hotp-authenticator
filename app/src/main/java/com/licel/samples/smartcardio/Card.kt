package com.licel.samples.smartcardio

import javacard.framework.CardException




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
 * Base class for interfacing to smart cards
 */
abstract class Card
/**
 * Main constructor
 */
protected constructor() {
    /** @return the ATR sent by the card
     */
    abstract val aTR: ATR?

    /** @return the protocol used to communicate with the card
     */
    abstract val protocol: String?

    /** @return the basic channel for the card
     */
    abstract val basicChannel: CardChannel?

    /**
     * Open a logical channel
     * @return the channel
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun openLogicalChannel(): CardChannel?

    /**
     * Lock the card for thread-exclusive access
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun beginExclusive()

    /**
     * End thread-exclusive access
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun endExclusive()

    /**
     * Transmit a control command
     * @param controlCode
     * @param command
     * @return
     * @throws CardException
     */
    @Throws(CardException::class)
    abstract fun transmitControlCommand(
        controlCode: Int,
        command: ByteArray?
    ): ByteArray?

    /**
     * Disconnect from the card
     * @param reset true to reset the card
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun disconnect(reset: Boolean)
}