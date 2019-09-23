
# JINN-Compiler

**JINN-Compiler** is an open-source compilation framework which is capable of mapping
program parts to ideal hardware configurations in the big.LITTLE architecture. The goal of performing such mapping is to optimize applications for specific resource usage, for example energy or time. This technique explicitly considers input sizes when deciding on which hardware configuration a program should run.

## Requirements

To compile and run JINN-C, you'll need:

 - Oracle openJDK 11
 - [Soot 3.2.0] [Soot9 branch] (https://github.com/Sable/soot)
    * *currently shipped with JINN-C from its github repository.*
 - GNU sed
 - Make
 - Bash
 - cpufreq-set
 - taskset
 - Python 2.7 and the following packages:
	 - pandas
	 - sklearn
	 - numpy
	 

## Installation

1 - Clone the repository:
```bash
git clone https://github.com/lac-dcc/JINN-C.git
```

2 - Access JINN-C dir and open the Makefile file:
```bash
cd JINN-C
vim Makefile
```
3 - Update the variable JAVADIR, setting it to the place where you installed the openJDK 11, for example:
*	JAVADIR=/usr/lib/jvm/java-11-openjdk-amd64/

You also have to update this variable inside the `JINN-FUNCTIONS.sh` script
`src/scripts/JINN-FUNCTIONS.sh`. You should not need, but you may update some other variables in that file, if you want.

4 - Run the make command for compiling JINN-C and moving  run scripts:
```console
make
```

After running this command you should have an executable script `jinn-c` placed inside the *build* dir.

```bash
file build/jinn-c 
build/jinn-c: Bourne-Again shell script, ASCII text executable
```

Now, you should be able to run **jinn-c** on your big.LITTLE device.

## First run

JINN-C comes with some examples/tests you can use to test the tool and check how the annotation system works. They are placed in the directory `src/tests/`. You can compile them with the command:
```bash
make tests
```
After compiling them, you can run the tests with the following command line:
```bash
./build/jinn-c build/tests/InputDependentTest.class src/tests/inputs/input1.txt
```

You should have an output similar to this:
```bash
- JINN-C Adaptive Compiler -

Running Stage 1 - Instrummenting input app with profiling information
Soot started on Mon Sep 21 10:10:22 BRT 2019

================ Initializing JINN-C IR Parser ================

jinn.ArgumentsAnalyzer.internalTransform - 
Found Adaptive Method: <InputDependentTest: void adaptive(int,int,int)>
    -> method does not contain any special tag (@Input @HiddenInput).
        JINN-C will try to use all parameters

Soot finished on Mon Sep 21 10:10:25 BRT 2019
Soot has run for 0 min. 3 sec.

Running Stage 2 - Executing instrummented app with target configurations and inputs
Running Application (InputDependentTest) with 0x09 0x60 0x69 0xf0 0x0f 0xff
    Setting big cluster frequency to 1.8GHz
        Setting hardware configuration to 0x09
        Setting hardware configuration to 0x60
        Setting hardware configuration to 0x69
        Setting hardware configuration to 0xf0
        Setting hardware configuration to 0x0f
        Setting hardware configuration to 0xff
    Setting big cluster frequency to 1.6GHz
        Setting hardware configuration to 0x09
        Setting hardware configuration to 0x60
        Setting hardware configuration to 0x69
        Setting hardware configuration to 0xf0
        Setting hardware configuration to 0x0f
        Setting hardware configuration to 0xff

Running Stage 3 - Parsing runtime logs
1.6GHz-0x0f is the best configuration for Input = {-1578145061,1,2,3}:  0.20486 / 0.221378
1.8GHz-0xff is the best configuration for Input = {-1578145061,10,11,12}:  0.208466 / 0.208466
1.8GHz-0xff is the best configuration for Input = {-1578145061,123,432,65}:  0.211874 / 0.211874
1.8GHz-0xf0 is the best configuration for Input = {-1578145061,7,8,9}:  0.20603 / 0.21835
1.8GHz-0x69 is the best configuration for Input = {-1578145061,4,5,6}:  0.209758 / 0.221093

Running Stage 4 - Performing Logistic Regression Analysis
Number of observations :: 5
Number of columns :: 5
Headers :: [0 1 2 3 4]
Classes: 
['1.6GHz-0x0f' '1.8GHz-0x69' '1.8GHz-0xf0' '1.8GHz-0xff']
Matrix - Theta
[[-0.14746 -0.13316 -0.11974  0.40209]
 [-0.08074 -0.11202 -0.1442   0.33828]
 [-0.24229 -0.16288 -0.08432  0.49182]]
Intercept - Theta_0
[-0.70265407 -0.69704994 -0.69246162 -0.16670177]
Classes: {"1.6GHz-0x0f","1.8GHz-0x69","1.8GHz-0xf0","1.8GHz-0xff"};
Interception with plane: {-0.702654066944073,-0.6970499368530471,-0.692461622175571,-0.166701772313119};
{{-0.14745666234956298,-0.13315614550443142,-0.11973870580984824,0.4020888927234696},
{-0.08074399474061239,-0.11202308080442078,-0.14420046510597045,0.3382848986044038},
{-0.24228909058068698,-0.16287752817305337,-0.08432401907666205,0.4918220902576409}};
Normalization mean: {29.0,91.6,19.0};
Normalization scale: {47.095647357266465,170.22643742967776,23.194827009486403};

Running Stage 5 - Specializing Input-guided Predictor
  done

Running Stage 6 - Instrummenting original application, adding call to predictor
Soot started on Mon Sep 21 10:10:39 BRT 2019
Soot finished on Mon Sep 21 10:10:42 BRT 2019
Soot has run for 0 min. 3 sec.

```

## Annotation System

JINN-C compilation technique is guided by a number of user-inserted code annotations. Currently, the tool works with the following annotations tags:

**@AdaptiveMethod**: marks a method as the target of multivariate regression.  Unless the **@Input** annotation is also used, every formal parameter of the method will be used as an independent variable of the linear regression. Global variables are not considered inputs in this case.

**@Input**: specifies which references or primitive values are independent variables in the regression. This annotation must be employed when Jinn-C’s  users know that some function arguments bear no effect onto the choice of  ideal configurations for the target method. Function parameters and global variables (whose scope includes the point where the target method is declared) can be marked as inputs. 


**@HiddenInput**: specifies extra information to be used as independent variables. These hidden inputs are mostly system variables, such as the number of threads; however, hidden inputs can also be global variables that are not directly used within a function, albeit they are accessed within methods called by said function. A method, chain of methods or any
expression can be used to obtain a reference to a hidden input. The names used in these expressions must be visible during compilation time, otherwise an error is thrown. 
* ***Note 1***: When calling a static method from the same class, use the class name + method, instead of just the method name. For example:
-- Annotation that needs information from static method of current class:
-- Instead of using @HiddenInput( expr=foo( ) ); use @HiddenInput( expr=ClassName.foo( ) )
* ***Note 2***: The same applies to dynamic calls. Instead of just using the method's name, please also include the *this* keyword. For example:
-- Instead of using @HiddenInput( expr=foo( ) ); use @HiddenInput( expr=this.foo( ) )

### Examples of code annotations (from src/tests):
```java
@AdaptiveMethod
private static void sample(String[] VAR1, double VAR2, Integer VAR3,
                           List<String> VAR4) { ... }
```

```java
@AdaptiveMethod
@HiddenInput(expr="Runtime.getRuntime().availableProcessors()")
private static void case5HiddenInput(int VAR1, double VAR2) { 
... 
}
```

```java
@AdaptiveMethod
@Input(param="VAR2")
@HiddenInput(expr="HiddenTest.getValue()")
private static void case7HiddenInputAndInput(int VAR1, double VAR2) { ... }
```

```java
// should consider both global num and parameter c
@AdaptiveMethod
@Input(param="num")
@Input(param="c")
public static void globalAdaptive (int c) { ... }
```


## License

GNU GPL3
