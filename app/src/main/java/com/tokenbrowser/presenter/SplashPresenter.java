/*
 * 	Copyright (c) 2017. Token Browser, Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tokenbrowser.presenter;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;

import com.tokenbrowser.util.LogUtil;
import com.tokenbrowser.util.SharedPrefsUtil;
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.activity.MainActivity;
import com.tokenbrowser.view.activity.QrCodeHandlerActivity;
import com.tokenbrowser.view.activity.SignInActivity;
import com.tokenbrowser.view.activity.SplashActivity;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SplashPresenter implements Presenter<SplashActivity> {

    private SplashActivity activity;
    private CompositeSubscription subscriptions;
    private boolean firstTimeAttaching = true;

    @Override
    public void onViewAttached(SplashActivity view) {
        this.activity = view;

        if (this.firstTimeAttaching) {
            this.firstTimeAttaching = false;
            initLongLivingObjects();
        }

        redirect();
    }

    private void initLongLivingObjects() {
        this.subscriptions = new CompositeSubscription();
    }

    private void redirect() {
        final boolean hasSignedOut = SharedPrefsUtil.hasSignedOut();

        if (!hasSignedOut) {
            initManagersAndGoToAnotherActivity();
        } else {
            goToSignInActivity();
        }
    }

    private void initManagersAndGoToAnotherActivity() {
        final Subscription sub =
                BaseApplication
                .get()
                .getTokenManager()
                .init()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        __ -> goToAnotherActivity(),
                        this::handleError
                );

        this.subscriptions.add(sub);
    }

    private void handleError(final Throwable throwable) {
        LogUtil.exception(getClass(), "Error while initiating managers", throwable);
    }

    private void goToAnotherActivity() {
        SharedPrefsUtil.setSignedIn();

        final PendingIntent nextIntent = this.activity.getIntent().getParcelableExtra(SplashActivity.EXTRA__NEXT_INTENT);
        if (nextIntent != null) {
            try {
                nextIntent.send();
            } catch (final PendingIntent.CanceledException ex) {
                LogUtil.exception(getClass(), ex);
            }
            this.activity.finish();
        } else {
            if (!tryGoToQrActivity()) {
                goToMainActivity();
            }
        }
    }

    private boolean tryGoToQrActivity() {
        final Uri uri = this.activity.getIntent().getData();
        if (uri != null) {
            goToQrCodeActivity(uri);
            return true;
        }
        return false;
    }

    private void goToQrCodeActivity(final Uri uri) {
        final Intent intent = new Intent(this.activity, QrCodeHandlerActivity.class)
                .setData(uri);
        goToActivity(intent);
    }

    private void goToMainActivity() {
        final Intent intent = new Intent(this.activity, MainActivity.class);
        goToActivity(intent);
    }

    private void goToSignInActivity() {
        final Intent intent = new Intent(this.activity, SignInActivity.class);
        goToActivity(intent);
    }

    private void goToActivity(final Intent intent) {
        this.activity.startActivity(intent);
        this.activity.finish();
    }

    @Override
    public void onViewDetached() {
        this.subscriptions.clear();
        this.activity = null;
    }

    @Override
    public void onDestroyed() {
        this.subscriptions = null;
        this.activity = null;
    }
}
