kSar
====

[![Build Status](https://github.com/vlsi/ksar/workflows/Test/badge.svg?branch=master)](https://github.com/vlsi/ksar/actions?query=branch%3Amaster)

Quick Start
-----------

ksar is a sar graphing tool that can graph for now linux, maOS and solaris sar output. sar statistics graph can be output to a pdf file.
This is a fork of http://sourceforge.net/projects/ksar/

Prerequisite:

- Java 8 or later

Download a pre-built jar from [GitHub releases page](https://github.com/vlsi/ksar/releases).

```
$ java -jar ksar-5.2.4-all.jar
```

Building from source
--------------------

Prerequisite:

- JDK 8 or later

The following command would build and launch kSar from sources:

```
$ ./gradlew runShadow
```

or

```
$ ./gradlew shadowJar
$ java -jar build/libs/ksar-5.2.4-SNAPSHOT-all.jar
```
