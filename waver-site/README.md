# Waver Site

[![Next.js 15](https://img.shields.io/badge/Next.js-15-black.svg)](https://nextjs.org/)
[![React 19](https://img.shields.io/badge/React-19-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![Tailwind CSS 4](https://img.shields.io/badge/Tailwind%20CSS-4-38B2AC.svg)](https://tailwindcss.com/)

A beautiful, modern frontend for the Waver AI-powered tutorial generation platform. Built with Next.js 15, this web application provides an intuitive interface for browsing existing tutorials and generating new ones from GitHub repositories using Large Language Models.

## ✨ Features

- **🎨 Modern UI**: Clean, responsive design built with Tailwind CSS and Radix UI components
- **🔍 Tutorial Discovery**: Browse and search through generated tutorials with category filtering
- **⚡ Real-time Generation**: Trigger AI-powered tutorial generation from GitHub repositories
- **📊 Cloud Events Integration**: Seamless communication with the serverless backend
- **🗃️ MinIO Integration**: Direct access to stored tutorials and files
- **📱 Responsive Design**: Optimized for desktop, tablet, and mobile devices
- **🎯 SSR Ready**: Server-side rendering for optimal performance and SEO
- **🔧 Type Safe**: Full TypeScript support with strict type checking

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Next.js UI    │───▶│   Cloud Events   │───▶│ Waver Backend   │
│   (Frontend)    │    │   (HTTP/Events)  │    │  (Serverless)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                                              │
         ▼                                              ▼
┌─────────────────┐                            ┌─────────────────┐
│   MinIO S3      │◀───────────────────────────│ Generated       │
│   (Storage)     │                            │ Tutorials       │
└─────────────────┘                            └─────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- **Node.js 18+** - Required for Next.js 15
- **npm or yarn** - Package manager
- **MinIO Server** - For tutorial storage (local or remote)
- **Waver Backend** - AI tutorial generation service

### 1. Install Dependencies

```bash
cd waver-site
npm install
```

### 2. Configure Environment

Create a `.env.local` file:

```env
# MinIO Configuration
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=waver-bucket

# Backend Service
CLOUD_EVENT_SERVICE_URL=http://localhost:8080
```

### 3. Run Development Server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) to see the application.

## 🛠️ Development

### Available Scripts

```bash
# Start development server with Turbopack
npm run dev

# Build for production
npm run build

# Start production server
npm start

# Run ESLint
npm run lint
```

### Project Structure

```
src/
├── app/                    # Next.js App Router
│   ├── api/               # API Routes
│   │   └── generate-tutorial/ # Tutorial generation endpoint
│   ├── tutorial/          # Tutorial pages
│   │   └── [slug]/       # Dynamic tutorial routes
│   ├── layout.tsx        # Root layout
│   └── page.tsx          # Homepage
├── components/           # React Components
│   ├── tutorial/        # Tutorial-specific components
│   ├── ui/              # Reusable UI components
│   ├── HomePageClient.tsx
│   └── MermaidRenderer.tsx
├── lib/                 # Utility libraries
│   ├── minio-client.ts  # MinIO integration
│   ├── tutorial-loader.ts
│   └── utils.ts
├── types/               # TypeScript definitions
└── data/               # Static data and configurations
```

### Key Components

#### **HomePageClient**
Main homepage component that handles:
- Tutorial browsing and filtering
- Search functionality
- GitHub repository submission for generation
- Category-based filtering

#### **TutorialDisplay**
Renders individual tutorials with:
- Markdown processing with syntax highlighting
- Mermaid diagram support
- Chapter navigation
- Responsive layout

#### **MinIO Integration**
Direct integration with object storage:
- Tutorial file listing and retrieval
- Metadata extraction
- Presigned URL generation for downloads

## 🔌 API Integration

### Tutorial Generation

```typescript
// POST /api/generate-tutorial
const response = await fetch('/api/generate-tutorial', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    repositoryUrl: 'https://github.com/user/repo'
  })
});
```

### Cloud Events

The frontend sends Cloud Events to the backend service:

```typescript
const cloudEvent = {
  specversion: '1.0',
  type: 'waver.tutorial.generate',
  source: 'waver-site',
  id: uuidv4(),
  time: new Date().toISOString(),
  datacontenttype: 'application/json',
  data: { sourceUrl: repositoryUrl }
};
```

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO server endpoint |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `MINIO_BUCKET` | `waver-bucket` | Default bucket for tutorials |
| `CLOUD_EVENT_SERVICE_URL` | `http://localhost:8080` | Backend service URL |

### Next.js Configuration

The application uses these Next.js features:
- **Standalone Output**: Optimized for container deployments
- **Server-side Environment**: Secure environment variable handling
- **App Router**: Modern routing with layouts and server components
- **TypeScript**: Full type safety across the application

## 🎨 Styling

### Tailwind CSS 4

The application uses Tailwind CSS 4 with:
- **Custom Design System**: Consistent spacing, colors, and typography
- **Dark Mode Ready**: CSS variables for theme switching
- **Component Variants**: Using `class-variance-authority` for component APIs
- **Responsive Design**: Mobile-first responsive utilities

### UI Components

Built with Radix UI primitives:
- **Accessible**: Full keyboard navigation and screen reader support
- **Customizable**: Styled with Tailwind CSS
- **Composable**: Flexible component composition patterns

## 🚀 Deployment

### Docker Build

```bash
# Build image
docker build -t waver-site .

# Run container
docker run -p 3000:3000 \
  -e MINIO_ENDPOINT=http://minio:9000 \
  -e CLOUD_EVENT_SERVICE_URL=http://backend:8080 \
  waver-site
```

### OpenShift Deployment

```bash
# Build and push to registry
docker build -t quay.io/yourusername/waver-site:latest .
docker push quay.io/yourusername/waver-site:latest

# Deploy using provided manifests
oc apply -f openshift/
```

### Environment Setup for Production

```bash
# Production environment variables
export MINIO_ENDPOINT=https://minio.your-domain.com
export MINIO_ACCESS_KEY=your-production-key
export MINIO_SECRET_KEY=your-production-secret
export CLOUD_EVENT_SERVICE_URL=https://backend.your-domain.com
```

## 🧪 Features in Detail

### Tutorial Generation Flow

1. **User Input**: User enters GitHub repository URL
2. **Validation**: Frontend validates URL format
3. **Cloud Event**: Sends structured event to backend service
4. **Processing**: Backend generates tutorial using AI
5. **Storage**: Results stored in MinIO bucket
6. **Display**: Frontend automatically shows new tutorial

### Tutorial Browsing

- **Category Filtering**: Browse by programming language or framework
- **Search**: Full-text search across tutorial titles and descriptions
- **Pagination**: Efficient loading of large tutorial collections
- **Preview**: Rich preview cards with metadata

### Markdown Processing

- **Syntax Highlighting**: Code blocks with language-specific highlighting
- **Mermaid Diagrams**: Interactive diagram rendering
- **GitHub Flavored Markdown**: Full GFM support including tables and task lists
- **Custom Components**: Enhanced rendering with custom React components

## 🔍 Troubleshooting

### Common Issues

**MinIO Connection Errors**
```bash
# Check MinIO connectivity
curl -v $MINIO_ENDPOINT/minio/health/live

# Verify credentials
docker exec minio-container mc admin info local
```

**Backend Service Unreachable**
```bash
# Test backend health
curl $CLOUD_EVENT_SERVICE_URL/q/health/ready

# Check service discovery
nslookup your-backend-service
```

**Build Failures**
```bash
# Clear Next.js cache
rm -rf .next node_modules
npm install
npm run build
```

### Development Tips

**Hot Reload Issues**
- Ensure Turbopack is enabled with `--turbopack` flag
- Check file permissions in mounted volumes
- Restart dev server if TypeScript cache is stale

**Environment Variables Not Loading**
- Restart development server after changing `.env.local`
- Check variable names match `process.env.VARIABLE_NAME`
- Verify variables are prefixed in `next.config.ts` if needed

## 🤝 Contributing

### Development Workflow

1. **Fork** the repository
2. **Create** feature branch (`git checkout -b feature/amazing-feature`)
3. **Install** dependencies (`npm install`)
4. **Start** dev server (`npm run dev`)
5. **Make** changes and test thoroughly
6. **Commit** changes (`git commit -m 'Add amazing feature'`)
7. **Push** to branch (`git push origin feature/amazing-feature`)
8. **Create** Pull Request

### Code Standards

- **ESLint**: Follow configured linting rules
- **TypeScript**: Maintain strict type safety
- **Components**: Use consistent component patterns
- **Styling**: Follow Tailwind CSS best practices
- **Testing**: Add tests for new functionality

## 📚 Related Documentation

- **[Waver Backend](../waver-backend/README.md)** - AI tutorial generation service
- **[Deployment Guide](../DEPLOYMENT_GUIDE.md)** - Complete OpenShift setup
- **[Next.js Docs](https://nextjs.org/docs)** - Framework documentation
- **[Tailwind CSS](https://tailwindcss.com/docs)** - Styling framework
- **[MinIO Client](https://min.io/docs/minio/linux/developers/javascript/minio-javascript.html)** - Object storage integration

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

_Built with ❤️ for the Java community • Transform code into knowledge_

For questions, issues, or contributions, visit our [GitHub repository](https://github.com/sshaaf/waver-kubernetes).
