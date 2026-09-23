-- SEOlytics MySQL schema (Hibernate also creates/updates tables automatically)
CREATE DATABASE IF NOT EXISTS seolytics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE seolytics;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(180) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS websites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  url VARCHAR(2048) NOT NULL,
  domain VARCHAR(255) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_websites_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS crawl_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  website_id BIGINT NOT NULL,
  start_url VARCHAR(2048) NOT NULL,
  status VARCHAR(32) NOT NULL,
  max_pages INT,
  max_depth INT,
  pages_crawled INT,
  issues_found INT,
  healthy_pages INT,
  seo_score DOUBLE,
  progress_message VARCHAR(500),
  progress_percent INT,
  robots_txt_accessible BOOLEAN,
  sitemap_found BOOLEAN,
  sitemap_valid BOOLEAN,
  ga4_tag_detected BOOLEAN,
  error_message VARCHAR(1000),
  scoring_breakdown VARCHAR(2000),
  started_at TIMESTAMP(6) NULL,
  finished_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_sessions_website FOREIGN KEY (website_id) REFERENCES websites(id)
);

CREATE TABLE IF NOT EXISTS crawled_pages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  url VARCHAR(2048) NOT NULL,
  status_code INT,
  redirect_hops INT,
  depth INT,
  fetch_time_ms BIGINT,
  title VARCHAR(512),
  meta_description VARCHAR(1024),
  canonical VARCHAR(2048),
  h1_count INT,
  image_count INT,
  images_missing_alt INT,
  has_viewport BOOLEAN,
  https BOOLEAN,
  ga4_tag_detected BOOLEAN,
  indexable BOOLEAN,
  h1_text TEXT,
  CONSTRAINT fk_pages_session FOREIGN KEY (session_id) REFERENCES crawl_sessions(id)
);

CREATE TABLE IF NOT EXISTS seo_issues (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  page_id BIGINT NULL,
  page_url VARCHAR(2048),
  issue_type VARCHAR(64) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  recommendation TEXT,
  detected_value VARCHAR(2048),
  expected_value VARCHAR(2048),
  explanation TEXT,
  why_it_matters TEXT,
  how_to_fix TEXT,
  example_solution TEXT,
  verification_method TEXT,
  status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
  CONSTRAINT fk_issues_session FOREIGN KEY (session_id) REFERENCES crawl_sessions(id),
  CONSTRAINT fk_issues_page FOREIGN KEY (page_id) REFERENCES crawled_pages(id)
);
