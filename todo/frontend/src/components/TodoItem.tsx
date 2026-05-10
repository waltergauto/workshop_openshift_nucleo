import { useState } from 'react';
import { Todo, TodoStatus } from '../types/todo';
import { useUpdateTodo, useDeleteTodo } from '../hooks/useTodos';

interface TodoItemProps {
  todo: Todo;
}

const statusColors: Record<TodoStatus, { bg: string; text: string; badge: string }> = {
  [TodoStatus.PENDING]: {
    bg: 'bg-yellow-50 border-yellow-200',
    text: 'text-yellow-800',
    badge: 'bg-yellow-100 text-yellow-800',
  },
  [TodoStatus.COMPLETED]: {
    bg: 'bg-green-50 border-green-200',
    text: 'text-green-800',
    badge: 'bg-green-100 text-green-800',
  },
  [TodoStatus.DELETED]: {
    bg: 'bg-red-50 border-red-200',
    text: 'text-red-800',
    badge: 'bg-red-100 text-red-800',
  },
};

export function TodoItem({ todo }: TodoItemProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(todo.title);
  const [editDescription, setEditDescription] = useState(todo.description || '');
  
  const updateTodo = useUpdateTodo();
  const deleteTodo = useDeleteTodo();
  
  const colors = statusColors[todo.status];

  const handleStatusChange = (newStatus: TodoStatus) => {
    updateTodo.mutate({ id: todo.id, request: { status: newStatus } });
  };

  const handleSaveEdit = () => {
    if (!editTitle.trim()) return;
    updateTodo.mutate({
      id: todo.id,
      request: { title: editTitle.trim(), description: editDescription.trim() || undefined },
    });
    setIsEditing(false);
  };

  const handleCancelEdit = () => {
    setEditTitle(todo.title);
    setEditDescription(todo.description || '');
    setIsEditing(false);
  };

  return (
    <div className={`border-l-4 ${colors.bg} border ${colors.text} rounded-lg p-4 mb-3 transition-all`}>
      {isEditing ? (
        <div className="space-y-3">
          <input
            type="text"
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            autoFocus
          />
          <textarea
            value={editDescription}
            onChange={(e) => setEditDescription(e.target.value)}
            rows={2}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
          />
          <div className="flex gap-2">
            <button
              onClick={handleSaveEdit}
              disabled={updateTodo.isPending}
              className="px-4 py-1 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 text-sm"
            >
              Save
            </button>
            <button
              onClick={handleCancelEdit}
              className="px-4 py-1 bg-gray-400 text-white rounded hover:bg-gray-500 text-sm"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <>
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <div className="flex items-center gap-3 mb-1">
                <h3 className={`font-semibold ${todo.status === TodoStatus.COMPLETED ? 'line-through opacity-70' : ''}`}>
                  {todo.title}
                </h3>
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colors.badge}`}>
                  {todo.status}
                </span>
              </div>
              {todo.description && (
                <p className="text-sm opacity-80 mt-1">{todo.description}</p>
              )}
              <p className="text-xs opacity-60 mt-2">
                Created: {new Date(todo.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>
          
          <div className="flex gap-2 mt-4">
            {todo.status === TodoStatus.PENDING && (
              <>
                <button
                  onClick={() => handleStatusChange(TodoStatus.COMPLETED)}
                  disabled={updateTodo.isPending}
                  className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 text-sm transition-colors"
                >
                  Complete
                </button>
                <button
                  onClick={() => setIsEditing(true)}
                  className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm transition-colors"
                >
                  Edit
                </button>
              </>
            )}
            {todo.status === TodoStatus.COMPLETED && (
              <button
                onClick={() => handleStatusChange(TodoStatus.PENDING)}
                disabled={updateTodo.isPending}
                className="px-3 py-1 bg-yellow-600 text-white rounded hover:bg-yellow-700 disabled:opacity-50 text-sm transition-colors"
              >
                Reopen
              </button>
            )}
            <button
              onClick={() => {
                if (confirm('Are you sure you want to delete this todo?')) {
                  deleteTodo.mutate(todo.id);
                }
              }}
              disabled={deleteTodo.isPending}
              className="px-3 py-1 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50 text-sm transition-colors"
            >
              {deleteTodo.isPending ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
