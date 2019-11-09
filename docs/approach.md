# Approach to solution

## Objective
From an initial look at the existing solution, and considering the requirements (below), plan is to re-write the service.

## Scope
Re-writing will entail re-creating the existing API and re-implementing the color mix function.  To make this work easier and
to facilitate on-going testing and deployment, a stand-alone service will be created.  Given free choice of framework
and cloud platform, many of the requirements listed can be met out of the box, so all of the requirements will be
addressed, in a succinct manner, not to burden the reviewer unduly.

## Verifying the solution
The existing code-base contains test-cases, so these should be reviewed for correctness and potentially re-used. 
Considering point(5) below, it is assumed that these tests do in fact represent the current behaviour of the system,
whether they are correct and comprehensive or not.

## Requirements
1.  A solution that is stable, secure and maintainable.
2.  Have monitoring that helps to identify issues.
3.  A production ready HTTP server and a good API.
4.  To introduce authentication, and provide users a history of their requests.
5.  Backward compatibility is crucial for our customers.
6.  An efficient (fast) algorithm.
7.  To deploy this service to a cloud provider, and automate the deployment.
8.  To introduce some self-healing capabilities, e.g. restart after a crash.
9.  To horizontally scale this app. How would you integrate the app with an auto-scaling solution, 
and how would you choose the scaling rules?
