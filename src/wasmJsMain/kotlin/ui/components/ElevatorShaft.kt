package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import model.Direction
import model.ElevatorState

@Composable
fun ElevatorShaft(
    elevatorState: ElevatorState,
    label: String,
    modifier: Modifier = Modifier
) {
    val carColor = Color(0xFFFFB300)
    val doorColor = Color(0xFF757575)
    val floorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elevator label
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .aspectRatio(0.3f)
            ) {
                val totalFloors = 6
                val floorHeight = size.height / totalFloors
                val shaftWidth = size.width
                val carWidth = shaftWidth * 0.8f
                val carLeftOffset = (shaftWidth - carWidth) / 2

                val floorGap = 4.dp.toPx()
                for (floor in 1..totalFloors) {
                    val floorTop = size.height - (floor * floorHeight)

                    drawRect(
                        color = floorColor,
                        topLeft = Offset(0f, floorTop + floorGap),
                        size = Size(shaftWidth, floorHeight - floorGap)
                    )
                }

                val carClearance = floorHeight * 0.15f
                val carHeight = floorHeight - carClearance
                val carY = size.height - (elevatorState.absolutePosition * floorHeight) + carClearance

                drawRect(
                    color = carColor,
                    topLeft = Offset(carLeftOffset, carY),
                    size = Size(carWidth, carHeight)
                )

                val maxDoorWidth = carWidth * 0.35f
                val doorHeight = carHeight * 0.85f
                val doorY = carY + (carHeight - doorHeight) / 2
                val doorGap = carWidth * 0.05f
                val centerX = carLeftOffset + carWidth / 2

                val currentDoorWidth = maxDoorWidth * (1f - elevatorState.doorProgress)

                if (currentDoorWidth > 0.5f) {
                    drawRect(
                        color = doorColor,
                        topLeft = Offset(centerX - doorGap / 2 - maxDoorWidth, doorY),
                        size = Size(currentDoorWidth, doorHeight)
                    )
                    drawRect(
                        color = doorColor,
                        topLeft = Offset(centerX + doorGap / 2 + maxDoorWidth - currentDoorWidth, doorY),
                        size = Size(currentDoorWidth, doorHeight)
                    )
                }

                if (elevatorState.direction != Direction.NONE) {
                    val arrowSize = carHeight * 0.2f
                    val arrowCenterY = carY + carHeight * 0.3f

                    val path = Path().apply {
                        if (elevatorState.direction == Direction.UP) {
                            moveTo(centerX, arrowCenterY - arrowSize / 2)
                            lineTo(centerX - arrowSize / 2, arrowCenterY + arrowSize / 2)
                            lineTo(centerX + arrowSize / 2, arrowCenterY + arrowSize / 2)
                            close()
                        } else {
                            moveTo(centerX, arrowCenterY + arrowSize / 2)
                            lineTo(centerX - arrowSize / 2, arrowCenterY - arrowSize / 2)
                            lineTo(centerX + arrowSize / 2, arrowCenterY - arrowSize / 2)
                            close()
                        }
                    }
                    drawPath(path, color = Color.Black.copy(alpha = 0.7f))
                }
            }
        }
    }
}
