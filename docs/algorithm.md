# Algorithm:
1.  Naive 4-liner (recursive symmetric difference against optimal run): elegant but wrong - failed to find solution in 
many cases
2.  Python -> Scala, ok, but full of mutable state and loops
3.  Revised (2) based on a Github find (https://github.com/engineerlondon/Paint-Batch-Optimiser), again, very stateful, 
but interesting use of permutations to seek a solution.  
Probably would not scale well as likely to exhibit big Big-O numbers (maybe O(n^2 * n!)?); This optimizer specifies
a hard-coded 3000 t-value limit as a precondition, to prevent long processing times / errors
4.  For a production service, the notion of hard-coding knowledge of what customers prefer in this optimisation might 
not be the best: different customers will most like have differing preferences, and a dynamic data-driven approach 
would be more appropriate (also able to factor in things like paint availability, production times, etc.)