# Two Elevators

## Project Overview
A dual elevator simulation built with Kotlin/Compose for WebAssembly (WASM). The application simulates two elevators serving a 6-floor building with realistic dispatching, movement, and door animations.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose Multiplatform
- **Target Platform**: WebAssembly (wasmJs)
- **Build System**: Gradle with Kotlin Multiplatform

## Project Structure
```
src/wasmJsMain/kotlin/
└── Main.kt          # All application code (single file)
```

## Key Components

### Models
- `Direction` - Elevator direction enum (UP, DOWN, NONE)
- `DoorState` - Door state machine (CLOSED, OPENING, OPEN, CLOSING)
- `ElevatorState` - Individual elevator state (position, floor, doors, queues)
- `BuildingState` - Building-wide state (hall calls, both elevators)

### Business Logic
- `calculateETA()` - Cost calculation for dispatching
- `dispatchCall()` - Assigns hall calls to best elevator
- `getNextFloor()` - SCAN algorithm for floor selection
- `clearFloorRequests()` - Clears requests when servicing a floor
- `handleCabButtonPress()` - Toggles cab button requests

### UI Components
- `App()` - Root composable with layout
- `ElevatorController()` - State management and animations
- `ElevatorShaft()` - Canvas-based elevator visualization
- `CentralCallButtonPanel()` - Hall call buttons (shared)
- `ElevatorButtonPanel()` - Cab buttons per elevator
- `FloorButton()` / `CallButton()` - Individual button components

## Build Commands
```bash
# Development run (hot reload)
./gradlew wasmJsBrowserDevelopmentRun

# Production build
./gradlew wasmJsBrowserProductionWebpack
```

## Elevator Behavior
- Both elevators home to floor 1
- SCAN algorithm for efficient floor servicing
- 2-second door dwell time
- 4-second idle timeout before returning home
- Smart dispatching based on ETA calculation
