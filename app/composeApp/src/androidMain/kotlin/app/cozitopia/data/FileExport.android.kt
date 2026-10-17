package app.cozitopia.data

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

actual fun exportFile(fileName: String, mimeType: String, content: String) {
    val context = AndroidContext.application
    val file = File(context.cacheDir, fileName).apply { writeText(content) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
