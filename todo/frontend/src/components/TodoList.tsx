import { Todo, TodoStatus } from '../types/todo';
import { TodoItem } from './TodoItem';

interface TodoListProps {
  todos: Todo[];
  isLoading: boolean;
  error: Error | null;
}

export function TodoList({ todos, isLoading, error }: TodoListProps) {
  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-800">
        <p className="font-semibold">Error loading todos</p>
        <p className="text-sm">{error.message}</p>
      </div>
    );
  }

  if (todos.length === 0) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-8 text-center">
        <p className="text-gray-600 text-lg">No todos yet. Add your first todo above!</p>
      </div>
    );
  }

  const pendingTodos = todos.filter((t) => t.status === TodoStatus.PENDING);
  const completedTodos = todos.filter((t) => t.status === TodoStatus.COMPLETED);

  return (
    <div className="space-y-6">
      {pendingTodos.length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-3 flex items-center gap-2">
            <span className="w-3 h-3 bg-yellow-500 rounded-full"></span>
            Pending ({pendingTodos.length})
          </h2>
          <div className="space-y-1">
            {pendingTodos.map((todo) => (
              <TodoItem key={todo.id} todo={todo} />
            ))}
          </div>
        </section>
      )}

      {completedTodos.length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-3 flex items-center gap-2">
            <span className="w-3 h-3 bg-green-500 rounded-full"></span>
            Completed ({completedTodos.length})
          </h2>
          <div className="space-y-1">
            {completedTodos.map((todo) => (
              <TodoItem key={todo.id} todo={todo} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
