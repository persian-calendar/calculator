# Calendar
[![](https://jitpack.io/v/persian-calendar/calculator.svg)](https://jitpack.io/#persian-calendar/calculator)

Mathematics expressions evaluator

# Usage

Add this in your root build.gradle at the end of repositories section:
```kotlin
allprojects {
    repositories {
        ...
        maven("https://jitpack.io")
    }
}
```

Now actually add the dependency:
```kotlin
dependencies {
    implementation("com.github.persian-calendar:calculator:0.0.1")
}
```
 
For other build tools support have a look at [this](https://jitpack.io/#persian-calendar/calculator).

# Web demo

There is a small demo of this library in the [web](web) module, which evaluates to JavaScript
and to WebAssembly and shows the result in the browser as it is typed.
