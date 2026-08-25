package com.htoms.brief.theme

import androidx.compose.ui.graphics.Color
import com.htoms.brief.model.DeliveryStatus

/**
 * 공항 출발/도착 안내판에 한통도시락의 그레이·오렌지 제품 색상을 결합한 색 체계.
 */
object BriefTheme {
    val background = BrandPalette.background
    val card = BrandPalette.card
    val cardStroke = Color.White.copy(alpha = 0.12f)

    /** 제품 사진의 코럴 오렌지를 앱 전체 강조색으로 사용한다. */
    val brandOrange = BrandPalette.orange
    val boardCell = BrandPalette.boardCell
    val boardAmber = brandOrange
    val mutedText = BrandPalette.mutedText
    val accent = brandOrange
    val positive = Color(0xFF4DC78C) // Color(red: 0.30, green: 0.78, blue: 0.55)
    val warning = brandOrange
    val negative = Color(0xFFEB6166) // Color(red: 0.92, green: 0.38, blue: 0.40)

    val seriesPalette: List<Color> = listOf(
        brandOrange,
        Color(0xFFC7C7CC), // Color(red: 0.78, green: 0.78, blue: 0.80)
        Color(0xFFF59163), // Color(red: 0.96, green: 0.57, blue: 0.39)
        Color(0xFF7A7A80), // Color(red: 0.48, green: 0.48, blue: 0.50)
        Color(0xFFFCB894), // Color(red: 0.99, green: 0.72, blue: 0.58)
        Color(0xFF525257)  // Color(red: 0.32, green: 0.32, blue: 0.34)
    )

    fun color(status: DeliveryStatus): Color {
        return when (status) {
            DeliveryStatus.PREPARING -> accent
            DeliveryStatus.ACCEPTED -> warning
            DeliveryStatus.MOVING -> positive
            DeliveryStatus.DEPARTING -> mutedText
            DeliveryStatus.COMPLETED -> Color.White.copy(alpha = 0.72f)
            DeliveryStatus.INVOICE_ERROR, DeliveryStatus.UNDELIVERED -> Color(0xFFC73875) // Color(red: 0.78, green: 0.22, blue: 0.46)
            DeliveryStatus.UNAVAILABLE -> negative
        }
    }
}
