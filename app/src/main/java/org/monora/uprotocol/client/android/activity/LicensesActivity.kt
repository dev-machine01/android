/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.monora.uprotocol.client.android.activity

import org.monora.uprotocol.client.android.R
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import org.monora.uprotocol.client.android.app.Activity

/**
 * created by: veli
 * date: 7/20/18 10:19 PM
 */
class LicensesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third_party_libraries)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actions_third_party_libraries, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.menu_action_info -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.text_help)
                    .setMessage(R.string.text_thirdPartyLibrariesHelp)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}