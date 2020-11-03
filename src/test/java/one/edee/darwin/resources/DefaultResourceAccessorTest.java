package one.edee.darwin.resources;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.model.Platform;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class DefaultResourceAccessorTest extends AbstractDarwinTest {

	@SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    @Qualifier("darwinResourceAccessor")
    private DefaultResourceAccessor darwinResourceAccessor;

	@SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    @Qualifier("darwinResourceAccessor4Test")
    private DefaultResourceAccessor alternativeDarwinResourceAccessor;

    @Test
    public void testGetTextContentFromResource() throws Exception {
        String content = darwinResourceAccessor.getTextContentFromResource("mysql/create.sql");
        assertNotNull(content);
        assertTrue(content.startsWith("create table"));
    }

    @Test
    public void testCommentWithSingleQuote() {
        String content = alternativeDarwinResourceAccessor.getTextContentFromResource("mysql/commented-with-single-quote.sql");
        assertNotNull(content);
        List<String> statements = alternativeDarwinResourceAccessor.tokenizeSQLScriptContent(content);
        assertEquals(4, statements.size());
        assertEquals("INSERT INTO `T_CONTENT_SEARCH_INDEX` VALUES ('1', '1', 'Nejlepší časosběrný pop? \\\"Bestofka\\\" Eddie Stoilow', 'koule', '/cs/clanky/nejlepsi-casosberny-pop-bestofka-eddie-stoilow-1416.shtml', 'clanek', '2009-10-01 16:59:40', '2009-10-01 16:59:44', '2009-12-24 16:59:51')", statements.get(0));
        assertEquals("INSERT INTO `T_CONTENT_SEARCH_INDEX` VALUES ('2', '2', 'Prohrála v kartách natočila klip s J. X. Doležalem z Reflexu ', 'koule', '/cs/clanky/prohrala-v-kartach-natocila-klip-s-j-x-dolezalem-z-reflexu--1415.shtml', 'clanek', '2009-10-08 17:00:48', '2009-10-08 17:00:53', '2009-11-25 17:00:57')", statements.get(1));
        assertEquals("INSERT INTO `T_CONTENT_SEARCH_INDEX` VALUES ('3', '3', 'Kapela J.A.R. slaví 20 let spolu, přijďte jim popřát', 'koule', '/cs/clanky/kapela-jar-slavi-20-let-spolu-prijdte-jim-poprat-1413.shtml', 'clanek', '2009-10-23 17:01:33', '2009-12-30 17:01:44', '2009-12-31 17:01:54')", statements.get(2));
        assertEquals("INSERT INTO `T_CONTENT_SEARCH_INDEX` VALUES ('4', '4', 'Gaia Mesiah', 'koule', '/cs/interpreti/gaia/**/-mesiah.shtml', 'interpret', '2009-10-30 17:08:26', null, null)", statements.get(3));
    }

    @Test
    public void testEscapedWithSingleQuote() {
        String content = alternativeDarwinResourceAccessor.getTextContentFromResource("mysql/escaped-with-single-quote.sql");
        assertNotNull(content);
        List<String> statements = alternativeDarwinResourceAccessor.tokenizeSQLScriptContent(content);
        assertEquals(2, statements.size());
        assertEquals("INSERT INTO \"T_MAIL_NEWSLETTER_BODY\" (id, idNewsletter, partName, bodyPart) VALUES (1, 1, 'question', 'How it''s goin'' bro?')", statements.get(0));
        assertEquals("INSERT INTO \"T_MAIL_NEWSLETTER_BODY\" (id, idNewsletter, partName, bodyPart) VALUES (2, 1, 'answer', 'Sweet as hell!!!')", statements.get(1));
    }

	@Test
	public void testKeepSemicolonsInTrigger() {
		String content = alternativeDarwinResourceAccessor.getTextContentFromResource("mysql/trigger.sql");
		assertNotNull(content);
		final List<String> result = alternativeDarwinResourceAccessor.tokenizeSQLScriptContent(content);
		assertEquals(3, result.size());
		assertEquals("DELIMITER $$", result.get(0));
		assertEquals("CREATE TRIGGER TR_DENY_SUBJECT_DELETION\n" +
				"BEFORE DELETE\n" +
				"ON T_FRAUS_SUBJECT\n" +
				"FOR EACH ROW\n" +
				"  BEGIN\n" +
				"    IF OLD.id is not null THEN\n" +
				"      SIGNAL SQLSTATE '50000'\n" +
				"      SET MESSAGE_TEXT = 'Row cannot be removed';\n" +
				"    END IF;\n" +
				"  END\n" +
				"$$", result.get(1).replace("\r\n","\n"));
		assertEquals("DELIMITER ;", result.get(2));
	}

	@Test
	public void testParseProcedure() {
		String content = alternativeDarwinResourceAccessor.getTextContentFromResource("mysql/procedure.sql");
		assertNotNull(content);
		final List<String> result = alternativeDarwinResourceAccessor.tokenizeSQLScriptContent(content);
		assertEquals(2, result.size());
		assertEquals("DROP PROCEDURE IF EXISTS `EdeeCreatePage`", result.get(0));
		assertEquals("CREATE PROCEDURE `EdeeCreatePage` (\n" +
				"\tIN inPageName VARCHAR(255),\n" +
				"\tIN inSourceDataName VARCHAR(255),\n" +
				"\tIN inLang VARCHAR(5),\n" +
				"\tIN inSystemId VARCHAR(45),\n" +
				"\tIN inParentSystemId VARCHAR(45),\n" +
				"\tIN inUrl VARCHAR(255),\n" +
				"\tIN inItemUrl VARCHAR(255),\n" +
				"\tIN inSourceType VARCHAR(45),\n" +
				"\tIN inPrototype VARCHAR(45),\n" +
				"\tIN inDataType VARCHAR(40),\n" +
				"\tIN inSourceDataTable VARCHAR(45),\n" +
				"\tIN inFlagEditable TINYINT(1),\n" +
				"\tIN inFlagPublishable TINYINT(1),\n" +
				"\tIN inFlagPreviewable TINYINT(1),\n" +
				"\tIN inFlagMenuhide TINYINT(1),\n" +
				"\tIN inSortOrder INT(11),\n" +
				"\tIN inLockType VARCHAR(45),\n" +
				"\tIN inLockedBy VARCHAR(45)\n" +
				")\n" +
				"BEGIN\n" +
				"\tDECLARE tParentId INT(11) DEFAULT NULL;\n" +
				"\tDECLARE tFinalSortOrder INT(11) DEFAULT 1;\n" +
				"\tDECLARE tSourceId INT(11);\n" +
				"\tDECLARE tPageTreeId INT(11);\n" +
				"\tDECLARE tVersionId INT(11) DEFAULT 1;\n" +
				"\n" +
				"\t\tSELECT id INTO tParentId FROM EDEE_PAGETREE WHERE systemId = inParentSystemId COLLATE utf8_unicode_ci AND lang = inLang COLLATE utf8_unicode_ci;\n" +
				"\n" +
				"\t\tIF inSortOrder = -1 THEN\n" +
				"\t\tSELECT sortOrder + 1 INTO tFinalSortOrder FROM EDEE_PAGETREE WHERE parentId = tParentId ORDER BY sortOrder DESC LIMIT 1;\n" +
				"\tELSE\n" +
				"\t\tSET tFinalSortOrder = inSortOrder;\n" +
				"\tEND IF;\n" +
				"\n" +
				"\t\tINSERT INTO EDEE_PAGETREE (\n" +
				"\t\tsystemId,\n" +
				"\t\tlang,\n" +
				"\t\tparentId,\n" +
				"\t\tsortOrder,\n" +
				"\t\tprototype,\n" +
				"\t\turl,\n" +
				"\t\tflagEditable,\n" +
				"\t\tflagPublishable,\n" +
				"\t\tflagPreviewable,\n" +
				"\t\tflagMenuhide,\n" +
				"\t\tlockType,\n" +
				"\t\tlockedBy\n" +
				"\t) VALUES (\n" +
				"\t\tinSystemId,\n" +
				"\t\tinLang,\n" +
				"\t\ttParentId,\n" +
				"\t\ttFinalSortOrder,\n" +
				"\t\tinPrototype,\n" +
				"\t\tinUrl,\n" +
				"\t\tinFlagEditable,\n" +
				"\t\tinFlagPublishable,\n" +
				"\t\tinFlagPreviewable,\n" +
				"\t\tinFlagMenuhide,\n" +
				"\t\tinLockType,\n" +
				"\t\tinLockedBy\n" +
				"\t);\n" +
				"\n" +
				"\t\tINSERT INTO EDEE_SOURCE (\n" +
				"\t\tlang,\n" +
				"\t\tsourceType,\n" +
				"\t\tpageState,\n" +
				"\t\tdataType,\n" +
				"\t\tsystemName,\n" +
				"\t\titemUrl,\n" +
				"\t\tcreated,\n" +
				"\t\tchanged,\n" +
				"\t\tcreatedBy,\n" +
				"\t\tchangedBy,\n" +
				"\t\tsource\n" +
				"\t) VALUES (\n" +
				"\t\tinLang,\n" +
				"\t\tinSourceType,\n" +
				"\t\t'PUBLISHED',\n" +
				"\t\tinDataType,\n" +
				"\t\tinPageName,\n" +
				"\t\tinItemUrl,\n" +
				"\t\tNow(),\n" +
				"\t\tNow(),\n" +
				"\t\t'edee_user',\n" +
				"\t\t'edee_user',\n" +
				"\t\t'Edee create script'\n" +
				"\t);\n" +
				"\n" +
				"\t\tSELECT id INTO tSourceId FROM EDEE_SOURCE ORDER BY id DESC LIMIT 1;\n" +
				"\tSELECT id INTO tPageTreeId FROM EDEE_PAGETREE ORDER BY id DESC LIMIT 1;\n" +
				"\n" +
				"\tINSERT INTO EDEE_PAGETREE_SOURCE_REL (\n" +
				"\t\tpageTreeId,\n" +
				"\t\tsourceId,\n" +
				"\t\trelationType,\n" +
				"\t\tsortOrder\n" +
				"\t) VALUES (\n" +
				"\t\ttPageTreeId,\n" +
				"\t\ttSourceId,\n" +
				"\t\t1,\n" +
				"\t\t0\n" +
				"\t);\n" +
				"\n" +
				"\tINSERT INTO T_EDEE_VERSION (\n" +
				"\t\tidVersionedEntity,\n" +
				"\t\tentityName,\n" +
				"\t\tversionNumber,\n" +
				"\t\tdescription,\n" +
				"\t\tstate,\n" +
				"\t\tcreated,\n" +
				"\t\tlastSaved,\n" +
				"\t\tlastVersion\n" +
				"\t) VALUES (\n" +
				"\t\ttSourceId,\n" +
				"\t\tinDataType,\n" +
				"\t\t1,\n" +
				"\t\t'initial.revision.desc',\n" +
				"\t\t'initial.revision',\n" +
				"\t\tNow(),\n" +
				"\t\tNow(),\n" +
				"\t\ttrue\n" +
				"\t);\n" +
				"\n" +
				"\t\tSELECT id INTO tVersionId FROM T_EDEE_VERSION ORDER BY id DESC LIMIT 1;\n" +
				"\n" +
				"\t\tSET @edeeCreatePageScriptSourceId = tSourceId;\n" +
				"\tSET @edeeCreatePageScriptVersionId = tVersionId;\n" +
				"\tSET @edeeCreatePageScriptSourceDataName = inSourceDataName;\n" +
				"\tSET @edeeCreatePageScript = CONCAT(\n" +
				"\t\t\"INSERT INTO \",\n" +
				"\t\tinSourceDataTable,\n" +
				"\t\t\" (sourceId, versionId, name, date) VALUES (@edeeCreatePageScriptSourceId, @edeeCreatePageScriptVersionId, @edeeCreatePageScriptSourceDataName, Now())\"\n" +
				"\t);\n" +
				"\tPREPARE ps from @edeeCreatePageScript;\n" +
				"\n" +
				"\tEXECUTE ps;\n" +
				"END", result.get(1).replace("\r\n","\n"));
	}

    @Test
    public void testTokenizeSQLScriptContent() throws Exception {
        List result = darwinResourceAccessor.tokenizeSQLScriptContent("" +
                "-- Comment 1;\n" +
                "/* Comment 2;;3;;4; */\n" +
                "create table DARWIN\n" +
                "(\n" +
                "\tid integer not null auto_increment,\n" +
                "\tcomponent varchar(255) not null,\n" +
                "\tmodified datetime not null,\n" +
                "\tversion varchar(20) not null,\n" +
                "\tconstraint CNPK_DB_AUTOUPDATE primary key (id),\n" +
                "\tindex IX_DB_AUTOUPDATE_COMPONENT (component)\n" +
                ") engine=InnoDB;" +
                "\n\n" +
                "update DARWIN set component = 'XX;;X;XX';\n" +
                "delete from DARWIN;;");
        assertEquals(3, result.size());
        assertTrue(((String) result.get(0)).startsWith("create table"));
        assertTrue(((String) result.get(1)).startsWith("update"));
        assertTrue(((String) result.get(2)).startsWith("delete"));
    }

    @Test
    public void testTokenizeSQLScriptContentWithTrigger() throws Exception {
        String test2 = "create trigger TR_POSITIVE_AMOUNT_INS before insert on T_ACCOUNT_BALANCE\n" +
                "for each row\n" +
                "begin\n" +
                "\tdeclare msg varchar(255);;\n" +
                "\tif NEW.`amount` < 0 then\n" +
                "\t\tset msg = concat('PositiveAmountTriggerError: Trying to insert a negative value into amount: ', cast(NEW.`amount` as char));;\n" +
                "\t\tsignal sqlstate '45000' set message_text = msg;;\n" +
                "\tend if;;\n" +
                "end;";
        List<String> result = darwinResourceAccessor.tokenizeSQLScriptContent(test2);
        assertEquals("create trigger TR_POSITIVE_AMOUNT_INS before insert on T_ACCOUNT_BALANCE\n" +
                "for each row\n" +
                "begin\n" +
                "\tdeclare msg varchar(255);\n" +
                "\tif NEW.`amount` < 0 then\n" +
                "\t\tset msg = concat('PositiveAmountTriggerError: Trying to insert a negative value into amount: ', cast(NEW.`amount` as char));\n" +
                "\t\tsignal sqlstate '45000' set message_text = msg;\n" +
                "\tend if;\n" +
                "end", result.get(0));
    }

    @Test
    public void testAlterInsert() throws Exception {
        ClassPathResource sqlResource = new ClassPathResource("/META-INF/darwin/sql-test/upgrade/mysql/alter-insert.sql");

        String sql = IOUtils.toString(sqlResource.getInputStream(), StandardCharsets.UTF_8);
        List<String> queries = darwinResourceAccessor.tokenizeSQLScriptContent(sql);
        assertEquals(4, queries.size());
    }

    /**
     * Originally this test took 3:27 minutes. I made it under 1s - how am I? :)
     */
    @Test
    public void testTokenizeVeryLargeSQLScript() throws Exception {
        ClassPathResource sqlResource = new ClassPathResource("/META-INF/darwin/sql-test/upgrade/mysql/verylarge.sql");
        String sql = IOUtils.toString(sqlResource.getInputStream(), StandardCharsets.UTF_8);
        List<String> queries = darwinResourceAccessor.tokenizeSQLScriptContent(sql);
        assertEquals(12185, queries.size());
    }

    @DirtiesContext
    @Test
    public void testGetSortedResourceList() throws Exception {
        darwinResourceAccessor.setResourcePath("/META-INF/darwin/sortedResourceTest");
        Resource[] files = darwinResourceAccessor.getSortedResourceList(Platform.MYSQL);
        assertNotNull(files);
        assertTrue(files.length > 4);
        assertTrue(files[0].getFilename().endsWith("create.sql"), files[0].getFilename() + " doesn't end with create.sql");
        assertTrue(files[files.length - 1].getFilename().endsWith("patch_3.0.sql"));
    }

    @Test
    public void testTokenizeSQLScriptContentWithCommentInsideSQL() throws Exception {
        ClassPathResource sqlResource = new ClassPathResource("/META-INF/darwin/sql-test/upgrade/mysql/commentInsideSqlScript.sql");
        String sql = IOUtils.toString(sqlResource.getInputStream(), StandardCharsets.UTF_8);
        List<String> result = darwinResourceAccessor.tokenizeSQLScriptContent(sql);
        assertEquals(1, result.size());
        assertTrue(result.get(0).startsWith("CREATE TABLE T_FORUM_TOPIC ("));
    }
}