package com.bakkenbaeck.toshi.network.ws.model;


import android.util.Log;

import com.bakkenbaeck.toshi.model.jsonadapter.BigIntegerAdapter;
import com.bakkenbaeck.toshi.network.rest.model.TransactionSent;
import com.bakkenbaeck.toshi.network.rest.model.VerificationSent;
import com.bakkenbaeck.toshi.network.ws.SocketObservables;
import com.bakkenbaeck.toshi.util.LogUtil;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

public class SocketToPojo {
    private static final String TAG = "SocketToPojo";
    private final Moshi moshi;
    private final JsonAdapter<WebSocketType> jsonAdapter;
    private final JsonAdapter<Message> messageAdapter;
    private final JsonAdapter<TransactionSent> transactionSentAdapter;
    private final JsonAdapter<TransactionConfirmation> confirmationAdapter;
    private final JsonAdapter<VerificationSent> verificationSentAdapter;
    private final JsonAdapter<WebSocketError> errorAdapter;
    private final SocketObservables socketObservables;

    public SocketToPojo(final SocketObservables socketObservables) {
        this.socketObservables = socketObservables;
        this.moshi = new Moshi
                            .Builder()
                            .add(new BigIntegerAdapter())
                            .build();
        this.jsonAdapter = this.moshi.adapter(WebSocketType.class);
        this.messageAdapter = this.moshi.adapter(Message.class);
        this.transactionSentAdapter = this.moshi.adapter(TransactionSent.class);
        this.confirmationAdapter = this.moshi.adapter(TransactionConfirmation.class);
        this.verificationSentAdapter = this.moshi.adapter(VerificationSent.class);
        this.errorAdapter = this.moshi.adapter(WebSocketError.class);
    }

    public void handleNewMessage(final String json) {
        try {
            convertAndEmitPojo(json);
        } catch (final IOException e) {
            LogUtil.e(getClass(), e.toString());
        }

    }

    private void convertAndEmitPojo(final String json) throws IOException {
        LogUtil.i(getClass(), "Incoming WS event. " + json);
        final WebSocketType webSocketType = getWebSocketMessageFromJson(json);
        if (webSocketType == null) {
            LogUtil.e(getClass(), "Websocket frame unhandled");
            return;
        }

        Log.d(TAG, "convertAndEmitPojo: " + json);

        switch (webSocketType.get()) {
            case "hello":
                LogUtil.i(getClass(), "Ignoring websocket event -- hello");
                break;
            case "message":
                Log.d(TAG, "convertAndEmitPojo: 1");
                final Message message = this.messageAdapter.fromJson(json);
                this.socketObservables.emitMessage(message);
                break;
            case "transaction_sent":
                final TransactionSent transactionSent = this.transactionSentAdapter.fromJson(json);
                this.socketObservables.emitTransactionSent(transactionSent);
                break;
            case "transaction_confirmation":
                final TransactionConfirmation confirmation = this.confirmationAdapter.fromJson(json);
                this.socketObservables.emitTransactionConfirmation(confirmation);
                break;
            case "verification_sent":
                final VerificationSent verificationSent = this.verificationSentAdapter.fromJson(json);
                this.socketObservables.emitVerificationSent(verificationSent);
                break;
            case "verification_success":
                this.socketObservables.emitVerificationSuccess(new VerificationSuccess());
                break;
            case "error":
                WebSocketError error;
                try {
                    error = this.errorAdapter.fromJson(json);
                } catch (final Exception ex) {
                    LogUtil.e(getClass(), "Unrecognised error code. Emitting generic error.");
                    error = new WebSocketError();
                }
                this.socketObservables.emitError(error);
                break;
            default:
                LogUtil.e(getClass(), "Unrecognised websocket event - " + webSocketType.get());
        }
    }

    private WebSocketType getWebSocketMessageFromJson(final String message) {
        try {
            return this.jsonAdapter.fromJson(message);
        } catch (final IOException e) {
            LogUtil.e(getClass(), "Invalid JSON input. " + e);
            return null;
        }
    }
}
