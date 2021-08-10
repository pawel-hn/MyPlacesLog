package pawel.hn.myplaceslog.fragments

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pawel.hn.myplaceslog.*
import pawel.hn.myplaceslog.adapters.PlacesAdapter
import pawel.hn.myplaceslog.databinding.FragmentPlacesBinding
import pawel.hn.myplaceslog.model.Place
import pawel.hn.myplaceslog.utils.ADD_EDIT_REQUEST_KEY
import pawel.hn.myplaceslog.utils.ADD_EDIT_RESULT
import pawel.hn.myplaceslog.utils.LAST_VIEWED_STARTING_POSITION
import pawel.hn.myplaceslog.utils.SortOrder
import pawel.hn.myplaceslog.viewmodels.MainViewModel


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class PlacesFragment : Fragment(R.layout.fragment_places), PlacesAdapter.OnCLickListener {

    private lateinit var placesAdapter: PlacesAdapter
    private lateinit var binding: FragmentPlacesBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPlacesBinding.bind(view)
        placesAdapter = PlacesAdapter(this, requireActivity(), viewModel)


        binding.apply {
            recyclerViewPlaces.adapter = placesAdapter
            ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerViewPlaces)

            buttonAddPlace.setOnClickListener {
                viewModel.onAddNewPlaceClick()
            }
        }
        viewModel.lastViewedPlaceDetail(LAST_VIEWED_STARTING_POSITION)

        setFragmentResultListener(ADD_EDIT_REQUEST_KEY) { _, bundle ->
            val result = bundle.getInt(ADD_EDIT_RESULT)
            viewModel.onAddEditResult(result, requireContext())
        }

        subscribeToEvent()
        subscribeToObservers()
        setHasOptionsMenu(true)
    }

    private fun subscribeToEvent() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.placesEvent.collect { event ->
                if (event is MainViewModel.PlacesEvent.ShowUndoDeleteMessage) {
                    Snackbar.make(requireView(), "Item deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo delete?") {
                            viewModel.insertPlace(event.place)
                        }.show()
                }
                else if (event is MainViewModel.PlacesEvent.NavigateToAddScreen) {
                    val action = PlacesFragmentDirections
                        .actionPlacesFragmentToAddItemFragment(null, "Add place")
                    findNavController().navigate(action)
                }
                else if (event is MainViewModel.PlacesEvent.NavigateToEditScreen) {
                    val action = PlacesFragmentDirections
                        .actionPlacesFragmentToAddItemFragment(event.place, "Edit place")
                    findNavController().navigate(action)
                }
                else if (event is MainViewModel.PlacesEvent.ShowMsg) {
                    Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                }
                else if (event is MainViewModel.PlacesEvent.NavigateToDetailScreen) {
                    val action = PlacesFragmentDirections
                        .actionPlacesFragmentToDetailFragmentRecyclerView(event.place)
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchQuery.value = newText.orEmpty()
                return true
            }
        })
        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.menu_favourite).isChecked =
                viewModel.preferencesFlow.first().showFavs
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_date -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
            }
            R.id.menu_sort_by_title -> {
                viewModel.onSortOrderSelected(SortOrder.BY_TITLE)
            }
            R.id.menu_favourite -> {
                item.isChecked = !item.isChecked
                viewModel.onShowFavsChecked(item.isChecked)
            }
            R.id.menu_delete_all -> {
                viewModel.deleteAll()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Subscribe to livedata containing list of items added by user
     */
    private fun subscribeToObservers() {
        viewModel.placesObservable.observe(viewLifecycleOwner) {
            placesAdapter.submitList(it)
        }
    }


    override fun placeClicked(placePosition: Int, place: Place) {
        viewModel.onPlaceClicked(place)
    }

    override fun favouriteChecked(place: Place, isFav: Boolean) {
        viewModel.placeFavouriteChecked(place, isFav)
    }

    private val swipeHelper = object : ItemTouchHelper
    .SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val place = placesAdapter.currentList[viewHolder.adapterPosition]
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    viewModel.onPlaceSwipeLeftEdit(place)
                }
                ItemTouchHelper.RIGHT -> {
                    viewModel.onPlaceSwipeRightDelete(place)
                }
            }
        }

        val backgroundRed = ColorDrawable(Color.RED)
        val backgroundBlue = ColorDrawable(Color.BLUE)


        /**
         * draw colors when swiping item left and right
         */
        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView

            val backgroundCornerOffset = 20
            if (dX > 0) {
                backgroundRed.setBounds(
                    itemView.left, itemView.top,
                    itemView.left + dX.toInt() - backgroundCornerOffset, itemView.bottom
                )
                backgroundRed.draw(c)
            } else if (dX < 0) {
                backgroundBlue.setBounds(
                    itemView.right + dX.toInt() - backgroundCornerOffset, itemView.top,
                    itemView.right, itemView.bottom
                )
                backgroundBlue.draw(c)
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
}