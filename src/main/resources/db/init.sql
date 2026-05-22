-- ============================================================
-- TrekMate Database Initialization Script
-- Chạy script này trên PostgreSQL trước khi khởi động ứng dụng
-- ============================================================

-- Tạo database (chạy với superuser nếu chưa có)
-- CREATE DATABASE trekmate_db;

-- Tạo user riêng cho app (tùy chọn, khuyến nghị trên prod)
-- CREATE USER trekmate_user WITH ENCRYPTED PASSWORD 'your_secure_password';
-- GRANT ALL PRIVILEGES ON DATABASE trekmate_db TO trekmate_user;

-- Kết nối vào database
-- \c trekmate_db

-- Extensions hữu ích
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- full-text search

-- Spring JPA với ddl-auto=update sẽ tự tạo các bảng còn lại.
-- Script này chỉ cần chạy 1 lần để khởi tạo database và extensions.
