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

abstract class CardTerminals
/**
 * Main constructor
 */
protected constructor() {
    /** Reader states  */
    enum class State {
        ALL, CARD_PRESENT, CARD_ABSENT, CARD_INSERTION, CARD_REMOVAL
    }

    /**
     * Get a specific terminal by name
     * @param name of the terminal
     * @return the terminal or null
     */
    fun getTerminal(name: String): CardTerminal? {
        return try {
            for (terminal in list()) {
                if (terminal.name == name) {
                    return terminal
                }
            }
            null
        } catch (e: CardException) {
            null
        }
    }

    /**
     * List all terminals in this set
     * @return list of all terminals
     * @throws CardException on error
     */
    @Throws(CardException::class)
    fun list(): List<CardTerminal> {
        return list(State.ALL)
    }

    /**
     * List all terminals in the given state
     * @param state to query
     * @return list of terminals in given state
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun list(state: State?): List<CardTerminal>

    /**
     * Wait for any addition, removal or state change
     *
     *
     * Convenience variant with infinite timeout.
     *
     *
     * @throws CardException on error
     */
    @Throws(CardException::class)
    fun waitForChange() {
        waitForChange(0)
    }

    /**
     * Wait for any addition, removal or state change
     * @throws CardException on error
     */
    @Throws(CardException::class)
    abstract fun waitForChange(timeout: Long): Boolean
}