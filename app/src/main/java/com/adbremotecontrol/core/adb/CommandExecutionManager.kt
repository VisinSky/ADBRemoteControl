package com.adbremotecontrol.core.adb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 命令执行管理器，负责执行shell命令和监控设备状态
 *
 * @property adbManager ADB管理器实例
 */
class CommandExecutionManager(private val adbManager: AdbManager) {

    private val TAG = "CommandExecutionManager"

    /**
     * 执行shell命令
     *
     * @param serialNumber 设备序列号
     * @param command 要执行的命令
     * @param timeout 超时时间（秒）
     * @return 命令执行结果
     */
    suspend fun executeShellCommand(
        serialNumber: String,
        command: String,
        timeout: Long = 30
    ): String = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", command
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute shell command", e)
            "Error: ${e.message}"
        }
    }

    /**
     * 执行长时间运行的命令并实时获取输出
     *
     * @param serialNumber 设备序列号
     * @param command 要执行的命令
     * @param outputChannel 输出通道
     * @return 进程ID，用于后续终止命令
     */
    suspend fun executeLongRunningCommand(
        serialNumber: String,
        command: String,
        outputChannel: Channel<String>
    ): Int? = withContext(Dispatchers.IO) {
        try {
            // 首先执行命令并获取PID
            val pidCommand = "nohup sh -c \"$command\" > /data/local/tmp/cmd_output.txt 2>&1 & echo \\$!"
            val pidResult = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", pidCommand
            )
            
            val pid = pidResult.trim().toIntOrNull() ?: return@withContext null
            
            // 启动一个协程来读取输出
            kotlinx.coroutines.launch {
                try {
                    var lastReadPosition = 0
                    
                    while (true) {
                        // 读取文件内容
                        val catCommand = "tail -c +${lastReadPosition + 1} /data/local/tmp/cmd_output.txt"
                        val output = adbManager.executeAdbCommand(
                            "-s", serialNumber, "shell", catCommand
                        )
                        
                        if (output.isNotEmpty()) {
                            outputChannel.send(output)
                            lastReadPosition += output.length
                        }
                        
                        // 检查进程是否仍在运行
                        val psCommand = "ps -p $pid"
                        val psResult = adbManager.executeAdbCommand(
                            "-s", serialNumber, "shell", psCommand
                        )
                        
                        if (!psResult.contains(pid.toString())) {
                            // 进程已结束，发送剩余输出
                            val remainingOutput = adbManager.executeAdbCommand(
                                "-s", serialNumber, "shell", "cat /data/local/tmp/cmd_output.txt"
                            ).substring(lastReadPosition)
                            
                            if (remainingOutput.isNotEmpty()) {
                                outputChannel.send(remainingOutput)
                            }
                            
                            break
                        }
                        
                        // 短暂休眠
                        kotlinx.coroutines.delay(500)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading command output", e)
                    outputChannel.send("Error: ${e.message}")
                } finally {
                    // 清理临时文件
                    try {
                        adbManager.executeAdbCommand(
                            "-s", serialNumber, "shell", "rm /data/local/tmp/cmd_output.txt"
                        )
                    } catch (ignored: Exception) {
                    }
                    
                    outputChannel.close()
                }
            }
            
            pid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute long running command", e)
            outputChannel.send("Error: ${e.message}")
            outputChannel.close()
            null
        }
    }

    /**
     * 终止正在运行的命令
     *
     * @param serialNumber 设备序列号
     * @param pid 进程ID
     * @return 是否成功终止
     */
    suspend fun terminateCommand(serialNumber: String, pid: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "kill", pid.toString()
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to terminate command", e)
            false
        }
    }

    /**
     * 获取设备CPU使用率
     *
     * @param serialNumber 设备序列号
     * @return CPU使用率（百分比）
     */
    suspend fun getCpuUsage(serialNumber: String): Float = withContext(Dispatchers.IO) {
        try {
            // 使用top命令获取CPU使用率
            val result = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "top -n 1 | grep 'CPU:'"
            )
            
            // 解析输出，示例: CPU: 10% user + 5% kernel + 85% idle
            val cpuRegex = Regex("CPU:\\s*(\\d+)%\\s+user\\s+\\+\\s*(\\d+)%\\s+kernel")
            val matchResult = cpuRegex.find(result)
            
            if (matchResult != null) {
                val userCpu = matchResult.groupValues[1].toFloat()
                val kernelCpu = matchResult.groupValues[2].toFloat()
                userCpu + kernelCpu
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get CPU usage", e)
            0f
        }
    }

    /**
     * 获取设备内存使用情况
     *
     * @param serialNumber 设备序列号
     * @return 内存使用情况（已用/总内存，单位MB）
     */
    suspend fun getMemoryUsage(serialNumber: String): Pair<Long, Long> = withContext(Dispatchers.IO) {
        try {
            // 使用free命令获取内存使用情况
            val result = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "free -m"
            )
            
            // 解析输出
            val lines = result.lines()
            if (lines.size >= 2) {
                val memoryLine = lines[1] // Mem行
                val parts = memoryLine.split("\\s+".toRegex())
                
                if (parts.size >= 3) {
                    val totalMemory = parts[1].toLong()
                    val usedMemory = parts[2].toLong()
                    return@withContext Pair(usedMemory, totalMemory)
                }
            }
            
            Pair(0, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory usage", e)
            Pair(0, 0)
        }
    }

    /**
     * 获取设备电池状态
     *
     * @param serialNumber 设备序列号
     * @return 电池状态（百分比，是否充电）
     */
    suspend fun getBatteryStatus(serialNumber: String): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        try {
            // 获取电池电量
            val levelResult = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "cat /sys/class/power_supply/battery/capacity"
            )
            val level = levelResult.trim().toIntOrNull() ?: 0
            
            // 获取充电状态
            val statusResult = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "cat /sys/class/power_supply/battery/status"
            )
            val isCharging = statusResult.trim().equals("Charging", ignoreCase = true)
            
            Pair(level, isCharging)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery status", e)
            Pair(0, false)
        }
    }

    /**
     * 获取设备网络状态
     *
     * @param serialNumber 设备序列号
     * @return 网络状态（类型，IP地址）
     */
    suspend fun getNetworkStatus(serialNumber: String): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            // 检查WiFi状态
            val wifiResult = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "ifconfig wlan0 || ip addr show wlan0"
            )
            
            val wifiIpRegex = Regex("inet\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)")
            val wifiMatch = wifiIpRegex.find(wifiResult)
            
            if (wifiMatch != null) {
                return@withContext Pair("WiFi", wifiMatch.groupValues[1])
            }
            
            // 检查移动网络状态
            val mobileResult = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "ifconfig rmnet0 || ip addr show rmnet0"
            )
            
            val mobileMatch = wifiIpRegex.find(mobileResult)
            if (mobileMatch != null) {
                return@withContext Pair("Mobile", mobileMatch.groupValues[1])
            }
            
            Pair("None", "No network")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get network status", e)
            Pair("Unknown", "Error")
        }
    }

    /**
     * 获取已安装应用列表
     *
     * @param serialNumber 设备序列号
     * @return 应用包名列表
     */
    suspend fun getInstalledApps(serialNumber: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val result = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "pm list packages"
            )
            
            // 解析输出，示例: package:com.example.app
            result.lines()
                .map { it.trim() }
                .filter { it.startsWith("package:") }
                .map { it.substring(8) } // 移除"package:"前缀
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed apps", e)
            emptyList()
        }
    }

    /**
     * 获取应用详细信息
     *
     * @param serialNumber 设备序列号
     * @param packageName 应用包名
     * @return 应用信息
     */
    suspend fun getAppInfo(serialNumber: String, packageName: String): AppInfo? = withContext(Dispatchers.IO) {
        try {
            // 获取应用版本
            val versionResult = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "dumpsys package $packageName | grep versionName"
            )
            
            val versionRegex = Regex("versionName=(\\S+)")
            val version = versionRegex.find(versionResult)?.groupValues?.get(1) ?: "Unknown"
            
            // 获取应用安装路径
            val pathResult = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "pm path $packageName"
            )
            
            val pathRegex = Regex("package:(\\S+)")
            val path = pathRegex.find(pathResult)?.groupValues?.get(1) ?: "Unknown"
            
            // 获取应用权限
            val permissionsResult = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "dumpsys package $packageName | grep permission"
            )
            
            val permissions = permissionsResult.lines()
                .map { it.trim() }
                .filter { it.contains("permission:") }
                .map { it.substringAfterLast("permission:").trim() }
                .filter { it.isNotEmpty() }
            
            AppInfo(
                packageName = packageName,
                version = version,
                path = path,
                permissions = permissions
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get app info", e)
            null
        }
    }

    /**
     * 应用信息数据类
     */
    data class AppInfo(
        val packageName: String,
        val version: String,
        val path: String,
        val permissions: List<String>
    )
}