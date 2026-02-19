package paulify.baeumeinwien.data.remote.dto

import paulify.baeumeinwien.data.domain.Tree
import java.util.UUID

fun Feature.toTree(): Tree? {
    if (geometry.coordinates.size < 2) return null
    
    val longitude = geometry.coordinates[0]
    val latitude = geometry.coordinates[1]
    
    val treeId = properties.objectId?.takeIf { it.isNotBlank() }
        ?: properties.baumId?.takeIf { it.isNotBlank() }
        ?: id?.takeIf { it.isNotBlank() }
        ?: properties.fid?.takeIf { it.isNotBlank() }
        ?: UUID.randomUUID().toString()
    
    val speciesGerman = properties.speciesName?.takeIf { it.isNotBlank() } ?: "Unbekannte Art"
    
    val height = when {
        properties.heightText != null -> parseHeightFromText(properties.heightText)
        properties.heightCategory != null -> estimateHeightFromCategory(properties.heightCategory)
        else -> null
    }
    
    val crownDiameter = when {
        properties.crownDiameterText != null -> parseDiameterFromText(properties.crownDiameterText)
        properties.crownDiameterCategory != null -> estimateDiameterFromCategory(properties.crownDiameterCategory)
        else -> null
    }
    
    return Tree(
        id = treeId,
        speciesGerman = speciesGerman,
        speciesScientific = null,
        plantYear = properties.plantYear?.takeIf { it > 0 },
        trunkCircumference = properties.trunkCircumference?.takeIf { it > 0 },
        height = height,
        crownDiameter = crownDiameter,
        district = properties.district?.takeIf { it in 1..23 },
        area = properties.areaGroup?.takeIf { it.isNotBlank() },
        street = properties.objectStreet?.takeIf { it.isNotBlank() },
        latitude = latitude,
        longitude = longitude,
        fid = properties.fid,
        objectId = properties.objectId,
        baumId = properties.baumId,
        datenfuehrung = properties.datenfuehrung,
        isFavorite = false
    )
}

private fun parseHeightFromText(text: String): Double? {
    return try {
        val numbers = text.replace("m", "").replace("M", "")
            .split("bis", "to", "-")
            .mapNotNull { it.trim().toDoubleOrNull() }
        
        if (numbers.isNotEmpty()) {
            numbers.average()
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun parseDiameterFromText(text: String): Double? {
    return try {
        val numbers = text.replace("m", "").replace("M", "")
            .split("bis", "to", "-")
            .mapNotNull { it.trim().toDoubleOrNull() }
        
        if (numbers.isNotEmpty()) {
            numbers.average()
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun estimateHeightFromCategory(category: Int): Double? {
    return when (category) {
        1 -> 2.5
        2 -> 7.5
        3 -> 12.5
        4 -> 17.5
        5 -> 22.5
        6 -> 27.5
        7 -> 32.5
        8 -> 40.0
        else -> null
    }
}

private fun estimateDiameterFromCategory(category: Int): Double? {
    return when (category) {
        1 -> 2.0
        2 -> 5.0
        3 -> 8.0
        4 -> 11.0
        5 -> 14.0
        6 -> 17.0
        7 -> 20.0
        8 -> 25.0
        else -> null
    }
}

fun GeoJsonResponse.toTrees(): List<Tree> {
    val trees = features.mapNotNull { it.toTree() }
    android.util.Log.d("DtoMapper", "Mapped ${trees.size} trees from ${features.size} features")
    return trees
}
