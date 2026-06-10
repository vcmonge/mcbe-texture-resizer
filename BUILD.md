# Build Guide - Iron Fat JAR

This guide explains how to build Iron as a self-contained executable JAR on
Windows. It is intended for someone who has just cloned the repository and wants
to generate the final artifact:

```text
target/iron-1.0.jar
```

The original NetBeans/Ant project is preserved. The fat JAR is generated with
Maven Shade using the `pom.xml` included in the repository root.

## 1. Requirements

You need:
- Java `25`.
- Apache Maven.

The final JAR is run with:

```powershell
java -jar target\iron-1.0.jar
```

## 2. Clone the repository

Open PowerShell in the folder where you want to download the project and clone
the repository:

```powershell
git clone <REPOSITORY_URL>
cd iron
```

If you already have the repository, just open PowerShell at the project root.
The root is the folder containing these files:

```text
BUILD.md
README.md
pom.xml
src\
```

You can confirm this by running:

```powershell
Get-ChildItem
```

## 3. Verify Java 25

Run:

```powershell
java -version
javac -version
```

The output should indicate Java `25`. For example:

```text
java version "25.0.x"
javac 25.0.x
```

If `java` or `javac` are not found, install a JDK 25 and open a new terminal
before continuing. Having only a JRE is not enough: Maven needs `javac` to
compile.

## 4. Install or verify Maven

First check whether Maven is already installed:

```powershell
mvn -version
```

If it works, continue with step 5.

If it fails, install Maven manually from Apache using the following complete
PowerShell block. The script:

- Downloads Maven `3.9.16`.
- Downloads the official SHA512 checksum.
- Verifies that the downloaded zip is not corrupt.
- Installs it in the current user's profile using `%LOCALAPPDATA%`.
- Configures `MAVEN_HOME`.
- Adds Maven to the user's `Path` and to the current terminal's `Path`.

```powershell
$version = '3.9.16'
$base = Join-Path $env:LOCALAPPDATA 'Programs\Apache'
$zip = Join-Path $env:TEMP "apache-maven-$version-bin.zip"
$sha = Join-Path $env:TEMP "apache-maven-$version-bin.zip.sha512"
$downloadUrl = "https://dlcdn.apache.org/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip"
$shaUrl = "https://downloads.apache.org/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip.sha512"

Invoke-WebRequest `
  -Uri $downloadUrl `
  -OutFile $zip

Invoke-WebRequest `
  -Uri $shaUrl `
  -OutFile $sha

$expected = (Get-Content $sha -Raw).Split(' ', [System.StringSplitOptions]::RemoveEmptyEntries)[0].Trim().ToUpperInvariant()
$actual = (Get-FileHash $zip -Algorithm SHA512).Hash.ToUpperInvariant()
if ($actual -ne $expected) { throw 'Maven SHA512 mismatch' }

New-Item -ItemType Directory -Force -Path $base | Out-Null
Expand-Archive -Path $zip -DestinationPath $base -Force

$mavenHome = Join-Path $base "apache-maven-$version"
[Environment]::SetEnvironmentVariable('MAVEN_HOME', $mavenHome, 'User')
$env:MAVEN_HOME = $mavenHome

$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$mavenBin = Join-Path $mavenHome 'bin'
if (($userPath -split ';') -notcontains $mavenBin) {
    [Environment]::SetEnvironmentVariable('Path', ($userPath.TrimEnd(';') + ';' + $mavenBin), 'User')
}
if (($env:Path -split ';') -notcontains $mavenBin) {
    $env:Path = $env:Path.TrimEnd(';') + ';' + $mavenBin
}
```

Verify Maven:

```powershell
mvn -version
```

If `mvn` still cannot be resolved by name in the same terminal, use the direct
path:

```powershell
& "$env:MAVEN_HOME\bin\mvn.cmd" -version
```

When you open a new terminal, `mvn` should work directly because the script
saves `MAVEN_HOME` and updates the user's `Path`.

## 5. Build the fat JAR

From the repository root, run:

```powershell
mvn clean package
```

If you just installed Maven manually and `mvn` still cannot be resolved by
name, use:

```powershell
& "$env:MAVEN_HOME\bin\mvn.cmd" clean package
```

The first build may take longer because Maven downloads dependencies from
Maven Central.

When it finishes successfully, you should see:

```text
BUILD SUCCESS
```

The final artifact is located at:

```text
target/iron-1.0.jar
```

## 6. Run the application

Run the JAR from the repository root:

```powershell
java -jar target\iron-1.0.jar
```

The Iron window should open without passing `--module-path` and without
installing the JavaFX SDK separately.

On Java 25 you may see native access warnings because JavaFX is loaded from a
fat JAR on the classpath. These do not prevent startup. To suppress them:

```powershell
java --enable-native-access=ALL-UNNAMED -jar target\iron-1.0.jar
```

## 7. Verify the generated JAR

Confirm that the file exists:

```powershell
Get-ChildItem target\iron-1.0.jar
```

Confirm that the manifest points to the correct launcher:

```powershell
jar xf target\iron-1.0.jar META-INF/MANIFEST.MF
Get-Content META-INF\MANIFEST.MF
```

It should include:

```text
Main-Class: view.IronLauncher
```

Confirm that JavaFX is included inside the fat JAR:

```powershell
jar tf target\iron-1.0.jar | Select-String "javafx/application/Application.class|javafx/fxml/FXMLLoader.class|javafx/embed/swing/SwingFXUtils.class"
```

The output should include those classes. This confirms that the JAR includes
JavaFX and the `javafx-swing` module required by `SwingFXUtils`.

## 8. Why `IronLauncher` exists

The actual main class of the application, `view.Iron`, extends
`javafx.application.Application`. In Java 11 and above, if the Java launcher
detects that the main class extends `Application`, it expects to find JavaFX
on the module path.

In this project JavaFX is bundled inside the fat JAR, loaded from the
classpath. That is why the manifest uses a plain class that does not extend
`Application`:

```text
Main-Class: view.IronLauncher
```

`view.IronLauncher` simply calls `Iron.main(args)`. This separation avoids the
following error when running the JAR:

```text
Error: JavaFX runtime components are missing, and are required to run this application
```

## 9. Included JavaFX dependencies

The `pom.xml` includes the following OpenJFX `25.0.3` modules:

- `javafx-base`
- `javafx-graphics`
- `javafx-controls`
- `javafx-fxml`
- `javafx-swing`

The dependencies use the `win` classifier, so the generated fat JAR is
Windows-specific. This classifier includes the native JavaFX binaries required
to run the application on Windows.

## 10. Troubleshooting

If `mvn` is not recognized:

```powershell
& "$env:MAVEN_HOME\bin\mvn.cmd" -version
```

If that works, open a new terminal and try `mvn -version` again.

If `javac` is not recognized, install a full JDK 25 and make sure its `bin`
folder is in your `Path`.

If the build fails while downloading dependencies, check your internet
connection and run again:

```powershell
mvn clean package
```

If `JavaFX runtime components are missing` appears when running, confirm that
the JAR manifest contains:

```text
Main-Class: view.IronLauncher
```

## Notes

- The Maven build does not move the source code to `src/main/java`; the
  `pom.xml` compiles directly from `src`.
- The existing NetBeans/Ant configuration is preserved for local development.
- The generated fat JAR is Windows-specific due to the `win` classifier of
  OpenJFX.
