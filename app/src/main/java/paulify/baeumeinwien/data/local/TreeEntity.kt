package paulify.baeumeinwien.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import paulify.baeumeinwien.data.domain.Tree

@Entity(tableName = "trees")
data class TreeEntity(
    @PrimaryKey
    val id: String,
    val speciesGerman: String,
    val speciesScientific: String?,
    val plantYear: Int?,
    val trunkCircumference: Int?,
    val height: Double?,
    val crownDiameter: Double?,
    val district: Int?,
    val area: String?,
    val street: String?,
    val latitude: Double,
    val longitude: Double,
    val fid: String?,
    val objectId: String?,
    val baumId: String?,
    val datenfuehrung: String?,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

fun Tree.toEntity(): TreeEntity {
    return TreeEntity(
        id = id,
        speciesGerman = speciesGerman,
        speciesScientific = speciesScientific,
        plantYear = plantYear,
        trunkCircumference = trunkCircumference,
        height = height,
        crownDiameter = crownDiameter,
        district = district,
        area = area,
        street = street,
        latitude = latitude,
        longitude = longitude,
        fid = fid,
        objectId = objectId,
        baumId = baumId,
        datenfuehrung = datenfuehrung,
        isFavorite = isFavorite
    )
}

fun TreeEntity.toTree(): Tree {
    return Tree(
        id = id,
        speciesGerman = speciesGerman,
        speciesScientific = speciesScientific,
        plantYear = plantYear,
        trunkCircumference = trunkCircumference,
        height = height,
        crownDiameter = crownDiameter,
        district = district,
        area = area,
        street = street,
        latitude = latitude,
        longitude = longitude,
        fid = fid,
        objectId = objectId,
        baumId = baumId,
        datenfuehrung = datenfuehrung,
        isFavorite = isFavorite
    )
}
