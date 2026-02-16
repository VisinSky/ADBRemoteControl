package com.adbremotecontrol.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adbremotecontrol.core.adb.AdbManager
import com.adbremotecontrol.core.adb.CommandExecutionManager
import com.adbremotecontrol.core.adb.FileTransferManager
import com.adbremotecontrol.core.adb.ScreenControlManager
import com.adbremotecontrol.data.model.Device
import com.adbremotecontrol.data.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"

    // ADB管理器实例
    private val adbManager = AdbManager(application)
    private val screenControlManager = ScreenControlManager(adbManager)
    private val fileTransferManager = FileTransferManager(adbManager)
    private val commandExecutionManager = CommandExecutionManager(adbManager)

    // 设备列表状态
    val devices = mutableStateListOf<Device>()
    var selectedDevice by mutableStateOf<Device?>(null)
        private set

    // 连接状态
    var isConnecting by mutableStateOf(false)
        private set
    var connectionError by mutableStateOf<String?>(null)
        private set

    // 远程连接表单
    var remoteIp by mutableStateOf("")
        private set
    var remotePort by mutableStateOf("5555")
        private set

    // 屏幕控制状态
    var screenBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var isScreenRecording by mutableStateOf(false)
        private set
    var screenRecordingProgress by mutableStateOf(0)
        private set

    // 文件管理状态
    var currentRemotePath by mutableStateOf("/")
        private set
    val remoteFiles = mutableStateListOf<FileInfo>()
    var isLoadingFiles by mutableStateOf(false)
        private set

    // 终端状态
    var terminalCommand by mutableStateOf("")
        private set
    val terminalOutput = mutableStateListOf<String>()
    private var terminalCommandChannel: Channel<String>? = null
    private var terminalCommandPid: Int? = null

    // 设备监控状态
    private val _deviceMonitorData = MutableStateFlow(DeviceMonitorData())
    val deviceMonitorData: StateFlow<DeviceMonitorData> = _deviceMonitorData.asStateFlow()

    // 初始化
    init {
        initializeAdb()
    }

    // 初始化ADB
    private fun initializeAdb() {
        viewModelScope.launch {
            try {
                isConnecting = true
                connectionError = null

                val success = adbManager.initializeAdb()
                if (success) {
                    scanDevices()
                } else {
                    connectionError = "Failed to initialize ADB"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ADB", e)
                connectionError = "Error: ${e.message}"
            } finally {
                isConnecting = false
            }
        }
    }

    // 扫描设备
    fun scanDevices() {
        viewModelScope.launch {
            try {
                isConnecting = true
                connectionError = null

                val deviceList = adbManager.getConnectedDevices()
                
                // 更新设备列表
                devices.clear()
                devices.addAll(deviceList)
                
                // 获取每个设备的详细信息
                for (device in devices) {
                    val deviceInfo = adbManager.getDeviceInfo(device.serialNumber)
                    if (deviceInfo != null) {
                        val index = devices.indexOf(device)
                        if (index != -1) {
                            devices[index] = deviceInfo
                        }
                    }
                }
                
                // 如果之前选中的设备仍然连接，保持选中状态
                if (selectedDevice != null && !devices.any { it.serialNumber == selectedDevice?.serialNumber }) {
                    selectedDevice = null
                    resetDeviceSpecificState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning devices", e)
                connectionError = "Error scanning devices: ${e.message}"
            } finally {
                isConnecting = false
            }
        }
    }

    // 连接远程设备
    fun connectRemoteDevice() {
        if (remoteIp.isBlank()) {
            connectionError = "IP address cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                isConnecting = true
                connectionError = null

                val port = remotePort.toIntOrNull() ?: 5555
                val success = adbManager.connectRemoteDevice(remoteIp, port)
                
                if (success) {
                    // 连接成功后扫描设备
                    scanDevices()
                } else {
                    connectionError = "Failed to connect to remote device"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to remote device", e)
                connectionError = "Error: ${e.message}"
            } finally {
                isConnecting = false
            }
        }
    }

    // 断开设备连接
    fun disconnectDevice(serialNumber: String) {
        viewModelScope.launch {
            try {
                isConnecting = true
                
                val success = adbManager.disconnectDevice(serialNumber)
                if (success) {
                    // 如果断开的是当前选中的设备，清除选中状态
                    if (selectedDevice?.serialNumber == serialNumber) {
                        selectedDevice = null
                        resetDeviceSpecificState()
                    }
                    
                    // 重新扫描设备
                    scanDevices()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting device", e)
            } finally {
                isConnecting = false
            }
        }
    }

    // 选择设备
    fun selectDevice(device: Device) {
        selectedDevice = device
        resetDeviceSpecificState()
        
        // 加载远程文件列表
        loadRemoteFiles()
        
        // 开始监控设备状态
        startDeviceMonitoring()
    }

    // 重置设备特定状态
    private fun resetDeviceSpecificState() {
        screenBitmap = null
        isScreenRecording = false
        screenRecordingProgress = 0
        currentRemotePath = "/"
        remoteFiles.clear()
        terminalOutput.clear()
        terminalCommandPid = null
        terminalCommandChannel?.close()
        terminalCommandChannel = null
    }

    // 更新远程连接表单
    fun updateRemoteConnectionForm(ip: String, port: String) {
        remoteIp = ip
        remotePort = port
    }

    // 截取屏幕截图
    fun takeScreenshot() {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            try {
                val bitmap = screenControlManager.takeScreenshot(device.serialNumber)
                screenBitmap = bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error taking screenshot", e)
            }
        }
    }

    // 开始屏幕录制
    fun startScreenRecording() {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            try {
                val success = screenControlManager.startScreenRecording(device.serialNumber)
                if (success) {
                    isScreenRecording = true
                    screenRecordingProgress = 0
                    
                    // 模拟录制进度
                    viewModelScope.launch {
                        while (isScreenRecording && screenRecordingProgress < 100) {
                            withContext(Dispatchers.Main) {
                                screenRecordingProgress += 1
                            }
                            kotlinx.coroutines.delay(1000)
                        }
                        
                        // 如果进度达到100%，自动停止录制
                        if (isScreenRecording && screenRecordingProgress >= 100) {
                            stopScreenRecording()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting screen recording", e)
            }
        }
    }

    // 停止屏幕录制
    fun stopScreenRecording() {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            try {
                val success = screenControlManager.stopScreenRecording(device.serialNumber)
                if (success) {
                    isScreenRecording = false
                    
                    // 下载录制文件
                    val localPath = File(getApplication<Application>().externalCacheDir, "screenrecord_${System.currentTimeMillis()}.mp4").absolutePath
                    screenControlManager.downloadRecording(device.serialNumber, "/data/local/tmp/screenrecord.mp4", localPath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping screen recording", e)
            }
        }
    }

    // 发送触摸事件
    fun sendTouchEvent(x: Int, y: Int, action: ScreenControlManager.TouchAction) {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            try {
                screenControlManager.sendTouchEvent(device.serialNumber, x, y, action)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending touch event", e)
            }
        }
    }

    // 发送按键事件
    fun sendKeyEvent(keyCode: ScreenControlManager.KeyCode) {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            try {
                screenControlManager.sendKeyEvent(device.serialNumber, keyCode)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending key event", e)
            }
        }
    }

    // 加载远程文件列表
    fun loadRemoteFiles(path: String? = null) {
        val device = selectedDevice ?: return
        val newPath = path ?: currentRemotePath
        
        viewModelScope.launch {
            try {
                isLoadingFiles = true
                
                val files = fileTransferManager.getFileList(device.serialNumber, newPath)
                remoteFiles.clear()
                remoteFiles.addAll(files)
                
                currentRemotePath = newPath
            } catch (e: Exception) {
                Log.e(TAG, "Error loading remote files", e)
            } finally {
                isLoadingFiles = false
            }
        }
    }

    // 执行终端命令
    fun executeTerminalCommand(command: String) {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            try {
                terminalOutput.add("> $command")
                terminalCommand = ""
                
                val result = commandExecutionManager.executeShellCommand(device.serialNumber, command)
                terminalOutput.add(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing terminal command", e)
                terminalOutput.add("Error: ${e.message}")
            }
        }
    }

    // 执行长时间运行的终端命令
    @OptIn(ExperimentalCoroutinesApi::class)
    fun executeLongRunningTerminalCommand(command: String) {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            try {
                terminalOutput.add("> $command (running...)")
                terminalCommand = ""
                
                // 创建新的通道
                terminalCommandChannel?.close()
                val channel = Channel<String>()
                terminalCommandChannel = channel
                
                // 执行命令
                val pid = commandExecutionManager.executeLongRunningCommand(device.serialNumber, command, channel)
                terminalCommandPid = pid
                
                // 收集输出
                channel.collectLatest { output ->
                    terminalOutput.add(output)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing long running terminal command", e)
                terminalOutput.add("Error: ${e.message}")
            }
        }
    }

    // 停止长时间运行的命令
    fun stopLongRunningCommand() {
        val device = selectedDevice ?: return
        val pid = terminalCommandPid ?: return
        
        viewModelScope.launch {
            try {
                commandExecutionManager.terminateCommand(device.serialNumber, pid)
                terminalOutput.add("Command terminated")
                terminalCommandPid = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping long running command", e)
            }
        }
    }

    // 开始设备监控
    private fun startDeviceMonitoring() {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            while (selectedDevice?.serialNumber == device.serialNumber) {
                try {
                    // 获取CPU使用率
                    val cpuUsage = commandExecutionManager.getCpuUsage(device.serialNumber)
                    
                    // 获取内存使用情况
                    val (usedMemory, totalMemory) = commandExecutionManager.getMemoryUsage(device.serialNumber)
                    
                    // 获取电池状态
                    val (batteryLevel, isCharging) = commandExecutionManager.getBatteryStatus(device.serialNumber)
                    
                    // 获取网络状态
                    val (networkType, ipAddress) = commandExecutionManager.getNetworkStatus(device.serialNumber)
                    
                    // 更新监控数据
                    _deviceMonitorData.value = DeviceMonitorData(
                        cpuUsage = cpuUsage,
                        usedMemory = usedMemory,
                        totalMemory = totalMemory,
                        batteryLevel = batteryLevel,
                        isCharging = isCharging,
                        networkType = networkType,
                        ipAddress = ipAddress
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error monitoring device", e)
                }
                
                // 每5秒更新一次
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    // 更新终端命令
    fun updateTerminalCommand(command: String) {
        terminalCommand = command
    }

    // 清理资源
    override fun onCleared() {
        super.onCleared()
        adbManager.cleanup()
        terminalCommandChannel?.close()
    }

    // 设备监控数据类
    data class DeviceMonitorData(
        val cpuUsage: Float = 0f,
        val usedMemory: Long = 0,
        val totalMemory: Long = 0,
        val batteryLevel: Int = 0,
        val isCharging: Boolean = false,
        val networkType: String = "None",
        val ipAddress: String = "No network"
    )
}