package pawel.hn.myplaceslog.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import pawel.hn.myplaceslog.R

class MapsFragment : Fragment(R.layout.fragment_maps) {


    private var latitude = -34.0
    private var longitude = 151.0
    private var address = ""

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        val place = LatLng(latitude, longitude)
        googleMap.apply {
            addMarker(MarkerOptions().position(place).title(address))
            moveCamera(CameraUpdateFactory.newLatLng(place))
            animateCamera(
                CameraUpdateFactory.newLatLngZoom(place, 15f)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        latitude = MapsFragmentArgs.fromBundle(requireArguments()).latitude.toDouble()
        longitude = MapsFragmentArgs.fromBundle(requireArguments()).longitude.toDouble()
        address = MapsFragmentArgs.fromBundle(requireArguments()).placeLocation

        mapFragment?.getMapAsync(callback)

    }
}