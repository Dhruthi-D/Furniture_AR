package io.github.chayanforyou.arfurniture.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chayanforyou.arfurniture.R
import io.github.chayanforyou.arfurniture.data.Product
import io.github.chayanforyou.arfurniture.data.designerColors
import io.github.chayanforyou.arfurniture.data.productCategories
import io.github.chayanforyou.arfurniture.data.products
import io.github.chayanforyou.arfurniture.ui.arview.ARCoachingOverlay
import io.github.chayanforyou.arfurniture.ui.arview.ARView
import io.github.chayanforyou.arfurniture.utils.extension.setColor
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt

class PlacedItem(
    val id: String = UUID.randomUUID().toString(),
    val product: Product,
    val anchorNode: AnchorNode,
    val modelNode: ModelNode,
    val initialScale: Float
) {
    var scalePercent by mutableIntStateOf(100)
    var selectedColor by mutableStateOf<Color?>(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val childNodes = rememberNodes()

    var showCoachingOverlay by remember { mutableStateOf(true) }
    var showUiControls by remember { mutableStateOf(false) }

    // Placed Items in the AR Room
    val placedItems = remember { mutableStateListOf<PlacedItem>() }
    var selectedPlacedItem by remember { mutableStateOf<PlacedItem?>(null) }

    // Selection Nodes
    var selectedAnchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    var selectedModelNode by remember { mutableStateOf<ModelNode?>(null) }

    // Triggers
    var modelToPlace by remember { mutableStateOf<String?>(null) }
    var pendingProduct by remember { mutableStateOf<Product?>(null) }
    var colorToApply by remember { mutableStateOf<Color?>(null) }

    // UI Dialogs
    var showCartSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        delay(2000)
        showCoachingOverlay = false
        showUiControls = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Augmented Reality Scene View
        ARView(
            childNodes = childNodes,
            modelToPlace = modelToPlace,
            onModelPlaced = { anchor, model, _ ->
                pendingProduct?.let { prod ->
                    val newItem = PlacedItem(
                        product = prod,
                        anchorNode = anchor,
                        modelNode = model,
                        initialScale = model.scale.x
                    )
                    placedItems.add(newItem)
                    selectedPlacedItem = newItem
                    selectedAnchorNode = anchor
                    selectedModelNode = model
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            onResetModelToPlace = {
                modelToPlace = null
                pendingProduct = null
            },
            selectedAnchorNode = selectedAnchorNode,
            selectedModelNode = selectedModelNode,
            onNodeSelected = { anchor, model ->
                if (anchor != null && model != null) {
                    val matching = placedItems.firstOrNull { it.anchorNode == anchor || it.modelNode == model }
                    if (matching != null) {
                        selectedPlacedItem = matching
                        selectedAnchorNode = anchor
                        selectedModelNode = model
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            },
            colorToApply = colorToApply,
            onScaleUpdated = { model ->
                val matching = placedItems.firstOrNull { it.modelNode == model }
                if (matching != null && matching.initialScale > 0f) {
                    matching.scalePercent = ((model.scale.x / matching.initialScale) * 100f).roundToInt()
                }
            }
        )

        // Top Header Bar: Clean Translucent White Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Room Total Pill
            val totalRoomPrice = placedItems.sumOf { it.product.price.toDouble() }.toInt()
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
                modifier = Modifier.clickable {
                    showCartSheet = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Room Budget",
                        tint = Color(0xFF673AB7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "₹$totalRoomPrice",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = " (${placedItems.size})",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Right Action Buttons: Share & Reset
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Snapshot & Share Button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            captureAndShareRoom(context)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Snapshot Room",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Clear All Button (shows when items exist)
                if (placedItems.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFEBEE),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable {
                                childNodes.forEach { it.destroy() }
                                childNodes.clear()
                                placedItems.clear()
                                selectedPlacedItem = null
                                selectedAnchorNode = null
                                selectedModelNode = null
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, "Room Cleared", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Room",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Coaching Overlay (Subtle, no black blackout screen)
        if (showCoachingOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.90f),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = stringResource(R.string.message_scan_horizontal_plane),
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 8.dp)
        ) {
            // Selected Model Floating Toolbar (Clean White Card, NOT a black screen!)
            AnimatedVisibility(
                visible = selectedPlacedItem != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                selectedPlacedItem?.let { item ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Row 1: Name, Dimensions, Scale controls, Rotate, Delete, and OK button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.product.name,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1
                                        )
                                        if (placedItems.size > 1) {
                                            val currentIndex = placedItems.indexOf(item)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFEDE7F6)
                                            ) {
                                                Text(
                                                    text = "${currentIndex + 1}/${placedItems.size}",
                                                    color = Color(0xFF673AB7),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "📏 ${item.product.dimensions} • ${item.scalePercent}%",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (item.scalePercent != 100) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE3F2FD),
                                                modifier = Modifier.clickable {
                                                    item.scalePercent = 100
                                                    item.modelNode.scale = Scale(item.initialScale)
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            ) {
                                                Text(
                                                    text = "Reset 1x",
                                                    color = Color(0xFF1976D2),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Overlapping Object Switcher Buttons
                                    if (placedItems.size > 1) {
                                        val currentIndex = placedItems.indexOf(item)
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFF3F3F3),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable {
                                                    val prevIdx = if (currentIndex > 0) currentIndex - 1 else placedItems.size - 1
                                                    val prevItem = placedItems[prevIdx]
                                                    selectedPlacedItem = prevItem
                                                    selectedAnchorNode = prevItem.anchorNode
                                                    selectedModelNode = prevItem.modelNode
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("◀", color = Color.Black, fontSize = 11.sp)
                                            }
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFF3F3F3),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable {
                                                    val nextIdx = (currentIndex + 1) % placedItems.size
                                                    val nextItem = placedItems[nextIdx]
                                                    selectedPlacedItem = nextItem
                                                    selectedAnchorNode = nextItem.anchorNode
                                                    selectedModelNode = nextItem.modelNode
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("▶", color = Color.Black, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    // Scale Down (-): 5% fine step
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF3F3F3),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                val newPercent = (item.scalePercent - 5).coerceAtLeast(30)
                                                item.scalePercent = newPercent
                                                item.modelNode.scale = Scale(item.initialScale * (newPercent / 100f))
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("-", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }

                                    // Scale Up (+): 5% fine step
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF3F3F3),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                val newPercent = (item.scalePercent + 5).coerceAtMost(350)
                                                item.scalePercent = newPercent
                                                item.modelNode.scale = Scale(item.initialScale * (newPercent / 100f))
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("+", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }

                                    // Rotate 45°
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF3F3F3),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                item.modelNode.rotation = Rotation(y = item.modelNode.rotation.y + 45f)
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Rotate 45°",
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Delete Selected Object
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFFEBEE),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                val anchor = item.anchorNode
                                                childNodes.remove(anchor)
                                                anchor.destroy()
                                                placedItems.remove(item)
                                                selectedPlacedItem = null
                                                selectedAnchorNode = null
                                                selectedModelNode = null
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFD32F2F),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Green OK Button: Confirms placement, dismisses toolbar, brings back catalog
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.clickable {
                                            val name = item.product.name
                                            selectedPlacedItem = null
                                            selectedAnchorNode = null
                                            selectedModelNode = null
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            Toast.makeText(context, "✅ $name placed! Ready for next.", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "OK", tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Fabric / Color Customizer Swatches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Fabric Colors:",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(designerColors) { (name, color) ->
                                        val isSelected = item.selectedColor == color
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF673AB7) else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    colorToApply = color
                                                    item.selectedColor = color
                                                    item.modelNode.setColor(color)
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    Toast.makeText(context, "Applied $name", Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = if (color == Color(0xFFF7F4EB)) Color.Black else Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips (Shown when NO furniture is being actively edited)
            AnimatedVisibility(visible = showUiControls && selectedPlacedItem == null) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(productCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF673AB7),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.90f),
                                labelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Products Carousel (Clean white cards matching the original app!)
            AnimatedVisibility(visible = showUiControls && selectedPlacedItem == null) {
                val filteredProducts = if (selectedCategory == "All") products else products.filter { it.category == selectedCategory }
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { prod ->
                        ProductCard(
                            product = prod,
                            onPlace = {
                                selectedPlacedItem = null
                                selectedAnchorNode = null
                                selectedModelNode = null
                                pendingProduct = prod
                                modelToPlace = prod.modelName
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }
        }

        // Room Cart / Budget Bottom Sheet
        if (showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Your AR Room Setup",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${placedItems.size} furniture items placed in room",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (placedItems.isEmpty()) {
                        Text(
                            text = "No furniture placed in the room yet. Tap items below to add them to your room!",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(placedItems) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF7F7F7),
                                    modifier = Modifier.clickable {
                                        selectedPlacedItem = item
                                        selectedAnchorNode = item.anchorNode
                                        selectedModelNode = item.modelNode
                                        showCartSheet = false
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = item.product.image),
                                            contentDescription = item.product.name,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFECECEC))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.product.name,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "₹${item.product.price.toInt()} • ${item.product.dimensions}",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val anchor = item.anchorNode
                                                childNodes.remove(anchor)
                                                anchor.destroy()
                                                placedItems.remove(item)
                                                if (selectedPlacedItem == item) {
                                                    selectedPlacedItem = null
                                                    selectedAnchorNode = null
                                                    selectedModelNode = null
                                                }
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = Color(0xFFD32F2F)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val grandTotal = placedItems.sumOf { it.product.price.toDouble() }.toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Room Budget:", color = Color.Gray, fontSize = 16.sp)
                            Text("₹$grandTotal", color = Color(0xFF2E7D32), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    childNodes.forEach { it.destroy() }
                                    childNodes.clear()
                                    placedItems.clear()
                                    selectedPlacedItem = null
                                    selectedAnchorNode = null
                                    selectedModelNode = null
                                    showCartSheet = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                            ) {
                                Text("Clear All")
                            }

                            Button(
                                onClick = {
                                    showCartSheet = false
                                    Toast.makeText(context, "🎉 Order request created for ₹$grandTotal!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.weight(2f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Order Setup", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onPlace: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onPlace() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7F7F7)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = product.image),
                    contentDescription = product.name,
                    modifier = Modifier.size(80.dp)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )

            Text(
                text = "📏 ${product.dimensions}",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${product.price.toInt()}",
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF673AB7),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Place in Room",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Capture high-resolution screenshot with watermark and launch Android Share Sheet
 */
private fun captureAndShareRoom(context: Context) {
    val activity = context as? Activity ?: return
    Toast.makeText(context, "📸 Capturing AR Room...", Toast.LENGTH_SHORT).show()

    val window = activity.window
    val view = window.decorView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val location = IntArray(2)
    view.getLocationInWindow(location)

    try {
        PixelCopy.request(
            window,
            Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
            bitmap,
            { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    val watermarked = addBrandWatermark(bitmap)
                    val imagePath = MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        watermarked,
                        "AR_Furniture_${System.currentTimeMillis()}",
                        "AR Furniture Room Design"
                    )
                    if (imagePath != null) {
                        val imageUri = Uri.parse(imagePath)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            putExtra(Intent.EXTRA_TEXT, "Check out my 3D Room Design created with ARFurniture! 🪑✨")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share your AR Room"))
                    } else {
                        Toast.makeText(context, "Saved snapshot to gallery!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Snapshot capture failed", Toast.LENGTH_SHORT).show()
                }
            },
            Handler(Looper.getMainLooper())
        )
    } catch (e: Exception) {
        Toast.makeText(context, "Capture error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun addBrandWatermark(original: Bitmap): Bitmap {
    val watermarked = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(watermarked)
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 42f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(6f, 3f, 3f, android.graphics.Color.BLACK)
    }
    canvas.drawText("🪑 ARFurniture", 60f, watermarked.height - 80f, textPaint)
    return watermarked
}
