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
</div>

## About the project

Javabyte - Simple library that helps you to create new classes

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

Bytecode runCode = run.getBytecode();

// push string to stack
runCode.loadString("Hello world! :3");
// call System.out.println using "callMacro"
runCode.callMacro(Macro.SOUT);

// or without macro:
// runCode.fieldInsn(FieldOpcode.GET_STATIC, "out")
//        .in(System.class)
//        .descriptor(PrintStream.class);
//
// runCode.loadString("Hello world! :3");
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

Bytecode applyCode = apply.getBytecode();
applyCode.loadLocal(1); // push local 1 (Integer) to stack
applyCode.callUnbox(); // convert Integer to int
applyCode.loadLocal(2); // push local 2 (Integer) to stack
applyCode.callUnbox(); // convert Integer to int
applyCode.callMath(MathOpcode.IADD); // sum two integers
applyCode.callBox(); // convert int to Integer
applyCode.callReturn(); // return int

Calculator calculator = type.load(Example.class.getClassLoader())
        .asSubclass(BiFunction.class)
        .newInstance();

return calculator.apply(10, 20);
```
> Iterate over Iterable or array
```java
// push iterable or array to stack
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
body.callMacro(Macro.SOUT);
```
> Switch-Case statement
```java
code.loadString("Some string");

// for integers you should use intsSwitchCaseInsn()
StringsSwitchInsn switchCase = code.stringsSwitchCaseInsn();

// following code is equal to
// case "A": case "B": case "C":
//   return "A or B or C";
switchCase.branch("A");
switchCase.branch("B");
CaseBranch branchAorBorC = switchCase.branch("C");
branchAorBorC.loadString("A or B or C");
branchAorBorC.callReturn();
```
## Contact
[Vkontakte](https://vk.com/id623151994),
[Telegram](https://t.me/whilein)

### Post Scriptum

I will be very glad if someone can help me with development.

<!-- @formatter:on  -->
