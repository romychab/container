package com.elveum.container.demo.feature.examples.subject_chunks

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random

class MosaicDataSource @Inject constructor(
    private val random: Random,
) {

    private var loadIndex = 0

    suspend fun fetchMosaicInfo(): MosaicInfo {
        delay(2_000)
        val design = Designs[loadIndex % Designs.size]
        loadIndex++
        return MosaicInfo(
            designIndex = (loadIndex - 1) % Designs.size,
            rows = design.pixels.size,
            cols = design.pixels[0].length,
            title = design.title,
            backgroundColor = design.backgroundColor,
        )
    }

    suspend fun fetchTile(designIndex: Int, row: Int, col: Int): Color {
        delay(random.nextLong(100, 3500))
        val design = Designs[designIndex]
        val char = design.pixels[row][col]
        return design.palette[char] ?: design.backgroundColor
    }

    data class MosaicInfo(
        val designIndex: Int,
        val rows: Int,
        val cols: Int,
        val title: String,
        val backgroundColor: Color,
    )

    private data class Design(
        val title: String,
        val backgroundColor: Color,
        val palette: Map<Char, Color>,
        val pixels: List<String>,
    )

    private companion object {

        val Heart = Design(
            title = "Heart",
            backgroundColor = Color(0xFF0D1B2A),
            palette = mapOf('R' to Color(0xFFEF5350)),
            pixels = listOf(
                "............",
                "..RR....RR..",
                ".RRR....RRR.",
                "RRRRR..RRRRR",
                "RRRRRRRRRRRR",
                "RRRRRRRRRRRR",
                ".RRRRRRRRRR.",
                "..RRRRRRRR..",
                "...RRRRRR...",
                "....RRRR....",
                ".....RR.....",
                "............",
            ),
        )

        val Smiley = Design(
            title = "Smiley",
            backgroundColor = Color(0xFF00332B),
            palette = mapOf(
                'Y' to Color(0xFFFFBB58),
                'K' to Color(0xFF212121),
            ),
            pixels = listOf(
                "....YYYY....",
                "..YYYYYYYY..",
                ".YYYYYYYYYY.",
                ".YYYYYYYYYY.",
                ".YYYKYYKYYY.",
                ".YYYKYYKYYY.",
                ".YYYYYYYYYY.",
                ".YKYYYYYYKY.",
                ".YYKKKKKKYY.",
                ".YYYYYYYYYY.",
                "..YYYYYYYY..",
                "....YYYY....",
            ),
        )

        val Rocket = Design(
            title = "Rocket",
            backgroundColor = Color(0xFF0A0A1E),
            palette = mapOf(
                'W' to Color(0xFFACAFB1),
                'L' to Color(0xFF81D4FA),
                'R' to Color(0xFFEF5350),
                'O' to Color(0xFFFF9800),
            ),
            pixels = listOf(
                "....WWWW....",
                "...WWWWWW...",
                "..WWWWWWWW..",
                "..WWLLLLWW..",
                "..WWWWWWWW..",
                "..WWWWWWWW..",
                ".WWWWWWWWWW.",
                "WWWWWWWWWWWW",
                "W...WWWW...W",
                "....WWWW....",
                "...RRRRRR...",
                "....OOOO....",
            ),
        )

        val Designs = listOf(Heart, Smiley, Rocket)
    }
}
