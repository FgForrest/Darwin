DROP PROCEDURE IF EXISTS `EdeeCreatePage`;
CREATE PROCEDURE `EdeeCreatePage` (
	IN inPageName VARCHAR(255),
	IN inSourceDataName VARCHAR(255),
	IN inLang VARCHAR(5),
	IN inSystemId VARCHAR(45),
	IN inParentSystemId VARCHAR(45),
	IN inUrl VARCHAR(255),
	IN inItemUrl VARCHAR(255),
	IN inSourceType VARCHAR(45),
	IN inPrototype VARCHAR(45),
	IN inDataType VARCHAR(40),
	IN inSourceDataTable VARCHAR(45),
	IN inFlagEditable TINYINT(1),
	IN inFlagPublishable TINYINT(1),
	IN inFlagPreviewable TINYINT(1),
	IN inFlagMenuhide TINYINT(1),
	IN inSortOrder INT(11),
	IN inLockType VARCHAR(45),
	IN inLockedBy VARCHAR(45)
)
BEGIN
	DECLARE tParentId INT(11) DEFAULT NULL;;
	DECLARE tFinalSortOrder INT(11) DEFAULT 1;;
	DECLARE tSourceId INT(11);;
	DECLARE tPageTreeId INT(11);;
	DECLARE tVersionId INT(11) DEFAULT 1;;

	-- get parent id
	SELECT id INTO tParentId FROM EDEE_PAGETREE WHERE systemId = inParentSystemId COLLATE utf8_unicode_ci AND lang = inLang COLLATE utf8_unicode_ci;;

	-- sort order - if not provided (-1), do the math, else use it
	IF inSortOrder = -1 THEN
		SELECT sortOrder + 1 INTO tFinalSortOrder FROM EDEE_PAGETREE WHERE parentId = tParentId ORDER BY sortOrder DESC LIMIT 1;;
	ELSE
		SET tFinalSortOrder = inSortOrder;;
	END IF;;

	-- EDEE_PAGETREE record
	INSERT INTO EDEE_PAGETREE (
		systemId,
		lang,
		parentId,
		sortOrder,
		prototype,
		url,
		flagEditable,
		flagPublishable,
		flagPreviewable,
		flagMenuhide,
		lockType,
		lockedBy
	) VALUES (
		inSystemId,
		inLang,
		tParentId,
		tFinalSortOrder,
		inPrototype,
		inUrl,
		inFlagEditable,
		inFlagPublishable,
		inFlagPreviewable,
		inFlagMenuhide,
		inLockType,
		inLockedBy
	);;

	-- EDEE_SOURCE record
	INSERT INTO EDEE_SOURCE (
		lang,
		sourceType,
		pageState,
		dataType,
		systemName,
		itemUrl,
		created,
		changed,
		createdBy,
		changedBy,
		source
	) VALUES (
		inLang,
		inSourceType,
		'PUBLISHED',
		inDataType,
		inPageName,
		inItemUrl,
		Now(),
		Now(),
		'edee_user',
		'edee_user',
		'Edee create script'
	);;

	-- retrieve new IDs to create relation between them in EDEE_PAGETREE_SOURCE_REL
	SELECT id INTO tSourceId FROM EDEE_SOURCE ORDER BY id DESC LIMIT 1;;
	SELECT id INTO tPageTreeId FROM EDEE_PAGETREE ORDER BY id DESC LIMIT 1;;

	INSERT INTO EDEE_PAGETREE_SOURCE_REL (
		pageTreeId,
		sourceId,
		relationType,
		sortOrder
	) VALUES (
		tPageTreeId,
		tSourceId,
		1,
		0
	);;

	INSERT INTO T_EDEE_VERSION (
		idVersionedEntity,
		entityName,
		versionNumber,
		description,
		state,
		created,
		lastSaved,
		lastVersion
	) VALUES (
		tSourceId,
		inDataType,
		1,
		'initial.revision.desc',
		'initial.revision',
		Now(),
		Now(),
		true
	);;

	-- retrieve new versionId
	SELECT id INTO tVersionId FROM T_EDEE_VERSION ORDER BY id DESC LIMIT 1;;

	-- prepared statement due to dynamic source_data table
	SET @edeeCreatePageScriptSourceId = tSourceId;;
	SET @edeeCreatePageScriptVersionId = tVersionId;;
	SET @edeeCreatePageScriptSourceDataName = inSourceDataName;;
	SET @edeeCreatePageScript = CONCAT(
		"INSERT INTO ",
		inSourceDataTable,
		" (sourceId, versionId, name, date) VALUES (@edeeCreatePageScriptSourceId, @edeeCreatePageScriptVersionId, @edeeCreatePageScriptSourceDataName, Now())"
	);;
	PREPARE ps from @edeeCreatePageScript;;

	EXECUTE ps;;
END;