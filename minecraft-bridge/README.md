# RNK Minecraft Bridge

The RNK Minecraft Bridge is a Universal Mod Weaver Bridge that connects Minecraft servers to The Tync for universal mod delivery and injection.

**Features:**
- **One-way Architecture**: Bridge calls Tync, Tync responds (no reverse connections)
- **Runtime Injection**: Inject mods into running servers without restart
- **Auto-Detection**: Automatically finds Minecraft server installations
- **Universal Compatibility**: Works with any Minecraft server type
- **Lightweight**: <5s startup, <256MB memory usage
- **Super Easy**: One-click operation, drag-and-drop support

## Security Notice

**Known Security Issue**: The build process uses `pkg` which has a moderate severity vulnerability (Local Privilege Escalation). This only affects the executable building process, not the runtime application.

- **Risk**: Only affects systems where untrusted users can execute the build process
- **Mitigation**: Only run builds on trusted systems, or use the source code version
- **Status**: No upstream fix available from pkg maintainers

## System Requirements

- **Node.js**: 16.0.0 or higher
- **Java**: JDK 17+ (for server launching and Tync integration)
- **The Tync**: Optional but recommended for full functionality
- **Minecraft Server**: Any vanilla, Paper, Spigot, etc. server

## Installation

### Option 1: Download Pre-built Executable (Recommended)
1. Download `rnk-bridge.exe` (Windows) or `rnk-bridge` (Linux/Mac) from the releases page
2. Run the executable - no installation required!
3. The application will automatically detect your Minecraft servers and Tync installation

### Option 2: Build from Source
```bash
# Clone the repository
git clone <repository-url>
cd minecraft-bridge

# Install dependencies
npm install

# Build standalone executables for all platforms
npm run build

# The executables will be created in the 'dist' folder
```

## Usage

### Running the Application
```bash
# Using pre-built executable
./rnk-bridge.exe  # Windows
./rnk-bridge      # Linux/Mac

# Or run from source
npm start
```

### First Time Setup
1. **Launch**: Run the RNK Minecraft Bridge
2. **Auto-Detection**: The bridge automatically finds Minecraft server installations
3. **Tync Integration**: Connects to your Tync installation for universal mod delivery
4. **Server Selection**: Choose from detected servers or enter custom paths

### Command Line Options
```bash
node index.js --help
node index.js --server-path /path/to/server --auto-launch
```

## Architecture

### Core Components
1. **TyncConnectionLayer**: One-way HTTP/direct calls to Tync engines
2. **ServerLauncherLayer**: Auto-detects and launches Minecraft servers
3. **UniversalModInjectionLayer**: Runtime bytecode injection into running JVMs
4. **UserInterfaceLayer**: Simple CLI with optional GUI

### Communication Flow
```
User → Bridge → Tync Engine Call → Tync Processes → Tync Responds → Bridge → Server Launch → Mod Injection → User
```

### Security
- **No Persistent Connections**: Each call is independent
- **Session Tokens**: For call authentication
- **Timeout Protection**: Calls complete within time limits
- **One-Way Only**: Bridge initiates all communication

## Supported Servers
- **Vanilla Minecraft**
- **Paper/Bukkit**
- **Spigot**
- **Forge**
- **Fabric**
- **And more...**

## Troubleshooting

### Tync Connection Issues
- Ensure The Tync is compiled and in the parent directory
- Check Java classpath in TyncConnectionLayer.js
- Verify SimpleEngineRunner class exists

### Injection Issues
- Requires Java Attach API (JDK 9+)
- The injection agent (`injector.jar`) ships with release builds; from source, a
  placeholder is generated automatically at runtime
- Verify server is running before injection

### Common Errors
- **"Tync connection failed"**: Check Tync installation
- **"No servers detected"**: Use manual path entry
- **"Injection failed"**: Check Java version and permissions

## Project Structure
```
minecraft-bridge/
├── index.js                 # Main entry point
├── TyncConnectionLayer.js   # Tync communication
├── ServerLauncherLayer.js   # Server detection/launching
├── UniversalModInjectionLayer.js  # Runtime injection
├── UserInterfaceLayer.js    # CLI interface
├── META-INF/                # Injection agent manifest
├── package.json            # Node.js configuration
└── README.md               # This file
```

## Testing
```bash
npm test
```

## Support RNK Studios

Support the development of RNK Studios tools and get access to premium features, early releases, and priority support:

- GitHub: https://github.com/RNK-Enterprise
- Issues: https://github.com/RNK-Enterprise/rnk-public/issues

## License
Licensed under the GNU General Public License v3.0 — see the repository root [LICENSE](../LICENSE).

## Version
1.0.0

## Contributors

- **RNK-Enterprise** — [github.com/RNK-Enterprise](https://github.com/RNK-Enterprise)
- **Lisa's Dungeon** — [github.com/lisasdungeon](https://github.com/lisasdungeon) · Lisasdungeon@gmail.com
