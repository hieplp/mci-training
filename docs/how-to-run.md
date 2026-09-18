# Running the Application — Beginner's Guide

This guide explains how to run the **mci-web** application on your own
computer. No programming experience is required — follow the steps in order
and the app will be running in your web browser.

**What the app does:** it adds two whole numbers of any size and displays
the full column-by-column working, the way you would solve it on paper.

| | |
|---|---|
| **Time required** | ~15 minutes the first time (mostly downloading); under a minute after that |
| **You will need** | A computer running Windows, macOS, or Linux, and an internet connection |
| **Cost** | Nothing — everything used here is free |

---

## Step 1 — Install Java

The app is written in Java, so your computer needs **Java 21 or newer**
installed first.

### Windows

1. Open https://adoptium.net/temurin/releases/?version=21 in your browser.
2. Under **Operating System** choose **Windows**, under **Architecture**
   choose **x64**, and download the **MSI** package.
3. Double-click the downloaded file and follow the installer.
4. When the installer shows **Custom Setup**, enable the option
   **"Set JAVA_HOME variable"** (it is disabled by default — click it and
   choose *"Will be installed on local hard drive"*).

### macOS

1. Open https://adoptium.net/temurin/releases/?version=21 in your browser.
2. Under **Operating System** choose **macOS**. Under **Architecture**:
   - **AArch64** — Macs with Apple Silicon (M1, M2, M3, M4)
   - **x64** — older Intel-based Macs

   (Not sure? Click the Apple menu  → **About This Mac** — "Chip" means
   Apple Silicon, "Processor" means Intel.)
3. Download the **PKG** package, double-click it, and follow the installer.

### Linux (Ubuntu / Debian)

Open a terminal and run:

```bash
sudo apt update && sudo apt install -y temurin-21-jdk
```

> [!NOTE]
> On other distributions, install any package that provides JDK 21 or newer
> (for example `openjdk-21-jdk`).

### Verify the installation

Open a terminal and run:

```bash
java -version
```

Expected output (the exact numbers may differ):

```
openjdk version "21.0.x" 2025-xx-xx
OpenJDK Runtime Environment Temurin-21...
```

If you see `command not found` (macOS/Linux) or `'java' is not recognized`
(Windows), Java is not on your PATH — reinstall it and make sure the
installer option to set environment variables is enabled, then open a
**new** terminal window and try again.

> [!TIP]
> **How to open a terminal**
>
> | OS | Steps |
> |---|---|
> | Windows | Press `Win + R`, type `cmd`, press Enter (PowerShell also works) |
> | macOS | Press `Cmd + Space`, type `Terminal`, press Enter |
> | Linux | Press `Ctrl + Alt + T` |

---

## Step 2 — Download the project

Choose **one** of the two options below.

### Option A — Download a ZIP file (recommended, no tools needed)

1. Go to https://github.com/hieplp/mci-training
2. Click the green **Code** button, then **Download ZIP**.
3. Extract the ZIP file. You will get a folder named `mci-training-main`.

### Option B — Clone with Git

If you have Git installed, run in a terminal:

```bash
git clone https://github.com/hieplp/mci-training.git
```

> [!IMPORTANT]
> Whichever option you use, the extracted folder must contain **both**
> `mci-core` and `mci-web` sitting side by side. The web app depends on the
> core library and will not start without it — do not move or rename these
> folders.

---

## Step 3 — Open a terminal inside the `mci-web` folder

The app is started from inside the `mci-web` subfolder.

**Windows (easiest way):** open the `mci-web` folder in File Explorer, click
the address bar, type `cmd`, and press Enter — a terminal opens already in
the right place.

**Any OS:** open a terminal and navigate to the folder:

```bash
cd path/to/mci-training-main/mci-web
```

Replace `path/to/...` with the real location where you extracted or cloned
the project — for example `cd ~/Downloads/mci-training-main/mci-web`.

---

## Step 4 — Start the application

Run the command for your operating system:

**macOS / Linux**

```bash
./run.sh
```

**Windows**

```cmd
gradlew.bat bootRun
```

> [!NOTE]
> The first launch takes a few minutes: the build tool downloads Java
> components and libraries it needs. This happens once — every later start
> takes only a few seconds.

The app is ready when you see a line similar to:

```
Started MciWebApplication in 3.4 seconds
```

**Leave the terminal window open** while you use the app. Closing it stops
the app.

---

## Step 5 — Open the app in your browser

Go to the address that matches how you started the app:

| How you started it | Address |
|---|---|
| `./run.sh` (macOS / Linux) | http://localhost:8081 |
| `gradlew.bat bootRun` (Windows) | http://localhost:8080 |

Enter two numbers in the input fields and submit — the page shows the sum
together with every column-addition step.

> [!TIP]
> `localhost` simply means "this computer" — the app runs entirely on your
> machine and nothing is sent over the internet.

---

## Stopping the application

Click inside the terminal window and press `Ctrl + C`. You can then close
the terminal.

To start it again later, repeat **Step 4** only — everything else is a
one-time setup.

---

## Troubleshooting

| Problem | Solution |
|---|---|
| `java -version` reports "command not found" or "not recognized" | Java is not installed or not on PATH — repeat **Step 1**, then open a new terminal. |
| `Permission denied: ./run.sh` | Run `chmod +x run.sh` once, then `./run.sh` again. |
| Browser shows "can't reach this page" | Check the port matches your start command (8081 vs 8080 — see **Step 5**) and that the terminal still shows `Started MciWebApplication`. |
| `Port 8081 was already in use` | Another copy of the app is already running — either use it, or find the other terminal and press `Ctrl + C` there first. |
| Build fails with an error mentioning `mci-core` | The `mci-core` folder must sit **next to** `mci-web` in the same parent folder — re-extract the ZIP without moving folders. |
| Windows: `'gradlew' is not recognized` | You are not inside the `mci-web` folder, or you typed `gradlew` instead of `gradlew.bat` — repeat **Step 3**. |
| Page loads but shows an error after submitting | Only whole, non-negative numbers are accepted — remove spaces, letters, minus signs, and decimal points. |

If none of the above helps, copy the full text of the error from the
terminal and include it when asking for help — it contains the details
needed to diagnose the problem.
