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

package com.tokenbrowser.view.adapter.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.tokenbrowser.R;
import com.tokenbrowser.model.local.Dapp;
import com.tokenbrowser.view.adapter.listeners.OnItemClickListener;

public class SearchAppDappViewHolder extends RecyclerView.ViewHolder {

    private Button launchDappButton;
    private Dapp dapp;

    public SearchAppDappViewHolder(View itemView) {
        super(itemView);
        this.launchDappButton = (Button) itemView.findViewById(R.id.dapp_launch_button);
    }

    public SearchAppDappViewHolder setDapp(final Dapp dapp) {
        this.dapp = dapp;
        this.launchDappButton.setText(this.dapp.getAddress());
        return this;
    }

    public SearchAppDappViewHolder setListener(final OnItemClickListener<Dapp> listener) {
        this.launchDappButton.setOnClickListener(view -> listener.onItemClick(this.dapp));
        return this;
    }
}
