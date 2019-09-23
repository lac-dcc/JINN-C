package org.renaissance;
// JINN Project
// Author: Junio

import java.util.Vector;

public class BenchmarkInputs {
	private int numThreads; // general
	private int numRatings; // als
	private int requestCount; // FinagleChirper
	private int userCount; // FinagleChirper
	private int numRequests; // FinagleHttp
	private int numClients; // FinagleHttp
	private int numMeals; // Philosophers
	private int numCopies; // dectree
	private int chromoCount; // future-genetic
	private int genCount; // future-genetic
	private int size; // chisquare

	private static BenchmarkInputs singleton = null;
	private static boolean enableDebug = true;

	private BenchmarkInputs () {
		this.numThreads = Runtime.getRuntime().availableProcessors();
		this.numRatings = 20000;
		this.requestCount = 1250;
		this.userCount = 5000;
		this.numMeals = 500000;
		this.numCopies = 100;
		this.genCount = 5000;
		this.chromoCount = 50;
		this.size = 1500000;
		this.numRequests = 2000000;
		this.numClients = 20;
	} 

	private static void init() {
		if (singleton == null) {
			singleton = new BenchmarkInputs();
		}
		debug("Running renaissance with GTS structure");
	}

	public static String[] parse (String[] args) {
		init();
		Vector<String> vecArgs = new Vector<String>();
		for (int i = 0; i < args.length; i++) {
			String token = args[i];
			//
			// assumes the following arg is not the last one in the cmd line
			if (token.equals("--als-threads")) {	
				singleton.numThreads = Integer.parseInt(args[++i]);			
			} else if (token.equals("--als-ratings")) {
				singleton.numRatings = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.numRatings + " ratings");		
			} else if (token.equals("--threads")) {	
				singleton.numThreads = Integer.parseInt(args[++i]);
			} else if (token.equals("--finaglechirper-usercount")) {
				singleton.userCount = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.userCount + " users");		
			} else if (token.equals("--finaglechirper-requestcount")) {				
				singleton.requestCount = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.requestCount + " requests");		
			} else if (token.equals("--philosophers-meals")) {				
				singleton.numMeals = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.numMeals + " meals");					
			} else if (token.equals("--dectree-copies")) {				
				singleton.numCopies = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.numCopies + " copies");					
			} else if (token.equals("--futuregenetic-generations")) {				
				singleton.genCount = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.genCount + " generations");					
			} else if (token.equals("--futuregenetic-chromosomes")) {				
				singleton.chromoCount = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.chromoCount + " chromosomes");					
			} else if (token.equals("--chisquare")) {				
				singleton.size = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.size + " chisquare size");					

			} else if (token.equals("--finaglehttp-requests")) {				
				singleton.numRequests = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.numRequests + " requests");
			} else if (token.equals("--finaglehttp-clients")) {				
				singleton.numClients = Integer.parseInt(args[++i]);
					debug ("Processing workload with " + singleton.numClients + " clients");

			} else {
				vecArgs.add(token);
			}
		}
		debug ("Processing workload with " + singleton.numThreads + " threads");		
		return vecArgs.toArray(new String[vecArgs.size()]);
	}    

    private static void debug (String msg) {
      if (! enableDebug) return;
      else {
        System.out.println("\t:: DEBUG :: " + msg);
      }
    }


    // ========= getters ========

    public static int getUserCount () {
    	init();
    	return singleton.userCount;
    }

    public static int getRequestCount () {
    	init();
    	return singleton.requestCount;
    }

	public static int getNumThreads () {
		init();
		return singleton.numThreads;
	}

	public static int getNumRatings () {
		init();
		return singleton.numRatings;
	}	

	public static int getNumMeals () {
		init();
		return singleton.numMeals;
	}

	public static int getNumCopies () {
		init();
		return singleton.numCopies;
	}

	public static int getChromoCount () {
		init();
		return singleton.chromoCount;
	}

	public static int getGenCount () {
		init();
		return singleton.genCount;
	}

	public static int getSize() {
		init();
		return singleton.size;
	}

	public static int getNumClients () {
		init();
		return singleton.numClients;
	}

	public static int getNumRequests () {
		init();
		return singleton.numRequests;
	}
}
