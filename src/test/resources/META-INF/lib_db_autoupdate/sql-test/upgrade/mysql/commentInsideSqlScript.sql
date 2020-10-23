CREATE TABLE T_FORUM_TOPIC (
  id INT NOT NULL auto_increment,
  parent INT,
  itemIndex INT NOT NULL DEFAULT 0,
  itemLevel INT NOT NULL DEFAULT 0,
  rootItem INT,
  title VARCHAR(255) NOT NULL,
  perex TEXT,
  description TEXT,
  created DATETIME NOT NULL,
  modified datetime DEFAULT NULL,
  topicState TINYINT unsigned,
  topicType VARCHAR(64),
  contentId VARCHAR(512),
  username VARCHAR(64),  -- consider increase to 128
  PRIMARY KEY (id),
  INDEX IX_TOPIC_INDEX (itemIndex),
  INDEX IX_ROOT_TOPIC (rootItem),
  INDEX IX_TITLE_TOPIC (title),
  CONSTRAINT CNFK_TOPIC_FORUM_ID FOREIGN KEY (parent) REFERENCES T_FORUM_TOPIC (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET='utf8';