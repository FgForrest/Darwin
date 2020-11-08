# How to introduce Darwin to existing database

You can also use `Darwin` in projects where a data structure already exists. By default, you create **create.sql**
script and patch set as if the database were empty (see [documentation](how-to-create-migration-scripts.md)). 
Then add one or more **guess_version.sql** scripts, which Darwin uses to determine the version of an existing data structure.

The detection procedure is as follows:
 
- in case that Darwin does not have an entry in its internal table about existence of the component it'll try to find 
  all scripts starting with `guess_` word before executing any of `create.sql` or `patch_*.sql` scripts 
  (eg. `guess_1.0.sql`, `guess_2.1.0.sql`, etc.)
- it starts executing them in order from the lowest version to the highest version
- last script, which does not finish with an exception (or if the guess contents starts with `select count` and returns 
  a number greater than 0) will be considered determines the version of model existing in the database
- version number is parsed from the `guess_(.*?).sql` file name and stored to Darwin internal table specifying
  component version to start with

If even first `guess` script fails with exception or returns number equal to zero, Darwin creates model with `create.sql`
script.

This way, you can easily connect to an existing data structure.

## Examples of the guess scripts

### 1. Use SQL query that must finish without exception in case model is up-to-date

Check existence of some table that was created for specific model version:

```
select * from SOME_TABLE
```

If table doesn't exist it means, that model of requested version doesn't exist in the database.

### 2. Use SQL count query that must return result greater that one

Check existence of some record that was inserted in specific model version:

```
select count(0) from SOME_TABLE where col = 'value';
```

If statement returns `1` it means that record is already in the table and patch with this version was already
applied, if it returns `0` it means that Darwin would need to apply the patch now.

You can also query database infrastructural tables in the guess script - for example:

```
SELECT count(0) 
FROM information_schema.tables
WHERE table_schema = 'test' 
    AND table_name = 'testtable'
```