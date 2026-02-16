package com.adbremotecontrol.core.adb

import android.content.Context
import android.util.Log
import com.adbremotecontrol.data.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * ADB管理器，负责ADB命令的执行和设备管理
 *
 * @property context 上下文
 */
class AdbManager(private val context: Context) {

    private val TAG = "AdbManager"
    private val adbPath: String by lazy {
        // 获取ADB可执行文件路径
        val adbFile = File(context.filesDir, "adb")
        if (!adbFile.exists()) {
            // 从assets复制ADB可执行文件到应用目录
            copyAdbExecutable(adbFile)
        }
        adbFile.absolutePath
    }

    /**
     * 初始化ADB
     */
    suspend fun initializeAdb(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 设置ADB可执行权限
            executeCommand("chmod", "+x", adbPath)
            // 启动ADB服务器
            executeAdbCommand("start-server")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ADB", e)
            false
        }
    }

    /**
     * 获取已连接的设备列表
     */
    suspend fun getConnectedDevices(): List<Device> = withContext(Dispatchers.IO) {
        try {
            val result = executeAdbCommand("devices", "-l")
            parseDeviceList(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get connected devices", e)
            emptyList()
        }
    }

    /**
     * 连接远程设备
     */
    suspend fun connectRemoteDevice(ipAddress: String, port: Int = 5555): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = executeAdbCommand("connect", "$ipAddress:$port")
            result.contains("connected to")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to remote device", e)
            false
        }
    }

    /**
     * 断开设备连接
     */
    suspend fun disconnectDevice(serialNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            executeAdbCommand("disconnect", serialNumber)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect device", e)
            false
        }
    }

    /**
     * 获取设备信息
     */
    suspend fun getDeviceInfo(serialNumber: String): Device? = withContext(Dispatchers.IO) {
        try {
            val model = executeAdbCommand("-s", serialNumber, "shell", "getprop", "ro.product.model")
            val manufacturer = executeAdbCommand("-s", serialNumber, "shell", "getprop", "ro.product.manufacturer")
            val androidVersion = executeAdbCommand("-s", serialNumber, "shell", "getprop", "ro.build.version.release")
            val deviceName = executeAdbCommand("-s", serialNumber, "shell", "getprop", "ro.product.name")

            // 判断连接类型
            val connectionType = if (serialNumber.contains(":")) {
                Device.ConnectionType.TCP_IP
            } else {
                Device.ConnectionType.USB
            }

            // 解析IP地址和端口
            var ipAddress: String? = null
            var port: Int? = null
            if (connectionType == Device.ConnectionType.TCP_IP) {
                val parts = serialNumber.split(":")
                if (parts.size == 2) {
                    ipAddress = parts[0]
                    port = parts[1].toIntOrNull()
                }
            }

            Device(
                serialNumber = serialNumber,
                model = model.trim(),
                manufacturer = manufacturer.trim(),
                androidVersion = androidVersion.trim(),
                deviceName = deviceName.trim(),
                isConnected = true,
                connectionType = connectionType,
                ipAddress = ipAddress,
                port = port
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get device info", e)
            null
        }
    }

    /**
     * 执行ADB命令
     */
    private suspend fun executeAdbCommand(vararg args: String): String = withContext(Dispatchers.IO) {
        val command = mutableListOf(adbPath)
        command.addAll(args)
        executeCommand(*command.toTypedArray())
    }

    /**
     * 执行系统命令
     */
    private suspend fun executeCommand(vararg args: String): String = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(*args)
            .redirectErrorStream(true)
            .start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        process.waitFor(10, TimeUnit.SECONDS)
        reader.close()

        output.toString()
    }

    /**
     * 解析设备列表输出
     */
    private fun parseDeviceList(output: String): List<Device> {
        val devices = mutableListOf<Device>()
        val lines = output.lines()

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val parts = line.split("\\s+".toRegex())
            if (parts.size < 2) continue

            val serialNumber = parts[0]
            val status = parts[1]

            if (status == "device") {
                val device = Device(
                    serialNumber = serialNumber,
                    isConnected = true,
                    connectionType = if (serialNumber.contains(":")) {
                        Device.ConnectionType.TCP_IP
                    } else {
                        Device.ConnectionType.USB
                    }
                )
                devices.add(device)
            }
        }

        return devices
    }

    /**
     * 从assets复制ADB可执行文件
     */
    private fun copyAdbExecutable(destination: File) {
        try {
            val inputStream = context.assets.open("adb")
            val outputStream = destination.outputStream()
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy ADB executable", e)
            throw e
        }
    }

    /**
     * 清理ADB资源
     */
    fun cleanup() {
        try {
            executeCommand(adbPath, "kill-server")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill ADB server", e)
        }
    }
}