package paulify.baeumeinwien.data.wikipedia

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class WikipediaRepository {
    
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    private val baseUrl = "https://de.wikipedia.org/w/api.php"
    
    sealed class WikiResult<out T> {
        data class Success<T>(val data: T) : WikiResult<T>()
        data class Error(val message: String) : WikiResult<Nothing>()
    }
    
    suspend fun getTreeInfo(
        speciesGerman: String,
        speciesScientific: String?
    ): WikiResult<WikipediaInfo> = withContext(Dispatchers.IO) {
        try {
            val searchTerms = listOfNotNull(speciesScientific, speciesGerman)
            
            for (searchTerm in searchTerms) {
                val result = searchAndFetch(searchTerm)
                if (result is WikiResult.Success) {
                    return@withContext result
                }
            }
            
            WikiResult.Error("Keine Wikipedia-Informationen gefunden")
        } catch (e: Exception) {
            Log.e("WikipediaRepository", "Error fetching tree info", e)
            WikiResult.Error("Fehler beim Laden: ${e.message}")
        }
    }
    
    private suspend fun searchAndFetch(searchTerm: String): WikiResult<WikipediaInfo> {
        try {
            val searchResponse: WikipediaSearchResponse = client.get(baseUrl) {
                parameter("action", "query")
                parameter("format", "json")
                parameter("list", "search")
                parameter("srsearch", searchTerm)
                parameter("srlimit", 1)
                parameter("utf8", 1)
            }.body()
            
            val pageId = searchResponse.query?.search?.firstOrNull()?.pageid
                ?: return WikiResult.Error("Seite nicht gefunden")
            
            val pageTitle = searchResponse.query?.search?.firstOrNull()?.title
                ?: return WikiResult.Error("Seite nicht gefunden")
            
            val pageResponse: WikipediaSearchResponse = client.get(baseUrl) {
                parameter("action", "query")
                parameter("format", "json")
                parameter("pageids", pageId)
                parameter("prop", "extracts|pageimages")
                parameter("exintro", 1)
                parameter("explaintext", 1)
                parameter("piprop", "thumbnail|original|name")
                parameter("pithumbsize", 800)
                parameter("utf8", 1)
            }.body()
            
            val page = pageResponse.query?.pages?.values?.firstOrNull()
                ?: return WikiResult.Error("Seite nicht gefunden")
            
            Log.d("WikipediaRepository", "Page title: ${page.title}")
            Log.d("WikipediaRepository", "Page image name: ${page.pageimage}")
            Log.d("WikipediaRepository", "Original: ${page.original?.source}")
            Log.d("WikipediaRepository", "Thumbnail: ${page.thumbnail?.source}")
            
            var imageUrl: String? = page.original?.source ?: page.thumbnail?.source
            
            if (imageUrl == null && page.pageimage != null) {
                imageUrl = fetchImageInfo(page.pageimage)
            }
            
            Log.d("WikipediaRepository", "Final image URL: $imageUrl")
            
            val extract = page.extract?.trim()?.takeIf { it.isNotEmpty() }
                ?: return WikiResult.Error("Keine Beschreibung verfügbar")
            
            val wikiInfo = WikipediaInfo(
                title = page.title,
                extract = extract,
                imageUrl = imageUrl,
                pageUrl = "https://de.wikipedia.org/wiki/${page.title.replace(" ", "_")}"
            )
            
            return WikiResult.Success(wikiInfo)
        } catch (e: Exception) {
            Log.e("WikipediaRepository", "Error in searchAndFetch", e)
            return WikiResult.Error("Fehler beim Laden: ${e.message}")
        }
    }
    
    private suspend fun fetchImageInfo(imageName: String): String? {
        return try {
            Log.d("WikipediaRepository", "Fetching image info for: $imageName")
            
            val response: WikipediaSearchResponse = client.get(baseUrl) {
                parameter("action", "query")
                parameter("format", "json")
                parameter("titles", "File:$imageName")
                parameter("prop", "imageinfo")
                parameter("iiprop", "url")
                parameter("utf8", 1)
            }.body()
            
            val imageUrl = response.query?.pages?.values?.firstOrNull()?.imageinfo?.firstOrNull()?.url
            Log.d("WikipediaRepository", "Fetched image URL: $imageUrl")
            imageUrl
        } catch (e: Exception) {
            Log.e("WikipediaRepository", "Error fetching image info", e)
            null
        }
    }
    
    suspend fun getImageUrl(filename: String): WikiResult<String> = withContext(Dispatchers.IO) {
        try {
            val response: WikipediaSearchResponse = client.get(baseUrl) {
                parameter("action", "query")
                parameter("format", "json")
                parameter("titles", filename)
                parameter("prop", "imageinfo")
                parameter("iiprop", "url")
            }.body()
            
            val imageUrl = response.query?.pages?.values?.firstOrNull()
                ?.let { "Image URL parsing needed" }
                ?: return@withContext WikiResult.Error("Bild nicht gefunden")
            
            WikiResult.Success(imageUrl)
        } catch (e: Exception) {
            WikiResult.Error("Fehler beim Laden: ${e.message}")
        }
    }
    
    fun close() {
        client.close()
    }
}
