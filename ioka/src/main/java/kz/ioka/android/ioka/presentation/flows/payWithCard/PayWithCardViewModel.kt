package kz.ioka.android.ioka.presentation.flows.payWithCard

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kz.ioka.android.ioka.domain.common.ResultWrapper
import kz.ioka.android.ioka.domain.payment.PaymentModel
import kz.ioka.android.ioka.domain.payment.PaymentRepository
import kz.ioka.android.ioka.util.getOrderId
import kz.ioka.android.ioka.viewBase.BaseActivity
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PayWithCardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val launcher = savedStateHandle.get<PayWithCardLauncher>(BaseActivity.LAUNCHER)
    val price = launcher?.price
    private var paymentId: String = ""

    private val _isCardPanValid = MutableStateFlow(false)
    private val _isExpireDateValid = MutableStateFlow(false)
    private val _isCvvValid = MutableStateFlow(false)

    private val allFieldsAreValid: Flow<Boolean> = combine(
        _isCardPanValid,
        _isExpireDateValid,
        _isCvvValid
    ) { isCardPanValid, isExpireDateValid, isCvvValid ->
        isCardPanValid && isExpireDateValid && isCvvValid
    }

    private val _payState = MutableLiveData<PayState>(PayState.DEFAULT)
    val payState = _payState as LiveData<PayState>

    init {
        viewModelScope.launch {
            allFieldsAreValid.collect { areAllFieldsValid ->
                if (areAllFieldsValid) {
                    _payState.postValue(PayState.DEFAULT)
                } else {
                    _payState.postValue(PayState.DISABLED)
                }
            }
        }
    }

    fun onCardPanEntered(cardPan: String) {
        _isCardPanValid.value = cardPan.length in 15..19
    }

    fun onExpireDateEntered(expireDate: String) {
        _isExpireDateValid.value = if (expireDate.length < 4) {
            false
        } else {
            val month = expireDate.substring(0..1).toInt()
            val year = expireDate.substring(2).toInt()

            val currentTime = Calendar.getInstance()
            val currentMonth = currentTime.get(Calendar.MONTH)
            val currentYear = currentTime.get(Calendar.YEAR) - 2000

            month <= 12 && (year > currentYear || (year == currentYear && month >= currentMonth))
        }
    }

    fun onCvvEntered(cvv: String) {
        _isCvvValid.value = cvv.length == 3
    }

    fun onPayClicked(cardPan: String, expireDate: String, cvv: String, bindCard: Boolean) {
        viewModelScope.launch {
            val areAllFieldsValid = allFieldsAreValid.first()

            if (areAllFieldsValid) {
                _payState.postValue(PayState.LOADING)

                val cardPayment = paymentRepository.createCardPayment(
                    launcher?.orderToken?.getOrderId() ?: "",
                    launcher?.customerToken ?: "",
                    launcher?.apiKey ?: "",
                    cardPan, expireDate, cvv, bindCard
                )

                when (cardPayment) {
                    is ResultWrapper.GenericError -> {
                        _payState.postValue(PayState.ERROR)
                    }
                    is ResultWrapper.NetworkError -> {
                        _payState.postValue(PayState.ERROR)
                    }
                    is ResultWrapper.Success -> {
                        processSuccessfulResponse(cardPayment.value)
                    }
                }
            }
        }
    }

    private fun processSuccessfulResponse(cardPayment: PaymentModel) {
        when (cardPayment) {
            is PaymentModel.Pending -> {
                paymentId = cardPayment.paymentId
                _payState.postValue(PayState.PENDING(cardPayment.actionUrl))
            }
            is PaymentModel.Declined -> _payState.postValue(PayState.ERROR)
            else -> _payState.postValue(PayState.SUCCESS)
        }
    }

    fun on3DSecurePassed() {
        viewModelScope.launch {
            _payState.postValue(PayState.LOADING)

            val cardPayment = paymentRepository.isPaymentSuccessful(
                launcher?.apiKey ?: "",
                launcher?.customerToken ?: "",
                launcher?.orderToken ?: "",
                paymentId
            )

            when (cardPayment) {
                is ResultWrapper.GenericError -> {
                    _payState.postValue(PayState.ERROR)
                }
                is ResultWrapper.NetworkError -> {
                    _payState.postValue(PayState.ERROR)
                }
                is ResultWrapper.Success -> {
                    if (cardPayment.value) _payState.postValue(PayState.SUCCESS)
                    else _payState.postValue(PayState.ERROR)
                }
            }
        }
    }

}

sealed class PayState {

    object DEFAULT : PayState()
    object DISABLED : PayState()
    object LOADING : PayState()
    object SUCCESS : PayState()
    object ERROR : PayState()

    class PENDING(val actionUrl: String) : PayState()
}