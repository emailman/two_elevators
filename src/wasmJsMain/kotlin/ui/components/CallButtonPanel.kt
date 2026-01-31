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
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val totalFloors = 6
        val shaftHeight = maxHeight * 0.85f
        val floorHeight = shaftHeight / totalFloors
        val verticalOffset = (maxHeight - shaftHeight) / 2 + 32.dp // Account for label

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "CALL",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                for (floor in 1..totalFloors) {
                    val floorCenterY = verticalOffset + shaftHeight -
                            (floorHeight * floor) + (floorHeight / 2)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = floorCenterY - 18.dp - 32.dp),
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
                            Spacer(modifier = Modifier.size(36.dp))
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
                            Spacer(modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }
    }
}
