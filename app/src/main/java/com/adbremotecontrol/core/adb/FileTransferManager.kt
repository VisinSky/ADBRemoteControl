package com.adbremotecontrol.core.adb

import android.util.Log
import com.adbremotecontrol.data.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件传输管理器，负责文件浏览和传输功能
 *
 * @property adbManager ADB管理器实例
 */
class FileTransferManager(private val adbManager: AdbManager) {

    private val TAG = "FileTransferManager"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 获取设备文件列表
     *
     * @param serialNumber 设备序列号
     * @param path 目录路径
     * @return 文件列表，如果失败返回空列表
     */
    suspend fun getFileList(serialNumber: String, path: String): List<FileInfo> = withContext(Dispatchers.IO) {
        try {
            // 使用ls命令获取文件列表，包含详细信息
            val result = adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "ls", "-la", path
            )
            
            parseFileList(result, path)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file list", e)
            emptyList()
        }
    }

    /**
     * 从设备下载文件
     *
     * @param serialNumber 设备序列号
     * @param remotePath 远程文件路径
     * @param localPath 本地文件路径
     * @return 是否下载成功
     */
    suspend fun downloadFile(serialNumber: String, remotePath: String, localPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand("-s", serialNumber, "pull", remotePath, localPath)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file", e)
            false
        }
    }

    /**
     * 上传文件到设备
     *
     * @param serialNumber 设备序列号
     * @param localPath 本地文件路径
     * @param remotePath 远程文件路径
     * @return 是否上传成功
     */
    suspend fun uploadFile(serialNumber: String, localPath: String, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand("-s", serialNumber, "push", localPath, remotePath)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file", e)
            false
        }
    }

    /**
     * 创建目录
     *
     * @param serialNumber 设备序列号
     * @param path 目录路径
     * @return 是否创建成功
     */
    suspend fun createDirectory(serialNumber: String, path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand("-s", serialNumber, "shell", "mkdir", "-p", path)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create directory", e)
            false
        }
    }

    /**
     * 删除文件或目录
     *
     * @param serialNumber 设备序列号
     * @param path 文件或目录路径
     * @param recursive 是否递归删除目录
     * @return 是否删除成功
     */
    suspend fun deleteFile(serialNumber: String, path: String, recursive: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = if (recursive) {
                "rm -rf \"$path\""
            } else {
                "rm \"$path\""
            }
            adbManager.executeAdbCommand("-s", serialNumber, "shell", command)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file", e)
            false
        }
    }

    /**
     * 重命名文件或目录
     *
     * @param serialNumber 设备序列号
     * @param oldPath 旧路径
     * @param newPath 新路径
     * @return 是否重命名成功
     */
    suspend fun renameFile(serialNumber: String, oldPath: String, newPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand("-s", serialNumber, "shell", "mv", "\"$oldPath\"", "\"$newPath\"")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename file", e)
            false
        }
    }

    /**
     * 获取文件内容
     *
     * @param serialNumber 设备序列号
     * @param path 文件路径
     * @return 文件内容，如果失败返回null
     */
    suspend fun getFileContent(serialNumber: String, path: String): String? = withContext(Dispatchers.IO) {
        try {
            adbManager.executeAdbCommand("-s", serialNumber, "shell", "cat", "\"$path\"")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file content", e)
            null
        }
    }

    /**
     * 写入文件内容
     *
     * @param serialNumber 设备序列号
     * @param path 文件路径
     * @param content 文件内容
     * @return 是否写入成功
     */
    suspend fun writeFileContent(serialNumber: String, path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 使用echo命令写入内容，需要处理特殊字符
            val escapedContent = content.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
            adbManager.executeAdbCommand(
                "-s", serialNumber, "shell", "echo", "\"$escapedContent\"", ">", "\"$path\""
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file content", e)
            false
        }
    }

    /**
     * 安装APK文件
     *
     * @param serialNumber 设备序列号
     * @param apkPath APK文件路径
     * @param reinstall 是否重新安装
     * @return 是否安装成功
     */
    suspend fun installApk(serialNumber: String, apkPath: String, reinstall: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = mutableListOf("-s", serialNumber, "install")
            if (reinstall) {
                command.add("-r")
            }
            command.add(apkPath)
            
            val result = adbManager.executeAdbCommand(*command.toTypedArray())
            result.contains("Success")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
            false
        }
    }

    /**
     * 解析文件列表输出
     */
    private fun parseFileList(output: String, parentPath: String): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        val lines = output.lines()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("total")) continue

            // 解析ls -la输出格式
            // 示例: -rw-r--r-- 1 root root 1024 Jan 1 12:00 filename.txt
            val parts = trimmedLine.split("\\s+".toRegex())
            if (parts.size < 9) continue

            val permissions = parts[0]
            val owner = parts[2]
            val group = parts[3]
            val size = parts[4].toLongOrNull() ?: 0
            val dateStr = "${parts[5]} ${parts[6]} ${parts[7]}"
            val name = parts.drop(8).joinToString(" ")

            // 跳过.和..目录
            if (name == "." || name == "..") continue

            val isDirectory = permissions.startsWith("d")
            val path = if (parentPath.endsWith("/")) {
                "$parentPath$name"
            } else {
                "$parentPath/$name"
            }

            // 解析日期
            val lastModified = try {
                dateFormat.parse(dateStr)?.time ?: 0
            } catch (e: Exception) {
                0
            }

            files.add(
                FileInfo(
                    name = name,
                    path = path,
                    isDirectory = isDirectory,
                    size = size,
                    lastModified = lastModified,
                    permissions = permissions,
                    owner = owner,
                    group = group
                )
            )
        }

        return files
    }
}