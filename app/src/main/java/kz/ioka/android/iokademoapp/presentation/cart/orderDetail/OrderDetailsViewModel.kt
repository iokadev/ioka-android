package kz.ioka.android.iokademoapp.presentation.cart.orderDetail

import android.app.Activity
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kz.ioka.android.ioka.api.CardDvo
import kz.ioka.android.ioka.api.Ioka
import kz.ioka.android.iokademoapp.data.OrderRepository
import kz.ioka.android.iokademoapp.presentation.cart.PaymentTypeDvo
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val launcher = savedStateHandle.get<OrderLauncher>("OrderDetailsActivity_LAUNCHER")

    val itemName = launcher?.itemName
    val itemPrice = launcher?.price
    val itemImage = launcher?.itemImage

    private var orderToken: String = ""
    private var customerToken: String = ""

    private val _progress = MutableLiveData(false)
    val progress = _progress as LiveData<Boolean>

    private val _paymentAction = MutableLiveData<(Activity) -> Unit>()
    val paymentAction = _paymentAction as LiveData<(Activity) -> Unit>

    private val _selectedPaymentType = MutableLiveData<PaymentTypeDvo>()
    var selectedPaymentType = _selectedPaymentType as LiveData<PaymentTypeDvo>

    init {
        viewModelScope.launch {
            _progress.postValue(true)

            val checkout = orderRepository.checkout(itemPrice ?: BigDecimal.ZERO)

            orderToken = checkout.orderToken ?: ""
            customerToken = checkout.customerToken ?: ""

            _progress.postValue(false)
        }
    }

    fun onContinueClicked() {
        _paymentAction.value = when (selectedPaymentType.value) {
            PaymentTypeDvo.GooglePayDvo ->
                { activity: Activity ->
                    Ioka.startPaymentFlow(activity, orderToken)
                }
            PaymentTypeDvo.PayWithCardDvo ->
                { activity: Activity ->
                    Ioka.startPaymentFlow(activity, orderToken)
                }
            PaymentTypeDvo.PayWithCashDvo ->
                { activity: Activity ->
                    Ioka.startPaymentFlow(activity, orderToken)
                }
            is PaymentTypeDvo.PayWithSavedCardDvo -> {
                val cardDvo = selectedPaymentType.value as PaymentTypeDvo.PayWithSavedCardDvo

                { activity: Activity ->
                    Ioka.startPaymentWithSavedCardFlow(
                        activity,
                        orderToken,
                        CardDvo(
                            cardDvo.cardId,
                            cardDvo.maskedCardNumber,
                            cardDvo.cardType.code,
                            cardDvo.cvvRequired,
                        )
                    )
                }
            }
            null ->
                { activity: Activity ->
                    Ioka.startPaymentFlow(activity, orderToken)
                }
        }
    }

    fun onPaymentTypeSelected(paymentType: PaymentTypeDvo) {
        _selectedPaymentType.value = paymentType
    }

}