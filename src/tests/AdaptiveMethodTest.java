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

/**
 * Class for testing default @AdaptiveMethod annotation. All parameters
 * of the sample method should be parsed and have their values extracted.
 * @author juniocezar
 */
public class AdaptiveMethodTest {
	static List<String> globalStr;

	public static void main (String[] args) {
		globalStr = new ArrayList<String>();
		globalStr.add("10");
		globalStr.add("20");

        // should work with all parameters
		sample(args, 10.0, 1000, globalStr);

        // should work with only the second parameter
        sample2(false, 10.0, new AdaptiveMethodTest());
	}

	@AdaptiveMethod
	private static void sample(String[] VAR1, double VAR2, Integer VAR3,
                                                    List<String> VAR4) {
		System.err.println("Testing @AdaptiveMethod with Array, "
                        + "Primive, Primitive Wrapper and Collection");
	}

    @AdaptiveMethod
	private static void sample2(boolean VAR1, double VAR2,
                                                    AdaptiveMethodTest VAR3) {
		System.err.println("Testing @AdaptiveMethod with boolean, "
                        + " double and non-compatible class");
	}
}
