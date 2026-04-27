-- V2__seed_sample_data.sql
-- Sample seed data for development

INSERT INTO tasks (id, title, description, status, due_date, version)
VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Set up CI/CD pipeline',
     'Configure Jenkins pipeline for the task-manager service', 'TODO',
     NOW() + INTERVAL '7 days', 0),

    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Design database schema',
     'Create ERD and write Flyway migrations for all entities', 'DONE',
     NOW() - INTERVAL '2 days', 0),

    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Implement REST API',
     'Build CRUD endpoints with full validation and error handling', 'IN_PROGRESS',
     NOW() + INTERVAL '3 days', 0),

    ('d4e5f6a7-b8c9-0123-defa-234567890123', 'Write unit tests',
     'Achieve 80%+ line coverage across service and controller layers', 'TODO',
     NOW() + INTERVAL '5 days', 0),

    ('e5f6a7b8-c9d0-1234-efab-345678901234', 'Frontend React app',
     'Build task management UI with React and TypeScript', 'TODO',
     NOW() + INTERVAL '14 days', 0)
ON CONFLICT (id) DO NOTHING;
