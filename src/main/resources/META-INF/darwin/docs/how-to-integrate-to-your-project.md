# How to integrate Darwin to your project

Include Darwin library in your Maven descriptor (`pom.xml`):

``` xml
<dependency>   
    <groupId>one.edee.oss</groupId>   
    <artifactId>darwin</artifactId>   
    <version>1.0.0</version>
</dependency>
```

Or Gradle:

```
dependencies {
    compile 'one.edee.oss:darwin:1.0.0'
}
```

## Use in your Spring Java Configuration

Declare Darwin bean in your Spring Java Config:

``` java
@Configuration
@Import(DarwinConfiguration.class)
public class YourNameOfConfigFile {

    @Bean
    public Darwin nameOfYourSchemaComponent(ApplicationContext applicationContext) {
        return new DarwinBuilder(applicationContext, "name_of_your_component", "1.0")              
                .withResourcePath("classpath:/META-INF/name_of_your_component/sql/")
                .build();
    }

}
```

The path to the resource folder expects another subfolder (or more subfolders for different platforms) specifying 
the platform the scripts are aimed for. In above example MySQL migration scripts would be placed in the folder: 
`/META-INF/name_of_your_component/sql/mysql/`.