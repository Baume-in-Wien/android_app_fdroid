package paulify.baeumeinwien.data.wikipedia

object Wikipedia {
    val repository: WikipediaRepository by lazy {
        WikipediaRepository()
    }
}
