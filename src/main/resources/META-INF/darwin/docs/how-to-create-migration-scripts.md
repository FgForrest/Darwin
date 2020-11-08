# How to create migration scripts

## How to create model in the database

First [integrate Darwin to your project](how-to-integrate-to-your-project.md) and create `create.sql` SQL script 
in a platform specific folder. Let's have new component `adam` with initial version `1.0`:

### 1. Create initial setup script

Create a file on your classpath `/META-INF/adam/sql/mysql/create.sql` with example content:

``` sql
create table ADAM (
	id integer not null auto_increment,
	preciousContent varchar(255) not null,
	constraint CNPK_ADAM primary key (id)
) engine=InnoDB;
```

### 2. Make it live with Darwin

Create Darwin bean in your Spring configuration:

``` java
@Configuration
@Import(DarwinConfiguration.class)
public class YourNameOfConfigFile {

    @Bean
    public Darwin adamSchema(ApplicationContext applicationContext) {
        return new DarwinBuilder(applicationContext, "adam", "1.0")              
                .withResourcePath("classpath:/META-INF/adam/sql/")
                .build();
    }

}
```

... and you're done - Darwin creates `ADAM` table for you once Spring tries to instantiate `adamSchema` bean. If you 
require `ADAM` table to exist in order to be used in other bean setup you should force `Darwin` bean to instantiate first
using Spring [DependsOn](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/DependsOn.html)
annotation.

## How to upgrade existing model in database

### 1. Create patch script

Create a file on your classpath `/META-INF/adam/sql/mysql/patch_1.1.sql` with example content:

``` sql
alter table ADAM add column additionalContent varchar(64) null;
```

### 2. Make it live with Darwin

Update version in your Darwin bean declaration in your Spring configuration:

``` java
@Configuration
@Import(DarwinConfiguration.class)
public class YourNameOfConfigFile {

    @Bean
    public Darwin adamSchema(ApplicationContext applicationContext) {
        return new DarwinBuilder(applicationContext, "adam", "1.1")              
                .withResourcePath("classpath:/META-INF/adam/sql/")
                .build();
    }

}
```

... and you're done - once `Darwin` bean is instantiated again, it finds out, that version of the model is `1.1`, but 
there is older version (`1.0`) in the database and it automatically finds and applies `patch_1.1.sql` script. When it's
finished it writes down that database now contains model with version `1.1`.