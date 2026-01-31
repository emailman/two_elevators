package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.handleCabButtonPress
import model.BuildingState
import ui.components.CentralCallButtonPanel
import ui.components.ElevatorButtonPanel
import ui.components.ElevatorShaft
import viewmodel.ElevatorController

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
                    onButtonPress = { floor -> handleCabButtonPress(buildingState.elevatorA, floor) },
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
                    onButtonPress = { floor -> handleCabButtonPress(buildingState.elevatorB, floor) },
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                )
            }
        }
    }
}
