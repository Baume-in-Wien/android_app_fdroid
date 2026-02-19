package paulify.baeumeinwien.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get

object LeafDetector {
    fun isLeaf(bitmap: Bitmap): Boolean {
        val scaled = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        var greenPixels = 0
        
        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val pixel = scaled[x, y]
                if (isGreen(pixel)) {
                    greenPixels++
                }
            }
        }
        
        return greenPixels > (scaled.width * scaled.height * 0.15)
    }

    private fun isGreen(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        
        return g > r * 1.2 && g > b * 1.2 && g > 40
    }
}
