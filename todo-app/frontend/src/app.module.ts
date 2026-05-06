import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { TodoController } from './todo.controller';
import { HealthController } from './health.controller';
import { TodoService } from './services/todo.service';

@Module({
  imports: [HttpModule],
  controllers: [TodoController, HealthController],
  providers: [TodoService],
})
export class AppModule {}