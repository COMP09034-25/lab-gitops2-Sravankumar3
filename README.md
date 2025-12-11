<img src="./atuLogo2.jpg" height="200" align="centre"/>

# ATU Cloud Native Computing

# Lab Part 2: GitOps Deployment with ArgoCD

## Introduction to GitOps

So far, your pipeline builds, tests, and packages your application into container images. But how do these images get deployed to Kubernetes?

**GitOps** is a deployment approach where your Git repository is the single source of truth for your infrastructure and application state. Instead of manually running `kubectl apply` or using scripts, a GitOps operator watches your Git repository and automatically synchronizes your Kubernetes cluster to match the desired state defined in Git.

### GitOps Principles

1. **Declarative**: Everything is described declaratively (YAML manifests)
2. **Versioned**: All configuration is stored in Git with full version history
3. **Immutable**: Container images are immutable and tagged with commit SHAs
4. **Pulled Automatically**: Changes are pulled and applied by the cluster, not pushed by CI/CD
5. **Continuously Reconciled**: The cluster state is continuously monitored and corrected

In this section, you'll use **ArgoCD** to implement GitOps for your catalog-service.

## Prerequisites

Before starting this section, ensure you have:

- Docker Desktop with Kubernetes enabled and running
- Your catalog-service container images pushed to GitHub Container Registry (completed in Part 1)


### Verify Your Setup

Check that your local Kubernetes cluster is running:

```bash
kubectl cluster-info
kubectl get nodes
```

You should see your Docker Desktop Kubernetes cluster responding.

### Rebuild Container Image (If Necessary)

If you need to rebuild your container image or don't have it pushed to GitHub Container Registry yet, follow these steps:

1. Navigate to your catalog-service repository:

```bash
cd path/to/catalog-service
```

2. Build the container image:

```bash
# Replace USERNAME with your GitHub username
docker build -t ghcr.io/comp09034-25/catalog-service-USERNAME:latest .
```

3. Log in to GitHub Container Registry:

```bash
# Create a Personal Access Token with write:packages scope if you don't have one
# Go to: GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)

echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

4. Push the image to GHCR:

```bash
docker push ghcr.io/comp09034-25/catalog-service-USERNAME:latest
```

5. Optionally, tag and push with a specific version:

```bash
# Get current commit SHA
COMMIT_SHA=$(git rev-parse --short HEAD)

# Tag the image
docker tag ghcr.io/comp09034-25/catalog-service-USERNAME:latest \
  ghcr.io/comp09034-25/catalog-service-USERNAME:${COMMIT_SHA}

# Push the tagged image
docker push ghcr.io/comp09034-25/catalog-service-USERNAME:${COMMIT_SHA}
```

Now your container image is available in GHCR and ready for deployment with ArgoCD.

## Part 1: Install ArgoCD

Install ArgoCD in your Kubernetes cluster:

```bash
# Create namespace for ArgoCD
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for all pods to be ready (this may take a few minutes)
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s
```

Verify the installation:

```bash
kubectl get pods -n argocd
```

You should see several pods including `argocd-server`, `argocd-repo-server`, and `argocd-application-controller`, all in Running state.

## Part 2: Access ArgoCD UI

### Expose the ArgoCD Server

In a terminal window, port-forward the ArgoCD server:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Keep this terminal open. The ArgoCD UI is now accessible at https://localhost:8080

### Get the Initial Admin Password

In a new terminal:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```

Copy this password - you'll need it to log in.

### Log In to ArgoCD UI

1. Open https://localhost:8080 in your browser
2. Accept the self-signed certificate warning (click "Advanced" → "Proceed")
3. Username: `admin`
4. Password: (the password from the previous command)

You should now see the ArgoCD dashboard.

### Optional: Install ArgoCD CLI

For command-line management, install the ArgoCD CLI:

**macOS:**
```bash
brew install argocd
```

**Linux:**
```bash
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
rm argocd-linux-amd64
```

**Windows:**
```bash
# Using Chocolatey
choco install argocd-cli

# Or download from: https://github.com/argoproj/argo-cd/releases/latest
```

Then login via CLI:

```bash
argocd login localhost:8080 --username admin --insecure
# Enter the password when prompted
```

## Part 3: Configure GitHub Container Registry Access

For your local cluster to pull images from GitHub Container Registry, you need to either make your package public or create an image pull secret.

### Option A: Make Your Package Public (Simplest)

1. Go to the COMP09034-25 organization packages: `github.com/orgs/COMP09034-25/packages`
2. Find your `catalog-service-USERNAME` package (where USERNAME is your GitHub username)
3. Click **Package settings** (right sidebar)
4. Scroll to **Danger Zone**
5. Click **Change visibility** → **Public**
6. Confirm the change

### Option B: Create Image Pull Secret (For Private Packages)

If you want to keep your package private, create an image pull secret:

1. First, create a GitHub Personal Access Token (PAT):
   - Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Generate new token with `read:packages` scope
   - Copy the token

2. Create the secret in your cluster:

```bash
kubectl create namespace catalog

kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  -n catalog
```

3. Update your `deployment.yaml` to reference the secret:

Add this under `spec.template.spec`:

```yaml
imagePullSecrets:
- name: ghcr-secret
```

## Part 4: Create ArgoCD Application

Now configure ArgoCD to watch your catalog-service repository and deploy your application. You can do this via UI, CLI, or a manifest file.

### Method 1: Via ArgoCD UI (Recommended for First Time)

1. In the ArgoCD UI, click **+ NEW APP** (top left)

2. Fill in the **GENERAL** section:
   - **Application Name**: `catalog-service`
   - **Project**: `default`
   - **Sync Policy**: `Manual`

3. Fill in the **SOURCE** section:
   - **Repository URL**: `https://github.com/COMP09034-25/catalog-service-USERNAME` (replace USERNAME)
   - **Revision**: `main` (or your main branch name)
   - **Path**: `k8s` (or wherever your Kubernetes manifests are located)

4. Fill in the **DESTINATION** section:
   - **Cluster URL**: `https://kubernetes.default.svc` (select from dropdown)
   - **Namespace**: `catalog`

5. Click **CREATE** at the top

### Method 2: Via ArgoCD CLI

```bash
# Replace USERNAME with your GitHub username in the repo URL
argocd app create catalog-service \
  --repo https://github.com/COMP09034-25/catalog-service-USERNAME.git \
  --path k8s \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace catalog \
  --sync-policy manual
```

### Method 3: Via Manifest (GitOps Way)

Create a file `argocd-application.yaml` in your catalog-service repository:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: catalog-service
  namespace: argocd
spec:
  project: default

  source:
    repoURL: https://github.com/COMP09034-25/catalog-service-USERNAME.git  # Replace USERNAME
    targetRevision: main
    path: k8s

  destination:
    server: https://kubernetes.default.svc
    namespace: catalog

  syncPolicy:
    syncOptions:
    - CreateNamespace=true
```

Apply it:
```bash
kubectl apply -f argocd-application.yaml
```

**Note**: Replace `USERNAME` with your actual GitHub username in the repository URL, and adjust the `path` if your manifests are in a different directory.

## Part 5: Initial Sync and Deployment

### Sync the Application

Your application is now registered in ArgoCD but not yet deployed. In the ArgoCD UI:

1. Click on your `catalog-service` application card
2. You'll see the application status is **OutOfSync** (yellow)
3. Click the **SYNC** button at the top
4. Review the resources that will be created
5. Click **SYNCHRONIZE**

ArgoCD will now deploy your application to the cluster.

### Watch the Deployment

In your terminal, watch the pods being created:

```bash
# Watch pods in real-time
kubectl get pods -n catalog -w

# In another terminal, check deployment status
kubectl get deployment -n catalog
kubectl get svc -n catalog
```

You should see your catalog-service pods starting up.

### Verify the Application

Once the pods are running, test your application:

```bash
# Port-forward to access the service
kubectl port-forward -n catalog svc/catalog-service 9001:80

# In another terminal, test the application
curl http://localhost:9001/

# Or open in browser
open http://localhost:9001
```

You should see your catalog-service responding.

## Part 6: Enable Auto-Sync

Currently, ArgoCD requires manual syncing. Let's enable automatic synchronization so ArgoCD automatically deploys changes when you push to Git.

### Via UI

1. Click your application in ArgoCD UI
2. Click **APP DETAILS** (top left button)
3. Click **ENABLE AUTO-SYNC**
4. Check **PRUNE RESOURCES** (removes resources deleted from Git)
5. Check **SELF HEAL** (automatically corrects manual changes)
6. Click **OK**

### Via CLI

```bash
argocd app set catalog-service \
  --sync-policy automated \
  --auto-prune \
  --self-heal
```

### Via Manifest

Update your `argocd-application.yaml` to include:

```yaml
syncPolicy:
  automated:
    prune: true
    selfHeal: true
  syncOptions:
  - CreateNamespace=true
```

Then apply: `kubectl apply -f argocd-application.yaml`

### What These Options Mean

- **Auto-Sync**: ArgoCD automatically syncs when it detects changes in Git
- **Prune**: Removes resources from the cluster if they're deleted from Git
- **Self-Heal**: Corrects any manual changes made to the cluster (reverts to Git state)

## Part 7: Test the GitOps Workflow

Now test the complete end-to-end workflow: code change → CI/CD → new image → GitOps deployment.

### Step 1: Make a Code Change

In your catalog-service repository, update your application code. For example, modify the welcome message:

```java
@GetMapping("/")
public String greeting() {
    return "Welcome to the Catalog Service - GitOps Edition";
}
```

### Step 2: Commit and Push

```bash
git add src/main/java/com/example/catalogservice/HomeController.java
git commit -m "Update greeting for GitOps demo"
git push
```

### Step 3: Watch CI/CD Pipeline

Go to GitHub Actions and watch your workflow:

1. **Build job** runs (compile, test)
2. **Package job** runs (builds container image)
3. New image is pushed to GHCR with the commit SHA tag

Note the commit SHA from your git push output or run:

```bash
git rev-parse --short HEAD
```

### Step 4: Update Kubernetes Manifest

Update your `deployment.yaml` to use the new image tag. Your deployment should reference the image in the COMP09034-25 org:

```bash
# Get the latest commit SHA
COMMIT_SHA=$(git rev-parse --short HEAD)

# Update the image in your deployment.yaml
# The image line should look like:
#   image: ghcr.io/comp09034-25/catalog-service-USERNAME:${COMMIT_SHA}
```

Or use sed (replace USERNAME with your actual GitHub username):

```bash
COMMIT_SHA=$(git rev-parse --short HEAD)
sed -i "s|catalog-service-USERNAME:.*|catalog-service-USERNAME:${COMMIT_SHA}|" k8s/deployment.yaml
```

Commit and push the manifest change:

```bash
git add k8s/deployment.yaml
git commit -m "Update image to ${COMMIT_SHA}"
git push
```

### Step 5: Watch ArgoCD Auto-Deploy

With auto-sync enabled, ArgoCD will:

1. Detect the change in Git (polls every 3 minutes by default, or immediately if using webhooks)
2. Compare cluster state to desired Git state
3. Automatically sync the new deployment
4. Roll out new pods with the updated image

**Watch in the UI**: The application will show "OutOfSync" → "Syncing" → "Synced"

**Watch in terminal**:

```bash
# Watch ArgoCD application status
argocd app get catalog-service --watch

# In another terminal, watch pods rolling out
kubectl get pods -n catalog -w
```

You'll see the old pods terminating and new pods starting with the updated image.

### Step 6: Verify the Change

Once the rollout completes:

```bash
curl http://localhost:9001/
# Should show: "Welcome to the Catalog Service - GitOps Edition"
```

Congratulations. You've successfully implemented GitOps.

## Part 8: Understanding the Complete Pipeline

Your complete cloud-native pipeline now looks like this:

```
┌────────────────────────────────────────────────────────────┐
│                    Developer Workflow                       │
│  (Write code, commit, push to GitHub)                       │
└────────────────────────────────────────────────────────────┘
                          │
                          │ git push
                          ▼
┌────────────────────────────────────────────────────────────┐
│              GitHub Repository (COMP09034-25 Org)           │
│  - Application source code                                  │
│  - Kubernetes manifests (k8s/)                              │
│  - CI/CD workflow definitions                               │
└────────────────────────────────────────────────────────────┘
                          │
                          │ triggers
                          ▼
┌────────────────────────────────────────────────────────────┐
│              GitHub Actions (CI/CD Pipeline)                │
├────────────────────────────────────────────────────────────┤
│  Commit Stage:                                              │
│    • Build & Test                                           │
│    • Dependency Submission                                  │
│                                                             │
│  Package Stage: (if main && build passes)                   │
│    • Build Container Image                                  │
│    • Tag with commit SHA                                    │
│    • Push to COMP09034-25 org in GHCR                       │
│      (as catalog-service-USERNAME)                          │
└────────────────────────────────────────────────────────────┘
                          │
                          │ pushes image
                          ▼
┌────────────────────────────────────────────────────────────┐
│    GitHub Container Registry (ghcr.io/comp09034-25)         │
│  - Images: catalog-service-USERNAME                         │
│  - Tagged with commit SHA for traceability                  │
│  - Each developer has uniquely named images                 │
└────────────────────────────────────────────────────────────┘
                          │                      ▲
                          │                      │
                          │                      │ pulls image
                          │                      │
                ┌─────────▼──────────────────────┴─────┐
                │    ArgoCD (GitOps Operator)          │
                ├──────────────────────────────────────┤
                │  • Monitors k8s/ directory in Git    │
                │  • Detects manifest changes          │
                │  • Compares desired vs actual state  │
                │  • Automatically syncs differences   │
                │  • Self-heals if state drifts        │
                └──────────────────────────────────────┘
                          │
                          │ applies manifests
                          ▼
┌────────────────────────────────────────────────────────────┐
│     Kubernetes Cluster (Local - per developer)              │
├────────────────────────────────────────────────────────────┤
│  Namespace: catalog                                         │
│    • Deployment: catalog-service                            │
│    • Pods: Running containerized application                │
│    • Service: Load balancing traffic                        │
└────────────────────────────────────────────────────────────┘
```

### Separation of Concerns

- **CI/CD (GitHub Actions)**: Responsible for building, testing, and packaging
- **GitOps (ArgoCD)**: Responsible for deploying and maintaining cluster state
- **Git**: Single source of truth for code AND infrastructure

### Benefits of This Approach

| Benefit | Description |
|---------|-------------|
| **Audit Trail** | Every deployment is a Git commit with full history |
| **Rollback** | Simple `git revert` to roll back any deployment |
| **Disaster Recovery** | Entire cluster can be recreated from Git |
| **Security** | No cluster credentials in CI/CD pipeline |
| **Declarative** | Infrastructure as code - desired state in YAML |
| **Automated** | No manual `kubectl apply` commands needed |
| **Consistency** | Same process for all environments |

## Part 9: ArgoCD Management Commands

### View Application Status

```bash
# Get application details
argocd app get catalog-service

# Watch application status in real-time
argocd app get catalog-service --watch

# List all applications
argocd app list
```

### Manual Sync

```bash
# Sync application manually
argocd app sync catalog-service

# Sync only specific resources
argocd app sync catalog-service --resource :Deployment:catalog-service
```

### View Differences

```bash
# See what's different between Git and cluster
argocd app diff catalog-service
```

### View Sync History

```bash
# View deployment history
argocd app history catalog-service

# In the UI: Click app → "HISTORY AND ROLLBACK" tab
```

### Rollback

To rollback to a previous version:

**Via Git** (Recommended):
```bash
# Revert the manifest change
git revert HEAD
git push

# ArgoCD will automatically sync to the previous state
```

**Via ArgoCD CLI**:
```bash
# Rollback to previous revision
argocd app rollback catalog-service

# Rollback to specific revision
argocd app rollback catalog-service 5
```

### Refresh Application

Force ArgoCD to check Git immediately (instead of waiting for poll interval):

```bash
argocd app get catalog-service --refresh
```

## Part 10: Troubleshooting

### Application Stuck OutOfSync

**Check sync status:**
```bash
argocd app get catalog-service
argocd app diff catalog-service
```

**Force sync:**
```bash
argocd app sync catalog-service --force
```

**Check ArgoCD logs:**
```bash
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-application-controller
```

### Pods Not Pulling Images

**Symptoms**: Pods in `ImagePullBackOff` state

**Diagnose:**
```bash
kubectl describe pod -n catalog POD_NAME

# Look for errors like:
# - "unauthorized: unauthenticated"
# - "manifest unknown"
```

**Solutions:**
1. Verify image exists in GHCR:
   ```bash
   # Replace USERNAME with your GitHub username
   docker pull ghcr.io/comp09034-25/catalog-service-USERNAME:TAG
   ```

2. Check package visibility (make it public or add image pull secret)

3. Verify image pull secret if using private packages:
   ```bash
   kubectl get secret ghcr-secret -n catalog
   ```

### ArgoCD Not Detecting Changes

**Force refresh:**
```bash
argocd app get catalog-service --refresh
```

**Check polling interval** (default: 3 minutes):
```bash
kubectl get cm argocd-cm -n argocd -o yaml | grep timeout
```

**View repo server logs:**
```bash
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-repo-server --tail=100
```

### Health Check Failures

If your application shows as "Degraded" in ArgoCD:

```bash
# Check pod status
kubectl get pods -n catalog
kubectl describe pod -n catalog POD_NAME

# Check application events in ArgoCD UI
# Click app → "EVENTS" tab

# Check deployment rollout status
kubectl rollout status deployment/catalog-service -n catalog
```

## Part 11: Cleanup

When you're finished with this lab:

### Delete the Application

**Via UI**: Click app → "DELETE" button

**Via CLI**:
```bash
argocd app delete catalog-service
```

**Via kubectl**:
```bash
kubectl delete -f argocd-application.yaml
```

### Delete Deployed Resources

```bash
kubectl delete namespace catalog
```

### Uninstall ArgoCD (Optional)

```bash
kubectl delete namespace argocd
```

The namespace and all ArgoCD components will be removed.

## Conclusion

You've successfully implemented a complete GitOps deployment pipeline using ArgoCD.

### What You Accomplished

- Installed ArgoCD on your local Kubernetes cluster
- Configured ArgoCD to monitor your Git repository in the COMP09034-25 organization
- Deployed your catalog-service using GitOps principles with unique naming
- Enabled automatic synchronization and self-healing
- Tested the complete workflow: code → CI/CD → GHCR → ArgoCD → K8s
- Understood the separation between CI/CD and deployment
- Achieved continuous deployment with Git as single source of truth

### Your Complete Cloud-Native Pipeline

You now have a production-grade deployment pipeline:

1. **Build Stage** (GitHub Actions): Compile, test
2. **Package Stage** (GitHub Actions): Create immutable container images with unique naming
3. **Deploy Stage** (ArgoCD): Automatically deploy to Kubernetes via GitOps

Every aspect is:
- **Automated**: No manual intervention required
- **Traceable**: Full audit trail in Git
- **Secure**: No cluster credentials in CI/CD
- **Declarative**: Infrastructure and applications defined as code
- **Recoverable**: Cluster state can be recreated from Git

