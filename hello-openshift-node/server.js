const express = require('express');
const os = require('os');

const app = express();
const port = process.env.PORT || 8080;

app.get('/', (req, res) => {
  const hostname = os.hostname();
  res.send(`
    <html>
      <head>
        <title>Hello OpenShift</title>
        <style>
          body { font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f0f2f5; }
          .container { text-align: center; background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
          h1 { color: #ee0000; }
          .pod-name { font-size: 1.5rem; font-weight: bold; color: #333; margin-top: 1rem; padding: 0.5rem; background: #e2e8f0; border-radius: 4px; display: inline-block;}
        </style>
      </head>
      <body>
        <div class="container">
          <h1>Hello OpenShift!</h1>
          <p>La petición ha sido respondida por el Pod:</p>
          <div class="pod-name">${hostname}</div>
        </div>
      </body>
    </html>
  `);
});

app.listen(port, () => {
  console.log(`Aplicación Node.js escuchando en el puerto ${port}`);
});
