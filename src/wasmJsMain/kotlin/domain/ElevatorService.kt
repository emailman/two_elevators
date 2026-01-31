package domain

import model.BuildingState
import model.DoorState
import model.ElevatorState

// Clear all requests for a floor (assigned calls and queued floors)
fun clearFloorRequests(
    floor: Int,
    elevatorState: ElevatorState,
    buildingState: BuildingState
) {
    if (floor in elevatorState.assignedCallsUp) {
        elevatorState.assignedCallsUp -= floor
        buildingState.callButtonsUp -= floor
    }
    if (floor in elevatorState.assignedCallsDown) {
        elevatorState.assignedCallsDown -= floor
        buildingState.callButtonsDown -= floor
    }
    elevatorState.queuedFloors -= floor
}

// Handle cab button press - toggles floor in queue
fun handleCabButtonPress(elevator: ElevatorState, floor: Int) {
    if (elevator.currentFloor == floor &&
        !elevator.isMoving &&
        elevator.doorState == DoorState.OPEN) {
        return
    }
    elevator.queuedFloors =
        if (floor in elevator.queuedFloors) {
            elevator.queuedFloors - floor
        } else {
            elevator.queuedFloors + floor
        }
}
