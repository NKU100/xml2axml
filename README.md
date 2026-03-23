# xml2axml

Encode xml to axml OR decode xml from axml

## Usage

* encode  
```
java -jar xml2axml.jar e [readable.xml] [binary-out.xml]
```
* decode  
```
java -jar xml2axml.jar d [binary.xml] [readable-out.xml]
```

## Building from Source

### Prerequisites

| Component | Version | Location |
|-----------|---------|----------|
| JDK | TencentKona 17 | `external/TencentKona-17.0.16.b1/` |
| Maven | 3.9.14 | `external/apache-maven-3.9.14/` |

Both are bundled in the parent project's `external/` directory.

### Why is `libs/android.jar` local?

The project needs `android.R$attr` class for resource ID lookup. On Maven
Central, `com.google.android:android` only goes up to version **4.1.1.4**
(Android 4.1.1, circa 2015). Newer Android API levels (34, 35, 36…) are
**not published** to any public Maven repository. Therefore we keep
`libs/android.jar` (API 36) locally and use `maven-install-plugin` to install
it into the local Maven repo during the `clean` phase.

### Build Steps

**一键构建（双击即可运行）：**

```
external\build_xml2axml.cmd              完整构建（clean + package）
external\build_xml2axml.cmd skip         跳过 clean（快速重编译）
```

**手动步骤：**

```bat
set JAVA_HOME=external\TencentKona-17.0.16.b1
set MVN=external\apache-maven-3.9.14\bin\mvn.cmd

cd external\xml2axml

REM 首次构建：安装本地 android.jar 到 Maven 仓库 + 编译打包
call %MVN% clean package

REM 后续构建（跳过 android.jar 安装）：
call %MVN% package
```

The output fat jar is at `target/xml2axml-1.1.0-SNAPSHOT.jar`.  
The build script automatically copies it to `external/xml2axml.jar` for use with the main project.

### Build Notes

- **`mvn clean`** triggers `maven-install-plugin` which installs
  `libs/android.jar` into your local `~/.m2/repository` as
  `com.google.android:android:36.0.0`. This only needs to be done once
  (or when `libs/android.jar` is updated).

- **`maven-shade-plugin`** creates an executable fat jar with all
  dependencies bundled. It only includes `android/R.class` and
  `android/R$*.class` from `android.jar` — the rest is excluded to avoid
  conflicts with project stub classes (e.g. `android.content.Context`).

## Credits

Based on [l741589/xml2axml](https://github.com/l741589/xml2axml)
