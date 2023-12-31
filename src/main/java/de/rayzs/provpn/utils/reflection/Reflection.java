package de.rayzs.provpn.utils.reflection;

import net.md_5.bungee.api.ProxyServer;
import org.bukkit.entity.Player;
import java.lang.reflect.*;
import org.bukkit.Bukkit;
import java.util.*;

public class Reflection {

    private static boolean legacy, bungeecord;
    private static String versionName, rawVersionName, versionPackageName;
    private static Version version;
    private static int major, minor, release;

    public static void initialize(Object serverObject) {
        try {
            Class.forName("org.bukkit.Server");
            bungeecord = false;
        } catch (ClassNotFoundException ignored) { bungeecord = true; }

        if (!bungeecord) {
            loadVersionName(serverObject);
            loadAges();
            loadVersionEnum();
            legacy = minor <= 16;
        }
    }

    public static int[] getAges() { return new int[]{major, minor, release}; }
    public static String getVersionName() { return versionName; }
    public static String getRawVersionName() { return rawVersionName; }
    public static String getVersionPackageName() { return versionPackageName; }
    public static Version getVersion() { return version; }
    public static boolean isModern() { return !legacy; }
    public static boolean isLegacy() { return legacy; }
    public static boolean isBungeecordServer() {
        return bungeecord;
    }

    public static Class<?> getMinecraftClass(String name) throws ClassNotFoundException {
        String classPath = (isLegacy() ? "net.minecraft.server." + versionPackageName : "net.minecraft.network.protocol.game") + "." + name;
        return Class.forName(classPath);
    }

    public static Class<?> getClass(String path, String name) throws ClassNotFoundException {
        String classPath = path + "." + name;
        return Class.forName(classPath);
    }

    public static Class<?> getPacketClass() throws ClassNotFoundException {
        String classPath = (isLegacy() ? "net.minecraft.server." + versionPackageName : "net.minecraft.network.protocol") + ".Packet";
        return Class.forName(classPath);
    }

    public static Class<?> getMinecraftServerClass() throws ClassNotFoundException {
        String classPath = (isLegacy() ? "net.minecraft.server." + versionPackageName : "net.minecraft.server") + ".MinecraftServer";
        return Class.forName(classPath);
    }

    public static Class<?> getPacketDataSerializerClass() throws ClassNotFoundException {
        String classPath = (isLegacy() ? "net.minecraft.server." + versionPackageName : "net.minecraft.network") + ".PacketDataSerializer";
        return Class.forName(classPath);
    }

    public static Field getFieldByName(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        openAccess(field, true);
        return field;
    }

    public static Field getFirstFieldByType(Class<?> clazz, Class<?> type) {
        return getFieldsByType(clazz, type).get(0);
    }

    public static Field getFirstFieldByType(Class<?> clazz, String type) {
        return getFieldsByType(clazz, type).get(0);
    }

    public static Field getLastFieldByType(Class<?> clazz, Class<?> type) {
        List<Field> fields = getFieldsByType(clazz, type);
        return fields.get(fields.size()-1);
    }

    public static Field getLastFieldByType(Class<?> clazz, String type) {
        List<Field> fields = getFieldsByType(clazz, type);
        return fields.get(fields.size()-1);
    }

    public static List<Field> getFieldsByType(Class<?> clazz, Class<?> type) {
        List<Field> result = new ArrayList<>();
        List<Field[]> fieldLists = Arrays.asList(clazz.getFields(), clazz.getDeclaredFields());

        for (Field[] fields : fieldLists)
            for (Field field : fields) {
                if(!openAccess(field)) continue;
                if(field.getAnnotatedType().getType() != type) {
                    closeAccess(field);
                    continue;
                }
                result.add(field);
            }
        return result;
    }

    public static boolean openAccess(Field field) {
        return openAccess(field, false);
    }

    public static boolean openAccess(Field field, boolean ignore) {
        if(!ignore && (Modifier.isFinal(field.getModifiers()) || field.isAccessible())) {
            return false;
        }
        field.setAccessible(true);
        return true;
    }

    public static boolean closeAccess(Field field, boolean ignore) {
        if(!ignore && Modifier.isFinal(field.getModifiers()) || !field.isAccessible()) return false;
        field.setAccessible(false);
        return true;
    }

    public static boolean closeAccess(Field field) {
        return closeAccess(field, false);
    }

    public static List<Field> getFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        List<Field[]> fieldLists = Arrays.asList(clazz.getFields(), clazz.getDeclaredFields());

        for (Field[] fields : fieldLists)
            for(Field field : fields)
                result.addAll(Collections.singletonList(field));
        return result;
    }

    public static List<Field> getFieldsByType(Class<?> clazz, String type) {
        boolean useContains = type.endsWith("%contains%");
        boolean useStarts = type.endsWith("%starts%");
        boolean useEnds = type.endsWith("%ends%");

        if(useContains) type = type.replace("%contains%", "");
        else if(useStarts) type = type.replace("%starts%", "");
        else if(useEnds) type = type.replace("%ends%", "");

        type = type.replace("%contains%", "");

        List<Field> result = new ArrayList<>();
        List<Field> fieldLists = getFields(clazz);

        for (Field field : fieldLists) {
            openAccess(field, true);
            String typeName = field.getAnnotatedType().getType().getTypeName();
            if(useContains && typeName.contains(type)
                    || useStarts && typeName.startsWith(type)
                    || useEnds && typeName.endsWith(type)
                    || typeName.equals(type))
                result.add(field);
            else closeAccess(field);
        }
        return result;
    }

    public static Object getPlayerConnection(Player player) throws Exception {
        Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
        return getFieldsByType(entityPlayer.getClass(), "PlayerConnection%contains%").get(0).get(entityPlayer);
    }

    public static Object getPlayerNetworkManager(Object playerConnection) throws Exception {
        Optional<Field> optional = getFieldsByType(playerConnection.getClass(), "NetworkManager%contains%").stream().findFirst();
        if(!optional.isPresent()) return null;
        return optional.get().get(playerConnection);
    }

    public static void sendPacket(Player player, Object packetObj) throws Exception {
        Class<?> packetClazz = getPacketClass();
        Object playerConnection = getPlayerConnection(player);
        playerConnection.getClass().getMethod(minor < 18  ? "sendPacket" : "a", packetClazz).invoke(playerConnection, packetObj);
    }

    public static void setFieldValue(Field field, Object clazzObj, Object value, boolean closeAccessibility) throws IllegalAccessException {
        field.set(clazzObj, value);
        if(closeAccessibility) field.setAccessible(false);
    }

    public static void getAndSetField(String fieldName, Class<?> clazz, Object clazzObj, Object value, boolean closeAccessibility) throws Exception {
        Field field = getFieldByName(clazz, fieldName);
        setFieldValue(field, clazzObj, value, closeAccessibility);
    }

    public static void getAndSetField(Field field, Object clazzObj, Object value, boolean closeAccessibility) throws Exception {
        setFieldValue(field, clazzObj, value, closeAccessibility);
    }

    public static void getAndSetField(Class<?> type, int which, Class<?> clazz, Object clazzObj, Object value, boolean closeAccessibility) throws Exception {
        List<Field> fields = getFieldsByType(clazz, type);
        setFieldValue(fields.get(which), clazzObj, value, closeAccessibility);
    }

    public static boolean doesClassExist(String classPath, String className) {
        classPath = (classPath.contains("/") ? className.replace("/", ".") : classPath).toLowerCase();
        return doesClassExist(classPath + "." + className);
    }

    public static boolean doesClassExist(String className) {
        try {
            Class.forName(className);
            return true;
        }catch (ClassNotFoundException classNotFoundException) {
            return false;
        }
    }

    private static void loadVersionName(Object serverObject) {
        try {
            versionName = bungeecord ? ProxyServer.getInstance().getName() : Bukkit.getName();
            rawVersionName = (String) serverObject.getClass().getMethod("getBukkitVersion").invoke(serverObject);
            rawVersionName = rawVersionName.split("-")[0].replace(".", "_");
            versionPackageName = serverObject.getClass().getPackage().getName();
            versionPackageName = versionPackageName.substring(versionPackageName.lastIndexOf('.') + 1);
        }catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void loadVersionEnum() {
        final String primaryVersionName, fullVersionName;
        StringBuilder builder = new StringBuilder("v_");
        builder.append(major).append("_").append(minor);
        primaryVersionName = builder.toString();
        if(release != 0) builder.append("_").append(release);
        fullVersionName = builder.toString();
        boolean couldFindOriginalVersion = Arrays.stream(Version.values()).anyMatch(searchingVersion -> searchingVersion.toString().equals(fullVersionName));
        version = Version.valueOf(couldFindOriginalVersion ? fullVersionName : primaryVersionName);
    }

    private static void loadAges() {
        String[] versionArgs = rawVersionName.split("_");
        major = Integer.parseInt(versionArgs[0]);
        minor = Integer.parseInt(versionArgs[1]);
        release = versionArgs.length > 2 ? Integer.parseInt(versionArgs[2]) : 0;
    }

    public static int getMajor() { return major; }
    public static int getMinor() { return minor; }
    public static int getRelease() { return release; }

    public enum Version {
        v_1_8, v_1_8_8,
        v_1_9, v_1_9_4,
        v_1_10, v_1_10_2,
        v_1_11, v_1_11_2,
        v_1_12, v_1_12_2,
        v_1_13, v_1_13_2,
        v_1_14, v_1_14_4,
        v_1_15, v_1_15_2,
        v_1_16, v_1_16_4, v_1_16_5,
        v_1_17, v_1_17_1, v_1_18,
        v_1_18_1, v_1_18_2,
        v_1_19, v_1_19_1, v_1_19_2, v_1_19_3,
        v_1_20, v_1_20_1, v_1_20_2, v_1_20_3, v_1_20_4,
        v_1_21, v_1_21_1, v_1_21_2;
    }
}
