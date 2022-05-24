package kz.ioka.android.ioka.googlePay

import android.app.Activity
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object GooglePayUtil {

    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    private fun gatewayTokenizationSpecification(merchantId: String): JSONObject {
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put(
                "parameters", JSONObject(
                    mapOf(
                        "gateway" to "sberbank",
                        "gatewayMerchantId" to merchantId
                    )
                )
            )
        }
    }

    private val allowedCardNetworks =
        JSONArray(listOf("AMEX", "DISCOVER", "MASTERCARD", "MIR", "VISA", "UNIONPAY"))

    private val allowedCardAuthMethods =
        JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS"))

    fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()

        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    fun isReadyToPayRequest(): JSONObject? {
        return try {
            baseRequest.put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
        } catch (e: JSONException) {
            null
        }
    }

    private fun baseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {

            val parameters = JSONObject().apply {
                put("allowedAuthMethods", allowedCardAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
                put("allowPrepaidCards", false)
                put("billingAddressRequired", true)
                put("billingAddressParameters", JSONObject().apply {
                    put("format", "FULL")
                    put("phoneNumberRequired", true)
                })
            }

            put("type", "CARD")
            put("parameters", parameters)
        }
    }

    private fun cardPaymentMethod(merchantId: String): JSONObject {
        val cardPaymentMethod = baseCardPaymentMethod()
        cardPaymentMethod.put(
            "tokenizationSpecification",
            gatewayTokenizationSpecification(merchantId)
        )

        return cardPaymentMethod
    }

    fun getPaymentDataRequest(price: String, currency: Int, merchantId: String): JSONObject? {
        return try {
            baseRequest.apply {
                put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod(merchantId)))
                put("transactionInfo", getTransactionInfo(price, currency))
            }
        } catch (e: JSONException) {
            null
        }
    }

    private fun getTransactionInfo(price: String, currency: Int): JSONObject {
        return JSONObject().apply {
            put("totalPrice", price)
            put("totalPriceStatus", "FINAL")
            put("currencyCode", currency)
        }
    }
}