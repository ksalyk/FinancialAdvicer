package kz.fearsom.financiallifev2.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.ui.screens.EraCard


@Composable
@Preview
fun EraCardPreview() {
    EraCard(era = SeedData.eras.first(), onClick = {})
}