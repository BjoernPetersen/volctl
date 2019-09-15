# volctl

[![GitHub (pre-)release](https://img.shields.io/github/release/BjoernPetersen/volctl/all.svg)](https://github.com/BjoernPetersen/volctl/releases) [![GitHub license](https://img.shields.io/github/license/BjoernPetersen/volctl.svg)](https://github.com/BjoernPetersen/volctl/blob/master/LICENSE)

A simple Kotlin library providing access to audio volume control on Windows and Linux.
Can also be used from Java.

The library uses native C++ code to directly access the relevant system APIs,
there are no further dependencies.

## Compatibility

This library is compatible with Java 1.8+.

## Usage

### Example

`Kotlin`

```kotlin
val volumeControl = VolumeControl()
// Gets the current master audio volume
val value: Int = volumeControl.volume
// Sets the current master audio volume to 82%
volumeControl.volume = 82
```

`Java`

```java
VolumeControl volumeControl = new VolumeControl();
// Gets the current master audio volume
int value = volumeControl.getVolume();
// Sets the current master audio volume to 82%
volumeControl.setVolume(82);
```

### Gradle

#### Kotlin DSL

`build.gradle.kts`

```kotlin
dependencies {
    // ...
    implementation("com.github.bjoernpetersen:volctl:${Lib.VOLCTL}")
    // or
    implementation(
        group = "com.github.bjoernpetersen",
        name = "volctl",
        version = Lib.VOLCTL)
}
```

#### Groovy DSL

`build.gradle`

```groovy
dependencies {
    // ...
    implementation 'com.github.bjoernpetersen:volctl:$volctlVersion'
}
```

### Maven

`pom.xml`

```xml
<dependency>
    <groupId>com.github.bjoernpetersen</groupId>
    <artifactId>volctl</artifactId>
    <version>${volctl.version}</version>
</dependency>
```

## Building

Gradle is used to build the project.

Before packaging, you'll need to generate the header files and compile the native library:

```bash
./gradlew buildNative
```

Note that this requires CMake and a C++ toolchain to be installed.
Only the native library for the current platform will be built.

## License

This project is released under the MIT License. That includes every file in this repository,
unless explicitly stated otherwise at the top of a file.
A copy of the license text can be found in the [LICENSE file](LICENSE).
