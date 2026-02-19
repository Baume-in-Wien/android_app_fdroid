package paulify.baeumeinwien.data.repository

data class DownloadProgress(
    val downloadedTrees: Int = 0,
    val totalTrees: Int = 0,
    val currentBatch: Int = 0,
    val totalBatches: Int = 0,
    val elapsedTimeMs: Long = 0,
    val estimatedRemainingMs: Long = 0
) {
    val progressPercent: Int
        get() = if (totalTrees > 0) ((downloadedTrees * 100) / totalTrees) else 0
    
    val isComplete: Boolean
        get() = downloadedTrees >= totalTrees && totalTrees > 0
}
