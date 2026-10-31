package ru.hukm.effectiveSpigot.minecraft.nms;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;

/**
 * Direct, JIT-inlinable access to the server's chunk sections: every member is a {@code static final}
 * {@link MethodHandle} called with {@code invokeExact}, so a call costs about as much as a plain method call, unlike the
 * reflection proxies. Exposes the raw pieces of a section's {@code PalettedContainer} (palette and packed
 * {@code long[]} of {@code BitStorage}) so callers can scan a section without one call per block.
 */
final class NmsSections {
    private static final MethodHandle GET_CHUNK;
    private static final MethodHandle GET_SECTIONS;
    private static final MethodHandle HAS_ONLY_AIR;
    private static final MethodHandle GET_BLOCK_STATE;
    private static final MethodHandle GET_STATES;
    private static final MethodHandle DATA;
    private static final MethodHandle STORAGE;
    private static final MethodHandle PALETTE;
    private static final MethodHandle RAW;
    private static final MethodHandle BITS;
    private static final MethodHandle PALETTE_SIZE;
    private static final MethodHandle VALUE_FOR;
    private static final MethodHandle GET_BLOCK;

    static {
        try {
            ReflectionRemapper remapper = NmsProxies.INSTANCE.getRemapper();
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> level = nms(remapper, "net.minecraft.world.level.Level");
            Class<?> chunkAccess = nms(remapper, "net.minecraft.world.level.chunk.ChunkAccess");
            Class<?> section = nms(remapper, "net.minecraft.world.level.chunk.LevelChunkSection");
            Class<?> container = nms(remapper, "net.minecraft.world.level.chunk.PalettedContainer");
            Class<?> data = nms(remapper, "net.minecraft.world.level.chunk.PalettedContainer$Data");
            Class<?> bitStorage = nms(remapper, "net.minecraft.util.BitStorage");
            Class<?> palette = nms(remapper, "net.minecraft.world.level.chunk.Palette");
            Class<?> blockState = nms(remapper, "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase");

            GET_CHUNK = method(lookup, remapper, level, "getChunk", int.class, int.class)
                .asType(MethodType.methodType(Object.class, Object.class, int.class, int.class));
            GET_SECTIONS = method(lookup, remapper, chunkAccess, "getSections")
                .asType(MethodType.methodType(Object[].class, Object.class));
            HAS_ONLY_AIR = method(lookup, remapper, section, "hasOnlyAir")
                .asType(MethodType.methodType(boolean.class, Object.class));
            GET_BLOCK_STATE = method(lookup, remapper, section, "getBlockState", int.class, int.class, int.class)
                .asType(MethodType.methodType(Object.class, Object.class, int.class, int.class, int.class));
            GET_STATES = method(lookup, remapper, section, "getStates")
                .asType(MethodType.methodType(Object.class, Object.class));
            DATA = getter(lookup, remapper, container, "data")
                .asType(MethodType.methodType(Object.class, Object.class));
            STORAGE = method(lookup, remapper, data, "storage")
                .asType(MethodType.methodType(Object.class, Object.class));
            PALETTE = method(lookup, remapper, data, "palette")
                .asType(MethodType.methodType(Object.class, Object.class));
            RAW = method(lookup, remapper, bitStorage, "getRaw")
                .asType(MethodType.methodType(long[].class, Object.class));
            BITS = method(lookup, remapper, bitStorage, "getBits")
                .asType(MethodType.methodType(int.class, Object.class));
            PALETTE_SIZE = method(lookup, remapper, palette, "getSize")
                .asType(MethodType.methodType(int.class, Object.class));
            VALUE_FOR = method(lookup, remapper, palette, "valueFor", int.class)
                .asType(MethodType.methodType(Object.class, Object.class, int.class));
            GET_BLOCK = method(lookup, remapper, blockState, "getBlock")
                .asType(MethodType.methodType(Object.class, Object.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private NmsSections() {
    }

    private static Class<?> nms(ReflectionRemapper remapper, String name) throws ClassNotFoundException {
        return Class.forName(remapper.remapClassName(name));
    }

    private static MethodHandle method(MethodHandles.Lookup lookup, ReflectionRemapper remapper, Class<?> owner, String name, Class<?>... params) throws ReflectiveOperationException {
        Method method = owner.getMethod(remapper.remapMethodName(owner, name, params), params);
        method.setAccessible(true);
        return lookup.unreflect(method);
    }

    private static MethodHandle getter(MethodHandles.Lookup lookup, ReflectionRemapper remapper, Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(remapper.remapFieldName(owner, name));
        field.setAccessible(true);
        return lookup.unreflectGetter(field);
    }

    static Object chunk(Object level, int chunkX, int chunkZ) throws Throwable {
        return (Object) GET_CHUNK.invokeExact(level, chunkX, chunkZ);
    }

    static Object[] sections(Object chunk) throws Throwable {
        return (Object[]) GET_SECTIONS.invokeExact(chunk);
    }

    static boolean hasOnlyAir(Object section) throws Throwable {
        return (boolean) HAS_ONLY_AIR.invokeExact(section);
    }

    static Object blockState(Object section, int x, int y, int z) throws Throwable {
        return (Object) GET_BLOCK_STATE.invokeExact(section, x, y, z);
    }

    static Object containerData(Object section) throws Throwable {
        return (Object) DATA.invokeExact((Object) GET_STATES.invokeExact(section));
    }

    static Object storage(Object data) throws Throwable {
        return (Object) STORAGE.invokeExact(data);
    }

    static Object palette(Object data) throws Throwable {
        return (Object) PALETTE.invokeExact(data);
    }

    static long[] raw(Object storage) throws Throwable {
        return (long[]) RAW.invokeExact(storage);
    }

    static int bits(Object storage) throws Throwable {
        return (int) BITS.invokeExact(storage);
    }

    static int paletteSize(Object palette) throws Throwable {
        return (int) PALETTE_SIZE.invokeExact(palette);
    }

    static Object valueFor(Object palette, int id) throws Throwable {
        return (Object) VALUE_FOR.invokeExact(palette, id);
    }

    static Object block(Object state) throws Throwable {
        return (Object) GET_BLOCK.invokeExact(state);
    }
}
