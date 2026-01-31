package viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import domain.clearFloorRequests
import domain.getNextFloor
import kotlinx.coroutines.delay
import model.BuildingState
import model.Direction
import model.DoorState
import model.ElevatorState

// Elevator controller - manages all behavior for a single elevator
@Composable
fun ElevatorController(
    elevatorState: ElevatorState,
    buildingState: BuildingState
) {
    // Elevator controller logic
    LaunchedEffect(
        elevatorState.queuedFloors,
        elevatorState.assignedCallsUp,
        elevatorState.assignedCallsDown,
        elevatorState.isMoving,
        elevatorState.doorState
    ) {
        // Don't process while doors are closing
        if (elevatorState.doorState == DoorState.CLOSING) {
            return@LaunchedEffect
        }

        if (elevatorState.isMoving) {
            return@LaunchedEffect
        }

        // Check if any requests are pending
        val hasRequests = elevatorState.queuedFloors.isNotEmpty() ||
                elevatorState.assignedCallsUp.isNotEmpty() ||
                elevatorState.assignedCallsDown.isNotEmpty()

        // Handle button press while idle with doors open
        if (elevatorState.doorState == DoorState.OPEN && hasRequests) {
            elevatorState.doorState = DoorState.CLOSING
            return@LaunchedEffect
        }

        if (elevatorState.doorState == DoorState.CLOSED) {
            // Check if there's a call or queued request at the current floor
            val currentFloor = elevatorState.currentFloor
            val callAtCurrentFloor = currentFloor in elevatorState.assignedCallsUp ||
                    currentFloor in elevatorState.assignedCallsDown ||
                    currentFloor in elevatorState.queuedFloors

            if (callAtCurrentFloor) {
                // Clear the assigned calls/queue and open doors
                clearFloorRequests(currentFloor, elevatorState, buildingState)
                elevatorState.doorState = DoorState.OPENING
                return@LaunchedEffect
            }

            // Determine next floor using elevator algorithm
            val nextFloor = getNextFloor(elevatorState)
            if (nextFloor != null && nextFloor != currentFloor) {
                elevatorState.targetFloor = nextFloor
                elevatorState.direction =
                    if (nextFloor > currentFloor) Direction.UP
                    else Direction.DOWN
                elevatorState.isMoving = true
            } else if (!hasRequests) {
                // No requests - stay idle at the current floor
                elevatorState.direction = Direction.NONE
            }
        }
    }

    // Idle homing logic - return to home floor after 4 seconds of no requests
    LaunchedEffect(Unit) {
        var idleTime = 0L
        val checkInterval = 100L
        val homingDelay = 4000L

        while (true) {
            delay(checkInterval)

            val hasRequests = elevatorState.queuedFloors.isNotEmpty() ||
                    elevatorState.assignedCallsUp.isNotEmpty() ||
                    elevatorState.assignedCallsDown.isNotEmpty()

            val isIdle = !hasRequests &&
                    !elevatorState.isMoving &&
                    elevatorState.doorState == DoorState.CLOSED &&
                    elevatorState.currentFloor != elevatorState.homeFloor

            if (isIdle) {
                idleTime += checkInterval
                if (idleTime >= homingDelay) {
                    // Start homing to home floor
                    elevatorState.queuedFloors += elevatorState.homeFloor
                    elevatorState.targetFloor = elevatorState.homeFloor
                    elevatorState.direction =
                        if (elevatorState.homeFloor > elevatorState.currentFloor)
                            Direction.UP else Direction.DOWN
                    elevatorState.isMoving = true
                    idleTime = 0L
                }
            } else {
                idleTime = 0L
            }
        }
    }

    // Door animation and dwell logic
    LaunchedEffect(elevatorState.doorState) {
        when (elevatorState.doorState) {
            DoorState.OPENING -> {
                val startProgress = elevatorState.doorProgress
                val duration = 500
                val frames = duration / 16
                for (i in 1..frames) {
                    val progress = i.toFloat() / frames
                    val eased = 1f - (1f - progress) * (1f - progress)
                    elevatorState.doorProgress =
                        startProgress + (1f - startProgress) * eased
                    delay(16)
                }
                elevatorState.doorProgress = 1f

                // Dwell time
                delay(2000)

                // Now decide: close doors or stay open
                val hasRequests = elevatorState.queuedFloors.isNotEmpty() ||
                        elevatorState.assignedCallsUp.isNotEmpty() ||
                        elevatorState.assignedCallsDown.isNotEmpty()
                if (hasRequests || elevatorState.currentFloor != elevatorState.homeFloor) {
                    elevatorState.doorState = DoorState.CLOSING
                } else {
                    elevatorState.direction = Direction.NONE
                    elevatorState.doorState = DoorState.OPEN
                }
            }
            DoorState.CLOSING -> {
                val startProgress = elevatorState.doorProgress
                val duration = 500
                val frames = duration / 16
                for (i in 1..frames) {
                    val progress = i.toFloat() / frames
                    val eased = progress * progress
                    elevatorState.doorProgress =
                        startProgress * (1f - eased)
                    delay(16)
                }
                elevatorState.doorProgress = 0f
                elevatorState.doorState = DoorState.CLOSED
            }
            else -> {}
        }
    }

    // Movement animation
    LaunchedEffect(
        elevatorState.isMoving,
        elevatorState.targetFloor
    ) {
        if (!elevatorState.isMoving) return@LaunchedEffect

        var goingUp = elevatorState.direction == Direction.UP
        val frameTime = 16L
        val movementPerFrame = 0.5f / (1000f / frameTime)

        fun shouldStopAt(floor: Int, direction: Boolean): Boolean {
            val internalStop = floor in elevatorState.queuedFloors
            val matchingCallStop = if (direction) {
                floor in elevatorState.assignedCallsUp
            } else {
                floor in elevatorState.assignedCallsDown
            }
            return internalStop || matchingCallStop
        }

        fun hasRequestsInDirection(floor: Int, direction: Boolean): Boolean {
            val allRequests = elevatorState.queuedFloors +
                    elevatorState.assignedCallsUp +
                    elevatorState.assignedCallsDown
            return if (direction) {
                allRequests.any { it > floor }
            } else {
                allRequests.any { it < floor }
            }
        }

        while (true) {
            val movement = movementPerFrame * (if (goingUp) 1f else -1f)
            val newPosition = (elevatorState.absolutePosition + movement).coerceIn(1f, 6f)
            elevatorState.absolutePosition = newPosition

            val nearestFloor = kotlin.math.round(newPosition).toInt()
            val atFloor = kotlin.math.abs(newPosition - nearestFloor) < 0.01f

            if (atFloor) {
                elevatorState.currentFloor = nearestFloor

                // Boundary check
                if (nearestFloor <= 1 && !goingUp) {
                    elevatorState.absolutePosition = 1f
                    goingUp = true
                    elevatorState.direction = Direction.UP
                } else if (nearestFloor >= 6 && goingUp) {
                    elevatorState.absolutePosition = 6f
                    goingUp = false
                    elevatorState.direction = Direction.DOWN
                }

                // Check if we should stop here
                if (shouldStopAt(nearestFloor, goingUp)) {
                    elevatorState.absolutePosition = nearestFloor.toFloat()
                    elevatorState.queuedFloors -= nearestFloor
                    if (goingUp) {
                        if (nearestFloor in elevatorState.assignedCallsUp) {
                            elevatorState.assignedCallsUp -= nearestFloor
                            buildingState.callButtonsUp -= nearestFloor
                        }
                    } else {
                        if (nearestFloor in elevatorState.assignedCallsDown) {
                            elevatorState.assignedCallsDown -= nearestFloor
                            buildingState.callButtonsDown -= nearestFloor
                        }
                    }

                    if (elevatorState.queuedFloors.isEmpty() &&
                        elevatorState.assignedCallsUp.isEmpty() &&
                        elevatorState.assignedCallsDown.isEmpty()) {
                        elevatorState.direction = Direction.NONE
                    }
                    elevatorState.doorState = DoorState.OPENING
                    elevatorState.isMoving = false
                    return@LaunchedEffect
                }

                // Check if there are more requests in our current direction
                if (hasRequestsInDirection(nearestFloor, goingUp)) {
                    // Keep going
                } else {
                    // Check for reverse call here
                    val hasReverseCallHere = if (goingUp) {
                        nearestFloor in elevatorState.assignedCallsDown
                    } else {
                        nearestFloor in elevatorState.assignedCallsUp
                    }

                    if (hasReverseCallHere) {
                        elevatorState.absolutePosition = nearestFloor.toFloat()
                        goingUp = !goingUp
                        elevatorState.direction = if (goingUp) Direction.UP else Direction.DOWN
                        clearFloorRequests(nearestFloor, elevatorState, buildingState)

                        if (elevatorState.queuedFloors.isEmpty() &&
                            elevatorState.assignedCallsUp.isEmpty() &&
                            elevatorState.assignedCallsDown.isEmpty()) {
                            elevatorState.direction = Direction.NONE
                        }
                        elevatorState.doorState = DoorState.OPENING
                        elevatorState.isMoving = false
                        return@LaunchedEffect
                    }

                    // Check for requests in the reverse direction
                    val requestsInReverse = if (goingUp) {
                        (elevatorState.queuedFloors + elevatorState.assignedCallsDown)
                            .any { it < nearestFloor }
                    } else {
                        (elevatorState.queuedFloors + elevatorState.assignedCallsUp)
                            .any { it > nearestFloor }
                    }

                    if (requestsInReverse) {
                        goingUp = !goingUp
                        elevatorState.direction =
                            if (goingUp) Direction.UP else Direction.DOWN
                    } else {
                        // No requests anywhere - stop
                        elevatorState.absolutePosition = nearestFloor.toFloat()
                        elevatorState.direction = Direction.NONE
                        elevatorState.isMoving = false
                        if (nearestFloor == elevatorState.homeFloor) {
                            elevatorState.doorState = DoorState.OPENING
                        }
                        return@LaunchedEffect
                    }
                }
            }

            delay(16)
        }
    }
}
