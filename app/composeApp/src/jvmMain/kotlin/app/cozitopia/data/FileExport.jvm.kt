package app.cozitopia.data

import java.io.File

actual fun exportFile(fileName: String, mimeType: String, content: String) {
    File(System.getProperty("java.io.tmpdir"), fileName).writeText(content)
}
