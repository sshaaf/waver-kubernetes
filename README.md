# Waver

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.24.5-blue.svg)](https://quarkus.io)
[![Next.js](https://img.shields.io/badge/Next.js-15-black.svg)](https://nextjs.org/)
[![OpenShift](https://img.shields.io/badge/OpenShift-Ready-red.svg)](https://www.openshift.com/)
[![Serverless](https://img.shields.io/badge/Serverless-Knative-green.svg)](https://knative.dev/)

**Transform code into knowledge** • An AI-powered platform that automatically generates comprehensive tutorials from GitHub repositories using Large Language Models.

Waver takes the complexity out of documentation by analyzing your source code and creating detailed, structured tutorials that help developers understand and learn from real-world projects. Whether you're documenting your own work or exploring new codebases, Waver bridges the gap between code and comprehension.

## 🎯 What Waver Does

Waver analyzes any GitHub repository and creates:
- **📚 Comprehensive tutorials** with step-by-step explanations
- **🏗️ Architecture overviews** showing how components connect
- **💡 Implementation guides** breaking down complex patterns
- **📝 Code walkthroughs** explaining key decisions and techniques
- **🚀 Deployment instructions** for getting projects running

All powered by state-of-the-art LLMs (OpenAI GPT or Google Gemini) and delivered through a beautiful, modern web interface.

## 🏛️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Next.js UI    │───▶│   Cloud Events   │───▶│ Quarkus Backend │
│   (TypeScript)  │    │   (Serverless)   │    │ (Java 21 + AI)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                                              │
         ▼                                              ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Tutorial      │◀───│     MinIO        │◀───│   Generated     │
│   Browser       │    │   (S3 Storage)   │    │   Content       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## ⚡ Quick Start

### One-Command Deployment
```bash
# Deploy to OpenShift with your API key
./deploy.sh -n my-waver -o "your-openai-api-key"
```

### Local Development
```bash
# Clone the repository
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

## 🚀 Key Features

### **AI-Powered Analysis**
- **Smart Code Understanding** - Analyzes repository structure, dependencies, and patterns
- **Context-Aware Generation** - Creates tutorials that match the project's complexity and style
- **Multiple LLM Support** - Choose between OpenAI GPT and Google Gemini models

### **Production-Ready Platform**
- **Serverless Backend** - Auto-scaling Quarkus functions that scale to zero when not in use
- **Modern Frontend** - Responsive Next.js 15 application with beautiful UI
- **Cloud Storage** - MinIO integration for reliable tutorial storage and retrieval

### **Developer Experience**
- **One-Click Generation** - Simply paste a GitHub URL and watch the magic happen
- **Beautiful Presentations** - Syntax highlighting, Mermaid diagrams, and rich formatting
- **Search & Discovery** - Find and browse generated tutorials with powerful filtering

### **Enterprise Ready**
- **OpenShift Native** - Deploy anywhere OpenShift runs
- **CI/CD Integrated** - GitHub Actions workflows for automated builds and deployments
- **Secure by Design** - Proper secret management and network policies

## 📦 Components

### [🔧 Waver Backend](./waver-backend/)
**Java 21 + Quarkus + LangChain4j**
- Serverless tutorial generation functions
- LLM integration and processing pipeline
- MinIO storage management
- Health checks and monitoring

### [🎨 Waver Site](./waver-site/)
**Next.js 15 + React 19 + TypeScript**
- Beautiful, responsive web interface
- Real-time tutorial generation
- MinIO integration for content browsing
- Modern component-based architecture

### [🚀 Deployment](./DEPLOYMENT_GUIDE.md)
**OpenShift + Knative + MinIO**
- Complete deployment automation
- Serverless scaling and management
- Production-ready configurations
- Comprehensive troubleshooting

## 🎬 See It In Action

1. **Enter a GitHub URL** - Paste any public repository URL
2. **Watch the Generation** - AI analyzes the code structure and creates comprehensive tutorials
3. **Browse Results** - Rich, formatted tutorials with syntax highlighting and diagrams
4. **Share Knowledge** - Generated tutorials are immediately available for the community

### Example Repositories That Work Great:
- **Spring Boot applications** - Microservices tutorials with architecture explanations
- **React projects** - Component guides and state management patterns  
- **Python libraries** - API documentation and usage examples
- **DevOps repositories** - Deployment and infrastructure guides

## 🛠️ Development

### Backend Development
```bash
cd waver-backend
mvn compile quarkus:dev
```

### Frontend Development
```bash
cd waver-site
npm run dev
```

### Testing
```bash
# Backend tests (104 comprehensive tests)
cd waver-backend && mvn test

# Frontend linting and type checking
cd waver-site && npm run lint
```

## 📋 Prerequisites

- **Java 21+** for backend development
- **Node.js 18+** for frontend development
- **OpenShift cluster** for production deployment
- **LLM API key** (OpenAI or Google Gemini)

## 🚀 Deployment Options

### **Quick Deploy**
```bash
./deploy.sh -n production -o "your-api-key"
```

### **Custom Deployment**
```bash
# With Gemini AI and custom images
./deploy.sh -n custom -p Gemini -g "gemini-key" \
  -b quay.io/myorg/backend:v1.0 \
  -s quay.io/myorg/frontend:v1.0
```

### **Development Mode**
```bash
# Skip serverless checks for faster testing
./deploy.sh --skip-serverless-check -n dev-test -o "dev-key"
```

## 🎯 Use Cases

### **For Individual Developers**
- **Document your projects** automatically without writing extensive READMEs
- **Understand unfamiliar codebases** by generating architectural overviews
- **Create learning materials** from your favorite open source projects

### **For Teams & Organizations**  
- **Onboard new developers** with auto-generated project guides
- **Maintain up-to-date documentation** without manual effort
- **Share knowledge** across teams with consistent tutorial formats

### **For Educators & Content Creators**
- **Generate course materials** from real-world projects
- **Create coding tutorials** with authentic examples
- **Demonstrate best practices** from production codebases

## 🌟 What Makes Waver Special

### **Intelligent Analysis**
Unlike simple documentation generators, Waver understands context, patterns, and architectural decisions to create meaningful explanations.

### **Production Architecture**  
Built with enterprise-grade technologies that scale from development to production without configuration changes.

### **Modern Stack**
Leverages the latest in cloud-native technologies: serverless functions, reactive programming, and modern web frameworks.

### **Open Ecosystem**
Designed to integrate with existing DevOps workflows, CI/CD pipelines, and development tools.

## 🔄 CI/CD Integration

Waver includes production-ready GitHub Actions workflows:

- **Backend Pipeline** - Builds and pushes Quarkus applications to container registries
- **Frontend Pipeline** - Optimizes and deploys Next.js applications
- **Automated Testing** - Comprehensive test suites with 100+ test cases

## 🤝 Contributing

We welcome contributions! Whether it's:

- **🐛 Bug reports** and feature requests
- **💡 New LLM integrations** and processing improvements  
- **🎨 UI/UX enhancements** and new components
- **📚 Documentation** improvements and examples
- **🚀 Deployment** configurations and platform support

See individual component READMEs for detailed development guides.

## 🌍 Community & Support

- **GitHub Issues** - Report bugs and request features
- **Discussions** - Share use cases and get help from the community
- **Pull Requests** - Contribute code improvements and new features

## 🎊 Getting Started

1. **Clone this repository** and explore the codebase
2. **Follow the Quick Start** to see Waver in action locally  
3. **Deploy to OpenShift** using the automated deployment script
4. **Generate your first tutorial** from a GitHub repository you're curious about
5. **Share your experience** and help improve the platform

## 📚 Documentation

- **[Backend Documentation](./waver-backend/README.md)** - API, architecture, and development
- **[Frontend Documentation](./waver-site/README.md)** - UI components, features, and customization  
- **[Deployment Guide](./DEPLOYMENT_GUIDE.md)** - Complete OpenShift setup and configuration
- **[API Reference](./waver-backend/README.md#api-usage)** - REST endpoints and Cloud Events

---

_Built with ❤️ for the Java community • Transform code into knowledge_

**Ready to transform your code into knowledge?** Start with `./deploy.sh --help` or dive into the [Deployment Guide](./DEPLOYMENT_GUIDE.md) to get your Waver instance running in minutes.

For questions, issues, or contributions, visit our [GitHub repository](https://github.com/sshaaf/waver-kubernetes).
