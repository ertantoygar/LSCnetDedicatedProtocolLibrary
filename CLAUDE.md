# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

XGBCNet Protocol Communication Library - A Java library for industrial PLC (Programmable Logic Controller) communication using the XGBCNet protocol over Serial (RS232/RS485) and TCP connections. The library provides real-time data exchange, monitoring, and control operations for industrial automation environments.

**Base Package**: `tr.com.logidex.cnetdedicated`

## Build & Development Commands

This is a Maven-based Java 17 project. Use the following commands:

```bash
# Build the project
mvn clean install

# Compile only
mvn compile

# Run tests (if available)
mvn test

# Package as JAR
mvn package
```

## Architecture Overview

### Core Design Pattern

The library implements a **Singleton pattern** for the main client (`XGBCNetClient`) with thread-safe operations using `ConcurrentHashMap` for request/response handling and synchronized methods for critical sections.

### Layer Architecture

```
XGBCNetClient (Singleton Entry Point)
├── Connection Layer (protocol/connection/)
│   ├── ConnectionFactory - Creates connection instances
│   ├── Connection interface - Abstract connection contract
│   ├── SerialConnection - RS232/RS485 implementation
│   ├── TCPConnection - Ethernet implementation
│   └── ResponseReader interface - Async response handling
│       ├── SerialReader
│       └── TCPReader
├── Protocol Layer (protocol/)
│   ├── Command enum - R(Read), W(Write), X(Register), Y(Execute)
│   ├── CommandType enum - RSS, SS, SB
│   ├── Response hierarchy - AckResponse, NakResponse, InvalidResponse
│   └── ResponseEvaluator - Parses and validates responses
├── Device Layer (device/)
│   ├── Tag - Primary data model for PLC variables
│   ├── DataBlock - Request/response data structure
│   ├── RegisteredDataBlock - Monitoring registration data
│   ├── Device enum - PLC memory areas (M, D, F, etc.)
│   ├── DataType enum - Bit, Byte, Word, DWord, Float, String
│   └── DisplayFormat enum - Value formatting options
└── UI Controls (fxcontrols/)
    ├── LSButton - Touch-enabled PLC control button
    ├── LSTextField - Touch-enabled PLC value input
    ├── ImperialMeasurement* - Imperial unit handling
    └── Touch* - Touch keyboard implementations
```

### Communication Flow

1. **Connection Establishment**: `XGBCNetClient.getInstance().connect(connectionParams)` uses `ConnectionFactory` to create either `SerialConnection` or `TCPConnection`
2. **Request Generation**: Client methods format requests according to XGBCNet protocol with ENQ/EOT framing
3. **Async Response Handling**: `ResponseReader` implementations (Serial/TCP) notify client via observer pattern
4. **Response Parsing**: `ResponseEvaluator` parses raw responses into structured `Response` objects
5. **Data Mapping**: Tag objects convert between PLC hex format and Java types based on `DisplayFormat`

### Key Architectural Decisions

**Request-Response Mapping**: Uses `ConcurrentHashMap<String, String>` where key is request ID and value is response. The observer pattern (`SerialReaderObserver`, `TCPReaderObserver`) enables async communication without blocking.

**Tag-Based Abstraction**: The `Tag` class serves as the primary data model, encapsulating:
- PLC address (device type + address)
- Data type and display format
- Value with JavaFX property binding for UI updates
- Multiplier for decimal representation
- Pause/resume update control

**Device Registration System**: Commands X (register) and Y (execute) enable batch monitoring:
- `registerDevicesToMonitor()` - Registers multiple tags with a registration number
- `executeRegisteredDeviceToMonitor()` - Polls all registered tags in a single transaction
- Stored in `Map<Integer, List<Tag>>` for efficient batch operations

**Frame Validation**: Protocol includes BCC (Block Check Character) for error detection. Set `USE_FRAME_CHECK = true` in `XGBCNetClient`.

## Protocol Details

### XGBCNet Commands

| Command | Name | Purpose | Usage |
|---------|------|---------|-------|
| R | Read | Read PLC data | Single or batch read operations |
| W | Write | Write PLC data | Single write operations |
| X | Register | Register devices for monitoring | Batch registration with registration number |
| Y | Execute | Execute registered monitoring | Poll all devices registered under a number |

### Command Types (for R/W)

- **RSS**: Read/Write by specifying device type and address
- **SS**: Direct address specification
- **SB**: Bit-level operations

### Frame Structure

```
[ENQ][Station][Command][Type][Data][BCC][EOT]
```

- ENQ (0x05) - Start of frame
- Station - 2-digit station number
- Command - R/W/X/Y
- Type - RSS/SS/SB (for R/W only)
- Data - Hex-encoded data
- BCC - Block check character (XOR of all bytes)
- EOT (0x04) - End of frame

## Working with Tags

Tags are the primary data model. Key methods:

```java
// Create tag
Tag tag = new Tag(name, device, dataType, address, displayFormat, multiplier);

// Read/Write
client.readSingle(tag);
client.writeSingle(tag);

// Batch monitoring
client.registerDevicesToMonitor(List<Tag>, "registrationNumber");
List<Tag> updated = client.executeRegisteredDeviceToMonitor("registrationNumber");

// Bit manipulation
Tag.setBitOf(tag, bitIndex, value);
boolean status = Tag.getStatusOfBit(tag, bitIndex);
```

### Tag Address Format

- Word/DWord: Decimal address (e.g., "100" for D100)
- Bit: "address.bit" format (e.g., "100.5" for M100.5)

### Display Format Conversion

The `Tag` class handles conversion between PLC hex strings and display values based on `DisplayFormat`:
- **SIGNED_INT**: Two's complement signed integer
- **UNSIGNED_INT**: Unsigned integer
- **BINARY**: Binary string representation
- **HEX**: Hexadecimal string
- **FLOAT**: IEEE 754 floating point (DWord only)
- **STRING**: ASCII string

## JavaFX UI Controls

The library includes custom JavaFX controls for touch-enabled HMI applications:

### LSButton
- Bound to PLC bit address
- Action types: MOMENTARY, TOGGLE, SET_ONLY, RESET_ONLY
- Visual feedback with configurable true/false text and colors
- Optional feedback address different from write address
- Twin bit support (write to two bits simultaneously)

### LSTextField
- Bound to PLC word/dword address
- Touch keyboard integration (numeric/alphanumeric)
- Validation support (min/max, character limits)
- Auto-formatting based on display format
- Imperial measurement support via `ImperialMeasLSTextField`

### Touch Screen Mode

Enable/disable touch handlers globally:
```java
XGBCNetClient.setTouchScreen(true); // Default is true
```

## Exception Handling

Protocol-specific exceptions in `protocol/exceptions/`:
- **NoResponseException**: Timeout waiting for PLC response
- **FrameCheckException**: BCC validation failed (data corruption)
- **NoAcknowledgeMessageFromThePLCException**: PLC returned NAK (negative acknowledgment)

## Thread Safety

- `XGBCNetClient` is a thread-safe singleton
- `ConcurrentHashMap` for request/response mapping
- Synchronized methods on critical operations (read/write/register)
- `ReentrantLock` in LSButton for state management

## Connection Parameters

### Serial Connection
```java
new SerialConnectionParams(portName, baudRate, parity, dataBits, stopBits, stationNumber)
```
Default: 9600 baud, even parity, 8 data bits, 1 stop bit

### TCP Connection
```java
new TCPConnectionParams(ipAddress, port, stationNumber)
```
Default port: 8000

## Testing & Debugging

- Set log level: `client.setLogLevel(Level.INFO)`
- Test entry point: `TestMain.java`
- Response logging includes raw frames and parsed structures

## Dependencies

- **jSerialComm 2.10.4**: Serial port communication
- **JavaFX 11.0.2**: UI controls and property binding
- **ControlsFX 11.1.2**: Enhanced validation support
