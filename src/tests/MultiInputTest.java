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

import java.util.*;

public class MultiInputTest {
	public static int num = 0;

    public static void main (String[] args) {
        if (args.length < 3) {
            System.err.println("This test needs 3 int parameters");
            System.exit(1);
        }

        adaptive(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        
        num = Integer.parseInt(args[0]);
        globalAdaptive(Integer.parseInt(args[1]));

    }

    // should consider both the parameter a and b
    @AdaptiveMethod
    @Input(param="a")
    @Input(param="b")
    public static void adaptive (int a, int b, int c) {
        for (int i = 0; i < a; i++) {
            b += c;
        }
        System.err.println(b);
    }

    // should consider both global num and parameter c
    @AdaptiveMethod
    @Input(param="num")
    @Input(param="c")
    public static void globalAdaptive (int c) {
        for (int i = 0; i < num; i++) {
            c++;
        }
        System.err.println(c);
    }
}
