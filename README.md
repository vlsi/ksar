kSar
====

[![Build Status](https://travis-ci.org/vlsi/ksar.svg?branch=master)](https://travis-ci.org/vlsi/ksar)

Quick Start
-----------

Prerequisite:

- Java 8 or later

Download a pre-built jar from [GitHub releases page](https://github.com/vlsi/ksar/releases).

```
$ java -jar ksar-5.2.1-all.jar
```

Building from source
--------------------

Prerequisite:

- JDK 8 or later

```
$ ./gradlew shadowJar
$ java -jar build/libs/ksar-5.2.2-SNAPSHOT-all.jar
```

Changelog
---------

HEAD

v5.2.1 -- 6 August 2016
* Sort elements in human-friendly order: cpu1 -> cpu2 -> ... -> cpu10 -> ...
* Display load graph for sysstat 9.1.7 and higher

v5.2.0 -- 1 May 2016
* Migrated build to Gradle
