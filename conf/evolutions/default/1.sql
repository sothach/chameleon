-- Users schema

-- !Ups

CREATE TABLE job(
   job_id SERIAL PRIMARY KEY,
   user_email TEXT NOT NULL,
   request TEXT NOT NULL,
   result TEXT DEFAULT NULL,
   status TEXT DEFAULT 'Created',
   created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   version INTEGER NOT NULL DEFAULT 0
);

-- !Downs

DROP TABLE Job;