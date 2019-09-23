package org.renaissance;

public class EmbeddedSystem {
    //
    // false = measurement disabled
    // true  = measurement enabled
    private static boolean niStatus = false;

    private static boolean measurementEnabled = true;

    private static String port = "31";


    /**
     * Sends the new state to the measuremnt device
     * @param state New state to be sent to the measurement device. Possible 
     * values are: 0 and 1.
     * @return Success of operation.
     */
    private static boolean changeMeterState(String state) {
        //
        // State 0 will enable energy measurement
        // State 1 will disable it
        String command = "echo " + state + " > /sys/class/gpio/gpio" + port + "/value";
        ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
        try {
           Process p = pb.start();
           // running command
           if (p.waitFor() != 0) {
               System.err.println("lac.DVFS: Error while update GPIO state to 31");
               return false;
           }
       } catch (Exception e) {}
       return true;
    }

    //
    // return current timestamp in nanoseconds
    // and change measurement device status
    public static long measureNano () {
        long timestamp;

        if (!niStatus) {
            //
            // if we are not measuring yet, we want to perform the
            // systemcall first and get the timestamp later, as
            // the time spent in the system call must not be 
            // included in the benchmarks execution time
            changeMeterState("0");
            System.err.println(". . . Energy Measument enabled . . . : Signal (" + measurementEnabled + ")" );  
            timestamp = System.nanoTime();
        } else {
            // if we are already measuring execution time then we 
            // collect the timestamp first and switch of the monitor
            // later, in order to not include the system call time 
            // into the benchmarks execution time.
            timestamp = System.nanoTime();
            changeMeterState("1");
            System.err.println(". . . Energy Measument disabled . . . : Signal (" + measurementEnabled + ")" ); 
        }
        return timestamp;
    }

	public static void startEnergyMeasurement() {
            changeMeterState("0");
            System.err.println(". . . Energy Measurement started . . ."); 
	}

	public static void stopEnergyMeasurement() {
            changeMeterState("1");
            System.err.println(". . . Energy Measurement stoppped" ); 
	}

    public static void enableEnergyMeasurement () {
        measurementEnabled = true;
    }


    public static long nanoTime () {
        return System.nanoTime();
    }
}
