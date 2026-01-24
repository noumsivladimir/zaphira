ALTER TABLE user_security_answers
    ADD COLUMN IF NOT EXISTS answer_salt VARCHAR(255) NOT NULL DEFAULT '';
