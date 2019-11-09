-- Users schema

-- !Ups

CREATE TABLE job(
   job_id SERIAL NOT NULL PRIMARY KEY,
   user_email TEXT NOT NULL,
   request TEXT DEFAULT NULL,
   status TEXT DEFAULT 'Created',
   created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   version INTEGER NOT NULL DEFAULT 0
);
INSERT INTO job(user_email,request) VALUES ('test@mail.com','paint me');

-- !Downs

DROP TABLE Job;