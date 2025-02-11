[//]: # (title: What's new in Kotlin %kotlinEapVersion%)

_[Released: %kotlinEapReleaseDate%](eap.md#build-details)_

> This document doesn't cover all of the features of the Early Access Preview (EAP) release, 
> but it highlights some major improvements.
>
> See the full list of changes in the [GitHub changelog](https://github.com/JetBrains/kotlin/releases/tag/v%kotlinEapVersion%).
>
{type="note"}

The Kotlin %kotlinEapVersion% release is out! It mostly covers the stabilization of the [new Kotlin K2 compiler](#kotlin-k2-compiler), 
which reached its Beta status for all targets since 1.9.20. In addition, there are [improvements for the Gradle build tool](#gradle-improvements)
as well as changes in [Kotlin/JS](#kotlin-js).

## IDE support

The Kotlin plugins that support %kotlinEapVersion% are bundled in the latest IntelliJ IDEA and Android Studio. 
You don't need to update the Kotlin plugin in your IDE. 
All you need to do is to [change the Kotlin version](configure-build-for-eap.md) to %kotlinEapVersion% in your build scripts.

For details about IntelliJ IDEA's support for the Kotlin K2 compiler, see [Support in IntelliJ IDEA](#support-in-intellij-idea).

## Kotlin K2 compiler

The JetBrains team is still working on the stabilization of the new Kotlin K2 compiler.
The new Kotlin K2 compiler will bring major performance improvements, speed up new language feature development,
unify all platforms that Kotlin supports, and provide a better architecture for multiplatform projects.

The K2 compiler is in [Beta](components-stability.md) for all target platforms: JVM, Native, Wasm, and JS.
The JetBrains team has ensured the quality of the new compiler by successfully compiling dozens of user and internal projects.
A large number of users are also involved in the stabilization process, trying the new K2 compiler in their projects and reporting any problems they find.

### Current K2 compiler limitations

Enabling K2 in your Gradle project comes with certain limitations that can affect projects using Gradle versions below 8.3 in the following cases:

* Compilation of source code from `buildSrc`.
* Compilation of Gradle plugins in included builds.
* Compilation of other Gradle plugins if they are used in projects with Gradle versions below 8.3.
* Building Gradle plugin dependencies.

If you encounter any of the problems mentioned above, you can take the following steps to address them:

* Set the language version for `buildSrc`, any Gradle plugins, and their dependencies:

  ```kotlin
  kotlin {
      compilerOptions {
          languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
          apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
      }
  }
  ```
  > If you configure language and API versions for specific tasks, these values will override the values set by the `compilerOptions` extension.
  > In this case, language and API versions should not be higher than 1.9.
  >
  {type="note"}

* Update the Gradle version in your project to 8.3 when it becomes available.

### Smart cast improvements

The Kotlin compiler can automatically cast an object to a type in specific cases,
saving you the trouble of having to explicitly cast it yourself. This is called [smart casting](typecasts.md#smart-casts).
The Kotlin K2 compiler now performs smart casts in even more scenarios than before.

In Kotlin %kotlinEapVersion%, we've made improvements related to smart casts in the following areas:
* [Local variables and further scopes](#local-variables-and-further-scopes)
* [Type checks with logical `or` operator](#type-checks-with-logical-or-operator)
* [Inline functions](#inline-functions)
* [Properties with function types](#properties-with-function-types)
* [Exception handling](#exception-handling)
* [Increment and decrement operators](#increment-and-decrement-operators)

#### Local variables and further scopes

Previously, if a variable was evaluated as not `null` within an `if` condition, 
the variable was smart cast, and information about this variable was shared further within the scope of the `if` block. 
However, if you declared the variable **outside** the `if` condition, no information about the variable was available 
within the `if` condition, so it couldn't be smart cast. This behavior was also seen with `when` expressions and `while` loops.

From Kotlin %kotlinEapVersion%, if you declare a variable before using it in your `if`, `when`, or `while` condition 
then any information collected by the compiler about the variable is accessible in the condition statement and its block for smart casting.
This can be useful when you want to do things like extract boolean conditions into variables.
Then, you can give the variable a meaningful name, which makes your code easier to read, and easily reuse the variable later in your code.
For example:

```kotlin
class Cat {
    fun purr() {
        println("Purr purr")
    }
}

fun petAnimal(animal: Any) {
    val isCat = animal is Cat
    if (isCat) {
        // In Kotlin %kotlinEapVersion%, the compiler can access
        // information about isCat, so it knows that
        // isCat was smart cast to type Cat.
        // Therefore, the purr() function is successfully called.
        // In Kotlin 1.9.20, the compiler doesn't know
        // about the smart cast, so calling the purr()
        // function triggers an error.
        animal.purr()
    }
}

fun main(){
    val kitty = Cat()
    petAnimal(kitty)
    // Purr purr
}
```
{kotlin-runnable="true" kotlin-min-compiler-version="2.0" id="kotlin-smart-casts-k2-local-variables" validate="false"}

#### Type checks with logical `or` operator

In Kotlin %kotlinEapVersion%, if you combine type checks for objects with an `or` operator (`||`), then a smart cast 
is made to their closest common supertype. Before this change, a smart cast was always made to `Any` type. 
In this case, you still had to manually check the type of the object afterward before you could access any of its properties or call its functions.
For example:

```kotlin
interface Status {
    fun signal() {}
}

interface Ok : Status
interface Postponed : Status
interface Declined : Status

fun signalCheck(signalStatus: Any) {
    if (signalStatus is Postponed || signalStatus is Declined) {
        // signalStatus is smart cast to a common supertype Status
        signalStatus.signal()
        // Prior to Kotlin %kotlinEapVersion%, signalStatus is smart cast 
        // to type Any, so calling the signal() function triggered an
        // Unresolved reference error. The signal() function can only 
        // be called successfully after another type check:
        
        // check(signalStatus is Status)
        // signalStatus.signal()
    }
}
```

> The common supertype is an **approximation** of a union type. [Union types](https://en.wikipedia.org/wiki/Union_type)
> are not supported in Kotlin.
>
{type="note"}

#### Inline functions

In Kotlin %kotlinEapVersion%, the K2 compiler treats inline functions differently, 
allowing it to determine in combination with other compiler analyses whether it's safe to smart cast.

Specifically, inline functions are now treated as having an implicit 
[`callsInPlace`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.contracts/-contract-builder/calls-in-place.html) contract.
So any lambda functions passed to an inline function are called "in place". Since lambda functions are called in place, 
the compiler knows that a lambda function can't leak references to any variables contained within its function body. 
The compiler uses this knowledge along with other compiler analyses to decide if it's safe to smart cast any of the captured variables. 
For example:

```kotlin
interface Processor {
    fun process()
}

inline fun inlineAction(f: () -> Unit) = f()

fun nextProcessor(): Processor? = null

fun runProcessor(): Processor? {
    var processor: Processor? = null
    inlineAction {
        // In Kotlin %kotlinEapVersion%, the compiler knows that processor 
        // is a local variable, and inlineAction() is an inline function, so 
        // references to processor can't be leaked. Therefore, it's safe 
        // to smart cast processor.
      
        // If processor isn't null, processor is smart cast
        if (processor != null) {
            // The compiler knows that processor isn't null, so no safe call 
            // is needed
            processor.process()

            // In Kotlin 1.9.20, you have to perform a safe call:
            // processor?.process()
        }

        processor = nextProcessor()
    }

    return processor
}
```

#### Properties with function types

In previous versions of Kotlin, it was a bug that class properties with a function type weren't smart cast.
We fixed this behavior in the K2 compiler in Kotlin %kotlinEapVersion%.
For example:

```kotlin
class Holder(val provider: (() -> Unit)?) {
    fun process() {
        // In Kotlin %kotlinEapVersion%, if provider isn't null, then
        // provider is smart cast
        if (provider != null) {
            // The compiler knows that provider isn't null
            provider()

            // In 1.9.20, the compiler doesn't know that provider isn't 
            // null, so it triggers an error:
            // Reference has a nullable type '(() -> Unit)?', use explicit '?.invoke()' to make a function-like call instead
        }
    }
}
```

This change also applies if you overload your `invoke` operator. For example:

```kotlin
interface Provider {
    operator fun invoke()
}

interface Processor : () -> String

class Holder(val provider: Provider?, val processor: Processor?) {
    fun process() {
        if (provider != null) {
            provider() 
            // In 1.9.20, the compiler triggers an error: 
            // Reference has a nullable type 'Provider?' use explicit '?.invoke()' to make a function-like call instead
        }
    }
}
```

#### Exception handling

In Kotlin %kotlinEapVersion%, we've made improvements to exception handling so that smart cast information can be passed
on to `catch` and `finally` blocks. This change makes your code safer as the compiler keeps track of whether 
your object has a nullable type or not.
For example:

```kotlin
//sampleStart
fun testString() {
    var stringInput: String? = null
    // stringInput is smart cast to String type
    stringInput = ""
    try {
        // The compiler knows that stringInput isn't null
        println(stringInput.length)
        // 0

        // The compiler rejects previous smart cast information for 
        // stringInput. Now stringInput has the String? type.
        stringInput = null

        // Trigger an exception
        if (2 > 1) throw Exception()
        stringInput = ""
    } catch (exception: Exception) {
        // In Kotlin %kotlinEapVersion%, the compiler knows stringInput 
        // can be null, so stringInput stays nullable.
        println(stringInput?.length)
        // null

        // In Kotlin 1.9.20, the compiler says that a safe call isn't
        // needed, but this is incorrect.
    }
}
//sampleEnd
fun main() {
    testString()
}
```
{kotlin-runnable="true" kotlin-min-compiler-version="2.0" id="kotlin-smart-casts-k2-exception-handling"}

#### Increment and decrement operators

Prior to Kotlin %kotlinEapVersion%, the compiler didn't understand that the type of an object can change after using 
an increment or decrement operator. As the compiler couldn't accurately track the object type, 
your code could lead to unresolved reference errors. In Kotlin %kotlinEapVersion%, this is fixed:

```kotlin
interface Rho {
    operator fun inc(): Sigma = TODO()
}

interface Sigma : Rho {
    fun sigma() = Unit
}

interface Tau {
    fun tau() = Unit
}

fun main(input: Rho) {
    var unknownObject: Rho = input

    // Check if unknownObject inherits from the Tau interface
    if (unknownObject is Tau) {

        // Uses the overloaded inc() operator from interface Rho,
        // which smart casts the type of unknownObject to Sigma.
        ++unknownObject

        // In Kotlin %kotlinEapVersion%, the compiler knows unknownObject has type
        // Sigma, so the sigma() function is called successfully.
        unknownObject.sigma()

        // In Kotlin 1.9.20, the compiler thinks unknownObject has type
        // Tau, so calling the sigma() function throws an error.

        // In Kotlin %kotlinEapVersion%, the compiler knows unknownObject has type
        // Sigma, so calling the tau() function throws an error.
        unknownObject.tau()
        // Unresolved reference 'tau'

        // In Kotlin 1.9.20, the compiler mistakenly thinks that 
        // unknownObject has type Tau, so the tau() function is
        // called successfully.
    }
}
```
{kotlin-runnable="true" kotlin-min-compiler-version="2.0" id="kotlin-smart-casts-k2-increment-decrement-operators" validate="false"}

### Kotlin Multiplatform improvements

In Kotlin %kotlinEapVersion%, we've made improvements in the K2 compiler related to Kotlin Multiplatform in the following areas:
* [Separation of common and platform sources during compilation](#separation-of-common-and-platform-sources-during-compilation)
* [Different visibility levels of expected and actual declarations](#different-visibility-levels-of-expected-and-actual-declarations)

#### Separation of common and platform sources during compilation

Previously, due to its design, the Kotlin compiler couldn't keep common and platform source sets separate at compile time.
This means that common code could access platform code, which resulted in different behavior between platforms. In addition,
some compiler settings and dependencies from common code were leaked into platform code.

In Kotlin %kotlinEapVersion%, we redesigned the compilation scheme as part of the new Kotlin K2 compiler so that there is a strict 
separation between common and platform source sets. The most noticeable change is when you use [expected and actual functions](multiplatform-expect-actual.md#expected-and-actual-functions).
Previously, it was possible for a function call in your common code to resolve to a function in platform code. For example:

<table header-style="top">
   <tr>
       <td>Common code</td>
       <td>Platform code</td>
   </tr>
   <tr>
<td>

```kotlin
fun foo(x: Any) = println("common foo")

fun exampleFunction() {
    foo(42)  
}
```

</td>
<td>

```kotlin
// JVM
fun foo(x: Int) = println("platform foo")

// JavaScript
// There is no foo() function overload
// on the JavaScript platform
```

</td>
</tr>
</table>

In this example, the common code has different behavior depending on which platform it is run on:

* On the JVM platform, calling the `foo()` function in common code results in the `foo()` function from platform code 
being called: `platform foo`

* On the JavaScript platform, calling the `foo()` function in common code results in the `foo()` function from common 
code being called: `common foo`, since there is none available in platform code.

In Kotlin %kotlinEapVersion%, common code doesn’t have access to platform code, so both platforms successfully resolve the `foo()` 
function to the `foo()` function in common code: `common foo`

In addition to the improved consistency of behavior across platforms, we also worked hard to fix cases where there was 
conflicting behavior between IntelliJ IDEA or Android Studio and the compiler. For example, if you used [expected and actual classes](multiplatform-expect-actual.md#expected-and-actual-classes):

<table header-style="top">
   <tr>
       <td>Common code</td>
       <td>Platform code</td>
   </tr>
   <tr>
<td>

```kotlin
expect class Identity {
    fun confirmIdentity(): String
}

fun common() {
    // Before %kotlinEapVersion%,
    // it triggers an IDE-only error
    Identity().confirmIdentity()
    // RESOLUTION_TO_CLASSIFIER : Expected class
    // Identity has no default constructor.
}

```

</td>
<td>

```kotlin
actual class Identity {
    actual fun confirmIdentity() = "expect class fun: jvm"
}
```

</td>
</tr>
</table>

In this example, the expected class `Identity` has no default constructor, so it can’t be called successfully in common code.
Previously, only an IDE error was reported, but the code still compiled successfully on the JVM. However, now the compiler
correctly reports an error:

```kotlin
Expected class 'expect class Identity : Any' does not have default constructor
```

##### When resolution behavior doesn’t change

We are still in the process of migrating to the new compilation scheme, so the resolution behavior is still the same when
you call functions that aren’t within the same source set. You will notice this difference mainly when you use overloads 
from a multiplatform library in your common code.

For example, if you have this library, which has two `whichFun()` functions with different signatures:

```kotlin
// Example library

// MODULE: common
fun whichFun(x: Any) = println("common function") 

// MODULE: JVM
fun whichFun(x: Int) = println("platform function")
```

If you call the `whichFun()` function in your common code, the function that has the most relevant argument type in the 
library is resolved:

```kotlin
// A project that uses the example library for the JVM target

// MODULE: common
fun main(){
    whichFun(2) 
    // platform function
}
```
In comparison, if you declare the overloads for `whichFun()` within the same source set, the function from common code 
is resolved because your code doesn’t have access to the platform-specific version:

```kotlin
// Example library isn’t used

// MODULE: common
fun whichFun(x: Any) = println("common function") 

fun main(){
    whichFun(2) 
    // common function
}

// MODULE: JVM
fun whichFun(x: Int) = println("platform function")
```

Similar to multiplatform libraries, since the `commonTest` module is in a separate source set, it also still has access 
to platform-specific code. Therefore, the resolution of calls to functions in the `commonTest` module has the same behavior
as in the old compilation scheme.

In the future, these remaining cases will be more consistent with the new compilation scheme.

#### Different visibility levels of expected and actual declarations

Before Kotlin %kotlinEapVersion%, if you used [expected and actual declarations](multiplatform-expect-actual.md) in your
Kotlin Multiplatform project, they had to have the same [visibility level](visibility-modifiers.md). 
Kotlin %kotlinEapVersion% supports different visibility levels **only** if the actual declaration is _less_ strict than 
the expected declaration. For example:

```kotlin
expect internal class Attribute // Visibility is internal
actual class Attribute          // Visibility is public by default,
                                // which is less strict
```

If you are using a [type alias](type-aliases.md) in your actual declaration, the visibility of the type **must** be less
strict. Any visibility modifiers for `actual typealias` are ignored. For example:

```kotlin
expect internal class Attribute                 // Visibility is internal
internal actual typealias Attribute = Expanded  // The internal visibility 
                                                // modifier is ignored
class Expanded                                  // Visibility is public by default,
                                                // which is less strict
```

### Compiler plugins support

Currently, the Kotlin K2 compiler supports the following Kotlin compiler plugins:

* [`all-open`](all-open-plugin.md)
* [AtomicFU](https://github.com/Kotlin/kotlinx-atomicfu)
* [`jvm-abi-gen`](https://github.com/JetBrains/kotlin/tree/master/plugins/jvm-abi-gen)
* [`js-plain-objects`](https://github.com/JetBrains/kotlin/tree/master/plugins/js-plain-objects)
* [kapt](whatsnew1920.md#preview-kapt-compiler-plugin-with-k2)
* [Lombok](lombok.md)
* [`no-arg`](no-arg-plugin.md)
* [Parcelize](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.parcelize)
* [SAM with receiver](sam-with-receiver-plugin.md)
* [serialization](serialization.md)

In addition, the Kotlin K2 compiler supports:
* the [Jetpack Compose](https://developer.android.com/jetpack/compose) 1.5.0 compiler plugin and later versions.
* the [Kotlin Symbol Processing (KSP) plugin](ksp-overview.md)
since [KSP2](https://android-developers.googleblog.com/2023/12/ksp2-preview-kotlin-k2-standalone.html).

> If you use any additional compiler plugins, check their documentation to see if they are compatible with K2.
>
{type="tip"}

### How to enable the Kotlin K2 compiler

Starting with Kotlin 2.0.0-Beta1, the Kotlin K2 compiler is enabled by default.
No additional actions are required.

### Try the Kotlin K2 compiler in Kotlin Playground

Kotlin Playground supports the 2.0.0-Beta4 release. [Check it out!](https://pl.kotl.in/czuoQprce)

### Support in IntelliJ IDEA

> The K2 Kotlin mode is in Alpha. The performance and stability of code highlighting and code completion have been significantly improved,
> but not all IDE features are supported yet.
>
{type="warning"}

IntelliJ IDEA can use the new K2 compiler to analyze your code with its K2 Kotlin mode from 
[IntelliJ IDEA 2024.1 EAP 1](https://blog.jetbrains.com/idea/2024/01/intellij-idea-2024-1/#intellij-idea%\E2%80%99s-k2-kotlin-mode-now-in-alpha).

We are actively collecting feedback about K2 Kotlin mode.
Please share your thoughts in our [public Slack channel](https://kotlinlang.slack.com/archives/C0B8H786P)!

### Leave your feedback on the new K2 compiler

We would appreciate any feedback you may have!

* Provide your feedback directly to K2 developers on Kotlin
  Slack – [get an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up?_gl=1*ju6cbn*_ga*MTA3MTk5NDkzMC4xNjQ2MDY3MDU4*_ga_9J976DJZ68*MTY1ODMzNzA3OS4xMDAuMS4xNjU4MzQwODEwLjYw)
  and join the [#k2-early-adopters](https://kotlinlang.slack.com/archives/C03PK0PE257) channel.
* Report any problems you face with the new K2 compiler
  in [our issue tracker](https://kotl.in/issue).
* [Enable the Send usage statistics option](https://www.jetbrains.com/help/idea/settings-usage-statistics.html) to
  allow JetBrains to collect anonymous data about K2 usage.

## Gradle improvements

Kotlin %kotlinEapVersion% is fully compatible with Gradle 6.8.3 through 8.4.
You can also use Gradle versions up to the latest Gradle release, but if you do, 
keep in mind that you might encounter deprecation warnings or some new Gradle features might not work.

This version brings the following changes:
* [Improved Gradle dependency handling for CInteropProcess in Kotlin/Native](#improved-gradle-dependency-handling-for-cinteropprocess-in-kotlin-native)
* [Visibility changes in Gradle](#visibility-changes-in-gradle)
* [New directory for Kotlin data in Gradle projects](#new-directory-for-kotlin-data-in-gradle-projects)
* [Kotlin/Native compiler downloaded when needed](#kotlin-native-compiler-downloaded-when-needed)

### Improved Gradle dependency handling for CInteropProcess in Kotlin/Native

In this release, we enhanced the handling of the `defFile` property to ensure better Gradle task dependency management in 
Kotlin/Native projects. 

Before this update, Gradle builds could fail if the `defFile` property was designated as an output 
of another task that hadn't been executed yet. The workaround for this issue was to add a dependency on this task:

```kotlin
kotlin {
    macosArm64("native") {
        compilations.getByName("main") {
            cinterops {
                val cinterop by creating {
                    defFileProperty.set(createDefFileTask.flatMap { it.defFile.asFile })
                    project.tasks.named(interopProcessingTaskName).configure {
                        dependsOn(createDefFileTask)
                    }
                }
            }
        }
    }
}
```

To fix this, there is a new `RegularFileProperty` called `definitionFile`. Now, Gradle lazily verifies the presence of 
the `definitionFile` property after the connected task has run later in the build process. This new approach eliminates 
the need for additional dependencies.

The `CInteropProcess` task and the `CInteropSettings` class use the `definitionFile` property instead of `defFile` and 
`defFileProperty`:

<tabs group ="build-script">

<tab id="kotlin" title="Kotlin" group-key="kotlin">

```kotlin
kotlin {
    macosArm64("native") {
        compilations.getByName("main") {
            cinterops {
                val cinterop by creating {
                    definitionFile.set(project.file("def-file.def"))
                }
            }
        }
    }
}
```

</tab>

<tab id="groovy" title="Groovy" group-key="groovy">

```groovy
kotlin {
    macosArm64("native") {
        compilations.main {
            cinterops {
                cinterop {
                    definitionFile.set(project.file("def-file.def"))
                }
            }
        }
    }
}
```

</tab>

</tabs>

> `defFile` and `defFileProperty` parameters are now deprecated.
>
{type="warning"}

### Visibility changes in Gradle

> This change impacts only Kotlin DSL users.
> 
{type="note"}

In Kotlin %kotlinEapVersion%, we've modified the Kotlin Gradle Plugin for better control and safety in your build scripts.
Previously, certain Kotlin DSL functions and properties intended for a specific DSL context would inadvertently leak to 
other DSL contexts. This leakage could lead to the use of incorrect compiler options, settings being applied multiple times,
and other misconfigurations:

```kotlin
kotlin {
    // Target DSL couldn't access methods and properties defined in the
    // kotlin{} extension DSL
    jvm {
        // Compilation DSL couldn't access methods and properties defined
        // in the kotlin{} extension DSL and Kotlin jvm{} target DSL
        compilations.configureEach {
            // Compilation task DSLs couldn't access methods and 
            // properties defined in the kotlin{} extension, Kotlin jvm{}
            // target or Kotlin compilation DSL
            compileTaskProvider.configure {
                // For example:
                explicitApi()
                // ERROR as it is defined in the kotlin{} extension DSL
                mavenPublication {}
                // ERROR as it is defined in the Kotlin jvm{} target DSL
                defaultSourceSet {}
                // ERROR as it is defined in the Kotlin compilation DSL
            }
        }
    }
}
```

To fix this issue, we've added the `@KotlinGradlePluginDsl` annotation,
preventing the exposure of the Kotlin Gradle plugin DSL functions and properties to the levels where they are not intended
to be available. The following levels are separated from each other:

* Kotlin extension
* Kotlin target
* Kotlin compilation
* Kotlin compilation task

If your build script is configured incorrectly, you should see compiler warnings with suggestions on how to fix it. For example:

```kotlin
kotlin {
    jvm {
        sourceSets.getByName("jvmMain").dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
        }
    }
}
```

In this case, the warning message for `sourceSets` is:

```kotlin
[DEPRECATION] 'sourceSets: NamedDomainObjectContainer<KotlinSourceSet>' is deprecated.Accessing 'sourceSets' container on the Kotlin target level DSL is deprecated . Consider configuring 'sourceSets' on the Kotlin extension level .
```

We would appreciate your feedback on this change! Share your comments directly to Kotlin developers in our [#eap Slack channel](https://kotlinlang.slack.com/archives/C0KLZSCHF).
[Get a Slack invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).

### New directory for Kotlin data in Gradle projects

> With this change, you may need to add the `.kotlin` directory to your project's `.gitignore` file.
>
{type="warning"}

In Kotlin 1.8.20, the Kotlin Gradle plugin started to store its data in the Gradle project cache directory: `<project-root-directory>/.gradle/kotlin`.
However, the `.gradle` directory is reserved for Gradle only, and as a result it's not future-proof. To solve this, since 
Kotlin 2.0.0-Beta2 we store Kotlin data in your `<project-root-directory>/.kotlin` by default. We will continue to store
some data in `.gradle/kotlin` directory for backward compatibility.

There are new Gradle properties so that you can configure a directory of your choice and more:

| Gradle property                                     | Description                                                                                                      |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `kotlin.project.persistent.dir`                     | Configures the location where your project-level data is stored. Default: `<project-root-directory>/.kotlin`     |
| `kotlin.project.persistent.dir.gradle.disableWrite` | A boolean value that controls whether writing Kotlin data to the `.gradle` directory is disabled. Default: false |

Add these properties to the `gradle.properties` file in your projects for them to take effect.

### Kotlin/Native compiler downloaded when needed

Before Kotlin 2.0.0-Beta4, if you had a [Kotlin/Native target](native-target-support.md) configured in the Gradle build script
of your multiplatform project, then Gradle would always download the Kotlin/Native compiler in the 
[configuration phase](https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:configuration).
Even if there was no task to compile code for a Kotlin/Native target due to run in the [execution phase](https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:execution).
Downloading the Kotlin/Native compiler in this way was particularly inefficient for users who only wanted to check the 
JVM or JavaScript code in their projects. For example, to perform tests or checks with their Kotlin project as part of a CI process.

In Kotlin 2.0.0-Beta4, we changed this behavior in the Kotlin Gradle plugin so that the Kotlin/Native 
compiler is downloaded in the [execution phase](https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:execution) 
**only** when a compilation is requested for a Kotlin/Native target.

## Kotlin/JS

This version brings the following changes:
* [Support for type-safe plain JavaScript objects](#support-for-type-safe-plain-javascript-objects)
* [Support for npm package manager](#support-for-npm-package-manager)
 
### Support for type-safe plain JavaScript objects

> The `js-plain-objects` plugin is [Experimental](components-stability.md#stability-levels-explained).
> It may be dropped or changed at any time. The `js-plain-objects` plugin **only** supports the K2 compiler.
>
{type="warning"}

To make it easier to work with JavaScript APIs, in Kotlin 2.0.0-Beta4, we provide a new plugin: 
[`js-plain-objects`](https://github.com/JetBrains/kotlin/tree/master/plugins/js-plain-objects),
that you can use to create type-safe plain JavaScript objects.
The plugin checks your code for any [external interfaces](wasm-js-interop.md#external-interfaces) 
that have a `@JsPlainObject` annotation and adds:

* an inline `invoke` operator function inside the companion object that you can use as a constructor.
* a `.copy()` function that you can use to create a copy of your object while adjusting some of its properties.

For example:

```kotlin
import kotlinx.js.JsPlainObject

@JsPlainObject
external interface User {
    var name: String
    val age: Int
    val email: String? 
}

fun main() {
    // Creates a JavaScript object
    val user = User(name = "Name", age = 10)
    // Copies the object and adds an email
    val copy = user.copy(age = 11, email = "some@user.com")

    println(JSON.stringify(user)) 
    // { "name": "Name", "age": 10 }
    println(JSON.stringify(copy)) 
    // { "name": "Name", "age": 11, "email": "some@user.com" }
}
```

Any JavaScript objects created with this approach are safer because instead of only seeing errors at runtime, 
you can see them at compile time or even highlighted by your IDE.

Consider this example that uses a `fetch()` function to interact with a JavaScript API using external interfaces 
to describe the shape of the JavaScript objects:

```kotlin
import kotlinx.js.JsPlainObject

@JsPlainObject
external interface FetchOptions {
    val body: String?
    val method: String
}

// A wrapper for Window.fetch
suspend fun fetch(url: String, options: FetchOptions? = null) = TODO("Add your custom behavior here")

// A compile-time error is triggered as metod is not recognized
// as method
fetch("https://google.com", options = FetchOptions(metod = "POST")) 
// A compile-time error is triggered as method is required
fetch("https://google.com", options = FetchOptions(body = "SOME STRING")) 
```

In comparison, if you use the `js()` function instead to create your JavaScript objects,
errors are only found at runtime or aren't triggered at all:

```kotlin
suspend fun fetch(url: String, options: FetchOptions? = null) = TODO("Add your custom behavior here")

// No error is triggered. As metod is not recognized, the wrong method 
// (GET) is used.
fetch("https://google.com", options = js("{ metod: 'POST' }")) 

// By default, the GET method is used. A runtime error is triggered as 
// body shouldn't be present.
fetch("https://google.com", options = js("{ body: 'SOME STRING' }")) 
// TypeError: Window.fetch: HEAD or GET Request cannot have a body
```

To use the `js-plain-objects` plugin, add the following to your `build.gradle.kts` file:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
plugins {
    kotlin("plugin.js-plain-objects") version "%kotlinEapVersion%"
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
plugins {
    id "org.jetbrains.kotlin.plugin.js-plain-objects" version "%kotlinEapVersion%"
}
```

</tab>
</tabs>

### Support for npm package manager

Previously, it was only possible for the Kotlin Multiplatform Gradle plugin to use [Yarn](https://yarnpkg.com/lang/en/) 
as a package manager to download and install npm dependencies. From Kotlin %kotlinEapVersion%, you can use [npm](https://www.npmjs.com/) 
as your package manager instead. Using npm as a package manager means that you have one less tool to manage during your setup. 

For backward compatibility, Yarn is still the default package manager. To use npm as your package manager, 
in your `gradle.properties` file, set the following property:

```kotlin
kotlin.js.yarn=false
```

## What to expect from upcoming Kotlin EAP releases

The upcoming 2.0.0-Beta5 release will increase the stability of the K2 compiler.
If you are currently using K2 in your project, 
we encourage you to stay updated on Kotlin releases and experiment with the updated K2 compiler. 
[Share your feedback on using Kotlin K2](#leave-your-feedback-on-the-new-k2-compiler).

> Despite the fact that the Kotlin K2 compiler is in Beta for all targets, it is not recommended to use it in production.
> This is due to K2 binaries poisoning: we need to ensure that code compiled with different versions of Kotlin maintains binary compatibility with K2 binaries.
> 
> You can start using the K2 compiler in production starting from **Kotlin 2.0.0-RC1**.
>
{type="warning"}

## How to update to Kotlin %kotlinEapVersion%

Starting from IntelliJ IDEA 2023.3 and Android Studio Iguana (2023.2.1) Canary 15, the Kotlin plugin is distributed as a 
bundled plugin included in your IDE. This means that you can't install the plugin from JetBrains Marketplace anymore. 
The bundled plugin supports upcoming Kotlin EAP releases.

To update to the new Kotlin EAP version, [change the Kotlin version](configure-build-for-eap.md) to %kotlinEapVersion% in your build scripts.