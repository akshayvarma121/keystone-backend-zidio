-- Add missing foreign key indexes as identified

CREATE INDEX IF NOT EXISTS idx_users_customer_id ON users(customer_id);
CREATE INDEX IF NOT EXISTS idx_wo_created_by ON work_orders(created_by);
CREATE INDEX IF NOT EXISTS idx_wo_req_skill ON work_orders(required_skill_id);

CREATE INDEX IF NOT EXISTS idx_wosh_changed_by ON work_order_status_history(changed_by);
CREATE INDEX IF NOT EXISTS idx_part_usage_wo_id ON part_usage(work_order_id);
CREATE INDEX IF NOT EXISTS idx_part_usage_part_id ON part_usage(part_id);
CREATE INDEX IF NOT EXISTS idx_part_usage_logged_by ON part_usage(logged_by);

CREATE INDEX IF NOT EXISTS idx_time_logs_wo_id ON time_logs(work_order_id);
CREATE INDEX IF NOT EXISTS idx_time_logs_tech_id ON time_logs(technician_id);

CREATE INDEX IF NOT EXISTS idx_user_skills_user_id ON user_skills(user_id);
CREATE INDEX IF NOT EXISTS idx_user_skills_skill_id ON user_skills(skill_id);

CREATE INDEX IF NOT EXISTS idx_attachments_wo_id ON work_order_attachments(work_order_id);
CREATE INDEX IF NOT EXISTS idx_attachments_upl_by ON work_order_attachments(uploaded_by);

CREATE INDEX IF NOT EXISTS idx_comments_wo_id ON work_order_comments(work_order_id);
CREATE INDEX IF NOT EXISTS idx_comments_auth_id ON work_order_comments(author_id);

CREATE INDEX IF NOT EXISTS idx_notif_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_wo_id ON notifications(work_order_id);

CREATE INDEX IF NOT EXISTS idx_maint_sched_cust_id ON maintenance_schedules(customer_id);
CREATE INDEX IF NOT EXISTS idx_maint_sched_site_id ON maintenance_schedules(site_id);
CREATE INDEX IF NOT EXISTS idx_maint_sched_req_skill ON maintenance_schedules(required_skill_id);
CREATE INDEX IF NOT EXISTS idx_maint_sched_created_by ON maintenance_schedules(created_by);


CREATE INDEX IF NOT EXISTS idx_worating_created_by ON work_order_ratings(created_by);
CREATE INDEX IF NOT EXISTS idx_wo_created_at ON work_orders(created_at);