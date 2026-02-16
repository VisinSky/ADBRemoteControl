package com.adbremotecontrol.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbremotecontrol.R
import com.adbremotecontrol.ui.viewmodel.MainViewModel

@Composable
fun DeviceMonitor(monitorData: MainViewModel.DeviceMonitorData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = stringResource(R.string.device_info),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 监控卡片网格
        Column(modifier = Modifier.fillMaxWidth()) {
            // 第一行
            Row(modifier = Modifier.fillMaxWidth()) {
                // CPU使用率
                Box(modifier = Modifier.weight(1f)) {
                    MonitorCard(
                        title = stringResource(R.string.cpu_usage),
                        value = "${monitorData.cpuUsage.toInt()}%",
                        icon = androidx.compose.material.icons.Icons.Default.Cpu,
                        progress = monitorData.cpuUsage / 100f,
                        progressColor = when {
                            monitorData.cpuUsage > 80f -> MaterialTheme.colorScheme.error
                            monitorData.cpuUsage > 50f -> MaterialTheme.colorScheme.warning
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }

                // 内存使用情况
                Box(modifier = Modifier.weight(1f)) {
                    MonitorCard(
                        title = stringResource(R.string.memory_usage),
                        value = "${monitorData.usedMemory} MB / ${monitorData.totalMemory} MB",
                        icon = androidx.compose.material.icons.Icons.Default.Memory,
                        progress = if (monitorData.totalMemory > 0) {
                            monitorData.usedMemory.toFloat() / monitorData.totalMemory.toFloat()
                        } else {
                            0f
                        }
                    )
                }
            }

            // 第二行
            Row(modifier = Modifier.fillMaxWidth()) {
                // 电池状态
                Box(modifier = Modifier.weight(1f)) {
                    MonitorCard(
                        title = stringResource(R.string.battery_level),
                        value = "${monitorData.batteryLevel}%",
                        icon = if (monitorData.isCharging) {
                            androidx.compose.material.icons.Icons.Default.BatteryChargingFull
                        } else {
                            getBatteryIcon(monitorData.batteryLevel)
                        },
                        progress = monitorData.batteryLevel.toFloat() / 100f,
                        progressColor = when {
                            monitorData.batteryLevel < 20 -> MaterialTheme.colorScheme.error
                            monitorData.batteryLevel < 50 -> MaterialTheme.colorScheme.warning
                            else -> MaterialTheme.colorScheme.success
                        }
                    )
                }

                // 网络状态
                Box(modifier = Modifier.weight(1f)) {
                    MonitorCard(
                        title = stringResource(R.string.network_status),
                        value = "${monitorData.networkType}: ${monitorData.ipAddress}",
                        icon = when (monitorData.networkType) {
                            "WiFi" -> androidx.compose.material.icons.Icons.Default.Wifi
                            "Mobile" -> androidx.compose.material.icons.Icons.Default.Cell
                            else -> androidx.compose.material.icons.Icons.Default.SignalWifiOff
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MonitorCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    progress: Float? = null,
    progressColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = progressColor
                )
            }
        }
    }
}

// 根据电池电量返回对应的图标
private fun getBatteryIcon(level: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        level >= 90 -> androidx.compose.material.icons.Icons.Default.BatteryFull
        level >= 70 -> androidx.compose.material.icons.Icons.Default.BatteryThreeQuarter
        level >= 50 -> androidx.compose.material.icons.Icons.Default.BatteryHalf
        level >= 30 -> androidx.compose.material.icons.Icons.Default.BatteryQuarter
        else -> androidx.compose.material.icons.Icons.Default.BatteryAlert
    }
}

// 简单的网格布局
@Composable
fun Grid(columns: Int, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}