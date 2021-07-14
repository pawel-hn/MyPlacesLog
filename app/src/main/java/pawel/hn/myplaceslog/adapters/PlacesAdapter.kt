package pawel.hn.myplaceslog.adapters

import android.net.Uri
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pawel.hn.myplaceslog.DATE_FORMAT
import pawel.hn.myplaceslog.R
import pawel.hn.myplaceslog.databinding.ItemPlaceBinding
import pawel.hn.myplaceslog.model.Place
import pawel.hn.myplaceslog.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*


@ExperimentalCoroutinesApi
class PlacesAdapter(
    private val listener: OnCLickListener,
    private val fragmentActivity: FragmentActivity,
    private val viewModel: MainViewModel
) : ListAdapter<Place, PlacesAdapter.PlacesViewHolder>(PlacesDiff()),
    ActionMode.Callback {

    interface OnCLickListener {
        fun placeClicked(placePosition: Int, place: Place)
        fun favouriteChecked(place: Place, isFav: Boolean)
    }

    /**
     * ActionMode is initialized when contextual menu is lunched (when signe item is long clicked)
     */
    private lateinit var actionMode: ActionMode
    private var multipleSelection = false
    private var selectedPlaces = arrayListOf<Place>()
    private var viewHolders = arrayListOf<PlacesViewHolder>()


    /**
     * To be called when action mode is on and apply proper title for actionmode app bar
     */
    private fun applySelection(place: Place) {
        if (selectedPlaces.contains(place)) {
            selectedPlaces.remove(place)
            setActionModeTitle()
        } else {
            selectedPlaces.add(place)
            setActionModeTitle()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacesViewHolder {
        val binding = ItemPlaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlacesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlacesViewHolder, position: Int) {
        viewHolders.add(holder)

        if (!selectedPlaces.contains(getItem(position)) && holder.binding.flipView.isBackSide) {
            holder.binding.flipView.flipTheView()
        } else if (selectedPlaces.contains(getItem(position)) && holder.binding.flipView.isFrontSide) {
            holder.binding.flipView.flipTheView()
        }

        holder.bind(getItem(position), position)
    }

    /**
     * below four methods come from ActionMode callback.
     */
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        actionMode = mode!!
        mode.menuInflater?.inflate(R.menu.contextual_menu, menu)
        setUpStatusBarColorInActionMode(R.color.dark_grey)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_delete_selected -> {
                selectedPlaces.forEach {
                    viewModel.deletePlace(it)
                }
                if (selectedPlaces.size == 1) {
                    viewModel.showMsgOnPlacesList(
                        fragmentActivity.getString(R.string.one_item_selected)
                    )
                } else {
                    viewModel.showMsgOnPlacesList(
                        "${selectedPlaces.size} ${fragmentActivity.getString(R.string.items_selected)}")
                }
                multipleSelection = false
                selectedPlaces.clear()
                actionMode.finish()
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        setUpStatusBarColorInActionMode(R.color.green_700)
        viewHolders.forEach { viewHolder ->
            if (viewHolder.binding.flipView.isBackSide) {
                viewHolder.binding.flipView.flipTheView()
            }
        }
        multipleSelection = false
        selectedPlaces.clear()
    }

    /**
     * Change title while ActionMode is on.
     */
    private fun setActionModeTitle() {
        when (selectedPlaces.size) {
            0 -> {
                actionMode.finish()
                multipleSelection = false
            }
            1 -> {
                actionMode.title = "${selectedPlaces.size} item selected"
            }
            else -> {
                actionMode.title = "${selectedPlaces.size} items selected"
            }
        }
    }

    private fun setUpStatusBarColorInActionMode(color: Int) {
        fragmentActivity.window.statusBarColor = ContextCompat.getColor(fragmentActivity, color)
    }

    inner class PlacesViewHolder(val binding: ItemPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(place: Place, position: Int) {
            binding.apply {
                textViewTitle.text = place.name
                textViewDescription.text = place.description
                val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                textViewDate.text = dateFormatter.format(place.date)

                checkboxFavourite.isChecked = place.favourite
                checkboxFavourite.setOnClickListener {
                    listener.favouriteChecked(place, checkboxFavourite.isChecked)
                }

                root.setOnClickListener {
                    if (multipleSelection) {
                        applySelection(place)
                        flipView.flipTheView()
                    } else {
                        if (position != RecyclerView.NO_POSITION) {
                            listener.placeClicked(position, place)
                        }
                    }
                }

                root.setOnLongClickListener {
                    if (!multipleSelection) {
                        fragmentActivity.startActionMode(this@PlacesAdapter)
                        multipleSelection = true
                        applySelection(place)
                        flipView.flipTheView()
                    }
                    true
                }

                if (place.image != null) {
                    flipViewFront.imageViewCircularFront.setImageURI(Uri.parse(place.image))
                } else {
                    flipViewFront.imageViewCircularFront.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_placeholder)
                    )
                }
            }
        }
    }
}

class PlacesDiff : DiffUtil.ItemCallback<Place>() {
    override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean = oldItem == newItem
}