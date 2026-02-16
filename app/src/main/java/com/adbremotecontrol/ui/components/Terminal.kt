package com.adbremotecontrol.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.adbremotecontrol.R

@Composable
fun Terminal(
    command: String,
    output: List<String>,
    isCommandRunning: Boolean,
    onCommandChange: (String) -> Unit,
    onExecuteCommand: (String) -> Unit,
    onStopCommand: () -> Unit
) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题和停止按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.terminal),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (isCommandRunning) {
                Button(
                    onClick = onStopCommand,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Stop,
                        contentDescription = stringResource(R.string.stop),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.stop))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 终端输出区域
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                state = listState
            ) {
                if (output.isEmpty()) {
                    item {
                        Text(
                            text = "Terminal ready. Enter a command below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(output) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                // 自动滚动到底部
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 命令输入区域
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 命令提示符
                Text(
                    text = "$ ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // 命令输入框
                val commandFieldValue = remember { TextFieldValue(command) }
                OutlinedTextField(
                    value = commandFieldValue,
                    onValueChange = {
                        onCommandChange(it.text)
                    },
                    placeholder = { Text(stringResource(R.string.enter_command)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Send
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !isCommandRunning,
                    singleLine = true,
                    maxLines = 1,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (command.isNotBlank() && !isCommandRunning) {
                                    onExecuteCommand(command)
                                }
                            },
                            enabled = command.isNotBlank() && !isCommandRunning
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Send,
                                contentDescription = stringResource(R.string.execute)
                            )
                        }
                    }
                )
            }
        }

        // 常用命令提示
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = "Common commands:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.padding(top = 4.dp)) {
                CommandChip(
                    text = "ls -la",
                    onClick = {
                        if (!isCommandRunning) {
                            onExecuteCommand("ls -la")
                        }
                    },
                    enabled = !isCommandRunning
                )
                CommandChip(
                    text = "ps",
                    onClick = {
                        if (!isCommandRunning) {
                            onExecuteCommand("ps")
                        }
                    },
                    enabled = !isCommandRunning
                )
                CommandChip(
                    text = "df -h",
                    onClick = {
                        if (!isCommandRunning) {
                            onExecuteCommand("df -h")
                        }
                    },
                    enabled = !isCommandRunning
                )
                CommandChip(
                    text = "top",
                    onClick = {
                        if (!isCommandRunning) {
                            onExecuteCommand("top -n 1")
                        }
                    },
                    enabled = !isCommandRunning
                )
            }
        }
    }
}

@Composable
fun CommandChip(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Chip(
        onClick = onClick,
        modifier = Modifier
            .padding(end = 8.dp)
            .height(32.dp),
        enabled = enabled
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}