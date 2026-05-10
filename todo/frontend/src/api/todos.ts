import { CreateTodoRequest, Todo, UpdateTodoRequest } from '../types/todo';

const API_BASE = '/todos';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || `HTTP error! status: ${response.status}`);
  }
  return response.json();
}

export async function getTodos(): Promise<Todo[]> {
  const response = await fetch(API_BASE);
  return handleResponse<Todo[]>(response);
}

export async function getTodo(id: number): Promise<Todo> {
  const response = await fetch(`${API_BASE}/${id}`);
  return handleResponse<Todo>(response);
}

export async function createTodo(request: CreateTodoRequest): Promise<Todo> {
  const response = await fetch(API_BASE, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<Todo>(response);
}

export async function updateTodo(id: number, request: UpdateTodoRequest): Promise<Todo> {
  const response = await fetch(`${API_BASE}/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<Todo>(response);
}

export async function deleteTodo(id: number): Promise<Todo> {
  const response = await fetch(`${API_BASE}/${id}`, {
    method: 'DELETE',
  });
  return handleResponse<Todo>(response);
}
