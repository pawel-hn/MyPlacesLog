package pawel.hn.myplaceslog.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pawel.hn.myplaceslog.utils.DATE_FORMAT
import pawel.hn.myplaceslog.R
import pawel.hn.myplaceslog.databinding.FragmentDetailBinding
import pawel.hn.myplaceslog.model.Place
import pawel.hn.myplaceslog.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalCoroutinesApi
class PlacesHorizontalAdapter(private val mainViewModel: MainViewModel) : ListAdapter<Place,
        PlacesHorizontalAdapter.PlacesHorizontalViewHolder>(PlacesDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacesHorizontalViewHolder {
        val binding = FragmentDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlacesHorizontalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlacesHorizontalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    @ExperimentalCoroutinesApi
    inner class PlacesHorizontalViewHolder(private val binding: FragmentDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(place: Place) {
            binding.apply {
                textVieWTitle.text = place.name
                textViewDescriptionDetail.text = place.description
                val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                textViewDateDetail.text = dateFormatter.format(place.date)
                if (place.image != null) {
                    imageVieWDetail.setImageURI(Uri.parse(place.image))
                } else {
                    imageVieWDetail.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_placeholder)
                    )
                }
                textViewLocationDetail.text = place.location ?: root.context.getString(R.string.missing_location)

                buttonViewOnMapDetail.setOnClickListener {
                    if (place.location != null) {
                        mainViewModel.onViewOnMapClicked(place)
                    } else {
                        mainViewModel.showMsgOnDetail(root.context.getString(R.string.missing_location))
                    }
                }
                imageViewDetailFavourite.isVisible = place.favourite

            }
        }
    }
}