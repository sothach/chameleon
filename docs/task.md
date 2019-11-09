First of all, we expect you to implement only one or two of the tasks, although there are many
options. Since this is an interview question, you should pick an area where you can demonstrate
your skills and present how you apply best practices. We expect that the implemented parts are
production ready. If this is not the case, please, explain the underlying reasons. We believe that
it is important to identify what are your strongest skills, so we came up with an end-to-end
problem that gives opportunity for engineers with different background to show their capabilities.

Imagine that your team have to take over a service. This is service is up and running, but you
have no idea how that service was deployed. (It seems that someone simply copied the code to
the server.) You identified a repository that contains the code:
[technical challenge](https://github.com/AYLIEN/technical_challenge) Just for clarification, the code we provide for this
challenge is intentionally crap and not reusable. Even if you would like to implement your
solution in python, you must want to write a new service. Nevertheless, you can leave it as it is if
you want to focus on non-coding tasks, e.g. scaling. For now, just assume that this is a legacy
code. This might be the right time to take a look at the code itself before you continue.

We would like to improve this service.

*  We would like a solution that is stable, secure and maintainable.
*  We would like to have monitoring that helps to identify issues.
*  We would like a production ready HTTP server and a good API.
*  We would like to introduce authentication, and provide users a history of their requests.
*  Backward compatibility is crucial for our customers.
*  We would like an efficient (fast) algorithm.
*  We would like to deploy this service to a cloud provider, and automate the deployment.
*  We would like to introduce some self-healing capabilities, e.g. restart after a crash.
*  We would like to horizontally scale this app. How would you integrate the app with an
auto-scaling solution, and how would you choose the scaling rules?

We would like you to identify tasks that are required, provide a time estimation for all of these
tasks before you start working on them. If you can split these tasks into smaller items, also
share those, please. It’s up to you how much time you invest, but keep in mind that we expect
some meaningful contribution and also that it should be possible to review your solution in a
reasonable amount of time.Your solution is expected as a zip file, but please include your .git
directory, we are interested in the steps of your development, not just the end result.

We forged this exercise with great care and enthusiasm. Even if you decide not to submit your
solution, we appreciate your feedback. What was the most enjoyable? What was unreasonably
annoying? Let us know, so we can improve.