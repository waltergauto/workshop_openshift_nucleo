export default function ProductTable({ products }) {
  if (products.length === 0) {
    return <div className="state-box"><p>No hay productos disponibles.</p></div>;
  }

  return (
    <div className="table-wrapper">
      <div className="table-meta">
        <span>{products.length} productos encontrados</span>
      </div>
      <table className="table">
        <thead>
          <tr>
            <th>#</th>
            <th>Nombre</th>
            <th>Descripción</th>
            <th>Precio</th>
            <th>Stock</th>
          </tr>
        </thead>
        <tbody>
          {products.map(p => (
            <tr key={p.id}>
              <td className="id">{p.id}</td>
              <td className="name">{p.name}</td>
              <td className="desc">{p.description}</td>
              <td className="price">${p.price.toFixed(2)}</td>
              <td className="stock">
                <span className={`stock-badge ${p.stock > 20 ? 'high' : p.stock > 5 ? 'mid' : 'low'}`}>
                  {p.stock}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
