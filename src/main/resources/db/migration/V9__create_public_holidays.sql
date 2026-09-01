CREATE TABLE public_holidays (
    id          UUID NOT NULL,
    date        DATE NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_public_holidays PRIMARY KEY (id),
    CONSTRAINT uq_public_holidays_date UNIQUE (date)
);
