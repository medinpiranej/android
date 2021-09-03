/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2020 Andy Scherzinger
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
package com.owncloud.android.ui.fragment

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class BackupListFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(ContactsPreferenceActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun showContactListFragmentLoading() {
        val sut = testActivityRule.launchActivity(null)
        val file = OCFile("/", "00000001")
        val transaction = sut.supportFragmentManager.beginTransaction()

        transaction.replace(R.id.frame_container, BackupListFragment.newInstance(file, user))
        transaction.commit()

        waitForIdleSync()
        screenshot(sut)

        longSleep()
    }

    @Test
    @ScreenshotTest
    fun showContactList() {
        val sut = testActivityRule.launchActivity(null)
        val transaction = sut.supportFragmentManager.beginTransaction()
        val file = getFile("vcard.vcf")
        val ocFile = OCFile("/vcard.vcf", "00000002")
        ocFile.storagePath = file.absolutePath
        ocFile.mimeType = "text/vcard"

        transaction.replace(R.id.frame_container, BackupListFragment.newInstance(ocFile, user))
        transaction.commit()

        waitForIdleSync()
        screenshot(sut)

        longSleep()
    }

    @Test
    @ScreenshotTest
    fun showCalendarList() {
        val sut = testActivityRule.launchActivity(null)
        val transaction = sut.supportFragmentManager.beginTransaction()
        val file = getFile("calendar.ics")
        val ocFile = OCFile("/calendar.ics", "00000003")
        ocFile.storagePath = file.absolutePath
        ocFile.mimeType = "text/calendar"

        transaction.replace(R.id.frame_container, BackupListFragment.newInstance(ocFile, user))
        transaction.commit()

        waitForIdleSync()
        screenshot(sut)

        longSleep()
    }

    @Test
    @ScreenshotTest
    fun showCalendarAndContactsList() {
        val sut = testActivityRule.launchActivity(null)
        val transaction = sut.supportFragmentManager.beginTransaction()

        val calendarFile = getFile("calendar.ics")
        val calendarOcFile = OCFile("/calendar.ics", "00000003")
        calendarOcFile.storagePath = calendarFile.absolutePath
        calendarOcFile.mimeType = "text/calendar"

        val contactFile = getFile("vcard.vcf")
        val contactOcFile = OCFile("/vcard.vcf", "00000002")
        contactOcFile.storagePath = contactFile.absolutePath
        contactOcFile.mimeType = "text/vcard"

        val files = arrayOf(calendarOcFile, contactOcFile)
        transaction.replace(R.id.frame_container, BackupListFragment.newInstance(files, user))
        transaction.commit()

        waitForIdleSync()
        screenshot(sut)

        longSleep()
    }
}
