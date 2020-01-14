# sipka.cmdline

![Build status](https://img.shields.io/azure-devops/build/sipkab/de839a76-f269-4f5c-86d5-dcaab98d57f1/3/master) [![Latest version](https://mirror.nest.saker.build/badges/sipka.cmdline/version.svg)](https://nest.saker.build/package/sipka.cmdline "sipka.cmdline | saker.nest")

Annotation processor for generating command line argument parser and invoker. The project implements an annotation processor that generates Java classes which parse the input command line arguments based on the rules specified by the annotated source elements.

## Features

* Compile time class generation
	* Annotation with source retention policy
	* No reflection *at all*
* Help generation
* Customizable argument parsing
* Invokes the command specified by the arguments
* Supports command files

## Build instructions

The library uses the [saker.build system](https://saker.build) for building. Use the following command to build the project:

```
java -jar path/to/saker.build.jar -bd build compile saker.build
```

## Usage

Usage with the [saker.build system](https://saker.build):

```
# set the version of the library to use
global(VERSION_sipka.cmdline) = "0.8.0"

saker.java.compile(
    # add the annotations and runtime to the compilation classpath
    ClassPath: [
        saker.java.classpath.bundle("sipka.cmdline-api-v{ global(VERSION_sipka.cmdline) }"),
        saker.java.classpath.bundle("sipka.cmdline-runtime-v{ global(VERSION_sipka.cmdline) }"),
    ],
    # load and add the annotation processor
    AnnotationProcessors: {
        Processor: saker.java.processor.bundle(
            Bundle: "sipka.cmdline-processor-v{ global(VERSION_sipka.cmdline) }",
            Class: sipka.cmdline.processor.CommandLineProcessor,
            Aggregating: false,
        ),
        SuppressWarnings: [ 
            LastRoundGeneration,
        ],
    },
)
# make sure to add the runtime classes to the created JAR if you package the created app
saker.jar.create(
    Includes: [
        {
            Archive: nest.bundle.download("sipka.cmdline-runtime-v{ global(VERSION_sipka.cmdline) }")[BundlePaths][0],
            Resources: sipka/cmdline/runtime/**/*.class,
        },
    ],
)
```

## Documentation

Work in progress. See [related issue](https://github.com/Sipkab/sipka.cmdline/issues/1).

## License

Different parts of the source code for the project is licensed under different terms. The API and runtime is licensed under *Apache License 2.0* ( [`Apache-2.0`](https://spdx.org/licenses/Apache-2.0.html)), while the annotation processor related codes are licensed under *GNU General Public License v3.0 only* ([`GPL-3.0-only`](https://spdx.org/licenses/GPL-3.0-only.html)). See the LICENSE files under the `api`, 'runtime' and `processor` directories.

This is in order to allow more convenient usage of the library. 

Official releases of the project (and parts of it) may be licensed under different terms. See the particular releases for more information.
