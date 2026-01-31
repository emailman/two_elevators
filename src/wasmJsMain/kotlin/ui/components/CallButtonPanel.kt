package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.dispatchCall
import model.BuildingState
import model.Direction
import model.DoorState

// Central call button panel - shared hall call buttons
@Composable
fun CentralCallButtonPanel(
    buildingState: BuildingState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CALL",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "BUTTONS",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Inner BoxWithConstraints matches elevator shaft's 85% height
        BoxWithConstraints(
            modifier = Modifier.fillMaxHeight(0.85f)
        ) {
            val totalFloors = 6
            val floorHeight = maxHeight / totalFloors

            for (floor in 1..totalFloors) {
                // Calculate from bottom - same as elevator shaft
                // Subtract 28.dp to compensate for taller header (extra "BUTTONS" line)
                // Add 2.dp to account for the floorGap in elevator shaft (4.dp gap at top of each floor)
                val floorCenterY = maxHeight - (floorHeight * floor) + (floorHeight / 2) - 26.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = floorCenterY - 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Floor number centered above the arrows
                    Text(
                        text = floor.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Up button (floors 1-5 only)
                        if (floor < 6) {
                            CallButton(
                                isUp = true,
                                isLit = floor in buildingState.callButtonsUp,
                                onClick = {
                                    // Check if either elevator is at this floor with doors open
                                    val elevatorAtFloor = buildingState.getAllElevators().any { elevator ->
                                        elevator.currentFloor == floor &&
                                                !elevator.isMoving &&
                                                elevator.doorState == DoorState.OPEN
                                    }
                                    if (elevatorAtFloor) return@CallButton

                                    if (floor !in buildingState.callButtonsUp) {
                                        buildingState.callButtonsUp += floor
                                        // Dispatch to best elevator
                                        val assignedElevator = dispatchCall(floor, Direction.UP, buildingState)
                                        assignedElevator.assignedCallsUp += floor
                                    }
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(30.dp))
                        }

                        // Down button (floors 2-6 only)
                        if (floor > 1) {
                            CallButton(
                                isUp = false,
                                isLit = floor in buildingState.callButtonsDown,
                                onClick = {
                                    val elevatorAtFloor = buildingState.getAllElevators().any { elevator ->
                                        elevator.currentFloor == floor &&
                                                !elevator.isMoving &&
                                                elevator.doorState == DoorState.OPEN
                                    }
                                    if (elevatorAtFloor) return@CallButton

                                    if (floor !in buildingState.callButtonsDown) {
                                        buildingState.callButtonsDown += floor
                                        // Dispatch to best elevator
                                        val assignedElevator = dispatchCall(floor, Direction.DOWN, buildingState)
                                        assignedElevator.assignedCallsDown += floor
                                    }
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        }
    }
}
