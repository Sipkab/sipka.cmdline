# saker.cmdline

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

TBD TODO
