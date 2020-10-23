create view V_MAIL_RECIPIENT as
select * from T_MAIL_RCPT inner join T_MAIL_RCPT_PROPS on
T_MAIL_RCPT.id = T_MAIL_RCPT_PROPS.idRcpt;

-- ----------------------------
-- Table structure for t_skin
-- ----------------------------
# fifthComment
-- secondComment
/* thirdComment */
CREATE TABLE `T_SKIN` (
  `id` int(11) NOT NULL auto_increment, 
  `title` varchar(255) default NULL,
  `isPublic` tinyint(4) default NULL,
  `bgrPath` varchar(255) default NULL,
  `bgrColor` varchar(255) default NULL,
  `footerType` varchar(255) default NULL,
  `bgrColorBanner` varchar(255) default NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records
-- ----------------------------
/*
sixthComment
*/
INSERT INTO `T_SKIN` VALUES ('1', 'Koule', '1', '/img/edee/skiny/1/pozadi/pozadi.jpg', '#24292F', '1', '#E4F0F8');

-- ----------------------------
-- Table structure for t_skin_settings
-- ----------------------------
CREATE TABLE `T_SKIN_SETTINGS` (
  `id` int(11) NOT NULL auto_increment,
  `idSkin` int(11) default NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records
-- ----------------------------
INSERT INTO `T_SKIN_SETTINGS` VALUES ('1', '1');
INSERT INTO `T_SKIN_SETTINGS` VALUES ('2', '0');

