export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Task {
  id?: string;
  ownerId?: string;
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignee: string;
  project: string;
  dueDate: string | null;
  tags?: string[];
  storyPoints?: number | null;
  createdAt?: string;
  updatedAt?: string;
}
