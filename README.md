Curve Detect
-------------

Simple cross-platform way to convert scan or picture of your graph to digital
data.

Curve Detect can be built to both fat-jar (which includes JavaFX libraries) and
native image by using GraalVM.

Curve Detect uses Java 8 so can be run on pretty old hardware.


Compiling
--------------------

### Fat-jar

While in root directory run
~~~
gradlew jar
~~~
Resulting jar file will be created inside `build/libs`


### Native image

##### Windows

1. Install Visual Studio 2019 (Community Edition is OK). C++ development bundle
   should be OK.
2. Install [GraalVM CE](https://www.graalvm.org/downloads/) somewhere on your
PC, for example, at `C:\graalvm`. Tested with GraalVM CE 21.0
3. Setup environment variable `GRAALVM_HOME` that points to your installation,
for example `C:\graalvm`
4. Run x64 Native Tools Command Prompt for VS 2019 and navigate to project dir
5. Run `gradlew nativeBuild`
6. Build will take several minutes to finish and will require ~6GB of RAM.

Resulting .exe file will be inside `build/client/x86_64-windows`


Examples
--------

In `examples` directory you can find result of processing two images of the same
[curve](https://www.wolframalpha.com/input/?i=plot+10%2Bx%5E2%2B5*x*sin%28x%29+from+0+to+10):
`y=10+x^2+5*x*sin(x); x=0:10`.
First sample is the screenshot of this curve, second sample is photo of curve,
taken from ~40cm distance (so camera doesn't focus on pixels and perspective
distortion is low).


License
-------

This project is licensed under MIT license (see LICENSE.txt)
