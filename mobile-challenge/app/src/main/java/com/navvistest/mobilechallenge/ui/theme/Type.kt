package com.navvistest.mobilechallenge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.navvistest.mobilechallenge.R

// Same fonts as navvis.com: Poppins for headings and buttons, Open Sans for everything else.
// Only the weights the app uses are bundled.
private val Poppins = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val OpenSans = FontFamily(
    Font(R.font.open_sans_regular, FontWeight.Normal),
)

private fun poppins(weight: FontWeight, size: Int, lineHeight: Int) =
    TextStyle(fontFamily = Poppins, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

private fun openSans(size: Int, lineHeight: Int) =
    TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Normal, fontSize = size.sp, lineHeight = lineHeight.sp)

val Typography = Typography(
    titleLarge = poppins(FontWeight.Bold, size = 20, lineHeight = 28),
    titleMedium = poppins(FontWeight.Medium, size = 16, lineHeight = 24),
    titleSmall = poppins(FontWeight.Medium, size = 14, lineHeight = 20),
    labelLarge = poppins(FontWeight.Medium, size = 14, lineHeight = 20),
    bodyLarge = openSans(size = 16, lineHeight = 24),
    bodyMedium = openSans(size = 14, lineHeight = 20),
    bodySmall = openSans(size = 12, lineHeight = 16),
)
