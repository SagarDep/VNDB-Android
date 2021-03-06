package com.booboot.vndbandroid.ui.hometabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.booboot.vndbandroid.R
import com.booboot.vndbandroid.extensions.home
import com.booboot.vndbandroid.extensions.observeOnce
import com.booboot.vndbandroid.extensions.selectIf
import com.booboot.vndbandroid.extensions.setupToolbar
import com.booboot.vndbandroid.extensions.toggle
import com.booboot.vndbandroid.extensions.updateTabs
import com.booboot.vndbandroid.model.vndbandroid.Preferences
import com.booboot.vndbandroid.model.vndbandroid.SORT_ID
import com.booboot.vndbandroid.model.vndbandroid.SORT_LENGTH
import com.booboot.vndbandroid.model.vndbandroid.SORT_POPULARITY
import com.booboot.vndbandroid.model.vndbandroid.SORT_PRIORITY
import com.booboot.vndbandroid.model.vndbandroid.SORT_RATING
import com.booboot.vndbandroid.model.vndbandroid.SORT_RELEASE_DATE
import com.booboot.vndbandroid.model.vndbandroid.SORT_STATUS
import com.booboot.vndbandroid.model.vndbandroid.SORT_VOTE
import com.booboot.vndbandroid.ui.base.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.home_tabs_fragment.*
import kotlinx.android.synthetic.main.vn_list_sort_bottom_sheet.*

class HomeTabsFragment : BaseFragment(), TabLayout.OnTabSelectedListener, View.OnClickListener {
    override val layout: Int = R.layout.home_tabs_fragment
    lateinit var viewModel: HomeTabsViewModel
    lateinit var sortBottomSheetBehavior: BottomSheetBehavior<View>

    private var type: Int = 0
    private var adapter: HomeTabsAdapter? = null
    private val sortBottomSheetButtons by lazy {
        listOf(buttonReverseSort, buttonSortID, buttonSortReleaseDate, buttonSortLength, buttonSortPopularity, buttonSortRating, buttonSortStatus, buttonSortVote, buttonSortPriority)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        type = arguments?.getInt(LIST_TYPE_ARG) ?: VNLIST
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity ?: return

        setupToolbar()
        floatingSearchButton.setOnClickListener(this)

        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(this)

        sortBottomSheetBehavior = BottomSheetBehavior.from(sortBottomSheet)
        sortBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        sortBottomSheetHeader.setOnClickListener(this)
        sortBottomSheetButtons.forEach { it.setOnClickListener(this) }

        viewModel = ViewModelProviders.of(this).get(HomeTabsViewModel::class.java)
        viewModel.titlesData.observe(this, Observer { showTitles(it) })
        viewModel.sortData.observe(this, Observer { showSort() })
        viewModel.errorData.observeOnce(this, ::showError)
        viewModel.loadingData.observe(this, Observer { showLoading(it) })
        home()?.viewModel?.syncAccountData?.observe(this, Observer { it?.let { update() } })
        update(false)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        sortBottomSheetBehavior.state = savedInstanceState?.getInt(SORT_BOTTOM_SHEET_STATE) ?: BottomSheetBehavior.STATE_HIDDEN
    }

    private fun update(force: Boolean = true) = viewModel.getTabTitles(type, force)

    private fun showTitles(titles: List<String>?) {
        if (titles == null) return

        val tabLayoutEmpty = tabLayout.tabCount <= 0
        tabLayout.updateTabs(titles)

        if (tabLayoutEmpty) { // INIT
            adapter = HomeTabsAdapter(childFragmentManager, tabLayout.tabCount, type)
            viewPager.adapter = adapter
            if (currentPage >= 0) viewPager.currentItem = currentPage
        }
    }

    private fun showSort() {
        buttonReverseSort?.selectIf(Preferences.reverseSort)
        buttonSortID?.selectIf(Preferences.sort == SORT_ID)
        buttonSortReleaseDate?.selectIf(Preferences.sort == SORT_RELEASE_DATE)
        buttonSortLength?.selectIf(Preferences.sort == SORT_LENGTH)
        buttonSortPopularity?.selectIf(Preferences.sort == SORT_POPULARITY)
        buttonSortRating?.selectIf(Preferences.sort == SORT_RATING)
        buttonSortStatus?.selectIf(Preferences.sort == SORT_STATUS)
        buttonSortVote?.selectIf(Preferences.sort == SORT_VOTE)
        buttonSortPriority?.selectIf(Preferences.sort == SORT_PRIORITY)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_tabs_fragment, menu)
        menu.findItem(R.id.action_filter)?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sort -> sortBottomSheetBehavior.toggle()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.sortBottomSheetHeader -> sortBottomSheetBehavior.toggle()
            R.id.buttonReverseSort -> viewModel.reverseSort()
            R.id.buttonSortID -> viewModel.setSort(SORT_ID)
            R.id.buttonSortReleaseDate -> viewModel.setSort(SORT_RELEASE_DATE)
            R.id.buttonSortLength -> viewModel.setSort(SORT_LENGTH)
            R.id.buttonSortPopularity -> viewModel.setSort(SORT_POPULARITY)
            R.id.buttonSortRating -> viewModel.setSort(SORT_RATING)
            R.id.buttonSortStatus -> viewModel.setSort(SORT_STATUS)
            R.id.buttonSortVote -> viewModel.setSort(SORT_VOTE)
            R.id.buttonSortPriority -> viewModel.setSort(SORT_PRIORITY)
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        viewPager.currentItem = tab.position
        currentPage = tab.position
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}

    override fun onTabReselected(tab: TabLayout.Tab) {}

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SORT_BOTTOM_SHEET_STATE, sortBottomSheetBehavior.state)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val LIST_TYPE_ARG = "LIST_TYPE_ARG"
        const val TAB_VALUE_ARG = "TAB_VALUE_ARG"
        const val SORT_BOTTOM_SHEET_STATE = "SORT_BOTTOM_SHEET_STATE"
        const val VNLIST = 1
        const val VOTELIST = 2
        const val WISHLIST = 3

        var currentPage = 0
    }
}