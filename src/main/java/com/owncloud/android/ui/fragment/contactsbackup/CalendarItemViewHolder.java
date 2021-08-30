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

import com.owncloud.android.databinding.CalendarlistListItemBinding;

import androidx.recyclerview.widget.RecyclerView;

class CalendarItemViewHolder extends RecyclerView.ViewHolder {
    public CalendarlistListItemBinding binding;

    CalendarItemViewHolder(CalendarlistListItemBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }

    public void setListener(View.OnClickListener onClickListener) {
        itemView.setOnClickListener(onClickListener);
    }
}
