package pawel.hn.myplaceslog.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "places_table")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String?,
    val description: String?,
    val image: String? = null,
    val date: Long,
    val favourite: Boolean,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable