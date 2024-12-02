package com.licel.samples.smartcardio

/**
 * Create a Simulator object using a provided Runtime.
 *
 *  * SimulatorRuntime#resetRuntime is called
 *
 *
 * @param runtime SimulatorRuntime instance to use
 * @throws java.lang.NullPointerException if `runtime` is null
 */

import com.licel.jcardsim.base.CardManager
import com.licel.jcardsim.base.SimulatorRuntime
import com.licel.samples.io.JavaxSmartCardInterface
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

/**
 * Create a Simulator object using a new SimulatorRuntime.
 *
 *  * SimulatorRuntime#resetRuntime is called
 *
 */
class CardSimulator(runtime: SimulatorRuntime? = SimulatorRuntime()) : JavaxSmartCardInterface(runtime) {
    private val card: CardImpl = CardImpl()
    private val owningCardTerminalReference: AtomicReference<CardTerminal?> =
        AtomicReference<CardTerminal?>()
    private val threadReference = AtomicReference<Thread?>()
//    private val runtime: SimulatorRuntime = SimulatorRuntime()
    /**
     * Wrapper for [.transmitCommand]
     *
     * @param commandApdu CommandAPDU
     * @return ResponseAPDU
     */
    override fun transmitCommand(commandApdu: CommandAPDU): ResponseAPDU {
        return ResponseAPDU(transmitCommand(commandApdu.bytes))
    }

    /**
     *
     * Assigns this simulated card to a CardTerminal.
     *
     * If the card is already assigned to another CardTerminal, it will be ejected
     * and inserted into the CardTerminal `terminal`.
     *
     * @param terminal card terminal or `null`
     */
    @Synchronized
    fun assignToTerminal(terminal: CardTerminal?) {
        val oldCardTerminal: CardTerminal? =
            owningCardTerminalReference.getAndSet(terminal)
        if (terminal === oldCardTerminal) {
            return
        }
        if (oldCardTerminal != null) {
            // eject card from old Terminal
            (oldCardTerminal as CardTerminalSimulator.CardTerminalImpl).assignSimulator(null)
        }
        if (terminal != null) {
            // reset card
            card.disconnect()
            // assign to new terminal
            (terminal as CardTerminalSimulator.CardTerminalImpl).assignSimulator(this)
        }
    }

    /**
     * @return the assigned CardTerminal or null if none is assigned
     */
    val assignedCardTerminal: CardTerminal?
        get() = owningCardTerminalReference.get()

    fun internalConnect(protocol: String): Card {
        card.connect(protocol)
        return card
    }

    fun internalEject(oldTerminal: CardTerminal?) {
        if (owningCardTerminalReference.compareAndSet(oldTerminal, null)) {
            card.eject()
        }
    }

    private enum class CardState {
        Connected, Disconnected, Ejected
    }

    private class CardChannelImpl(override val card: CardImpl, private val channelNr: Int) : CardChannel() {
//        fun getCard(): Card {
//            return card
//        }

        override val channelNumber: Int
            get() {
                card.ensureConnected()
                return channelNr
            }

//        @Throws(javax.smartcardio.CardException::class)
        override fun transmit(commandAPDU: CommandAPDU?): ResponseAPDU? {
            return ResponseAPDU(card.transmitCommand(commandAPDU!!.bytes))
        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun transmit(byteBuffer: ByteBuffer?, byteBuffer2: ByteBuffer?): Int {
            val result = card.transmitCommand(CommandAPDU(byteBuffer!!).bytes)
            byteBuffer2?.put(result)
            return result.size
        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun close() {
            throw Exception("Can not close basic channel")
        }
    }

    private inner class CardImpl : Card() {
        override val basicChannel: CardChannel

        @Volatile
        override var protocol = "T=0"
            private set

        @Volatile
        private var protocolByte: Byte = 0

        @Volatile
        private var state = CardState.Connected

        init {
            basicChannel = CardChannelImpl(this, 0)
        }

        fun ensureConnected() {
            val cardState = state
            check(cardState != CardState.Disconnected) { "Card was disconnected" }
            check(cardState != CardState.Ejected) { "Card was removed" }
        }

        override val aTR: ATR
            get() = ATR(this@CardSimulator.getATR())

//        fun getBasicChannel(): CardChannel {
//            return basicChannel
//        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun openLogicalChannel(): CardChannel {
            throw Exception("Logical channel not supported")
        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun beginExclusive() {
            synchronized(runtime) {
                if (!threadReference.compareAndSet(null, Thread.currentThread())) {
                    throw Exception("Card is held exclusively by Thread " + threadReference.get())
                }
            }
        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun endExclusive() {
            synchronized(runtime) {
                if (!threadReference.compareAndSet(Thread.currentThread(), null)) {
                    throw Exception("Card is held exclusively by Thread " + threadReference.get())
                }
            }
        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun transmitControlCommand(controlCode: Int, command: ByteArray?): ByteArray? {
            throw UnsupportedOperationException("Not supported yet.")
        }

//        @Throws(javax.smartcardio.CardException::class)
        override fun disconnect(reset: Boolean) {
            synchronized(runtime) {
                if (reset) {
                    this@CardSimulator.reset()
                }
                state = CardState.Disconnected
            }
        }

        fun connect(protocol: String) {
            synchronized(runtime) {
                protocolByte = this@CardSimulator.getProtocolByte(protocol)
                this.protocol = protocol
                state = CardState.Connected
            }
        }

        fun eject() {
            synchronized(runtime) {
                this@CardSimulator.reset()
                state = CardState.Ejected
            }
        }

        fun disconnect() {
            synchronized(runtime) {
                this@CardSimulator.reset()
                state = CardState.Disconnected
            }
        }

//        @Throws(javax.smartcardio.CardException::class)
        fun transmitCommand(capdu: ByteArray?): ByteArray {
            synchronized(runtime) {
                ensureConnected()
                val thread = threadReference.get()
                if (thread != null && thread !== Thread.currentThread()) {
                    throw Exception("Card is held exclusively by Thread " + thread.name)
                }
                val currentProtocol: Byte = getProtocolByte(this@CardSimulator.getProtocol())
                return try {
                    runtime.changeProtocol(protocolByte)
                    CardManager.dispatchApdu(this@CardSimulator, capdu)
                } finally {
                    runtime.changeProtocol(currentProtocol)
                }
            }
        }
    }
}