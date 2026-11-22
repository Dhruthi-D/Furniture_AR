package io.github.chayanforyou.arfurniture.ui.arview

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import io.github.chayanforyou.arfurniture.utils.extension.applyCalibratedScale
import io.github.chayanforyou.arfurniture.utils.extension.enableGestures
import io.github.chayanforyou.arfurniture.utils.extension.initialBaseScale
import io.github.chayanforyou.arfurniture.utils.extension.setColor
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.gesture.RotateGestureDetector
import io.github.sceneview.gesture.ScaleGestureDetector
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import java.util.EnumSet

@Composable
internal fun ARView(
    childNodes: MutableList<Node>,
    modelToPlace: String?,
    onModelPlaced: (AnchorNode, ModelNode, String) -> Unit,
    onResetModelToPlace: () -> Unit,
    selectedAnchorNode: AnchorNode?,
    selectedModelNode: ModelNode?,
    onNodeSelected: (AnchorNode?, ModelNode?) -> Unit,
    colorToApply: ComposeColor?,
    onScaleUpdated: ((ModelNode) -> Unit)? = null
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)

    var planeRenderer by remember { mutableStateOf(true) }
    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var frame by remember { mutableStateOf<Frame?>(null) }
    var lastMoveTime by remember { mutableStateOf(0L) }

    // Live color updates on selected node
    LaunchedEffect(colorToApply, selectedModelNode) {
        if (colorToApply != null && selectedModelNode != null) {
            selectedModelNode.setColor(colorToApply)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            collisionSystem = collisionSystem,
            cameraNode = cameraNode,
            planeRenderer = planeRenderer,
            sessionCameraConfig = { session -> session.disableDepthConfig() },
            sessionConfiguration = { _, config -> config.configure() },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { _, node ->
                    if (node != null) {
                        val modelNode = if (node is ModelNode) node
                        else node.childNodes.filterIsInstance<ModelNode>().firstOrNull()
                            ?: (node.parent as? AnchorNode)?.childNodes?.filterIsInstance<ModelNode>()?.firstOrNull()
                        val anchorNode = (node as? AnchorNode) ?: (node.parent as? AnchorNode)
                        if (anchorNode != null && modelNode != null) {
                            onNodeSelected(anchorNode, modelNode)
                        }
                    }
                },
                onMove = { _, event, node ->
                    // 1-finger floor drag: smoothly move the anchor along the floor
                    if (event.pointerCount == 1) {
                        val anchorNode = (node as? AnchorNode)
                            ?: (node?.parent as? AnchorNode)
                            ?: selectedAnchorNode

                        if (anchorNode != null) {
                            // Find horizontal floor planes first; fallback to instant placement
                            val floorHit = frame?.hitTest(event)?.firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is Plane && trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                            } ?: frame?.hitTestInstantPlacement(event.x, event.y, 1.0f)?.firstOrNull()

                            floorHit?.let { hit ->
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastMoveTime >= 25) { // Responsive ~40fps drag
                                    try {
                                        val newPose = hit.hitPose
                                        val prevPose = anchorNode.anchor?.pose
                                        val dx = newPose.tx() - (prevPose?.tx() ?: 0f)
                                        val dz = newPose.tz() - (prevPose?.tz() ?: 0f)
                                        val distanceSq = dx * dx + dz * dz

                                        if (distanceSq > 0.0001f) { // > 1cm movement
                                            anchorNode.anchor?.detach()
                                            anchorNode.anchor = hit.createAnchor()
                                            lastMoveTime = currentTime
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                },
                onScale = { detector, _, node ->
                    val target = when {
                        node is ModelNode -> node
                        node != null -> node.childNodes.filterIsInstance<ModelNode>().firstOrNull()
                            ?: (node.parent as? AnchorNode)?.childNodes?.filterIsInstance<ModelNode>()?.firstOrNull()
                        else -> selectedModelNode
                    }
                    if (target != null) {
                        val factor = detector.scaleFactor
                        if (factor > 0.01f && factor < 5.0f && factor != 1.0f) {
                            target.applyCalibratedScale(factor)
                            onScaleUpdated?.invoke(target)
                        }
                    }
                },
                onRotate = { detector, _, node ->
                    val target = when {
                        node is ModelNode -> node
                        node != null -> node.childNodes.filterIsInstance<ModelNode>().firstOrNull()
                            ?: (node.parent as? AnchorNode)?.childNodes?.filterIsInstance<ModelNode>()?.firstOrNull()
                        else -> selectedModelNode
                    }
                    if (target != null) {
                        val deltaAngle = -(detector.currentAngle - detector.lastAngle)
                        if (kotlin.math.abs(deltaAngle) in 0.05f..35f) {
                            target.rotation = Rotation(y = target.rotation.y + deltaAngle)
                        }
                    }
                }
            ),
            onTrackingFailureChanged = {
                trackingFailureReason = it
            },
            onSessionUpdated = { _, updatedFrame ->
                frame = updatedFrame
            },
            onSessionCreated = {
                planeRenderer = false
            }
        )
    }

    // Place Model at center of viewport
    LaunchedEffect(modelToPlace) {
        if (!modelToPlace.isNullOrEmpty()) {
            frame?.let { currentFrame ->
                val (x, y) = view.viewport.run {
                    (width / 2F) to (height * 0.42F)
                }

                val hitResult = currentFrame.getHitResult(x, y)
                hitResult?.let { hit ->
                    val anchor = hit.createAnchor()
                    val anchorNode = anchor.createAnchorNode(
                        engine,
                        modelLoader,
                        modelToPlace
                    )

                    childNodes.add(anchorNode)
                    val modelNode = anchorNode.childNodes.filterIsInstance<ModelNode>().firstOrNull()
                    if (modelNode != null) {
                        onModelPlaced(anchorNode, modelNode, modelToPlace)
                    }
                }
            }
            onResetModelToPlace()
        }
    }
}

private fun Session.disableDepthConfig(): CameraConfig {
    val filter = CameraConfigFilter(this)
    filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE))
    val cameraConfigList = getSupportedCameraConfigs(filter)
    return cameraConfigList.first()
}

private fun Config.configure() {
    depthMode = Config.DepthMode.DISABLED
    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
    instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
    planeFindingMode = PlaneFindingMode.HORIZONTAL
    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
}

private fun Anchor.createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    model: String
): AnchorNode {
    val anchorNode = AnchorNode(engine, this)
    val modelNode = ModelNode(
        modelInstance = modelLoader.createModelInstance("models/${model}.glb")
    ).apply {
        val baseScale = 0.3F / size.x
        scale = Scale(baseScale)
        rotation = Rotation()
        initialBaseScale = baseScale
        enableGestures()

        onScale = { _, _, _ ->
            false // Suppress SceneView raw un-damped scaling; handled smoothly by onScale listener in ARScene!
        }
    }

    anchorNode.addChildNode(modelNode)
    return anchorNode
}

private fun Frame.getHitResult(x: Float, y: Float): HitResult? =
    hitTest(x, y).firstOrNull { hit ->
        val trackable = hit.trackable
        trackable is Plane && trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING
    } ?: hitTestInstantPlacement(x, y, 1.5f).firstOrNull()
