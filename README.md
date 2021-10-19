kSar
====

[![Build Status](https://travis-ci.com/vlsi/ksar.svg?branch=master)](https://app.travis-ci.com/github/vlsi/ksar)

Quick Start
-----------

Prerequisite:

- Java 8 or later

Download a pre-built jar from [GitHub releases page](https://github.com/vlsi/ksar/releases).

```
$ java -jar ksar-5.2.3-all.jar
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

Changelog
---------

v5.2.3 -- 30 March 2017
* support more date/time formats 
    * "MM/DD/YY 23:59:59"
    * "MM/DD/YYYY 12:59:59 AM|PM"
    * "DD/MM/YYYY 12:59:59 AM|PM"

* fix align the page (#61)
* fix issue (#64) - returning wrong unit for small values


v5.2.2 -- 3 November 2016
* Update IO charts: byte->blocks/s, remove util, remove svctm
* Linux: add new DateTime format "YYYY-MM-DD 12:59:59 AM" (#52)
* Linux: charts memory - PAGE / PAGING / SWAP / KMEM (#53)
* Linux: KMem stat/graph definition (#48)

v5.2.1 -- 6 August 2016
* Sort elements in human-friendly order: cpu1 -> cpu2 -> ... -> cpu10 -> ...
* Display load graph for sysstat 9.1.7 and higher

v5.2.0 -- 1 May 2016
* Migrated build to Gradle
