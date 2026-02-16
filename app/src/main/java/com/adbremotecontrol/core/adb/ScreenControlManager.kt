package com.adbremotecontrol.core.adb

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * 屏幕控制管理器，负责屏幕镜像和触控操作
 *
 * @property adbManager ADB管理器实例
 */
class ScreenControlManager(private val adbManager: AdbManager) {

    private val TAG = "ScreenControlManager"

    /**
     * 获取设备屏幕截图
     *
     * @param serialNumber 设备序列号
     * @return 屏幕截图Bitmap，如果失败返回null
     */
    suspend fun takeScreenshot(serialNumber: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 使用ADB命令获取屏幕截图
            val screenshotPath = "/data/local/tmp/screenshot.png"
            adbManager.executeAdbCommand("-s", serialNumber, "shell", "screencap", "-p", screenshotPath)
            
            // 将截图从设备拉取到本地
            val localPath = File.createTempFile("screenshot", ".png").absolutePath
            adbManager.executeAdbCommand("-s", serialNumber, "pull", screenshotPath, localPath)
            
            // 读取并返回Bitmap
            val bitmap = BitmapFactory.decodeFile(localPath)
            
            // 删除临时文件
            File(localPath).delete()
            adbManager.executeAdbCommand("-s", serialNumber, "shell", "rm", screenshotPath)
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
            null
        }
    }

    /**
     * 开始屏幕录制
     *
     * @param serialNumber 设备序列号
     * @param outputPath 输出文件路径
     * @param duration 录制时长（秒）
     * @param width 视频宽度
     * @param height 视频高度
     * @param bitRate 比特率（bps）
     * @return 是否成功开始录制
     */
    suspend fun startScreenRecording(
        serialNumber: String,
        outputPath: String = "/data/local/tmp/screenrecord.mp4",
        duration: Int = 60,
        width: Int = 0,
        height: Int = 0,
        bitRate: Int = 4000000
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = mutableListOf("shell", "screenrecord")
            
            // 添加录制参数
            command.add("--time-limit")
            command.add(duration.toString())
            
            if (width > 0 && height > 0) {
                command.add("--size")
                command.add("$width×$height")
            }
            
            command.add("--bit-rate")
            command.add(bitRate.toString())
            
            command.add(outputPath)
            
            // 在后台执行录制命令
            adbManager.executeAdbCommand("-s", serialNumber, *command.toTypedArray())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording", e)
            false
        }
    }

    /**
     * 停止屏幕录制
     *
     * @param serialNumber 设备序列号
     * @return 是否成功停止录制
     */
    suspend fun stopScreenRecording(serialNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 查找并终止screenrecord进程
            adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "pkill", "-f", "screenrecord"
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop screen recording", e)
            false
        }
    }

    /**
     * 下载录制的视频文件
     *
     * @param serialNumber 设备序列号
     * @param remotePath 远程文件路径
     * @param localPath 本地文件路径
     * @return 是否成功下载
     */
    suspend fun downloadRecording(
        serialNumber: String,
        remotePath: String = "/data/local/tmp/screenrecord.mp4",
        localPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand("-s", serialNumber, "pull", remotePath, localPath)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download recording", e)
            false
        }
    }

    /**
     * 发送触摸事件
     *
     * @param serialNumber 设备序列号
     * @param x X坐标
     * @param y Y坐标
     * @param action 触摸动作（DOWN, MOVE, UP）
     * @return 是否成功发送
     */
    suspend fun sendTouchEvent(
        serialNumber: String,
        x: Int,
        y: Int,
        action: TouchAction
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val actionCode = when (action) {
                TouchAction.DOWN -> 0
                TouchAction.MOVE -> 2
                TouchAction.UP -> 1
            }
            
            adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "input", "tap", x.toString(), y.toString()
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send touch event", e)
            false
        }
    }

    /**
     * 发送滑动事件
     *
     * @param serialNumber 设备序列号
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 结束X坐标
     * @param endY 结束Y坐标
     * @param duration 滑动持续时间（毫秒）
     * @return 是否成功发送
     */
    suspend fun sendSwipeEvent(
        serialNumber: String,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Int = 300
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "input", "swipe",
                startX.toString(), startY.toString(),
                endX.toString(), endY.toString(),
                duration.toString()
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send swipe event", e)
            false
        }
    }

    /**
     * 发送按键事件
     *
     * @param serialNumber 设备序列号
     * @param keyCode 按键代码
     * @return 是否成功发送
     */
    suspend fun sendKeyEvent(serialNumber: String, keyCode: KeyCode): Boolean = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "input", "keyevent", keyCode.code.toString()
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send key event", e)
            false
        }
    }

    /**
     * 发送文本输入事件
     *
     * @param serialNumber 设备序列号
     * @param text 要输入的文本
     * @return 是否成功发送
     */
    suspend fun sendTextInput(serialNumber: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 对文本进行转义，处理特殊字符
            val escapedText = text.replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("`", "\\`")
            
            adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "input", "text", escapedText
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send text input", e)
            false
        }
    }

    /**
     * 触摸动作枚举
     */
    enum class TouchAction {
        DOWN,
        MOVE,
        UP
    }

    /**
     * 按键代码枚举
     */
    enum class KeyCode(val code: Int) {
        HOME(3),
        BACK(4),
        MENU(82),
        POWER(26),
        VOLUME_UP(24),
        VOLUME_DOWN(25),
        CAMERA(27),
        SEARCH(84),
        ENTER(66),
        DELETE(67),
        SPACE(62),
        TAB(61),
        SYM(63),
        ENDCALL(6),
        CALL(5),
        STAR(17),
        POUND(18),
        DPAD_UP(19),
        DPAD_DOWN(20),
        DPAD_LEFT(21),
        DPAD_RIGHT(22),
        DPAD_CENTER(23)
    }
}