/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment.contactsbackup;

import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.owncloud.android.R;

import androidx.recyclerview.widget.RecyclerView;

class ContactItemViewHolder extends RecyclerView.ViewHolder {
    private ImageView badge;
    private CheckedTextView name;

    ContactItemViewHolder(View itemView) {
        super(itemView);

        badge = itemView.findViewById(R.id.contactlist_item_icon);
        name = itemView.findViewById(R.id.contactlist_item_name);


        itemView.setTag(this);
    }

    public void setVCardListener(View.OnClickListener onClickListener) {
        itemView.setOnClickListener(onClickListener);
    }

    public ImageView getBadge() {
        return badge;
    }

    public void setBadge(ImageView badge) {
        this.badge = badge;
    }

    public CheckedTextView getName() {
        return name;
    }

    public void setName(CheckedTextView name) {
        this.name = name;
    }
}
