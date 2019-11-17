# Chameleon
[![Build Status](https://travis-ci.org/sothach/chameleon.svg?branch=master)](https://travis-ci.org/sothach/chameleon)
[![Coverage Status](https://coveralls.io/repos/github/sothach/chameleon/badge.svg?branch=master)](https://coveralls.io/github/sothach/chameleon?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2a3bec483a96489196102d5bfea2b8e2)](https://www.codacy.com/manual/sothach/chameleon?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=sothach/chameleon&amp;utm_campaign=Badge_Grade)


_Color mixer batch planning service_

## Documentation
1.  [Assigment](docs/task.md)
2.  [Approach](docs/approach.md)
3.  [Backlog](docs/backlog.md)
4.  [Solution](docs/solution.md)
5.  [Algorithm](docs/algorithm.md)
6.  [API Docs](docs/api.md)
7.  [Deploying](docs/deployment.md)


[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

Note: to use the deployed service, it will be necessary to add a config variable `APP_SECRET` (in the Settings tab), that will be used to verify the Json Web Token in eacxh request's Authorization header (Beare token).  Use the same secret to sign the JWT.  This project contains a CLI utility `security.TokenTool` to create a token from a secret key.
