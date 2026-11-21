package com.example.filereader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filereader.ui.theme.FileReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileReaderTheme {
                ReaderScreen()
            }
        }
    }
}

@Composable
fun ReaderScreen(vm: FileViewModel = viewModel()) {
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            vm.loadFile(uri, context) // Вызываем функцию из ViewModel
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                filePicker.launch(arrayOf("text/plain", "text/xml", "application/xml", "*/*"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Открыть файл (.txt или .fb2)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (vm.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (vm.errorMessage != null) {
            Text(
                text = "Ошибка: ${vm.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (vm.textContent.isNotEmpty()) {
                Text(
                    text = vm.textContent,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}