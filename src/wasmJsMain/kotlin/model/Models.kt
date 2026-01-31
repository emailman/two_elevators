package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Elevator direction
enum class Direction { UP, DOWN, NONE }

// Door state
enum class DoorState { CLOSED, OPENING, OPEN, CLOSING }

// Elevator state holder
class ElevatorState(val homeFloor: Int = 1) {
    var currentFloor by mutableStateOf(homeFloor)
    var targetFloor by mutableStateOf(homeFloor)
    var direction by mutableStateOf(Direction.NONE)

    // Start with doors open at home floor
    var doorState by mutableStateOf(DoorState.OPEN)

    // 0 = closed, 1 = open
    var doorProgress by mutableStateOf(1f)

    // Absolute position (1.0 = floor 1, 2.5 = halfway between floor 2 and 3)
    var absolutePosition by mutableStateOf(homeFloor.toFloat())
    var isMoving by mutableStateOf(false)
    var queuedFloors by mutableStateOf(setOf<Int>())

    // Assigned call buttons (assigned by dispatcher)
    var assignedCallsUp by mutableStateOf(setOf<Int>())
    var assignedCallsDown by mutableStateOf(setOf<Int>())
}

// Building-wide state - holds hall call buttons and both elevators
class BuildingState {
    // Hall call buttons (building-wide)
    var callButtonsUp by mutableStateOf(setOf<Int>())    // Floors 1-5
    var callButtonsDown by mutableStateOf(setOf<Int>())  // Floors 2-6

    // Two elevators - both start and home at floor 1
    val elevatorA = ElevatorState(homeFloor = 1)
    val elevatorB = ElevatorState(homeFloor = 1)

    fun getAllElevators() = listOf(elevatorA, elevatorB)
}
