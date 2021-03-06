// ========================================================================
// $Id: TypeUtil.java,v 1.14 2005/12/06 00:51:40 gregwilkins Exp $
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package net.lightbody.bmp.proxy.jetty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * TYPE Utilities.
 * Provides various static utiltiy methods for manipulating types and their string representations.
 *
 * @author Greg Wilkins (gregw)
 * @version $Revision: 1.14 $
 * @since Jetty 4.1
 */
public class TypeUtil {
    
    private static final HashMap name2Class = new HashMap();
    
    private static final HashMap class2Name = new HashMap();
    
    private static final HashMap class2Value = new HashMap();
    private static final Logger log = LoggerFactory.getLogger(TypeUtil.class);
    
    private static final Class[] stringArg = {java.lang.String.class};
    
    private static final int intCacheSize = Integer.getInteger("net.lightbody.bmp.proxy.jetty.util.TypeUtil.IntegerCacheSize", 600);
    private static final Integer[] integerCache = new Integer[intCacheSize];
    private static final String[] integerStrCache = new String[intCacheSize];
    private static final Integer minusOne = -1;

    static {
        name2Class.put("boolean", java.lang.Boolean.TYPE);
        name2Class.put("byte", java.lang.Byte.TYPE);
        name2Class.put("char", java.lang.Character.TYPE);
        name2Class.put("double", java.lang.Double.TYPE);
        name2Class.put("float", java.lang.Float.TYPE);
        name2Class.put("int", java.lang.Integer.TYPE);
        name2Class.put("long", java.lang.Long.TYPE);
        name2Class.put("short", java.lang.Short.TYPE);
        name2Class.put("void", java.lang.Void.TYPE);

        name2Class.put("java.lang.Boolean.TYPE", java.lang.Boolean.TYPE);
        name2Class.put("java.lang.Byte.TYPE", java.lang.Byte.TYPE);
        name2Class.put("java.lang.Character.TYPE", java.lang.Character.TYPE);
        name2Class.put("java.lang.Double.TYPE", java.lang.Double.TYPE);
        name2Class.put("java.lang.Float.TYPE", java.lang.Float.TYPE);
        name2Class.put("java.lang.Integer.TYPE", java.lang.Integer.TYPE);
        name2Class.put("java.lang.Long.TYPE", java.lang.Long.TYPE);
        name2Class.put("java.lang.Short.TYPE", java.lang.Short.TYPE);
        name2Class.put("java.lang.Void.TYPE", java.lang.Void.TYPE);

        name2Class.put("java.lang.Boolean", java.lang.Boolean.class);
        name2Class.put("java.lang.Byte", java.lang.Byte.class);
        name2Class.put("java.lang.Character", java.lang.Character.class);
        name2Class.put("java.lang.Double", java.lang.Double.class);
        name2Class.put("java.lang.Float", java.lang.Float.class);
        name2Class.put("java.lang.Integer", java.lang.Integer.class);
        name2Class.put("java.lang.Long", java.lang.Long.class);
        name2Class.put("java.lang.Short", java.lang.Short.class);

        name2Class.put("Boolean", java.lang.Boolean.class);
        name2Class.put("Byte", java.lang.Byte.class);
        name2Class.put("Character", java.lang.Character.class);
        name2Class.put("Double", java.lang.Double.class);
        name2Class.put("Float", java.lang.Float.class);
        name2Class.put("Integer", java.lang.Integer.class);
        name2Class.put("Long", java.lang.Long.class);
        name2Class.put("Short", java.lang.Short.class);

        name2Class.put(null, java.lang.Void.TYPE);
        name2Class.put("string", java.lang.String.class);
        name2Class.put("String", java.lang.String.class);
        name2Class.put("java.lang.String", java.lang.String.class);
    }

    static {
        class2Name.put(java.lang.Boolean.TYPE, "boolean");
        class2Name.put(java.lang.Byte.TYPE, "byte");
        class2Name.put(java.lang.Character.TYPE, "char");
        class2Name.put(java.lang.Double.TYPE, "double");
        class2Name.put(java.lang.Float.TYPE, "float");
        class2Name.put(java.lang.Integer.TYPE, "int");
        class2Name.put(java.lang.Long.TYPE, "long");
        class2Name.put(java.lang.Short.TYPE, "short");
        class2Name.put(java.lang.Void.TYPE, "void");

        class2Name.put(java.lang.Boolean.class, "java.lang.Boolean");
        class2Name.put(java.lang.Byte.class, "java.lang.Byte");
        class2Name.put(java.lang.Character.class, "java.lang.Character");
        class2Name.put(java.lang.Double.class, "java.lang.Double");
        class2Name.put(java.lang.Float.class, "java.lang.Float");
        class2Name.put(java.lang.Integer.class, "java.lang.Integer");
        class2Name.put(java.lang.Long.class, "java.lang.Long");
        class2Name.put(java.lang.Short.class, "java.lang.Short");

        class2Name.put(null, "void");
        name2Class.put(java.lang.String.class, "java.lang.String");
    }

    static {
        try {
            Class[] s = {java.lang.String.class};

            class2Value.put(java.lang.Boolean.TYPE, java.lang.Boolean.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Byte.TYPE, java.lang.Byte.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Double.TYPE, java.lang.Double.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Float.TYPE, java.lang.Float.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Integer.TYPE, java.lang.Integer.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Long.TYPE, java.lang.Long.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Short.TYPE, java.lang.Short.class.getMethod("valueOf", s));

            class2Value.put(java.lang.Boolean.class, java.lang.Boolean.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Byte.class, java.lang.Byte.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Double.class, java.lang.Double.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Float.class, java.lang.Float.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Integer.class, java.lang.Integer.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Long.class, java.lang.Long.class.getMethod("valueOf", s));
            class2Value.put(java.lang.Short.class, java.lang.Short.class.getMethod("valueOf", s));
        } catch (Exception e) {
            log.warn(LogSupport.EXCEPTION, e);
        }
    }

    /**
     * Class from a canonical name for a type.
     *
     * @param name A class or type name.
     * @return A class , which may be a primitive TYPE field..
     */
    public static Class fromName(String name) {
        return (Class) name2Class.get(name);
    }

    /**
     * Convert String value to instance.
     *
     * @param type  The class of the instance, which may be a primitive TYPE field.
     * @param value The value as a string.
     * @return The value as an Object.
     */
    public static Object valueOf(Class type, String value) {
        try {
            if (type.equals(java.lang.String.class)) {
                return value;
            }

            Method m = (Method) class2Value.get(type);
            if (m != null) {
                return m.invoke(null, new Object[]{value});
            }

            if (type.equals(java.lang.Character.TYPE) || type.equals(java.lang.Character.class)) {
                return new Character(value.charAt(0));
            }

            Constructor c = type.getConstructor(stringArg);
            return c.newInstance(new Object[]{value});
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            //
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) (e.getTargetException());
            }
        }
        return null;
    }

    /**
     * Convert int to Integer using cache.
     */
    public static Integer newInteger(int i) {
        if (i >= 0 && i < intCacheSize) {
            if (integerCache[i] == null) {
                integerCache[i] = i;
            }
            return integerCache[i];
        } else {
            if (i == -1) {
                return minusOne;
            }
        }
        return i;
    }

    /**
     * Convert int to String using cache.
     */
    public static String toString(int i) {
        if (i >= 0 && i < intCacheSize) {
            if (integerStrCache[i] == null) {
                integerStrCache[i] = Integer.toString(i);
            }
            return integerStrCache[i];
        } else {
            if (i == -1) {
                return "-1";
            }
        }
        return Integer.toString(i);
    }

    /**
     * Parse an int from a substring. Negative numbers are not handled.
     *
     * @param s      String
     * @param offset Offset within string
     * @param length Length of integer or -1 for remainder of string
     * @param base   base of the integer
     * @throws NumberFormatException
     */
    public static int parseInt(String s, int offset, int length, int base) throws NumberFormatException {
        int value = 0;

        if (length < 0) {
            length = s.length() - offset;
        }

        for (int i = 0; i < length; i++) {
            char c = s.charAt(offset + i);

            int digit = c - '0';
            if (digit < 0 || digit >= base || digit >= 10) {
                digit = 10 + c - 'A';
                if (digit < 10 || digit >= base) {
                    digit = 10 + c - 'a';
                }
            }
            if (digit < 0 || digit >= base) {
                throw new NumberFormatException(s.substring(offset, offset + length));
            }
            value = value * base + digit;
        }
        return value;
    }
    
    public static byte[] parseBytes(String s, int base) {
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            bytes[i / 2] = (byte) TypeUtil.parseInt(s, i, 2, base);
        }
        return bytes;
    }

    public static String toString(byte[] bytes, int base) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int bi = 0xff & bytes[i];
            int c = '0' + (bi / base) % base;
            if (c > '9') {
                c = 'a' + (c - '0' - 10);
            }
            buf.append((char) c);
            c = '0' + bi % base;
            if (c > '9') {
                c = 'a' + (c - '0' - 10);
            }
            buf.append((char) c);
        }
        return buf.toString();
    }

    /**
     * @param b An ASCII encoded character 0-9 a-f A-F
     * @return The byte value of the character 0-16.
     */
    public static byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) {
            return (byte) (b - '0');
        }
        if ((b >= 'a') && (b <= 'f')) {
            return (byte) (b - 'a' + 10);
        }
        if ((b >= 'A') && (b <= 'F')) {
            return (byte) (b - 'A' + 10);
        }
        return 0;
    }

    public static char toHexChar(int b) {
        return (char) (b < 10 ? ('0' + b) : ('A' + b - 10));
    }

}
