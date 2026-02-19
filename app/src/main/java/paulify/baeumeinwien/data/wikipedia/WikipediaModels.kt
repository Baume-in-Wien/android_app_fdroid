package paulify.baeumeinwien.data.wikipedia

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class WikipediaSearchResponse(
    val query: Query? = null
)

@Serializable
data class Query(
    val pages: Map<String, Page>? = null,
    val search: List<SearchResult>? = null
)

@Serializable
data class SearchResult(
    val title: String,
    val pageid: Int
)

@Serializable
data class Page(
    val pageid: Int,
    val title: String,
    val extract: String? = null,
    val thumbnail: Thumbnail? = null,
    val original: Original? = null,
    val pageimage: String? = null,
    val imageinfo: List<ImageInfo>? = null
)

@Serializable
data class ImageInfo(
    val url: String,
    val descriptionurl: String? = null
)

@Serializable
data class Thumbnail(
    val source: String,
    val width: Int,
    val height: Int
)

@Serializable
data class Original(
    val source: String,
    val width: Int,
    val height: Int
)

data class WikipediaInfo(
    val title: String,
    val extract: String,
    val imageUrl: String?,
    val pageUrl: String
)
