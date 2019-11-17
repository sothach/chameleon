# Legacy API

## Calculate optimum batch

Given a spec file consisting of customer requests, determine and answer with the best solution for a batch run

**URL** : `/v1/`

**Method** : `GET`

**Auth required** : JWT

**Permissions required** : Role is Customer

**Data constraints**

Posted data meets specification

**Data example** 

```{"colors":2,"customers":2,"demands":[[1,1,0],[1,1,0]]}```
```bash
curl -H "Accept: text/plain" -H "Authorization: Bearer ....." \
http://localhost:9000/v1/?input=%7B%22colors%22%3A5%2C%22customers%22%3A3%2C%22demands%22%3A%5B%5B1%2C1%2C1%5D%2C%5B2%2C1%2C0%2C2%2C0%5D%2C%5B1%2C5%2C0%5D%5D%7D%20
```

### Success Response

**Condition** : If solution calculated

**Code** : `200 SUCCESS`

**Content example**

```bash
1 0 0 0 0
```

### Error Responses

**Condition** : If empty data-set provided

**Code** : `400 BAD REQUEST`

**Content example** : `no data provided`

**Condition** : Request is valid but no solution could be formed

**Code** : `422 UNPROCESSABLE ENTITY`

**Content example** : `IMPOSSIBLE`

**Condition** : Missing or invalid JWT token

**Code** : `401 UNAUTHORIZED`

**Condition** : Authentication token is invalid or expired