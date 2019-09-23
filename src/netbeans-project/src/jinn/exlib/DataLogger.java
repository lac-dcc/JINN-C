/*
 * Copyright (C) 2019 juniocezar.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jinn.exlib;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author juniocezar
 */
public class DataLogger {
    private static Set<String> log = new HashSet<String>();
    private static String logname = "methods-runtime.txt";

    public static void logStr (int hashcode, long timestamp, String a1) {
        String data = "" + hashcode + "," + a1 + "," +  timestamp + "\n";
        log.add(data);
    }

    public static void logStr (int hashcode, long timestamp, String a1
        , String a2) {
        String data = "" + hashcode + "," + a1 + "," + a2 + "," +  timestamp  + "\n";
        log.add(data);
    }

    public static void logStr (int hashcode, long timestamp, String a1
        , String a2, String a3) {
        String data = "" + hashcode + "," + a1 + "," + a2 + "," + a3 +
                "," + timestamp + "\n";
        log.add(data);
    }

    public static void logStr (int hashcode, long timestamp, String a1
        , String a2, String a3, String a4) {
        String data = "" + hashcode + "," + a1 + "," + a2 + "," + a3 +
                 "," + a4 + "," + timestamp + "\n";
        log.add(data);
    }


    public static void dump () {
        try {
            FileWriter fw = new FileWriter(logname, true);
            BufferedWriter bw = new BufferedWriter(fw);
            for (String line : log) {
                bw.write(line);
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.clear();
    }
}
