package com.adbremotecontrol.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbremotecontrol.R
import com.adbremotecontrol.data.model.FileInfo

@Composable
fun FileManager(
    currentPath: String,
    files: List<FileInfo>,
    isLoading: Boolean,
    onFileClick: (FileInfo) -> Unit,
    onParentClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题和刷新按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.file_manager),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 当前路径
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 文件列表
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // 父目录项
                if (currentPath != "/") {
                    item {
                        FileItem(
                            fileInfo = FileInfo(
                                name = ".. (Parent Directory)",
                                path = currentPath.substringBeforeLast('/', ""),
                                isDirectory = true
                            ),
                            onClick = onParentClick
                        )
                    }
                }

                // 文件列表
                if (files.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No files found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(files) { file ->
                        FileItem(fileInfo = file, onClick = { onFileClick(file) })
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(fileInfo: FileInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标
            Icon(
                imageVector = if (fileInfo.isDirectory) {
                    androidx.compose.material.icons.Icons.Default.Folder
                } else {
                    getFileIcon(fileInfo.extension)
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (fileInfo.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 文件名
            Text(
                text = fileInfo.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // 文件大小或目录标记
            if (fileInfo.isDirectory) {
                Text(
                    text = "DIR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (fileInfo.formattedSize.isNotEmpty()) {
                Text(
                    text = fileInfo.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 根据文件扩展名返回对应的图标
private fun getFileIcon(extension: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (extension.lowercase()) {
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> androidx.compose.material.icons.Icons.Default.Image
        "mp4", "avi", "mov", "mkv", "flv", "wmv" -> androidx.compose.material.icons.Icons.Default.Movie
        "mp3", "wav", "ogg", "flac", "aac", "m4a" -> androidx.compose.material.icons.Icons.Default.MusicNote
        "pdf" -> androidx.compose.material.icons.Icons.Default.PictureAsPdf
        "doc", "docx" -> androidx.compose.material.icons.Icons.Default.Description
        "xls", "xlsx" -> androidx.compose.material.icons.Icons.Default.Spreadsheet
        "ppt", "pptx" -> androidx.compose.material.icons.Icons.Default.Slideshow
        "txt", "md", "csv", "json", "xml", "html", "css", "js", "kt", "java", "py", "cpp", "h", "c" -> androidx.compose.material.icons.Icons.Default.TextSnippet
        "zip", "rar", "7z", "tar", "gz", "bz2" -> androidx.compose.material.icons.Icons.Default.Zip
        "apk" -> androidx.compose.material.icons.Icons.Default.Android
        else -> androidx.compose.material.icons.Icons.Default.InsertDriveFile
    }
}