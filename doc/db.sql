-- DROP TABLE aplatform.traffic_internet_records;

CREATE TABLE traffic_internet_records
(
  id            INT AUTO_INCREMENT
    PRIMARY KEY,
  start_time    DATETIME      NULL,
  business_name VARCHAR(500)  NULL,
  url           VARCHAR(5000) NULL
)
  ENGINE = InnoDB;


-- DROP TABLE aplatform.detailed_flow;

  CREATE TABLE detailed_flow
(
  id          INT AUTO_INCREMENT
    PRIMARY KEY,
  start_time  DATETIME     NULL,
  end_time    DATETIME     NULL,
  flow        VARCHAR(100) NULL,
  type        VARCHAR(100) NULL,
  belong_area VARCHAR(100) NULL,
  key_type    VARCHAR(100) NULL
)
  ENGINE = InnoDB;





