<p align="center">
    <img src="https://user-images.githubusercontent.com/35042166/222891304-7d72bffa-ef16-47b6-9b35-d01a04b5537a.png" alt="DemiGlace logo"><br/>
    Java Parsing Tool<br/>
</p>

## DemiGlace

DemiGlace is a tool for matching Java method signatures in bytecode form (fully qualified name + method descriptor) with the locations of the actual methods and method calls in the source code. It is powered by <a href="https://javaparser.org">JavaParser</a>.

![image](https://user-images.githubusercontent.com/35042166/222883529-4c0503bc-8a16-4e16-822d-8a86be43cf63.png)

## Features

When providing source code and a call tree XML (JProfiler), DemiGlace can generate the method-level execution graph with exact in-source-code positions of each method call and its respective method definition.

Simple source code and call tree XML example:
```java
// MyClass.java
...
public void doMath() {
  double dblResult = Math.pow(4.2, 3);
  System.out.println(dblResult);
}
...
```

```xml
<!--call tree-->
<node class="main.java.components.MyClass" methodName="doMath" methodSignature="()V">
    <node class="java.lang.Math" methodName="pow" methodSignature="(DD)D" />
    <node class="java.io.PrintStream" methodName="println" methodSignature="(Ljava/lang/String;)V" />
</node>
```

## Motivation
Why was this tool created? As a research assistant, I once had the task to match nodes of JProfiler call trees to their corresponding method definitions and calls.

JProfiler is able to record the line number of method calls - if configured to do so. This would have made my task comparatively easy. Unfortunately, all measurements (hundreds of hours of profiling) were already done before realizing that the necessary option for recording line numbers was turned off.

## Development

This project is using the <a href="https://docs.gradle.org/current/userguide/application_plugin.html">application plugin</a> for Gradle.

### Run

In the root of this project, run:

```
./gradlew run
```

### Other tasks

Thanks to the application plugin, distribution is also easy.

Use the `installDist` task to generate a directory structure including the built JAR, all of the JARs that it depends on, and a startup script that pulls it all together into a program you can run.

The `distZip` and `distTar` tasks that create archives containing a complete application distribution (startup scripts and JARs).

```
./gradlew <task-name>
```

For more details and tasks, checkout the official application plugin <a href="https://docs.gradle.org/current/userguide/application_plugin.html">documentation</a>.