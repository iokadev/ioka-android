package kz.ioka.android.ioka.api

import android.app.Activity
import kz.ioka.android.ioka.Config
import kz.ioka.android.ioka.di.DependencyInjector
import kz.ioka.android.ioka.presentation.flows.payment.PaymentActivity
import kz.ioka.android.ioka.presentation.flows.paymentWithSavedCard.withCvv.CvvPaymentLauncher
import kz.ioka.android.ioka.presentation.flows.paymentWithSavedCard.withCvv.PayWithCvvActivity
import kz.ioka.android.ioka.presentation.flows.paymentWithSavedCard.withoutCvv.PayWithCardIdActivity
import kz.ioka.android.ioka.presentation.flows.paymentWithSavedCard.withoutCvv.PayWithCardIdLauncher
import kz.ioka.android.ioka.presentation.flows.saveCard.SaveCardActivity
import kz.ioka.android.ioka.util.getCustomerId
import java.net.ProtocolException

object Ioka {

    private val cardApi by lazy { DependencyInjector.cardApi }

    // Формат apiKey:
    // <SHOPID>_test_public_<KEY> - для стейджа
    // <SHOPID>_live_public_<KEY> - для прода
    fun init(apiKey: String) {
        Config.apiKey = apiKey
        Config.isDebug = apiKey.contains("test_public")
    }

    fun startPaymentFlow(
        activity: Activity,
        orderToken: String,
        configuration: Configuration? = null
    ) {
        if (Config.isApiKeyInitialized().not()) {
            throw RuntimeException("Init Ioka with your API_KEY")
        }

        val intent = PaymentActivity.provideIntent(activity, orderToken, configuration)

        activity.startActivityForResult(intent, IOKA_PAYMENT_REQUEST_CODE)
    }

    fun startPaymentWithSavedCardFlow(
        activity: Activity,
        orderToken: String,
        card: CardDvo,
        configuration: Configuration? = null
    ) {
        val intent = if (card.cvvRequired) {
            PayWithCvvActivity.provideIntent(
                activity, CvvPaymentLauncher(orderToken, card, configuration)
            )
        } else {
            PayWithCardIdActivity.provideIntent(
                activity, PayWithCardIdLauncher(orderToken, card.cardId)
            )
        }

        activity.startActivityForResult(intent, IOKA_PAYMENT_REQUEST_CODE)
    }

    fun startSaveCardFlow(
        activity: Activity,
        customerToken: String,
        configuration: Configuration? = null
    ) {
        if (Config.isApiKeyInitialized().not()) {
            throw RuntimeException("Init Ioka with your API_KEY")
        }

        val intent = SaveCardActivity.provideIntent(
            activity,
            customerToken,
            configuration
        )

        activity.startActivityForResult(intent, IOKA_SAVE_CARD_REQUEST_CODE)
    }

    suspend fun getCards(customerToken: String): List<CardModel> {
        if (Config.isApiKeyInitialized().not()) {
            throw RuntimeException("Init Ioka with your API_KEY")
        }

        return cardApi.getCards(
            Config.apiKey, customerToken, customerToken.getCustomerId()
        ).map {
            CardModel(
                id = it.id,
                customerId = it.customer_id,
                createdAt = it.created_at,
                panMasked = it.pan_masked,
                expiryDate = it.expiry_date,
                holder = it.holder,
                paymentSystem = CardBrandModel.getByCode(it.payment_system),
                emitter = CardEmitterModel.getByCode(it.emitter),
                cvcRequired = it.cvc_required,
            )
        }
    }

    suspend fun getCardById(customerToken: String, cardId: String): CardModel {
        if (Config.isApiKeyInitialized().not()) {
            throw RuntimeException("Init Ioka with your API_KEY")
        }

        val cardResponse =
            cardApi.getCardById(Config.apiKey, customerToken, customerToken.getCustomerId(), cardId)

        return CardModel(
            id = cardResponse.id,
            customerId = cardResponse.customer_id,
            createdAt = cardResponse.created_at,
            panMasked = cardResponse.pan_masked,
            expiryDate = cardResponse.expiry_date,
            holder = cardResponse.holder,
            paymentSystem = CardBrandModel.getByCode(cardResponse.payment_system),
            emitter = CardEmitterModel.getByCode(cardResponse.emitter),
            cvcRequired = cardResponse.cvc_required,
        )
    }

    suspend fun removeCard(
        customerToken: String,
        cardId: String
    ): Boolean {
        if (Config.isApiKeyInitialized().not()) {
            throw RuntimeException("Init Ioka with your API_KEY")
        }

        return try {
            cardApi.removeCard(
                Config.apiKey,
                customerToken,
                customerToken.getCustomerId(),
                cardId
            )
            true
        } catch (e: Exception) {
            e is ProtocolException
        }
    }

}