# Bridge Swift

Kotlin interface in `iosMain` → Swift implements it in `iosApp` → Inject via Koin DI.

**Architecture Reference:** `shared/kmp-patterns.md`

## When you arrive here

This skill is the **iOS-Swift leg** of the Rule 14 platform path (`skills/kmp-create-feature/references/architecture/platform.md`). Reach it when a feature's iOS `actual` can't be written cleanly in Kotlin/Native and needs a Swift class. `/kmp-create-feature` and `/kmp-modify-feature` finish the Kotlin side (the `commonMain` interface + Android/desktop actuals + `expect/actual val platformModule`) and then route the user here — they never call this skill directly. Android almost never needs a bridge (Kotlin calls the Android SDK directly), so this is iOS-specific. The Provider-wraps-bridge-in-`Either<T>` pattern below is the same DataSource contract platform.md describes.

## Critical: Swift Inheritance

Swift class inherits from `<ModulePrefix><InterfaceName>`:

| Module | Kotlin | Swift Inherits |
|--------|--------|----------------|
| `core:data` | `RecaptchaBridge` | `DataRecaptchaBridge` |
| `feature:auth` | `AuthBridge` | `AuthAuthBridge` |

Use Xcode autocomplete (`Ctrl+Space`) to find exact protocol name.

## Implementation Steps

### 1. Bridge Interface (`iosMain`)
```kotlin
// <module>/src/iosMain/kotlin/{PKG_PREFIX}/<feature>/<Feature>Bridge.kt
interface <Feature>Bridge {
    suspend fun execute(param: String): String
}
```

### 2. Provider (`iosMain`)
```kotlin
// <module>/src/iosMain/kotlin/{PKG_PREFIX}/<feature>/IOS<Feature>Provider.kt
class IOS<Feature>Provider(private val bridge: <Feature>Bridge) : <Feature>Provider {
    override suspend fun execute(): Either<String> =
        try { Either.Success(bridge.execute("param")) }
        catch (e: Exception) { Either.Failure(ErrorModel.Exception(e)) }
}
```

### 3. Swift Implementation (`iosApp`)
```swift
// iosApp/iosApp/<Feature>/<Feature>BridgeImpl.swift
import ComposeApp

class <Feature>BridgeImpl: <ModulePrefix><Feature>Bridge {
    func execute(param: String, completionHandler: @escaping (String?, Error?) -> Void) {
        Task {
            do { completionHandler(try await nativeOperation(param), nil) }
            catch { completionHandler(nil, error) }
        }
    }
}
```

### 4. DI Connection
**MainViewController.kt** (`composeApp/src/iosMain/`):
```kotlin
fun MainViewController(bridge: <Feature>Bridge) = ComposeUIViewController(
    configure = { initKoin { modules(module { single { bridge } }) } }
) { App() }
```

**ContentView.swift** (`iosApp/iosApp/`):
```swift
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(bridge: <Feature>BridgeImpl())
    }
}
```

### 5. Register Provider (`iosMain`)
```kotlin
// <module>/src/iosMain/kotlin/{PKG_PREFIX}/<feature>/di/Module.ios.kt
internal actual val platformModule = module {
    singleOf(::IOS<Feature>Provider).bind<<Feature>Provider>()
}
```

### 6. Export Module
```kotlin
// composeApp/build.gradle.kts
iosTarget.binaries.framework { export(project(":core:data")) }
```

## Signature Mapping

| Kotlin | Swift |
|--------|-------|
| `suspend fun foo()` | `func foo(completionHandler: @escaping (Error?) -> Void)` |
| `suspend fun foo(): T` | `func foo(completionHandler: @escaping (T?, Error?) -> Void)` |
| `Int` | `Int32` |
| `Long` | `Int64` |
| `Boolean` (return) | `KotlinBoolean` |
| `Boolean` (param) | `Bool` |

## Validation

```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

## Checklist

- [ ] Interface in `<module>/src/iosMain/kotlin/{PKG_PREFIX}/<feature>/`
- [ ] Provider wraps in try-catch returning `Either<T>`
- [ ] Swift inherits `<ModulePrefix><InterfaceName>`
- [ ] Completion handler signature matches exactly
- [ ] MainViewController accepts + registers bridge
- [ ] ContentView passes implementation
- [ ] Module exported in `composeApp/build.gradle.kts`
