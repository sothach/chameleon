# Solution Description
This section describes the elements of the solution, both frameworks used and rationale, and design and coding
decisions

## Play Framework


## PostgreSql
The storage requirements of the solution are straight-forward: a log of each job requested is kept, including 
the requesting customer, the request and the date and time of the request.  The outcome of the attempt to 
optimize will also be recorded: success or failure, and in the former case, the solution returned.

## Akka-streams & Processing pipeline
Based on the the 'pipes & filters' enterprise pattern, the implementation of the solution can be visualized
by the following flow:

```
  +---------+    +--------+    +-------+    +-------+    +--------+
  | Request | => | Verfiy | => | Start | => | Solve | => | Finish |
  +---------+    +--------+    +-------+    +-------+    +--------+
```
1.  Request
2.  Verify
3.  Start
4.  Solve
5.  Finish

Using a 'white-board-to-code' approach, this flow is implemented verbatim by the line below:
```
    source.async via verifyRequest via start via solve via finish runWith Sink.seq
```
The framework, providing execution context and a supervisor removes the essential yet distracting
matters of error handling and concurrency from the application code.

The approach taken here is to 'flow' each request through the pipeline from start to finish, using Scala's `Try`
container as the processing frame.  If any stage fails, the failure is passed thru and returned, when the client
is free to interpret the meaning of failure, for example, as a 4xxx or 5xx response code.

Although this is only used for single requests at a time, the pipeline is capable of stream processing, unchanged.

## Domain modelling / DSL

## Security



