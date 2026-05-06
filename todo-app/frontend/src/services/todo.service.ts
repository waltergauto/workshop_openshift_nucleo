import { Injectable } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { CreateTodoDto, TodoResponse } from './todo.dto';
import { trace, SpanKind, propagation, ROOT_CONTEXT } from '@opentelemetry/api';
import { firstValueFrom } from 'rxjs';
import { AxiosRequestConfig } from 'axios';

const tracer = trace.getTracer('todo-service');

@Injectable()
export class TodoService {
  private readonly backendUrl = process.env.BACKEND_URL || 'http://todo-backend:8080';

  constructor(private readonly httpService: HttpService) {}

  private injectContext(config: AxiosRequestConfig): AxiosRequestConfig {
    const carrier: Record<string, string> = {};
    propagation.inject(ROOT_CONTEXT, carrier);
    return {
      ...config,
      headers: {
        ...config.headers,
        ...carrier,
      },
    };
  }

  async getAllTodos(): Promise<TodoResponse[]> {
    return tracer.startActiveSpan('GET /todos', { kind: SpanKind.CLIENT }, async (span) => {
      try {
        span.setAttribute('http.method', 'GET');
        span.setAttribute('http.url', `${this.backendUrl}/todos`);
        
        const config = this.injectContext({});
        const response = await firstValueFrom(
          this.httpService.get<TodoResponse[]>(`${this.backendUrl}/todos`, config),
        );
        span.setAttribute('http.status_code', response.status);
        return response.data;
      } catch (error) {
        span.setAttribute('error', true);
        span.recordException(error);
        throw error;
      } finally {
        span.end();
      }
    });
  }

  async createTodo(dto: CreateTodoDto): Promise<TodoResponse> {
    return tracer.startActiveSpan('POST /todos', { kind: SpanKind.CLIENT }, async (span) => {
      try {
        span.setAttribute('http.method', 'POST');
        span.setAttribute('http.url', `${this.backendUrl}/todos`);
        
        const config = this.injectContext({});
        const response = await firstValueFrom(
          this.httpService.post<TodoResponse>(`${this.backendUrl}/todos`, dto, config),
        );
        span.setAttribute('http.status_code', response.status);
        return response.data;
      } catch (error) {
        span.setAttribute('error', true);
        span.recordException(error);
        throw error;
      } finally {
        span.end();
      }
    });
  }
}