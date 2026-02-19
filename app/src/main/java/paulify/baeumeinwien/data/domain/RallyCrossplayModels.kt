package paulify.baeumeinwien.data.domain

import kotlinx.serialization.Serializable


@Serializable
data class CrossplayRally(
    val id: String,
    val code: String,
    val name: String,
    val description: String = "",
    val creator_id: String,
    val creator_platform: String,
    val mode: String,
    val status: String = "active",
    val max_participants: Int = 50,
    val target_species_count: Int? = null,
    val time_limit_minutes: Int? = null,
    
    @Serializable(with = JsonListStringSerializer::class)
    val target_tree_ids: List<String>? = null,
    
    val district_filter: List<Int>? = null,
    val radius_meters: Int? = null,
    val center_lat: Double? = null,
    val center_lng: Double? = null,
    
    val created_at: String? = null,
    val started_at: String? = null,
    val ended_at: String? = null,
    
    val is_public: Boolean = false,
    val allow_join_after_start: Boolean = true
)

object JsonListStringSerializer : kotlinx.serialization.KSerializer<List<String>?> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "JsonListString", 
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: List<String>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val jsonArray = kotlinx.serialization.json.JsonArray(value.map { kotlinx.serialization.json.JsonPrimitive(it) })
            encoder.encodeString(jsonArray.toString())
        }
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): List<String>? {
        return try {
            val jsonDecoder = decoder as kotlinx.serialization.json.JsonDecoder
            val element = jsonDecoder.decodeJsonElement()
            
            when (element) {
                is kotlinx.serialization.json.JsonNull -> null
                is kotlinx.serialization.json.JsonArray -> element.map { 
                    (it as kotlinx.serialization.json.JsonPrimitive).content 
                }
                else -> {
                    android.util.Log.w("CrossplayRally", "Unexpected JSON element for target_tree_ids: $element")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CrossplayRally", "Failed to deserialize target_tree_ids", e)
            null
        }
    }
}

@Serializable
data class RallyParticipant(
    val id: String,
    val rally_id: String,
    val device_id: String,
    val platform: String,
    val display_name: String,
    
    val joined_at: String? = null,
    val last_active_at: String? = null,
    val is_active: Boolean = true,
    
    val species_collected: Int = 0,
    val trees_scanned: Int = 0
)

@Serializable
data class TreeCollection(
    val id: String,
    val rally_id: String,
    val participant_id: String,
    
    val tree_id: String,
    val species: String,
    val latitude: Double,
    val longitude: Double,
    
    val photo_url: String? = null,
    val notes: String? = null,
    
    val collected_at: String? = null
)

@Serializable
data class RallyEvent(
    val id: String,
    val rally_id: String,
    val event_type: String,
    val participant_id: String? = null,
    val event_data: Map<String, String>? = null,
    val created_at: String? = null
)

@Serializable
data class RallyStatistics(
    val total_participants: Int,
    val total_trees_collected: Int,
    val total_unique_species: Int,
    val top_collectors: List<TopCollector>,
    val most_collected_species: List<SpeciesCount>
)

@Serializable
data class TopCollector(
    val name: String,
    val platform: String,
    val species_count: Int,
    val tree_count: Int
)

@Serializable
data class SpeciesCount(
    val species: String,
    val count: Int
)

@Serializable

data class CreateRallyResult(
    val rally_id: String,
    val join_code: String,
    val rallyId: String? = null,
    val joinCode: String? = null
)

@Serializable
data class JoinRallyResult(
    val rally_id: String,
    val participant_id: String,
    val rallyId: String? = null,
    val participantId: String? = null
)

@Serializable
data class CollectTreeResult(
    val collection_id: String,
    val is_new_species: Boolean,
    val collectionId: String? = null,
    val isNewSpecies: Boolean? = null
)

sealed class RallyRealtimeEvent {
    data class TreeCollected(val collection: TreeCollection) : RallyRealtimeEvent()
    data class ParticipantJoined(val participant: RallyParticipant) : RallyRealtimeEvent()
    data class ParticipantLeft(val participantId: String) : RallyRealtimeEvent()
    data class RallyUpdated(val rally: CrossplayRally) : RallyRealtimeEvent()
    data class RallyFinished(val rallyId: String) : RallyRealtimeEvent()
}

data class RallyUiStateData(
    val rally: CrossplayRally? = null,
    val participants: List<RallyParticipant> = emptyList(),
    val collections: List<TreeCollection> = emptyList(),
    val statistics: RallyStatistics? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
