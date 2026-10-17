package app.cozitopia.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cozitopia.data.AppError
import app.cozitopia.data.ErrorBus
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class ScreenViewModel(protected val errors: ErrorBus) : ViewModel() {
    protected fun launchCatching(block: suspend () -> Unit): Job = viewModelScope.launch {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            errors.report(AppError.from(failure))
        }
    }
}
