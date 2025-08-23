# 🚀 Waver OpenShift Deployment Guide

Welcome to the complete deployment guide for Waver on OpenShift! This guide will walk you through deploying both the backend (as serverless functions) and frontend components, creating a fully functional AI-powered tutorial generation platform.

## 📋 Prerequisites

Before diving in, ensure you have:

- **OpenShift cluster access** with admin privileges
- **oc CLI** installed and authenticated
- **Quay.io account** (or access to another container registry)
- **API keys** for your chosen LLM provider (OpenAI or Google Gemini)
- **Git repository** cloned locally

```bash
git clone https://github.com/sshaaf/waver-kubernetes.git
cd waver-kubernetes
```

## 🎯 Deployment Overview

Our deployment consists of three main components:
1. **Waver Backend** - Serverless tutorial generation service (Knative)
2. **Waver Frontend** - Next.js web application (Traditional deployment)
3. **MinIO** - Object storage for generated tutorials

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │───▶│ Serverless       │───▶│   MinIO         │
│   (Next.js)     │    │ Backend (Knative)│    │ (Object Store)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 🔧 Step 1: Install OpenShift Serverless

OpenShift Serverless provides the Knative infrastructure needed for our backend functions.

### 1.1 Install Serverless Operator

```bash
# Create the openshift-serverless namespace
oc create namespace openshift-serverless

# Install the OpenShift Serverless Operator
cat << EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: serverless-operator
  namespace: openshift-serverless
spec:
  channel: stable
  name: serverless-operator
  source: redhat-operators
  sourceNamespace: openshift-marketplace
EOF
```

Wait for the operator to be ready:
```bash
oc get csv -n openshift-serverless
```

### 1.2 Create Knative Serving

```bash
# Create KnativeServing instance
cat << EOF | oc apply -f -
apiVersion: operator.knative.dev/v1beta1
kind: KnativeServing
metadata:
  name: knative-serving
  namespace: knative-serving
spec:
  ingress:
    kourier:
      enabled: true
  config:
    network:
      ingress-class: "kourier.ingress.networking.knative.dev"
EOF
```

### 1.3 Create Knative Eventing

```bash
# Create KnativeEventing instance  
cat << EOF | oc apply -f -
apiVersion: operator.knative.dev/v1beta1
kind: KnativeEventing
metadata:
  name: knative-eventing
  namespace: knative-eventing
EOF
```

Verify installation:
```bash
oc get knativeserving.operator.knative.dev/knative-serving -n knative-serving --template='{{range .status.conditions}}{{printf "%s=%s\n" .type .status}}{{end}}'
```

## 🏗️ Step 2: Deploy MinIO Object Storage

MinIO will store all generated tutorials and provide S3-compatible API access.

### 2.1 Create Project and Deploy MinIO

```bash
# Create project for Waver
oc new-project waver-demo

# Deploy MinIO with all required components
oc apply -f deploy/openshift/minio.yaml
```

### 2.2 Wait for MinIO to be Ready

```bash
# Monitor deployment progress
oc get pods -w

# Once running, get the MinIO console route
oc get route minio-console
```

### 2.3 Access MinIO Console

```bash
# Get MinIO admin credentials
MINIO_USER=$(oc get secret minio-root-user -o jsonpath='{.data.MINIO_ROOT_USER}' | base64 -d)
MINIO_PASSWORD=$(oc get secret minio-root-user -o jsonpath='{.data.MINIO_ROOT_PASSWORD}' | base64 -d)

echo "MinIO Console URL: https://$(oc get route minio-console -o jsonpath='{.spec.host}')"
echo "Username: $MINIO_USER"
echo "Password: $MINIO_PASSWORD"
```

## ⚡ Step 3: Deploy Waver Backend (Serverless)

Time to deploy our AI-powered backend as serverless functions!

### 3.1 Build and Push Backend Image

```bash
cd waver-backend

# Build the application
mvn clean package -DskipTests

# Build and push container image (replace with your registry)
export QUAY_USERNAME="your-quay-username"
mvn clean package -Dquarkus.container-image.build=true \
                  -Dquarkus.container-image.push=true \
                  -Dquarkus.container-image.registry=quay.io \
                  -Dquarkus.container-image.group=${QUAY_USERNAME} \
                  -Dquarkus.container-image.name=waver-backend
```

### 3.2 Create Backend Configuration

```bash
# Create secret for LLM API keys
oc create secret generic waver-backend-secrets \
  --from-literal=openai-api-key="your-openai-api-key" \
  --from-literal=gemini-api-key="your-gemini-api-key"

# Create backend configuration
cat << EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: waver-backend-config
  labels:
    app: waver-backend
data:
  waver.llm-provider: "OpenAI"
  waver.output-path: "/tmp/generated"
  waver.verbose: "true"
  waver.output-format: "MARKDOWN"
  minio.endpoint: "http://minio:9000"
  minio.bucket-name: "waver-bucket"
EOF
```

### 3.3 Deploy as Knative Service

```bash
# Create Knative Service for serverless backend
cat << EOF | oc apply -f -
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: waver-backend
  labels:
    app: waver-backend
spec:
  template:
    metadata:
      annotations:
        autoscaling.knative.dev/minScale: "0"
        autoscaling.knative.dev/maxScale: "10"
        autoscaling.knative.dev/target: "1"
    spec:
      containers:
      - name: waver-backend
        image: quay.io/${QUAY_USERNAME}/waver-backend:latest
        ports:
        - containerPort: 8080
          protocol: TCP
        env:
        - name: WAVER_LLM_PROVIDER
          valueFrom:
            configMapKeyRef:
              name: waver-backend-config
              key: waver.llm-provider
        - name: WAVER_OUTPUT_PATH
          valueFrom:
            configMapKeyRef:
              name: waver-backend-config
              key: waver.output-path
        - name: WAVER_VERBOSE
          valueFrom:
            configMapKeyRef:
              name: waver-backend-config
              key: waver.verbose
        - name: WAVER_OUTPUT_FORMAT
          valueFrom:
            configMapKeyRef:
              name: waver-backend-config
              key: waver.output-format
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: waver-backend-secrets
              key: openai-api-key
        - name: GEMINI_AI_KEY
          valueFrom:
            secretKeyRef:
              name: waver-backend-secrets
              key: gemini-api-key
        - name: MINIO_ENDPOINT
          valueFrom:
            configMapKeyRef:
              name: waver-backend-config
              key: minio.endpoint
        - name: MINIO_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: minio-root-user
              key: MINIO_ROOT_USER
        - name: MINIO_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: minio-root-user
              key: MINIO_ROOT_PASSWORD
        - name: MINIO_BUCKET_NAME
          valueFrom:
            configMapKeyRef:
              name: waver-backend-config
              key: minio.bucket-name
        resources:
          requests:
            memory: "512Mi"
            cpu: "200m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
EOF
```

### 3.4 Verify Backend Deployment

```bash
# Check Knative service status
oc get ksvc waver-backend

# Get the backend URL
BACKEND_URL=$(oc get ksvc waver-backend -o jsonpath='{.status.url}')
echo "Backend URL: $BACKEND_URL"

# Test the health endpoint
curl $BACKEND_URL/q/health/ready
```

## 🎨 Step 4: Deploy Waver Frontend

Now let's deploy the beautiful Next.js frontend that will interact with our serverless backend.

### 4.1 Build and Push Frontend Image

```bash
cd ../waver-site

# Build and push the frontend image
export QUAY_USERNAME="your-quay-username"
docker build -t quay.io/${QUAY_USERNAME}/waver-site:latest .
docker push quay.io/${QUAY_USERNAME}/waver-site:latest
```

### 4.2 Update Frontend Configuration

```bash
# Update configmap with the actual backend URL
cat << EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: waver-site-config
  labels:
    app: waver-site
data:
  minio-endpoint: "https://$(oc get route minio-s3 -o jsonpath='{.spec.host}')"
  minio-bucket: "waver-bucket"
  cloud-event-service-url: "${BACKEND_URL}"
EOF
```

### 4.3 Create Frontend Secrets

```bash
# Create frontend secrets for MinIO access
oc create secret generic waver-site-secrets \
  --from-literal=minio-access-key="$(oc get secret minio-root-user -o jsonpath='{.data.MINIO_ROOT_USER}' | base64 -d)" \
  --from-literal=minio-secret-key="$(oc get secret minio-root-user -o jsonpath='{.data.MINIO_ROOT_PASSWORD}' | base64 -d)"
```

### 4.4 Deploy Frontend Components

```bash
# Deploy all frontend resources
export QUAY_NAMESPACE=${QUAY_USERNAME}
export IMAGE_TAG="latest"
export CLUSTER_DOMAIN=$(oc get ingresses.config/cluster -o jsonpath={.spec.domain})

# Process and apply the deployment templates
envsubst < waver-site/openshift/deployment.yaml | oc apply -f -
envsubst < waver-site/openshift/service.yaml | oc apply -f -
envsubst < waver-site/openshift/route.yaml | oc apply -f -
```

### 4.5 Verify Frontend Deployment

```bash
# Check deployment status
oc get pods -l app=waver-site

# Get the frontend URL
FRONTEND_URL="https://$(oc get route waver-site-route -o jsonpath='{.spec.host}')"
echo "Frontend URL: $FRONTEND_URL"
```

## 🧪 Step 5: Test the Complete Setup

Let's verify everything works together by generating our first tutorial!

### 5.1 Test Backend Function Directly

```bash
# Test the serverless function with a sample repository
curl -X POST "${BACKEND_URL}/generate" \
  -H "Content-Type: application/json" \
  -d '{"sourceUrl": "https://github.com/quarkusio/quarkus-quickstarts"}'
```

### 5.2 Test with Cloud Events

```bash
# Test using Cloud Events format (more production-like)
curl -X POST "${BACKEND_URL}/" \
  -H "Ce-Specversion: 1.0" \
  -H "Ce-Type: waver.tutorial.generate" \
  -H "Ce-Source: waver-client" \
  -H "Ce-Id: test-123" \
  -H "Content-Type: application/json" \
  -d '{"sourceUrl": "https://github.com/quarkusio/quarkus-quickstarts"}'
```

### 5.3 Monitor Function Execution

```bash
# Watch the backend logs to see processing
oc logs -f -l app=waver-backend

# You should see:
# 🚀 Event is invoked, starting generation: https://github.com/...
# 🚀 Starting Tutorial Generation for: https://github.com/...
# 🚀 Generation has ended. Good bye! https://github.com/...
```

## 🚀 Quick Deployment with Script

For a fast, automated deployment, use the provided deployment script:

```bash
# Make the script executable
chmod +x deploy.sh

# Basic deployment with OpenAI
./deploy.sh -n my-waver -o "sk-your-openai-api-key"

# Deploy with Gemini AI
./deploy.sh -n prod-waver -p Gemini -g "your-gemini-api-key"

# Deploy with custom images and cleanup existing
./deploy.sh --cleanup -b quay.io/myorg/waver-backend:v1.2 -s quay.io/myorg/waver-site:v1.2

# Get help with all options
./deploy.sh --help
```

The script automates all the manual steps below and provides:
- ✅ **Automated OpenShift Serverless setup**
- ✅ **One-command deployment** of all components  
- ✅ **Parameter validation** and error handling
- ✅ **Deployment verification** and testing
- ✅ **Complete setup summary** with access URLs

## 🎉 Step 6: Using the Frontend

Your Waver platform is now live! Here's how to use it effectively:

### 6.1 Access the Web Interface

1. **Navigate to your frontend**: Open `$FRONTEND_URL` in your browser
2. **Browse existing tutorials**: The homepage shows previously generated content
3. **Generate new tutorials**: Use the generation form to create new content

### 6.2 Generate Your First Tutorial

1. **Enter a GitHub Repository URL**: 
   - Try: `https://github.com/quarkusio/quarkus-quickstarts`
   - Or your own repository URL

2. **Submit the Request**: The frontend sends a Cloud Event to your serverless backend

3. **Monitor Progress**: 
   - Check the browser console for any errors
   - Watch OpenShift logs: `oc logs -f -l app=waver-backend`

4. **View Results**: 
   - Generated tutorials appear automatically in the MinIO bucket
   - The frontend will display them once processing completes

### 6.3 Browse Generated Content

1. **MinIO Console**: Access at the MinIO console URL to see raw files
2. **Frontend Gallery**: View formatted tutorials through the web interface
3. **Direct Download**: Download markdown files directly from MinIO

## 🔍 Troubleshooting

### Backend Issues

**Function not scaling up:**
```bash
# Check Knative service status
oc describe ksvc waver-backend

# Verify image pull
oc get pods -l app=waver-backend
oc describe pod <pod-name>
```

**API Key issues:**
```bash
# Verify secrets exist
oc get secret waver-backend-secrets -o yaml

# Check environment variable injection
oc exec deployment/waver-backend -- env | grep -E "OPENAI|GEMINI"
```

### Frontend Issues

**Cannot connect to backend:**
```bash
# Verify configmap values
oc get configmap waver-site-config -o yaml

# Check service connectivity
oc exec deployment/waver-site -- curl -v $BACKEND_URL/q/health
```

**MinIO connection problems:**
```bash
# Test MinIO connectivity
oc exec deployment/waver-site -- curl -v https://$(oc get route minio-s3 -o jsonpath='{.spec.host}')
```

## 📊 Monitoring & Observability

### View Metrics

```bash
# Knative service metrics
oc get ksvc waver-backend -o yaml | grep -A 10 status

# Pod resource usage
oc top pods -l app=waver-backend
```

### Scaling Behavior

Your serverless backend will:
- **Scale to zero** when not in use (saves resources!)
- **Auto-scale up** based on request load (handles traffic spikes)
- **Scale between 0-10 replicas** as configured

## 🚀 Production Considerations

### Security Enhancements

1. **Use proper secrets management** (e.g., External Secrets Operator)
2. **Configure network policies** to restrict pod-to-pod communication
3. **Enable TLS everywhere** with proper certificates
4. **Set up RBAC** with minimal required permissions

### Performance Optimization

1. **Tune Knative scaling parameters** based on your usage patterns
2. **Configure persistent volumes** for MinIO in production
3. **Set appropriate resource limits** based on your LLM provider's requirements
4. **Enable horizontal pod autoscaling** for the frontend

### Monitoring & Alerting

1. **Install Prometheus/Grafana** for comprehensive monitoring
2. **Set up alerts** for failed tutorial generations
3. **Monitor API quota usage** for your LLM provider
4. **Track storage usage** in MinIO

## 🎊 Congratulations!

You've successfully deployed a complete AI-powered tutorial generation platform on OpenShift! Your setup includes:

✅ **Serverless Backend** - Scales automatically, pays per use
✅ **Modern Frontend** - Beautiful, responsive web interface  
✅ **Object Storage** - Secure, scalable tutorial storage
✅ **Production Ready** - Health checks, monitoring, and observability

## 🔗 What's Next?

- **Customize the UI** to match your branding
- **Add authentication** for private repositories
- **Integrate with CI/CD** for automatic tutorial updates
- **Extend with new LLM providers** or custom models
- **Build integrations** with your existing documentation systems

---

_Built with ❤️ for the Java community • Transform code into knowledge_

For questions, issues, or contributions, visit our [GitHub repository](https://github.com/sshaaf/waver-kubernetes).
