# How to make programmable scripts

There are situations when you need to parametrize SQL scripts or create tables dynamically based on some input
variables. Darwin supports also this approach using [FreeMarker templating library](https://freemarker.apache.org/).
You'd need to manually add FreeMarker to your Maven or Gradle descriptor because Freemarker is optional library 
in Darwin.

``` xml
<dependency>   
    <groupId>org.freemarker</groupId>
    <artifactId>freemarker</artifactId>
    <version>2.3.28</version>
</dependency>
```

Or Gradle:

```
dependencies {
    compile 'org.freemarker:freemarker:2.3.28'
}
```

After linking FreeMarker library you'd need to reconfigure Darwin:

``` java
@Bean
public Darwin adam(ApplicationContext applicationContext) {
	final ScriptableResourceAccessor resourceAccessor = new ScriptableResourceAccessor(
			applicationContext, "utf-8", "classpath:/META-INF/adam/sq/"
	);
	final Map<String, Object> variables = new HashMap<>();
	variables.put("prefix", "SOME_PREFIX");
	variables.put("loop_count", 5);
	resourceAccessor.setVars(variables);
	return new DarwinBuilder(applicationContext, "adam", "1.0")
			.withResourceAccessor(resourceAccessor)
			.build();
}
```

... and then take advantage of the variables in your script:

``` sql
<#list 0..loop_count as iteration>
create table ${prefix}_ADAM_${(iteration_index + 1)?string} (
	id integer not null auto_increment,
	preciousContent varchar(255) not null,
	constraint CNPK_${prefix}_ADAM_${(iteration_index + 1)?string} primary key (id)
) engine=InnoDB;
</#list>
```

This setup will create 5 tables:

- SOME_PREFIX_ADAM_1
- SOME_PREFIX_ADAM_2
- SOME_PREFIX_ADAM_3
- SOME_PREFIX_ADAM_4
- SOME_PREFIX_ADAM_5