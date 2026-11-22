package io.github.chayanforyou.arfurniture.data

import androidx.compose.ui.graphics.Color
import io.github.chayanforyou.arfurniture.R

val fabrics = listOf(
    FabricItem("Off White", R.drawable.fabric_off_wihite),
    FabricItem("Ash", R.drawable.fabric_ash),
    FabricItem("Royal Blue", R.drawable.fabric_blue),
    FabricItem("Ruby Red", R.drawable.fabric_red),
)

val designerColors = listOf(
    "Cream White" to Color(0xFFF7F4EB),
    "Charcoal" to Color(0xFF2B2D2F),
    "Royal Blue" to Color(0xFF1E3D59),
    "Ruby Red" to Color(0xFF8B1E3F),
    "Forest Green" to Color(0xFF2D5A27),
    "Warm Amber" to Color(0xFFD4A373),
    "Terracotta" to Color(0xFFC85A32),
)

val productCategories = listOf("All", "Sofas", "Tables", "Beds", "Chairs", "Desks")

val products = listOf(
    Product(0, R.drawable.double_bed, "Double bed", "Solid Teak Wood", "double_bed", 25_000f, "200 × 160 × 90 cm", "Beds"),
    Product(1, R.drawable.standing_desk, "Standing desk", "Height Adjustable", "standing_desk", 12_000f, "140 × 70 × 110 cm", "Desks"),
    Product(2, R.drawable.dining_table, "Dining table", "Natural Oak", "dining_table", 7_000f, "180 × 90 × 75 cm", "Tables"),
    Product(3, R.drawable.folding_table, "Folding table", "Space-Saving Ceramic", "folding_table", 5_000f, "100 × 60 × 75 cm", "Tables"),
    Product(4, R.drawable.tuxedo_sofa, "Tuxedo sofa", "Premium Faux Leather", "tuxedo_sofa", 15_000f, "210 × 85 × 80 cm", "Sofas"),
    Product(5, R.drawable.apartment_sofa, "Apartment sofa", "Modern Compact Style", "apartment_sofa", 18_000f, "180 × 80 × 80 cm", "Sofas"),
    Product(6, R.drawable.dining_table_set, "Dining table set", "6-Seater Table & Chairs", "dining_table_set", 28_000f, "200 × 120 × 75 cm", "Tables"),
    Product(7, R.drawable.corner_sofa, "Corner sofa", "L-Shaped Sectional", "corner_sofa", 22_000f, "250 × 200 × 85 cm", "Sofas"),
    Product(8, R.drawable.three_seater_sofa, "Three seater sofa", "High Density Cushion", "three_seater_sofa", 20_000f, "220 × 90 × 85 cm", "Sofas"),
    Product(9, R.drawable.bauhaus_chair, "Bauhaus Chair", "Italian Leather Accent", "bauhaus_chair", 6_000f, "75 × 75 × 75 cm", "Chairs"),
)

data class FabricItem(var name: String, var imageId: Int)
