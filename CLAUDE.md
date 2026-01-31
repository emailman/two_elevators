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
├── Main.kt                      # Entry point
├── model/                       # Data models
│   ├── Direction.kt
│   ├── DoorState.kt
│   ├── ElevatorState.kt
│   └── BuildingState.kt
├── domain/                      # Business logic
│   └── ElevatorLogic.kt
├── viewmodel/                   # State management
│   └── ElevatorController.kt
└── ui/                          # UI layer
    ├── App.kt                   # Root composable with responsive layout
    └── components/
        ├── Buttons.kt           # FloorButton, CallButton (with size params)
        ├── CallButtonPanel.kt   # Hall call buttons (with scaleFactor)
        ├── ElevatorButtonPanel.kt # Cab buttons (with scaleFactor)
        └── ElevatorShaft.kt     # Canvas-based elevator visualization
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

## Responsive Layout (In Progress)

### Current Implementation
- `BoxWithConstraints` in App.kt detects screen width
- `scaleFactor` calculated based on width breakpoints:
  - >= 600dp: 1.0 (full size)
  - 400-600dp: 0.75
  - < 400dp: 0.6
- Button sizes scale via `buttonSize` parameter in FloorButton/CallButton
- Panel padding and spacing scale in ElevatorButtonPanel
- Call button panel aligned with elevator floors via matching headers

### Future Mobile Work
- Test and refine scaling on actual mobile devices
- Consider alternative layouts for very narrow screens (stacked vs side-by-side)
- May need to hide cab button panels on mobile or use overlay/modal
- Test touch target sizes meet accessibility guidelines (48dp minimum)
