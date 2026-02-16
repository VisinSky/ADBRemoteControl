package com.adbremotecontrol.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adbremotecontrol.R
import com.adbremotecontrol.ui.components.*
import com.adbremotecontrol.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedDevice = viewModel.selectedDevice
    val deviceMonitorData by viewModel.deviceMonitorData.collectAsState()

    // 当前选中的标签页
    var selectedTab by remember { mutableStateOf(MainTab.SCREEN_CONTROL) }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧设备列表
        DeviceList(
            devices = viewModel.devices,
            selectedDevice = selectedDevice,
            onSelectDevice = { viewModel.selectDevice(it) },
            onDisconnectDevice = { viewModel.disconnectDevice(it) },
            isLoading = viewModel.isConnecting,
            onRefresh = { viewModel.scanDevices() }
        )

        // 右侧主内容区域
        Column(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
            if (selectedDevice == null) {
                // 未选择设备时显示远程连接表单
                RemoteConnectionForm(
                    ipAddress = viewModel.remoteIp,
                    port = viewModel.remotePort,
                    isConnecting = viewModel.isConnecting,
                    connectionError = viewModel.connectionError,
                    onIpAddressChange = { viewModel.updateRemoteConnectionForm(it, viewModel.remotePort) },
                    onPortChange = { viewModel.updateRemoteConnectionForm(viewModel.remoteIp, it) },
                    onConnect = { viewModel.connectRemoteDevice() }
                )
            } else {
                // 选择设备后显示控制界面
                Column(modifier = Modifier.fillMaxSize()) {
                    // 设备信息标题栏
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedDevice.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                                )
                                Text(
                                    text = selectedDevice.connectionInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { viewModel.disconnectDevice(selectedDevice.serialNumber) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.disconnect))
                            }
                        }
                    }

                    // 标签页导航
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MainTab.values().forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(stringResource(tab.titleResId)) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.titleResId)
                                    )
                                }
                            )
                        }
                    }

                    // 标签页内容
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)) {
                        when (selectedTab) {
                            MainTab.SCREEN_CONTROL -> {
                                ScreenControl(
                                    screenBitmap = viewModel.screenBitmap,
                                    isScreenRecording = viewModel.isScreenRecording,
                                    screenRecordingProgress = viewModel.screenRecordingProgress,
                                    onTakeScreenshot = { viewModel.takeScreenshot() },
                                    onStartRecording = { viewModel.startScreenRecording() },
                                    onStopRecording = { viewModel.stopScreenRecording() },
                                    onTouchEvent = { x, y, action ->
                                        viewModel.sendTouchEvent(x, y, action)
                                    },
                                    onKeyEvent = { keyCode ->
                                        viewModel.sendKeyEvent(keyCode)
                                    }
                                )
                            }
                            MainTab.FILE_MANAGER -> {
                                FileManager(
                                    currentPath = viewModel.currentRemotePath,
                                    files = viewModel.remoteFiles,
                                    isLoading = viewModel.isLoadingFiles,
                                    onFileClick = { file ->
                                        if (file.isDirectory) {
                                            viewModel.loadRemoteFiles(file.path)
                                        }
                                    },
                                    onParentClick = {
                                        val parentPath = viewModel.currentRemotePath.substringBeforeLast('/', "")
                                        viewModel.loadRemoteFiles(if (parentPath.isEmpty()) "/" else parentPath)
                                    },
                                    onRefresh = { viewModel.loadRemoteFiles() }
                                )
                            }
                            MainTab.TERMINAL -> {
                                Terminal(
                                    command = viewModel.terminalCommand,
                                    output = viewModel.terminalOutput,
                                    isCommandRunning = viewModel.terminalCommandPid != null,
                                    onCommandChange = { viewModel.updateTerminalCommand(it) },
                                    onExecuteCommand = {
                                        if (it.trim().endsWith("&")) {
                                            // 长时间运行的命令
                                            viewModel.executeLongRunningTerminalCommand(it.trim().removeSuffix("&").trim())
                                        } else {
                                            // 普通命令
                                            viewModel.executeTerminalCommand(it)
                                        }
                                    },
                                    onStopCommand = { viewModel.stopLongRunningCommand() }
                                )
                            }
                            MainTab.DEVICE_MONITOR -> {
                                DeviceMonitor(monitorData = deviceMonitorData)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 主界面标签页枚举
enum class MainTab(val titleResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SCREEN_CONTROL(R.string.screen_control, androidx.compose.material.icons.Icons.Default.Smartphone),
    FILE_MANAGER(R.string.file_manager, androidx.compose.material.icons.Icons.Default.Folder),
    TERMINAL(R.string.terminal, androidx.compose.material.icons.Icons.Default.Terminal),
    DEVICE_MONITOR(R.string.device_info, androidx.compose.material.icons.Icons.Default.Info)
}