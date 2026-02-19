package paulify.baeumeinwien.data.realtime

import android.util.Log
import paulify.baeumeinwien.data.domain.*
import paulify.baeumeinwien.data.remote.SupabaseInstance
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.*
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.Json

class RallyRealtimeManager {
    
    private val supabase: SupabaseClient = SupabaseInstance.client
    private val TAG = "RallyRealtimeManager"
    
    private var currentChannel: RealtimeChannel? = null
    
    fun subscribeToRally(rallyId: String): Flow<RallyRealtimeEvent> {
        Log.d(TAG, "Subscribing to rally: $rallyId")
        
        val channelId = "rally:$rallyId"
        val channel = supabase.realtime.channel(channelId)
        currentChannel = channel
        
        return kotlinx.coroutines.flow.merge(
            participantJoinedFlow(channel, rallyId),
            
            participantLeftFlow(channel, rallyId),
            
            treeCollectedFlow(channel, rallyId),
            
            rallyUpdatedFlow(channel, rallyId)
        ).catch { e ->
            Log.e(TAG, "Realtime error for rally $rallyId", e)
        }
    }
    
    private fun participantJoinedFlow(channel: io.github.jan.supabase.realtime.RealtimeChannel, rallyId: String): Flow<RallyRealtimeEvent> {
        return channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "rally_participants"
            filter("rally_id", FilterOperator.EQ, rallyId)
        }.map { action ->
            val participant = action.decodeRecord<RallyParticipant>()
            Log.d(TAG, "Participant joined: ${participant.display_name} (${participant.platform})")
            RallyRealtimeEvent.ParticipantJoined(participant)
        }
    }
    
    private fun participantLeftFlow(channel: io.github.jan.supabase.realtime.RealtimeChannel, rallyId: String): Flow<RallyRealtimeEvent> {
        return channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "rally_participants"
            filter("rally_id", FilterOperator.EQ, rallyId)
        }.map { action ->
            val participant = action.decodeRecord<RallyParticipant>()
            if (!participant.is_active) {
                Log.d(TAG, "Participant left: ${participant.display_name}")
                RallyRealtimeEvent.ParticipantLeft(participant.id)
            } else {
                RallyRealtimeEvent.ParticipantJoined(participant)
            }
        }
    }
    
    private fun treeCollectedFlow(channel: io.github.jan.supabase.realtime.RealtimeChannel, rallyId: String): Flow<RallyRealtimeEvent> {
        return channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "rally_collections"
            filter("rally_id", FilterOperator.EQ, rallyId)
        }.map { action ->
            val collection = action.decodeRecord<TreeCollection>()
            Log.d(TAG, "Tree collected: ${collection.species} by participant ${collection.participant_id}")
            RallyRealtimeEvent.TreeCollected(collection)
        }
    }
    
    private fun rallyUpdatedFlow(channel: io.github.jan.supabase.realtime.RealtimeChannel, rallyId: String): Flow<RallyRealtimeEvent> {
        return channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "rallies"
            filter("id", FilterOperator.EQ, rallyId)
        }.map { action ->
            val rally = action.decodeRecord<CrossplayRally>()
            Log.d(TAG, "Rally updated: status=${rally.status}")
            
            if (rally.status == "finished") {
                RallyRealtimeEvent.RallyFinished(rally.id)
            } else {
                RallyRealtimeEvent.RallyUpdated(rally)
            }
        }
    }
    
    suspend fun unsubscribe() {
        currentChannel?.let { channel ->
            Log.d(TAG, "Unsubscribing from channel: ${channel.topic}")
            try {
                supabase.realtime.removeChannel(channel)
                currentChannel = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe", e)
            }
        }
    }
}

private fun <T> merge(vararg flows: Flow<T>): Flow<T> = kotlinx.coroutines.flow.merge(*flows)
