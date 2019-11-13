# Sizing
## Request Sizes
None of our users produce more than 2000 different colors, or have more than 2000 customers
(1 <= N <= 2000 1 <= M <= 2000) 
The sum of all the T values for the customers in a request will not exceed 3000.
```
mixer-service.limits.max-colors=2000
mixer-service.limits.max-customers=2000
mixer-service.limits.t-max=3000
```