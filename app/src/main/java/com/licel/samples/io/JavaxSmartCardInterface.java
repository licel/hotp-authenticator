package com.licel.samples.io;// Copyright 2023 LICEL CORPORATION
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

import com.licel.jcardsim.base.Simulator;
import com.licel.jcardsim.base.SimulatorRuntime;
import com.licel.samples.smartcardio.CommandAPDU;
import com.licel.samples.smartcardio.ResponseAPDU;

public class JavaxSmartCardInterface extends Simulator {
    /**
     * Create a JavaxSmartCardInterface object using the default SimulatorRuntime.
     *
     * <ul>
     *     <li>All <code>JavaxSmartCardInterface</code> instances share one <code>SimulatorRuntime</code>.</li>
     *     <li>SimulatorRuntime#resetRuntime is called</li>
     *     <li>If your want multiple independent simulators use <code>JavaxSmartCardInterface(SimulatorRuntime)</code></li>
     * </ul>
     */
    public JavaxSmartCardInterface() {
        super();
    }

    /**
     * Create a JavaxSmartCardInterface object using a provided Runtime.
     *
     * <ul>
     *     <li>SimulatorRuntime#resetRuntime is called</li>
     * </ul>
     *
     * @param runtime SimulatorRuntime instance to use
     * @throws NullPointerException if <code>runtime</code> is null
     */
    public JavaxSmartCardInterface(SimulatorRuntime runtime) {
        super(runtime);
    }

    /**
     * Wrapper for transmitCommand(byte[])
     * @param commandApdu CommandAPDU
     * @return ResponseAPDU
     */
    public ResponseAPDU transmitCommand(CommandAPDU commandApdu) {
        return new ResponseAPDU(transmitCommand(commandApdu.getBytes()));
    }
}
