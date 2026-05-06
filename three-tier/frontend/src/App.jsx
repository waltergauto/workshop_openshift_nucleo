import { useState, useEffect } from 'react';
import ProductTable from './components/ProductTable';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8081';

export default function App() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(`${API_URL}/api/products`)
      .then(res => {
        if (!res.ok) throw new Error(`Error ${res.status}`);
        return res.json();
      })
      .then(data => {
        setProducts(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  return (
    <div className="app">
      <header className="header">
        <div className="header-inner">
          <div className="logo">
            <span className="logo-dot" />
            Workshop
          </div>
          <h1 className="title">Catálogo de Productos</h1>
          <span className="badge">Spring Boot + PostgreSQL</span>
        </div>
      </header>

      <main className="main">
        {loading && (
          <div className="state-box">
            <div className="spinner" />
            <p>Cargando productos...</p>
          </div>
        )}
        {error && (
          <div className="state-box error">
            <p>Error al conectar con la API</p>
            <code>{error}</code>
          </div>
        )}
        {!loading && !error && <ProductTable products={products} />}
      </main>
    </div>
  );
}
