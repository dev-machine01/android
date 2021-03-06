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
package org.monora.uprotocol.client.android.app

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.app.RecyclerViewFragment
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.actionperformer.*
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import com.genonbeta.android.framework.widget.recyclerview.FastScroller
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.dialog.SelectionEditorDialog
import org.monora.uprotocol.client.android.model.ContentModel
import org.monora.uprotocol.client.android.util.Selections
import org.monora.uprotocol.client.android.view.LongTextBubbleFastScrollViewProvider
import org.monora.uprotocol.client.android.widget.ListingAdapter
import org.monora.uprotocol.client.android.widget.ListingAdapterBase
import org.monora.uprotocol.client.android.widget.recyclerview.SwipeSelectionListener
import java.util.*

/**
 * created by: Veli
 * date: 21.11.2017 10:12
 */
abstract class ListingFragment<T : ContentModel, V : ViewHolder, E : ListingAdapter<T, V>>
    : RecyclerViewFragment<T, V, E>(), IListingFragment<T, V> {
    override var adapter: E
        get() = super.adapter
        set(value) {
            super.adapter = value
            engineConnection.setSelectionModelProvider(adapter)
            fastScroller?.recyclerView = listView
            adapter.fragment = this
        }

    override val adapterImpl: ListingAdapterBase<T>
        get() = adapter

    var bottomSpaceShown = false

    var dividerResId = R.id.abstract_layout_fast_scroll_recyclerview_bottom_divider

    override val engineConnection: IEngineConnection<T> by lazy {
        EngineConnection(this, this)
    }

    var fastScroller: FastScroller? = null
        private set

    override var filteringDelegate: FilteringDelegate<T> = object : FilteringDelegate<T> {
        override var keyword: String? = null
            set(value) {
                field = value?.trim()
                keywordMap = field?.split("".toRegex())
            }

        private var keywordMap: List<String>? = null

        override fun enabled(): Boolean = keywordMap != null

        override fun filter(listFragment: ListingFragmentBase<T>, item: T): Boolean {
            val keywordMap = keywordMap ?: return false
            keywordMap.forEach { keyword -> if (item.filter(keyword)) return true }
            return false
        }
    }

    override var filteringSupported: Boolean = false

    override var localSelectionActivated: Boolean = false
        set(value) {
            field = value
            if (!value) engineConnection.getSelectionList()?.let {
                if (it.isNotEmpty()) engineConnection.setSelected(it, IntArray(it.size), false)
            }
            activity?.invalidateOptionsMenu()
        }

    override val localSelectionMode: Boolean
        get() = activity !is PerformerEngineProvider && performerMenu != null

    var layoutResId = R.layout.abstract_layout_editable_list_fragment

    private var layoutClickListener: LayoutClickListener<V>? = null

    override var listView: RecyclerView
        get() = super.listView
        set(value) {
            super.listView = value

            snackbarContainer = value
            value.addOnItemTouchListener(SwipeSelectionListener(this))

            if (bottomSpaceShown) {
                value.clipToPadding = false
                value.setPadding(0, 0, 0, (resources.getDimension(R.dimen.fab_margin) * 12).toInt())
            }
        }

    private val performerEngine: IPerformerEngine = PerformerEngine()

    private var performerMenu: PerformerMenu? = null

    override var refreshRequested: Boolean = false

    private val selectionList: MutableList<T> = ArrayList()

    override var selectByClickEnabled: Boolean = false
        get() = field || localSelectionMode

    open fun onCreatePerformerMenu(context: Context): PerformerMenu? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selectByClickEnabled = it.getBoolean(ARG_SELECT_BY_CLICK, selectByClickEnabled)
            bottomSpaceShown = it.getBoolean(ARG_HAS_BOTTOM_SPACE, bottomSpaceShown)
        }
        performerMenu?.setUp(performerEngine)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fastScroller = view.findViewById(R.id.abstract_layout_fast_scroll_recyclerview_fastscroll_view)
        fastScroller?.viewProvider = LongTextBubbleFastScrollViewProvider()

        setDividerVisible(true)
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        performerMenu = onCreatePerformerMenu(context)
        engineConnection.addSelectionListener(this)
        getPerformerEngine()?.ensureSlot(this, engineConnection)
    }

    override fun onDetach() {
        super.onDetach()
        getPerformerEngine()?.removeSlot(engineConnection)
    }

    override fun onSelected(
        engine: IPerformerEngine, owner: IEngineConnection<T>, model: T, isSelected: Boolean, position: Int,
    ) {
        if (position >= 0) adapter.syncAndNotify(position) else adapter.syncAllAndNotify()
        ensureLocalSelection()
    }

    override fun onSelected(
        engine: IPerformerEngine, owner: IEngineConnection<T>, modelList: MutableList<T>,
        isSelected: Boolean, positions: IntArray,
    ) {
        adapter.syncAllAndNotify()
        ensureLocalSelection()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (localSelectionMode && localSelectionActivated) {
            performerMenu?.populateMenu(menu)
        } else {
            inflater.inflate(R.menu.actions_abs_editable_list, menu)
            val filterItem = menu.findItem(R.id.actions_abs_editable_filter)
            if (filterItem != null) {
                filterItem.isVisible = filteringSupported
                if (filteringSupported) {
                    val view = filterItem.actionView
                    if (view is SearchView) {
                        view.setOnQueryTextListener(
                            object : SearchView.OnQueryTextListener {
                                override fun onQueryTextSubmit(query: String): Boolean {
                                    refreshList()
                                    return true
                                }

                                override fun onQueryTextChange(newText: String): Boolean {
                                    filteringDelegate.keyword = newText
                                    refreshList()
                                    return true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.actions_abs_editable_multi_select && performerMenu != null && activity != null)
            localSelectionActivated = !localSelectionActivated
        else if (performerMenu?.onMenuItemClick(item) == true)
            localSelectionActivated = false
        else
            return super.onOptionsItemSelected(item)

        return true
    }

    private fun ensureLocalSelection() {
        val shouldActivate = engineConnection.getSelectionList()?.isNotEmpty() ?: false

        if (localSelectionActivated != shouldActivate) {
            Log.d(TAG, "ensureLocalSelection: Altering local selection state to '$shouldActivate'")
            localSelectionActivated = shouldActivate
        }
    }

    override fun getPerformerEngine(): IPerformerEngine? {
        activity?.let {
            if (it is PerformerEngineProvider) return it.getPerformerEngine()
        }

        return performerEngine
    }

    override fun getAvailableList(): MutableList<T> {
        return adapter.getList()
    }

    override fun getSelectionList(): MutableList<T> {
        return selectionList
    }

    override fun getUniqueSettingKey(setting: String): String {
        return javaClass.simpleName + "_" + setting
    }

    fun invokeClickListener(holder: V, longClick: Boolean): Boolean {
        return layoutClickListener?.onLayoutClick(this, holder, longClick) ?: false
    }

    override fun loadIfRequested(): Boolean {
        val refreshed = refreshRequested
        refreshRequested = false

        if (refreshed) {
            refreshList()
        }

        return refreshed
    }

    override fun openUri(uri: Uri): Boolean {
        return Files.openUri(requireContext(), uri)
    }

    override fun performLayoutClickOpen(holder: V): Boolean {
        val pos = holder.adapterPosition
        return pos != RecyclerView.NO_POSITION && performLayoutClickOpen(holder, adapterImpl.getItem(pos))
    }

    // FIXME: 2/20/21 Reimplement content opening if possible. Should we be doing ourselves?
    override fun performLayoutClickOpen(holder: V, target: T): Boolean = false

    override fun performDefaultLayoutLongClick(holder: V, target: T): Boolean {
        return false
    }

    override fun performLayoutClick(holder: V): Boolean {
        val position = holder.adapterPosition
        return if (position == RecyclerView.NO_POSITION) false else setItemSelected(holder)
                || invokeClickListener(holder, false)
                || performDefaultLayoutClick(holder, adapterImpl.getItem(position))
    }

    override fun performLayoutLongClick(holder: V): Boolean {
        val position = holder.adapterPosition
        return if (position == RecyclerView.NO_POSITION) false else invokeClickListener(holder, true)
                || performDefaultLayoutLongClick(holder, adapterImpl.getItem(position))
                || setItemSelected(holder, true)
    }

    override fun registerLayoutViewClicks(holder: V) {
        holder.itemView.setOnClickListener { performLayoutClick(holder) }
        holder.itemView.setOnLongClickListener { performLayoutLongClick(holder) }
    }

    fun setDividerVisible(visible: Boolean) {
        view?.let {
            val divider = it.findViewById<View>(dividerResId)
            divider.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    override fun setItemSelected(holder: V): Boolean {
        return setItemSelected(holder, selectByClickEnabled)
    }

    override fun setItemSelected(holder: V, force: Boolean): Boolean {
        Log.d(TAG, "setItemSelected: $selectByClickEnabled $force")

        if (engineConnection.getSelectionList().let { it != null && it.size <= 0 } && !force) {
            return false
        }

        try {
            engineConnection.setSelected(holder)
            return true
        } catch (e: SelectionModelNotFoundException) {
            e.printStackTrace()
        } catch (e: CouldNotAlterException) {
            if (BuildConfig.DEBUG) e.printStackTrace()
        }
        return false
    }

    override fun setLayoutClickListener(clickListener: LayoutClickListener<V>?) {
        layoutClickListener = clickListener
    }

    interface LayoutClickListener<V : ViewHolder> {
        fun onLayoutClick(listFragment: ListingFragmentBase<*>, holder: V, longClick: Boolean): Boolean
    }

    interface FilteringDelegate<T : ContentModel> {
        var keyword: String?

        fun disabled(): Boolean = !enabled()

        fun enabled(): Boolean

        fun filter(listFragment: ListingFragmentBase<T>, item: T): Boolean
    }

    open class SelectionCallback(
        val activity: Activity, private val provider: PerformerEngineProvider,
    ) : PerformerMenu.Callback, PerformerEngineProvider {
        private lateinit var previewSelections: MenuItem

        var foregroundConnection: IEngineConnection<*>? = null

        var cancellable = true

        override fun onPerformerMenuList(
            performerMenu: PerformerMenu,
            inflater: MenuInflater,
            targetMenu: Menu,
        ): Boolean {
            inflater.inflate(R.menu.action_mode_abs_editable, targetMenu)

            if (!cancellable)
                targetMenu.findItem(R.id.action_mode_abs_editable_cancel_selection).isVisible = false

            previewSelections = targetMenu.findItem(R.id.action_mode_abs_editable_preview_selections)

            getPerformerEngine()?.let { updateTitle(Selections.getTotalSize(it)) }
            return true
        }

        override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_mode_abs_editable_cancel_selection -> setSelectionState(false, false)
                R.id.action_mode_abs_editable_select_all -> setSelectionState(true, true)
                R.id.action_mode_abs_editable_select_none -> setSelectionState(false, true)
                R.id.action_mode_abs_editable_preview_selections -> SelectionEditorDialog(activity, provider).show()
            }

            // Returning false means that the target item did not execute anything other than small changes.
            // It can be said that descendants can interfere with this by making changes and returning true;
            return false
        }

        override fun onPerformerMenuItemSelection(
            performerMenu: PerformerMenu, engine: IPerformerEngine,
            owner: IBaseEngineConnection, selectionModel: SelectionModel,
            isSelected: Boolean, position: Int,
        ): Boolean {
            return true
        }

        override fun onPerformerMenuItemSelection(
            performerMenu: PerformerMenu, engine: IPerformerEngine, owner: IBaseEngineConnection,
            selectionModelList: MutableList<out SelectionModel>, isSelected: Boolean, positions: IntArray,
        ): Boolean {
            return true
        }

        override fun onPerformerMenuItemSelected(
            performerMenu: PerformerMenu, engine: IPerformerEngine,
            owner: IBaseEngineConnection, selectionModel: SelectionModel, isSelected: Boolean,
            position: Int,
        ) {
            updateTitle(Selections.getTotalSize(engine))
        }

        override fun onPerformerMenuItemSelected(
            performerMenu: PerformerMenu, engine: IPerformerEngine, owner: IBaseEngineConnection,
            selectionModelList: MutableList<out SelectionModel>, isSelected: Boolean, positions: IntArray,
        ) {
            updateTitle(Selections.getTotalSize(engine))
        }

        fun setSelectionState(newState: Boolean, tryForeground: Boolean) {
            val engine = provider.getPerformerEngine()
            val foreground = foregroundConnection

            if (foreground != null && tryForeground) {
                setSelectionState(foreground, newState)
            } else if (engine != null) for (baseEngineConnection in engine.getConnectionList()) {
                if (baseEngineConnection is IEngineConnection<*>) setSelectionState(
                    baseEngineConnection, newState
                )
            }
        }

        private fun <T : SelectionModel> setSelectionState(connection: IEngineConnection<T>, newState: Boolean) {
            connection.getAvailableList()?.let {
                if (it.size > 0) {
                    val positions = IntArray(it.size)
                    for (i in positions.indices)
                        positions[i] = i
                    connection.setSelected(it, positions, newState)
                }
            }
        }

        override fun getPerformerEngine(): IPerformerEngine? {
            return provider.getPerformerEngine()
        }

        private fun updateTitle(totalSelections: Int) {
            // For local selections, the menu may be invalidated or may be just created meaning the menu item may not be
            // available yet.
            previewSelections.title = totalSelections.toString()
            previewSelections.isEnabled = totalSelections > 0
        }
    }

    companion object {
        private val TAG = ListingFragment::class.simpleName

        const val ARG_SELECT_BY_CLICK = "argSelectByClick"

        const val ARG_HAS_BOTTOM_SPACE = "argHasBottomSpace"
    }
}