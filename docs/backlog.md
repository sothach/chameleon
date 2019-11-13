# Backlog

| Item | done | Name | Description | Estimate (hours) | 
| ---- | ---- | ---- | ----------- | ---------------- | 
| 1 |✅| Set-up | Clone giter8 Play framwork template, minimize for API use, create Github repo | 1  | 
| 2 |✅| Job management | Create job-management service, persistence, to store and retrieve user's requests | 2 | 
| 3 |✅| Mixer function | Write/re-write color mix function, to efficiently pass supplied tests | 2 | 
| 4 |✅| API (legacy) | Expose mixer function from existing API | 1 |
| 5 |  | API (evolution) | Create target API, add job retrieval, 301 re-direct for legacy API | 2 |  
| 6 |  | Authentication | Require valid JWT with user email and role, base API access on this | 1 | 
| 7 |  | Metrics | Add metrics endpoint for monitoring purposes | 1 |
| 8 |  | Deployment | Dockerize, create Heroku project, provision with database and logging, metrics | 1 |
| 9 |  | CI/CD pipeline | Create build, coverage and code quality pipeline, add deployment | 2 |

# Definition of done
*  Service automatically building
*  100% test coverage
*  No major code quality issues reported
*  Design documented
*  API documented
*  Stateless service: can be horizontally-scaled