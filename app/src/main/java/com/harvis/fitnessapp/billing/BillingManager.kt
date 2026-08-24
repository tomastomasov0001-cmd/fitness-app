package com.harvis.fitnessapp.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.harvis.fitnessapp.util.PremiumManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PREMIUM_PRODUCT_ID = "premium_unlock"
    }

    private var billingClient: BillingClient? = null
    private var premiumProductDetails: ProductDetails? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    queryProducts()
                    queryExistingPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
            }
        })
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient?.queryProductDetailsAsync(params) { _, productDetailsList ->
            premiumProductDetails = productDetailsList.firstOrNull()
        }
    }

    private fun queryExistingPurchases() {
        coroutineScope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            billingClient?.queryPurchasesAsync(params) { billingResult, purchaseList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (purchase in purchaseList) {
                        if (purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            handlePurchase(purchase)
                        }
                    }
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity): Boolean {
        val productDetails = premiumProductDetails ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            ))
            .build()
        return billingClient?.launchBillingFlow(activity, params)?.responseCode ==
            BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
            PremiumManager.setPremiumPurchased(context, true)
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        coroutineScope.launch {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            withContext(Dispatchers.IO) {
                billingClient?.acknowledgePurchase(params) { }
            }
        }
    }

    fun getFormattedPrice(): String? = premiumProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice
    fun isProductAvailable(): Boolean = premiumProductDetails != null
    fun endConnection() { billingClient?.endConnection(); billingClient = null }
}
