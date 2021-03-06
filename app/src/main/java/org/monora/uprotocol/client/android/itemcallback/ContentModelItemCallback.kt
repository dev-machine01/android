/*
 * Copyright (C) 2021 Veli Tasalı
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

package org.monora.uprotocol.client.android.itemcallback

import androidx.recyclerview.widget.DiffUtil
import org.monora.uprotocol.client.android.model.ContentModel

class ContentModelItemCallback : DiffUtil.ItemCallback<ContentModel>() {
    override fun areItemsTheSame(oldItem: ContentModel, newItem: ContentModel): Boolean {
        return oldItem.id() == newItem.id()
    }

    override fun areContentsTheSame(oldItem: ContentModel, newItem: ContentModel): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }
}