DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phoneNo` varchar(50) NOT NULL COMMENT '手机号码',
  `gender` varchar(50) NOT NULL COMMENT '性别',
  `idNo` varchar(50) NOT NULL COMMENT '身份证号码',
  `createTime` datetime NOT NULL COMMENT '创建时间',
    `createBy` bigint NOT NULL DEFAULT '0' COMMENT '创建人',
    `updateTime` datetime NOT NULL COMMENT '更新时间',
    `updateBy` bigint NOT NULL DEFAULT '0' COMMENT '更新人',
    `enableFlag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '删除标识符,正常1,删除0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义字段信息表';

DROP TABLE IF EXISTS `belong_org`;
CREATE TABLE `belong_org` (
  `id` bigint NOT NULL COMMENT '主键',
  `objectType` varchar(100) NOT NULL DEFAULT '' COMMENT '对象类型',
  `objectId` bigint NOT NULL DEFAULT '0' COMMENT '对象id',
  `orgId` bigint NOT NULL DEFAULT '0' COMMENT '组织id',
  `createTime` datetime NOT NULL COMMENT '创建时间',
  `createBy` bigint NOT NULL DEFAULT '0' COMMENT '创建人',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  `updateBy` bigint NOT NULL DEFAULT '0' COMMENT '更新人',
  `enableFlag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '删除标识符,正常1,删除0',
  PRIMARY KEY (`id`),
  KEY `object_index` (`objectType`,`objectId`,`orgId`),
  KEY `holder_index` (`orgId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='对象数据所属组织表';

DROP TABLE IF EXISTS `white_list`;
CREATE TABLE `white_list` (
  `id` bigint NOT NULL COMMENT '主键',
  `objectType` varchar(100) NOT NULL DEFAULT '' COMMENT '对象类型',
  `objectId` bigint NOT NULL DEFAULT '0' COMMENT '对象id',
  `staffId` bigint NOT NULL DEFAULT '0' COMMENT '员工id',
  `operationContent` varchar(500) NOT NULL DEFAULT '' COMMENT '权限编码(创建人owner、查看view、管理员manage),多个用逗号拼接',
  `createTime` datetime NOT NULL COMMENT '创建时间',
  `createBy` bigint NOT NULL DEFAULT '0' COMMENT '创建人',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  `updateBy` bigint NOT NULL DEFAULT '0' COMMENT '更新人',
  `enableFlag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '删除标识符,正常1,删除0',
  PRIMARY KEY (`id`),
  KEY `object_index` (`objectType`,`objectId`,`staffId`),
  KEY `holder_index` (`staffId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='对象白名单owner表';

DROP TABLE IF EXISTS `unit_info`;
CREATE TABLE `unit_info` (
  `unitId` int NOT NULL COMMENT '组织ID',
  `unitName` varchar(256) DEFAULT NULL COMMENT '组织名称',
  `unitFullName` varchar(256) DEFAULT NULL COMMENT '组织全名',
  `unitFullPath` varchar(256) DEFAULT NULL COMMENT '组织全路径',
  `parentUnitId` int DEFAULT NULL COMMENT '父组织ID',
  `virtualFlag` tinyint(1) DEFAULT NULL COMMENT '虚拟组织（1：虚拟组织，0，实体组织）',
  `createTime` datetime NOT NULL COMMENT '创建时间',
  `createBy` bigint NOT NULL DEFAULT '0' COMMENT '创建人',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  `updateBy` bigint NOT NULL DEFAULT '0' COMMENT '更新人',
  `enableFlag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '删除标识符（正常1,删除0）',
  PRIMARY KEY (`unitId`),
  KEY `unitFullPathIndex` (`unitFullPath`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织信息表';