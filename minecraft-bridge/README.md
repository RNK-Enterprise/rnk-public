# RNK Minecraft Bridge

The RNK Minecraft Bridge is a Universal Mod Weaver Bridge that connects Minecraft servers to The Tync for universal mod delivery and injection.

**Features:**
- **One-way Architecture**: Bridge calls Tync, Tync responds (no reverse connections)
- **Mod delivery pipeline**: adapted artifacts are delivered into detected servers (agent-based live injection ships with the hosted tier)
- **Auto-Detection**: Automatically finds Minecraft server installations
- **Broad server support**: detects Paper, Spigot, Purpur, and other servers by JAR signature
- **Lightweight**: <5s startup, <256MB memory usage
- **Super Easy**: One-click operation, drag-and-drop support

## Security Notice

**Known Security Issue**: The build process uses `pkg` which has a moderate severity vulnerability (Local Privilege Escalation). This only affects the executable building process, not the runtime application.

- **Risk**: Only affects systems where untrusted users can execute the build process
- **Mitigation**: Only run builds on trusted systems, or use the source code version
- **Status**: No upstream fix available from pkg maintainers

## System Requirements

- **Node.js**: 16.0.0 or higher
- **Java**: JDK 21+ (required to build/run The Tync; server launching uses your installed JDK)
- **The Tync**: Optional but recommended for full functionality
- **Minecraft Server**: Any vanilla, Paper, Spigot, etc. server

## Installation

### Run from Source
```bash
# Clone the repository
git clone https://github.com/RNK-Enterprise/rnk-public.git
cd rnk-public/minecraft-bridge

# Install dependencies and start
npm install
npm start
```

Prebuilt executables are not yet published; if you want standalone binaries,
`npm run build` creates them in `dist/`.

## Usage

### Running the Application
```bash
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
- The injection agent (`injector.jar`) is a placeholder generated automatically at
  runtime; agent-based live injection ships with the hosted tier
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
