import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';
import { diag, DiagConsoleLogger } from '@opentelemetry/api';

const serviceName = process.env.OTEL_SERVICE_NAME || 'todo-frontend';
const otelEndpoint = process.env.OTEL_EXPORTER_OTLP_ENDPOINT || 'http://otel-collector:4317';

diag.setLogger(new DiagConsoleLogger());

const traceExporter = new OTLPTraceExporter({
  url: otelEndpoint,
});

const sdk = new NodeSDK({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: serviceName,
    [SemanticResourceAttributes.SERVICE_VERSION]: '1.0.0',
  }),
  traceExporter: traceExporter,
  instrumentations: [getNodeAutoInstrumentations()],
});

sdk.start();

process.on('SIGTRAP', () => {
  sdk.shutdown().then(() => process.exit(0)).catch((error) => process.exit(1));
});