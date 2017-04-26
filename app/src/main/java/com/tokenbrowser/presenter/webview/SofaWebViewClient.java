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


import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tokenbrowser.R;
import com.tokenbrowser.util.LogUtil;
import com.tokenbrowser.view.BaseApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SofaWebViewClient extends WebViewClient {

    private String sofaScript;

    /* package */ SofaWebViewClient() {
        try {
            this.sofaScript = loadScriptFromAssets();
        } catch (final IOException ex) {
            LogUtil.exception(getClass(), "Unable to initialise SofaWebViewClient", ex);
        }
    }

    @Override
    public void onPageFinished(final WebView webView, final String url) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(this.sofaScript, null);
            webView.evaluateJavascript("SOFA.initialize();", null);
        } else {
            webView.loadUrl("javascript:" + this.sofaScript, null);
            webView.loadUrl("javascript:SOFA.initialize();");
        }
    }

    private String loadScriptFromAssets() throws IOException {
        final StringBuilder sb = new StringBuilder();
        final InputStream stream = BaseApplication.get().getResources().openRawResource(R.raw.sofa);
        final BufferedReader in = new BufferedReader(new InputStreamReader(stream));

        String str;
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }
        in.close();

        return sb.toString();
    }
}
