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
package org.monora.uprotocol.client.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.databinding.ListClientGridBinding
import org.monora.uprotocol.client.android.itemcallback.UClientItemCallback
import org.monora.uprotocol.client.android.util.Graphics
import org.monora.uprotocol.client.android.viewholder.ClientGridViewHolder
import javax.inject.Inject

/**
 * created by: veli
 * date: 3/11/19 7:43 PM
 */
@AndroidEntryPoint
class OnlineClientsFragment : Fragment(R.layout.layout_online_client) {
    @Inject
    lateinit var clientRepository: ClientRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        val adapter = Adapter()

        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager?.let {
            if (it is GridLayoutManager) {
                it.orientation = GridLayoutManager.HORIZONTAL
            }
        }

        clientRepository.getAll().observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    class Adapter : ListAdapter<UClient, ClientGridViewHolder>(UClientItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientGridViewHolder {
            return ClientGridViewHolder(
                ListClientGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: ClientGridViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).uid.hashCode().toLong()
        }
    }
}

