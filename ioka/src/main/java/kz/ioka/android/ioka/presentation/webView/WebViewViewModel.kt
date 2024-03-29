package kz.ioka.android.ioka.presentation.webView

import android.os.Parcelable
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

internal class WebViewViewModelFactory(
    private val behavior: WebViewBehavior
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WebViewViewModel(behavior) as T
    }

}

internal class WebViewViewModel(
    private val behavior: WebViewBehavior
) : ViewModel() {

    val actionUrl: String = behavior.actionUrl
    val toolbarTitleRes: Int = behavior.toolbarTitleRes

    private val _progress = MutableLiveData(false)
    val progress = _progress as LiveData<Boolean>

    private val _result = MutableLiveData<ResultState>()
    val result = _result as LiveData<ResultState>

    init {
        behavior
            .observeProgress()
            .flowOn(Dispatchers.IO)
            .onEach {
                _progress.postValue(it)
            }
            .launchIn(viewModelScope)
    }

    fun onRedirected() {
        viewModelScope.launch(Dispatchers.IO) {
            _result.postValue(behavior.onActionFinished())
        }
    }

}

internal sealed class ResultState : Parcelable {

    @Parcelize
    object Success : ResultState()

    @Parcelize
    class Fail(val cause: String? = null) : ResultState()

    @Parcelize
    object Canceled : ResultState()

}