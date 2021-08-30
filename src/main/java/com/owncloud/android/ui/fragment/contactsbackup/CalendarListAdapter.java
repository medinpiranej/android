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

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.owncloud.android.databinding.CalendarlistListItemBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.events.VCardToggleEvent;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class CalendarListAdapter extends RecyclerView.Adapter<CalendarItemViewHolder> {
    private static final int SINGLE_SELECTION = 1;

    private List<OCFile> calendars;
    private Set<Integer> checkedVCards = new HashSet<>();

    private final Context context;

    public CalendarListAdapter(Context context) {
        this.context = context;
        calendars = new ArrayList<>();
    }

    public int getCheckedCount() {
        if (checkedVCards != null) {
            return checkedVCards.size();
        } else {
            return 0;
        }
    }

    public void replaceVCards(List<OCFile> calendars) {
        this.calendars = calendars;
        notifyDataSetChanged();
    }

    public void add(OCFile calendar) {
        calendars.add(calendar);
        notifyItemInserted(calendars.size());
    }

    public int[] getCheckedIntArray() {
        int[] intArray;
        if (checkedVCards != null && checkedVCards.size() > 0) {
            intArray = new int[checkedVCards.size()];
            int i = 0;
            for (int position : checkedVCards) {
                intArray[i] = position;
                i++;
            }
            return intArray;
        } else {
            return new int[0];
        }
    }

    @NonNull
    @Override
    public CalendarItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CalendarItemViewHolder(CalendarlistListItemBinding.inflate(LayoutInflater.from(context)));
    }

    @Override
    public void onBindViewHolder(@NonNull final CalendarItemViewHolder holder, final int position) {
        final int verifiedPosition = holder.getAdapterPosition();
        final OCFile ocFile = calendars.get(verifiedPosition);

        if (ocFile != null) {
            setChecked(checkedVCards.contains(position), holder.binding.name);

            holder.binding.name.setText(ocFile.getFileName());

            holder.setListener(v -> toggleCalendar(holder, verifiedPosition));
        }
    }

    private void setChecked(boolean checked, CheckedTextView checkedTextView) {
        checkedTextView.setChecked(checked);

        if (checked) {
            checkedTextView.getCheckMarkDrawable()
                .setColorFilter(ThemeColorUtils.primaryColor(context), PorterDuff.Mode.SRC_ATOP);
        } else {
            checkedTextView.getCheckMarkDrawable().clearColorFilter();
        }
    }

    private void toggleCalendar(CalendarItemViewHolder holder, int verifiedPosition) {
        CheckedTextView checkedTextView = holder.binding.name;

        checkedTextView.setChecked(!checkedTextView.isChecked());

        if (checkedTextView.isChecked()) {
            checkedTextView.getCheckMarkDrawable().setColorFilter(ThemeColorUtils.primaryColor(context),
                                                                  PorterDuff.Mode.SRC_ATOP);

            checkedVCards.add(verifiedPosition);
            if (checkedVCards.size() == SINGLE_SELECTION) {
                EventBus.getDefault().post(new VCardToggleEvent(true));
            }
        } else {
            checkedTextView.getCheckMarkDrawable().clearColorFilter();

            checkedVCards.remove(verifiedPosition);

            if (checkedVCards.isEmpty()) {
                EventBus.getDefault().post(new VCardToggleEvent(false));
            }
        }
    }

    @Override
    public int getItemCount() {
        return calendars.size();
    }

    public void selectAllFiles(boolean select) {
        checkedVCards = new HashSet<>();
        if (select) {
            for (int i = 0; i < calendars.size(); i++) {
                checkedVCards.add(i);
            }
        }

        if (checkedVCards.size() > 0) {
            EventBus.getDefault().post(new VCardToggleEvent(true));
        } else {
            EventBus.getDefault().post(new VCardToggleEvent(false));
        }

        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }
}
