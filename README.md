# Waver

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.24.5-blue.svg)](https://quarkus.io)
[![Next.js](https://img.shields.io/badge/Next.js-15-black.svg)](https://nextjs.org/)
[![OpenShift](https://img.shields.io/badge/OpenShift-Ready-red.svg)](https://www.openshift.com/)
[![Serverless](https://img.shields.io/badge/Serverless-Knative-green.svg)](https://knative.dev/)

**Transform code into knowledge** • AI-powered platform that automatically generates comprehensive tutorials from GitHub repositories using Large Language Models.

Waver analyzes your source code and creates detailed, structured tutorials with architecture overviews, implementation guides, and deployment instructions. Powered by OpenAI GPT or Google Gemini.

## ⚡ Quick Start

### One-Command Deployment
```bash
./deploy.sh -n my-waver -o "your-openai-api-key"
```

### Local Development
```bash
git clone https://github.com/sshaaf/waver-kubernetes.git
cd waver-kubernetes

# Start MinIO
docker run -d -p 9000:9000 -p 9001:9001 quay.io/minio/minio server /data --console-address ":9001"

# Start backend (requires API key)
cd waver-backend
export OPENAI_API_KEY="your-api-key"
mvn compile quarkus:dev

# Start frontend
cd ../waver-site  
npm install && npm run dev
```

Open [http://localhost:3000](http://localhost:3000) and start generating tutorials!

## 🏛️ Architecture

```
Frontend (Next.js) → Cloud Events → Serverless Backend (Quarkus + AI) → MinIO Storage
```

## 🚀 Features

- **AI-Powered**: Smart code analysis using OpenAI GPT or Google Gemini
- **Serverless**: Auto-scaling Quarkus functions on OpenShift Serverless  
- **Modern UI**: Beautiful Next.js 15 web interface
- **One-Click Deploy**: Automated OpenShift deployment script
- **Production Ready**: Enterprise-grade with CI/CD integration

## 📦 Components

- **[Backend](./waver-backend/)** - Java 21 + Quarkus + LangChain4j serverless functions
- **[Frontend](./waver-site/)** - Next.js 15 + TypeScript web application  
- **[Deployment](./DEPLOYMENT_GUIDE.md)** - Complete OpenShift setup guide

## 🎯 Usage

1. **Enter GitHub URL** - Any public repository
2. **AI Analysis** - Understands code structure and patterns  
3. **Tutorial Generation** - Creates comprehensive guides with diagrams
4. **Browse & Share** - View tutorials through the web interface

## 🛠️ Development

```bash
# Backend tests (104 comprehensive tests)
cd waver-backend && mvn test

# Frontend development
cd waver-site && npm run dev
```

## 📋 Prerequisites

- **Java 21+** and **Node.js 18+** for development
- **OpenShift cluster** for production deployment  
- **LLM API key** (OpenAI or Google Gemini)

## 🚀 Deployment

| Command | Description |
|---------|-------------|
| `./deploy.sh -n prod -o "key"` | Basic OpenAI deployment |
| `./deploy.sh -p Gemini -g "key"` | Use Gemini AI instead |
| `./deploy.sh --cleanup -n test` | Clean deploy to test namespace |

See the [Deployment Guide](./DEPLOYMENT_GUIDE.md) for complete setup instructions.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch  
3. Add tests for new functionality
4. Submit a pull request

See component READMEs for detailed development guides.

---

_Built with ❤️ for the Java community • Transform code into knowledge_

**Ready to start?** Try `./deploy.sh --help` or check the [Deployment Guide](./DEPLOYMENT_GUIDE.md).

For questions and contributions, visit our [GitHub repository](https://github.com/sshaaf/waver-kubernetes).