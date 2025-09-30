DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` varchar(20) NOT NULL COMMENT 'ID主键',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phoneNo` varchar(50) NOT NULL COMMENT '手机号码',
  `gender` varchar(50) NOT NULL COMMENT '性别',
  `idNo` varchar(50) NOT NULL COMMENT '身份证号码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义字段信息表';
