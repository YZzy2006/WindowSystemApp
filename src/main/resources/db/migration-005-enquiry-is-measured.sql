ALTER TABLE enquiry ADD COLUMN is_measured TINYINT DEFAULT 0 COMMENT '是否已量尺 0否 1是' AFTER need_measure;
