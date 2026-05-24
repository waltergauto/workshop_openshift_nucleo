import React, { useState } from 'react';
import './App.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://192.168.20.244:5000';

function App() {
  const [text, setText] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const analyze = async () => {
    if (!text.trim()) return;

    setLoading(true);
    setError('');
    setResult(null);

    try {
      const res = await fetch(`${API_URL}/analyze`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Request failed');
      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') analyze();
  };

  return (
    <div className="container">
      <h1>Feelings Analyzer</h1>
      <p className="subtitle">
        Write something and I'll tell you if it's positive or negative
      </p>

      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="How are you feeling today?"
        rows={4}
      />

      <button onClick={analyze} disabled={loading || !text.trim()}>
        {loading ? 'Analyzing...' : 'Analyze'}
      </button>

      {error && <div className="error">{error}</div>}

      {result && (
        <div className={`result ${result.sentiment}`}>
          <div className="sentiment-badge">{result.sentiment}</div>
          <p className="result-text">"{result.text}"</p>
          <p className="service-info">
            Resolved by: <code>{result.service}</code>
          </p>
        </div>
      )}
    </div>
  );
}

export default App;
