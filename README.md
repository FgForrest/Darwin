# Princip fungování

## Automatická aktualizace databáze

Základním stavebním kamenem je třída AutoUpdater - ta je deklarovaná jako InitializingBean, což znamená, že ihned po jejím 
vytvoření proběhne aplikace její logiky. AutoUpdater si zjistí nad jakou platformou (databází) běží dle použitého datasource 
(respektive package driveru, který je pro připojení k data sourcu použit).

Dále se koukne do databáze, zda tam existuje tabulka `T_DB_AUTOUPDATE`, ve které si ukládá názvy a verze komponent, které se 
skrze tuto knihovnu aktualizují. Pokud tabulka neexistuje, je založena. Do této tabulky si ihned uloží také svou vlastní 
verzi - knihovna totiž umí stejným způsobem, jakým vytváří / aktualizuje databáze pro ostatní knihovny, aktualizovat i sama sebe.

Výše uvedená logika se aplikuje pouze tím, že ve svém spring konfiguračním souboru uvedete import konfigurace z 
*classpath:/META-INF/lib_db_autoupdate/spring/db-autoupdate-config.xml.*

Stejná logika se aplikuje i na vaši knihovnu. Instanciujete beanu AutoUpdateru, dodáte třídu, která AutoUpdateru poskytne 
základní informace o vaší knihovně - tzn. unikátní název a aktuální verzi knihovny. Na základě těchto dat si AutoUpdater 
ve své tabulce ověří, zda má pro vaši komponentu vytvořen datový model a v jaké verzi. Pokud nemá - založí jej, pokud má, 
porovná verzi načtenou ze své tabulky s verzí, kterou mu vrátí vaše beana - pokud se verze liší (tzn. vaše knihovna má 
vyšší verzi, než ta uložená v databázi), provede se aplikování všech patchů mezi těmito dvěma verzemi.

VersionComparator se stará o porovnávání dvou verzí. Dokáže porovnávat verze z více fragmentů i s textovými částmi. Uvádím 
výňatek z testu: *1.1 > 1.0, 1.0-SNAPSHOT = 1.0, 1.1.1 > 1.1, 1.1-alfa > 1.1-beta, 1.1-alfa-2 > 1.1-alfa-1*

Soubory s SQL příkazy jsou hledány ve složce, kterou uvedete v beaně com.fg.autoupdate.resources.DefaultResourceAccessor 
v property resourcePath. Lépe řečeno ve výchozí implementaci nejsou tyto soubory hledány přímo v této složce ale **podsložce 
použité databázové platformy**. Tzn. pokud uvedete cestu ke složce /META-INF/lib_user/sql a pracujete nad MySQL databází, 
bude DefaultResourceAccessor hledat SQL soubory ve složce /META-INF/lib_user/sql/mysql/* - tímto způsobem můžete svoji 
knihovnu jednoduše portovat nad různé databáze.

AutoUpdater používá pro nalezení správných souborů s SQL příkazy rozhraní `ResourceNameAnalyzer`. Defaultní implementace 
hledá soubor **create.sql**  s příkazy pro založení struktury vaší komponenty. V případě rozdílů mezi verzemi hledá jakékoliv 
soubory s názvy ve formátu **patch_verze.sql**, kde verze je např **1.1**.

Pokud si tedy vezmeme příklad, že v databázi máte strukturu své knihovny ve verzi 1.0 a nově se instaluje knihovna verze 
2.0 bude auto updater aplikovat všechny patche mezi těmito verzemi. Například tedy: patch_1.1.sql, patch_1.6.7.sql atd.

Téměř cokoliv, co používá AutoUpdater je zakryté interfacy, takže pokud vám nevyhovuje způsob načítání SQL příkazů, 
aplikování příkazů nad databází aj., můžete si vytvořit vlastní implementace a jednoduše přes settery ty původní nahradit.

Aktualizace používá pro běh synchronizaci (viz. další kapitola) - tzn. je bezpečná i v prostředí clusteru. Aktualizace se 
při svém spuštění snaží získat zámek, aby si pouze jeden thread zajistil exklusivní přístup k databázi pro případ, že by 
byl autoupdater spuštěn najednou ve více vláknech. Pokud se mu nepodaří zámek získat opakuje pokus o získání zámku ještě 
20x vždy po 5 vteřinách. Terpve potom vyhodí vyjímku a ukončí se.

### Integrace do projektů s existující datovou strukturou

AutoUpdater můžete také použít i v projektech, kde již existuje nějaká datová struktura. Standardně vytvoříte **create.sql** 
skript a sadu patchů stejně jako kdyby byla databáze prázdná. Následně přidáte jeden nebo více **guess_version.sql** skriptů, 
které AutoUpdater použije k zjištění verze existující datové struktury.

Postup zjišťování je následující. V případě, že AutoUpdater nebude mít záznam ve své interní tabulce o existenci vámi 
instalované komponenty, dřív než použije create.sql skript, se pokusí najít všechny skripty začínající řetězcem guess 
(např. guess_1.0.sql, guess_2.1.0.sql apod.) a začne je spouštět v pořadí od nejnižší verze po verzi nejvyšší. Poslední skript, 
který neskončí vyjímkou (nebo v případě, že guess začíná na `select count` a bude vracet počet výšší než 0) bude považován 
za skript určující verzi existující datové struktury a místo, aby použil skript create.sql, uloží jen uhádnuté číslo 
verze modulu (bere se z názvu guess skriptu) do své interní tabulky.

Tímto způsobem se lze jednoduše napojit na existující datovou strukturu.

## Verze 1.12.0

Od verze 1.12.0 db_autoupdater ignoruje SQL komentáře.

např: -- ignoruj tento řádek

## Verze 3.0.0

Hlavní změna je v rozšíření o aplikaci db patchu nikoliv primárně podle verze, ale podle vazby zda byla patch aplikován či nikoliv.

Nově AutoUpdater umí:

* v tabulce `T_DB_AUTOUPDATE_PATCH` je historie kompletně i částečně provedených patchů
* v tabulce `T_DB_AUTOUPDATE_SQL` je historie provedených SQL příkazů a případnou vyjímku vrácenou DB
* dokáže aplikovat i všechny chybějící patche, tj. pokud již bude aplikován patch 2.2 a nově se mu na classpath objeví patch 2.1, pokusí se jej zpětně aplikovat
* dokáže navázat aplikování patchů i z prostřed SQL souboru (bude porovnávat SQL příkaz proti těm, co už provedl)
* vede si informaci o tom, jak dlouho mu provedení toho konkrétního SQL / patche trvalo
* vede si informaci o tom, kdy konkrétně dané SQL/patch provedl a kdy se o něm poprvé dozvěděl

Poznámka

Pokud narazíte na problémy se zpětnou aplikací patchů dle nové logiky, pak vymazaním tablky `T_DB_AUTOUPDATE_PATCH` použije 
knihovna pouze informace o verzích komponent jako dříve.