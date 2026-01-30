import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.delay


// Elevator direction
enum class Direction { UP, DOWN, NONE }

// Door state
enum class DoorState { CLOSED, OPENING, OPEN, CLOSING }

// Elevator state holder - now takes an id and home floor
class ElevatorState(val id: Int, val homeFloor: Int = 1) {
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
    val elevatorA = ElevatorState(id = 1, homeFloor = 1)
    val elevatorB = ElevatorState(id = 2, homeFloor = 1)

    fun getAllElevators() = listOf(elevatorA, elevatorB)
}

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
        val movingToward = when {
            elevator.direction == Direction.UP && targetFloor > currentFloor -> true
            elevator.direction == Direction.DOWN && targetFloor < currentFloor -> true
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

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App()
    }
}

@Composable
fun App() {
    val buildingState = remember { BuildingState() }

    // Run elevator controllers for both elevators
    ElevatorController(buildingState.elevatorA, buildingState)
    ElevatorController(buildingState.elevatorB, buildingState)

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Elevator Simulator",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "by Claude and Eric - Version 4.0",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // CAB A buttons (left side)
                ElevatorButtonPanel(
                    label = "CAB A",
                    litButtons = buildingState.elevatorA.queuedFloors,
                    onButtonPress = { floor ->
                        val elevator = buildingState.elevatorA
                        if (elevator.currentFloor == floor &&
                            !elevator.isMoving &&
                            elevator.doorState == DoorState.OPEN) {
                            return@ElevatorButtonPanel
                        }
                        elevator.queuedFloors =
                            if (floor in elevator.queuedFloors) {
                                elevator.queuedFloors - floor
                            } else {
                                elevator.queuedFloors + floor
                            }
                    },
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                )

                // Elevator A shaft
                ElevatorShaft(
                    elevatorState = buildingState.elevatorA,
                    label = "A",
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp)
                )

                // Central call buttons (shared, centered between elevators)
                CentralCallButtonPanel(
                    buildingState = buildingState,
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .padding(vertical = 16.dp)
                )

                // Elevator B shaft
                ElevatorShaft(
                    elevatorState = buildingState.elevatorB,
                    label = "B",
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp)
                )

                // CAB B buttons (right side)
                ElevatorButtonPanel(
                    label = "CAB B",
                    litButtons = buildingState.elevatorB.queuedFloors,
                    onButtonPress = { floor ->
                        val elevator = buildingState.elevatorB
                        if (elevator.currentFloor == floor &&
                            !elevator.isMoving &&
                            elevator.doorState == DoorState.OPEN) {
                            return@ElevatorButtonPanel
                        }
                        elevator.queuedFloors =
                            if (floor in elevator.queuedFloors) {
                                elevator.queuedFloors - floor
                            } else {
                                elevator.queuedFloors + floor
                            }
                    },
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

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
                if (currentFloor in elevatorState.assignedCallsUp) {
                    elevatorState.assignedCallsUp -= currentFloor
                    buildingState.callButtonsUp -= currentFloor
                }
                if (currentFloor in elevatorState.assignedCallsDown) {
                    elevatorState.assignedCallsDown -= currentFloor
                    buildingState.callButtonsDown -= currentFloor
                }
                elevatorState.queuedFloors -= currentFloor
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
                        if (nearestFloor in elevatorState.assignedCallsUp) {
                            elevatorState.assignedCallsUp -= nearestFloor
                            buildingState.callButtonsUp -= nearestFloor
                        }
                        if (nearestFloor in elevatorState.assignedCallsDown) {
                            elevatorState.assignedCallsDown -= nearestFloor
                            buildingState.callButtonsDown -= nearestFloor
                        }
                        elevatorState.queuedFloors -= nearestFloor

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

// SCAN/Elevator algorithm - now uses assigned calls instead of building-wide calls
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

@Composable
fun ElevatorShaft(
    elevatorState: ElevatorState,
    label: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val carColor = Color(0xFFFFB300)
    val doorColor = Color(0xFF757575)
    val floorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    val labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elevator label - offset to center over shaft (accounting for floor label space)
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .aspectRatio(0.3f)
            ) {
                val totalFloors = 6
                val floorHeight = size.height / totalFloors
                val shaftLeft = 40.dp.toPx()
                val shaftWidth = size.width - shaftLeft
                val carWidth = shaftWidth * 0.8f
                val carLeftOffset = shaftLeft + (shaftWidth - carWidth) / 2

                val floorGap = 4.dp.toPx()
                for (floor in 1..totalFloors) {
                    val floorTop = size.height - (floor * floorHeight)

                    val labelText = floor.toString()
                    val textLayoutResult = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = labelColor
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = (shaftLeft - textLayoutResult.size.width) / 2,
                            y = floorTop + (floorHeight - textLayoutResult.size.height) / 2
                        )
                    )

                    drawRect(
                        color = floorColor,
                        topLeft = Offset(shaftLeft, floorTop + floorGap),
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

@Composable
fun ElevatorButtonPanel(
    label: String,
    litButtons: Set<Int>,
    onButtonPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title above the panel
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Button panel surface
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Floor buttons 6 down to 1 (top to bottom)
                for (floor in 6 downTo 1) {
                    FloorButton(
                        floor = floor,
                        isLit = floor in litButtons,
                        onClick = { onButtonPress(floor) }
                    )
                }
            }
        }
    }
}

@Composable
fun FloorButton(
    floor: Int,
    isLit: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = if (isLit) {
        Color(0xFFFFB300)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isLit) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = onClick,
        modifier = Modifier.requiredSize(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isLit) 8.dp else 2.dp
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = floor.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun CallButton(
    isUp: Boolean,
    isLit: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = if (isLit) {
        Color(0xFFFFB300)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val arrowColor = if (isLit) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isLit) 8.dp else 2.dp
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val arrowPath = Path().apply {
                if (isUp) {
                    moveTo(size.width / 2, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                } else {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
            }
            drawPath(arrowPath, color = arrowColor)
        }
    }
}
