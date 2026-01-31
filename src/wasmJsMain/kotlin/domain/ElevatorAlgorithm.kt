package domain

import model.Direction
import model.ElevatorState

// SCAN/Elevator algorithm - uses assigned calls instead of building-wide calls
fun getNextFloor(state: ElevatorState): Int? {
    val allEmpty = state.queuedFloors.isEmpty() &&
            state.assignedCallsUp.isEmpty() &&
            state.assignedCallsDown.isEmpty()
    if (allEmpty) return null

    val current = state.currentFloor

    // Floors to service when going UP: internal requests + assigned up calls
    val upFloors = state.queuedFloors + state.assignedCallsUp
    // Floors to service when going DOWN: internal requests + assigned down calls
    val downFloors = state.queuedFloors + state.assignedCallsDown

    return when (state.direction) {
        Direction.UP -> {
            val floorsAbove = upFloors.filter { it > current }.minOrNull()
            floorsAbove ?: downFloors.filter { it < current }.maxOrNull()
            ?: upFloors.filter { it < current }.maxOrNull()
        }
        Direction.DOWN -> {
            val floorsBelow = downFloors.filter { it < current }.maxOrNull()
            floorsBelow ?: upFloors.filter { it > current }.minOrNull()
            ?: downFloors.filter { it > current }.minOrNull()
        }
        Direction.NONE -> {
            val serviceableGoingUp =
                (state.queuedFloors + state.assignedCallsUp).filter { it > current }
            val serviceableGoingDown =
                (state.queuedFloors + state.assignedCallsDown).filter { it < current }

            val atCurrent = current in state.queuedFloors ||
                    current in state.assignedCallsUp ||
                    current in state.assignedCallsDown

            when {
                atCurrent -> current
                serviceableGoingUp.isNotEmpty() && serviceableGoingDown.isNotEmpty() -> {
                    val nearestUp = serviceableGoingUp.minOrNull()!!
                    val nearestDown = serviceableGoingDown.maxOrNull()!!
                    if (nearestUp - current <= current - nearestDown) nearestUp else nearestDown
                }
                serviceableGoingUp.isNotEmpty() -> serviceableGoingUp.minOrNull()
                serviceableGoingDown.isNotEmpty() -> serviceableGoingDown.maxOrNull()
                else -> {
                    val allCalls = state.assignedCallsUp + state.assignedCallsDown
                    allCalls.minByOrNull { kotlin.math.abs(it - current) }
                }
            }
        }
    }
}
