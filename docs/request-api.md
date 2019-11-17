# Request API

## Calculate optimum batch

Given a spec file consisting of customer requests, determine and answer with the best solution for a batch run

**URL** : `/api/v2/jobs/request`

**Method** : `POST`

**Auth required** : JWT

**Permissions required** : Role is Customer

**Data constraints**

Posted data meets specification

**Data example** 

```bash
curl -H "Accept: application/json" -H "Authorization: Bearer ...."  http://localhost:9000/api/v2/jobs/request -d ...
```

### Success Response

**Condition** : If solution calculated

**Code** : `200 SUCCESS`

**Content example**

```json
{"finishes":[1,0,0,0,0]}
```

### Error Responses

**Condition** : If empty data-set provided

**Code** : `400 BAD REQUEST`

**Content example** : `no data provided`

**Condition** : Request is valid but no solution could be formed

**Code** : `422 UNPROCESSABLE ENTITY`

**Condition** : Missing or invalid JWT token

**Code** : `401 UNAUTHORIZED`

**Condition** : Authentication token is invalid or expired

**Content example** : `IMPOSSIBLE`