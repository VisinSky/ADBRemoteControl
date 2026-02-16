package com.adbremotecontrol.data.model

import java.io.Serializable

/**
 * 设备信息模型
 *
 * @property serialNumber 设备序列号
 * @property model 设备型号
 * @property manufacturer 制造商
 * @property androidVersion Android版本
 * @property deviceName 设备名称
 * @property isConnected 是否已连接
 * @property connectionType 连接类型（USB或TCP/IP）
 * @property ipAddress IP地址（TCP/IP连接时有效）
 * @property port 端口号（TCP/IP连接时有效）
 */
data class Device(
    val serialNumber: String,
    val model: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val deviceName: String = "",
    var isConnected: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.USB,
    val ipAddress: String? = null,
    val port: Int? = null
) : Serializable {

    enum class ConnectionType {
        USB,
        TCP_IP
    }

    // 设备显示名称
    val displayName: String
        get() = when {
            deviceName.isNotEmpty() -> deviceName
            model.isNotEmpty() -> model
            else -> serialNumber
        }

    // 连接信息显示
    val connectionInfo: String
        get() = when (connectionType) {
            ConnectionType.USB -> "USB: $serialNumber"
            ConnectionType.TCP_IP -> "TCP/IP: ${ipAddress}:${port}"
        }
}