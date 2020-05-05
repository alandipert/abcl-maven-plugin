This is a Maven plugin for [Armed Bear Common
Lisp](https://common-lisp.net/project/armedbear/). Currently, only one
goal is provided: `abcl:repl`.

## Installing

This project is not yet deployed to a Maven repository. To install it locally:

    mvn install

## Getting started

See `example/pom.xml` for a minimum viable pom.xml. Copy it to a new project directory. Then, in the project directory, run:

    mvn abcl:repl

You should be presented with a REPL.

## License

Copyright Alan Dipert <alan@dipert.org>, provided under the terms of the MIT License.

Portions borrowed from [clojure-maven-plugin](https://github.com/talios/clojure-maven-plugin) by Mark Derricutt and licensed under the Eclipse Public License 1.0.

`AutoFlushingPumpStreamHandler` and `AutoFlushingStreamPumper` are based on `PumpStreamHandler` and `StreamPumper` from Apache Commons Exec and were [modified by Marty Pitt](https://stackoverflow.com/questions/7113007/trouble-providing-multiple-input-to-a-command-using-apache-commons-exec-and-extr/7531626#7531626). I think these files are still Apache licensed.
