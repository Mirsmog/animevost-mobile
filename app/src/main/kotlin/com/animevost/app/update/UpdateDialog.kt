package com.animevost.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.animevost.app.UpdateState
import java.io.File

@Composable
fun UpdateDialog(state: UpdateState, onDownload: () -> Unit, onDismiss: () -> Unit) {
    when (state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Доступно обновление") },
            text = { Text("Версия ${state.info.versionName} готова к загрузке.") },
            confirmButton = { TextButton(onClick = onDownload) { Text("Скачать") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Позже") } },
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Загрузка обновления…") },
            text = {
                Column {
                    Text("${state.progress}%")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {},
        )

        is UpdateState.ReadyToInstall -> {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Обновление загружено") },
                text = { Text("Нажмите «Установить» чтобы обновить приложение.") },
                confirmButton = {
                    TextButton(onClick = { installApk(context, state.file) }) {
                        Text("Установить")
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Позже") } },
            )
        }

        else -> Unit
    }
}

private fun installApk(context: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
