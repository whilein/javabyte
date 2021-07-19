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

## Contact
[Vkontakte](https://vk.com/id623151994),
[Telegram](https://t.me/whilein)

### Post Scriptum

I will be very glad if someone can help me with development.

<!-- @formatter:on  -->
