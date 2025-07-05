package lt.itsvaidas.annotationCommandAPI.dtos;

import lt.itsvaidas.annotationCommandAPI.enums.PathType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

public class PathSegment {
    private final @NotNull String name;
    private final @Nullable String argument;
    private final @NotNull PathType pathType;
    private final @NotNull Map<String, PathSegment> subCommands;
    private final @Nullable Parameter parameter;
    private final @NotNull Object clazz;
    private final @Nullable String permission;
    private @Nullable Method method;

    public PathSegment(@NotNull String name, @Nullable String argument, @NotNull PathType pathType, @NotNull Map<String, PathSegment> subCommands, @Nullable Parameter parameter, @NotNull Object clazz, @Nullable String permission) {
        this.name = name;
        this.argument = argument;
        this.pathType = pathType;
        this.subCommands = subCommands;
        this.parameter = parameter;
        this.clazz = clazz;
        this.permission = permission;
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable String getArgument() {
        return argument;
    }

    public @NotNull PathType getPathType() {
        return pathType;
    }

    public @NotNull Map<String, PathSegment> getSubCommands() {
        return subCommands;
    }

    public @Nullable Parameter getParameter() {
        return parameter;
    }

    public @NotNull Object getClazz() {
        return clazz;
    }

    public @Nullable String getPermission() {
        return permission;
    }

    public @Nullable Method getMethod() {
        return method;
    }

    public PathSegment setMethod(@Nullable Method method) {
        this.method = method;

        return this;
    }
}
