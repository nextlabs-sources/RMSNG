ALTER TABLE project_space_item
  ADD CONSTRAINT u_file_path_project UNIQUE(project_id, file_path);
