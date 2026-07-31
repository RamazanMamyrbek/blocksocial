package com.blocksocial.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 24f

private fun stroked(name: String, stroke: Float = 2f, block: StrokeBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    ).apply {
        StrokeBuilder(this, stroke).block()
    }.build()

private class StrokeBuilder(val builder: ImageVector.Builder, val stroke: Float) {
    fun line(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        builder.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }

    fun filled(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        builder.path(fill = SolidColor(Color.Black), pathBuilder = block)
    }
}

object BlockSocialIcons {

    val Shield: ImageVector = stroked("Shield") {
        line {
            moveTo(12f, 3f)
            lineTo(19.5f, 6f)
            verticalLineTo(11.5f)
            curveTo(19.5f, 16f, 16.4f, 19.4f, 12f, 21f)
            curveTo(7.6f, 19.4f, 4.5f, 16f, 4.5f, 11.5f)
            verticalLineTo(6f)
            close()
        }
    }

    val ShieldCheck: ImageVector = stroked("ShieldCheck") {
        line {
            moveTo(12f, 3f)
            lineTo(19.5f, 6f)
            verticalLineTo(11.5f)
            curveTo(19.5f, 16f, 16.4f, 19.4f, 12f, 21f)
            curveTo(7.6f, 19.4f, 4.5f, 16f, 4.5f, 11.5f)
            verticalLineTo(6f)
            close()
        }
        line {
            moveTo(8.8f, 11.8f)
            lineTo(11.2f, 14.2f)
            lineTo(15.4f, 9.4f)
        }
    }

    val Apps: ImageVector = stroked("Apps") {
        line {
            moveTo(4.5f, 4.5f)
            horizontalLineTo(10f)
            verticalLineTo(10f)
            horizontalLineTo(4.5f)
            close()
        }
        line {
            moveTo(14f, 4.5f)
            horizontalLineTo(19.5f)
            verticalLineTo(10f)
            horizontalLineTo(14f)
            close()
        }
        line {
            moveTo(4.5f, 14f)
            horizontalLineTo(10f)
            verticalLineTo(19.5f)
            horizontalLineTo(4.5f)
            close()
        }
        line {
            moveTo(14f, 14f)
            horizontalLineTo(19.5f)
            verticalLineTo(19.5f)
            horizontalLineTo(14f)
            close()
        }
    }

    val History: ImageVector = stroked("History") {
        line {
            moveTo(4.2f, 12f)
            curveTo(4.2f, 7.7f, 7.7f, 4.2f, 12f, 4.2f)
            curveTo(16.3f, 4.2f, 19.8f, 7.7f, 19.8f, 12f)
            curveTo(19.8f, 16.3f, 16.3f, 19.8f, 12f, 19.8f)
            curveTo(9.2f, 19.8f, 6.7f, 18.3f, 5.3f, 16f)
        }
        line {
            moveTo(12f, 7.6f)
            verticalLineTo(12f)
            lineTo(15f, 14f)
        }
    }

    val Pause: ImageVector = stroked("Pause") {
        line {
            moveTo(9.4f, 6.5f)
            verticalLineTo(17.5f)
        }
        line {
            moveTo(14.6f, 6.5f)
            verticalLineTo(17.5f)
        }
    }

    val Check: ImageVector = stroked("Check") {
        line {
            moveTo(5.5f, 12.6f)
            lineTo(9.8f, 16.9f)
            lineTo(18.5f, 7.4f)
        }
    }

    val Circle: ImageVector = stroked("Circle") {
        line {
            moveTo(12f, 4.8f)
            curveTo(15.9f, 4.8f, 19.2f, 8.1f, 19.2f, 12f)
            curveTo(19.2f, 15.9f, 15.9f, 19.2f, 12f, 19.2f)
            curveTo(8.1f, 19.2f, 4.8f, 15.9f, 4.8f, 12f)
            curveTo(4.8f, 8.1f, 8.1f, 4.8f, 12f, 4.8f)
            close()
        }
    }

    val Warning: ImageVector = stroked("Warning") {
        line {
            moveTo(12f, 4.2f)
            lineTo(20.6f, 19.2f)
            horizontalLineTo(3.4f)
            close()
        }
        line {
            moveTo(12f, 10f)
            verticalLineTo(14.2f)
        }
        filled {
            moveTo(12f, 15.9f)
            curveTo(12.6f, 15.9f, 13.1f, 16.4f, 13.1f, 17f)
            curveTo(13.1f, 17.6f, 12.6f, 18.1f, 12f, 18.1f)
            curveTo(11.4f, 18.1f, 10.9f, 17.6f, 10.9f, 17f)
            curveTo(10.9f, 16.4f, 11.4f, 15.9f, 12f, 15.9f)
            close()
        }
    }

    val Plus: ImageVector = stroked("Plus") {
        line {
            moveTo(12f, 5.5f)
            verticalLineTo(18.5f)
        }
        line {
            moveTo(5.5f, 12f)
            horizontalLineTo(18.5f)
        }
    }

    val Chevron: ImageVector = stroked("Chevron") {
        line {
            moveTo(9.5f, 5.5f)
            lineTo(16f, 12f)
            lineTo(9.5f, 18.5f)
        }
    }

    val Clock: ImageVector = stroked("Clock") {
        line {
            moveTo(12f, 4.8f)
            curveTo(15.9f, 4.8f, 19.2f, 8.1f, 19.2f, 12f)
            curveTo(19.2f, 15.9f, 15.9f, 19.2f, 12f, 19.2f)
            curveTo(8.1f, 19.2f, 4.8f, 15.9f, 4.8f, 12f)
            curveTo(4.8f, 8.1f, 8.1f, 4.8f, 12f, 4.8f)
            close()
        }
        line {
            moveTo(12f, 8f)
            verticalLineTo(12f)
            lineTo(14.8f, 13.8f)
        }
    }

    val Hourglass: ImageVector = stroked("Hourglass") {
        line {
            moveTo(7f, 4.5f)
            horizontalLineTo(17f)
        }
        line {
            moveTo(7f, 19.5f)
            horizontalLineTo(17f)
        }
        line {
            moveTo(7.8f, 4.5f)
            curveTo(7.8f, 9f, 12f, 10.6f, 12f, 12f)
            curveTo(12f, 13.4f, 7.8f, 15f, 7.8f, 19.5f)
        }
        line {
            moveTo(16.2f, 4.5f)
            curveTo(16.2f, 9f, 12f, 10.6f, 12f, 12f)
            curveTo(12f, 13.4f, 16.2f, 15f, 16.2f, 19.5f)
        }
    }
}
