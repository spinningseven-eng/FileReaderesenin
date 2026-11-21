package com.example.filereader

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

// ViewModel отвечает ТОЛЬКО за логику и данные.
class FileViewModel : ViewModel() {

    // Состояния UI (Compose сам обновит экран при их изменении)
    var textContent by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun loadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            textContent = ""

            try {
                val result = withContext(Dispatchers.IO) {
                    readUri(context, uri)
                }

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

    private fun readUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Не удалось открыть файл")

        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val stringBuilder = StringBuilder()

        reader.useLines { lines ->
            lines.forEach { line ->
                stringBuilder.append(line).append("\n")
            }
        }

        val rawText = stringBuilder.toString()

        // Парсинг FB2
        if (uri.toString().endsWith(".fb2") || rawText.contains("<FictionBook")) {
            val paragraphRegex = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
            val matches = paragraphRegex.findAll(rawText)

            val cleanText = StringBuilder()
            for (match in matches) {
                val text = match.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                if (text.isNotEmpty()) {
                    cleanText.append(text).append("\n\n")
                }
            }
            return cleanText.toString()
        }

        return rawText
    }
}