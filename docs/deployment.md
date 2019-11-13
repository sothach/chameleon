# Deployment
## Local
### Prerequisites
An accessible PostgreSql server, e.g.,
'% docker run --rm -p 5434:5432 --name postgres -e POSTGRES_PASSWORD=postgres -d postgres'
### Dev mode
`% sbt ~run`

```bash
% docker exec -it postgres /bin/bash
% psql -h localhost -U postgres
postgres=# create database jobsdb;
```

```bash
% docker stop postgres
```

## Add application secret to cloud environment
```bash
APPLICATION_SECRET=`sbt playGenerateSecret`
```
E.g., locally:
```bash
% docker run -p 9000:9000 -e play.http.secret.key=$APPLICATION_SECRET dscr.io/sothach/chameleon:latest
```