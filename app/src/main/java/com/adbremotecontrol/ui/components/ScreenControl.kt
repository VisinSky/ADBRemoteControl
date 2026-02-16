package com.adbremotecontrol.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adbremotecontrol.R
import com.adbremotecontrol.core.adb.ScreenControlManager

@Composable
fun ScreenControl(
    screenBitmap: Bitmap?,
    isScreenRecording: Boolean,
    screenRecordingProgress: Int,
    onTakeScreenshot: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTouchEvent: (Int, Int, ScreenControlManager.TouchAction) -> Unit,
    onKeyEvent: (ScreenControlManager.KeyCode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 屏幕显示区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            if (screenBitmap != null) {
                // 屏幕镜像
                ScreenMirror(
                    bitmap = screenBitmap,
                    onTouchEvent = onTouchEvent
                )
            } else {
                // 占位符
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Smartphone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No screen mirror",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap 'Screenshot' to capture screen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 控制按钮区域
        Column {
            // 屏幕录制进度条
            if (isScreenRecording) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recording...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "$screenRecordingProgress%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    LinearProgressIndicator(
                        progress = screenRecordingProgress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 第一行按钮：截图、录制
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onTakeScreenshot,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.screenshot))
                }

                Button(
                    onClick = if (isScreenRecording) onStopRecording else onStartRecording,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScreenRecording) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isScreenRecording) {
                            androidx.compose.material.icons.Icons.Default.Stop
                        } else {
                            androidx.compose.material.icons.Icons.Default.Videocam
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isScreenRecording) {
                            stringResource(R.string.stop)
                        } else {
                            stringResource(R.string.record_screen)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 第二行按钮：导航键
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { onKeyEvent(ScreenControlManager.KeyCode.HOME) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Home,
                        contentDescription = "Home"
                    )
                }

                IconButton(
                    onClick = { onKeyEvent(ScreenControlManager.KeyCode.BACK) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                IconButton(
                    onClick = { onKeyEvent(ScreenControlManager.KeyCode.MENU) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }

                IconButton(
                    onClick = { onKeyEvent(ScreenControlManager.KeyCode.VOLUME_UP) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.VolumeUp,
                        contentDescription = "Volume Up"
                    )
                }

                IconButton(
                    onClick = { onKeyEvent(ScreenControlManager.KeyCode.VOLUME_DOWN) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.VolumeDown,
                        contentDescription = "Volume Down"
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenMirror(
    bitmap: Bitmap,
    onTouchEvent: (Int, Int, ScreenControlManager.TouchAction) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = maxOf(1f, minOf(scale * zoom, 3f))
                    offsetX = pan.x
                    offsetY = pan.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        // 计算相对于位图的坐标
                        val bitmapX = ((offset.x - offsetX) / scale * bitmap.width / size.width).toInt()
                        val bitmapY = ((offset.y - offsetY) / scale * bitmap.height / size.height).toInt()
                        
                        // 发送按下事件
                        onTouchEvent(bitmapX, bitmapY, ScreenControlManager.TouchAction.DOWN)
                        
                        // 等待释放
                        awaitRelease()
                        
                        // 发送抬起事件
                        onTouchEvent(bitmapX, bitmapY, ScreenControlManager.TouchAction.UP)
                    }
                )
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Screen mirror",
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .offset { androidx.compose.ui.geometry.Offset(offsetX, offsetY) },
            contentScale = ContentScale.Fit
        )
    }
}