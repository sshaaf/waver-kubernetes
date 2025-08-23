# Waver Backend

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Quarkus 3.24.5](https://img.shields.io/badge/Quarkus-3.24.5-blue.svg)](https://quarkus.io)
[![Maven](https://img.shields.io/badge/Maven-3.8+-green.svg)](https://maven.apache.org)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.0-yellow.svg)](https://github.com/langchain4j/langchain4j)

A powerful backend service for automatically generating comprehensive tutorials from source code repositories using Large Language Models (LLMs). Built on Quarkus for high-performance, cloud-native deployments with support for both traditional and serverless architectures.

## 🚀 Features

- **AI-Powered Tutorial Generation**: Leverages LangChain4j with OpenAI GPT or Google Gemini models
- **Multiple Deployment Modes**: Traditional JVM, native binary, serverless functions, or containers
- **Cloud Storage Integration**: Seamless MinIO integration for storing generated tutorials
- **Pipeline Architecture**: Extensible processing pipeline using JGraphlet framework
- **Reactive Messaging**: Asynchronous processing with MicroProfile Reactive Messaging
- **Configuration Management**: Type-safe configuration with Quarkus ConfigMapping
- **Development Ready**: Hot reload in dev mode with comprehensive testing suite

## 📋 Prerequisites

- **Java 21+** - Required for compilation and runtime
- **Maven 3.8+** - For dependency management and builds
- **Docker** (optional) - For containerized deployments and MinIO
- **MinIO Server** - For tutorial storage (can run locally via Docker)
- **LLM API Key** - OpenAI API key or Google AI API key

## 🛠️ Quick Start

### 1. Clone and Setup

```bash
git clone <your-repo-url>
cd waver-backend
```

### 2. Configure Environment

Create your environment configuration:

```bash
# Set your LLM API key
export OPENAI_API_KEY="your-openai-api-key"
# OR
export GEMINI_AI_KEY="your-gemini-api-key"
```

### 3. Start MinIO (Development)

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  quay.io/minio/minio server /data --console-address ":9001"
```

### 4. Run in Development Mode

```bash
mvn compile quarkus:dev
```

The application will start on `http://localhost:8080` with hot reload enabled.

## ⚙️ Configuration

The application uses `src/main/resources/application.properties` for configuration:

```properties
# LLM Provider Configuration
waver.llm-provider=OpenAI                    # or Gemini
waver.openai.api-key=${OPENAI_API_KEY}      # OpenAI API key
waver.gemini.api-key=${GEMINI_AI_KEY}       # Gemini API key

# Output Configuration
waver.output-path=generated                  # Local output directory
waver.verbose=true                          # Enable verbose logging
waver.output-format=MARKDOWN                # Output format

# MinIO Configuration (Development)
%dev.minio.endpoint=http://localhost:9000
%dev.minio.access-key=minioadmin
%dev.minio.secret-key=minioadmin
%dev.minio.bucket-name=waver-bucket

# Container Image Configuration
quarkus.container-image.registry=quay.io
quarkus.container-image.group=sshaaf
quarkus.container-image.name=waver-backend
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key for GPT models | - |
| `GEMINI_AI_KEY` | Google AI API key for Gemini models | - |
| `MINIO_ENDPOINT` | MinIO server endpoint | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO secret key | `minioadmin` |

## 🔌 API Usage

### Serverless Function (Funqy)

The primary endpoint for triggering tutorial generation:

```bash
# HTTP POST
curl -X POST http://localhost:8080/generate \
  -H "Content-Type: application/json" \
  -d '{"sourceUrl": "https://github.com/user/repo"}'

# Cloud Events
curl -X POST http://localhost:8080/ \
  -H "Ce-Specversion: 1.0" \
  -H "Ce-Type: waver.tutorial.generate" \
  -H "Ce-Source: waver-client" \
  -H "Ce-Id: 123" \
  -H "Content-Type: application/json" \
  -d '{"sourceUrl": "https://github.com/user/repo"}'
```

### Response

The function returns immediately while processing happens asynchronously. Monitor logs for processing status:

```
🚀 Event is invoked, starting generation: https://github.com/user/repo
🚀 Starting Tutorial Generation for: https://github.com/user/repo
🚀 Generation has ended. Good bye! https://github.com/user/repo
```

## 🏗️ Architecture

### Core Components

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   WaverFunqy    │───▶│ Reactive Channel │───▶│ ProcessingService│
│ (HTTP/Events)   │    │   (Async Queue)  │    │   (Background)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   MinIO Store   │◀───│  Upload Task     │◀───│ Pipeline Engine │
│   (Tutorials)   │    │   (Results)      │    │   (JGraphlet)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Processing Pipeline

1. **Code Crawler**: Analyzes source repository structure
2. **LLM Generation**: Creates tutorial content using configured LLM
3. **File Uploader**: Stores results in MinIO with organized structure
4. **Notification**: Logs completion status

### Key Classes

- **`WaverFunqy`**: Serverless function endpoint
- **`BackendProcessingService`**: Main orchestration service
- **`MinioUploaderTask`**: Handles file uploads to object storage
- **`WaverConfig`** & **`MinioConfig`**: Type-safe configuration interfaces

## 🔨 Building & Deployment

### JVM Mode

```bash
# Package JAR
mvn clean package

# Run
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Binary

```bash
# Build native executable
mvn clean package -Pnative

# Run native binary
./target/waver-backend-1.0-SNAPSHOT-runner
```

### Container Image

```bash
# Build container image
mvn clean package -Dquarkus.container-image.build=true

# Build and push to registry
mvn clean package -Dquarkus.container-image.build=true \
                  -Dquarkus.container-image.push=true
```

### OpenShift/Kubernetes

```bash
# Deploy to OpenShift
mvn clean package -Dquarkus.kubernetes.deploy=true
```

## 🧪 Testing

### Run Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify
```

### Test Coverage

The project includes comprehensive unit tests for all components:

- **Service Tests**: `BackendProcessingServiceTest`
- **Utility Tests**: `FileUtilTest`
- **MinIO Tests**: `MinioUploaderTaskTest`, `MinioClientProducerTest`
- **Function Tests**: `WaverFunqyTest`
- **Model Tests**: `WaverProcessEventTest`, `UploadResultTest`

All tests run without mocks, testing real functionality with test implementations.

## 🔧 Development

### Dev Mode Features

```bash
mvn compile quarkus:dev
```

- **Hot Reload**: Automatic recompilation on code changes
- **Dev UI**: Available at `http://localhost:8080/q/dev/`
- **Live Coding**: Instant feedback during development
- **Dev Services**: Automatic MinIO container management

### Adding New LLM Providers

1. Extend `LLMProvider` enum in `WaverConfig`
2. Add provider-specific configuration interface
3. Update `ModelProviderFactory` in `waver-llm` dependency
4. Configure API key mapping in `application.properties`

### Extending the Pipeline

The tutorial generation uses JGraphlet pipeline architecture:

```java
tasksPipeLine.add("Custom Task", new CustomTask())
    .add("Another Task", new AnotherTask())
    .run(generationContext);
```

## 🚀 Production Deployment

### Environment Setup

For production deployments, ensure:

1. **Secure API Keys**: Use proper secret management
2. **MinIO Configuration**: Production MinIO cluster with backup
3. **Resource Limits**: Configure appropriate JVM/native memory limits
4. **Monitoring**: Enable health checks and metrics
5. **Logging**: Configure structured logging with log aggregation

### Health Checks

Quarkus provides built-in health checks:

- **Liveness**: `http://localhost:8080/q/health/live`
- **Readiness**: `http://localhost:8080/q/health/ready`

## 📝 Troubleshooting

### Common Issues

**"LLM API key is missing"**
```bash
# Ensure environment variable is set
export OPENAI_API_KEY="your-key"
# Or check application.properties configuration
```

**MinIO connection errors**
```bash
# Check MinIO is running
docker ps | grep minio
# Verify endpoint configuration in application.properties
```

**Generation not starting**
```bash
# Check logs for exceptions in async processing
# Verify reactive messaging channel configuration
```

## 🤝 Contributing

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Code Standards

- Follow Java 21 conventions
- Maintain comprehensive Javadoc documentation
- Add unit tests for new functionality
- Use Quarkus best practices for CDI and configuration

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Related Projects

- **[waver-site](../waver-site/)**: Frontend Next.js application
- **[waver-llm](https://github.com/yourusername/waver-llm)**: LLM integration library
- **[JGraphlet](https://github.com/yourusername/jgraphlet)**: Pipeline processing framework

---

_Built with ❤️ for the Java community_ • Transform code into knowledge*

For questions, issues, or contributions, please visit our [GitHub repository](https://github.com/sshaaf/waver-kubernetes).
