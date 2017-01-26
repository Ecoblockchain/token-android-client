package com.bakkenbaeck.token.model.sofa;


import android.support.annotation.IntDef;

import com.bakkenbaeck.token.crypto.util.TypeConverter;
import com.bakkenbaeck.token.view.BaseApplication;
import com.squareup.moshi.Json;

import java.math.BigInteger;

/**
 * PaymentRequest
 * <p>
 * Request an Ethereum transaction
 *
 */
public class PaymentRequest {

    @IntDef({
            PENDING,
            ACCEPTED,
            REJECTED,
    })
    public @interface State {}
    public static final int PENDING = 0;
    public static final int REJECTED = 1;
    public static final int ACCEPTED = 2;

    /**
     * Value
     * <p>
     * Value of transaction in wei JSON-RPC encoded
     * (Required)
     *
     */
    private String value;
    /**
     * Destination Address
     * <p>
     * Ethereum address of recipient
     * (Required)
     *
     */
    private String destinationAddress;

    @Json(name = SofaType.LOCAL_ONLY_PAYLOAD)
    private ClientSideCustomData androidClientSideCustomData;

    public PaymentRequest setValue(final BigInteger value) {
        this.value = TypeConverter.toJsonHex(value);
        final String localAmount = BaseApplication.get().getTokenManager().getBalanceManager().getMarketRateInLocalCurrency(value);
        setLocalPrice(localAmount);
        return this;
    }

    public PaymentRequest setDestinationAddress(final String destinationAddress) {
        this.destinationAddress = destinationAddress;
        return this;
    }

    public String getDestinationAddresss() {
        return this.destinationAddress;
    }

    public BigInteger getValue() {
        return TypeConverter.StringHexToBigInteger(this.value);
    }

    private PaymentRequest setLocalPrice(final String localPrice) {
        if (this.androidClientSideCustomData == null) {
            this.androidClientSideCustomData = new ClientSideCustomData();
        }

        this.androidClientSideCustomData.localPrice = localPrice;
        return this;
    }

    public String getLocalPrice() {
        if (this.androidClientSideCustomData == null) {
            return null;
        }

        return this.androidClientSideCustomData.localPrice;
    }

    public PaymentRequest setState(final @State int state) {
        if (this.androidClientSideCustomData == null) {
            this.androidClientSideCustomData = new ClientSideCustomData();
        }

        this.androidClientSideCustomData.state = state;
        return this;
    }

    public @State int getState() {
        if (this.androidClientSideCustomData == null) {
            return PENDING;
        }

        return this.androidClientSideCustomData.state;
    }

    private static class ClientSideCustomData {
        private String localPrice;
        private @State int state;
    }
}