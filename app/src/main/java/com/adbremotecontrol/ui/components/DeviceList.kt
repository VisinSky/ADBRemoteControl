package com.adbremotecontrol.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbremotecontrol.R
import com.adbremotecontrol.data.model.Device

@Composable
fun DeviceList(
    devices: List<Device>,
    selectedDevice: Device?,
    onSelectDevice: (Device) -> Unit,
    onDisconnectDevice: (String) -> Unit,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        // 标题和刷新按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.device_list),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 设备列表
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_devices_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(devices) { device ->
                    DeviceListItem(
                        device = device,
                        isSelected = selectedDevice?.serialNumber == device.serialNumber,
                        onSelect = onSelectDevice,
                        onDisconnect = onDisconnectDevice
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceListItem(
    device: Device,
    isSelected: Boolean,
    onSelect: (Device) -> Unit,
    onDisconnect: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        onClick = { onSelect(device) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设备图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .rounded(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (device.connectionType) {
                        Device.ConnectionType.USB -> androidx.compose.material.icons.Icons.Default.Usb
                        Device.ConnectionType.TCP_IP -> androidx.compose.material.icons.Icons.Default.Wifi
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 设备信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.connectionInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (device.androidVersion.isNotEmpty()) {
                    Text(
                        text = "Android ${device.androidVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 断开连接按钮
            IconButton(
                onClick = { onDisconnect(device.serialNumber) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Disconnect,
                    contentDescription = stringResource(R.string.disconnect)
                )
            }
        }
    }
}