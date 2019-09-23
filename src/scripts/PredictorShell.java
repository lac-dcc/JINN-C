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
  public static double coefs[][] = 
    //##COEFS##

  public static String[] hw_cfgs = 
    //##CLASSES##
  public static double inter_plane[] = 
    //##INTER##
  public static int _rows = coefs.length;
  public static int _cols = coefs[0].length;  

  public static double[] dot(double[] input) {
     double[] result = new double[_cols];
     double sum = 0.0;
     for (int d = 0; d < _cols; d++) {
       for (int k = 0; k < _rows; k++) {
         sum += input[k] * coefs[k][d];
       }
       result[d] = sum + inter_plane[d];
       sum = 0.0;
     }
     return result;
   }

   private static String softm(double[] X) {
     int best = 0;
     double best_val = -2;
     for (int i = 0; i < X.length; i++) {
       double val = 1 / (1 + Math.exp(-X[i]));
       if (val > best_val) {
         best_val = val;
         best = i;
       }
     }
     // In case of only two target classes, the
     // probability of the classes listed above are
     // [best_val -1] [best_val]. because of that
     // if best_val > 50, the probabilidates of
     // the classes are [-50][+50], so we return
     // config[1]
     if (hw_cfgs.length == 2 && best_val > 0.50)
        return hw_cfgs[1];

     return hw_cfgs[best];
  }

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

  private static double[] normalize (double[] input) {
     double mean[]  =
//##MEAN##
     double scale[] = 
//##SCALE##

     //input[0] = (input[0] - mean[0]) / scale[0];
//##INPUTS##
     return input;
  }

  public static double predict(double[] inputs) {
    long init = System.nanoTime();
    String config = softm(dot(normalize(inputs)));
    setConfig(config);
    double time = (System.nanoTime() - init) / 1e6;
    System.err.println("Predicted: " + config);
    return time;
  }
}
