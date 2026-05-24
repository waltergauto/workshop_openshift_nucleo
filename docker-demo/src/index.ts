import express, { Request, Response } from 'express'

const app = express()
const PORT = process.env.PORT || 3000

app.use(express.json())

app.get('/', (_req: Request, res: Response) => {
  res.json({
    message: 'API funcionando',
    env: process.env.NODE_ENV || 'development',
    timestamp: new Date().toISOString(),
  })
})

app.get('/health', (_req: Request, res: Response) => {
  res.json({ status: 'ok' })
})

app.listen(PORT, () => {
  console.log(`Servidor corriendo en puerto ${PORT}`)
})
