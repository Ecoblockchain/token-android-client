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

package com.tokenbrowser.presenter.webview;

import android.webkit.JavascriptInterface;

import com.tokenbrowser.presenter.Presenter;
import com.tokenbrowser.view.activity.WebViewActivity;

public class WebViewPresenter implements Presenter<WebViewActivity> {

    private WebViewActivity activity;

    @Override
    public void onViewAttached(final WebViewActivity view) {
        this.activity = view;
        initWebView();
    }

    private void initWebView() {
        loadTestWebApp();
        initTestWebApp();
    }

    private void loadTestWebApp() {
        final String address = this.activity.getIntent().getStringExtra(WebViewActivity.EXTRA__ADDRESS);
        this.activity.getBinding().webview.getSettings().setJavaScriptEnabled(true);
        this.activity.getBinding().webview.addJavascriptInterface(new SOFAHost(), "SOFAHost");
        this.activity.getBinding().webview.loadUrl(address);
    }

    private void initTestWebApp() {
        this.activity.getBinding().webview.setWebViewClient(new SofaWebViewClient());
    }

    @Override
    public void onViewDetached() {
        this.activity = null;
    }

    @Override
    public void onDestroyed() {
        this.activity = null;
    }

    private class SOFAHost {
        @JavascriptInterface
        public String getAccounts() {
            return "0x0";
        }

        @JavascriptInterface
        public boolean approveTransaction(final String details) {
            return true;
        }

        @JavascriptInterface
        public String signTransaction(final String unsignedTransaction) {
            return "signature";
        }
    }
}
