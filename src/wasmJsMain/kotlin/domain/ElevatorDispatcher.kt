package domain

import model.BuildingState
import model.Direction
import model.DoorState
import model.ElevatorState

// Calculate estimated cost for an elevator to service a call
// Lower cost = better choice
fun calculateETA(elevator: ElevatorState, targetFloor: Int, callDirection: Direction): Int {
    var cost = 0

    val currentFloor = elevator.currentFloor
    val distance = kotlin.math.abs(targetFloor - currentFloor)

    // Base cost: distance in floors
    cost += distance * 10

    // Penalty if elevator is moving away from the call
    if (elevator.isMoving) {
        val movingToward = when (elevator.direction) {
            Direction.UP if targetFloor > currentFloor -> true
            Direction.DOWN if targetFloor < currentFloor -> true
            else -> false
        }

        if (!movingToward) {
            // Must complete current run first - add significant penalty
            cost += 60
        }

        // Extra penalty if call direction doesn't match elevator direction
        if (movingToward && elevator.direction != callDirection) {
            cost += 30
        }
    }

    // Penalty for existing queue load
    cost += (elevator.queuedFloors.size +
            elevator.assignedCallsUp.size +
            elevator.assignedCallsDown.size) * 5

    // Bonus if elevator is idle with doors closed
    if (!elevator.isMoving && elevator.doorState == DoorState.CLOSED) {
        cost -= 5
    }

    // Immediate service if elevator is already at the floor with doors open
    if (currentFloor == targetFloor &&
        elevator.doorState == DoorState.OPEN &&
        !elevator.isMoving) {
        cost = 0
    }

    return cost
}

// Dispatch a hall call to the best elevator
fun dispatchCall(
    floor: Int,
    direction: Direction,
    buildingState: BuildingState
): ElevatorState {
    val elevators = buildingState.getAllElevators()

    return elevators.minByOrNull { elevator ->
        calculateETA(elevator, floor, direction)
    } ?: elevators.first()
}
