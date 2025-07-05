# AnnotatedCommandAPI

AnnotatedCommandAPI is a powerful, easy-to-use command framework designed to simplify Minecraft plugin command development by removing the complexity of Brigadier's command tree structure. It provides an intuitive annotation-based system that allows developers to define commands, subcommands, permissions, and argument parsing cleanly and efficiently within plain Java classes and methods.

---

## Key Features

- **Annotation-Driven Commands**: Define commands and subcommands using straightforward annotations like `@Command` and `@Path`, eliminating verbose and error-prone boilerplate.
- **Flexible Argument Parsing**: Supports primitive types (`String`, `int`, `double`, etc.) and advanced types (`World`, `Player`, custom objects) with automatic tab completion.
- **Custom Argument Providers**: Easily create your own argument providers to supply dynamic command argument suggestions.
- **Permission Integration**: Attach permissions at the command and subcommand level via annotations to control access seamlessly.
- **Clear Separation**: Organize commands by class and group subcommands as annotated methods, making your codebase clean and maintainable.
- **Player vs Console Execution**: Explicitly specify whether a command can be run by players, console, or both via method parameters (`Player` or `CommandSender`).

---

## Getting Started

### Integrating into your Plugin

To use AnnotatedCommandAPI in your Minecraft plugin, add the following dependency to your `build.gradle` file:
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.ItsVaidas:AnnotatedCommandAPI:TAG'
}
```

Replace `TAG` with the latest version tag from the [AnnotatedCommandAPI repository](https://github.com/ItsVaidas/AnnotatedCommandAPI)

### Basic Command Example

Define a simple command that broadcasts a message to all players:

```java
@Path(
        name = "<Message>",
        description = "Send message to the whole server"
)
public void command(CommandSender sender, Sentence message) {
    Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message.toString()));
}
```

- The `@Path` annotation declares a command path with parameters.
- `Sentence` is a special argument type designed to capture long strings including spaces.
- For a single word argument, use `String`.

---

### Full Command Class with Subcommands

A complex teleport command illustrating multiple subcommands and permissions:

```java
@Command(
        name = "tpw",
        description = "World teleport command",
        permission = "zccommands.command.teleport"
)
public class TpwCommand {

    @Path(
            name = "load <World>",
            description = "Load a world",
            permission = "zccommands.command.teleport.load"
    )
    public void load(CommandSender sender, String world) {
        WorldCreator newWorld = new WorldCreator(world);
        newWorld.generator(new VoidChunkGenerator());
        newWorld.createWorld();
    }

    @Path(
            name = "unload <World>",
            description = "Unload a world",
            permission = "zccommands.command.teleport.unload"
    )
    public void unload(CommandSender sender, String world) {
        Bukkit.unloadWorld(world, true);
    }

    @Path(
            name = "<World>",
            description = "Teleport to a world",
            permission = "zccommands.command.teleport.world"
    )
    public void teleportToWorld(Player sender, World world) {
        sender.teleportAsync(world.getSpawnLocation());
    }

    @Path(
            name = "<World> <X> <Y> <Z>",
            description = "Teleport to a world by coordinates",
            permission = "zccommands.command.teleport.world"
    )
    public void teleportToWorldByCoordinates(Player sender, World world, double x, double y, double z) {
        sender.teleportAsync(new Location(world, x, y, z));
    }
}
```

### Explanation:

- The `@Command` annotation marks the class as a command root, setting its base name, description, and permission.
- Each method annotated with `@Path` represents a subcommand or variant of the main command.
- The `name` property of `@Path` defines the command syntax, including required parameters (e.g., `<World> <X> <Y> <Z>`).
- Method parameters correspond to those command arguments, and their types can be native Java types or custom objects recognized by the system.
- Permissions can be set per subcommand to fine-tune access control.
- The first argument to every method is either a `Player` or `CommandSender`, depending on who can execute the command.

---

## Argument Types & Custom Providers

AnnotatedCommandAPI supports:

- **Primitive types**: `String`, `int`, `double`, `boolean`, etc.
- **Complex types**: `Player`, `World`, `Location`, etc., with automatic tab-completion.
- **Sentence**: A special argument type capturing entire sentences or multiple words with spaces.
- **Custom argument providers**: To support dynamic or plugin-specific argument suggestions.

---

### Creating a Custom Argument Provider

Example: Providing dynamic arena names for command arguments.

```java
@Path(
        name = "start <Name>",
        description = "Start an event"
)
public void onStart(CommandSender sender, @Argument(provider = ArenaNameProvider.class) String name) {
    // Implementation here
}

public class ArenaNameProvider extends ArgumentProvider {
    public ArenaNameProvider() {} // Required no-arg constructor

    @Override
    public Stream<String> provide(CommandSourceStack source) {
        return Data.getArenas().stream()
                   .map(ArenaDTO::getName);
    }
}
```

- Implement `ArgumentProvider` and override `provide` to return a stream of valid argument strings.
- Attach the provider to the command argument with `@Argument(provider = YourProvider.class)`.
- This enables custom tab completions and validation dynamically based on plugin data or runtime state.

---

## Command Registration

To activate your commands in your plugin, you need to register them with the `CommandRegister` class. Here's how you can do it in your plugin's main class (usually in `onEnable`):

```java
CommandRegister cr = new CommandRegister(this);
cr.register(new TpwCommand());
```

- `this` refers to your plugin instance.
- You create an instance of your command class (e.g., `TpwCommand`) and register it with the command register.
- This process automatically scans annotations and hooks commands into the server.

---

## How It Works

1. **Command registration:** AnnotatedCommandAPI registers all the commands and subcommands in one class.
2. **Argument mapping:** Method parameters are matched to command arguments by their order and type.
3. **Permission checks:** Before command execution, required permissions are verified automatically.
4. **Tab completion:** Based on argument types and custom providers, users receive dynamic tab completions.
5. **Execution context:** Methods receive the executing sender (`Player` or `CommandSender`) and parsed arguments, allowing full control over command logic.

---

## Why Choose AnnotatedCommandAPI?

- **Reduces complexity** — no more managing Brigadier trees manually.
- **Speeds up development** — annotate methods and handle commands with simple Java code.
- **Improves readability** — command logic is cleanly organized in classes and methods.
- **Enhances user experience** — built-in tab completion and permission handling.
- **Extensible** — easily add new argument types and providers tailored to your needs.

---

## Summary

AnnotatedCommandAPI is an elegant solution for Minecraft plugin developers seeking to create complex commands without the hassle of Brigadier’s intricate command trees. It embraces Java annotations to define commands, arguments, and permissions intuitively, supports rich argument types, custom providers, and grants full flexibility over who can execute commands and how.

Start using AnnotatedCommandAPI today and build commands faster, cleaner, and smarter!

---

If you want examples, detailed guides, or help integrating with your plugin, feel free to ask!