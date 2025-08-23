#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default values
NAMESPACE="waver-demo"
BACKEND_IMAGE="quay.io/sshaaf/waver-backend:latest"
SITE_IMAGE="quay.io/sshaaf/waver-site:latest"
OPENAI_API_KEY=""
GEMINI_API_KEY=""
LLM_PROVIDER="OpenAI"
SKIP_SERVERLESS_CHECK=false
SKIP_MINIO=false
CLEANUP=false

# Function to print colored output
print_step() {
    echo -e "${BLUE}🚀 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Function to show usage
usage() {
    cat << EOF
🚀 Waver OpenShift Deployment Script

Usage: $0 [OPTIONS]

Options:
    -n, --namespace NAMESPACE           OpenShift namespace (default: waver-demo)
    -b, --backend-image IMAGE          Backend container image (default: quay.io/sshaaf/waver-backend:latest)
    -s, --site-image IMAGE             Frontend site image (default: quay.io/sshaaf/waver-site:latest)
    -o, --openai-key KEY               OpenAI API key (required if using OpenAI)
    -g, --gemini-key KEY               Gemini API key (required if using Gemini)
    -p, --llm-provider PROVIDER        LLM provider: OpenAI or Gemini (default: OpenAI)
    --skip-serverless-check            Skip OpenShift Serverless installation check
    --skip-minio                       Skip MinIO deployment (use existing)
    --cleanup                          Remove existing deployment before deploying
    -h, --help                         Show this help message

Examples:
    # Basic deployment with OpenAI
    $0 -n my-waver -o "sk-your-openai-key"
    
    # Deploy with Gemini in custom namespace
    $0 -n prod-waver -p Gemini -g "your-gemini-key"
    
    # Deploy with custom images
    $0 -b quay.io/myorg/backend:v1.0 -s quay.io/myorg/site:v1.0

Environment Variables (alternative to flags):
    WAVER_NAMESPACE, WAVER_OPENAI_KEY, WAVER_GEMINI_KEY, WAVER_LLM_PROVIDER

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -b|--backend-image)
            BACKEND_IMAGE="$2"
            shift 2
            ;;
        -s|--site-image)
            SITE_IMAGE="$2"
            shift 2
            ;;
        -o|--openai-key)
            OPENAI_API_KEY="$2"
            shift 2
            ;;
        -g|--gemini-key)
            GEMINI_API_KEY="$2"
            shift 2
            ;;
        -p|--llm-provider)
            LLM_PROVIDER="$2"
            shift 2
            ;;
        --skip-serverless-check)
            SKIP_SERVERLESS_CHECK=true
            shift
            ;;
        --skip-minio)
            SKIP_MINIO=true
            shift
            ;;
        --cleanup)
            CLEANUP=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Override with environment variables if set
NAMESPACE=${WAVER_NAMESPACE:-$NAMESPACE}
OPENAI_API_KEY=${WAVER_OPENAI_KEY:-$OPENAI_API_KEY}
GEMINI_API_KEY=${WAVER_GEMINI_KEY:-$GEMINI_API_KEY}
LLM_PROVIDER=${WAVER_LLM_PROVIDER:-$LLM_PROVIDER}

# Validate required parameters
if [[ "$LLM_PROVIDER" == "OpenAI" && -z "$OPENAI_API_KEY" ]]; then
    print_error "OpenAI API key is required when using OpenAI provider"
    print_info "Use -o flag or set WAVER_OPENAI_KEY environment variable"
    exit 1
fi

if [[ "$LLM_PROVIDER" == "Gemini" && -z "$GEMINI_API_KEY" ]]; then
    print_error "Gemini API key is required when using Gemini provider"
    print_info "Use -g flag or set WAVER_GEMINI_KEY environment variable"
    exit 1
fi

# Validate LLM provider
if [[ "$LLM_PROVIDER" != "OpenAI" && "$LLM_PROVIDER" != "Gemini" ]]; then
    print_error "Invalid LLM provider: $LLM_PROVIDER. Must be 'OpenAI' or 'Gemini'"
    exit 1
fi

print_step "Starting Waver deployment to OpenShift"
print_info "Namespace: $NAMESPACE"
print_info "Backend Image: $BACKEND_IMAGE"
print_info "Site Image: $SITE_IMAGE"
print_info "LLM Provider: $LLM_PROVIDER"

# Check if oc is available
if ! command -v oc &> /dev/null; then
    print_error "oc CLI is not installed or not in PATH"
    print_info "Please install the OpenShift CLI: https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/getting-started-cli.html"
    exit 1
fi

# Check if logged in to OpenShift
if ! oc whoami &> /dev/null; then
    print_error "Not logged in to OpenShift"
    print_info "Please login with: oc login <your-cluster-url>"
    exit 1
fi

print_success "Connected to OpenShift as $(oc whoami)"

# Check OpenShift Serverless installation
if [[ "$SKIP_SERVERLESS_CHECK" != "true" ]]; then
    print_step "Checking OpenShift Serverless installation"
    
    # Check if Serverless operator is installed
    if ! oc get csv -n openshift-serverless | grep -q "serverless-operator.*Succeeded"; then
        print_warning "OpenShift Serverless operator not found"
        print_info "Installing OpenShift Serverless operator..."
        
        oc create namespace openshift-serverless --dry-run=client -o yaml | oc apply -f -
        
        echo 'apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: serverless-operator
  namespace: openshift-serverless
spec:
  channel: stable
  installPlanApproval: Automatic
  name: serverless-operator
  source: redhat-operators
  sourceNamespace: openshift-marketplace
  startingCSV: serverless-operator.v1.36.0' | oc apply -f -
        
        print_info "Waiting for Serverless operator to be ready..."
        print_info "This may take 2-3 minutes for CRDs to be installed..."
        
        # Wait for the serverless operator to be ready
        print_info "Waiting for operator installation to complete..."
        for i in {1..20}; do
            if oc get csv -n openshift-serverless | grep -q "serverless-operator.*Succeeded"; then
                print_success "Serverless operator is ready"
                break
            fi
            print_info "Waiting for operator CSV... ($i/20)"
            sleep 15
        done
        
        # Wait for CRDs to be available
        print_info "Waiting for Knative CRDs..."
        for i in {1..12}; do
            if oc get crd knativeservings.operator.knative.dev &>/dev/null; then
                print_success "KnativeServing CRD is available"
                break
            fi
            print_info "Waiting for operator CRDs... ($i/12)"
            sleep 15
        done
        
        # Verify the CRD exists
        if ! oc get crd knativeservings.operator.knative.dev &>/dev/null; then
            print_error "Timeout waiting for Serverless operator CRDs"
            print_info "Please check: oc get csv -n openshift-serverless"
            exit 1
        fi
        
        # Create KnativeServing
        echo 'apiVersion: operator.knative.dev/v1beta1
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
      ingress-class: "kourier.ingress.networking.knative.dev"' | oc apply -f -


        # Wait for KnativeEventing CRD
        print_info "Waiting for KnativeEventing CRD..."
        for i in {1..8}; do
            if oc get crd knativeeventings.operator.knative.dev &>/dev/null; then
                print_success "KnativeEventing CRD is available"
                break
            fi
            print_info "Waiting for eventing CRD... ($i/8)"
            sleep 10
        done
        
        # Create KnativeEventing
        echo 'apiVersion: operator.knative.dev/v1beta1
kind: KnativeEventing
metadata:
  name: knative-eventing
  namespace: knative-eventing' | oc apply -f -
        
        print_info "Waiting for Knative to be ready..."
        print_info "Checking KnativeServing readiness..."
        oc wait --for=condition=Ready knativeserving/knative-serving -n knative-serving --timeout=300s || true
        
        # Wait for Knative Service CRD to be available
        print_info "Verifying Knative Service CRD availability..."
        for i in {1..10}; do
            if oc get crd services.serving.knative.dev &>/dev/null; then
                print_success "Knative Service CRD is available"
                break
            fi
            print_info "Waiting for Knative Service CRD... ($i/10)"
            sleep 15
        done
        
        # Final verification
        if ! oc get crd services.serving.knative.dev &>/dev/null; then
            print_error "Knative Service CRD not available after timeout"
            print_info "Debug: oc get knativeserving/knative-serving -n knative-serving"
            oc get knativeserving/knative-serving -n knative-serving || true
            exit 1
        fi
    fi
    
    print_success "OpenShift Serverless is ready"
fi

# Cleanup if requested
if [[ "$CLEANUP" == "true" ]]; then
    print_step "Cleaning up existing deployment"
    oc delete project $NAMESPACE --ignore-not-found=true
    print_info "Waiting for namespace cleanup..."
    sleep 30
fi

# Create namespace
print_step "Creating namespace: $NAMESPACE"
oc new-project $NAMESPACE --display-name="Waver AI Tutorial Generator" --description="AI-powered tutorial generation platform" || oc project $NAMESPACE

# Deploy MinIO if not skipped
if [[ "$SKIP_MINIO" != "true" ]]; then
    print_step "Deploying MinIO object storage"
    oc apply -f deploy/openshift/minio.yaml
    
    print_info "Waiting for MinIO to be ready..."
    oc wait --for=condition=available --timeout=300s deployment/minio || true
    sleep 10
    print_success "MinIO deployment completed"
else
    print_info "Skipping MinIO deployment (using existing)"
fi

# Get MinIO credentials
print_step "Getting MinIO credentials"
MINIO_USER=$(oc get secret minio-root-user -o jsonpath='{.data.MINIO_ROOT_USER}' | base64 -d)
MINIO_PASSWORD=$(oc get secret minio-root-user -o jsonpath='{.data.MINIO_ROOT_PASSWORD}' | base64 -d)

# Create backend secrets
print_step "Creating backend secrets and configuration"
oc delete secret waver-backend-secrets --ignore-not-found=true

oc create secret generic waver-backend-secrets \
  --from-literal=openai-api-key="${OPENAI_API_KEY:-}" \
  --from-literal=gemini-api-key="${GEMINI_API_KEY:-}"

# Create backend configuration
cat << EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: waver-backend-config
  labels:
    app: waver-backend
data:
  waver.llm-provider: "$LLM_PROVIDER"
  waver.output-path: "/tmp/generated"
  waver.verbose: "true"
  waver.output-format: "MARKDOWN"
  minio.endpoint: "http://minio:9000"
  minio.bucket-name: "waver-bucket"
EOF

# Deploy backend as Knative Service
print_step "Deploying Waver Backend (Serverless)"
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
        image: $BACKEND_IMAGE
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

print_info "Waiting for backend Knative service to be ready..."
oc wait --for=condition=Ready --timeout=300s ksvc/waver-backend || true
sleep 10

# Get backend URL
BACKEND_URL=$(oc get ksvc waver-backend -o jsonpath='{.status.url}')
print_success "Backend deployed at: $BACKEND_URL"

# Get cluster domain for frontend route
CLUSTER_DOMAIN=$(oc get ingresses.config/cluster -o jsonpath={.spec.domain})

# Create frontend configuration
print_step "Creating frontend configuration"
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
  cloud-event-service-url: "$BACKEND_URL"
EOF

# Create frontend secrets
oc delete secret waver-site-secrets --ignore-not-found=true
oc create secret generic waver-site-secrets \
  --from-literal=minio-access-key="$MINIO_USER" \
  --from-literal=minio-secret-key="$MINIO_PASSWORD"

# Deploy frontend
print_step "Deploying Waver Frontend"

# Service
cat << EOF | oc apply -f -
apiVersion: v1
kind: Service
metadata:
  name: waver-site-service
  labels:
    app: waver-site
spec:
  ports:
  - name: http
    port: 80
    protocol: TCP
    targetPort: 3000
  selector:
    app: waver-site
  type: ClusterIP
EOF

# Deployment
cat << EOF | oc apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: waver-site
  labels:
    app: waver-site
spec:
  replicas: 2
  selector:
    matchLabels:
      app: waver-site
  template:
    metadata:
      labels:
        app: waver-site
    spec:
      containers:
      - name: waver-site
        image: $SITE_IMAGE
        ports:
        - containerPort: 3000
          protocol: TCP
        env:
        - name: NODE_ENV
          value: "production"
        - name: MINIO_ENDPOINT
          valueFrom:
            configMapKeyRef:
              name: waver-site-config
              key: minio-endpoint
        - name: MINIO_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: waver-site-secrets
              key: minio-access-key
        - name: MINIO_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: waver-site-secrets
              key: minio-secret-key
        - name: MINIO_BUCKET
          valueFrom:
            configMapKeyRef:
              name: waver-site-config
              key: minio-bucket
        - name: CLOUD_EVENT_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: waver-site-config
              key: cloud-event-service-url
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 5
      restartPolicy: Always
EOF

# Route
cat << EOF | oc apply -f -
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: waver-site-route
  labels:
    app: waver-site
spec:
  host: waver-site-$NAMESPACE.apps.$CLUSTER_DOMAIN
  to:
    kind: Service
    name: waver-site-service
    weight: 100
  port:
    targetPort: http
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
EOF

print_info "Waiting for frontend deployment to be ready..."
oc wait --for=condition=available --timeout=300s deployment/waver-site || true

# Get frontend URL
FRONTEND_URL="https://$(oc get route waver-site-route -o jsonpath='{.spec.host}')"

print_step "Running deployment verification"

# Test backend health
print_info "Testing backend health endpoint..."
if curl -s -f "$BACKEND_URL/q/health/ready" > /dev/null; then
    print_success "Backend health check passed"
else
    print_warning "Backend health check failed (may still be starting)"
fi

# Test frontend
print_info "Testing frontend accessibility..."
if curl -s -f "$FRONTEND_URL" > /dev/null; then
    print_success "Frontend accessibility check passed"
else
    print_warning "Frontend accessibility check failed (may still be starting)"
fi

print_step "Deployment Summary"
echo
print_success "🎉 Waver deployment completed successfully!"
echo
print_info "📋 Deployment Details:"
echo "   Namespace: $NAMESPACE"
echo "   LLM Provider: $LLM_PROVIDER"
echo
print_info "🔗 Access URLs:"
echo "   Frontend: $FRONTEND_URL"
echo "   Backend API: $BACKEND_URL"
echo "   MinIO Console: https://$(oc get route minio-console -o jsonpath='{.spec.host}')"
echo
print_info "🔐 MinIO Credentials:"
echo "   Username: $MINIO_USER"
echo "   Password: $MINIO_PASSWORD"
echo
print_info "🧪 Test Your Deployment:"
echo "   # Test backend function"
echo "   curl -X POST \"$BACKEND_URL/generate\" \\"
echo "     -H \"Content-Type: application/json\" \\"
echo "     -d '{\"sourceUrl\": \"https://github.com/quarkusio/quarkus-quickstarts\"}'"
echo
echo "   # Monitor backend logs"
echo "   oc logs -f -l app=waver-backend"
echo
print_info "🎯 Next Steps:"
echo "   1. Open the frontend URL in your browser"
echo "   2. Generate your first tutorial using a GitHub repository"
echo "   3. Monitor the backend logs to see the AI generation process"
echo "   4. Access generated tutorials through MinIO or the web interface"
echo
print_success "Happy tutorial generating! 🚀✨"
