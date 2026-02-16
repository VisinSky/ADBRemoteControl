package com.adbremotecontrol.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adbremotecontrol.R

@Composable
fun RemoteConnectionForm(
    ipAddress: String,
    port: String,
    isConnecting: Boolean,
    connectionError: String?,
    onIpAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.remote_connection),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // IP地址输入
            OutlinedTextField(
                value = ipAddress,
                onValueChange = onIpAddressChange,
                label = { Text(stringResource(R.string.ip_address)) },
                placeholder = { Text("192.168.1.100") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                enabled = !isConnecting
            )

            // 端口输入
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text(stringResource(R.string.port)) },
                placeholder = { Text("5555") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isConnecting
            )

            // 错误信息
            if (connectionError != null) {
                Text(
                    text = connectionError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // 连接按钮
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isConnecting && ipAddress.isNotBlank() && port.isNotBlank()
            ) {
                if (isConnecting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        Text(stringResource(R.string.connecting))
                    }
                } else {
                    Text(stringResource(R.string.connect))
                }
            }
        }
    }
}