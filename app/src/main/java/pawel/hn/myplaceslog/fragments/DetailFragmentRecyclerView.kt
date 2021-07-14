package pawel.hn.myplaceslog.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pawel.hn.myplaceslog.LAST_VIEWED_STARTING_POSITION
import pawel.hn.myplaceslog.R
import pawel.hn.myplaceslog.adapters.PlacesHorizontalAdapter
import pawel.hn.myplaceslog.databinding.FragmentDetailRecyclerViewBinding
import pawel.hn.myplaceslog.viewmodels.MainViewModel

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class DetailFragmentRecyclerView : Fragment(R.layout.fragment_detail_recycler_view) {

    private lateinit var binding: FragmentDetailRecyclerViewBinding
    private lateinit var adapterHorizontal: PlacesHorizontalAdapter
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var detailLayoutManager: LinearLayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val placeClicked = DetailFragmentRecyclerViewArgs.fromBundle(requireArguments()).place

        binding = FragmentDetailRecyclerViewBinding.bind(view)
        adapterHorizontal = PlacesHorizontalAdapter(mainViewModel)

        var lastViewed = LAST_VIEWED_STARTING_POSITION

        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.lastViewedPlaceDetailFlow.collect {
                lastViewed = it.position
            }
        }

        binding.recyclerViewDetailHorizontal.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            detailLayoutManager = this.layoutManager as LinearLayoutManager
            adapter = adapterHorizontal
            PagerSnapHelper().attachToRecyclerView(this)
            addOnScrollListener(onScrollListener)
        }


        /**
        * When user navigates to this fragment, he expects to see details of place which he clicked.
         * So recycler view scrolls to place which is passed via SafeArgs. When user updates
         * or presses back to detail screen, position is taken from Data Store. Its saved there every
         * time user scrolls between horizontal detail screens.
         */
        mainViewModel.placesObservable.observe(viewLifecycleOwner) {
            adapterHorizontal.submitList(it)
            val position = it.indexOf(placeClicked)
            binding.recyclerViewDetailHorizontal.scrollToPosition(
                if (lastViewed < 0) position else lastViewed
            )
        }

        /**
         *One two events for this fragment related with one action. When user clicks
         * to view location on map, if location available, its shown on map, if not, snack bar is shonw
         * to user.
         */
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            mainViewModel.placeDetailEvent.collect{ event ->
                if (event is MainViewModel.PlacesEvent.ShowMsg) {
                    Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                }
                else if (event is MainViewModel.PlacesEvent.NavigateToMaps) {
                    val action = DetailFragmentRecyclerViewDirections
                        .actionDetailFragmentRecyclerViewToMapsFragment(
                            event.place.latitude.toString(),
                            event.place.longitude.toString(),
                            event.place.location!!
                        )
                    findNavController().navigate(action)
                }
            }
        }
        setHasOptionsMenu(true)
    }

    /**
     * When user scrolls lastViewed place position is saved.
     */
    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val position = detailLayoutManager.findFirstVisibleItemPosition()
            mainViewModel.lastViewedPlaceDetail(position)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.detail_menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_edit_current -> {
                val place = adapterHorizontal
                    .currentList[detailLayoutManager.findFirstVisibleItemPosition()]
                val action = DetailFragmentRecyclerViewDirections
                    .actionDetailFragmentRecyclerViewToAddItemFragment(
                        place,"Edit place"
                    )
                findNavController().navigate(action)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}