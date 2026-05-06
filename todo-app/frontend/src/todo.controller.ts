import { Controller, Get, Post, Body, Res, Req } from '@nestjs/common';
import { TodoService } from './services/todo.service';
import { CreateTodoDto } from './services/todo.dto';
import { Response, Request } from 'express';

@Controller('todos')
export class TodoController {
  constructor(private readonly todoService: TodoService) {}

  @Get()
  async getAllTodos(@Res() res: Response, @Req() req: Request) {
    const todos = await this.todoService.getAllTodos();
    return res.status(200).json(todos);
  }

  @Post()
  async createTodo(@Body() dto: CreateTodoDto, @Res() res: Response) {
    const todo = await this.todoService.createTodo(dto);
    return res.status(201).json(todo);
  }
}