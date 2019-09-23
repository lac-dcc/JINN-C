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


import java.util.List;
import java.util.Random;
import java.util.ArrayList;

public class Predictor {
  private static String current = "0xff";

  public static void setConfig(String configStr) {
        try {
          //tokens[0] is frequency, tokens[1] is config
          String[] tokens = configStr.split("-");
          Runtime r = Runtime.getRuntime();
          final int pid = getProcessID();
          //
          // Change hardware configuration
          String cmd = "taskset -pa " + tokens[1] + " " + pid;
          Process p = r.exec(cmd);
          if (p.waitFor() != 0) {
            System.err.println("exit value = " + p.exitValue());
          }
          //
          // Change cluster frequency
          String[]  cmd2 = {"sudo", "cpufreq-set", "-f", tokens[0] , "-c" , "4"};
          p = r.exec(cmd2);
          if (p.waitFor() != 0) {
            System.err.println(">> exit value = " + p.exitValue());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
  }

 public static int getProcessID() /* throws
    NoSuchFieldException,
    IllegalAccessException,
    NoSuchMethodException,
    java.lang.reflect.InvocationTargetException {
        java.lang.management.RuntimeMXBean runtime =
          java.lang.management.ManagementFactory.getRuntimeMXBean();
        java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
        jvm.setAccessible(true);
        sun.management.VMManagement mgmt =
          (sun.management.VMManagement) jvm.get(runtime);
        java.lang.reflect.Method pid_method =
          mgmt.getClass().getDeclaredMethod("getProcessId");
        pid_method.setAccessible(true);
        return (Integer)pid_method.invoke(mgmt);*/
  {      return (int)ProcessHandle.current().pid();
  }

  public static double predict(double[] inputs) {
    long init = System.nanoTime();
    String config = 
        //##CONFIG##
    setConfig(config);
    double time = (System.nanoTime() - init) / 1e6;
    System.err.println("Predicted: " + config);
    return time;
  }
}
