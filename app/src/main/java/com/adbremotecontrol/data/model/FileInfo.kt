package com.adbremotecontrol.data.model

import java.io.Serializable

/**
 * 文件信息模型
 *
 * @property name 文件名
 * @property path 文件路径
 * @property isDirectory 是否为目录
 * @property size 文件大小（字节）
 * @property lastModified 最后修改时间
 * @property permissions 文件权限
 * @property owner 所有者
 * @property group 所属组
 */
data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val permissions: String = "",
    val owner: String = "",
    val group: String = ""
) : Serializable {

    // 文件扩展名
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")

    // 格式化的文件大小
    val formattedSize: String
        get() = if (isDirectory) "" else formatFileSize(size)

    // 格式化的最后修改时间
    val formattedLastModified: String
        get() = formatLastModified(lastModified)

    companion object {
        private fun formatFileSize(bytes: Long): String {
            if (bytes < 0) return "0 B"
            if (bytes == 0L) return "0 B"
            
            val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
            val unitIndex = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val size = bytes / Math.pow(1024.0, unitIndex.toDouble())
            
            return String.format("%.2f %s", size, units[unitIndex])
        }
        
        private fun formatLastModified(timestamp: Long): String {
            if (timestamp == 0L) return ""
            
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            return format.format(date)
        }
    }
}