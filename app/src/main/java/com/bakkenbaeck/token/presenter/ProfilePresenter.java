package com.bakkenbaeck.token.presenter;

import android.graphics.Bitmap;
import android.view.View;

import com.bakkenbaeck.token.model.User;
import com.bakkenbaeck.token.util.ImageUtil;
import com.bakkenbaeck.token.util.OnSingleClickListener;
import com.bakkenbaeck.token.util.SharedPrefsUtil;
import com.bakkenbaeck.token.util.SingleSuccessSubscriber;
import com.bakkenbaeck.token.view.BaseApplication;
import com.bakkenbaeck.token.view.activity.ProfileActivity;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public final class ProfilePresenter implements Presenter<ProfileActivity> {

    private ProfileActivity activity;
    private boolean firstTimeAttaching = true;
    private User localUser;

    @Override
    public void onViewAttached(final ProfileActivity fragment) {
        this.activity = fragment;
        if (this.firstTimeAttaching) {
            this.firstTimeAttaching = false;
            initLongLivingObjects();
        }

        initShortLivingObjects();
    }

    private void initLongLivingObjects() {
        BaseApplication.get()
                .getTokenManager()
                .getUserManager()
                .getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this.handleUserLoaded);
    }

    private void initShortLivingObjects() {
        initToolbar();
        updateView();
    }

    private void initToolbar() {
        this.activity.getBinding().closeButton.setOnClickListener(this.onCloseClicked);
    }

    private void updateView() {
        if (this.localUser == null) {
            return;
        }

        this.activity.getBinding().name.setText(this.localUser.getUsername());
        this.activity.getBinding().username.setText(this.localUser.getAddress());

        final byte[] decodedBitmap = SharedPrefsUtil.getQrCode();
        if (decodedBitmap != null) {
            renderQrCode(decodedBitmap);
        } else {
            generateQrCode();
        }
    }

    private final SingleSuccessSubscriber<User> handleUserLoaded = new SingleSuccessSubscriber<User>() {
        @Override
        public void onSuccess(final User user) {
            ProfilePresenter.this.localUser = user;
            updateView();
            this.unsubscribe();
        }
    };

    private void renderQrCode(final byte[] qrCodeImageBytes) {
        final Bitmap qrCodeBitmap = ImageUtil.decodeByteArray(qrCodeImageBytes);
        renderQrCode(qrCodeBitmap);
    }

    private void renderQrCode(final Bitmap qrCodeBitmap) {
        this.activity.getBinding().qrCodeImage.setAlpha(0.0f);
        this.activity.getBinding().qrCodeImage.setImageBitmap(qrCodeBitmap);
        this.activity.getBinding().qrCodeImage.animate().alpha(1f).setDuration(200).start();
    }

    private void generateQrCode() {
        ImageUtil.generateQrCodeForWalletAddress(this.localUser.getAddress())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this.handleQrCodeGenerated);
    }

    private SingleSuccessSubscriber<Bitmap> handleQrCodeGenerated = new SingleSuccessSubscriber<Bitmap>() {
        @Override
        public void onSuccess(final Bitmap qrBitmap) {
            SharedPrefsUtil.saveQrCode(ImageUtil.compressBitmap(qrBitmap));
            renderQrCode(qrBitmap);
        }
    };

    private final OnSingleClickListener onCloseClicked = new OnSingleClickListener() {
        @Override
        public void onSingleClick(final View v) {
            activity.onBackPressed();
        }
    };

    @Override
    public void onViewDetached() {
        this.activity = null;
    }

    @Override
    public void onViewDestroyed() {
        this.activity = null;
        this.handleUserLoaded.unsubscribe();
        this.handleQrCodeGenerated.unsubscribe();
    }
}
