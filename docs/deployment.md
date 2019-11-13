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

## Heroku Deploy

### Set-up
```% heroku stack:set container```
```% bgit push heroku master```
```% heroku container:push web```

### Test
#### Request
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