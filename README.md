# XGBCNet Protocol Communication Library

A comprehensive Java library for industrial PLC communication using the XGBCNet protocol over Serial and TCP connections.

## 🚀 Overview

This library provides a robust solution for communicating with Programmable Logic Controllers (PLCs) in industrial automation environments. It implements the XGBCNet protocol to enable real-time data exchange, monitoring, and control operations.

## ✨ Features

### Core Communication
- **Multi-Protocol Support**: Serial (RS232/RS485) and TCP/IP connections (future)
- **XGBCNet Protocol Implementation**: Full support for reading/writing PLC data
- **Real-time Data Monitoring**: Register devices for continuous monitoring
- **Frame Validation**: Built-in error checking and data integrity verification

### Data Types Support
- **Bit Operations**: Individual bit read/write operations
- **Word/DWord Operations**: 16-bit and 32-bit data handling
- **String Operations**: Text data exchange with PLCs
- **Float Operations**: Floating-point number support
- **Binary Data**: Raw binary data manipulation

### Advanced Features
- **Device Registration**: Batch register multiple tags for efficient monitoring
- **Connection Management**: Automatic connection handling and recovery
- **Thread-Safe Operations**: Concurrent access support
- **Logging Support**: Comprehensive logging with configurable levels
- **Touch Interface Controls**: Specialized UI components for touch screens

## 🏗️ Architecture

```
XGBCNetClient (Singleton)
├── Connection Layer
│   ├── SerialConnection (RS232/RS485)
│   └── TCPConnection (Ethernet)
├── Protocol Layer
│   ├── Command Processing (R/W/X/Y)
│   ├── Frame Management
│   └── Response Evaluation
├── Device Layer
│   ├── Tag Management
│   ├── Data Type Handling
│   └── Display Formatting
└── UI Controls (JavaFX)
    ├── LSButton (Touch Button)
    ├── LSTextField (Touch Input)
    └── Touch Keyboards
```

## 🔧 Quick Start

### 1. Basic Connection Setup

```java
// Serial Connection
SerialConnectionParams serialParams = new SerialConnectionParams(
    "COM3", 9600, Parity.even, 8, 1, 2
);

// TCP Connection  
TCPConnectionParams tcpParams = new TCPConnectionParams(
    "192.168.1.100", 8000, 2
);

// Connect to PLC
XGBCNetClient client = XGBCNetClient.getInstance();
boolean connected = client.connect(serialParams); // or tcpParams
```

### 2. Reading Data from PLC

```java
// Create a tag for reading
Tag temperatureTag = new Tag(
    "Temperature",           // Name
    Device.D,               // Device type (D-register)
    DataType.Word,          // Data type
    "100",                  // Address
    DisplayFormat.SIGNED_INT, // Display format
    10                      // Multiplier (for decimals)
);

// Read single value
client.readSingle(temperatureTag);
System.out.println("Temperature: " + temperatureTag.getValue());
```

### 3. Writing Data to PLC

```java
// Create a tag for writing
Tag setpointTag = new Tag(
    "Setpoint", Device.D, DataType.Word, "200", 
    DisplayFormat.SIGNED_INT, 10
);

// Set value and write to PLC
setpointTag.setValueAsHexString("01F4"); // 500 in hex
client.writeSingle(setpointTag);
```

### 4. Batch Monitoring

```java
// Create multiple tags
List<Tag> monitorTags = Arrays.asList(
    new Tag("Pressure", Device.D, DataType.Word, "300", DisplayFormat.SIGNED_INT, 1),
    new Tag("Flow", Device.D, DataType.Word, "301", DisplayFormat.SIGNED_INT, 1),
    new Tag("Status", Device.M, DataType.Bit, "100", DisplayFormat.BINARY, null)
);

// Register for monitoring
client.registerDevicesToMonitor(monitorTags, "1");

// Execute monitoring (call periodically)
List<Tag> updatedTags = client.executeRegisteredDeviceToMonitor("1");
```

## 📋 Supported Data Types

| Type | Description | Example Usage |
|------|-------------|---------------|
| **Bit** | Single bit operations | Status indicators, alarms |
| **Byte** | 8-bit unsigned integer | Small counters, flags |
| **Word** | 16-bit signed/unsigned | Temperature, pressure values |
| **DWord** | 32-bit signed/unsigned | Large counters, timestamps |
| **Float** | 32-bit floating point | Precise measurements |
| **String** | Text data | Product names, messages |

## 🎛️ UI Components

### Touch-Enabled Controls

```java
// Touch Button for PLC control
LSButton startButton = new LSButton();
startButton.setDevice(Device.M);
startButton.setTagAddressInHex("100.0"); // M100.0
startButton.setActionType(BtnActionType.MOMENTARY);
startButton.setTrueText("RUNNING");
startButton.setFalseText("STOPPED");

// Touch TextField for value input
LSTextField setpointField = new LSTextField();
setpointField.setDevice(Device.D);
setpointField.setTagAddress("200");
setpointField.setDataType(DataLen.Word);
setpointField.setDisplayFormat(DisplayFormat.SIGNED_INT);
setpointField.setMinValue(0);
setpointField.setMaxValue(1000);
```

## 🔌 Connection Types

### Serial Connection
```java
SerialConnectionParams params = new SerialConnectionParams(
    "COM1",          // Port name
    9600,            // Baud rate
    Parity.even,     // Parity
    8,               // Data bits
    1,               // Stop bits
    2                // Station number
);
```

### TCP Connection
```java
TCPConnectionParams params = new TCPConnectionParams(
    "192.168.1.100", // IP address
    8000,            // Port
    2                // Station number
);
```

## 📊 Protocol Commands

| Command | Purpose | Description |
|---------|---------|-------------|
| **R** | Read | Read data from PLC registers |
| **W** | Write | Write data to PLC registers |
| **X** | Register | Register devices for monitoring |
| **Y** | Execute | Execute registered device monitoring |

## 🛠️ Configuration

### Logging
```java
XGBCNetClient client = XGBCNetClient.getInstance();
client.setLogLevel(Level.INFO); // Set logging level
```

### Touch Screen Mode
```java
XGBCNetClient.setTouchScreen(true); // Enable touch mode
```

## 📦 Dependencies

- **JavaFX**: UI components and touch controls
- **jSerialComm**: Serial port communication
- **ControlsFX**: Enhanced UI validation

## 🔐 Thread Safety

The library is designed for multi-threaded environments:
- **Singleton Pattern**: Thread-safe client instance
- **Concurrent Collections**: Thread-safe request/response handling
- **Synchronized Methods**: Protected critical sections

## 🚨 Error Handling

The library includes comprehensive error handling:

```java
try {
    client.readSingle(tag);
} catch (NoResponseException e) {
    // Handle timeout
} catch (FrameCheckException e) {
    // Handle data corruption
} catch (NoAcknowledgeMessageFromThePLCException e) {
    // Handle PLC communication error
}
```

## 📈 Performance Tips

1. **Batch Operations**: Use device registration for multiple tags
2. **Connection Reuse**: Maintain persistent connections
3. **Optimal Polling**: Balance between real-time needs and network load
4. **Error Recovery**: Implement proper connection recovery logic

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Follow Java coding standards
4. Add comprehensive tests
5. Submit a pull request


## 🆘 Support

For technical support and questions:
- Create an issue in the GitHub repository
- Provide detailed error logs and configuration
- Include PLC model and connection details

