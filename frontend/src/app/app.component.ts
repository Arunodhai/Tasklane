import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AdminService } from './admin.service';
import { AuthService } from './auth.service';
import { Task, TaskPriority, TaskStatus } from './model/task.model';
import { UserAccount, UserRole } from './model/user.model';
import { TaskService } from './task.service';

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  tasks: Task[] = [];
  users: UserAccount[] = [];
  currentUser: UserAccount | null = null;
  authReady = false;
  authSubmitting = false;
  authMode: 'login' | 'register' = 'login';
  authError = '';
  showPassword = false;
  authForm = { fullName: '', email: '', password: '' };
  loading = false;
  usersLoading = false;
  saving = false;
  errorMessage = '';
  toastMessage = '';
  activeSection: 'dashboard' | 'tasks' | 'admin' = 'dashboard';
  viewMode: 'list' | 'board' = 'board';
  searchTerm = '';
  statusFilter: TaskStatus | 'ALL' = 'ALL';
  priorityFilter: TaskPriority | 'ALL' = 'ALL';
  dueFilter: 'ALL' | 'TODAY' | 'WEEK' | 'OVERDUE' | 'NONE' = 'ALL';
  projectFilter = 'ALL';
  sortMode: 'UPDATED' | 'DUE' | 'PRIORITY' | 'TITLE' = 'UPDATED';
  draggedTaskId: string | null = null;
  movingTaskIds = new Set<string>();
  panelOpen = false;
  editingTaskId: string | null = null;
  taskToDelete: Task | null = null;
  form: Task = this.emptyTask();

  readonly statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

  constructor(
    private readonly taskService: TaskService,
    private readonly authService: AuthService,
    private readonly adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => this.currentUser = user);
    this.authService.restoreSession().subscribe(user => {
      this.authReady = true;
      if (user) {
        this.loadTasks();
      }
    });
  }

  get isAdmin(): boolean {
    return this.currentUser?.role === 'ADMIN';
  }

  get profileInitials(): string {
    return this.currentUser ? this.initials(this.currentUser.fullName) : 'TL';
  }

  get filteredTasks(): Task[] {
    const query = this.searchTerm.trim().toLowerCase();
    const tasks = this.tasks.filter(task => {
      const matchesText = !query || [task.title, task.description, task.assignee, task.project]
        .some(value => value?.toLowerCase().includes(query))
        || task.tags?.some(tag => tag.toLowerCase().includes(query));
      const matchesStatus = this.statusFilter === 'ALL' || task.status === this.statusFilter;
      const matchesPriority = this.priorityFilter === 'ALL' || task.priority === this.priorityFilter;
      const matchesProject = this.projectFilter === 'ALL' || task.project === this.projectFilter;
      const matchesDue = this.matchesDueFilter(task);
      return matchesText && matchesStatus && matchesPriority && matchesProject && matchesDue;
    });
    return [...tasks].sort((a, b) => this.compareTasks(a, b));
  }

  get projects(): string[] {
    return [...new Set(this.tasks.map(task => task.project).filter(Boolean))].sort();
  }

  get completionPercentage(): number {
    return this.tasks.length ? Math.round(this.doneCount / this.tasks.length * 100) : 0;
  }

  get openStoryPoints(): number {
    return this.tasks.filter(task => task.status !== 'DONE')
      .reduce((total, task) => total + (task.storyPoints ?? 0), 0);
  }

  get assignableUsers(): UserAccount[] {
    return this.users.filter(user => user.enabled);
  }

  get dueTodayCount(): number {
    return this.tasks.filter(task => task.status !== 'DONE' && this.isDueToday(task)).length;
  }

  get highPriorityOpenCount(): number {
    return this.tasks.filter(task => task.status !== 'DONE' && task.priority === 'HIGH').length;
  }

  get dueSoonTasks(): Task[] {
    return this.tasks
      .filter(task => task.status !== 'DONE' && task.dueDate)
      .sort((a, b) => (a.dueDate ?? '').localeCompare(b.dueDate ?? ''))
      .slice(0, 5);
  }

  get recentlyUpdatedTasks(): Task[] {
    return [...this.tasks]
      .sort((a, b) => (b.updatedAt ?? '').localeCompare(a.updatedAt ?? ''))
      .slice(0, 4);
  }

  get projectSummaries(): Array<{ name: string; total: number; done: number; percent: number }> {
    return this.projects.map(name => {
      const projectTasks = this.tasks.filter(task => task.project === name);
      const done = projectTasks.filter(task => task.status === 'DONE').length;
      return { name, total: projectTasks.length, done, percent: Math.round(done / projectTasks.length * 100) };
    }).sort((a, b) => b.total - a.total).slice(0, 5);
  }

  get todoCount(): number {
    return this.tasks.filter(task => task.status === 'TODO').length;
  }

  get inProgressCount(): number {
    return this.tasks.filter(task => task.status === 'IN_PROGRESS').length;
  }

  get doneCount(): number {
    return this.tasks.filter(task => task.status === 'DONE').length;
  }

  get overdueCount(): number {
    return this.tasks.filter(task => this.isOverdue(task)).length;
  }

  get adminCount(): number {
    return this.users.filter(user => user.role === 'ADMIN').length;
  }

  get disabledUserCount(): number {
    return this.users.filter(user => !user.enabled).length;
  }

  switchAuthMode(mode: 'login' | 'register'): void {
    this.authMode = mode;
    this.authError = '';
  }

  submitAuth(): void {
    this.authError = '';
    this.authSubmitting = true;
    const request = this.authMode === 'login'
      ? this.authService.login({ email: this.authForm.email, password: this.authForm.password })
      : this.authService.register(this.authForm);

    request.subscribe({
      next: () => {
        this.authSubmitting = false;
        this.authForm = { fullName: '', email: '', password: '' };
        this.activeSection = 'dashboard';
        this.loadTasks();
      },
      error: error => {
        this.authSubmitting = false;
        this.authError = this.errorText(error, this.authMode === 'login'
          ? 'The email or password is incorrect.'
          : 'Your account could not be created.');
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.tasks = [];
    this.users = [];
    this.activeSection = 'dashboard';
    this.panelOpen = false;
    this.authMode = 'login';
  }

  selectSection(section: 'dashboard' | 'tasks' | 'admin'): void {
    this.activeSection = section;
    window.scrollTo({ top: 0, behavior: 'auto' });
    if (section === 'admin' && this.isAdmin) {
      this.loadUsers();
    }
  }

  loadTasks(): void {
    this.loading = true;
    this.errorMessage = '';
    this.taskService.getAllTasks().subscribe({
      next: tasks => {
        this.tasks = tasks;
        this.loading = false;
      },
      error: error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.logout();
          this.authError = 'Your session expired. Sign in again.';
        } else {
          this.errorMessage = 'Tasklane could not reach the API. Check that the backend is running.';
        }
        this.loading = false;
      }
    });
  }

  loadUsers(): void {
    this.usersLoading = true;
    this.adminService.getUsers().subscribe({
      next: users => {
        this.users = users;
        this.usersLoading = false;
      },
      error: error => {
        this.usersLoading = false;
        this.showToast(this.errorText(error, 'User access could not be loaded.'));
      }
    });
  }

  updateUserRole(user: UserAccount, role: UserRole): void {
    this.updateUser(user, { role });
  }

  toggleUserEnabled(user: UserAccount): void {
    this.updateUser(user, { enabled: !user.enabled });
  }

  openCreate(): void {
    this.editingTaskId = null;
    this.form = this.emptyTask();
    if (this.isAdmin) {
      this.form.ownerId = this.currentUser?.id;
      this.loadUsers();
    }
    this.panelOpen = true;
  }

  createInStatus(status: TaskStatus): void {
    this.openCreate();
    this.form.status = status;
  }

  openEdit(task: Task): void {
    this.editingTaskId = task.id ?? null;
    this.form = { ...task };
    if (this.isAdmin) {
      this.loadUsers();
    }
    this.panelOpen = true;
  }

  selectAssignee(ownerId: string): void {
    this.form.ownerId = ownerId;
    const owner = this.users.find(user => user.id === ownerId);
    if (owner) {
      this.form.assignee = owner.fullName;
    }
  }

  closePanel(): void {
    this.panelOpen = false;
    this.saving = false;
  }

  saveTask(): void {
    if (!this.form.title.trim() || !this.form.assignee.trim() || !this.form.project.trim()) {
      this.showToast('Complete the title, assignee, and project fields.');
      return;
    }

    this.saving = true;
    const request = this.editingTaskId
      ? this.taskService.updateTask(this.editingTaskId, this.form)
      : this.taskService.createTask(this.form);

    request.subscribe({
      next: savedTask => {
        if (this.editingTaskId) {
          this.tasks = this.tasks.map(task => task.id === savedTask.id ? savedTask : task);
        } else {
          this.tasks = [savedTask, ...this.tasks];
        }
        const message = this.editingTaskId ? 'Task updated.' : 'Task created.';
        this.closePanel();
        this.showToast(message);
      },
      error: error => {
        this.saving = false;
        this.showToast(this.errorText(error, 'The task could not be saved.'));
      }
    });
  }

  confirmDelete(task: Task): void {
    this.taskToDelete = task;
  }

  deleteTask(): void {
    if (!this.taskToDelete?.id) {
      return;
    }

    const id = this.taskToDelete.id;
    this.taskService.deleteTask(id).subscribe({
      next: () => {
        this.tasks = this.tasks.filter(task => task.id !== id);
        this.taskToDelete = null;
        this.showToast('Task deleted.');
      },
      error: error => this.showToast(this.errorText(error, 'The task could not be deleted.'))
    });
  }

  tasksForStatus(status: TaskStatus): Task[] {
    return this.filteredTasks.filter(task => task.status === status);
  }

  startDrag(task: Task): void {
    this.draggedTaskId = task.id ?? null;
  }

  endDrag(): void {
    this.draggedTaskId = null;
  }

  dropTask(status: TaskStatus): void {
    const task = this.tasks.find(item => item.id === this.draggedTaskId);
    this.draggedTaskId = null;
    if (task) {
      this.moveTask(task, status);
    }
  }

  moveTask(task: Task, status: TaskStatus): void {
    if (!task.id || task.status === status || this.movingTaskIds.has(task.id)) {
      return;
    }
    this.movingTaskIds.add(task.id);
    const previousStatus = task.status;
    const optimistic = { ...task, status };
    this.tasks = this.tasks.map(item => item.id === task.id ? optimistic : item);
    this.taskService.updateTask(task.id, optimistic).subscribe({
      next: saved => {
        this.movingTaskIds.delete(task.id!);
        this.tasks = this.tasks.map(item => item.id === saved.id ? saved : item);
        this.showToast(`Moved to ${this.statusLabel(status)}.`);
      },
      error: error => {
        this.movingTaskIds.delete(task.id!);
        this.tasks = this.tasks.map(item => item.id === task.id ? { ...task, status: previousStatus } : item);
        this.showToast(this.errorText(error, 'The task could not be moved.'));
      }
    });
  }

  statusLabel(status: TaskStatus): string {
    return status === 'TODO' ? 'To do' : status === 'IN_PROGRESS' ? 'In progress' : 'Done';
  }

  priorityLabel(priority: TaskPriority): string {
    return priority.charAt(0) + priority.slice(1).toLowerCase();
  }

  initials(name: string): string {
    return name.split(' ').filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }

  isOverdue(task: Task): boolean {
    if (!task.dueDate || task.status === 'DONE') {
      return false;
    }
    return new Date(`${task.dueDate}T23:59:59`).getTime() < Date.now();
  }

  isDueToday(task: Task): boolean {
    return task.dueDate === this.localDate(new Date());
  }

  dueLabel(task: Task): string {
    if (!task.dueDate) {
      return 'No due date';
    }
    if (this.isDueToday(task)) {
      return 'Today';
    }
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    if (task.dueDate === this.localDate(tomorrow)) {
      return 'Tomorrow';
    }
    return new Date(`${task.dueDate}T12:00:00`).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }

  updateFormTags(value: string): void {
    this.form.tags = value.split(',')
      .map(tag => tag.trim())
      .filter(Boolean)
      .slice(0, 5);
  }

  trackTask(_: number, task: Task): string | undefined {
    return task.id;
  }

  trackUser(_: number, user: UserAccount): string {
    return user.id;
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.statusFilter = 'ALL';
    this.priorityFilter = 'ALL';
    this.dueFilter = 'ALL';
    this.projectFilter = 'ALL';
  }

  showDue(filter: 'ALL' | 'TODAY' | 'WEEK' | 'OVERDUE' | 'NONE'): void {
    this.dueFilter = filter;
    this.selectSection('tasks');
  }

  private updateUser(user: UserAccount, update: { role?: UserRole; enabled?: boolean }): void {
    this.adminService.updateUser(user.id, update).subscribe({
      next: updated => {
        this.users = this.users.map(item => item.id === updated.id ? updated : item);
        this.showToast('Access updated.');
      },
      error: error => this.showToast(this.errorText(error, 'Access could not be updated.'))
    });
  }

  private emptyTask(): Task {
    return {
      title: '',
      description: '',
      status: 'TODO',
      priority: 'MEDIUM',
      assignee: this.currentUser?.fullName ?? '',
      project: '',
      dueDate: null,
      tags: [],
      storyPoints: null
    };
  }

  private matchesDueFilter(task: Task): boolean {
    if (this.dueFilter === 'ALL') {
      return true;
    }
    if (this.dueFilter === 'NONE') {
      return !task.dueDate;
    }
    if (this.dueFilter === 'OVERDUE') {
      return this.isOverdue(task);
    }
    if (this.dueFilter === 'TODAY') {
      return this.isDueToday(task);
    }
    if (!task.dueDate) {
      return false;
    }
    const today = new Date();
    const weekEnd = new Date();
    weekEnd.setDate(today.getDate() + 7);
    const due = new Date(`${task.dueDate}T12:00:00`);
    return due >= new Date(this.localDate(today) + 'T00:00:00') && due <= weekEnd;
  }

  private compareTasks(a: Task, b: Task): number {
    if (this.sortMode === 'TITLE') {
      return a.title.localeCompare(b.title);
    }
    if (this.sortMode === 'PRIORITY') {
      const weight: Record<TaskPriority, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };
      return weight[a.priority] - weight[b.priority];
    }
    if (this.sortMode === 'DUE') {
      return (a.dueDate ?? '9999-12-31').localeCompare(b.dueDate ?? '9999-12-31');
    }
    return (b.updatedAt ?? '').localeCompare(a.updatedAt ?? '');
  }

  private localDate(date: Date): string {
    const offset = date.getTimezoneOffset();
    return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 10);
  }

  private errorText(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return fallback;
  }

  private showToast(message: string): void {
    this.toastMessage = message;
    window.setTimeout(() => {
      if (this.toastMessage === message) {
        this.toastMessage = '';
      }
    }, 2800);
  }
}
