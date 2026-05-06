export class CreateTodoDto {
  title!: string;
  description?: string;
  completed?: boolean;
}

export class TodoResponse {
  id!: number;
  title!: string;
  description?: string;
  completed!: boolean;
}