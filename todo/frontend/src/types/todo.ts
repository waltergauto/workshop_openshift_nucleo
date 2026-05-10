export enum TodoStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  DELETED = 'DELETED',
}

export interface Todo {
  id: number;
  title: string;
  description: string | null;
  status: TodoStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTodoRequest {
  title: string;
  description?: string;
}

export interface UpdateTodoRequest {
  title?: string;
  description?: string;
  status?: TodoStatus;
}
