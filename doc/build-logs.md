# apis-web: OS-Specific Build & Logging Guide

Hello there! Welcome to the `apis-web` contributor team. To help you get up and running across different operating systems, I've put together this guide. It covers how to build the project, run it, and most importantly, how to keep an eye on those logs so you can debug like a pro!

---

## 🛠️ Prerequisites (All Systems)

Before we dive into OS-specific steps, ensure you have these installed:
- **JDK 11 or higher**: The project is built for Java 11.
- **Apache Maven**: For building and packaging.
- **Groovy**: Used for internal scripts.
- **Git**: To clone the repositories.

### 📦 Dependency Setup
`apis-web` relies on two other core components. You **must** install them in your local Maven repository first:

1. **Clone and Install `apis-bom`**:
   ```bash
   git clone https://github.com/hyphae/apis-bom.git
   cd apis-bom
   mvn install
   ```

2. **Clone and Install `apis-common`**:
   ```bash
   git clone https://github.com/hyphae/apis-common.git
   cd apis-common
   mvn install
   ```

---

## 🐧 Linux (Ubuntu/Debian/Fedora)

### Build Steps
1. Open your terminal in the `apis-web` root directory.
2. Run the packaging command:
   ```bash
   mvn package
   ```

### Running the App
1. Move to the execution folder:
   ```bash
   cd exe
   ```
2. Start the application using the provided script:
   ```bash
   bash start.sh
   ```

### Logging Commands
To capture output to a file and see it in real-time:
```bash
bash start.sh 2>&1 | tee apis-runtime.log
```
To view existing file logs:
```bash
tail -f 0.0.log
```

---

## 🍎 macOS

### Build Steps
The build process is identical to Linux:
```bash
mvn package
```

### Running the App
The `start.sh` script automatically detects macOS and uses `cluster-mac.xml` for Hazelcast:
```bash
cd exe
bash start.sh
```

### Logging Commands
MacOS uses the same shell commands as Linux:
```bash
# Capture combined stdout and stderr
bash start.sh > output.log 2>&1 &
# Watch logs live
tail -f 0.0.log
```

---

## 🪟 Windows (PowerShell/CMD)

### Build Steps
1. Open PowerShell or CMD in the `apis-web` root.
2. Run Maven:
   ```powershell
   mvn package
   ```

### Running the App
Windows doesn't use the `.sh` scripts directly (unless using Git Bash). To run in PowerShell:
1. Move to the `exe` folder:
   ```powershell
   cd exe
   ```
2. Run the Java command manually:
   ```powershell
   java -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Tokyo -Djava.util.logging.config.file=./logging.properties -Dvertx.hazelcast.config=.\cluster.xml -jar ..\target\apis-web-3.0.0-fat.jar run jp.co.sony.csl.dcoes.apis.tools.web.util.Starter --conf .\config.json --cluster --cluster-host 127.0.0.1
   ```

### Logging Commands
In PowerShell, you can use `Tee-Object` to log and view simultaneously:
```powershell
# Run and log
& java ... | Tee-Object -FilePath "apis.log"

# View file logs like 'tail'
Get-Content .\0.0.log -Wait
```

---

## 📜 Capturing Build & Runtime Logs

### Build Logs
If `mvn package` fails, capture the full stack trace:
- **Linux/macOS**: `mvn package > build.log 2>&1`
- **Windows**: `mvn package | Out-File -FilePath build.log`

### Runtime Logs
`apis-web` is configured via `logging.properties` to generate:
- `0.0.log`: General info and debug logs.
- `0.0.err`: Warning and error logs.
The `%u` in the filename expands to a unique number if multiple instances are running.

---

## 🚑 Known Issues & Fixes

### 1. `Could not find artifact jp.co.sony.csl.dcoes.apis:apis-bom:pom:3.0.0`
- **Meaning**: Maven can't find the parent/dependency repository.
- **Fix**: Go back to the **Dependency Setup** section and ensure you ran `mvn install` in the `apis-bom` and `apis-common` folders.

### 2. `Address already in use`
- **Meaning**: Another service is using the port `apis-web` or Hazelcast needs.
- **Fix**: 
  - Stop any existing instances using `bash stop.sh` or `bash kill.sh`.
  - Check for processes on port 8888 (Multicast) or your configured HTTP port.

### 3. `Hazelcast Cluster Join Failure`
- **Meaning**: The nodes can't see each other on the network.
- **Fix**: 
  - Ensure `-Djava.net.preferIPv4Stack=true` is set.
  - On Windows, ensure your Firewall allows Java/Hazelcast traffic.
  - On macOS, ensure you are using `cluster-mac.xml` (the `start.sh` does this automatically).

### 4. `UnsupportedClassVersionError` (class file version 55.0)
- **Meaning**: You are trying to run the app with a Java version older than 11.
- **Fix**: Run `java -version` and ensure it shows version 11 or higher. Update your `JAVA_HOME` if necessary.

---
Happy coding! If you hit a wall, check the logs first—they usually have the answer!
