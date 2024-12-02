package com.licel.samples.smartcardio

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

/*
 * Copyright 2015 Robert Bachmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.licel.jcardsim.utils.AutoResetEvent
import com.licel.samples.smartcardio.CardTerminals
import com.licel.samples.smartcardio.TerminalFactorySpi
import java.security.AccessController
import java.security.PrivilegedAction
import java.security.Provider
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * A simulated [TerminalFactory].
 *
 * Example: Obtaining a Card
 * <pre>
 * // create card simulator
 * CardSimulator cardSimulator = new CardSimulator();
 *
 * // connect to a card
 * CardTerminal terminal =
 * CardTerminalSimulator.terminal(cardSimulator);
 * Card card = terminal.connect("*");
</pre> *
 *
 * Example: Inserting/ejecting a Card
 * <pre>
 * // create card simulator
 * CardSimulator cardSimulator = new CardSimulator();
 *
 * // create CardTerminal
 * CardTerminals terminals = CardTerminalSimulator.terminals("my terminal")
 * CardTerminal terminal = terminals.getTerminal("my terminal");
 *
 * // insert Card
 * cardSimulator.assignToTerminal(terminal);
 *
 * // eject Card
 * cardSimulator.assignToTerminal(null);
</pre> *
 *
 * @see com.licel.jcardsim.smartcardio.CardSimulator
 */

object CardTerminalSimulator {
    /**
     * Create a single CardTerminal.
     *
     * @param cardSimulator card to insert
     * @param name          the terminal name
     * @return a new `CardTerminal` instance
     * @throws java.lang.NullPointerException if name or cardSimulator is null
     */
    fun terminal(cardSimulator: CardSimulator?, name: String?): CardTerminal? {
        if (name == null) {
            throw NullPointerException("name")
        }
        if (cardSimulator == null) {
            throw NullPointerException("cardSimulator")
        }
        val cardTerminal: CardTerminal? = terminals(name)!!.getTerminal(name)
        cardSimulator.assignToTerminal(cardTerminal)
        return cardTerminal
    }

    /**
     * Create a CardTerminal with name "jCardSim.Terminal".
     *
     * @param cardSimulator card to insert
     * @return a new `CardTerminal` instance
     * @throws java.lang.NullPointerException if name or cardSimulator is null
     */
    fun terminal(cardSimulator: CardSimulator?): CardTerminal? {
        return terminal(cardSimulator, "jCardSim.Terminal")
    }

    /**
     *
     * Create CardTerminals.
     *
     * Example:
     * <pre>
     * CardTerminals terminals = CardTerminalSimulator.terminals("1","2");
     * CardTerminal terminal = terminals.getTerminal("1");
     *
     * // assign simulator
     * CardSimulator cardSimulator = new CardSimulator();
     * cardSimulator.assignToTerminal(terminal);
    </pre> *
     *
     * @param names the terminal names
     * @return a new `CardTerminals` instance
     * @throws java.lang.NullPointerException     if names is null
     * @throws java.lang.IllegalArgumentException if any name is null or duplicated
     * @see javax.smartcardio.CardTerminals
     */
    fun terminals(vararg names: String): CardTerminals? {
        if (names == null) {
            throw NullPointerException("names")
        }
        val set: MutableSet<String> = HashSet(names.size)
        for (name in names) {
            require(!set.contains(name)) { "Duplicate name '$name'" }
            set.add(name)
        }
        return CardTerminalsImpl(set.toTypedArray())
    }

    @Throws(InterruptedException::class)
    fun waitForLatch(autoResetEvent: AutoResetEvent, timeoutMilliseconds: Long): Boolean {
        require(timeoutMilliseconds >= 0) { "timeout is negative" }
        if (timeoutMilliseconds == 0L) { // wait forever
            var success: Boolean
            do {
                success = autoResetEvent.await(1, TimeUnit.MINUTES)
            } while (!success)
            return true
        }
        return autoResetEvent.await(timeoutMilliseconds, TimeUnit.MILLISECONDS)
    }

    /**
     *
     * Security provider.
     *
     * Register the SecurityProvider with:
     * <pre>
     * if (Security.getProvider("CardTerminalSimulator") == null) {
     * Security.addProvider(new CardTerminalSimulator.SecurityProvider());
     * }
    </pre> *
     */
    class SecurityProvider :
        Provider("CardTerminalSimulator", 1.0, "jCardSim Virtual Terminal Provider") {
        init {
            AccessController.doPrivileged(PrivilegedAction<Any?> {
                put(
                    "TerminalFactory." + "CardTerminalSimulator", Factory::class.java.canonicalName
                        .replace(".Factory", "\$Factory")
                )
                null
            })
        }
    }

    /**
     * [javax.smartcardio.TerminalFactorySpi] implementation.
     * Applications do not access this class directly, instead see [javax.smartcardio.TerminalFactory].
     */
    class Factory(params: Any?) : TerminalFactorySpi() {
        private var cardTerminals: CardTerminals?

        init {
            val names: Array<String> = if (params == null) {
                arrayOf("jCardSim.Terminal")
            } else if (params is String) {
                arrayOf(params)
//            } else if (params is Array<*> && params?.isArrayOf<String>() == true)
//                params
            } else {
                throw IllegalArgumentException("Illegal parameter: $params")
            }
            cardTerminals = terminals(*names)
        }

        override fun engineTerminals(): CardTerminals? {
            return cardTerminals
        }
    }

    internal class CardTerminalsImpl(names: Array<String>) : CardTerminals() {
        private val waitCalled = AtomicBoolean(false)
        private val terminalsChangeAutoResetEvent = AutoResetEvent()
        private val simulatedTerminals: ArrayList<CardTerminalImpl>
        private val terminalStateMap: HashMap<CardTerminal?, State?>

        init {
            simulatedTerminals = ArrayList(names.size)
            terminalStateMap = HashMap<CardTerminal?, State?>(names.size)
            for (name in names) {
                simulatedTerminals.add(
                    CardTerminalImpl(
                        name,
                        terminalStateMap,
                        terminalsChangeAutoResetEvent
                    )
                )
            }
        }

        @Synchronized
//        @Throws(javax.smartcardio.CardException::class)
        override fun list(state: State?): List<CardTerminal> {
            if (state == null) {
                throw NullPointerException("state")
            }
            synchronized(terminalStateMap) {
                val result: ArrayList<CardTerminal> = ArrayList<CardTerminal>(simulatedTerminals.size)
                for (terminal in simulatedTerminals) {
                    val terminalState: State? = terminalStateMap[terminal]
                    when (terminalState) {
                        State.ALL -> result.add(terminal)
                        State.CARD_ABSENT -> if (!terminal.isCardPresent() && terminalState != State.CARD_REMOVAL) {
                            result.add(terminal)
                        }

                        State.CARD_PRESENT -> if (terminal.isCardPresent() && terminalState != State.CARD_INSERTION) {
                            result.add(terminal)
                        }

                        State.CARD_INSERTION -> if (waitCalled.get()) {
                            if (terminalState == State.CARD_INSERTION) {
                                terminalStateMap[terminal] =
                                    State.CARD_PRESENT
                                result.add(terminal)
                            }
                        } else if (terminal.isCardPresent()) {
                            result.add(terminal)
                        }

                        State.CARD_REMOVAL -> if (waitCalled.get()) {
                            if (terminalState == State.CARD_REMOVAL) {
                                terminalStateMap[terminal] = State.CARD_ABSENT
                                result.add(terminal)
                            }
                        } else if (!terminal.isCardPresent()) {
                            result.add(terminal)
                        }

                        else -> {}
                    }
                }
                return Collections.unmodifiableList<CardTerminal>(result)
            }
        }

        //        @Throws(javax.smartcardio.CardException::class)
        override fun waitForChange(timeoutMilliseconds: Long): Boolean {
            return try {
                waitForLatch(terminalsChangeAutoResetEvent, timeoutMilliseconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            } finally {
                waitCalled.set(true)
            }
        }
    }

    internal class CardTerminalImpl(
        override val name: String,
        terminalStateMap: MutableMap<CardTerminal?, CardTerminals.State?>,
        terminalsChangeAutoResetEvent: AutoResetEvent
    ) :
        CardTerminal() {
        private val terminalStateMap: MutableMap<CardTerminal?, CardTerminals.State?>
        private val terminalsChangeAutoResetEvent: AutoResetEvent
        private val cardPresent = AutoResetEvent()
        private val cardAbsent = AutoResetEvent()
        private val cardSimulatorReference: AtomicReference<CardSimulator?> =
            AtomicReference<CardSimulator?>()

        init {
            this.terminalStateMap = terminalStateMap
            this.terminalsChangeAutoResetEvent = terminalsChangeAutoResetEvent
            cardAbsent.signal()
            terminalStateMap[this] = CardTerminals.State.CARD_ABSENT
        }

        override fun connect(protocol: String): Card {
            val cardSimulator: CardSimulator = cardSimulatorReference.get()
                ?: throw Exception("No card inserted. You need to call CardTerminalSimulator#assignToTerminal")
            return cardSimulator.internalConnect(protocol)
        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun isCardPresent(): Boolean {
            return cardSimulatorReference.get() != null
        }

        //        @Throws(javax.smartcardio.CardException::class)
        override fun waitForCardPresent(timeoutMilliseconds: Long): Boolean {
            return try {
                waitForLatch(cardPresent, timeoutMilliseconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun waitForCardAbsent(timeoutMilliseconds: Long): Boolean {
            return try {
                waitForLatch(cardAbsent, timeoutMilliseconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        }

        fun assignSimulator(cardSimulator: CardSimulator?) {
            synchronized(terminalStateMap) {
                val oldCardSimulator: CardSimulator? =
                    cardSimulatorReference.getAndSet(cardSimulator)
                var change = false
                var present = false
                if (oldCardSimulator != null) {
                    oldCardSimulator.internalEject(this)
                    change = true
                }
                if (cardSimulator != null) {
                    present = true
                    change = true
                }
                if (change) {
                    if (present) {
                        terminalStateMap[this] = CardTerminals.State.CARD_INSERTION
                        cardPresent.signal()
                        cardAbsent.reset()
                    } else {
                        terminalStateMap[this] = CardTerminals.State.CARD_REMOVAL
                        cardPresent.reset()
                        cardAbsent.signal()
                    }
                    terminalsChangeAutoResetEvent.signal()
                }
            }
        }

        override fun toString(): String {
            return "jCardSim Terminal: $name"
        }
    }
}