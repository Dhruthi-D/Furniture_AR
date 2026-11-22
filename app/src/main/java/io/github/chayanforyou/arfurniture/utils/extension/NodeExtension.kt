package io.github.chayanforyou.arfurniture.utils.extension

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color as ComposeColor
import io.github.chayanforyou.arfurniture.utils.constant.Constants
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import java.util.WeakHashMap

private val modelInitialScaleMap = WeakHashMap<ModelNode, Float>()

var ModelNode.initialBaseScale: Float
    get() = modelInitialScaleMap[this] ?: scale.x
    set(value) {
        modelInitialScaleMap[this] = value
    }

/**
 * Calibrated pinch-to-scale calculation:
 * - Damped delta (30% sensitivity rate) so each gesture expands or shrinks gently ("quite a bit, not too much")
 * - Per-event multiplier clamped between 0.93 and 1.07 to prevent hyper-sensitive sudden jumps
 * - Generous range: 30% minimum up to 350% maximum (allows freely scaling well past 180%)
 */
fun ModelNode.applyCalibratedScale(detectorFactor: Float) {
    val initial = initialBaseScale
    val minScale = initial * 0.30f
    val maxScale = initial * 3.50f

    val delta = detectorFactor - 1.0f
    val dampedMultiplier = (1.0f + (delta * 0.30f)).coerceIn(0.93f, 1.07f)

    val currentScale = scale.x
    val targetScale = (currentScale * dampedMultiplier).coerceIn(minScale, maxScale)
    scale = Scale(targetScale)
}

/**
 * Set model's color dynamically across all submesh materials
 *
 * @param color The Compose Color to tint the model
 */
fun ModelNode.setColor(color: ComposeColor) {
    try {
        materialInstances.flatten().forEach { material ->
            try {
                material.setParameter(
                    "baseColorFactor",
                    color.red,
                    color.green,
                    color.blue,
                    color.alpha
                )
            } catch (_: Throwable) {
                try {
                    material.setParameter(
                        "baseColor",
                        color.red,
                        color.green,
                        color.blue,
                        color.alpha
                    )
                } catch (_: Throwable) {}
            }
        }
    } catch (_: Throwable) {}
}

/**
 * Enable native SceneView gestures (two-finger pinch to scale, two-finger rotate).
 * Uses bounded scaling range so models never shrink into specks or blow up beyond room size.
 */
fun ModelNode.enableGestures() {
    setPriority(Constants.MODEL_RENDER_LOWEST_PRIORITY)
    isEditable = true
    isScaleEditable = true
    isRotationEditable = true
    val initial = scale.x
    initialBaseScale = initial
    editableScaleRange = (initial * 0.25f)..(initial * 3.60f)
}

/**
 * Start bouncing effect on receiver [Node].
 */
suspend fun Node.startBouncingEffect(
    targetValue: Float = Constants.MODEL_BOUNCING_HEIGHT
) {
    animate(
        initialValue = Constants.MODEL_NO_HEIGHT,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = Constants.MODEL_BOUNCING_DURATION),
            repeatMode = RepeatMode.Reverse
        )
    ) { value, _ ->
        position = Position(y = value)
    }
}

/**
 * End bouncing effect on receiver [Node].
 */
suspend fun Node.endBouncingEffect() {
    animate(
        initialValue = position.y,
        targetValue = Constants.MODEL_NO_HEIGHT,
        animationSpec = tween()
    ) { value, _ ->
        position = Position(y = value)
    }
}

/**
 * Start rotation effect on [Node].
 */
suspend fun Node.startModelRotation(
    duration: Int = Constants.MODEL_ROTATION_DURATION
) {
    animate(
        initialValue = rotation.y,
        targetValue = rotation.y + 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    ) { value, _ ->
        rotation = Rotation(y = value)
    }
}
