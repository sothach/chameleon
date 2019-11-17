# Deployment
## Local
### Prerequisites
An accessible PostgreSql server, e.g.,
`% docker run --rm -p 5434:5432 --name postgres -e POSTGRES_PASSWORD=postgres -d postgres`
If required, create the database:
```
% docker exec -it postgres /bin/bash
% psql -h localhost -U postgres
postgres=# create database jobsdb;
```

### Run/Test in Dev mode
`% sbt ~run`
The API is now available on `localhost:9000` and can be exercised by, e.g., running some curl requests (see` ./scripts`)

Cleanup
```bash
% docker stop postgres
```

## Run as docker image
### Build image
`% sbt docker:publish`

```bash
APPLICATION_SECRET=`sbt playGenerateSecret`
```
E.g., locally:
```bash
% docker run -p 9000:9000 -e play.http.secret.key=$APPLICATION_SECRET dscr.io/sothach/chameleon:latest
```

## Heroku Deploy

### Set-up
```% sbt docker:publish```
```% heroku stack:set container```
```% git push heroku master```
```% heroku container:push web```

## Test
### Generate JWT
Run the TokenTool to create a valid JWT for the user, specifying email address, user role and expiry date:
`% sbt "runMain security.TokenTool secret-key user@mail.com Admin 2020-12-12T12:33:45"`

```bash
** Generate a JWT token for API Authorization ***
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InVzZXJAbWFpbC5jb20iLCJyb2xlIjoiQWRtaW4iLCJleHAiOjE2MDc3NzY0MjV9.wAjDnMPRY-2GLKZ3MDwK_y1-b5VoKeVT_eRIxoIP6Ts

```
### Request
```{"colors":2,"customers":2,"demands":[[1,1,0],[1,1,0]]}```
```
% curl -H "Accept: text/plain" https://kid-chameleon.herokuapp.com/v1/?input=\
%7B%22colors%22%3A5%2C%22customers%22%3A3%2C%22demands%22%3A%5B%5B1%2C1%2C1%5D%2C%5B2%2C1%2C0%2C2%2C0%5D%2C%5B1%2C5%2C0%5D%5D%7D%20
1 0 0 0 0
```
```{"colors":1,"customers":2,"demands":[[1,1,1],1,1,0]]} ```
```
% curl -H "Accept: text/plain" https://kid-chameleon.herokuapp.com/v1/?input=\
%7B%22colors%22%3A1%2C%22customers%22%3A2%2C%22demands%22%3A%5B%5B1%2C1%2C1%5D%2C%5B1%2C1%2C0%5D%5D%7D%20
IMPOSSIBLE
```

### Query Database
```
jobsdb=# select * from job;
 job_id |       user_email       |                              request                               |          result          |  status   |         created         | version 
--------+------------------------+--------------------------------------------------------------------+--------------------------+-----------+-------------------------+---------
      1 | strummer.joe@gmail.com | {"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}             |                          | Failed    | 2019-11-17 13:13:05.931 |       1
      2 | strummer.joe@gmail.com | {"colors":5,"customers":3,"demands":[[1,1,1],[2,1,0,2,0],[1,5,0]]} | {"finishes":[1,0,0,0,0]} | Completed | 2019-11-17 13:14:07.807 |       1
      3 | test@mail.org          | {"colors":5,"customers":3,"demands":[[1,1,1],[2,1,0,2,0],[1,5,0]]} | {"finishes":[1,0,0,0,0]} | Completed | 2019-11-17 13:20:01.801 |       1
(3 rows)
```