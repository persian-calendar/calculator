# Calculator Web

A tiny HTML demo of this library: type a mathematics expression, see it evaluated as you type,
and keep a short history of what you tried. The evaluation is `eval(expression)` in the
browser.

## How the page is put together

The Kotlin side is the evaluator and nothing else: it calls `calculatorReady` with the
library's `eval`, and `main.js` does the rest.

```
web/src/webMain/
├── kotlin/…/web/Main.kt   the evaluator, handed to the page
└── resources/
    ├── index.html         the markup and all of the CSS
    ├── main.js            the page: the box, the result, the examples, the history
    ├── sw.js              the service worker, for offline use
    └── icons/             the app icon, and the PNGs generated from `icon.svg`
```

`main.js` is loaded before the bundle, because it defines `calculatorReady` and the bundle
calls it as soon as it runs. That order matters: the Wasm build starts up after the page has
loaded, so a value published on `window` by the Kotlin side would arrive too late.

The evaluator *throws* when an expression does not parse, and the message is what the page
shows. What a thrown value looks like depends on the target - a real `Error` in JavaScript, a
Kotlin exception handed over by the WebAssembly runtime - so the page takes whatever arrives
and looks for the message in it, falling back to a plain sentence when there is nothing
readable.

## Run locally

From the repository root:

```sh
# JS backend (faster to build)
./gradlew :web:jsBrowserDevelopmentRun

# Wasm backend
./gradlew :web:wasmJsBrowserDevelopmentRun
```

Then open http://localhost:8080/.

## Build a static bundle

```sh
# JS bundle  -> web/build/dist/js/productionExecutable/
./gradlew :web:jsBrowserDistribution

# Wasm bundle -> web/build/dist/wasmJs/productionExecutable/
./gradlew :web:wasmJsBrowserDistribution
```

Either bundle directory is complete and can be served as it is. When the icons are
regenerated, `rsvg-convert` (from `librsvg`) has to be installed:

```sh
./gradlew :web:generateWebIcons
```
