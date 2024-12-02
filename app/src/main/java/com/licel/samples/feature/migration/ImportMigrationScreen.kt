package com.licel.samples.feature.migration

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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportMigrationScreen(
    uiState: ImportMigrationUiState,
    modifier: Modifier = Modifier,
    onSubmitButtonClick: (List<Int>) -> Unit = {},
) {
    val lazyListState = rememberLazyListState()
    val checkedStatus = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    titleContentColor = Color.White,
                    containerColor = Color.Black,
                ),
                actions = {
                    Text(
                        text = "Submit",
                        style = TextStyle(color = Color.White),
                        modifier = Modifier.clickable {
                            val keys = checkedStatus
                                .filter { it.value }
                                .map { it.key }
                            onSubmitButtonClick(keys)
                        }
                    )
                }
            )
        },
    ) { paddingValue ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                top = 20.dp,
                start = 20.dp,
                end = 20.dp
            ),
            modifier = modifier.padding(paddingValue),
            state = lazyListState
        ) {
            itemsIndexed(uiState.issuers) { index, issuer ->
                val isSelected =
                    ImportOtp(
                        issuer = issuer,
                        checked = checkedStatus[index] ?: false,
                        onCheckedChange = { checkedStatus[index] = it }
                    )
            }
        }
    }
}

@Composable
fun ImportOtp(
    issuer: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit) = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            modifier
        )
        Text(issuer, modifier.clickable { onCheckedChange(!checked) })
    }
}

@Preview
@Composable
fun PreviewImportMigrationScreen() {
    ImportMigrationScreen(ImportMigrationUiState())
}
