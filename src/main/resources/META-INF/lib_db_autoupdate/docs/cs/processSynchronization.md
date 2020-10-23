# Synchronizace procesů (i v clusteru)

Pro potřeby AutoUpdateru byla vytvořena třída Locker, kterou ale můžete použít i vy ve vaší aplikaci. Locker vám zajistí, 
aby vaše procesy, které mohou běžet pouze v jediné instanci, skutečně byly jediné i v takových prostředích jako je cluster. 
Nejedná se o žádnou alchymii, pouze o správu zámků v tabulce T_DB_AUTOUPDATE_LOCK.

Locker poskytuje následující tři metody:

* String leaseProcess(String processName, Date until) throws ProcessIsLockedException
* void renewLease(String processName, String unlockKey, Date until) throws ProcessIsLockedException
* void releaseProces(String processName, String unlockKey)

První metodou si získáte zámek pro svůj proces, druhou metodou zámek uvolníte. Locker nepovolí vydání dvou zámků na jeden 
proces. Celý princip je však založen na formě leasingu - zámek je pouze vypůjčen na uvedenou dobu (parametr until). V případě, 
že je například server restartován a nestihne se zámek uvolnit, bude se po nastartování procesu čekat až do doby než původní 
výpujčka zámku vyprší. Tímto způsobem je však zajištěno, že se systém automaticky po nějaké době dostane do normálu, při 
jistotě, že proces může běžet skutečně pouze jednou.

Pokud váš proces běží delší dobu, může si v pravidelných intervalech prodlužovat výpujčku svého zámku - čímž dává najevo, 
že stále žije a pracuje a jiná instance tého procesu by měla vyčkat až do doby, než se jí podaří vypůjčit vlastní zámek.

## Automatické prodlužování zámků

Ve verzi 1.11 bylo do Lockeru přidáno automatické prodlužování zámku. Celý princip je popsán v následujících bodech:

1. Vytvořit zámek s automatickým prodlužováním lze pomocí metod

* leaseProcess(String processName, Date until, LockRestorer lockerRestorer)
* leaseProcess(String processName, Date until, int waitForLockInMiliseconds, LockRestorer lockRestorer).

Poslední parametr implementuje rozhraní LockRestorer, které má jedinou metodu isFinished(), která vrací, zda zamčený proces byl/nebyl ukončen.

2. Zároveň je také vytvořen CheckLockTimerTask, který v pravidelných intervalech testuje zamčený proces:

    a. zda bylo dosaženo maximálního počtu prodloužení zámku. Maximální počet je definován konstantou MAX_RENEW_COUNT = 10,

    b. zda již byl zámek odemčen pomocí metody releaseProces(String processName, String unlockKey),

    c. zda byl proces ukončen a to pomocí metody lockRestorer.isFinished().

3. V případě, že test nesplnil ani jednu podmínku (2a,2b,2c), je zavolána metoda renewLease(String processName, String unlockKey, Date until) na prodloužení zámku.

4. V opačném případě zámek prodloužen nebude.

CheckLockerTimerTask je spouštěn po uplynutí 70% platnosti zámku (např. je-li platnost zámku stanovena na 10min, test na 
prodloužení zámku se provede v 7 minutě).

##Zámek v samostatné transakci
*(doplněno by MVE, od verze 1.19.0)*

Zjistil jsem, že Locker (resp. defaultní LockPersister storage) si při vytváření zámku nevytváří explicitně vlastní transakci. 
Používání Lockeru v dosavadní implementaci totiž předpokládá tuto logickou posloupnost kroků

``` java
public void someBusinessMethod(Data data) { 
    locker.leaseProcess(); 
    transactionTemplate.execute( performSomeLogicHere(data); ); 
    locker.releaseProces
}
```
   
Toto má prakticky za následek, že locker jede v jiné transakci, než aplikační logika, ale musíte na to pamatovat a sami 
to takto definovat. Osobně mi přijde tento postup nelogický a hlavně by byl pro mě poměrně pracný v okamžiku, kdy nasazuji 
locker do již existujícího kódu. Rovněž je tento postup problematický, pokud chcete vytvářet zámek uvnitř aplikační logiky.

Proto jsem po dohodě s JNO doplnil do třídy Locker setter setInSeparateTransaction, po jehož nastavení na true se v defaultním 
lock persisteru nastaví v TransactionTemplate definice transakce na TransactionDefinition.PROPAGATION_NOT_SUPPORTED. 
V opačném případě je nastaveno TransactionDefinition.PROPAGATION_REQUIRED.

Vzhledem ke zpětné kompatibilitě a možným problémům v testech zůstává ve výchozí locker nastaven tak, že si nevytváří 
vlastní transakci. Pokud tedy chcete využít této nové funkcionality, musíte si tedy vytvořit vlastní instanci lockeru, 
ve Springu např. takto:

``` xml
<bean id="paymentLocker" class="com.fg.autoupdate.Locker">    
    <property name="skipIfDataSourceNotPresent" value="false"/>    
    <property name="resourceAccessor" ref="dbAutoUpdateResourceAccessor"/>    
    <property name="timer" ref="timerFactory"/>    
    <property name="inSeparateTransaction" value="true"/> 
</bean>
```

Pozor, referenci na resourceAccessor je nutné mít nastavenu tak, jak je uvedena příkladu (tzn. accessor přímo z konfigurace 
samotné auto_update knihovny) a nikoliv na accessor, který byl vytvořen v rámci projektu. A pak nasetovat správnou instanci 
lockeru, protože výchozí instance samozřejmě existuje i nadále.