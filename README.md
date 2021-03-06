<!-- @formatter:off  -->

# Javabyte

<div align="center">
  <a href="https://github.com/whilein/javabyte/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/whilein/javabyte">
  </a>

  <a href="https://discord.gg/ANEHruraCc">
    <img src="https://img.shields.io/discord/819859288049844224?logo=discord">
  </a>

  <a href="https://github.com/whilein/javabyte/issues">
    <img src="https://img.shields.io/github/issues/whilein/javabyte">
  </a>

  <a href="https://github.com/whilein/javabyte/pulls">
    <img src="https://img.shields.io/github/issues-pr/whilein/javabyte">
  </a>

  <a href="https://search.maven.org/artifact/io.github.whilein/javabyte">
    <img src="https://img.shields.io/maven-central/v/io.github.whilein/javabyte">
  </a>

  <a href="https://github.com/whilein/javabyte/actions">
    <img src="https://img.shields.io/github/workflow/status/whilein/javabyte/test">
  </a>
</div>

## About the project

Javabyte - Simple library that helps you to create new classes

## Add as dependency

<div>
  <a href="https://search.maven.org/artifact/io.github.whilein/javabyte">
    <img src="https://img.shields.io/maven-central/v/io.github.whilein/javabyte">
  </a>
</div>

### Maven
```xml
<dependencies>
    <dependency>
        <groupId>io.github.whilein</groupId>
        <artifactId>javabyte</artifactId>
        <version>1.1.0</version>
    </dependency>
</dependencies>
```

### Gradle
```groovy
dependencies {
    implementation 'io.github.whilein:javabyte:1.1.0'
}
```

## Examples
> Hello World
```java
MakeClass type = Javabyte.make("SayHelloWorld");
// set public & final modifiers
type.setPublicFinal();
// implement Runnable
type.addInterface(Runnable.class);

MakeMethod run = type.addMethod("run"); // create method run
// set public modifier
run.setPublic();

InstructionSet runCode = run.getBytecode();

// push string into stack
runCode.pushString("Hello world! :3");
// call System.out.println using "callSout"
runCode.callSout();

// or without callSout:
// runCode.fieldInsn(FieldOpcode.GET_STATIC, "out")
//        .in(System.class)
//        .descriptor(PrintStream.class);
//
// runCode.pushString("Hello world! :3");
//
// runCode.methodInsn(MethodOpcode.VIRTUAL, "println")
//       .in(PrintStream.class)
//       .descriptor(void.class, Object.class);

runCode.callReturn();

// define Class to current classLoader and create it
Runnable sayHelloWorld = type.load(CLASS_LOADER)
        .asSubclass(Runnable.class)
        .newInstance();

sayHelloWorld.run();
```
> Sum two numbers
```java
MakeClass type = Javabyte.make("Calculator");
type.setPublicFinal();

type.addInterface(Names.of(BiFunction.class)
        .parameterized(Integer.class, Integer.class, Integer.class));

MakeMethod apply = type.addMethod("apply");
apply.setPublic();
apply.setReturnType(Integer.class);
apply.setParameterTypes(Integer.class, Integer.class);
apply.setOverrides(BiFunction.class);

InstructionSet applyCode = apply.getBytecode();
applyCode.loadLocal(1); // push local 1 (Integer) into stack
applyCode.callUnbox(); // convert Integer to int
applyCode.loadLocal(2); // push local 2 (Integer) into stack
applyCode.callUnbox(); // convert Integer to int
applyCode.callMath(MathOpcode.IADD); // sum two integers
applyCode.callBox(); // convert int to Integer
applyCode.callReturn(); // return int

BiFunction calculator = type.load(Example.class.getClassLoader())
        .asSubclass(BiFunction.class)
        .newInstance();

return calculator.apply(10, 20);
```
> Iterate over Iterable or array
```java
// push iterable or array into stack
code.loadLocal(...);

IterateOverInsn loop = code.iterateOver();
// or instead of loadLocal & iterateOver 
// you can use iterateOver().source(...)
// without pushing it into stack
loop.element(String.class); // element type

LoopBranch body = loop.getBody();
// load element
body.loadLocal(loop.getElementLocal());
// log element to console
body.callSout();
```
> Switch-Case statement
```java
// push string into stack
code.pushString("Some string");

// for integers you should use intsSwitchCaseInsn()
StringsSwitchInsn switchCase = code.stringsSwitchCaseInsn();

// following code is equal to
// case "A": case "B": case "C":
//   return "A or B or C";
CaseBranch branchAorBorC = switchCase.branch("A", "B", "C");
branchAorBorC.loadString("A or B or C");
branchAorBorC.callReturn();
```
> Init new object
```java
// no args constructor
// public SomeType() {}
code.callInit(SomeType.class);

// constructor with args
// public SomeType(String x, int y, double z) {}
InitInsn insn = code.callInit(SomeType.class);
insn.parameters(String.class, int.class, double.class);
// We can't get rid of initializer because
// we should to write NEW & DUP first
// and only then we can write a parameters
insn.init(initializer -> {
    initializer.pushString("First parameter");
    initializer.pushInt(2);
    initializer.pushDouble(3.3);
});

```
> Init new array
```java
code.pushInt(arraySize);
code.callNewArray(String[].class);
```
> toString & hashCode
```java
MakeClass type = Javabyte.make("Entity");
type.setPublicFinal();
 
// generates new hashCode method with following code
//   int hash = 1;
//   hash = 31 * hash + Objects.hashCode(object);
//   hash = 31 * hash + Arrays.hashCode(array);
//   hash = 31 * hash + Long.hashCode(primitiveLong);
//   return hash;
// and equals method:
//   if (subject == this) return true;
//   if (!(subject instanceof <type>)) return false;
//   <type> that = (<type>) that;
//   return primitive == that.primitive 
//          && Objects.equals(object, that.object) 
//          && Arrays.equals(array, that.array);
MakeHashCodeAndEquals hashCodeAndEquals = type.addHashCodeAndEquals();
// empty if all fields
hashCodeAndEquals.setFields(...);
// add hash = 31 * hash + super.hashCode()
hashCodeAndEquals.includeSuper();

// generates new toString method with contents:
//   return "<class name>[str='...', field=..., array=[...], @super=...]
MakeToString toString = type.addToString();
// empty if all fields
toString.setFields(...);
// add "@super=" + super.toString() into result
toString.includeSuper();
```
## Contact
[Vkontakte](https://vk.com/id623151994),
[Telegram](https://t.me/whilein)

### Post Scriptum

I will be very glad if someone can help me with development.

<!-- @formatter:on  -->
