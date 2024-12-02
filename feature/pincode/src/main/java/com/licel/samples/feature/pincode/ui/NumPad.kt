package com.licel.samples.feature.pincode.ui

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
//

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licel.samples.core.ui.indication.ScaleIndication

private typealias OnClickCallback = (String) -> Unit

private val LocalOnClick =
    staticCompositionLocalOf<OnClickCallback> { error("No onClick callback") }

@Composable
fun NumPad(
    modifier: Modifier = Modifier,
    showBiometric: Boolean = false,
    onClick: (String) -> Unit = {},
    onBiometricClick: () -> Unit = {},
    onDelClick: () -> Unit = {}
) {
    CompositionLocalProvider(
        LocalOnClick provides onClick,
        LocalIndication provides ScaleIndication
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Num("1", modifier.weight(1f))
            Num("2", modifier.weight(1f))
            Num("3", modifier.weight(1f))
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 30.dp),
        ) {
            Num("4", modifier.weight(1f))
            Num("5", modifier.weight(1f))
            Num("6", modifier.weight(1f))
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 30.dp),
        ) {
            Num("7", modifier.weight(1f))
            Num("8", modifier.weight(1f))
            Num("9", modifier.weight(1f))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 30.dp),
        ) {
            Icon(
                Icons.Outlined.Fingerprint,
                contentDescription = null,
                modifier
                    .weight(1f)
                    .size(if (showBiometric) 46.dp else 0.dp)
                    .clickable { onBiometricClick() }
            )
            Num("0", modifier.weight(1f))
            Icon(
                Icons.Outlined.Backspace,
                contentDescription = null,
                modifier
                    .weight(1f)
                    .size(46.dp)
                    .padding(8.dp)
                    .clickable { onDelClick() }
            )
        }
    }
}

@Composable
private fun Num(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit = LocalOnClick.current
) {
    Text(
        text = text,
        style = TextStyle(
            fontWeight = FontWeight.Light,
            fontSize = 40.sp,
            textAlign = TextAlign.Center
        ),
        color = Color.Black,
        modifier = modifier.clickable { onClick(text) }
    )
}