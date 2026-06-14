package kz.fearsom.financiallifev2.ui.components.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.model.Difficulty
import kz.fearsom.financiallifev2.ui.components.difficultyColor
import kz.fearsom.financiallifev2.ui.components.label

@Composable
fun CharacterDifficultyBadge(difficulty: Difficulty) {
    val color = difficulty.difficultyColor()
    Text(
        text = difficulty.label(),
        // Raised from 9sp to 11sp for accessibility (below 12sp comfort floor)
        fontSize = 11.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(color.copy(0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}