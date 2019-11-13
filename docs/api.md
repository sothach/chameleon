## API

### Calculate optimum batch

Given a spec file consisting of customer requests, determine and answer with the best solution for a batch run

**URL** : `/api/mixit`

**Method** : `POST`

**Auth required** : JWT

**Permissions required** : None

**Data constraints**

Posted data meets specification

**Data example** 

```bash
15
1
2
1 1 0
1 1 1
5
3
```

## Success Response

**Condition** : If solution calculated

**Code** : `200 SUCCESS`

**Content example**

```bash
Case #1: 1 0 0 0 0
```

## Error Responses

**Condition** : If empty data-set provided

**Code** : `400 BAD REQUEST`

**Content example** : `no data provided`

**Condition** : Request is valid but no solution could be formed

**Code** : `422 UNPROCESSABLE ENTITY`

**Content example** : `no solution`