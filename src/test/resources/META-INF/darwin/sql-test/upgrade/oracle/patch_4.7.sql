INSERT INTO DARWIN (component, modified, version) VALUES ('pokusna se povedla',SYSDATE,1.0);
INSERT INTO DARWIN_PATCH (id,componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUES (SQ_DARWIN_PATCH.nextval,'testovaci','3.7 byl proveden',0,to_date('2016-12-10', 'yyyy-mm-dd'),to_date('2016-12-10', 'yyyy-mm-dd'),'oracle');