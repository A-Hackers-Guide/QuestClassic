# unidbg — component-level emulation

[`unidbg`](https://github.com/zhkl0228/unidbg) runs ARM userspace in a stubbed JVM-hosted environment. It's the standard tool for "take a native library out of an Android app and poke at it offline." We use it for research — you supply the libraries from your own Quest 2 OTA.

## Setup

```bash
# 1. Clone unidbg somewhere (not in this repo — it's its own project).
git clone https://github.com/zhkl0228/unidbg.git
cd unidbg
./gradlew :unidbg-android:assemble

# 2. Extract libshell.so (or whichever library you're analysing) from a Meta OTA.
#    Our tools/ directory extracts partitions; libshell.so lives inside the VrShell APK
#    which lives on the system_ext partition.
#    You do this on your own machine against your own OTA; we don't redistribute.
```

## Minimal driver

```kotlin
// ShellLibDriver.kt (to run inside unidbg)
import com.github.unidbg.AndroidEmulator
import com.github.unidbg.linux.android.AndroidEmulatorBuilder
import com.github.unidbg.linux.android.dvm.DalvikModule

fun main() {
    val emulator: AndroidEmulator = AndroidEmulatorBuilder.for64Bit()
        .setProcessName("libshell-fuzz")
        .build()
    emulator.memory.setLibraryResolver(com.github.unidbg.linux.android.AndroidResolver(23))

    val vm = emulator.createDalvikVM()
    vm.setVerbose(true)
    val module: DalvikModule = vm.loadLibrary(java.io.File("YOUR_PATH/libshell.so"), true)
    module.callJNI_OnLoad(emulator)

    // From here: call exported methods with crafted inputs.
    // e.g. emulator.getMemory().pointer(x).setString(0, "fuzz input")
    //      then invoke a specific native function by address.
}
```

## What to look at first

We'd suggest these entry points from `libshell.so` (every one is a reachable JNI entry — a bug here could be triggered by any app the user installs):

- `Java_com_oculus_vrshell_ShellApplication_nativeDumpsys`
- `Java_com_oculus_vrshell_ShellApplication_nativeInjectPanelIpc`
- `Java_com_oculus_vrshell_ShellApplication_nativeFrameCommand`
- `Java_com_oculus_vrshell_ShellApplication_nativeHandleQAMSelectionResult`

These take Parcels/strings/ints from the Java side and parse them in C++. Classic buffer-handling bug territory.

## Rules of engagement

- Treat any crashes you find as a defensive-security finding. Write it up with:
  - Reproducer input (binary blob is fine).
  - Crash address + instruction.
  - Which JNI entry you called it through.
- **Report to Meta via [bugbounty.meta.com](https://bugbounty.meta.com/)** before publishing.
- Do NOT develop a working exploit chain against live devices before reporting.

## Alternative: `qiling`

Same idea, different tool: [qiling.io](https://qiling.io). Python-first. Can sometimes emulate things `unidbg` can't.

## What we deliberately don't do

- Package an Android emulator pre-loaded with Meta's `.so` files.
- Automate exploit-chain construction.
- Ship a "one-click fuzz Meta firmware" tool.

The responsible version of this work is: you extract, you stub, you fuzz, you report. We can help with the first two from your own extractions.

---

_Last reviewed Nov 2026._
