package com.licel.samples.di

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

import android.content.Context
import com.licel.jcardsim.base.PersistentSimulatorRuntime
import com.licel.jcardsim.base.Simulator
import com.licel.jcardsim.base.SimulatorRuntime
import com.licel.jcardsim.utils.AIDUtil
import com.licel.samples.applet.OTPApplet
import com.licel.samples.smartcardio.CardSimulator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object CardSimulatorModule {

    @Singleton
    @Provides
    fun cardSimulatorModule(@ApplicationContext context: Context): CardSimulator {
        val aidStr = "F000000001"
        val aid = AIDUtil.create(aidStr);
        var first_run = false;
        var eepromDir = File(context.filesDir, "eeprom")
        if (!eepromDir.exists()) {
            eepromDir.mkdir()
            first_run = true;
        }
        System.setProperty(PersistentSimulatorRuntime.PERSISTENT_BASE_DIR, eepromDir.absolutePath);
        System.setProperty(Simulator.ATR_SYSTEM_PROPERTY, Simulator.DEFAULT_ATR);

        val runtime: SimulatorRuntime = PersistentSimulatorRuntime()
        val simulator = CardSimulator(runtime)

        if(first_run) {
            simulator.installApplet(aid, OTPApplet::class.java)
        } else {
            runtime.loadApplet(aid, OTPApplet::class.java);
        }
        simulator.selectApplet(aid)
        return simulator
    }
}