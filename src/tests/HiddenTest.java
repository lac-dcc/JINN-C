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
import java.util.*;

public class HiddenTest {
    // global var to be captured by JINN system
    static int globalVar = 10;
    // global collection to be captured by JINN system
    static List<String> globalStr;

    public static void main (String[] args) {
        //
        // Initialize and populate global array
        globalStr = new ArrayList<String>();
        globalStr.add("10");
        globalStr.add("20");
        //
        // calls to our test sample methods
        case4HiddenInput(args.length, 10.0);
        case5HiddenInput(args.length, 10.0);
        case5_1HiddenInput(args.length, 10.0);
        case6HiddenInputArray(args.length, 10.0);
        case7HiddenInputAndInput(args.length, 10.0);
    }

    public static double getValue() {
        return 100.0;
    }


    @AdaptiveMethod
    @HiddenInput(expr="HiddenTest.getValue()")
    private static void case4HiddenInput(int VAR1, double VAR2) {
        System.err.println("Method");
    }

    @AdaptiveMethod
    @HiddenInput(expr="Runtime.getRuntime().availableProcessors()")
    private static void case5HiddenInput(int VAR1, double VAR2) {
        System.err.println("Single @HiddenInput for library");
    }

    @AdaptiveMethod
    @HiddenInput(expr="globalStr.size()")
    private static void case5_1HiddenInput(int VAR1, double VAR2) {
        System.err.println("Single @HiddenInput for global");
    }

    @AdaptiveMethod
    @HiddenInput(expr="globalStr.size()")
    @HiddenInput(expr="Runtime.getRuntime().availableProcessors()")
    private static void case6HiddenInputArray(int VAR1, double VAR2) {
        System.err.println("HiddenInputArray for library and global");
    }


    @AdaptiveMethod
    @Input(param="VAR2")
    @HiddenInput(expr="Runtime.getRuntime().availableProcessors()")
    private static void case7HiddenInputAndInput(int VAR1, double VAR2) {
        System.err.println("HiddenInputArray and Input for library and global");
    }
}
