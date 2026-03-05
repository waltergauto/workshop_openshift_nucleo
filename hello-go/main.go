package main

import (
  "encoding/json"
  "fmt"
  "log"
  "net/http"
  "os"
  "time"
)

type Response struct {
  Message   string `json:"message"`
  Timestamp time.Time `json:"timestamp"`
  Hostname  string `json:"hostname"`
  Version   string `json:"version"`
}

type HealthResponse struct {
  Status string `json:"status"`
  Version string `json:"version"`
}

var version = "1.0.0"

func helloHandler (w http.ResponseWriter, r *http.Request) {
  hostname, _ := os.Hostname ()

  resp := Response {
    Message: "Hello from OpenShift!",
    Timestamp: time.Now(),
    Hostname: hostname,
    Version: version,
  }

  w.Header().Set("Content-Type", "application/json")
  w.WriteHeader(http.StatusOK)
  json.NewEncoder(w).Encode(resp)

}

func healthHandler (w http.ResponseWriter, r *http.Request) {
  resp := HealthResponse {
    Status: "ok",
    Version: version,
  }
  
  w.Header().Set("Content-Type", "application/json")
  w.WriteHeader(http.StatusOK)
  json.NewEncoder(w).Encode(resp)
}

func main() {
  
    port := os.Getenv("PORT")
    if port == "" {
      port = "8080"
    }

    mux := http.NewServeMux()
    mux.HandleFunc("/api/v1/hello", helloHandler)
    mux.HandleFunc("/api/v1/health", healthHandler)
    mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
        fmt.Fprintf(w, "Go Hello API - Workshop OpenShift\nEndpoints:\n  GET /api/v1/hello\n  GET /api/v1/health\n")
    })

    log.Print("Servidor iniciado en el puerto %s", port)
    if err := http.ListenAndServe(":"+port, mux); err != nil {
      log.Fatalf("Error al inicial el servidor: %v", err)
    }
}
