/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment.contactsbackup;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.files.downloader.DownloadRequest;
import com.nextcloud.client.files.downloader.Request;
import com.nextcloud.client.files.downloader.Transfer;
import com.nextcloud.client.files.downloader.TransferManagerConnection;
import com.nextcloud.client.files.downloader.TransferState;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ContactlistFragmentBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.asynctasks.LoadContactsTask;
import com.owncloud.android.ui.events.VCardToggleEvent;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import ezvcard.VCard;
import kotlin.Unit;

/**
 * This fragment shows all contacts or calendars from files and allows to import them.
 */
public class BackupListFragment extends FileFragment implements Injectable {
    public static final String TAG = BackupListFragment.class.getSimpleName();

    public static final String FILE_NAMES = "FILE_NAMES";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String USER = "USER";
    public static final String CHECKED_ITEMS_ARRAY_KEY = "CHECKED_ITEMS";

    private static final int SINGLE_ACCOUNT = 1;

    private ContactlistFragmentBinding binding;

    private ContactListAdapter contactListAdapter;
    private CalendarListAdapter calenderListAdapter;
    private final List<VCard> vCards = new ArrayList<>();
    private final List<OCFile> ocFiles = new ArrayList<>();
    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    @Inject BackgroundJobManager backgroundJobManager;
    private TransferManagerConnection fileDownloader;
    private LoadContactsTask loadContactsTask = null;

    public static BackupListFragment newInstance(OCFile file, User user) {
        BackupListFragment frag = new BackupListFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(FILE_NAME, file);
        arguments.putParcelable(USER, user);
        frag.setArguments(arguments);

        return frag;
    }

    public static BackupListFragment newInstance(OCFile[] files, User user) {
        BackupListFragment frag = new BackupListFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelableArray(FILE_NAMES, files);
        arguments.putParcelable(USER, user);
        frag.setArguments(arguments);

        return frag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_contact_list, menu);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = ContactlistFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        setHasOptionsMenu(true);

        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        if (contactsPreferenceActivity != null) {
            ActionBar actionBar = contactsPreferenceActivity.getSupportActionBar();
            if (actionBar != null) {
                ThemeToolbarUtils.setColoredTitle(actionBar, R.string.actionbar_calendar_contacts_restore, getContext());
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            contactsPreferenceActivity.setDrawerIndicatorEnabled(false);
        }

        if (savedInstanceState == null) {
            contactListAdapter = new ContactListAdapter(accountManager, clientFactory, getContext(), vCards);
            calenderListAdapter = new CalendarListAdapter(requireContext());
        } else {
            Set<Integer> checkedItems = new HashSet<>();
            int[] itemsArray = savedInstanceState.getIntArray(CHECKED_ITEMS_ARRAY_KEY);
            if (itemsArray != null) {
                for (int checkedItem : itemsArray) {
                    checkedItems.add(checkedItem);
                }
            }
            if (checkedItems.size() > 0) {
                onMessageEvent(new VCardToggleEvent(true));
            }
            contactListAdapter = new ContactListAdapter(accountManager, getContext(), vCards, checkedItems);
            calenderListAdapter = new CalendarListAdapter(requireContext());
        }
        binding.contactlistRecyclerview.setAdapter(contactListAdapter);
        binding.contactlistRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.calendarRecyclerview.setAdapter(calenderListAdapter);
        binding.calendarRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));


        Bundle arguments = getArguments();
        if (arguments == null) {
            // TODO set error
            return view;
        }

        if (arguments.getParcelable(FILE_NAME) != null) {
            ocFiles.add(arguments.getParcelable(FILE_NAME));
        } else if (arguments.getParcelableArray(FILE_NAMES) != null) {
            for (Parcelable file : arguments.getParcelableArray(FILE_NAMES)) {
                ocFiles.add((OCFile) file);
            }
        } else {
            // TODO set error
            return view;
        }

        //setFile(ocFile); // TODO check

        User user = getArguments().getParcelable(USER);
        fileDownloader = new TransferManagerConnection(getActivity(), user);
        fileDownloader.registerTransferListener(this::onDownloadUpdate);
        fileDownloader.bind();

        boolean calendarFilePending = false;
        boolean contactsFilePending = false;
        for (OCFile file : ocFiles) {
            if (!file.isDown()) {
                Request request = new DownloadRequest(user, file);
                fileDownloader.enqueue(request);

                if (MimeTypeUtil.isVCard(file)) {
                    contactsFilePending = true;
                } else {
                    calendarFilePending = true;
                }
            }

            if (MimeTypeUtil.isVCard(file) && file.isDown()) {
                loadContactsTask = new LoadContactsTask(this, file);
                loadContactsTask.execute();
                contactsFilePending = true;
            }

            if (MimeTypeUtil.isCalendar(file) && file.isDown()) {
                calenderListAdapter.add(file);
            }
        }

        binding.contactlistRestoreSelected.setOnClickListener(v -> {
            if (checkAndAskForContactsWritePermission()) {
                getAccountForImport();
            }
        });

        binding.contactlistRestoreSelected.setTextColor(ThemeColorUtils.primaryAccentColor(getContext()));

        int accentColor = ThemeColorUtils.primaryAccentColor(requireContext());
        binding.calendarsHeadline.setTextColor(accentColor);
        binding.contactsHeadline.setTextColor(accentColor);

        if (!calendarFilePending && calenderListAdapter.isEmpty()) {
            binding.calendarRecyclerview.setVisibility(View.GONE);
            binding.calendarsHeadline.setVisibility(View.GONE);
        }

        if (!contactsFilePending) {
            binding.contactlistRecyclerview.setVisibility(View.GONE);
            binding.contactsHeadline.setVisibility(View.GONE);
        }

        if (!contactsFilePending && !calendarFilePending) {
            showLoadingMessage(false);
        }

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (fileDownloader != null) {
            fileDownloader.unbind();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(CHECKED_ITEMS_ARRAY_KEY, contactListAdapter.getCheckedIntArray());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(VCardToggleEvent event) {
        if (event.showRestoreButton) {
            binding.contactlistRestoreSelectedContainer.setVisibility(View.VISIBLE);
        } else {
            binding.contactlistRestoreSelectedContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        contactsPreferenceActivity.setDrawerIndicatorEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void onResume() {
        super.onResume();
        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        contactsPreferenceActivity.setDrawerIndicatorEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        if (loadContactsTask != null) {
            loadContactsTask.cancel(true);
        }
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
            if (contactsPreferenceActivity != null) {
                contactsPreferenceActivity.onBackPressed();
            }
            retval = true;
        } else if (itemId == R.id.action_select_all) {
            item.setChecked(!item.isChecked());
            setSelectAllMenuItem(item, item.isChecked());
            contactListAdapter.selectAllFiles(item.isChecked());
            retval = true;
        } else {
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    public void showLoadingMessage(boolean showIt) {
        binding.loadingListContainer.setVisibility(showIt ? View.VISIBLE : View.GONE);
    }

    private void setSelectAllMenuItem(MenuItem selectAll, boolean checked) {
        selectAll.setChecked(checked);
        if (checked) {
            selectAll.setIcon(R.drawable.ic_select_none);
        } else {
            selectAll.setIcon(R.drawable.ic_select_all);
        }
    }

    private void importContacts(ContactsAccount account) {
        backgroundJobManager.startImmediateContactsImport(account.getName(),
                                                          account.getType(),
                                                          getFile().getStoragePath(),
                                                          contactListAdapter.getCheckedIntArray());

        Snackbar
            .make(
                binding.contactlistRecyclerview,
                R.string.contacts_preferences_import_scheduled,
                Snackbar.LENGTH_LONG
                 )
            .show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    getActivity().finish();
                }
            }
        }, 1750);
    }

    private void getAccountForImport() {
        final ArrayList<ContactsAccount> contactsAccounts = new ArrayList<>();

        // add local one
        contactsAccounts.add(new ContactsAccount("Local contacts", null, null));

        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                                                             new String[]{ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE},
                                                             null,
                                                             null,
                                                             null);

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));
                    String type = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));

                    ContactsAccount account = new ContactsAccount(name, name, type);

                    if (!contactsAccounts.contains(account)) {
                        contactsAccounts.add(account);
                    }
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log_OC.d(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (contactsAccounts.size() == SINGLE_ACCOUNT) {
            importContacts(contactsAccounts.get(0));
        } else {
            ArrayAdapter adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, contactsAccounts);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.contactlist_account_chooser_title)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importContacts(contactsAccounts.get(which));
                    }
                }).show();
        }
    }

    private boolean checkAndAskForContactsWritePermission() {
        // check permissions
        if (!PermissionUtil.checkSelfPermission(getContext(), Manifest.permission.WRITE_CONTACTS)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS},
                               PermissionUtil.PERMISSIONS_WRITE_CONTACTS);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtil.PERMISSIONS_WRITE_CONTACTS) {
            for (int index = 0; index < permissions.length; index++) {
                if (Manifest.permission.WRITE_CONTACTS.equalsIgnoreCase(permissions[index])) {
                    if (grantResults[index] >= 0) {
                        getAccountForImport();
                    } else {
                        if (getView() != null) {
                            Snackbar.make(getView(), R.string.contactlist_no_permission, Snackbar.LENGTH_LONG)
                                .show();
                        } else {
                            Toast.makeText(getContext(), R.string.contactlist_no_permission, Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
                }
            }
        }
    }

    private Unit onDownloadUpdate(Transfer download) {
        final Activity activity = getActivity();
        if (download.getState() == TransferState.COMPLETED && activity != null) {
            OCFile ocFile = download.getFile();

            if (MimeTypeUtil.isVCard(ocFile)) {
                loadContactsTask = new LoadContactsTask(this, ocFile);
                loadContactsTask.execute();
            }
        }
        return Unit.INSTANCE;
    }

    public void loadVCards(List<VCard> cards) {
        showLoadingMessage(false);
        vCards.clear();
        vCards.addAll(cards);
        contactListAdapter.replaceVCards(vCards);
    }

    public static String getDisplayName(VCard vCard) {
        if (vCard.getFormattedName() != null) {
            return vCard.getFormattedName().getValue();
        } else if (vCard.getTelephoneNumbers() != null && vCard.getTelephoneNumbers().size() > 0) {
            return vCard.getTelephoneNumbers().get(0).getText();
        } else if (vCard.getEmails() != null && vCard.getEmails().size() > 0) {
            return vCard.getEmails().get(0).getValue();
        }

        return "";
    }
}
