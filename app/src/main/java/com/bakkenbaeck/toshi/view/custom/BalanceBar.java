package com.bakkenbaeck.toshi.view.custom;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bakkenbaeck.toshi.R;
import com.robinhood.ticker.TickerUtils;
import com.robinhood.ticker.TickerView;

public class BalanceBar extends LinearLayout {
    private static final String TAG = "BalanceBar";

    public BalanceBar(final Context context) {
        super(context);
        init();
    }

    public BalanceBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view__balance_bar, this);
        /*((TextView)findViewById(R.id.balance)).setText(String.valueOf(TickerUtils.getDefaultListForUSCurrency()));
        ((TickerView)findViewById(R.id.balance1)).setCharacterList(TickerUtils.getDefaultListForUSCurrency());*/
    }

    public void setBalance(final String balance) {
        Log.d(TAG, "setBalance: " + balance);
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                //findViewById(R.id.dot_loader).setVisibility(INVISIBLE);
                //((TickerView) findViewById(R.id.balance1)).setText(balance);
                ((TextView)findViewById(R.id.balance)).setText(balance);
            }
        }, 200);
    }

    public void setReputation(final int reputationScore){
        Log.d(TAG, "setReputation: " + reputationScore);
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.reputation)).setText(String.valueOf(reputationScore));
            }
        }, 200);
    }
}
