# Integrace do projektu

Pro přilinkování knihovny stačí abyste do svého pom.xml přidali následující fragment:

``` xml
<dependency>   
    <groupId>com.fg</groupId>   
    <artifactId>darwin</artifactId>   
    <version>4.0.0</version>
</dependency>
```

## Java config varianta

Pokud používáte JavaConfig konfiguraci Springového kontextu, můžete integraci DbAutoUpdater provést takto:

``` java
@Configuration
@Import(AutoUpdaterConfiguration.class)
public class StableConfiguration {

    @Bean
    public AutoUpdater getExamAutoUpdater(@Autowired ApplicationContext applicationContext) {
        return new AutoUpdaterBuilder(applicationContext, "id_komponenty", "1.0")
                .withSkipIfDataSourceNotPresent(true)
                .withResourcePath("classpath:/META-INF/slozka/sql/")
                .build();
    }

}
```

_Cesta k resource složce očekává ještě jednu (případně více podsložek) s určením platfomy, uvedený příklad tedy hledá 
SQL soubory na cestě `/META-INF/slozka/sql/mysql/`_

## XML varianta

Následně už stačí jen do Spring konfiguračního souboru vložit:

``` xml
<!-- import bean knihovny - zajistí setup infrastruktury pro fungování knihovny --> 
<import resource="classpath:/META-INF/darwin/spring/db-autoupdate-config.xml"/> 

<!-- TOTO JE JIŽ KONFIGURACE VAŠEHO PROJEKTU --> 
<!-- konfigurace třídy, která zajistí vlastní vytvoření / patch vaší databáze --> 
<bean id="mailAutoUpdater" class="com.fg.autoupdate.AutoUpdater">   
    <property name="componentDescriptor" ref="mailModuleInfo"/>   
    <property name="resourceAccessor" ref="mailAutoUpdateResourceAccessor"/>
</bean>  

<!-- popisná třídu vaší aplikace (vrací id a číslo aktuální verze) --> 
<bean id="mailModuleInfo" class="com.fg.autoupdate.AutoUpdaterInfo">   
    <property name="componentName" value="${pom.artifactId}"/>  
    <property name="componentVersion" value="${pom.version}"/>
</bean> 

<!-- třída definující přístup k resourcům s SQL příkazy --> 
<!-- defaultní implementace načte seznam SQL dotazů oddělených středníky ze souboru --> 
<bean id="mailAutoUpdateResourceAccessor" class="com.fg.autoupdate.resources.DefaultResourceAccessor">   
    <!-- odkaz do složky, kde se nachází podsložky platformy (mysql nebo oracle) s SQL soubory -->   
    <property name="resourcePath" value="classpath:/META-INF/lib_mail/sql/"/>   
    <!-- kódování souborů -->   
    <property name="encoding" value="windows-1250"/>
</bean>
```