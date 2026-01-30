# Two Elevators

### by Claude and Eric - Version 4.0

A visual two-elevator simulation built with Kotlin/WASM and Compose Multiplatform. The application renders two interactive elevator shafts with animated car movement, doors, intelligent dispatching, and control panels.

## Features

### Dual Elevator System
- **Two Independent Elevators**: Elevators A and B operate independently with their own state and controls
- **Intelligent Dispatching**: Hall call buttons automatically assign requests to the best elevator based on:
  - Distance to the call floor
  - Current travel direction
  - Existing queue load
  - Elevator availability

### Visual Elements
- **6-Floor Shafts**: Each elevator has a labeled shaft with floor indicators (1-6)
- **Animated Elevator Cars**: Amber/gold cars with gray sliding doors
- **Direction Indicators**: Arrows display on each car showing travel direction
- **Lit Buttons**: Active floor selections and call buttons glow amber

### Control Panels
- **CAB A / CAB B Panels**: Separate internal button panels for each elevator
  - Floor buttons can be toggled on/off
  - Buttons light up when selected
- **Shared Hall Call Buttons**: Centrally located up/down buttons for each floor
  - Floor 1: Up button only
  - Floors 2-5: Both up and down buttons
  - Floor 6: Down button only
  - Cannot be canceled once pressed

### Elevator Behavior
- **SCAN Algorithm**: Each elevator uses the classic elevator algorithm (continues in current direction servicing requests, then reverses)
- **Directional Awareness**: Elevators only stop for hall calls when traveling in the matching direction
- **Auto-Home**: Both elevators return to floor 1 with doors open after 4 seconds of idle time
- **Smooth Animations**:
  - Movement: 0.5 floors per second
  - Door open/close: 500ms with easing
  - Dwell time: 2 seconds at each stop

## Requirements

- JDK 17 or higher
- Gradle 8.x (included via wrapper)

## Build and Run

1. Clone the repository and navigate to the project directory

2. Build the project:
   ```
   ./gradlew build
   ```

3. Start the development server:
   ```
   ./gradlew wasmJsBrowserDevelopmentRun
   ```

4. Open your browser to http://localhost:8080 (port may vary)

## Project Structure

```
TwoElevators/
├── src/wasmJsMain/
│   ├── kotlin/Main.kt          # All application code (~750 lines)
│   └── resources/index.html    # HTML entry point
├── build.gradle.kts            # Gradle build configuration
├── settings.gradle.kts         # Project settings
└── vercel.json                 # Vercel deployment config
```

### Key Components in Main.kt

| Component | Description |
|-----------|-------------|
| `BuildingState` | Holds building-wide hall call buttons and both elevator instances |
| `ElevatorState` | Individual elevator state (position, doors, queued floors, assigned calls) |
| `ElevatorController` | Composable managing elevator logic via LaunchedEffects |
| `ElevatorShaft` | Canvas-based rendering of elevator shaft and car |
| `CentralCallButtonPanel` | Shared hall call buttons between elevators |
| `ElevatorButtonPanel` | Internal cab button panel |
| `calculateETA` / `dispatchCall` | Dispatch algorithm for assigning calls to elevators |

## Deploy to Vercel

This project can be deployed to Vercel as a static site.

### One-Click Deploy

1. Fork this repository to your GitHub account
2. Go to [vercel.com](https://vercel.com) and sign in with GitHub
3. Click "Add New Project" and import your forked repository
4. Vercel will auto-detect the configuration - click "Deploy"

### Updating the Deployment

1. Build the production bundle:
   ```
   ./gradlew wasmJsBrowserProductionWebpack
   ```

2. Commit the updated build output in `build/kotlin-webpack/wasmJs/productionExecutable`

3. Push to your repository - Vercel will automatically redeploy

## Technology Stack

- **Kotlin/WASM** - WebAssembly compilation target
- **Compose Multiplatform 1.9.3** - Declarative UI framework
- **Material 3** - Dark theme design system
- **Gradle** - Build automation with Kotlin DSL
