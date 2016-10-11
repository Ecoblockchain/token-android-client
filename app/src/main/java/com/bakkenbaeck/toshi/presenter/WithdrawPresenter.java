package com.bakkenbaeck.toshi.presenter;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.bakkenbaeck.toshi.R;
import com.bakkenbaeck.toshi.model.ActivityResultHolder;
import com.bakkenbaeck.toshi.model.LocalBalance;
import com.bakkenbaeck.toshi.model.User;
import com.bakkenbaeck.toshi.network.rest.ToshiService;
import com.bakkenbaeck.toshi.network.rest.model.SignatureRequest;
import com.bakkenbaeck.toshi.network.rest.model.SignedWithdrawalRequest;
import com.bakkenbaeck.toshi.network.rest.model.TransactionSent;
import com.bakkenbaeck.toshi.network.rest.model.WithdrawalRequest;
import com.bakkenbaeck.toshi.util.EthUtil;
import com.bakkenbaeck.toshi.util.LocaleUtil;
import com.bakkenbaeck.toshi.util.LogUtil;
import com.bakkenbaeck.toshi.util.OnNextObserver;
import com.bakkenbaeck.toshi.util.OnSingleClickListener;
import com.bakkenbaeck.toshi.util.RetryWithBackoff;
import com.bakkenbaeck.toshi.view.BaseApplication;
import com.bakkenbaeck.toshi.view.activity.BarcodeScannerActivity;
import com.bakkenbaeck.toshi.view.activity.WithdrawActivity;
import com.bakkenbaeck.toshi.view.adapter.PreviousWalletAddress;
import com.bakkenbaeck.toshi.view.dialog.ProgressDialog;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;

import rx.Subscriber;

import static android.app.Activity.RESULT_OK;

public class WithdrawPresenter implements Presenter<WithdrawActivity> {
    private static final String TAG = "WithdrawPresenter";
    static final String INTENT_WALLET_ADDRESS = "wallet_address";
    static final String INTENT_WITHDRAW_AMOUNT = "withdraw_amount";

    private WithdrawActivity activity;
    private User currentUser;
    private boolean firstTimeAttaching = true;
    private BigDecimal currentBalance = BigDecimal.ZERO;
    private final BigDecimal minWithdrawLimit = new BigDecimal("0.0000000001");
    private ProgressDialog progressDialog;

    private final PreviousWalletAddress previousWalletAddress = new PreviousWalletAddress();

    @Override
    public void onViewAttached(final WithdrawActivity activity) {
        this.activity = activity;
        initButtons();
        initToolbar();
        initPreviousAddress();

        if (firstTimeAttaching) {
            firstTimeAttaching = false;
            registerObservables();
            initProgressDialog2();
        }
    }

    private void initProgressDialog2(){
        progressDialog = ProgressDialog.newInstance();
    }

    private void initButtons() {
        Log.d(TAG, "initButtons: ");
        this.activity.getBinding().barcodeButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(final View v) {
                showBarcodeActivity();
            }
        });

        this.activity.getBinding().sendButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(final View view) {
                handleSendClicked();
            }
        });

        this.activity.getBinding().walletAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {}

            @Override
            public void onTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {
                Log.d(TAG, "onTextChanged: walletAddress");
                updateSendButtonEnabledState();
            }

            @Override
            public void afterTextChanged(final Editable editable) {}
        });

        this.activity.getBinding().amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: amount");
                updateSendButtonEnabledState();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //refreshButtonStates();
    }

    private void updateSendButtonEnabledState() {
        Log.d(TAG, "updateSendButtonEnabledState: ");
        final Editable walletAddress = this.activity.getBinding().walletAddress.getText();
        String amount = this.activity.getBinding().amount.getText().toString();
        final boolean shouldEnableButton = walletAddress.length() > 0 && userHasEnoughReputationScore() && amount.length() > 0;
        enableSendButton(shouldEnableButton);
        activity.getBinding().sendButton.setEnabled(shouldEnableButton);
    }

    private void enableSendButton(boolean enabled){
        Log.d(TAG, "enableSendButton: " + enabled);
        if(enabled){
            activity.getBinding().sendButton.setTextColor(Color.parseColor("#FFFFFF"));
            activity.getBinding().sendButton.setBackground(ContextCompat.getDrawable(activity, R.drawable.btn_with_radius));
        }else{
            activity.getBinding().sendButton.setTextColor(Color.parseColor("#33565A64"));
            activity.getBinding().sendButton.setBackground(ContextCompat.getDrawable(activity, R.drawable.disabled_background));
        }
    }

    private void initToolbar() {
        final String title = this.activity.getResources().getString(R.string.withdraw__title);
        final Toolbar toolbar = this.activity.getBinding().toolbar;
        this.activity.setSupportActionBar(toolbar);
        this.activity.getSupportActionBar().setTitle(title);
        this.activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initPreviousAddress() {
        final EditText walletAddress = this.activity.getBinding().walletAddress;
        walletAddress.setText(this.previousWalletAddress.getAddress());
        walletAddress.setSelection(walletAddress.getText().length());
    }

    private final OnNextObserver<LocalBalance> newBalanceSubscriber = new OnNextObserver<LocalBalance>() {
        @Override
        public void onNext(final LocalBalance newBalance) {
            if (activity != null && newBalance != null) {
                activity.getBinding().balanceBar.setBalance(newBalance.unconfirmedBalanceString());
                tryPopulateAmountField(currentBalance, newBalance.confirmedBalanceStringMinusTransferFee());
                currentBalance = newBalance.getConfirmedBalanceAsEthMinusTransferFee();
            }
        }
    };

    private final OnNextObserver<Integer> newReputationSubscriber = new OnNextObserver<Integer>() {
        @Override
        public void onNext(Integer reputationScore) {
            if(activity != null){
                activity.getBinding().balanceBar.setReputation(reputationScore);
            }
        }
    };

    private void tryPopulateAmountField(final BigDecimal previousBalance, final String newBalanceAsEthString) {
        Log.d(TAG, "tryPopulateAmountField: 1 " + previousBalance + " " + newBalanceAsEthString);
        try {
            Log.d(TAG, "tryPopulateAmountField: 2");

            this.activity.getBinding().amount.setText(newBalanceAsEthString);

            if (new BigDecimal(this.activity.getBinding().amount.getText().toString()).equals(previousBalance)) {
                Log.d(TAG, "tryPopulateAmountField: 3");
                this.activity.getBinding().amount.setText(newBalanceAsEthString);
                this.activity.getBinding().amount.setSelection(this.activity.getBinding().amount.getText().length());
            }
        } catch (final Exception ex) {
            Log.d(TAG, "tryPopulateAmountField: 4");
            // Do nothing -- user is editing the field
        }

    }

    @Override
    public void onViewDetached() {
        this.activity = null;
    }

    @Override
    public void onViewDestroyed() {
        this.activity = null;
    }

    /*public void onPhoneInputSuccess(final PhoneInputDialog dialog) {
        final String phoneNumber = dialog.getInputtedPhoneNumber();
        final VerificationCodeDialog vcDialog = VerificationCodeDialog.newInstance(phoneNumber);
        vcDialog.show(this.activity.getSupportFragmentManager(), "dialog");
    }

    public void onVerificationSuccess() {
        Snackbar.make(
                this.activity.getBinding().getRoot(),
                Html.fromHtml(this.activity.getString(R.string.verification_success)),
                Snackbar.LENGTH_LONG).show();
    }*/

    public void handleActivityResult(final ActivityResultHolder activityResultHolder) {
        if (activityResultHolder.getResultCode() != RESULT_OK) {
            return;
        }

        tryParseBarcodeResult(activityResultHolder);
    }

    private void showBarcodeActivity() {
        new IntentIntegrator(this.activity)
                .setCaptureActivity(BarcodeScannerActivity.class)
                .setOrientationLocked(true)
                .setPrompt("")
                .setBeepEnabled(true)
                .initiateScan();
    }

    private void tryParseBarcodeResult(final ActivityResultHolder activityResultHolder) {
        final IntentResult result = IntentIntegrator.parseActivityResult(
                activityResultHolder.getRequestCode(),
                activityResultHolder.getResultCode(),
                activityResultHolder.getIntent());
        if(result == null || result.getContents() == null) {
            return;
        }

        this.activity.getBinding().walletAddress.setText(result.getContents());
    }

    private void handleSendClicked() {
        Log.d(TAG, "handleSendClicked: ");
        if (!validate()) {
            Log.d(TAG, "handleSendClicked: return !valid");
            return;
        }

        try {
            final NumberFormat nf = NumberFormat.getInstance(LocaleUtil.getLocale());
            final String inputtedText = this.activity.getBinding().amount.getText().toString();
            final BigDecimal amountInEth = new BigDecimal(nf.parse(inputtedText).toString());

            final BigInteger amountInWei = EthUtil.ethToWei(amountInEth);
            final String toAddress = this.activity.getBinding().walletAddress.getText().toString();
            final WithdrawalRequest withdrawalRequest = new WithdrawalRequest(amountInWei, toAddress);
            ToshiService.getApi()
                    .postWithdrawalRequest(this.currentUser.getAuthToken(), withdrawalRequest)
                    .subscribe(generateSigningSubscriber());
            progressDialog.show(this.activity.getSupportFragmentManager(), "progressDialog");
            this.previousWalletAddress.setAddress(toAddress);
        } catch (final ParseException ex) {
            LogUtil.e(getClass(), ex.toString());
        }
    }

    private Subscriber<SignatureRequest> generateSigningSubscriber() {
        return new Subscriber<SignatureRequest>() {
            @Override
            public void onCompleted() {}

            @Override
            public void onError(final Throwable ex) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "There was a problem withdrawing, please try again.", Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }
                });
                LogUtil.e(getClass(), "postWithdrawalRequest: " + ex);
            }

            @Override
            public void onNext(final SignatureRequest signatureRequest) {
                final String unsignedTransaction = signatureRequest.getTransaction();
                final String signature = BaseApplication.get().getUserManager().signTransaction(unsignedTransaction);
                final SignedWithdrawalRequest request = new SignedWithdrawalRequest(unsignedTransaction, signature);
                ToshiService.getApi()
                        .postSignedWithdrawal(currentUser.getAuthToken(), request)
                        .retryWhen(new RetryWithBackoff(5))
                        .subscribe(generateSignedWithdrawalSubscriber());
            }

            private Subscriber<TransactionSent> generateSignedWithdrawalSubscriber() {
                return new Subscriber<TransactionSent>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(final Throwable ex) {
                        Log.d(TAG, "onError: ");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, R.string.error__withdrawing, Toast.LENGTH_LONG).show();
                                progressDialog.dismiss();
                            }
                        });
                        LogUtil.e(getClass(), "postSignedWithdrawal: " + ex);
                    }

                    @Override
                    public void onNext(final TransactionSent transactionSent) {
                        Log.d(TAG, "onNext: ");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        });

                        final Intent intent = new Intent();
                        intent.putExtra(INTENT_WALLET_ADDRESS, activity.getBinding().walletAddress.getText().toString());
                        intent.putExtra(INTENT_WITHDRAW_AMOUNT, new BigDecimal(activity.getBinding().amount.getText().toString()));
                        activity.setResult(RESULT_OK, intent);
                        activity.finish();
                        activity.overridePendingTransition(R.anim.enter_fade_in, R.anim.exit_fade_out);
                    }
                };
            }
        };
    }

    private boolean validate() {
        try {
            final NumberFormat nf = NumberFormat.getInstance(LocaleUtil.getLocale());
            final String inputtedText = this.activity.getBinding().amount.getText().toString();
            final BigDecimal amountRequested = new BigDecimal(nf.parse(inputtedText).toString());

            final String toAddress = this.activity.getBinding().walletAddress.getText().toString();
            this.activity.getBinding().walletAddress.setText(toAddress.replaceFirst("ethereum:", ""));

            if (amountRequested.compareTo(this.minWithdrawLimit) > 0 && amountRequested.compareTo(this.currentBalance) <= 0) {
                return true;
            }
        } catch (final NumberFormatException | ParseException ex) {
            LogUtil.e(getClass(), ex.toString());
        }

        this.activity.getBinding().amount.setError(this.activity.getResources().getString(R.string.withdraw__amount_error));
        this.activity.getBinding().amount.requestFocus();
        return false;
    }

    private boolean userHasEnoughReputationScore() {
        // Todo: Reputation required for withdrawal should be dictated by the server
        if (currentUser == null || currentUser.getReputationScore() == 0) {
            return false;
        }
        return true;
    }

    private void registerObservables() {
        BaseApplication.get().getLocalBalanceManager().getObservable().subscribe(this.newBalanceSubscriber);
        BaseApplication.get().getUserManager().getObservable().subscribe(this.userSubscriber);
        BaseApplication.get().getLocalBalanceManager().getReputationObservable().subscribe(this.newReputationSubscriber);
    }

    private final OnNextObserver<User> userSubscriber = new OnNextObserver<User>() {
        @Override
        public void onNext(final User user) {
            currentUser = user;
            refreshButtonStates();
            this.onCompleted();
        }
    };

    private void refreshButtonStates() {
        Log.d(TAG, "refreshButtonStates: ");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (activity == null) {
                    return;
                }
                updateSendButtonEnabledState();
            }
        });
    }
}
