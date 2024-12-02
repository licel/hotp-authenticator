package com.licel.samples.smartcardio

import javacard.framework.CardException


/**
 * Base class for interfacing to card terminals
 */
abstract class CardTerminal
/**
 * Main constructor
 */
protected constructor() {
    /** @return the name of this terminal
     */
    abstract val name: String?

    /** @return true of the terminal has a card present
     */
    @Throws(CardException::class)
    abstract fun isCardPresent(): Boolean

    /**
     * Connect to the card in this terminal
     * @param protocol to use or "*"
     * @return an interface to the card
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun connect(protocol: String): Card?

    /**
     * Wait for a card to be present
     * @param timeout after which to return
     * @return true if there is a card now
     * @throws CardException
     */
    @Throws(CardException::class)
    abstract fun waitForCardPresent(timeout: Long): Boolean

    /**
     * Wait for a card to be absent
     * @param timeout after which to return
     * @return true if the card is gone now
     * @throws CardException
     */
    @Throws(CardException::class)
    abstract fun waitForCardAbsent(timeout: Long): Boolean
}