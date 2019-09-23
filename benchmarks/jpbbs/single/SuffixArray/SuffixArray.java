import java.util.ArrayList;
import java.util.Arrays;

public class SuffixArray implements Runnable {
    private ArrayList<Integer> threadList;
    private int threadSplitDivisor;
    private String threadInput;
    private int nthreads;

    public SuffixArray() {}
    public SuffixArray(ArrayList<Integer> l, int div, String in, int NT) {
        threadList = l;
        threadSplitDivisor = div;
        threadInput = in;
        nthreads = NT;
    }

	public ArrayList<Integer> buildSuffixArray(String input, int NT)
        throws InterruptedException {

		ArrayList<Integer> ARR = new ArrayList<Integer>();
		ArrayList<Integer> A = new ArrayList<Integer>();
		ArrayList<Integer> C = new ArrayList<Integer>();
		ArrayList<Integer> G = new ArrayList<Integer>();
		ArrayList<Integer> T = new ArrayList<Integer>();
		ArrayList<Integer> S = new ArrayList<Integer>();

        int len = input.length() - 1;
		for(int i = 0; i < len; i++) {
		    if(input.charAt(i)=='A' || input.charAt(i)=='a'){
		    	A.add(i);
		    }else if(input.charAt(i)=='C' || input.charAt(i)=='c') {
		    	C.add(i);
		    }else if(input.charAt(i)=='G' || input.charAt(i)=='g') {
		    	G.add(i);
		    }else if(input.charAt(i)=='T' || input.charAt(i)=='t') {
		    	T.add(i);
		    }else if(input.charAt(i)=='$') {
		    	S.add(i);
		    }
		}


		A = parallelSort(A, 2, input, NT);
		C = parallelSort(C, 2, input, NT);
		G = parallelSort(G, 2, input, NT);
		T = parallelSort(T, 2, input, NT);

/*
		A = sort(A, 2, input);
		C = sort(C, 2, input);
		G = sort(G, 2, input);
		T = sort(T, 2, input);
*/

		ARR.addAll(S);
		ARR.addAll(A);
		ARR.addAll(C);
		ARR.addAll(G);
		ARR.addAll(T);

		return ARR ;
	}

	public String RANDOMIZE(int n) {
		return "";
	}

    @Override
    public void run() {
      try {
        this.threadList = parallelSort(this.threadList, this.threadSplitDivisor, 
                            this.threadInput, this.nthreads);
     } catch (InterruptedException e) {
         e.printStackTrace();
      }
    }

    public ArrayList<Integer> getList() {
        return this.threadList;
    }


    public ArrayList<Integer> parallelSort(ArrayList<Integer> list, int splitDivisor, String input,
            int cores) throws InterruptedException {
        if (cores <= 1) {
            return sort(list, splitDivisor, input);
        } else {
    		ArrayList<Integer> list1 = new ArrayList<Integer> (list.subList(0, list.size()/splitDivisor));
    		ArrayList<Integer> list2 = new ArrayList<Integer> (list.subList((list.size()/splitDivisor), list.size()));

            SuffixArray leftSorter = new SuffixArray(list1, splitDivisor, input, cores / 2);
            SuffixArray rightSorter = new SuffixArray(list2, splitDivisor, input, cores / 2);

            Thread leftThread = new Thread(leftSorter);
            Thread rightThread = new Thread(rightSorter);
            leftThread.start();
            rightThread.start();
            leftThread.join();
            rightThread.join();

    		return merge(leftSorter.getList(), rightSorter.getList(), input);
        }
    }

	public ArrayList<Integer> sort(ArrayList<Integer> list, int splitDivisor, String input)
    {
    	if(list.size() <= 1) {
    		return list;
    	}else {
    		ArrayList<Integer> list1 = new ArrayList<Integer> (list.subList(0, list.size()/splitDivisor));
    		ArrayList<Integer> list2 = new ArrayList<Integer> (list.subList((list.size()/splitDivisor), list.size()));

    		list1 = sort(list1, splitDivisor, input);
    		list2 = sort(list2, splitDivisor, input);

    		return merge(list1, list2, input);
    	}

    }

	private ArrayList<Integer> merge(ArrayList<Integer> list1, ArrayList<Integer> list2, String input) {

		ArrayList<Integer> merged = new ArrayList<Integer>();
		int i = 0, j = 0;

		while (i < list1.size() && j < list2.size()){

			if (input.charAt(list1.get(i)+1) < input.charAt(list2.get(j)+1)) {
				merged.add(list1.get(i));
				i++;
			} else if (input.charAt(list1.get(i)+1) > input.charAt(list2.get(j)+1)) {
				merged.add(list2.get(j));
				j++;
			} else if (input.charAt(list1.get(i)+1) == input.charAt(list2.get(j)+1)){
				if(checkEqual(list1.get(i)+1, list2.get(j)+1, input) == list1.get(i)) {
					merged.add(list1.get(i));
					i++;
				}else {
					merged.add(list2.get(j));
					j++;
				}
			}
	    }

	   while (i < list1.size()) {
			merged.add(list1.get(i)) ;
			i++;
		}


		while (j < list2.size()) {
			merged.add(list2.get(j));
			j++;
    	}
	    return merged;
	}

	private int checkEqual(int l1, int l2, String input) {
		int a = l1;
		int b = l2;
		int ans = 0;

        int len = input.length();

		while(input.charAt(a) == input.charAt(b)) {
			a++;
			b++;
            if (a >= len || b >= len) {
                a--;
                b--;
                break;
            }
		}


		if(input.charAt(a) < input.charAt(b)) {
			ans = l1;
		}else {
			ans= l2;
		}

		return ans;
	}
}
