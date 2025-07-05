package lt.itsvaidas.annotationCommandAPI;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lt.itsvaidas.annotationCommandAPI.anotations.Argument;
import lt.itsvaidas.annotationCommandAPI.anotations.Command;
import lt.itsvaidas.annotationCommandAPI.anotations.Path;
import lt.itsvaidas.annotationCommandAPI.dtos.PathSegment;
import lt.itsvaidas.annotationCommandAPI.dtos.Sentence;
import lt.itsvaidas.annotationCommandAPI.enums.PathType;
import lt.itsvaidas.annotationCommandAPI.exceptions.CommandExecuteException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class CommandRegister {

    private final MiniMessage mm = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolvers(
                            StandardTags.color(),
                            StandardTags.decorations(),
                            StandardTags.gradient(),
                            StandardTags.rainbow(),
                            StandardTags.reset(),
                            StandardTags.clickEvent(),
                            StandardTags.hoverEvent()
                    )
                    .build())
            .build();
    private final LifecycleEventManager<@NotNull Plugin> manager;

    public CommandRegister(@NotNull Plugin plugin) {
        this.manager = plugin.getLifecycleManager();
    }

    public void register(Object clazz) {
        if (clazz.getClass().isAnnotationPresent(Command.class)) {
            Command command = clazz.getClass().getAnnotation(Command.class);

            String baseCommand = command.name();
            String description = command.description();
            String[] aliases = command.aliases();
            String permission = command.permission();

            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(baseCommand);

            if (!permission.equalsIgnoreCase("")) {
                root.requires(source -> !(source.getSender() instanceof Player player) || player.hasPermission(permission));
            }

            Map<String, PathSegment> pathSegments = new HashMap<>();

            Method rootMethod = Arrays.stream(clazz.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Path.class) && m.getAnnotation(Path.class).name().equalsIgnoreCase("")).findAny().orElse(null);
            if (rootMethod != null) {
                String methodPermission = rootMethod.isAnnotationPresent(Path.class) && !rootMethod.getAnnotation(Path.class).permission().equalsIgnoreCase("") ? rootMethod.getAnnotation(Path.class).permission() : null;
                pathSegments.put("", (new PathSegment("", null, PathType.LITERAL, new HashMap<>(), null, clazz, methodPermission)).setMethod(rootMethod));
            }

            for (Method method : clazz.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Path.class)) {
                    Path path = method.getAnnotation(Path.class);
                    if (path.name().equalsIgnoreCase("")) continue;

                    String[] paths = path.name().split(" ");
                    String methodPermission = path.permission().equalsIgnoreCase("") ? null : path.permission();

                    Map<String, PathSegment> currentSegments = pathSegments;
                    PathSegment currentSegment = null;

                    for (int i = 0, j = 1; !path.name().equalsIgnoreCase("") && i < paths.length; i++) {
                        String pathSegment = paths[i];
                        currentSegment = currentSegments.get(pathSegment);

                        if (currentSegment != null) {
                            if (pathSegment.startsWith("<") || pathSegment.startsWith("[")) j++;

                            currentSegments = currentSegment.getSubCommands();

                            continue;
                        }

                        Map<String, PathSegment> newSubCommands = new HashMap<>();
                        String name = pathSegment.startsWith("<") || pathSegment.startsWith("[") ? pathSegment.substring(1, pathSegment.length() - 1).toLowerCase() : pathSegment.toLowerCase();
                        if (pathSegment.startsWith("<") && pathSegment.endsWith(">")) {
                            Parameter parameter = method.getParameters()[j++];

                            currentSegment = new PathSegment(name, parameter.getName(), PathType.REQUIRED_ARGUMENT, newSubCommands, parameter, clazz, methodPermission);
                        } else if (pathSegment.startsWith("[") && pathSegment.endsWith("]")) {
                            Parameter parameter = method.getParameters()[j++];

                            currentSegment = new PathSegment(name, parameter.getName(), PathType.OPTIONAL_ARGUMENT, newSubCommands, parameter, clazz, methodPermission);
                        } else {
                            currentSegment = new PathSegment(name, null, PathType.LITERAL, newSubCommands, null, clazz, methodPermission);
                        }

                        currentSegments.put(pathSegment, currentSegment);

                        if (paths.length > i + 1 && paths[i + 1].startsWith("[") && paths[i + 1].endsWith("]")) {
                            currentSegment.setMethod(method);
                        }

                        currentSegments = currentSegment.getSubCommands();
                    }

                    if (currentSegment == null)
                        throw new IllegalArgumentException("Invalid command path: " + path.name());
                    currentSegment.setMethod(method);
                }
            }

            if (pathSegments.containsKey("")) {
                root.executes(context -> executeCommand(pathSegments.get(""), context));
            }

            for (String key : pathSegments.keySet()) {
                if (key.equalsIgnoreCase("")) continue;
                PathSegment segment = pathSegments.get(key);
                recursiveCommandRegistering(root, segment);
            }

            root.then(Commands.literal("help").executes(context -> showHelp(context.getSource().getSender(), context.getInput().replace(" help", ""), pathSegments)));
            if (!pathSegments.containsKey(""))
                root.executes(context -> showHelp(context.getSource().getSender(), context.getInput(), pathSegments));

            manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(root.build(), description, Arrays.asList(aliases)));
        }
    }

    public void recursiveCommandRegistering(ArgumentBuilder<CommandSourceStack, ?> root, PathSegment segment) {
        if (segment.getPathType() == PathType.LITERAL) {
            LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal(segment.getName());
            if (segment.getPermission() != null && !segment.getPermission().equalsIgnoreCase("")) {
                literalBuilder.requires(source -> !(source.getSender() instanceof Player player) || player.hasPermission(segment.getPermission()));
            }
            for (String key : segment.getSubCommands().keySet()) {
                PathSegment subSegment = segment.getSubCommands().get(key);
                recursiveCommandRegistering(literalBuilder, subSegment);
            }

            if (segment.getMethod() != null) {
                literalBuilder.executes(context -> executeCommand(segment, context));
            } else {
                literalBuilder.executes(context -> showHelp(context.getSource().getSender(), context.getInput(), segment.getSubCommands()));
            }

            root.then(literalBuilder);
        } else {
            ArgumentType<?> argumentType = StringArgumentType.string();
            if (segment.getParameter() != null) {
                Class<?> parameterType = segment.getParameter().getType();
                if (parameterType.equals(int.class) || parameterType.equals(Integer.class)) {
                    argumentType = IntegerArgumentType.integer();
                } else if (parameterType.equals(double.class) || parameterType.equals(Double.class)) {
                    argumentType = DoubleArgumentType.doubleArg();
                } else if (parameterType.equals(float.class) || parameterType.equals(Float.class)) {
                    argumentType = FloatArgumentType.floatArg();
                } else if (parameterType.equals(boolean.class) || parameterType.equals(Boolean.class)) {
                    argumentType = BoolArgumentType.bool();
                } else if (parameterType.equals(long.class) || parameterType.equals(Long.class)) {
                    argumentType = LongArgumentType.longArg();
                } else if (parameterType.equals(Sentence.class)) {
                    argumentType = StringArgumentType.greedyString();
                }
            }

            if (segment.getArgument() == null)
                throw new IllegalArgumentException("Path segment '" + segment.getName() + "' must have an argument defined.");

            RequiredArgumentBuilder<CommandSourceStack, ?> argumentBuilder = Commands.argument(segment.getArgument(), argumentType);
            argumentBuilder.suggests((context, builder) -> getSuggestions(segment, context, builder));

            if (segment.getPermission() != null) {
                argumentBuilder.requires(source -> !(source.getSender() instanceof Player player) || player.hasPermission(segment.getPermission()));
            }

            for (String key : segment.getSubCommands().keySet()) {
                PathSegment subSegment = segment.getSubCommands().get(key);
                recursiveCommandRegistering(argumentBuilder, subSegment);
            }

            if (segment.getMethod() != null) {
                argumentBuilder.executes(context -> executeCommand(segment, context));
            } else {
                argumentBuilder.executes(context -> showHelp(context.getSource().getSender(), context.getInput(), segment.getSubCommands()));
            }

            root.then(argumentBuilder);
        }
    }

    private CompletableFuture<Suggestions> getSuggestions(PathSegment segment, CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            Stream<String> provided = Stream.empty();

            Parameter parameter = segment.getParameter();
            if (parameter == null) {
                return builder.buildFuture();
            }

            Class<?> parameterType = parameter.getType();
            if (parameter.isAnnotationPresent(Argument.class)) {
                Argument argumentAnnotation = parameter.getAnnotation(Argument.class);
                ArgumentProvider provider = argumentAnnotation.provider().getDeclaredConstructor().newInstance();
                provided = provider.provide(context.getSource());
            } else if (parameterType.equals(OfflinePlayer.class) || parameterType.equals(Player.class)) {
                provided = Bukkit.getOnlinePlayers().stream().map(Player::getName);
            } else if (parameterType.isEnum()) {
                provided = Arrays.stream(parameterType.getEnumConstants()).map(Object::toString);
            } else if (RegistryAPI.isRegistered(parameterType)) {
                provided = RegistryAPI.get(parameterType).stream().map(k -> k.key().value());
            } else if (parameterType.equals(World.class)) {
                provided = Bukkit.getWorlds().stream().map(World::getName);
            }

            provided.filter(s -> s.toLowerCase().startsWith(builder.getRemainingLowerCase())).forEach(builder::suggest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve suggestions", e);
        }
        return builder.buildFuture();
    }

    private int executeCommand(PathSegment segment, CommandContext<CommandSourceStack> context) {
        try {
            Method method = segment.getMethod();

            if (method == null) {
                throw new IllegalStateException("No method found for command segment: " + segment.getName());
            }

            CommandSourceStack source = context.getSource();
            Object[] parameters = new Object[method.getParameterCount()];

            for (int i = 0; i < method.getParameterCount(); i++) {
                Class<?> parameterType = method.getParameterTypes()[i];
                try {
                    if (i == 0) {
                        if (parameterType.equals(CommandSender.class)) {
                            parameters[i] = source.getSender();
                        } else if (parameterType.equals(Player.class)) {
                            if (source.getSender() instanceof Player player) {
                                parameters[i] = player;
                            } else {
                                throw new CommandExecuteException("Command sender must be a player for this command.");
                            }
                        }
                        continue;
                    }
                    if (parameterType.equals(Player.class)) {
                        Player player = Bukkit.getPlayer(context.getArgument(method.getParameters()[i].getName(), String.class));
                        if (player == null)
                            throw new CommandExecuteException("Player not found: " + context.getArgument(method.getParameters()[i].getName(), String.class));
                        parameters[i] = player;
                    } else if (parameterType.equals(OfflinePlayer.class)) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(context.getArgument(method.getParameters()[i].getName(), String.class));
                        if (!player.hasPlayedBefore())
                            throw new CommandExecuteException("Offline player not found: " + context.getArgument(method.getParameters()[i].getName(), String.class));
                        parameters[i] = player;
                    } else if (parameterType.isEnum()) {
                        Enum value = Enum.valueOf((Class<Enum>) parameterType, context.getArgument(method.getParameters()[i].getName(), String.class));
                        if (value == null)
                            throw new CommandExecuteException("Incorrect argument provided");
                        parameters[i] = value;
                    } else if (RegistryAPI.isRegistered(parameterType)) {
                        Keyed object = RegistryAPI.tryGet(parameterType, context.getArgument(method.getParameters()[i].getName(), String.class));
                        if (object == null)
                            throw new CommandExecuteException("Incorrect argument provided");
                        parameters[i] = object;
                    } else if (parameterType.equals(World.class)) {
                        World world = Bukkit.getWorld(context.getArgument(method.getParameters()[i].getName(), String.class));
                        if (world == null)
                            throw new CommandExecuteException("Incorrect argument provided");
                        parameters[i] = world;
                    } else if (parameterType.equals(Sentence.class)) {
                        parameters[i] = new Sentence(context.getArgument(method.getParameters()[i].getName(), String.class));
                    } else {
                        parameters[i] = context.getArgument(method.getParameters()[i].getName(), parameterType);
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null && !e.getMessage().startsWith("No such argument") && e instanceof IllegalArgumentException) {
                        context.getSource().getSender().sendMessage(Component.text("Incorrect argument provided: " + e.getMessage()));
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    }

                    if (e instanceof CommandExecuteException argumentNotFoundException) {
                        context.getSource().getSender().sendMessage(Component.text("Error executing command: " + argumentNotFoundException.getMessage()));
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    }

                    if (parameterType.equals(int.class)) {
                        parameters[i] = 0;
                    } else if (parameterType.equals(boolean.class)) {
                        parameters[i] = false;
                    } else if (parameterType.equals(double.class)) {
                        parameters[i] = 0.0;
                    } else if (parameterType.equals(float.class)) {
                        parameters[i] = 0.0f;
                    } else if (parameterType.equals(long.class)) {
                        parameters[i] = 0L;
                    } else if (parameterType.equals(short.class)) {
                        parameters[i] = (short) 0;
                    } else if (parameterType.equals(byte.class)) {
                        parameters[i] = (byte) 0;
                    } else if (parameterType.equals(char.class)) {
                        parameters[i] = ' ';
                    } else {
                        parameters[i] = null;
                    }
                }
            }

            method.invoke(segment.getClazz(), parameters);
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            return 0;
        }
    }

    private int showHelp(CommandSender sender, String label, Map<String, PathSegment> segments) {
        List<String> sortedSubCommands = segments.keySet().stream()
                .sorted()
                .toList();

        for (String key : sortedSubCommands) {
            PathSegment segment = segments.get(key);

            if (segment.getSubCommands().isEmpty() && segment.getMethod() != null) {
                Method method = segment.getMethod();
                Path path = method.getAnnotation(Path.class);

                if (!path.permission().equalsIgnoreCase("") && !sender.hasPermission(path.permission())) {
                    continue;
                }

                Command command = method.getDeclaringClass().getAnnotation(Command.class);

                String description = path.description();

                if (key.equalsIgnoreCase("")) {
                    command(sender, "/" + label, description);
                } else {
                    command(sender, "/" + label + " " + key, description);
                }
            } else {
                showHelp(sender, label + " " + key, segment.getSubCommands());
            }
        }

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private void command(CommandSender sender, String command, String description) {
        if (command.contains("<") || command.contains("[")) {
            String commandWithoutArgs = command.replaceAll("<.*?>", "").replaceAll("\\[.*?]", "");
            sender.sendMessage(stringToComponent("<gold> ▶ <dark_gray>|<gray> <click:suggest_command:" + commandWithoutArgs + "><hover:show_text:'" + description + "'>" + command + "</hover></click> - " + description));
        } else {
            sender.sendMessage(stringToComponent("<gold> ▶ <dark_gray>|<gray> <click:run_command:" + command + "><hover:show_text:'" + description + "'>" + command + "</hover></click> - " + description));
        }
    }


    private Component stringToComponent(@NotNull String string) {
        return Component.empty()
                .style(Style.style(TextDecoration.ITALIC.withState(false)))
                .append(mm.deserialize(string));
    }
}
