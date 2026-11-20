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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

// ==================== VIEWMODEL ====================
// Отвечает за бизнес-логику: чтение файла, парсинг и хранение текста
class FileViewModel : ViewModel() {
    var textContent by mutableStateOf("") // Прочитанный текст
    var isLoading by mutableStateOf(false) // Индикатор загрузки
    var errorMessage by mutableStateOf<String?>(null) // Сообщение об ошибке

    // Функция для запуска чтения файла
    fun loadFile(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            textContent = ""

            try {
                // Читаем файл в фоновом потоке (IO), чтобы не вешать интерфейс
                val result = withContext(Dispatchers.IO) {
                    readUri(context, uri)
                }

                // Проверка на пустой файл
                if (result.isBlank()) {
                    errorMessage = "Файл пуст"
                } else {
                    textContent = result
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка чтения: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Вспомогательная функция для чтения и парсинга файла
    private fun readUri(context: android.content.Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Не удалось открыть файл")

        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val stringBuilder = StringBuilder()

        // Читаем файл построчно
        reader.useLines { lines ->
            lines.forEach { line ->
                stringBuilder.append(line).append("\n")
            }
        }

        val rawText = stringBuilder.toString()

        // === ПОДДЕРЖКА FB2 (Задание со *) ===
        // Если файл имеет расширение .fb2 или содержит тег <FictionBook>
        if (uri.toString().endsWith(".fb2") || rawText.contains("<FictionBook")) {
            // Извлекаем чистый текст из тегов <p>...</p>
            val paragraphRegex = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
            val matches = paragraphRegex.findAll(rawText)

            val cleanText = StringBuilder()
            for (match in matches) {
                // Убираем остатки HTML-тегов внутри абзаца и лишние пробелы
                val text = match.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                if (text.isNotEmpty()) {
                    cleanText.append(text).append("\n\n")
                }
            }
            return cleanText.toString()
        }

        // Для обычных .txt файлов возвращаем текст как есть
        return rawText
    }
}

// ==================== UI ====================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ReaderScreen()
            }
        }
    }
}

@Composable
fun ReaderScreen(vm: FileViewModel = viewModel()) {
    // 1. Получаем контекст ЗАРАНЕЕ (вне лямбды)
    val context = LocalContext.current

    // 2. Лончер для выбора файла
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Используем сохраненную переменную context
            vm.loadFile(uri, context)
        }
    }

    // 3. Основной интерфейс
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Кнопка открытия файла
        Button(
            onClick = {
                // Открываем диалог выбора файла.
                // Разрешаем текстовые файлы и любые другие (на случай, если система не распознает fb2)
                filePicker.launch(arrayOf("text/plain", "text/xml", "application/xml", "*/*"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Открыть файл (.txt или .fb2)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Индикатор загрузки
        if (vm.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Отображение ошибки
        if (vm.errorMessage != null) {
            Text(
                text = "Ошибка: ${vm.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Область отображения текста с прокруткой
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Занимает всё оставшееся пространство
                .verticalScroll(rememberScrollState()) // Включаем скролл
        ) {
            if (vm.textContent.isNotEmpty()) {
                Text(
                    text = vm.textContent,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}