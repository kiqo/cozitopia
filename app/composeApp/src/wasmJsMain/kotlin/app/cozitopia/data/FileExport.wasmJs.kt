@file:OptIn(ExperimentalWasmJsInterop::class)

package app.cozitopia.data

@JsFun(
    """(fileName, mimeType, content) => {
        const url = URL.createObjectURL(new Blob([content], { type: mimeType }));
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(url);
    }""",
)
private external fun downloadBlob(fileName: String, mimeType: String, content: String)

actual fun exportFile(fileName: String, mimeType: String, content: String) =
    downloadBlob(fileName, mimeType, content)
