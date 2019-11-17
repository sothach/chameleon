# Management API

## List job history

Return a list of jobs run, either for a specified user (parameter `email` supplied), or own jobs (role is Customer, no
parameter supplied / used), or all users jobs (role is Admin)

**URL** : `/api/v2/jobs/list`

**Method** : `GET`

**Auth required** : JWT

**Permissions required** : Role is *Admin* (_to query other user's jobs_), Role is *Customer* (_to query own jobs_)

**Data constraints**

Posted data meets specification

**Data example** 

```bash
curl -H "Accept: application/json" -H "Authorization: Bearer ...."  http://localhost:9000/api/v2/jobs/list?strummer.joe@gmail.com
```

### Success Response

**Condition** : Jobs listed

**Code** : `200 SUCCESS`

**Content example**

```json
[
  {
    "userEmail": "strummer.joe@gmail.com",
    "request": {
      "colors": 5,
      "customers": 3,
      "demands": [
        [1, 1, 1],
        [2, 1, 0, 2, 0],
        [1, 5, 0]]
    },
    "result": {
      "finishes": [1, 0, 0, 0, 0]
    },
    "created": "2019-11-17T16:18:15.908",
    "status": "Completed",
    "jobId": 1,
    "version": 1
  },
  {
    "userEmail": "strummer.joe@gmail.com",
    "request": {
      "colors": 5,
      "customers": 3,
      "demands": [
        [1, 1, 1], 
        [2, 1, 0, 2, 0],
        [1, 5, 0]]
    },
    "result": {
      "finishes": [1, 0, 0, 0, 0]
    },
    "created": "2019-11-17T16:18:21.492",
    "status": "Completed",
    "jobId": 2,
    "version": 1
  }
]
```

### Error Responses

**Condition** : Missing or invalid JWT token

**Code** : `401 UNAUTHORIZED`

**Condition** : Authentication token is invalid or expired
