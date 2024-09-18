-- ------------------------------------------------------------------------------------------
-- Revoke script for Migrating Spring batch version to 5.0 as part of Java 21 Migration.
-- ------------------------------------------------------------------------------------------
ALTER TABLE BATCH_STEP_EXECUTION DROP CREATE_TIME TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:00';
ALTER TABLE BATCH_STEP_EXECUTION ALTER COLUMN START_TIME ADD NULL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ADD COLUMN DATE_VAL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ADD COLUMN LONG_VAL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ADD COLUMN DOUBLE_VAL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN TYPE_CD TYPE VARCHAR(6);
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS RENAME PARAMETER_TYPE TO TYPE_CD;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN KEY_NAME TYPE VARCHAR(100);
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS RENAME PARAMETER_NAME TO KEY_NAME;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN STRING_VAL TYPE VARCHAR(250);
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS RENAME PARAMETER_VALUE TO STRING_VAL;
ALTER TABLE BATCH_JOB_EXECUTION ADD COLUMN JOB_CONFIGURATION_LOCATION;

DROP SEQUENCE BATCH_STEP_EXECUTION_SEQ;
DROP SEQUENCE BATCH_JOB_EXECUTION_SEQ;
DROP SEQUENCE BATCH_JOB_SEQ;